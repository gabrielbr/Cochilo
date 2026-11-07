package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepRepairTest {
    private val h = 3_600_000L
    private fun sleep(id: String, start: Long, end: Long?, edited: Long = 0) = Event(id = id, type = EventType.SLEEP, start = start, end = end, updatedAt = edited)

    /** The owner's screenshot: both phones started the night at 19:15; one woke up at 00:28, the other stayed open. */
    @Test fun the_open_twin_of_a_finished_night_goes() {
        val done = sleep("a", 19 * h + 15 * MINUTE, 24 * h + 28 * MINUTE, edited = 5)
        val open = sleep("b", 19 * h + 15 * MINUTE, null, edited = 9)
        val fix = SleepRepair.fix(listOf(open, done), now = 40 * h)
        assertEquals(listOf("b"), fix.delete.map { it.id })
        assertTrue(fix.update.isEmpty())
    }

    @Test fun two_finished_twins_keep_the_newer_edit_on_both_phones() {
        val a = sleep("a", 10 * h, 11 * h, edited = 1)
        val b = sleep("b", 10 * h + 2 * MINUTE, 11 * h + 5 * MINUTE, edited = 2)
        assertEquals(listOf("a"), SleepRepair.fix(listOf(a, b), 20 * h).delete.map { it.id })
        // The same answer whatever order the log comes in.
        assertEquals(listOf("a"), SleepRepair.fix(listOf(b, a), 20 * h).delete.map { it.id })
    }

    @Test fun a_quick_second_nap_after_a_short_one_is_not_a_twin() {
        val a = sleep("a", 13 * h, 13 * h + 3 * MINUTE)
        val b = sleep("b", 13 * h + 4 * MINUTE, 14 * h)
        assertTrue(SleepRepair.fix(listOf(a, b), 20 * h).none)
    }

    @Test fun an_open_sleep_with_a_later_sleep_ends_where_that_one_begins() {
        val stale = sleep("a", 8 * h, null)
        val later = sleep("b", 12 * h, 13 * h)
        val fix = SleepRepair.fix(listOf(stale, later), now = 20 * h)
        assertEquals(12 * h, fix.update.single().end)
        assertEquals(20 * h, fix.update.single().updatedAt)
        assertTrue(fix.delete.isEmpty())
    }

    @Test fun the_running_sleep_now_is_left_alone() {
        val done = sleep("a", 8 * h, 9 * h)
        val running = sleep("b", 12 * h, null)
        assertTrue(SleepRepair.fix(listOf(done, running), 13 * h).none)
    }

    @Test fun overlapping_sleeps_count_once_and_a_day_never_passes_24_hours() {
        val from = 0L; val to = 24 * h; val now = 21 * h
        val log = listOf(
            sleep("night", -5 * h, 6 * h),                // from yesterday
            sleep("twin", 19 * h, 24 * h + 28 * MINUTE),   // tonight
            sleep("open", 19 * h, null),                   // its open twin: up to now
            sleep("nap", 10 * h, 11 * h),
        )
        assertEquals((6 + 1 + 2) * h, SleepRepair.sleptIn(log, from, to, now))
        assertTrue(SleepRepair.sleptIn(log, from, to, 30 * h) <= 24 * h)
    }
}
