package br.gabriel.cochilo.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The design system's self-update rules (docs/self-update.md there), as pure Kotlin so they are
// unit-tested in both builds. Only the github build does anything with them.

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
)

/** A newer release, resolved to Cochilo's APK and the release's checksum file. */
data class UpdateInfo(
    val versionName: String,
    val notes: String,
    val apkName: String,
    val apkUrl: String,
    val apkSize: Long,
    val sumsUrl: String,
)

sealed class Evaluation {
    data class Available(val info: UpdateInfo) : Evaluation()
    data object UpToDate : Evaluation()
    data class Invalid(val reason: String) : Evaluation()
}

object UpdateRules {
    const val SUMS_FILE = "SHA256SUMS.txt"
    private val json = Json { ignoreUnknownKeys = true }

    /** The family build's asset in a release: `cochilo-0.2.0.apk`. Play's files never match. */
    fun apkName(version: String) = "cochilo-$version.apk"

    /** "v0.2.0", "0.2.0" or "0.2.0-debug" → [0, 2, 0]; anything else → null. */
    fun parseVersion(text: String): List<Int>? {
        val core = text.trim().removePrefix("v").substringBefore('-').substringBefore('+')
        val parts = core.split('.')
        if (parts.size !in 1..4) return null
        return parts.map { p -> p.toIntOrNull()?.takeIf { it >= 0 } ?: return null }
    }

    /** True only when [remote] is strictly newer than [installed]; unreadable versions are never newer. */
    fun isNewer(remote: String, installed: String): Boolean {
        val r = parseVersion(remote) ?: return false
        val i = parseVersion(installed) ?: return false
        for (k in 0 until maxOf(r.size, i.size)) {
            val a = r.getOrElse(k) { 0 }
            val b = i.getOrElse(k) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    fun isHttps(url: String) = url.startsWith("https://", ignoreCase = true)

    /**
     * Compares the releases API's "latest" reply with the installed version. Drafts, pre-releases,
     * older or equal versions and releases without Cochilo's APK are "no update", never an error.
     */
    fun evaluate(body: String, installedVersion: String): Evaluation {
        val release = try {
            json.decodeFromString(GithubRelease.serializer(), body)
        } catch (_: Exception) {
            return Evaluation.Invalid("unexpected reply")
        }
        if (release.draft || release.prerelease) return Evaluation.UpToDate
        val version = release.tagName.removePrefix("v")
        if (!isNewer(version, installedVersion)) return Evaluation.UpToDate
        val apk = release.assets.firstOrNull { it.name == apkName(version) } ?: return Evaluation.UpToDate
        val sums = release.assets.firstOrNull { it.name == SUMS_FILE } ?: return Evaluation.UpToDate
        if (!isHttps(apk.url) || !isHttps(sums.url)) return Evaluation.UpToDate
        return Evaluation.Available(
            UpdateInfo(
                versionName = version,
                // The release text's first paragraph is "what's new"; the rest is install help for
                // the release page, not for the sheet.
                notes = release.body?.replace("\r\n", "\n")?.trim()?.substringBefore("\n\n")?.trim().orEmpty(),
                apkName = apk.name,
                apkUrl = apk.url,
                apkSize = apk.size,
                sumsUrl = sums.url,
            )
        )
    }

    /** `sha256sum` output ("<hex>  <name>" or "<hex> *<name>") → name to lowercase hex. */
    fun parseSums(text: String): Map<String, String> = text.lineSequence()
        .map { it.trim() }
        .mapNotNull { line ->
            val m = Regex("""^([0-9a-fA-F]{64})\s+\*?(.+)$""").find(line) ?: return@mapNotNull null
            m.groupValues[2].trim() to m.groupValues[1].lowercase()
        }
        .toMap()

    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}
