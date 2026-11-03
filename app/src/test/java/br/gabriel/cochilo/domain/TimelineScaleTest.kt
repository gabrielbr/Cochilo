package br.gabriel.cochilo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineScaleTest {
    @Test fun longer_is_always_taller() {
        val minutes = listOf(5, 10, 20, 40, 60, 90, 120, 240, 600)
        minutes.zipWithNext().forEach { (a, b) -> assertTrue("$a < $b", TimelineScale.units(a) < TimelineScale.units(b)) }
    }

    @Test fun two_hours_of_sleep_reads_clearly_longer_than_ten_minutes_of_feeding() {
        assertTrue(TimelineScale.units(120) >= 3 * TimelineScale.units(10))
    }

    @Test fun not_one_to_one_so_a_night_fits_the_screen() {
        // 10 hours is 60 times 10 minutes, but its block is under 10 times as tall.
        assertTrue(TimelineScale.units(600) < 10 * TimelineScale.units(10))
    }

    @Test fun bounded_both_ways() {
        assertEquals(TimelineScale.MIN_UNITS, TimelineScale.units(0))
        assertEquals(TimelineScale.MIN_UNITS, TimelineScale.units(1))
        assertEquals(TimelineScale.MAX_UNITS, TimelineScale.units(24 * 60))
    }
}
