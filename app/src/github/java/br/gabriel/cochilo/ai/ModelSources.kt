package br.gabriel.cochilo.ai

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import br.gabriel.cochilo.BuildConfig
import br.gabriel.cochilo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The family build: one tap downloads the model from Cochilo's own GitHub releases
 * ([ModelDownload]), through Android's download manager, so it keeps going with the app closed,
 * shows its progress in the system's notification, waits for Wi-Fi unless the parent says
 * otherwise, and resumes after a dropped connection. What arrives is checked against the
 * published SHA-256 before it's used. A file the parent already has can still be chosen instead.
 * The request carries nothing about the person or the baby.
 */
object ModelSources {
    fun create(context: Context): ModelSource = DownloadSource(context.applicationContext)
}

private const val PREFS = "assistant_model"
private const val KEY_ID = "download_id"
private const val KEY_SHA = "sha256"

private class DownloadSource(private val context: Context) : ModelSource {
    /** A chosen file is copied here. */
    private val imported = File(context.filesDir, "models/assistant.task")
    /** A download lands in the app's own external folder (no permission; other apps can't read it). */
    private val downloadedDir get() = File(context.getExternalFilesDir(null), "models")
    private val downloaded get() = File(downloadedDir, "assistant.task")
    private val partial get() = File(downloadedDir, "assistant.task.download")
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val dm = context.getSystemService(DownloadManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watching: Job? = null
    private val _state = MutableStateFlow<ModelState>(ModelState.Absent)
    override val kind = ModelSource.Kind.APP_DOWNLOAD
    override val state: StateFlow<ModelState> = _state

    init { refresh() }

    private fun present(): File? = listOf(imported, downloaded).firstOrNull { it.exists() }

    override fun refresh() {
        if (_state.value is ModelState.Getting) return
        val f = present()
        when {
            f != null -> _state.value = ModelState.Ready(f, f.length())
            prefs.getLong(KEY_ID, -1L) >= 0 -> watch()
            else -> _state.value = ModelState.Absent
        }
    }

    override fun download() {
        if (_state.value is ModelState.Getting) return
        start(wifiOnly = true)
    }

    override fun useMobileData() {
        cancelDownload()
        start(wifiOnly = false)
    }

    private fun start(wifiOnly: Boolean) {
        _state.value = ModelState.Getting(null)
        scope.launch {
            val sha = prefs.getString(KEY_SHA, null) ?: fetchSum()
            if (sha == null) { _state.value = ModelState.Failed(lastFailure); return@launch }
            downloadedDir.mkdirs()
            if (downloadedDir.usableSpace < ModelDownload.APPROX_MB * 1024L * 1024 + ModelFile.MIN_BYTES) {
                _state.value = ModelState.Failed(ModelState.Reason.NO_SPACE); return@launch
            }
            partial.delete()
            val request = DownloadManager.Request(Uri.parse(ModelDownload.modelUrl(BuildConfig.GITHUB_REPO)))
                .setTitle(context.getString(R.string.model_notification_title))
                .setDescription(context.getString(R.string.model_notification_line))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(partial))
                .setAllowedOverMetered(!wifiOnly)
                .setAllowedOverRoaming(false)
                .addRequestHeader("User-Agent", "Cochilo/${BuildConfig.VERSION_NAME}")
            val id = runCatching { dm.enqueue(request) }.getOrNull()
            if (id == null) { _state.value = ModelState.Failed(ModelState.Reason.DOWNLOAD); return@launch }
            prefs.edit().putLong(KEY_ID, id).putString(KEY_SHA, sha).apply()
            watch()
        }
    }

    private var lastFailure = ModelState.Reason.DOWNLOAD

    /** The published checksum, or null (and why) when it can't be read. */
    private fun fetchSum(): String? = runCatching {
        val conn = (URL(ModelDownload.sumUrl(BuildConfig.GITHUB_REPO)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 15_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Cochilo/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode == 404) { lastFailure = ModelState.Reason.NOT_PUBLISHED; return null }
            if (conn.responseCode != 200) { lastFailure = ModelState.Reason.DOWNLOAD; return null }
            ModelDownload.parseSum(conn.inputStream.bufferedReader().use { it.readText().take(512) })
                .also { if (it == null) lastFailure = ModelState.Reason.NOT_PUBLISHED }
        } finally { conn.disconnect() }
    }.getOrElse { lastFailure = ModelState.Reason.DOWNLOAD; null }

