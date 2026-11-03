package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.LocalDate
import java.time.ZoneId

/**
 * The night as one thing (docs/nights.md, 0.42.0). A night is the baby's sleeps from bedtime until
 * up for the day; the gaps between them are night wakings. Cochilo used to read nights off the
 * clock (19:00–07:00), which miscounted an evening catnap as bedtime and an early first nap as
 * night. Now each sleep says which it is: Start sleep guesses (one tap stays one tap), one tap
 * changes it, and Up for the day ends the night. Pure Kotlin, tested in NightsTest.
 */
object Nights {
    /** A night with no sleep for this long since its last one is over, even without Up for the day. */
    const val OPEN_HOURS = 4
    /** Without a learned bedtime, a sleep that starts from 18:30 is guessed to be the night. */
    const val EVENING_MINUTE = 18 * 60 + 30
    /** Up to 05:00 a sleep is the night; after it, a sleep is the night only while one is open. */
    const val EARLY_MINUTE = 5 * 60
    /** A sleep this close before the learned bedtime is the night (a bedtime a little early). */
    const val BEFORE_BEDTIME_MIN = 75
    /** The evening starts here: before 16:00 a sleep is never guessed to be the night. */
    const val AFTERNOON_MINUTE = 16 * 60

    /**
     * The night the baby is awake in, if any: its last sleep has ended, it isn't up for the day, and
     * it ended less than [OPEN_HOURS] ago. That's a night waking: the next sleep continues the night.
     */
    fun openNight(log: List<Event>, now: Long): Event? {
        val last = log.filter { it.type == EventType.SLEEP }.maxByOrNull { it.start } ?: return null
        val end = last.end ?: return null
        if (last.night != true || last.upForDay == true) return null
        return last.takeIf { now - end in 0 until OPEN_HOURS * HOUR }
    }

    /**
     * Whether a sleep starting at [start] is the night: during an open night, yes (back to sleep);
     * otherwise by the baby's own bedtime once it's learned ([bedtimeMinute], minutes after
     * midnight), else from [EVENING_MINUTE]; and in the small hours, up to [EARLY_MINUTE]. The
     * parent changes it with one tap if it's wrong.
     */
    fun guess(log: List<Event>, start: Long, zone: ZoneId, bedtimeMinute: Int?): Boolean {
        if (openNight(log, start) != null) return true
        val m = minuteOfDay(start, zone)
        if (m < EARLY_MINUTE) return true
        if (m < AFTERNOON_MINUTE) return false
        val from = bedtimeMinute?.let { (it - BEFORE_BEDTIME_MIN).coerceAtLeast(AFTERNOON_MINUTE) } ?: EVENING_MINUTE
        return m >= from
    }

    /**
     * The night that began on [date]'s evening: its sleeps in order, from the first night sleep that
     * starts between noon on [date] and noon the next day, up to the one marked up for the day (or
     * the last before a nap or an [OPEN_HOURS] gap).
     */
    fun of(sleeps: List<Event>, date: LocalDate, zone: ZoneId): List<Event> {
        val from = startOfDay(date, zone) + 12 * HOUR
        val to = startOfDay(date.plusDays(1), zone) + 12 * HOUR
        val sorted = sleeps.filter { it.type == EventType.SLEEP && it.start in from until to }.sortedBy { it.start }
        val first = sorted.indexOfFirst { it.isNightSleep(zone) }
        if (first < 0) return emptyList()
        val out = mutableListOf(sorted[first])
        for (s in sorted.drop(first + 1)) {
            val prev = out.last()
            if (prev.upForDay == true || !s.isNightSleep(zone)) break
            val prevEnd = prev.end ?: break
            if (s.start - prevEnd >= OPEN_HOURS * HOUR) break
            out += s
        }
        return out
    }

    /** The night [e] belongs to, in order: back from [e] while the sleep before is the same night. */
    fun containing(sleeps: List<Event>, e: Event, zone: ZoneId): List<Event> {
        if (!e.isNightSleep(zone)) return emptyList()
        val sorted = sleeps.filter { it.type == EventType.SLEEP && it.start <= e.start }.sortedBy { it.start }
        val out = ArrayDeque<Event>()
        for (s in sorted.asReversed()) {
            if (out.isEmpty()) { if (s.id == e.id) out.addFirst(s); continue }
            val next = out.first()
            val end = s.end ?: break
            if (!s.isNightSleep(zone) || s.upForDay == true || next.start - end >= OPEN_HOURS * HOUR) break
            out.addFirst(s)
        }
        return out.toList()
    }

    /** Night wakings in [night]: the gaps between its sleeps, in minutes. */
    fun wakings(night: List<Event>): List<Int> =
        night.zipWithNext { a, b -> (((b.start - (a.end ?: b.start)) / MINUTE).toInt()).coerceAtLeast(0) }
}
