package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * Predicts the next nap or bedtime from wake windows (docs/research.md §7.2, phase 1).
 *
 * For each slot of the day (1st window after the morning wake, 2nd, … and the last one before
 * bed) it takes the recency-weighted median of the baby's own recent windows, blended with the
 * age-based prior: `(n·personal + k·prior) / (n + k)`. With k = 5 the prior fades after about a
 * week of logging, which is how long commercial apps say they need too.
 *
 * Phase 2 (0.24.0, docs/prediction.md): the homeostatic part of the two-process model (§2.1). A
 * nap lowers sleep pressure by how long it lasts, so after a short nap the next window is shorter
 * and after a long one longer. How much, the predictor learns from this baby's own recent pairs
 * (the nap's length, the window after it) by weighted least squares, then shrinks it towards no
 * change like the windows themselves and caps it, and the card says what it did.
 */
class Predictor(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val lookbackDays: Int = 14,
    private val priorWeight: Double = 5.0,
    /** Whether the last nap's length moves the next window (phase 2); off only to compare in tests. */
    private val napEffect: Boolean = true,
) {
    enum class Next { NAP, BEDTIME }

    sealed interface Result {
        /** Nothing logged yet. */
        data object NeedData : Result

        data class Sleeping(val since: Long, val isNight: Boolean) : Result

        data class Awake(
            val since: Long,
            val next: Next,
            /** Best guess, epoch millis. */
            val at: Long,
            val windowStart: Long,
            val windowEnd: Long,
            /** Which window of the day this is: 0 = first after the morning wake. */
            val slot: Int,
            val wakeMinutes: Int,
            val personalMinutes: Int?,
            val priorMinutes: Int,
            val samples: Int,
            /** How the last nap's length moved this window, or null when it didn't (phase 2). */
            val napEffect: NapEffect? = null,
        ) : Result
    }

    /** The last nap lasted [lastNapMinutes] against a usual [usualNapMinutes], so the window moved [shiftMinutes] (− = sooner). */
    data class NapEffect(val lastNapMinutes: Int, val usualNapMinutes: Int, val shiftMinutes: Int)

    /** A daytime wake window seen in the log; [afterNapMinutes] is the nap before it (null after the night). */
    data class Window(val endedAt: Long, val minutes: Double, val slot: Int, val beforeBed: Boolean, val afterNapMinutes: Double? = null)

    fun windows(events: List<Event>, now: Long): List<Window> {
        val sleeps = events.filter { it.type == EventType.SLEEP && it.end != null && it.start >= now - lookbackDays * DAY }
            .sortedBy { it.start }
        val out = mutableListOf<Window>()
        var naps = 0 // naps finished since the morning wake
        for (i in 0 until sleeps.size - 1) {
            val a = sleeps[i]
            val b = sleeps[i + 1]
            val aNight = a.isNightSleep(zone)
            val bNight = b.isNightSleep(zone)
            if (aNight) naps = 0 else naps++
            // Night wakings (night → night) are not wake windows.
            if (aNight && bNight) continue
            val gap = (b.start - a.end!!).toDouble() / MINUTE
            if (gap < 10 || gap > 6 * 60) continue // a logging gap, not a real window
            val afterNap = if (aNight) null else (a.end - a.start).toDouble() / MINUTE
            out += Window(b.start, gap, slot = naps, beforeBed = bNight, afterNapMinutes = afterNap)
        }
        return out
    }

    /**
     * The next sleep. [maxNaps], used by the day plan (DayPlan, 0.49.0): once that many naps are
     * done today, the next sleep is bedtime, whatever the clock says.
     */
    fun predict(events: List<Event>, ageWeeks: Int, now: Long, maxNaps: Int? = null): Result {
        val sleeps = events.filter { it.type == EventType.SLEEP }.sortedBy { it.start }
        val running = sleeps.lastOrNull { it.end == null }
        if (running != null) return Result.Sleeping(running.start, running.isNightSleep(zone))
        val last = sleeps.lastOrNull { it.end != null } ?: return Result.NeedData
        val awakeSince = last.end!!

        val prior = Reference.priorFor(ageWeeks)
        val slot = if (last.isNightSleep(zone)) 0 else napsSinceMorningWake(sleeps, last)
        val all = windows(events, now)

        val bedtimeMinute = bedtimeMinute(sleeps, now) ?: prior.bedtimeMinute
        val nextNapMinutes = blended(all.filter { !it.beforeBed && it.slot == slot }.ifEmpty { all.filter { !it.beforeBed } },
            prior.wakeForSlot(slot, false), now)
        // Phase 2: a short last nap brings the next sleep sooner, a long one later.
        val effect = if (napEffect && !last.isNightSleep(zone)) napEffectFor(all, (last.end!! - last.start).toDouble() / MINUTE, now) else null
        val shift = (effect?.shiftMinutes ?: 0) * MINUTE
        var next = Next.NAP
        var chosen = nextNapMinutes
        var at = awakeSince + (nextNapMinutes.minutes * MINUTE).roundToLong() + shift

        if (bedtimeMinute != null && !last.isNightSleep(zone)) {
            val bedtimeAt = nextOccurrence(bedtimeMinute, awakeSince)
            // If the next nap would land within 45 minutes of bedtime, the next sleep is bedtime.
            if (at >= bedtimeAt - 45 * MINUTE || (maxNaps != null && slot >= maxNaps)) {
                val lastWindow = blended(all.filter { it.beforeBed }, prior.wakeForSlot(slot, true), now)
                val byWindow = awakeSince + (lastWindow.minutes * MINUTE).roundToLong() + shift
                // Near the usual bedtime, but never sooner than half the usual last window after
                // waking: a late nap that ends at bedtime still leaves the baby some time awake.
                val earliest = awakeSince + (lastWindow.minutes * MIN_LAST_WINDOW_SHARE * MINUTE).roundToLong()
                at = byWindow.coerceIn(bedtimeAt - 30 * MINUTE, bedtimeAt + 30 * MINUTE).coerceAtLeast(earliest)
                next = Next.BEDTIME
                chosen = lastWindow
            }
        }
        val half = (chosen.spreadMinutes.coerceIn(10.0, 30.0) * MINUTE).roundToLong()
        return Result.Awake(
            since = awakeSince, next = next, at = at,
            windowStart = at - half, windowEnd = at + half,
            slot = slot,
            wakeMinutes = ((at - awakeSince) / MINUTE).toInt(),
            personalMinutes = chosen.personal?.toInt(),
            priorMinutes = chosen.prior,
            samples = chosen.samples,
            napEffect = effect,
        )
    }

    /**
     * How much the last nap moves the next window: the slope of window length on the nap before
     * it, fitted over this baby's recent nap-to-nap pairs within each slot of the day
     * (recency-weighted least squares), times how far
     * [lastNap] is from the usual nap. The slope is kept between 0 and 1 (a longer nap never
     * shortens the window, and a minute of nap never buys more than a minute awake), shrunk by
     * n / (n + k) like the windows, and capped at [MAX_SHIFT_FRACTION] of the usual window. Null
     * with too few pairs, too little spread in nap length, or a shift under [MIN_SHIFT] minutes.
     */
    internal fun napEffectFor(windows: List<Window>, lastNap: Double, now: Long): NapEffect? {
        // Only nap-to-nap windows: the last window before bed ends when the clock says (process C),
        // so a long last nap shortens it, which isn't the pressure effect.
        val pairs = windows.filter { it.afterNapMinutes != null && !it.beforeBed }
        if (pairs.size < MIN_PAIRS) return null
        val w = pairs.map { recencyWeight(it.endedAt, now) }
        val sw = w.sum()
        val xs = pairs.map { it.afterNapMinutes!! }
        val ys = pairs.map { it.minutes }
        val mx = xs.indices.sumOf { w[it] * xs[it] } / sw
        val my = ys.indices.sumOf { w[it] * ys[it] } / sw
        // Compared within each slot of the day, so later windows simply being longer isn't read as
        // the nap's doing.
        val slotX = HashMap<Int, Double>()
        val slotY = HashMap<Int, Double>()
        pairs.indices.groupBy { pairs[it].slot }.forEach { (slot, idx) ->
            val sws = idx.sumOf { w[it] }
            slotX[slot] = idx.sumOf { w[it] * xs[it] } / sws
            slotY[slot] = idx.sumOf { w[it] * ys[it] } / sws
        }
        fun dx(i: Int) = xs[i] - slotX.getValue(pairs[i].slot)
        fun dy(i: Int) = ys[i] - slotY.getValue(pairs[i].slot)
        val vx = xs.indices.sumOf { w[it] * dx(it) * dx(it) } / sw
        if (vx < MIN_NAP_SPREAD * MIN_NAP_SPREAD) return null
        val cov = xs.indices.sumOf { w[it] * dx(it) * dy(it) } / sw
        val slope = (cov / vx).coerceIn(0.0, 1.0)
        val n = pairs.size.toDouble()
        val cap = my * MAX_SHIFT_FRACTION
        val shift = (n / (n + priorWeight) * slope * (lastNap - mx)).coerceIn(-cap, cap)
        if (kotlin.math.abs(shift) < MIN_SHIFT) return null
        return NapEffect(lastNap.roundToLong().toInt(), mx.roundToLong().toInt(), shift.roundToLong().toInt())
    }

    private data class Blend(val minutes: Double, val personal: Double?, val prior: Int, val samples: Int, val spreadMinutes: Double)

    private fun blended(samples: List<Window>, prior: Int, now: Long): Blend {
        val values = samples.map { it.minutes }
        val weights = samples.map { recencyWeight(it.endedAt, now) }
        val personal = weightedMedian(values, weights)
        val n = samples.size.toDouble()
        val minutes = if (personal == null) prior.toDouble() else (n * personal + priorWeight * prior) / (n + priorWeight)
        val spread = personal?.let { p -> weightedMad(values, weights, p)?.takeIf { samples.size >= 5 } }
            ?: (minutes * 0.15)
        return Blend(minutes, personal, prior, samples.size, spread)
    }

    /** How many naps the baby has had since this morning's wake, counting [last] if it's a nap. */
    private fun napsSinceMorningWake(sleeps: List<Event>, last: Event): Int {
        val idx = sleeps.indexOf(last)
        var n = 0
        for (i in idx downTo 0) {
            if (sleeps[i].isNightSleep(zone)) break
            n++
        }
        return n
    }

    /** Typical bedtime: recency-weighted median start of the first night sleep of each evening. */
    fun bedtimeMinute(sleeps: List<Event>, now: Long): Int? {
        val recent = sleeps.filter { it.start >= now - lookbackDays * DAY }.sortedBy { it.start }
        val starts = mutableListOf<Pair<Long, Double>>()
        for (i in recent.indices) {
            val s = recent[i]
            if (!s.isNightSleep(zone)) continue
            val prevNight = i > 0 && recent[i - 1].isNightSleep(zone)
            if (prevNight) continue
            var m = minuteOfDay(s.start, zone).toDouble()
            if (m < 12 * 60) m += 24 * 60 // after midnight still counts as "late evening"
            starts += s.start to m
        }
        if (starts.size < 3) return null
        val med = weightedMedian(starts.map { it.second }, starts.map { recencyWeight(it.first, now) }) ?: return null
        return (med.toInt()) % (24 * 60)
    }

    companion object {
        /** Pairs (nap, window after it) needed before the nap's length counts: about two days. */
        const val MIN_PAIRS = 8
        /** Naps must vary by at least this many minutes (standard deviation) for a slope to mean anything. */
        const val MIN_NAP_SPREAD = 10.0
        /** The nap's effect never moves a window by more than this share of the usual window. */
        const val MAX_SHIFT_FRACTION = 0.2
        /** After a late nap, bedtime comes at least this share of the usual last window after waking. */
        const val MIN_LAST_WINDOW_SHARE = 0.5
        /** Smaller shifts are noise: not applied, not shown. */
        const val MIN_SHIFT = 5.0
    }

    private fun nextOccurrence(minute: Int, after: Long): Long {
        val day = localDate(after, zone)
        val sameDay = startOfDay(day, zone) + minute * MINUTE
        return if (sameDay >= after - 6 * HOUR) sameDay else sameDay + DAY
    }
}