    /** Follows the download manager's progress until the download ends. */
    private fun watch() {
        if (watching?.isActive == true) return
        watching = scope.launch {
            while (isActive) {
                val id = prefs.getLong(KEY_ID, -1L)
                if (id < 0) return@launch
                val row = dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                    if (!c.moveToFirst()) null else Triple(
                        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                        c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) to
                            c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    )
                }
                if (row == null) { forget(); _state.value = ModelState.Absent; return@launch } // cancelled from the notification
                val (status, reason, sizes) = row
                val progress = sizes.second.takeIf { it > 0 }?.let { (sizes.first.toFloat() / it).coerceIn(0f, 1f) }
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> { finish(); return@launch }
                    DownloadManager.STATUS_FAILED -> {
                        forget(); partial.delete()
                        _state.value = ModelState.Failed(if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) ModelState.Reason.NO_SPACE else ModelState.Reason.DOWNLOAD)
                        return@launch
                    }
                    DownloadManager.STATUS_PAUSED -> _state.value = ModelState.Getting(progress, waitingForWifi = reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI)
                    else -> _state.value = ModelState.Getting(progress)
                }
                delay(1_000)
            }
        }
    }

    private fun finish() {
        val sha = prefs.getString(KEY_SHA, null)
        forget()
        val problem = if (sha == null || !partial.exists()) ModelState.Reason.DOWNLOAD else ModelDownload.verify(partial, sha)
        if (problem != null) {
            partial.delete()
            // A bad checksum may mean the published file changed: read it again next time.
            if (problem == ModelState.Reason.CHECK) prefs.edit().remove(KEY_SHA).apply()
            _state.value = ModelState.Failed(problem)
            return
        }
        downloaded.delete()
        _state.value = if (partial.renameTo(downloaded)) ModelState.Ready(downloaded, downloaded.length()) else ModelState.Failed(ModelState.Reason.OTHER)
    }

    private fun forget() = prefs.edit().remove(KEY_ID).apply()

    private fun cancelDownload() {
        val id = prefs.getLong(KEY_ID, -1L)
        if (id >= 0) runCatching { dm.remove(id) }
        forget()
        watching?.cancel()
        partial.delete()
    }

    override suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        cancelDownload()
        val resolver = context.contentResolver
        val size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()?.takeIf { it > 0 }
        val head = runCatching { resolver.openInputStream(uri)?.use { s -> ByteArray(2).also { s.read(it) } } }.getOrNull() ?: ByteArray(0)
        ModelFile.check(head, size ?: Long.MAX_VALUE)?.let { _state.value = ModelState.Failed(it); return@withContext }
        if (size != null && context.filesDir.usableSpace < size + ModelFile.MIN_BYTES) {
            _state.value = ModelState.Failed(ModelState.Reason.NO_SPACE); return@withContext
        }
        _state.value = ModelState.Getting(if (size != null) 0f else null, copying = true)
        val copied = runCatching {
            resolver.openInputStream(uri)!!.use { input ->
                ModelFile.copy(input, imported) { done -> if (size != null) _state.value = ModelState.Getting((done.toFloat() / size).coerceIn(0f, 1f), copying = true) }
            }
        }
        _state.value = copied.fold(
            onSuccess = { bytes ->
                if (bytes < ModelFile.MIN_BYTES) { imported.delete(); ModelState.Failed(ModelState.Reason.TOO_SMALL) }
                else { downloaded.delete(); ModelState.Ready(imported, bytes) }
            },
            onFailure = { ModelState.Failed(ModelState.Reason.OTHER) },
        )
    }

    override fun remove() {
        cancelDownload()
        imported.delete()
        downloaded.delete()
        _state.value = ModelState.Absent
    }
}
