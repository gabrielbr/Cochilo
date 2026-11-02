package br.gabriel.cochilo.update

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import br.gabriel.cochilo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/** Outcome of a check. "Couldn't check" is never reported as "up to date". */
sealed class CheckResult {
    data class Available(val info: UpdateInfo) : CheckResult()
    data object UpToDate : CheckResult()
    /** Debug build, or inside the 15-minute window between automatic checks. */
    data object Skipped : CheckResult()
    data class Failed(val reason: String) : CheckResult()
}

sealed class DownloadResult {
    data class Ready(val apk: File) : DownloadResult()
    data object Failed : DownloadResult()
    data object ChecksumMismatch : DownloadResult()
    data object SignatureMismatch : DownloadResult()
    /** Another download is already running in this process. */
    data object Busy : DownloadResult()
}

/**
 * The design system's self-update for the github build (docs/self-update.md there): checks
 * GitHub's latest release, downloads Cochilo's APK, and installs it only after four checks pass:
 * HTTPS all the way, the whole file arrived, its SHA-256 matches the release's SHA256SUMS.txt, and
 * it is this package signed with this app's key. The request carries nothing about the person or
 * the baby: no cookies, no IDs, just the app's name and version as User-Agent.
 */
object Updater {
    private const val PREFS = "self_update"
    private const val KEY_ETAG = "etag"
    private const val KEY_BODY = "body"
    private const val KEY_LAST = "last_check"
    private const val AUTO_INTERVAL_MS = 15 * 60_000L
    private val downloading = AtomicBoolean(false)

    fun isSupported(context: Context): Boolean = !BuildConfig.DEBUG && context.packageName == "br.gabriel.cochilo"

    private val latestUrl get() = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"
    private val userAgent get() = "Cochilo/${BuildConfig.VERSION_NAME}"

    /**
     * [force] (the About button) skips the 15-minute window and the ETag cache. A 304 re-reads the
     * cached release, so a version dismissed earlier is offered again next session.
     */
    suspend fun check(context: Context, force: Boolean): CheckResult = withContext(Dispatchers.IO) {
        if (!isSupported(context)) return@withContext CheckResult.Skipped
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST, 0L) < AUTO_INTERVAL_MS) return@withContext CheckResult.Skipped
        val cached = prefs.getString(KEY_BODY, null)
        val etag = if (!force && cached != null) prefs.getString(KEY_ETAG, null) else null

        val conn = try {
            open(latestUrl).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                if (etag != null) setRequestProperty("If-None-Match", etag)
            }
        } catch (e: IOException) {
            return@withContext CheckResult.Failed(reason(e))
        }
        try {
            val code = conn.responseCode
            val body = when {
                code == HttpURLConnection.HTTP_NOT_MODIFIED -> cached
                code in 200..299 -> conn.inputStream.bufferedReader().use { it.readText() }.also {
                    prefs.edit().putString(KEY_BODY, it).putString(KEY_ETAG, conn.getHeaderField("ETag")).apply()
                }
                else -> {
                    prefs.edit().putLong(KEY_LAST, now).apply()
                    return@withContext CheckResult.Failed("GitHub $code")
                }
            }
            prefs.edit().putLong(KEY_LAST, now).apply()
            if (body == null) return@withContext CheckResult.Failed("empty reply")
            when (val e = UpdateRules.evaluate(body, BuildConfig.VERSION_NAME)) {
                is Evaluation.Available -> CheckResult.Available(e.info)
                Evaluation.UpToDate -> CheckResult.UpToDate
                is Evaluation.Invalid -> CheckResult.Failed(e.reason)
            }
        } catch (e: IOException) {
            CheckResult.Failed(reason(e))
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Downloads [info]'s APK into cache/updates with progress 0..1. One download per process; a
     * cancelled or failed download deletes its file.
     */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): DownloadResult {
        if (!downloading.compareAndSet(false, true)) return DownloadResult.Busy
        val dir = File(context.cacheDir, "updates")
        val out = File(dir, info.apkName)
        var keep = false
        try {
            return withContext(Dispatchers.IO) {
                dir.mkdirs()
                dir.listFiles()?.forEach { it.delete() }

                // 3. The checksum comes from the same release; no published sum, no install.
                val expected = fetchText(info.sumsUrl)?.let { UpdateRules.parseSums(it)[info.apkName] }
                    ?: return@withContext DownloadResult.ChecksumMismatch

                val conn = open(info.apkUrl)
                val digest = MessageDigest.getInstance("SHA-256")
                var received = 0L
                try {
                    if (conn.responseCode !in 200..299) return@withContext DownloadResult.Failed
                    // 1. HTTPS only, redirects included (GitHub sends assets to its CDN).
                    if (!UpdateRules.isHttps(conn.url.toString())) return@withContext DownloadResult.Failed
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
                    conn.inputStream.use { input ->
                        out.outputStream().use { file ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                file.write(buf, 0, n)
                                digest.update(buf, 0, n)
                                received += n
                                if (total > 0) onProgress((received.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                    // 2. The whole file arrived.
                    if (info.apkSize > 0 && received != info.apkSize) return@withContext DownloadResult.Failed
                } finally {
                    conn.disconnect()
                }
                if (UpdateRules.hex(digest.digest()) != expected) return@withContext DownloadResult.ChecksumMismatch
                // 4. This package, signed with this app's key.
                if (!sameSigner(context, out)) return@withContext DownloadResult.SignatureMismatch
                keep = true
                DownloadResult.Ready(out)
            }
        } catch (_: IOException) {
            return DownloadResult.Failed
        } finally {
            if (!keep) out.delete()
            downloading.set(false)
        }
    }

    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Hands the verified APK to Android, which asks "Do you want to update this app?". */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun open(url: String): HttpURLConnection {
        if (!UpdateRules.isHttps(url)) throw IOException("not https")
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            // HttpURLConnection never follows a redirect from https to http; the final URL is
            // checked again after the download.
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("User-Agent", userAgent)
        }
    }

    private fun fetchText(url: String): String? {
        val conn = try { open(url) } catch (_: IOException) { return null }
        return try {
            if (conn.responseCode !in 200..299 || !UpdateRules.isHttps(conn.url.toString())) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun reason(e: IOException) = when (e) {
        is UnknownHostException -> "offline"
        else -> e.javaClass.simpleName.ifBlank { "network error" }
    }

    @SuppressLint("PackageManagerGetSignatures") // compared as a whole set, not trusted one by one
    @Suppress("DEPRECATION")
    private fun sameSigner(context: Context, apk: File): Boolean {
        val pm = context.packageManager
        fun signers(flagsNew: Boolean, archive: Boolean): Set<Signature>? {
            val info = if (archive) {
                pm.getPackageArchiveInfo(apk.path, if (flagsNew) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
                    ?.takeIf { it.packageName == context.packageName } ?: return null
            } else {
                pm.getPackageInfo(context.packageName, if (flagsNew) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
            }
            val list = if (flagsNew && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo?.apkContentsSigners else info.signatures
            return list?.toSet()?.takeIf { it.isNotEmpty() }
        }
        return try {
            val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            val archive = (if (modern) signers(true, archive = true) else null) ?: signers(false, archive = true) ?: return false
            val installed = (if (modern) signers(true, archive = false) else null) ?: signers(false, archive = false) ?: return false
            archive == installed
        } catch (_: Exception) {
            false
        }
    }
}
