package br.gabriel.cochilo.backup

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime

private typealias Row = List<String>

/** The byte-order mark some apps put at the start of a CSV. */
private const val BOM = '\uFEFF'

/** What a shared or opened file turned out to be. */
enum class FileKind { COCHILO_BACKUP, NAPPER_LOGS, NAPPER_MEASUREMENTS, UNKNOWN }

/** The result of reading a Napper "logs" export, before anything is saved. */
data class NapperResult(
    val events: List<Event>,
    /** Napper rows that had no place in Cochilo (unknown categories, a bedtime with no wake-up). */
    val skipped: Int,
)

/**
 * Reads the CSV that Napper's share button exports ("<name>-logs-<from>-<to>.csv").
 *
 * Napper doesn't store a night as one sleep: it logs BED_TIME and WOKE_UP as instants and each
 * NIGHT_WAKING as an awake interval. Cochilo rebuilds the night as the sleep between them. Naps and
 * nights are labelled explicitly (a nap at 06:54 is still a nap), and every event gets an id derived
 * from its Napper row, so importing the same file again changes nothing.
 */
object NapperImport {
    private val LOG_HEADER = listOf("start", "end", "category")
    private val MEASUREMENT_HEADER = listOf("dateOfMeasurement", "weightKg")

    /** How the baby fell asleep, in the user's language (Napper's howBabySlept codes). */
    interface Labels {
        fun fellAsleep(code: String): String?
        fun pauses(count: Int, minutes: Int?): String
    }

    fun detect(text: String): FileKind {
        val trimmed = text.trimStart(BOM, ' ', '\n', '\r', '\t')
        if (trimmed.startsWith("{")) return if (trimmed.contains("\"cochilo-backup\"")) FileKind.COCHILO_BACKUP else FileKind.UNKNOWN
        val header = parseCsv(trimmed.lineSequence().firstOrNull() ?: return FileKind.UNKNOWN).firstOrNull() ?: return FileKind.UNKNOWN
        return when {
            header.take(3) == LOG_HEADER -> FileKind.NAPPER_LOGS
            header.take(2) == MEASUREMENT_HEADER -> FileKind.NAPPER_MEASUREMENTS
            else -> FileKind.UNKNOWN
        }
    }

