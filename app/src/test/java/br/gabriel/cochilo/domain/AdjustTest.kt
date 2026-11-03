package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdjustTest {
    private val start = 1_000_000_000L
    private val nap = Event(type = EventType.SLEEP, start = start, end = start + 60 * MINUTE)
    private val now = start + 5 * HOUR

    @Test fun steps_round_to_the_nearest() {
        assertEquals(0, Adjust.steps(3f, 10f))
        assertEquals(1, Adjust.steps(6f, 10f))
        assertEquals(-2, Adjust.steps(-18f, 10f))
        assertEquals(0, Adjust.steps(50f, 0f))
    }

    @Test fun dragging_down_ends_it_later_in_five_minute_steps() {
        assertEquals(start + 75 * MINUTE, Adjust.newEnd(nap, 3, now, null))
    }

    @Test fun dragging_up_ends_it_sooner_but_never_under_five_minutes() {
        assertEquals(start + 45 * MINUTE, Adjust.newEnd(nap, -3, now, null))
        assertEquals(start + 5 * MINUTE, Adjust.newEnd(nap, -100, now, null))
    }

    @Test fun never_into_the_future_or_the_next_sleep() {
        assertEquals(start + 70 * MINUTE, Adjust.newEnd(nap, 10, start + 70 * MINUTE, null))
        assertEquals(start + 80 * MINUTE, Adjust.newEnd(nap, 10, now, start + 80 * MINUTE))
    }

    @Test fun no_change_is_null() {
        assertNull(Adjust.newEnd(nap, 0, now, null))
        // Already against the next sleep: dragging later changes nothing.
        assertNull(Adjust.newEnd(nap, 2, now, start + 60 * MINUTE))
    }

    @Test fun only_finished_sleeps() {
        assertTrue(Adjust.canAdjust(nap))
        assertFalse(Adjust.canAdjust(nap.copy(end = null)))
        assertFalse(Adjust.canAdjust(nap.copy(type = EventType.FEED)))
        assertNull(Adjust.newEnd(nap.copy(end = null), 1, now, null))
    }
}
