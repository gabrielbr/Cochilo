package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateTest {
    private val h = 3_600_000L

    @Test fun awakeSinceTheLastWake() {
        val s = widgetState(listOf(Event(type = EventType.SLEEP, start = 1 * h, end = 2 * h)), hasBaby = true)
        assertFalse(s.asleep)
        assertEquals(2 * h, s.since)
    }

    @Test fun asleepSinceTheRunningSleep() {
        val s = widgetState(listOf(
            Event(type = EventType.SLEEP, start = 1 * h, end = 2 * h),
            Event(type = EventType.SLEEP, start = 3 * h),
        ), hasBaby = true)
        assertTrue(s.asleep)
        assertEquals(3 * h, s.since)
    }

    @Test fun noSleepYetHasNoSince() {
        assertNull(widgetState(emptyList(), hasBaby = true).since)
    }

    @Test fun lastBreastIgnoresALaterBottle() {
        val s = widgetState(listOf(
            Event(type = EventType.FEED, start = 1 * h, end = 1 * h + 600_000, kind = Kinds.BREAST_RIGHT),
            Event(type = EventType.FEED, start = 2 * h, end = 2 * h + 600_000, kind = Kinds.BOTTLE, amountMl = 90),
        ), hasBaby = true)
        assertEquals(Kinds.BREAST_RIGHT, s.lastBreast)
        assertNull(s.feeding)
    }

    @Test fun aRunningBreastfeedIsFeeding_aRunningBottleIsNot() {
        val left = Event(type = EventType.FEED, start = 1 * h, kind = Kinds.BREAST_LEFT)
        assertEquals(left, widgetState(listOf(left), hasBaby = true).feeding)
        val bottle = Event(type = EventType.FEED, start = 1 * h, kind = Kinds.BOTTLE)
        assertNull(widgetState(listOf(bottle), hasBaby = true).feeding)
    }
}
