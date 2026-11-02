package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import java.time.LocalDate
import kotlin.math.abs

/**
 * Change detection instead of "regressions" (research §2.5; docs/changes.md): the app doesn't
 * hard-code sleep regressions; it notices a real change in this baby's own data. The last 7 days
 * against the 14 before them, per day. A change is shown only when it is both large (≥ 15%) and
 * meaningful in size, with both numbers, in neutral words ("longer", "more"), never "better" or
 * "worse", and never a prediction. Pure, so it is tested.
 */
enum class Metric { NIGHT_SLEEP, NAP_SLEEP, LONGEST_STRETCH, NIGHT_WAKINGS, NAPS }

data class Change(val metric: Metric, val now: Double, val before: Double) {
    val up: Boolean get() = now > before
}

object Changes {
    const val RECENT_DAYS = 7
    const val BEFORE_DAYS = 14
    /** Each window needs this many days with sleep logged before anything is said. */
    const val MIN_DAYS = 4
    private const val MIN_RELATIVE = 0.15

    /** The smallest change worth a sentence, per metric (minutes, or counts per day). */
    private fun minAbsolute(m: Metric): Double = when (m) {
        Metric.NIGHT_SLEEP, Metric.NAP_SLEEP -> 30.0
        Metric.LONGEST_STRETCH -> 30.0
        Metric.NIGHT_WAKINGS -> 0.75
        Metric.NAPS -> 0.75
    }

    /** Whole days only: the recent week ends yesterday, so a half-logged today never skews it. */
    fun detect(log: List<Event>, today: LocalDate, now: Long, stats: Stats): List<Change> {
        val end = today.minusDays(1)
        val recent = stats.averages(stats.days(log, end, RECENT_DAYS, now)) ?: return emptyList()
        val before = stats.averages(stats.days(log, end.minusDays(RECENT_DAYS.toLong()), BEFORE_DAYS, now)) ?: return emptyList()
        if (recent.days < MIN_DAYS || before.days < MIN_DAYS) return emptyList()
        fun value(a: Averages, m: Metric): Double = when (m) {
            Metric.NIGHT_SLEEP -> a.nightSleepMin.toDouble()
            Metric.NAP_SLEEP -> a.napSleepMin.toDouble()
            Metric.LONGEST_STRETCH -> a.longestStretchMin.toDouble()
            Metric.NIGHT_WAKINGS -> a.nightWakings
            Metric.NAPS -> a.naps
        }
        return Metric.entries.mapNotNull { m ->
            val n = value(recent, m)
            val b = value(before, m)
            val diff = abs(n - b)
            val relative = if (b == 0.0) Double.MAX_VALUE else diff / b
            if (diff >= minAbsolute(m) && relative >= MIN_RELATIVE) Change(m, n, b) else null
        }
    }
}
