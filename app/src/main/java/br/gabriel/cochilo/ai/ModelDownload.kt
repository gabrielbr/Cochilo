package br.gabriel.cochilo.ai

import java.io.File
import java.security.MessageDigest

/**
 * Where the family build downloads Ask Cochilo's model from, and how it checks what arrived
 * (docs/assistant.md). The model lives on a pre-release of Cochilo's own GitHub repository (so
 * self-update, which reads only full releases, never offers it), with a `.sha256` beside it.
 * Pure, so it's unit-tested.
 */
object ModelDownload {
    const val TAG = "assistant-model-v1"
    const val FILE = "gemma3-1b-it-int4.task"
    /** What the Settings row and the sheet promise (the file is 554,661,243 bytes). */
    const val APPROX_MB = 550

    fun modelUrl(repo: String) = "https://github.com/$repo/releases/download/$TAG/$FILE"
    fun sumUrl(repo: String) = modelUrl(repo) + ".sha256"

    /** The hash in a `sha256sum` line ("<64 hex>  <name>"), or null when it isn't one. */
    fun parseSum(text: String): String? =
        text.trim().split(Regex("\\s+")).firstOrNull()?.lowercase()?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { s ->
            val buf = ByteArray(1 shl 20)
            while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** What's wrong with a finished download, or null when it's the model we published. */
    fun verify(file: File, expectedSha: String): ModelState.Reason? {
        val head = file.inputStream().use { s -> ByteArray(2).also { s.read(it) } }
        ModelFile.check(head, file.length())?.let { return it }
        return if (sha256(file) == expectedSha) null else ModelState.Reason.CHECK
    }
}
