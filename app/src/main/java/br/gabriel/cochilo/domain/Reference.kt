package br.gabriel.cochilo.domain

/**
 * Age-based starting points. These are heuristics from sleep practitioners, not validated science
 * (see docs/research.md §3). The predictor only leans on them until it has about a week of the
 * baby's own data.
 */
data class AgePrior(
    /** Upper bound (exclusive) of the band, in weeks of (corrected) age. */
    val untilWeeks: Int,
    val wakeMinMinutes: Int,
    val wakeMaxMinutes: Int,
    val naps: Int,
    /** Typical bedtime as minutes after midnight; null when there is no real bedtime yet. */
    val bedtimeMinute: Int?,
) {
    /** First window of the day is the shortest, the one before bed the longest. */
    fun wakeForSlot(slot: Int, isLast: Boolean): Int = when {
        isLast -> wakeMaxMinutes
        slot == 0 -> wakeMinMinutes + (wakeMaxMinutes - wakeMinMinutes) / 4
        else -> (wakeMinMinutes + wakeMaxMinutes) / 2
    }
}

object Reference {
    val priors = listOf(
        AgePrior(6, 35, 60, 5, null),
        AgePrior(12, 60, 90, 4, 21 * 60),
        AgePrior(17, 75, 120, 4, 20 * 60),
        AgePrior(30, 120, 180, 3, 19 * 60 + 30),
        AgePrior(43, 150, 210, 2, 19 * 60),
        AgePrior(61, 180, 240, 2, 19 * 60),
        AgePrior(104, 240, 360, 1, 19 * 60 + 30),
        AgePrior(Int.MAX_VALUE, 300, 420, 1, 20 * 60),
    )

    fun priorFor(ageWeeks: Int): AgePrior = priors.first { ageWeeks < it.untilWeeks }

    /**
     * Total sleep per 24 h. Under 4 months: Galland et al. 2012 (mean 12.8 h, 9.7–15.9 h, wide normal
     * variation, AASM makes no recommendation). From 4 months: AASM 2016, 12–16 h including naps.
     */
    data class SleepRange(val lowHours: Double, val highHours: Double, val source: String)

    fun totalSleepRange(ageWeeks: Int): SleepRange = when {
        ageWeeks < 17 -> SleepRange(9.7, 15.9, "Galland 2012")
        ageWeeks < 52 -> SleepRange(12.0, 16.0, "AASM 2016")
        ageWeeks < 104 -> SleepRange(11.0, 14.0, "AASM 2016")
        else -> SleepRange(10.0, 13.0, "AASM 2016")
    }
}
