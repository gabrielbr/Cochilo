package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ChangesTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 11, 22)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun at(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    /** 21 whole days before today: nights of [oldNight] hours, then the last 7 of [newNight]. */
    private fun log(oldNight: Double, newNight: Double, naps: Int = 2): List<Event> = buildList {
        for (back in 1..22) {
            val d = today.minusDays(back.toLong())
            val hours = if (back <= 7) newNight else oldNight
            val start = at(d.minusDays(1), 20)
            add(Event(type = EventType.SLEEP, start = start, end = start + (hours * HOUR).toLong()))
            repeat(naps) { i -> add(Event(type = EventType.SLEEP, start = at(d, 10 + 3 * i), end = at(d, 11 + 3 * i))) }
        }
    }

    @Test fun aLongerNightIsNoticed_withBothNumbers() {
        val c = Changes.detect(log(8.0, 10.0), today, now, Stats(zone))
        val night = c.single { it.metric == Metric.NIGHT_SLEEP }
        assertTrue(night.up)
        // Per-day stats split each night at midnight, so the window's first day still holds part of an
        // older (shorter) night: about 565 rather than 600.
        assertEquals(600.0, night.now, 45.0)
        assertEquals(480.0, night.before, 45.0)
    }

    @Test fun aSteadyWeekSaysNothing() {
        assertEquals(emptyList<Change>(), Changes.detect(log(9.0, 9.2), today, now, Stats(zone)))
    }

    @Test fun tooLittleDataSaysNothing() {
        val few = log(8.0, 10.0).filter { it.start >= now - 10 * DAY }
        assertEquals(emptyList<Change>(), Changes.detect(few, today, now, Stats(zone)))
    }

    @Test fun napsCountChangesBothWays() {
        val old = log(9.0, 9.0, naps = 4).filter { it.start < now - 8 * DAY }
        val recent = log(9.0, 9.0, naps = 2).filter { it.start >= now - 8 * DAY }
        val c = Changes.detect(old + recent, today, now, Stats(zone)).single { it.metric == Metric.NAPS }
        assertTrue(!c.up)
    }
}
