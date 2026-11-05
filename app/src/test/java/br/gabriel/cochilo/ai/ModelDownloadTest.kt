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
        val f = File.createTempFile("model", ".litertlm")
        try {
            f.writeBytes("LITERTLM".toByteArray() + ByteArray(10))
            assertEquals(ModelState.Reason.TOO_SMALL, ModelDownload.verify(f, ModelDownload.sha256(f)))
            f.writeBytes("<html>Not Found</html>".toByteArray())
            assertEquals(ModelState.Reason.NOT_A_MODEL, ModelDownload.verify(f, ModelDownload.sha256(f)))
            assertFalse(ModelDownload.sha256(f) == "0".repeat(64))
        } finally { f.delete() }
    }

    /** Gemma 3n first on a phone with 8 GB, then the 1B for LiteRT-LM (0.56.0: no more .task). */
    @Test fun the_model_follows_the_phones_memory() {
        val gb = 1024L * 1024 * 1024
        assertEquals(ModelDownload.Choice.entries.toList(), ModelDownload.choices(8 * gb - 600 * 1024 * 1024))
        assertEquals(listOf(ModelDownload.Choice.GEMMA_1B), ModelDownload.choices(4 * gb))
        // Gemma 3n is over GitHub's 2 GB per file, so it comes in two parts.
        assertEquals(2, ModelDownload.Choice.GEMMA_3N.parts.size)
        assertEquals("https://github.com/gabrielbr/Cochilo/releases/download/assistant-model-v1/gemma-3n-E2B-it-int4.litertlm.sha256",
            ModelDownload.sumUrl("gabrielbr/Cochilo", ModelDownload.Choice.GEMMA_3N))
    }

    /** 0.57.0: the model comes in pieces, and an interrupted download carries on from the first missing one. */
    @Test fun reads_the_pieces_file_and_refuses_anything_else() {
        val a = "a".repeat(64); val b = "b".repeat(64)
        val p = ModelDownload.parsePieces("$a 104857600 gemma3-1b-it-int4.litertlm.p00\n$b 12 gemma3-1b-it-int4.litertlm.p01\n")!!
        assertEquals(listOf(ModelDownload.Piece("gemma3-1b-it-int4.litertlm.p00", 104857600, a), ModelDownload.Piece("gemma3-1b-it-int4.litertlm.p01", 12, b)), p)
        assertEquals(p, ModelDownload.decode(ModelDownload.encode(p)))
        assertNull(ModelDownload.parsePieces("Not Found"))
        assertNull(ModelDownload.parsePieces("<html>"))
        assertNull(ModelDownload.parsePieces("$a 0 x.p00"))
        assertNull(ModelDownload.parsePieces("$a 10 ../../evil"))
        assertNull(ModelDownload.parsePieces(""))
        assertEquals("https://github.com/gabrielbr/Cochilo/releases/download/assistant-model-v1/gemma3-1b-it-int4.litertlm.pieces",
            ModelDownload.piecesUrl("gabrielbr/Cochilo", ModelDownload.Choice.GEMMA_1B))
        // Before pieces were published: the model's own parts, sizes unknown, still round-trip.
        val legacy = ModelDownload.legacyPieces(ModelDownload.Choice.GEMMA_3N)
        assertEquals(legacy, ModelDownload.decode(ModelDownload.encode(legacy)))
    }

    @Test fun carries_on_from_the_first_missing_piece() {
        val p = (0 until 4).map { ModelDownload.Piece("m.p0$it", 100, null) }
        assertEquals(0, ModelDownload.resumeAt(p) { _, _ -> false })
        assertEquals(2, ModelDownload.resumeAt(p) { i, _ -> i < 2 })
        // A hole is fetched again, even when later pieces are there.
        assertEquals(1, ModelDownload.resumeAt(p) { i, _ -> i != 1 })
        assertEquals(4, ModelDownload.resumeAt(p) { _, _ -> true })
    }

    @Test fun progress_counts_the_kept_pieces() {
        val p = listOf(ModelDownload.Piece("a", 300, null), ModelDownload.Piece("b", 100, null))
        assertEquals(0.75f, ModelDownload.progress(p, 1, 0, 0)!!, 0.001f)
        assertEquals(0.875f, ModelDownload.progress(p, 1, 50, 100)!!, 0.001f)
        // Sizes unknown (legacy parts): each part counts the same.
        val legacy = ModelDownload.legacyPieces(ModelDownload.Choice.GEMMA_3N)
        assertEquals(0.75f, ModelDownload.progress(legacy, 1, 50, 100)!!, 0.001f)
        assertEquals(0.5f, ModelDownload.progress(legacy, 1, 0, 0)!!, 0.001f)
    }

    @Test fun a_piece_is_good_only_whole_and_unchanged() {
        val f = File.createTempFile("piece", ".p00")
        try {
            f.writeBytes(ByteArray(10) { it.toByte() })
            val sha = ModelDownload.sha256(f)
            assertTrue(ModelDownload.pieceIsGood(f, ModelDownload.Piece("x", 10, sha)))
            assertFalse(ModelDownload.pieceIsGood(f, ModelDownload.Piece("x", 11, sha)))
            assertFalse(ModelDownload.pieceIsGood(f, ModelDownload.Piece("x", 10, "0".repeat(64))))
            assertTrue(ModelDownload.pieceIsGood(f, ModelDownload.Piece("x", -1, null)))
            f.delete()
            assertFalse(ModelDownload.pieceIsGood(f, ModelDownload.Piece("x", -1, null)))
        } finally { f.delete() }
    }
}
