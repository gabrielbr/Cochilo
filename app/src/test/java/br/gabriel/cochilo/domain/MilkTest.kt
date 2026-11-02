package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MilkTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(h: Int) = LocalDateTime.of(2026, 11, 10, h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun nightIs19To07_byWhenItWasExpressed() {
        assertTrue(Milk.isNight(Event(type = EventType.PUMP, start = at(23)), zone))
        assertFalse(Milk.isNight(Event(type = EventType.PUMP, start = at(10)), zone))
        assertTrue(Milk.isNight(Event(type = EventType.PUMP, start = at(10), expressedAt = at(3)), zone))
    }

    @Test fun pumpedSplitsDayAndNight() {
        val log = listOf(
            Event(type = EventType.PUMP, start = at(9), end = at(9), amountMl = 120),
            Event(type = EventType.PUMP, start = at(22), end = at(22), amountMl = 90),
            Event(type = EventType.PUMP, start = at(14)),
        )
        assertEquals(120 to 90, Milk.pumped(log, zone, at(23)))
    }
}