    fun parseLogs(text: String, labels: Labels): NapperResult {
        val rows = parseCsv(text.trimStart(BOM))
        if (rows.isEmpty()) return NapperResult(emptyList(), 0)
        val header = rows.first()
        val idx = header.withIndex().associate { (i, h) -> h to i }
        fun Row.get(col: String): String = idx[col]?.let { getOrNull(it) }?.trim().orEmpty()

        data class R(val row: Row, val start: Long, val end: Long, val category: String, val updatedAt: Long)
        val parsed = rows.drop(1).mapNotNull { row ->
            val start = time(row.get("start")) ?: return@mapNotNull null
            val end = time(row.get("end")) ?: start
            R(row, start, end, row.get("category"), time(row.get("updatedAt")) ?: start)
        }.sortedBy { it.start }

        val out = mutableListOf<Event>()
        var skipped = 0
        fun note(r: R, vararg extra: String?): String? =
            (extra.toList() + r.row.get("comment")).mapNotNull { it?.trim()?.ifEmpty { null } }.joinToString(" · ").ifEmpty { null }
        fun id(vararg parts: Any) = "napper-" + sha(parts.joinToString("|")).take(24)

        // Nights: BED_TIME … NIGHT_WAKING* … WOKE_UP becomes the sleep between the wakings.
        val bedtimes = parsed.filter { it.category == "BED_TIME" }
        val wakeUps = parsed.filter { it.category == "WOKE_UP" }
        val wakings = parsed.filter { it.category == "NIGHT_WAKING" }
        val usedWakeUps = mutableSetOf<R>()
        for (bed in bedtimes) {
            val wake = wakeUps.firstOrNull { it.start > bed.start && it.start - bed.start <= 18 * HOUR && it !in usedWakeUps }
            if (wake == null) { skipped++; continue }
            usedWakeUps += wake
            val awake = wakings.filter { it.start >= bed.start && it.start < wake.start }.sortedBy { it.start }
            var from = bed.start
            val edited = maxOf(bed.updatedAt, wake.updatedAt, awake.maxOfOrNull { it.updatedAt } ?: 0)
            for (w in awake + null) {
                val to = w?.start ?: wake.start
                if (to - from >= MINUTE) {
                    out += Event(
                        id = id("night", from, to), type = EventType.SLEEP, start = from, end = to,
                        kind = Kinds.NIGHT_SLEEP, updatedAt = edited,
                        note = if (from == bed.start) note(bed) else null,
                    )
                }
                if (w != null) from = maxOf(from, w.end)
            }
        }
        skipped += wakeUps.count { it !in usedWakeUps }

        for (r in parsed) {
            val e: Event? = when (r.category) {
                "NAP" -> {
                    val pauses = r.row.get("numberOfPauses").toIntOrNull()?.takeIf { it > 0 }
                    Event(
                        id = id("nap", r.start, r.end), type = EventType.SLEEP, start = r.start, end = r.end,
                        kind = Kinds.NAP_SLEEP, updatedAt = r.updatedAt,
                        note = note(r, r.row.get("howBabySlept").takeIf { it.isNotEmpty() }?.let(labels::fellAsleep),
                            pauses?.let { labels.pauses(it, r.row.get("pausedMinutes").toIntOrNull()) }),
                    )
                }
                "NURSING" -> {
                    val left = r.row.get("breastLeftSeconds").toIntOrNull() ?: 0
                    val right = r.row.get("breastRightSeconds").toIntOrNull() ?: 0
                    val side = when {
                        left > 0 && right > 0 -> Kinds.BREAST_BOTH
                        right > 0 -> Kinds.BREAST_RIGHT
                        else -> Kinds.BREAST_LEFT
                    }
                    Event(id = id("feed", r.start, r.end, side), type = EventType.FEED, start = r.start,
                        end = maxOf(r.end, r.start), kind = side, updatedAt = r.updatedAt, note = note(r))
                }
                "BOTTLE" -> Event(
                    id = id("bottle", r.start), type = EventType.FEED, start = r.start, end = maxOf(r.end, r.start),
                    kind = Kinds.BOTTLE, amountMl = millilitres(r.row.get("feedingAmount"), r.row.get("units")),
                    updatedAt = r.updatedAt, note = note(r),
                )
                "CHANGED_DIAPER" -> Event(
                    id = id("diaper", r.start), type = EventType.DIAPER, start = r.start,
                    kind = when (r.row.get("diaperContent")) { "WET" -> Kinds.WET; "DIRTY", "POO" -> Kinds.DIRTY; "MIXED" -> Kinds.BOTH; else -> null },
                    updatedAt = r.updatedAt, note = note(r),
                )
                "PUMPING" -> Event(
                    id = id("pump", r.start), type = EventType.PUMP, start = r.start, end = maxOf(r.end, r.start),
                    amountMl = listOf(r.row.get("amountPumpedLeft"), r.row.get("amountPumpedRight"))
                        .mapNotNull { millilitres(it, r.row.get("units")) }.takeIf { it.isNotEmpty() }?.sum(),
                    expressedAt = r.start, updatedAt = r.updatedAt, note = note(r),
                )
                "MEDICINE" -> Event(id = id("medicine", r.start), type = EventType.MEDICINE, start = r.start,
                    updatedAt = r.updatedAt, note = note(r))
                "BED_TIME", "WOKE_UP", "NIGHT_WAKING" -> null // folded into night sleeps above
                else -> { skipped++; null }
            }
            if (e != null) out += e
        }
        return NapperResult(out.sortedBy { it.start }, skipped)
    }

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE

    private fun time(s: String): Long? = if (s.isEmpty()) null else runCatching {
        OffsetDateTime.parse(s).toInstant().toEpochMilli()
    }.recoverCatching { Instant.parse(s).toEpochMilli() }.getOrNull()

    private fun millilitres(amount: String, units: String): Int? {
        val v = amount.replace(',', '.').toDoubleOrNull() ?: return null
        return (if (units.equals("oz", ignoreCase = true) || units.contains("ounce", ignoreCase = true)) v * 29.5735 else v).toInt()
    }

    private fun sha(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }


    /** RFC 4180-ish: commas, quoted fields with "" escapes and line breaks inside quotes. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { row += field.toString(); field.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += field.toString(); field.clear()
                    if (row.any { it.isNotEmpty() }) rows += row
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            if (row.any { it.isNotEmpty() }) rows += row
        }
        return rows
    }
}
