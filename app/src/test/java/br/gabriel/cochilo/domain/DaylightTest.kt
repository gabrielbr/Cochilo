package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DaylightTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, 10, h, m).atZone(zone).toInstant().toEpochMilli()

    @Test fun nudgesFrom14WithNoDaylight() {
        assertTrue(Daylight.nudge(emptyList(), zone, at(14, 5)))
        assertTrue(Daylight.nudge(emptyList(), zone, at(15, 59)))
    }

    @Test fun neverBefore14OrFrom16() {
        assertFalse(Daylight.nudge(emptyList(), zone, at(12, 30)))
        assertFalse(Daylight.nudge(emptyList(), zone, at(16, 0)))
        assertFalse(Daylight.nudge(emptyList(), zone, at(20, 0)))
    }

    @Test fun daylightInTheWindowOrRunningSilencesIt() {
        val walk = Event(type = EventType.DAYLIGHT, start = at(12, 30), end = at(12, 50), kind = Kinds.OUTDOORS)
        assertFalse(Daylight.nudge(listOf(walk), zone, at(14, 30)))
        val running = Event(type = EventType.DAYLIGHT, start = at(14, 10), kind = Kinds.OUTDOORS)
        assertFalse(Daylight.nudge(listOf(running), zone, at(14, 30)))
    }

    @Test fun morningLightDoesNotCount() {
        val morning = Event(type = EventType.DAYLIGHT, start = at(9), end = at(10), kind = Kinds.OUTDOORS)
        assertTrue(Daylight.nudge(listOf(morning), zone, at(14, 30)))
    }
}
