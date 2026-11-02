package br.gabriel.cochilo.backup

import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Event
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The file a backup writes and an import reads. Everything stays on the phone unless you move it. */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val baby: Baby? = null,
    val events: List<Event>,
) {
    companion object {
        const val FORMAT = "cochilo-backup"
        /**
         * 2: entries may be MEDICINE (0.5.0). Version-1 files still read: they have no MEDICINE.
         * A version-2 file in an older app is refused as "made by a newer Cochilo".
         * 3: the baby has a look (the avatar, 0.13.0). Version-1 and -2 files still read: no look
         * means the default one.
         */
        const val VERSION = 3
    }
}

data class MergeResult(val merged: List<Event>, val added: Int, val updated: Int, val skipped: Int)

object Backup {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun toJson(baby: Baby?, events: List<Event>, now: Long): String =
        json.encodeToString(BackupFile.serializer(), BackupFile(exportedAt = now, baby = baby, events = events.sortedBy { it.start }))

    /** Throws IllegalArgumentException with a plain reason when the file isn't a Cochilo backup. */
    fun fromJson(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("not a Cochilo backup", e)
        }
        require(file.format == BackupFile.FORMAT) { "not a Cochilo backup" }
        require(file.version <= BackupFile.VERSION) { "made by a newer Cochilo" }
        return file
    }

    /**
     * Import never deletes. A new id is added; a known id is replaced only when the imported copy
     * was edited more recently, so importing the same file twice changes nothing.
     */
    fun merge(existing: List<Event>, incoming: List<Event>): MergeResult {
        val byId = existing.associateBy { it.id }.toMutableMap()
        var added = 0
        var updated = 0
        var skipped = 0
        for (e in incoming) {
            val old = byId[e.id]
            when {
                old == null -> { byId[e.id] = e; added++ }
                e.updatedAt > old.updatedAt -> { byId[e.id] = e; updated++ }
                else -> skipped++
            }
        }
        return MergeResult(byId.values.sortedBy { it.start }, added, updated, skipped)
    }

    /** A spreadsheet-friendly copy (export only: JSON is the format that round-trips). */
    fun toCsv(events: List<Event>, zone: ZoneId): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone)
        fun t(ms: Long?) = ms?.let { fmt.format(Instant.ofEpochMilli(it)) } ?: ""
        fun q(s: String?) = if (s == null) "" else "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("id,type,kind,start,end,duration_min,amount_ml,expressed_at,note\n")
        for (e in events.sortedBy { it.start }) {
            val dur = e.end?.let { ((it - e.start) / 60_000).toString() } ?: ""
            sb.append(listOf(e.id, e.type.name, e.kind ?: "", t(e.start), t(e.end), dur,
                e.amountMl?.toString() ?: "", t(e.expressedAt), q(e.note)).joinToString(",")).append('\n')
        }
        return sb.toString()
    }
}
