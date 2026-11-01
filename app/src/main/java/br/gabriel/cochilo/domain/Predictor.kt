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
 */
class Predictor(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val lookbackDays: Int = 14,
    private val priorWeight: Double = 5.0,
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
        ) : Result
    }

    /** A daytime wake window seen in the log. */
    data class Window(val endedAt: Long, val minutes: Double, val slot: Int, val beforeBed: Boolean)

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
            out += Window(b.start, gap, slot = naps, beforeBed = bNight)
        }
        return out
    }

    fun predict(events: List<Event>, ageWeeks: Int, now: Long): Result {
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
        var next = Next.NAP
        var chosen = nextNapMinutes
        var at = awakeSince + (nextNapMinutes.minutes * MINUTE).roundToLong()

        if (bedtimeMinute != null && !last.isNightSleep(zone)) {
            val bedtimeAt = nextOccurrence(bedtimeMinute, awakeSince)
            // If the next nap would land within 45 minutes of bedtime, the next sleep is bedtime.
            if (at >= bedtimeAt - 45 * MINUTE) {
                val lastWindow = blended(all.filter { it.beforeBed }, prior.wakeForSlot(slot, true), now)
                val byWindow = awakeSince + (lastWindow.minutes * MINUTE).roundToLong()
                at = byWindow.coerceIn(bedtimeAt - 30 * MINUTE, bedtimeAt + 30 * MINUTE)
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
        )
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

    private fun nextOccurrence(minute: Int, after: Long): Long {
        val day = localDate(after, zone)
        val sameDay = startOfDay(day, zone) + minute * MINUTE
        return if (sameDay >= after - 6 * HOUR) sameDay else sameDay + DAY
    }
}
