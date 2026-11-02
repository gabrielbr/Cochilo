package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.ZoneId

/**
 * Day milk and night milk (research §2.4, docs/changes.md): breast milk expressed at night carries
 * more of the night's hormones, so pumped milk is tagged by when it was expressed (night is
 * 19:00–07:00, as everywhere in the app) and a bottle at night can be night milk. Pure, tested.
 */
object Milk {
    fun isNight(e: Event, zone: ZoneId): Boolean = isNightStart(e.expressedAt ?: e.start, zone)

    /** Millilitres pumped in the last [days] days, by day and by night. */
    fun pumped(log: List<Event>, zone: ZoneId, now: Long, days: Int = 7): Pair<Int, Int> {
        val pumps = log.filter { it.type == EventType.PUMP && it.start >= now - days * DAY && it.amountMl != null }
        val night = pumps.filter { isNight(it, zone) }.sumOf { it.amountMl!! }
        val day = pumps.filterNot { isNight(it, zone) }.sumOf { it.amountMl!! }
        return day to night
    }
}
