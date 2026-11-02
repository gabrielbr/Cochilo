package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PlacesTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int) = LocalDateTime.of(2026, 11, d, h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun theLastPlaceIsRemembered() {
        val log = listOf(
            Event(type = EventType.SLEEP, start = at(1, 10), end = at(1, 11), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(2, 10), end = at(2, 11), kind = Kinds.SLING),
            Event(type = EventType.SLEEP, start = at(3, 10), end = at(3, 11)),
        )
        assertEquals(Kinds.SLING, Places.last(log))
        assertNull(Places.last(emptyList()))
    }

    @Test fun napsByPlace_mostFirst_nightsLeftOut() {
        val log = listOf(
            Event(type = EventType.SLEEP, start = at(8, 10), end = at(8, 11), kind = Kinds.SLING),
            Event(type = EventType.SLEEP, start = at(8, 14), end = at(8, 15), kind = Kinds.SLING),
            Event(type = EventType.SLEEP, start = at(9, 10), end = at(9, 11), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(9, 20), end = at(10, 5), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(1, 10), end = at(1, 11), kind = Kinds.ARMS),
        )
        assertEquals(listOf(Kinds.SLING to 2, Kinds.CRIB to 1), Places.napsByPlace(log, zone, at(10, 12)))
    }
}
