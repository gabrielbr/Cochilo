package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickLogTest {
    private fun feed(start: Long, kind: String) = Event(type = EventType.FEED, start = start, end = start + 600_000, kind = kind)

    @Test fun offersTheOtherBreastFromTheLastBreastfeed() {
        assertNull(suggestedBreast(emptyList()))
        assertEquals(Kinds.BREAST_RIGHT, suggestedBreast(listOf(feed(1, Kinds.BREAST_RIGHT), feed(2, Kinds.BREAST_LEFT))))
        assertEquals(Kinds.BREAST_LEFT, suggestedBreast(listOf(feed(2, Kinds.BREAST_RIGHT), feed(1, Kinds.BREAST_LEFT))))
        // A bottle afterwards doesn't change which breast is next.
        assertEquals(Kinds.BREAST_LEFT, suggestedBreast(listOf(feed(1, Kinds.BREAST_RIGHT), feed(5, Kinds.BOTTLE))))
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
