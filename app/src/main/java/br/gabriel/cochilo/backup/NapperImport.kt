package br.gabriel.cochilo.backup

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import java.time.OffsetDateTime
import java.util.UUID

/** Words the import writes into notes, in the app's language. */
data class NapperLabels(
    val heldOrWorn: String,
    val swing: String,
    val inBed: String,
    val nursing: String,
    val breastMilk: String,
    val formula: String,
    val medicine: String,
    /** "Left 5 min, right 9 min", from minutes on each breast. */
    val sides: (left: Int, right: Int) -> String,
)

data class NapperResult(val events: List<Event>, val skipped: Int)

/**
 * Reads Napper's "logs" CSV export (Settings › Export data in Napper). Napper keeps a night as a
 * BED_TIME and a WOKE_UP marker with NIGHT_WAKING spans between; Cochilo keeps sleeps, so each
 * bedtime is paired with the next wake-up and split around the wakings. Ids are derived from
 * Napper's own rows, so importing the same file twice changes nothing (Backup.merge).
 */
object NapperImport {
    private const val MAX_NIGHT_MS = 16 * 3_600_000L

    fun looksLikeNapper(text: String): Boolean =
        text.trimStart('\uFEFF').lineSequence().firstOrNull()?.let { h ->
            h.startsWith("start,end,category,") && "createdAt" in h
        } == true

    fun parse(text: String, labels: NapperLabels): NapperResult {
        val rows = Csv.parse(text.trimStart('\uFEFF'))
        require(rows.isNotEmpty()) { "empty file" }
        val header = rows.first()
        require(header.take(3) == listOf("start", "end", "category")) { "not a Napper export" }
        val records = rows.drop(1).filter { r -> r.any { it.isNotBlank() } }.map { r -> header.indices.associate { header[it] to r.getOrElse(it) { "" }.trim() } }

        val out = mutableListOf<Event>()
        var skipped = 0
        val markers = mutableListOf<Row>()
        val wakings = mutableListOf<Row>()

        for (m in records) {
            val row = Row.of(m) ?: run { skipped++; null } ?: continue
            when (row.category) {
                "NAP" -> out += sleep(row, row.start, row.end, "nap", noteOf(howSlept(m, labels), m["comment"]))
                "BED_TIME", "WOKE_UP" -> markers += row
                "NIGHT_WAKING" -> wakings += row
                "NURSING" -> out += nursing(row, m, labels)
                "BOTTLE" -> out += event(row, EventType.FEED, row.start, row.end, Kinds.BOTTLE,
                    amount = m["feedingAmount"]?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 },
                    note = noteOf(bottleType(m["bottleFeedingType"], labels), m["comment"]))
                "PUMPING" -> {
                    val ml = listOfNotNull(m["amountPumpedLeft"]?.toDoubleOrNull(), m["amountPumpedRight"]?.toDoubleOrNull()).sum().toInt()
                    out += event(row, EventType.PUMP, row.start, row.end, null, amount = ml.takeIf { it > 0 }, note = m["comment"], expressed = row.start)
                }
                "CHANGED_DIAPER" -> out += event(row, EventType.DIAPER, row.start, null, diaper(m["diaperContent"]), note = m["comment"])
                "MEDICINE" -> out += event(row, EventType.NOTE, row.start, null, null, note = noteOf(labels.medicine, m["comment"]))
                else -> {
                    // Solids, temperature, growth…: Cochilo has no place for them; keep any words.
                    val c = m["comment"]
                    if (!c.isNullOrBlank()) out += event(row, EventType.NOTE, row.start, null, null, note = c) else skipped++
                }
            }
        }

