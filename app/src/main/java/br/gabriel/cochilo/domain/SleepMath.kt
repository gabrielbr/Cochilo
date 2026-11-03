package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

/**
 * The clock rule, for sleeps logged before 0.42.0 (no `night` flag): one that starts 19:00–07:00 is
 * night sleep, anything else a nap. Since 0.42.0 a sleep says which it is (docs/nights.md).
 */
const val NIGHT_START_HOUR = 19
const val NIGHT_END_HOUR = 7

fun minuteOfDay(t: Long, zone: ZoneId): Int {
    val lt = Instant.ofEpochMilli(t).atZone(zone).toLocalTime()
    return lt.hour * 60 + lt.minute
}

fun isNightStart(t: Long, zone: ZoneId): Boolean {
    val h = Instant.ofEpochMilli(t).atZone(zone).hour
    return h >= NIGHT_START_HOUR || h < NIGHT_END_HOUR
}

fun Event.isNightSleep(zone: ZoneId) = type == EventType.SLEEP && (night ?: isNightStart(start, zone))
fun Event.isNap(zone: ZoneId) = type == EventType.SLEEP && !(night ?: isNightStart(start, zone))

/**
 * True when the entry touches [from, to): a sleep or feed by its span (a running one lasts until
 * [now]), a diaper or note by its moment. An instant has no end, so it must not borrow [now].
 */
fun Event.overlaps(from: Long, to: Long, now: Long): Boolean {
    val end = if (type.hasDuration) end ?: now else start
    return end >= from && start < to
}

/**
 * The time between two moments as the clock shows them: whole minutes from start's minute to end's.
 * 01:53:40 → 01:54:10 is 1 minute (the screen says 01:53 and 01:54), not 0.
 */
fun clockMinutes(start: Long, end: Long): Long = (Math.floorDiv(end, MINUTE) - Math.floorDiv(start, MINUTE)).coerceAtLeast(0)

/** [clockMinutes] in milliseconds, for Format.duration. */
fun clockSpan(start: Long, end: Long): Long = clockMinutes(start, end) * MINUTE

fun localDate(t: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
fun startOfDay(d: LocalDate, zone: ZoneId): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()

/** Weeks of age, corrected for prematurity when a due date is known. */
fun ageWeeks(birthDate: Long, dueDate: Long?, now: Long): Int {
    val ref = if (dueDate != null && dueDate > birthDate) dueDate else birthDate
    return (ChronoUnit.DAYS.between(Instant.ofEpochMilli(ref), Instant.ofEpochMilli(now)) / 7).toInt().coerceAtLeast(0)
}

/** Median of values weighted by weights. Empty input gives null. */
fun weightedMedian(values: List<Double>, weights: List<Double>): Double? {
    if (values.isEmpty()) return null
    val pairs = values.zip(weights).sortedBy { it.first }
    val total = pairs.sumOf { it.second }
    var acc = 0.0
    for ((v, w) in pairs) {
        acc += w
        if (acc >= total / 2) return v
    }
    return pairs.last().first
}

/** Weighted median absolute deviation: how spread out the samples are. */
fun weightedMad(values: List<Double>, weights: List<Double>, center: Double): Double? =
    weightedMedian(values.map { abs(it - center) }, weights)

/** Recent days count more: a sample loses half its weight every [halfLifeDays] days. */
fun recencyWeight(sampleTime: Long, now: Long, halfLifeDays: Double = 3.0): Double =
    0.5.pow(((now - sampleTime).toDouble() / DAY) / halfLifeDays)
