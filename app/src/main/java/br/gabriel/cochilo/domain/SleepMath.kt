package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

const val MINUTE = 60_000L
const val HOUR = 60 * MINUTE
const val DAY = 24 * HOUR

/** Night is 19:00–07:00: a sleep that starts in it is night sleep, anything else is a nap. */
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

/** A sleep's own nap/night label wins; without one, the time it started decides. */
fun Event.isNightSleep(zone: ZoneId) = type == EventType.SLEEP && when (kind) {
    Kinds.NIGHT_SLEEP -> true
    Kinds.NAP_SLEEP -> false
    else -> isNightStart(start, zone)
}
fun Event.isNap(zone: ZoneId) = type == EventType.SLEEP && !isNightSleep(zone)

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
