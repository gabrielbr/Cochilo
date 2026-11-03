package br.gabriel.cochilo.domain

import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * Window reminders (docs/notifications.md): a heads-up 30 and 15 minutes before the next sleep
 * window opens, so there's time to start the routine. Pure rules, tested; the alarm and the
 * notification are `notify/SleepReminders.kt`.
 */
object Reminders {
    /** The leads offered, in minutes before the window opens. */
    val LEADS = listOf(30, 15)

    /** A reminder that fires this late is noise: skip it. */
    const val GRACE = 10 * MINUTE

    /** An alarm may fire this early and still count. */
    const val EARLY = 2 * MINUTE

    /** No reminder from 21:00 to 06:00: the night has its own rhythm. */
    const val QUIET_FROM_HOUR = 21
    const val QUIET_UNTIL_HOUR = 6

    /** Two windows this close are the same window, moved a little by a newer guess. */
    const val SAME_WINDOW = 20 * MINUTE

    data class Due(val at: Long, val lead: Int, val next: Predictor.Next, val windowStart: Long, val windowEnd: Long)

    /** A reminder already sent: [lead] minutes before the window that opened at [windowStart]. */
    data class Sent(val lead: Int, val windowStart: Long)

    /** The next reminder to send for prediction [p], or null. */
    fun next(p: Predictor.Result, now: Long, leads: Set<Int>, zone: ZoneId, sent: List<Sent>): Due? {
        val a = p as? Predictor.Result.Awake ?: return null
        return leads.sortedDescending().asSequence()
            .map { lead -> Due(a.windowStart - lead * MINUTE, lead, a.next, a.windowStart, a.windowEnd) }
            .filter { it.at >= now - GRACE }
            .filter { !quiet(it.at, zone) }
            .filter { d -> sent.none { it.lead == d.lead && abs(it.windowStart - d.windowStart) < SAME_WINDOW } }
            .minByOrNull { it.at }
    }

    /** Whether [d] should be sent now (its time has come, give or take the alarm's slack). */
    fun isDue(d: Due, now: Long): Boolean = now >= d.at - EARLY && now <= d.at + GRACE

    fun quiet(t: Long, zone: ZoneId): Boolean {
        val h = Instant.ofEpochMilli(t).atZone(zone).hour
        return h >= QUIET_FROM_HOUR || h < QUIET_UNTIL_HOUR
    }
}
