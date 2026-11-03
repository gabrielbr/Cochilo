package br.gabriel.cochilo.domain

import kotlin.math.sqrt

/**
 * How tall a block is on the Day tab's timeline (0.46.0, docs/flows.md), in units of `space1`
 * (4dp). It grows with the square root of the minutes, so it isn't 1:1, but longer always looks
 * longer at a glance:
 * - a 10-minute feed is about 19dp;
 * - a 40-minute nap is about 38dp;
 * - a 2-hour nap is about 66dp, three and a half times the feed;
 * - a 10-hour night is about 147dp.
 *
 * The block is never shorter than [MIN_UNITS] (so a short feed still reads as a bar) or taller
 * than [MAX_UNITS]. Before 0.46.0 every row was at least 48dp and the bar filled its row, so
 * anything under 2 hours looked the same. Pure Kotlin, tested in TimelineScaleTest.
 */
object TimelineScale {
    /** Units per square root of a minute. */
    const val PER_ROOT_MINUTE = 1.5f
    /** At least 8dp: a 5-minute feed is still a bar, not a dot. */
    const val MIN_UNITS = 2f
    /** At most 192dp: a 14-hour night doesn't take the whole screen. */
    const val MAX_UNITS = 48f

    fun units(minutes: Int): Float =
        (PER_ROOT_MINUTE * sqrt(minutes.coerceAtLeast(0).toFloat())).coerceIn(MIN_UNITS, MAX_UNITS)
}
