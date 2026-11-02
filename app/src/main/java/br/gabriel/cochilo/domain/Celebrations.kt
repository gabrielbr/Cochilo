package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Celebrations (docs/celebrations.md): a small "parabéns" when what the parent was trying for
 * happens. Pure rules over the log, so each is unit-tested like the predictor. They celebrate,
 * never scold: a day without one is just a day, and nothing here ever predicts how the baby will
 * sleep.
 */
enum class Moment {
    /** A nap that lasted at least as long as expected. */
    FULL_NAP,

    /** A sleep that began inside the predicted window. */
    ON_TIME,

    /** The longest night sleep of the last week. */
    LONGEST_NIGHT,

    /** At least half an hour of daylight between 12:00 and 16:00 today. */
    DAYLIGHT,

    /** A bedtime routine logged before the night's sleep. */
    ROUTINE_BEFORE_BED,

    /** Something logged on each of the last seven days. */
    WEEK_LOGGED,
}

/** What just happened, from the parent's tap. */
sealed interface Trigger {
    /** A sleep just ended ([sleep] has its end). [before] is the prediction from before the tap. */
    data class WokeUp(val sleep: Event, val before: Predictor.Result) : Trigger

    /** A sleep just began. */
    data class StartedSleep(val sleep: Event, val before: Predictor.Result) : Trigger

    /** Anything else was logged, finished or saved. */
    data class Logged(val event: Event) : Trigger
}

/** A celebration to show. [quiet]: at night, words only, dim and still. */
data class Celebration(val moment: Moment, val quiet: Boolean)

object Celebrations {
    const val MAX_PER_DAY = 3
    private const val FULL_NAP_MIN = 30 * MINUTE
    /** A nap counts as full from five minutes before its expected end. */
    private const val FULL_NAP_GRACE = 5 * MINUTE
    private const val DAYLIGHT_GOAL = 30 * MINUTE
    private const val ROUTINE_LEAD = 90 * MINUTE
    private const val NIGHTS_TO_COMPARE = 3

    /**
     * The celebration for [trigger], or null. [log] is the log after the tap. [shown] is when each
     * moment was last shown, so each shows at most once a day (the week once a week) and at most
     * [MAX_PER_DAY] a day in all.
     */
    fun after(trigger: Trigger, log: List<Event>, zone: ZoneId, now: Long, shown: Map<Moment, LocalDate>): Celebration? {
        val today = day(now, zone)
        if (shown.values.count { it == today } >= MAX_PER_DAY) return null
        val candidates = when (trigger) {
            is Trigger.WokeUp -> listOfNotNull(fullNap(trigger, log, zone, now), longestNight(trigger, log, zone))
            is Trigger.StartedSleep -> listOfNotNull(routineBeforeBed(trigger, log, zone), onTime(trigger))
            is Trigger.Logged -> listOfNotNull(daylight(trigger, log, zone, now))
        } + listOfNotNull(weekLogged(log, zone, now))
        val moment = candidates.firstOrNull { m ->
            val last = shown[m]
            if (m == Moment.WEEK_LOGGED) last == null || last.plusDays(7) <= today else last != today
        } ?: return null
        return Celebration(moment, quiet = isNightStart(now, zone))
    }

    private fun fullNap(t: Trigger.WokeUp, log: List<Event>, zone: ZoneId, now: Long): Moment? {
        val s = t.sleep
        val end = s.end ?: return null
        if (s.isNightSleep(zone) || end - s.start < FULL_NAP_MIN) return null
        val expected = expectedWake(log, s.start, isNight = false, zone, now) ?: return null
        return if (end >= expected - FULL_NAP_GRACE) Moment.FULL_NAP else null
    }

    private fun longestNight(t: Trigger.WokeUp, log: List<Event>, zone: ZoneId): Moment? {
        val s = t.sleep
        val end = s.end ?: return null
        if (!s.isNightSleep(zone)) return null
        val earlier = log.filter {
            it.id != s.id && it.type == EventType.SLEEP && it.end != null && it.isNightSleep(zone) &&
                it.start in s.start - 7 * DAY until s.start
        }
        val nights = earlier.map { day(it.start - NIGHT_END_HOUR * HOUR, zone) }.toSet()
        if (nights.size < NIGHTS_TO_COMPARE) return null
        val longest = earlier.maxOf { it.end!! - it.start }
        return if (end - s.start > longest) Moment.LONGEST_NIGHT else null
    }

    private fun onTime(t: Trigger.StartedSleep): Moment? {
        val p = t.before as? Predictor.Result.Awake ?: return null
        if (p.samples == 0) return null
        return if (t.sleep.start in p.windowStart..p.windowEnd) Moment.ON_TIME else null
    }

    private fun routineBeforeBed(t: Trigger.StartedSleep, log: List<Event>, zone: ZoneId): Moment? {
        if (!isNightStart(t.sleep.start, zone)) return null
        val routine = log.any {
            it.type == EventType.ROUTINE && it.kind == Kinds.BEDTIME_ROUTINE &&
                it.start in t.sleep.start - ROUTINE_LEAD..t.sleep.start
        }
        return if (routine) Moment.ROUTINE_BEFORE_BED else null
    }

    private fun daylight(t: Trigger.Logged, log: List<Event>, zone: ZoneId, now: Long): Moment? {
        if (t.event.type != EventType.DAYLIGHT || t.event.end == null) return null
        val start = startOfDay(day(now, zone), zone)
        val noon = start + 12 * HOUR
        val four = start + 16 * HOUR
        fun minutesIn(e: Event): Long {
            val from = maxOf(e.start, noon)
            val to = minOf(e.end ?: now, four)
            return (to - from).coerceAtLeast(0)
        }
        val total = log.filter { it.type == EventType.DAYLIGHT }.sumOf { minutesIn(it) }
        val before = total - minutesIn(t.event)
        // Only the tap that crosses the goal celebrates, not every walk after it.
        return if (total >= DAYLIGHT_GOAL && before < DAYLIGHT_GOAL) Moment.DAYLIGHT else null
    }

    private fun weekLogged(log: List<Event>, zone: ZoneId, now: Long): Moment? {
        val today = day(now, zone)
        val days = log.map { day(it.start, zone) }.toSet()
        return if ((0L..6L).all { today.minusDays(it) in days }) Moment.WEEK_LOGGED else null
    }

    private fun day(t: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
}
