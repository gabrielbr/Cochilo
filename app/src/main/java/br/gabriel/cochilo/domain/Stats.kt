package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.LocalDate
import java.time.ZoneId

/** One calendar day of the log. Sleep that crosses midnight is split between the two days. */
data class DayStats(
    val date: LocalDate,
    val totalSleepMin: Int,
    val napSleepMin: Int,
    val nightSleepMin: Int,
    val naps: Int,
    /** Longest single sleep that started this day. */
    val longestStretchMin: Int,
    /** Times awake between night sleeps in the night that started this evening. */
    val nightWakings: Int,
    val feeds: Int,
    val diapers: Int,
    val pumpedMl: Int,
    val daylightMin: Int,
    /** Daylight between 12:00 and 16:00, the window Harrison (2004) linked to better night sleep. */
    val afternoonDaylightMin: Int,
)

data class Averages(
    val days: Int,
    val totalSleepMin: Int,
    val napSleepMin: Int,
    val nightSleepMin: Int,
    val naps: Double,
    val longestStretchMin: Int,
    val nightWakings: Double,
    val feeds: Double,
    val diapers: Double,
    val daylightMin: Int,
)

class Stats(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun day(events: List<Event>, date: LocalDate, now: Long): DayStats {
        val from = startOfDay(date, zone)
        val to = startOfDay(date.plusDays(1), zone)
        fun overlap(e: Event): Long {
            val end = e.end ?: now
            return (minOf(end, to) - maxOf(e.start, from)).coerceAtLeast(0)
        }
        val sleeps = events.filter { it.type == EventType.SLEEP }
        var nap = 0L
        var night = 0L
        for (s in sleeps) {
            val o = overlap(s)
            if (o == 0L) continue
            if (s.isNightSleep(zone)) night += o else nap += o
        }
        val startedToday = sleeps.filter { it.start in from until to }
        val longest = startedToday.maxOfOrNull { it.durationMs(now) } ?: 0L

        val daylight = events.filter { it.type == EventType.DAYLIGHT }
        val noon = from + 12 * HOUR
        val four = from + 16 * HOUR
        val afternoon = daylight.sumOf { e -> (minOf(e.end ?: now, four) - maxOf(e.start, noon)).coerceAtLeast(0) }

        val inDay = { e: Event -> e.start in from until to }
        return DayStats(
            date = date,
            totalSleepMin = ((nap + night) / MINUTE).toInt(),
            napSleepMin = (nap / MINUTE).toInt(),
            nightSleepMin = (night / MINUTE).toInt(),
            naps = startedToday.count { it.isNap(zone) },
            longestStretchMin = (longest / MINUTE).toInt(),
            nightWakings = nightWakings(sleeps, date),
            feeds = events.count { it.type == EventType.FEED && inDay(it) },
            diapers = events.count { it.type == EventType.DIAPER && inDay(it) },
            pumpedMl = events.filter { it.type == EventType.PUMP && inDay(it) }.sumOf { it.amountMl ?: 0 },
            daylightMin = (daylight.sumOf { overlap(it) } / MINUTE).toInt(),
            afternoonDaylightMin = (afternoon / MINUTE).toInt(),
        )
    }

    /** Wakings in the night that starts at 19:00 on [date] and ends 07:00 the next day. */
    fun nightWakings(sleeps: List<Event>, date: LocalDate): Int {
        val from = startOfDay(date, zone) + NIGHT_START_HOUR * HOUR
        val to = startOfDay(date.plusDays(1), zone) + NIGHT_END_HOUR * HOUR
        val night = sleeps.filter { it.start in from until to && it.isNightSleep(zone) }.sortedBy { it.start }
        return (night.size - 1).coerceAtLeast(0)
    }

    fun days(events: List<Event>, endDate: LocalDate, count: Int, now: Long): List<DayStats> =
        (count - 1 downTo 0).map { day(events, endDate.minusDays(it.toLong()), now) }

    /** Averages over the days that have any sleep logged, so an empty day doesn't drag them down. */
    fun averages(days: List<DayStats>): Averages? {
        val d = days.filter { it.totalSleepMin > 0 }
        if (d.isEmpty()) return null
        return Averages(
            days = d.size,
            totalSleepMin = d.map { it.totalSleepMin }.average().toInt(),
            napSleepMin = d.map { it.napSleepMin }.average().toInt(),
            nightSleepMin = d.map { it.nightSleepMin }.average().toInt(),
            naps = d.map { it.naps }.average(),
            longestStretchMin = d.map { it.longestStretchMin }.average().toInt(),
            nightWakings = d.map { it.nightWakings }.average(),
            feeds = d.map { it.feeds }.average(),
            diapers = d.map { it.diapers }.average(),
            daylightMin = d.map { it.daylightMin }.average().toInt(),
        )
    }
}
