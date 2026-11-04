package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.ZoneId
import kotlin.math.abs

/**
 * The baby's clock: process C of the two-process model (0.54.0, docs/prediction.md; research
 * §2.1, §2.2, §7.2). Pure, tested in ClockTest.
 *
 * Newborns have almost no day/night rhythm; it forms between about 6 and 18 weeks (cortisol
 * first, then melatonin with consolidated night sleep). Once it does, some sleeps come at a time
 * of day, not only after a stretch awake. So the clock is read from the baby's own log:
 * - **when**: the usual first nap of the day and the usual bedtime, as minutes of the day
 *   (recency-weighted medians over 14 days);
 * - **how strong**: whether the time of day is steadier than the time awake before it (a first
 *   nap at 9:00 whenever the baby woke, rather than two hours after waking), how steady it is,
 *   and how far the rhythm has had time to form at this age (none before 6 weeks, all from 16).
 *
 * The predictor leans the first nap and bedtime towards those times by that strength, capped.
 * A baby whose first nap simply follows waking has a scattered first-nap time, so a weak clock,
 * and barely moves.
 */
object Clock {
    data class Fit(
        /** The usual first nap's start, minutes after midnight; null with too few mornings. */
        val firstNapMinute: Int?,
        /** How strongly the first nap keeps to its time, 0..1. */
        val firstNapStrength: Double,
        /** The usual bedtime, minutes after midnight (may be past 24 h for after midnight); null with too few nights. */
        val bedtimeMinute: Int?,
        /** How strongly bedtime keeps to its time, 0..1. */
        val bedtimeStrength: Double,
        /** How far the rhythm has formed at this age, 0..1 (6 → 16 weeks). */
        val ageShare: Double,
        val days: Int,
    )

    /** Mornings or nights needed before a time counts. */
    const val MIN_DAYS = 5
    /** A spread (median absolute deviation) this wide or wider means no clock at all. */
    const val SPREAD_NONE = 45.0
    /** The clock never takes more than this share of the decision: the window still counts. */
    const val MAX_STRENGTH = 0.7

    fun ageShare(ageWeeks: Int): Double = ((ageWeeks - 6) / 10.0).coerceIn(0.0, 1.0)

    fun fit(events: List<Event>, ageWeeks: Int, now: Long, zone: ZoneId, lookbackDays: Int = 14): Fit {
        val sleeps = events.filter { it.type == EventType.SLEEP && it.end != null && it.start >= now - lookbackDays * DAY && it.start < now }
            .sortedBy { it.start }
        val firstNaps = mutableListOf<Pair<Long, Double>>()
        val bedtimes = mutableListOf<Pair<Long, Double>>()
        // The time awake before each: the first window, and the last one before bed.
        val firstWindows = mutableListOf<Pair<Long, Double>>()
        val lastWindows = mutableListOf<Pair<Long, Double>>()
        for (i in sleeps.indices) {
            val s = sleeps[i]
            val prev = sleeps.getOrNull(i - 1)
            val night = s.isNightSleep(zone)
            // The first nap: a nap right after a night.
            if (!night && prev != null && prev.isNightSleep(zone)) {
                firstNaps += s.start to minuteOfDay(s.start, zone).toDouble()
                firstWindows += s.start to (s.start - prev.end!!).toDouble() / MINUTE
            }
            // Bedtime: a night after a nap (not a waking inside the night).
            if (night && (prev == null || !prev.isNightSleep(zone))) {
                var m = minuteOfDay(s.start, zone).toDouble()
                if (m < 12 * 60) m += 24 * 60
                bedtimes += s.start to m
                if (prev != null) lastWindows += s.start to (s.start - prev.end!!).toDouble() / MINUTE
            }
        }
        val age = ageShare(ageWeeks)
        fun spread(xs: List<Pair<Long, Double>>): Pair<Double, Double>? {
            if (xs.size < MIN_DAYS) return null
            val w = xs.map { recencyWeight(it.first, now) }
            val med = weightedMedian(xs.map { it.second }, w) ?: return null
            return med to (weightedMad(xs.map { it.second }, w, med) ?: SPREAD_NONE)
        }
        fun time(clock: List<Pair<Long, Double>>, windows: List<Pair<Long, Double>>): Pair<Int?, Double> {
            val (med, sc) = spread(clock) ?: return null to 0.0
            // The clock counts only as far as the time of day is steadier than the time awake.
            val sw = spread(windows)?.second ?: return med.toInt() to 0.0
            val clockLed = if (sw <= 0) 0.0 else ((sw - sc) / sw).coerceIn(0.0, 1.0)
            val steady = (1 - sc / SPREAD_NONE).coerceIn(0.0, 1.0)
            return med.toInt() to (age * clockLed * steady * MAX_STRENGTH)
        }
        val (nap, napS) = time(firstNaps, firstWindows)
        val (bed, bedS) = time(bedtimes, lastWindows)
        return Fit(nap, napS, bed, bedS, age, maxOf(firstNaps.size, bedtimes.size))
    }

    /**
     * Leans [byWindow] (the window's guess) towards the clock's [clockAt] by [strength], moving it
     * at most [maxShift]. Returns the new time.
     */
    fun lean(byWindow: Long, clockAt: Long, strength: Double, maxShift: Long): Long {
        if (strength <= 0.0) return byWindow
        val shift = ((clockAt - byWindow) * strength).toLong().coerceIn(-maxShift, maxShift)
        return byWindow + shift
    }

    /** Shifts under this are noise: not applied, not shown. */
    const val MIN_SHIFT_MINUTES = 5

    fun meaningful(shiftMs: Long) = abs(shiftMs) >= MIN_SHIFT_MINUTES * MINUTE
}
