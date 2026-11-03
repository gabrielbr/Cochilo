package br.gabriel.cochilo.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class ModelFileTest {
    private val zip = byteArrayOf('P'.code.toByte(), 'K'.code.toByte())

    @Test fun only_a_task_file_of_a_real_size_is_taken() {
        assertNull(ModelFile.check(zip, 600L * 1024 * 1024))
        // Google's current Gemma .task isn't a zip: a binary header is fine.
        assertNull(ModelFile.check(byteArrayOf(0x1C, 0x00, 0x00, 0x00, 'T'.code.toByte()), 600L * 1024 * 1024))
        assertEquals(ModelState.Reason.NOT_A_MODEL, ModelFile.check("{}".toByteArray(), 600L * 1024 * 1024))
        assertEquals(ModelState.Reason.NOT_A_MODEL, ModelFile.check("  <!DOCTYPE html>".toByteArray(), 600L * 1024 * 1024))
        assertEquals(ModelState.Reason.TOO_SMALL, ModelFile.check(zip, 1024))
        assertFalse(ModelFile.looksLikeTask(ByteArray(0)))
    }

    @Test fun the_copy_lands_whole_and_reports_progress() {
        val dir = createTempDir()
        val target = File(dir, "models/assistant.task")
        val data = ByteArray(3 * (1 shl 20) + 7) { it.toByte() }
        val seen = mutableListOf<Long>()
        val n = ModelFile.copy(ByteArrayInputStream(data), target) { seen += it }
        assertEquals(data.size.toLong(), n)
        assertTrue(target.readBytes().contentEquals(data))
        assertEquals(data.size.toLong(), seen.last())
        assertFalse(File(target.parentFile, "assistant.task.part").exists())
        dir.deleteRecursively()
    }
}
