package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.Instant
import java.time.ZoneId

/**
 * What the live notification shows (docs/notifications.md): the current stretch, asleep or awake,
 * as a bar from when it began to when it is expected to end, with the entries logged during it.
 */
data class LiveStretch(
    val asleep: Boolean,
    val isNight: Boolean,
    val since: Long,
    /** Expected end: wake-up while asleep, the best nap or bedtime guess while awake. Null = no guess yet. */
    val expectedEnd: Long?,
    /** While awake: the nap or bedtime window. */
    val windowStart: Long? = null,
    val windowEnd: Long? = null,
    /** Entries inside the stretch, oldest first: the timeline's points. */
    val points: List<Event> = emptyList(),
) {
    /** The bar's end: the window's end if there is one, else the expected end, never before now. */
    fun barEnd(now: Long): Long = maxOf(windowEnd ?: expectedEnd ?: now, now)

    /** 0..1000 along the bar, for the notification's progress. */
    fun progress(now: Long): Int {
        val span = (barEnd(now) - since).coerceAtLeast(1)
        return (((now - since).coerceAtLeast(0) * 1000) / span).toInt().coerceIn(0, 1000)
    }
}

private fun median(xs: List<Long>): Long? = xs.sorted().let { s -> if (s.isEmpty()) null else s[s.size / 2] }

/**
 * When a sleep that began at [since] is likely to end, from the baby's own last 14 days:
 * a nap lasts its median nap length; a night ends at the median morning wake time.
 * Null with fewer than 3 examples: the notification then says nothing it can't back up.
 */
fun expectedWake(events: List<Event>, since: Long, isNight: Boolean, zone: ZoneId, now: Long): Long? {
    val recent = events.filter { it.type == EventType.SLEEP && it.end != null && it.start >= now - 14 * DAY && it.start < since }
    if (!isNight) {
        val naps = recent.filter { !it.isNightSleep(zone) }.map { it.end!! - it.start }.filter { it in 10 * MINUTE..4 * HOUR }
        if (naps.size < 3) return null
        return since + median(naps)!!
    }
    // Morning wake: the last night-sleep end between 04:00 and 11:00 on each date.
    val mornings = recent.filter { it.isNightSleep(zone) }
        .mapNotNull { e ->
            val t = Instant.ofEpochMilli(e.end!!).atZone(zone)
            if (t.hour in 4..10) t.toLocalDate() to (t.toLocalTime().toSecondOfDay() * 1000L) else null
        }
        .groupBy({ it.first }, { it.second }).map { (_, ends) -> ends.max() }
    if (mornings.size < 3) return null
    val ms = median(mornings)!!
    // The next such morning after the night began.
    var day = Instant.ofEpochMilli(since).atZone(zone).toLocalDate()
    var at = startOfDay(day, zone) + ms
    while (at <= since) { day = day.plusDays(1); at = startOfDay(day, zone) + ms }
    return at
}

/** The live stretch now, or null when nothing is logged yet. */
fun liveStretch(events: List<Event>, prediction: Predictor.Result, zone: ZoneId, now: Long): LiveStretch? {
    fun pointsSince(t: Long) = events
        .filter { it.type != EventType.SLEEP && it.start in t..now }
        .sortedBy { it.start }
    return when (prediction) {
        Predictor.Result.NeedData -> null
        is Predictor.Result.Sleeping -> LiveStretch(
            asleep = true, isNight = prediction.isNight, since = prediction.since,
            expectedEnd = expectedWake(events, prediction.since, prediction.isNight, zone, now),
            points = pointsSince(prediction.since),
        )
        is Predictor.Result.Awake -> LiveStretch(
            asleep = false, isNight = false, since = prediction.since,
            expectedEnd = prediction.at, windowStart = prediction.windowStart, windowEnd = prediction.windowEnd,
            points = pointsSince(prediction.since),
        )
    }
}

/** What a stretch of the bar shows. */
enum class BarPart { NAP, NIGHT, AWAKE, WINDOW }

/** A piece of the bar, as fractions 0..1 of its width. */
data class BarSegment(val from: Float, val to: Float, val part: BarPart)

/** Where [t] falls on the bar, 0..1. */
fun LiveStretch.fraction(t: Long, now: Long): Float {
    val span = (barEnd(now) - since).coerceAtLeast(1)
    return ((t - since).coerceIn(0, span).toFloat() / span)
}

/**
 * The bar's pieces. Asleep: one piece, nap or night. Awake: awake time up to the window, the
 * window (when the next sleep is likely), and any time after it.
 */
fun LiveStretch.segments(now: Long): List<BarSegment> {
    if (asleep) return listOf(BarSegment(0f, 1f, if (isNight) BarPart.NIGHT else BarPart.NAP))
    val ws = windowStart?.let { fraction(it, now) } ?: 1f
    val we = windowEnd?.let { fraction(it, now) } ?: 1f
    return buildList {
        if (ws > 0f) add(BarSegment(0f, ws, BarPart.AWAKE))
        if (we > ws) add(BarSegment(ws, we, BarPart.WINDOW))
        if (we < 1f) add(BarSegment(we, 1f, BarPart.AWAKE))
    }
}
