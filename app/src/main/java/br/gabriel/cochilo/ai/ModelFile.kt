package br.gabriel.cochilo.ai

import java.io.File
import java.io.InputStream

/** Checks on a model file before it's used: pure, so they're unit-tested. */
object ModelFile {
    /** A real model is hundreds of MB; anything this small was the wrong file. */
    const val MIN_BYTES = 50L * 1024 * 1024

    /** `.task` files are zip archives: they start with "PK". */
    fun looksLikeTask(head: ByteArray): Boolean = head.size >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()

    fun check(head: ByteArray, bytes: Long): ModelState.Reason? = when {
        !looksLikeTask(head) -> ModelState.Reason.NOT_A_MODEL
        bytes < MIN_BYTES -> ModelState.Reason.TOO_SMALL
        else -> null
    }

    /** Copies [input] to [target] through a temporary file, reporting bytes copied; the old file stays until the copy is whole. */
    fun copy(input: InputStream, target: File, onBytes: (Long) -> Unit): Long {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".part")
        var total = 0L
        tmp.outputStream().use { out ->
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                total += n
                onBytes(total)
            }
        }
        if (!tmp.renameTo(target)) { tmp.delete(); error("could not move the model into place") }
        return total
    }
}
