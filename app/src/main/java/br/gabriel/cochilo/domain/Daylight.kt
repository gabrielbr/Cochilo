package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.Instant
import java.time.ZoneId

/**
 * The daylight nudge (docs/night-light.md): babies with more daylight between 12:00 and 16:00
 * slept better at night at 6–12 weeks (research §2.3, Harrison 2004; an association, not a
 * promise). From 14:00 to 16:00, if no daylight is logged in that window and none is running,
 * Today shows one quiet line with a button to start a walk. Pure, so it is tested.
 */
object Daylight {
    const val FROM_HOUR = 12
    const val UNTIL_HOUR = 16
    /** The nudge waits until the window is half gone, so it never nags at noon. */
    const val NUDGE_FROM_HOUR = 14

    fun nudge(log: List<Event>, zone: ZoneId, now: Long): Boolean {
        val t = Instant.ofEpochMilli(now).atZone(zone)
        if (t.hour < NUDGE_FROM_HOUR || t.hour >= UNTIL_HOUR) return false
        val day = startOfDay(t.toLocalDate(), zone)
        val from = day + FROM_HOUR * HOUR
        val until = day + UNTIL_HOUR * HOUR
        return log.none { it.type == EventType.DAYLIGHT && it.start < until && (it.end ?: now) > from }
    }
}