        // Nights: each BED_TIME with the next WOKE_UP (no other bedtime between, within 16 h).
        val sorted = markers.sortedBy { it.start }
        val usedWakings = HashSet<Row>()
        for ((i, bed) in sorted.withIndex()) {
            if (bed.category != "BED_TIME") continue
            val wake = sorted.getOrNull(i + 1)?.takeIf { it.category == "WOKE_UP" && it.start - bed.start in 1..MAX_NIGHT_MS }
            if (wake == null) { skipped++; continue }
            var cursor = bed.start
            var part = 0
            val inside = wakings.filter { it.start >= bed.start && it.start < wake.start }.sortedBy { it.start }
            for (w in inside) {
                usedWakings += w
                if (w.start > cursor) out += sleep(bed, cursor, w.start, "night-${part++}", if (part == 1) noteOf(howSlept(bed.raw, labels), bed.raw["comment"]) else null)
                cursor = maxOf(cursor, w.end)
            }
            if (wake.start > cursor) out += sleep(bed, cursor, wake.start, "night-${part++}", if (part == 1) noteOf(howSlept(bed.raw, labels), bed.raw["comment"]) else null)
        }
        // A waking's own words (e.g. "voltou a dormir no colo") are kept as a note at its time.
        for (w in wakings) {
            val c = w.raw["comment"]
            if (!c.isNullOrBlank()) out += event(w, EventType.NOTE, w.start, null, null, note = c)
            else if (w !in usedWakings) skipped++
        }
        return NapperResult(out.sortedBy { it.start }, skipped)
    }

    private class Row(val category: String, val start: Long, val end: Long, val key: String, val updatedAt: Long, val raw: Map<String, String>) {
        companion object {
            fun of(m: Map<String, String>): Row? {
                val start = time(m["start"]) ?: return null
                val end = time(m["end"])?.coerceAtLeast(start) ?: start
                val cat = m["category"]?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
                val key = listOf(cat, m["start"], m["createdAt"]).joinToString("|")
                return Row(cat, start, end, key, time(m["updatedAt"]) ?: time(m["createdAt"]) ?: start, m)
            }
        }
    }

    private fun time(s: String?): Long? = s?.takeIf { it.isNotBlank() }?.let {
        runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    }

    private fun id(row: Row, part: String) = "napper-" + UUID.nameUUIDFromBytes("${row.key}|$part".toByteArray())

    private fun event(
        row: Row, type: EventType, start: Long, end: Long?, kind: String?,
        amount: Int? = null, note: String? = null, expressed: Long? = null, part: String = "",
    ) = Event(
        id = id(row, part), type = type, start = start,
        // A duration type always gets an end, or it would show as a running timer.
        end = if (type.hasDuration) (end ?: start) else null,
        kind = kind, amountMl = amount, expressedAt = expressed,
        note = note?.trim()?.ifEmpty { null }, updatedAt = row.updatedAt,
    )

    private fun sleep(row: Row, start: Long, end: Long, part: String, note: String?) =
        event(row, EventType.SLEEP, start, end, null, note = note, part = part)

    private fun nursing(row: Row, m: Map<String, String>, labels: NapperLabels): Event {
        val left = m["breastLeftSeconds"]?.toDoubleOrNull()?.toInt() ?: 0
        val right = m["breastRightSeconds"]?.toDoubleOrNull()?.toInt() ?: 0
        val kind = when {
            left == 0 && right == 0 -> null
            left >= right -> Kinds.BREAST_LEFT
            else -> Kinds.BREAST_RIGHT
        }
        val sides = if (left > 0 && right > 0) labels.sides(minutes(left), minutes(right)) else null
        return event(row, EventType.FEED, row.start, row.end, kind, note = noteOf(sides, m["comment"]))
    }

    private fun minutes(seconds: Int) = (seconds + 30) / 60

    private fun diaper(content: String?) = when (content?.uppercase()) {
        "WET", "PEE" -> Kinds.WET
        "DIRTY", "POO", "POOP", "SOLID" -> Kinds.DIRTY
        "MIXED", "BOTH" -> Kinds.BOTH
        else -> null
    }

    private fun howSlept(m: Map<String, String>, labels: NapperLabels) = when (m["howBabySlept"]?.uppercase()) {
        "WORN_OR_HELD" -> labels.heldOrWorn
        "SWING" -> labels.swing
        "IN_BED" -> labels.inBed
        "NURSING" -> labels.nursing
        else -> null
    }

    private fun bottleType(t: String?, labels: NapperLabels) = when (t?.uppercase()) {
        "BREAST_MILK" -> labels.breastMilk
        "FORMULA" -> labels.formula
        else -> null
    }

    private fun noteOf(vararg parts: String?) =
        parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(" · ").ifEmpty { null }
}

/** RFC 4180 CSV: quoted fields may hold commas, quotes ("") and line breaks. */
object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (quoted) {
                when {
                    ch == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                    ch == '"' -> quoted = false
                    else -> field.append(ch)
                }
            } else when (ch) {
                '"' -> quoted = true
                ',' -> { row += field.toString(); field.clear() }
                '\r' -> Unit
                '\n' -> { row += field.toString(); field.clear(); rows += row; row = mutableListOf() }
                else -> field.append(ch)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows
    }
}
