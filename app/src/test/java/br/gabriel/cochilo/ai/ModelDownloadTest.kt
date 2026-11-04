package br.gabriel.cochilo.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelDownloadTest {
    @Test fun urls_point_at_a_prerelease_of_this_repo() {
        assertEquals("https://github.com/gabrielbr/Cochilo/releases/download/assistant-model-v1/gemma3-1b-it-int4.task", ModelDownload.modelUrl("gabrielbr/Cochilo"))
        assertTrue(ModelDownload.sumUrl("gabrielbr/Cochilo").endsWith(".task.sha256"))
    }

    @Test fun reads_a_sha256sum_line_and_refuses_anything_else() {
        val h = "a".repeat(64)
        assertEquals(h, ModelDownload.parseSum("$h  gemma3-1b-it-int4.task\n"))
        assertEquals(h, ModelDownload.parseSum(h.uppercase()))
        assertNull(ModelDownload.parseSum("Not Found"))
        assertNull(ModelDownload.parseSum("<html>"))
        assertNull(ModelDownload.parseSum(""))
    }

    @Test fun verify_checks_the_header_the_size_and_the_hash() {
        val f = File.createTempFile("model", ".task")
        try {
            f.writeBytes(byteArrayOf('P'.code.toByte(), 'K'.code.toByte()) + ByteArray(10))
            assertEquals(ModelState.Reason.TOO_SMALL, ModelDownload.verify(f, ModelDownload.sha256(f)))
            f.writeBytes("<html>Not Found</html>".toByteArray())
            assertEquals(ModelState.Reason.NOT_A_MODEL, ModelDownload.verify(f, ModelDownload.sha256(f)))
            assertFalse(ModelDownload.sha256(f) == "0".repeat(64))
        } finally { f.delete() }
    }

    /** 0.55.0: Gemma 3n first on a phone with 8 GB, then the 1B for LiteRT-LM, then the old .task. */
    @Test fun the_model_follows_the_phones_memory() {
        val gb = 1024L * 1024 * 1024
        assertEquals(ModelDownload.Choice.entries.toList(), ModelDownload.choices(8 * gb - 600 * 1024 * 1024))
        assertEquals(listOf(ModelDownload.Choice.GEMMA_1B, ModelDownload.Choice.GEMMA_1B_TASK), ModelDownload.choices(4 * gb))
        // Gemma 3n is over GitHub's 2 GB per file, so it comes in two parts.
        assertEquals(2, ModelDownload.Choice.GEMMA_3N.parts.size)
        assertEquals("https://github.com/gabrielbr/Cochilo/releases/download/assistant-model-v1/gemma-3n-E2B-it-int4.litertlm.sha256",
            ModelDownload.sumUrl("gabrielbr/Cochilo", ModelDownload.Choice.GEMMA_3N))
    }
}
