package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailsTest {
    private val t = 1_000_000_000L

    @Test fun a_sleep_without_a_place_asks_where_once_it_ends() {
        val running = Event(type = EventType.SLEEP, start = t)
        assertNull(Details.askAfter(running))
        assertEquals(Details.Ask.PLACE, Details.askAfter(running.copy(end = t + HOUR)))
        assertNull(Details.askAfter(running.copy(end = t + HOUR, kind = Kinds.CRIB)))
    }

    @Test fun feeds_pump_and_daylight() {
        assertEquals(Details.Ask.KIND, Details.askAfter(Event(type = EventType.FEED, start = t, end = t + 1)))
        assertEquals(Details.Ask.AMOUNT, Details.askAfter(Event(type = EventType.FEED, start = t, end = t + 1, kind = Kinds.BOTTLE)))
        assertNull(Details.askAfter(Event(type = EventType.FEED, start = t, end = t + 1, kind = Kinds.BREAST_LEFT)))
        assertEquals(Details.Ask.AMOUNT, Details.askAfter(Event(type = EventType.PUMP, start = t, end = t + 1)))
        assertNull(Details.askAfter(Event(type = EventType.PUMP, start = t, end = t + 1, amountMl = 90)))
        assertEquals(Details.Ask.KIND, Details.askAfter(Event(type = EventType.DAYLIGHT, start = t, end = t + 1)))
        assertNull(Details.askAfter(Event(type = EventType.DIAPER, start = t, end = t + 1)))
    }

    @Test fun the_last_answer_is_remembered() {
        val now = Event(type = EventType.SLEEP, start = t + 3 * HOUR, end = t + 4 * HOUR)
        val log = listOf(
            Event(type = EventType.SLEEP, start = t, end = t + HOUR, kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = t + HOUR, end = t + 2 * HOUR, kind = Kinds.SLING),
            now,
        )
        assertEquals(Kinds.SLING, Details.lastKind(now, log))
        val pump = Event(type = EventType.PUMP, start = t + 5 * HOUR, end = t + 6 * HOUR)
        assertEquals(120, Details.lastAmount(pump, log + Event(type = EventType.PUMP, start = t, end = t + 1, amountMl = 120) + pump))
    }

    @Test fun on_open_asks_only_about_a_recent_unanswered_one_never_twice() {
        val woke = Event(type = EventType.SLEEP, start = t, end = t + HOUR)
        val now = t + 2 * HOUR
        assertEquals(woke, Details.pending(listOf(woke), now, emptySet()))
        assertNull(Details.pending(listOf(woke), now, setOf(woke.id)))
        assertNull(Details.pending(listOf(woke), t + 4 * HOUR, emptySet()))
        // The newest finished entry decides: if it needs nothing, nothing is asked.
        val diaper = Event(type = EventType.DIAPER, start = t + 90 * MINUTE, end = t + 90 * MINUTE, kind = Kinds.WET)
        assertNull(Details.pending(listOf(woke, diaper), now, emptySet()))
    }
}
