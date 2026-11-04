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
    /** What the Settings row and the sheet promise for the 1B model (the file is about 555–585 MB). */
    const val APPROX_MB = 600

    /**
     * The models on the pre-release, best first (0.55.0). [file] is the whole file, whose `.sha256`
     * sits beside it; [parts], when GitHub's 2 GB limit per file splits it, are downloaded in turn
     * and joined before the check.
     */
    enum class Choice(val file: String, val parts: List<String>, val approxMb: Int) {
        /** Gemma 3n E2B (3,655,827,456 bytes), for a phone with the memory for it. */
        GEMMA_3N("gemma-3n-E2B-it-int4.litertlm", listOf("gemma-3n-E2B-it-int4.litertlm.part1", "gemma-3n-E2B-it-int4.litertlm.part2"), 3500),
        /** Gemma 3 1B for LiteRT-LM (584,417,280 bytes). */
        GEMMA_1B("gemma3-1b-it-int4.litertlm", listOf("gemma3-1b-it-int4.litertlm"), APPROX_MB),
    }

    /** Gemma 3n wants about 3 GB free while it runs: only on a phone with this much memory or more. */
    const val BIG_PHONE_BYTES = 7L * 1024 * 1024 * 1024

    /** The models to try, in order, for a phone with [totalMemoryBytes]. */
    fun choices(totalMemoryBytes: Long): List<Choice> =
        if (totalMemoryBytes >= BIG_PHONE_BYTES) Choice.entries.toList() else listOf(Choice.GEMMA_1B)

    fun url(repo: String, name: String) = "https://github.com/$repo/releases/download/$TAG/$name"
    fun sumUrl(repo: String, c: Choice) = url(repo, c.file) + ".sha256"

    fun modelUrl(repo: String) = url(repo, FILE)
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
        val head = file.inputStream().use { s -> ByteArray(16).also { s.read(it) } }
        ModelFile.check(head, file.length())?.let { return it }
        return if (sha256(file) == expectedSha) null else ModelState.Reason.CHECK
    }
}
