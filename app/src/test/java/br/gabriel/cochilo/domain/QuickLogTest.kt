package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickLogTest {
    private fun feed(start: Long, kind: String) = Event(type = EventType.FEED, start = start, end = start + 600_000, kind = kind)

    @Test fun remembersTheLastBreast() {
        // Right was last: it's what the row marks and the sheet opens on.
        val log = listOf(feed(1, Kinds.BREAST_LEFT), feed(2, Kinds.BREAST_RIGHT))
        assertEquals(Kinds.BREAST_RIGHT, lastKind(log, EventType.FEED))
        assertNull(lastKind(emptyList(), EventType.FEED))
        // The row remembers the last breast even when a bottle came after.
        val withBottle = log + feed(3, Kinds.BOTTLE)
        assertEquals(Kinds.BREAST_RIGHT, lastKindAmong(withBottle, EventType.FEED, setOf(Kinds.BREAST_LEFT, Kinds.BREAST_RIGHT)))
        val fresh = Event(type = EventType.FEED, start = 10, kind = Kinds.BREAST_LEFT)
        assertEquals(Kinds.BREAST_RIGHT, fresh.rememberedFrom(log).kind)
    }

    @Test fun remembersTheBottleAmountAndTheMedicine() {
        val log = listOf(
            Event(type = EventType.FEED, start = 1, end = 2, kind = Kinds.BOTTLE, amountMl = 120),
            Event(type = EventType.MEDICINE, start = 3, note = "Luftal"),
            Event(type = EventType.PUMP, start = 4, end = 5, amountMl = 90),
            Event(type = EventType.DIAPER, start = 6, kind = Kinds.BOTH),
        )
        val bottle = Event(type = EventType.FEED, start = 10, kind = Kinds.BREAST_LEFT).rememberedFrom(log)
        assertEquals(Kinds.BOTTLE, bottle.kind)
        assertEquals(120, bottle.amountMl)
        assertEquals("Luftal", Event(type = EventType.MEDICINE, start = 10).rememberedFrom(log).note)
        assertEquals(90, Event(type = EventType.PUMP, start = 10).rememberedFrom(log).amountMl)
        assertEquals(Kinds.BOTH, Event(type = EventType.DIAPER, start = 10, kind = Kinds.WET).rememberedFrom(log).kind)
        // A note isn't repeated: notes are one-offs.
        assertNull(Event(type = EventType.NOTE, start = 10).rememberedFrom(log + Event(type = EventType.NOTE, start = 8, note = "x")).note)
    }

    @Test fun otherBreastIsOnlyForBreasts() {
        assertEquals(Kinds.BREAST_LEFT, otherBreast(Kinds.BREAST_RIGHT))
        assertNull(otherBreast(Kinds.BOTTLE))
    }

    @Test fun slidePicksTheOptionUnderTheFingerOncePastTheThreshold() {
        val w = 300f
        // Two options: left half and right half.
        assertNull(slideOption(x = 160f, startX = 150f, width = w, threshold = 48f, count = 2)) // not far enough
        assertEquals(1, slideOption(x = 260f, startX = 150f, width = w, threshold = 48f, count = 2)) // flick right
        assertEquals(0, slideOption(x = 40f, startX = 150f, width = w, threshold = 48f, count = 2)) // flick left
        // Three options: the middle one is reached by sliding in from a side.
        assertEquals(1, slideOption(x = 150f, startX = 20f, width = w, threshold = 48f, count = 3))
        // Past the edge still counts as the edge option.
        assertEquals(2, slideOption(x = 400f, startX = 150f, width = w, threshold = 48f, count = 3))
        // Back where it started: cancelled.
        assertNull(slideOption(x = 151f, startX = 150f, width = w, threshold = 48f, count = 3))
    }
}
