package br.gabriel.cochilo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChatStoreTest {
    private fun chat(id: String, at: Long, n: Int = 1) = Chat(id, at, at, "Q $id", (1..n).map { ChatItem("q$it", "not_sure") })

    @Test fun chats_are_kept_newest_first_and_survive_a_restart() {
        val f = File.createTempFile("chats", ".json"); f.delete()
        val s = ChatStore(f)
        s.save(chat("a", 1)); s.save(chat("b", 2)); s.save(chat("a", 3, n = 2))
        assertEquals(listOf("a", "b"), s.chats.value.map { it.id })
        assertEquals(2, s.chats.value.first().items.size)
        val again = ChatStore(f)
        assertEquals(listOf("a", "b"), again.chats.value.map { it.id })
        f.delete()
    }

    @Test fun an_empty_chat_is_not_kept_and_delete_can_be_undone() {
        val f = File.createTempFile("chats", ".json"); f.delete()
        val s = ChatStore(f)
        s.save(Chat("e", 1, 1, "", emptyList()))
        assertTrue(s.chats.value.isEmpty())
        val c = chat("x", 5); s.save(c); s.save(chat("y", 6))
        s.delete("x"); assertEquals(listOf("y"), s.chats.value.map { it.id })
        s.restore(c); assertEquals(listOf("y", "x"), s.chats.value.map { it.id })
        f.delete()
    }

    @Test fun the_title_is_the_first_question_short() {
        assertEquals("Quanto tempo duram as sonecas?", ChatStore.title("  Quanto tempo   duram as sonecas?  "))
        val long = ChatStore.title("a".repeat(100))
        assertEquals(60, long.length); assertTrue(long.endsWith("…"))
    }

    @Test fun a_broken_file_reads_as_no_chats() {
        val f = File.createTempFile("chats", ".json"); f.writeText("{not json")
        assertTrue(ChatStore(f).chats.value.isEmpty())
        f.delete()
    }
}
