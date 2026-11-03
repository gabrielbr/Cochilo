package br.gabriel.cochilo.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Ask Cochilo's chats (docs/assistant.md, 0.40.0): she can keep several conversations, one per
 * topic, and come back to them. Each answer is kept as it was shown (its words and numbers at the
 * time), so an old chat never changes under her. On the phone only: not in backups or exports.
 */
@Serializable
data class Chat(val id: String, val created: Long, val updated: Long, val title: String, val items: List<ChatItem>)

/**
 * One question and its answer, frozen. [kind]: "about" (a reviewed answer: [data], [info],
 * [tryIt], [source]), "care" ([care] names who to ask), "not_sure", or "generated" (the
 * on-device model's checked words in [generated]). [unchecked]: the model's reply failed the check.
 */
@Serializable
data class ChatItem(
    val question: String,
    val kind: String,
    val topic: String? = null,
    val care: String? = null,
    val data: String? = null,
    val info: String? = null,
    val tryIt: String? = null,
    val source: String? = null,
    val generated: String? = null,
    val unchecked: Boolean = false,
)

/** The chats, newest first, in one small JSON file in the app's own storage. */
class ChatStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _chats = MutableStateFlow(load())
    val chats: StateFlow<List<Chat>> = _chats

    private fun load(): List<Chat> = runCatching { json.decodeFromString<List<Chat>>(file.readText()) }.getOrDefault(emptyList())
        .sortedByDescending { it.updated }

    /** Saves [chat] (new or changed) at the top; an empty one isn't kept. */
    fun save(chat: Chat) {
        val rest = _chats.value.filter { it.id != chat.id }
        write(if (chat.items.isEmpty()) rest else (listOf(chat) + rest).take(MAX))
    }

    fun delete(id: String) = write(_chats.value.filter { it.id != id })

    /** Puts a deleted chat back (Undo). */
    fun restore(chat: Chat) = write((_chats.value.filter { it.id != chat.id } + chat).sortedByDescending { it.updated })

    private fun write(list: List<Chat>) {
        _chats.value = list
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Chat.serializer()), list))
            tmp.renameTo(file)
        }
    }

    companion object {
        /** Plenty for a family; the oldest goes when there are more. */
        const val MAX = 50

        /** A chat's name: its first question, short. */
        fun title(firstQuestion: String): String = firstQuestion.trim().replace(Regex("\\s+"), " ").let {
            if (it.length <= TITLE_CHARS) it else it.take(TITLE_CHARS - 1).trimEnd() + "…"
        }

        private const val TITLE_CHARS = 60
    }
}
