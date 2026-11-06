package br.gabriel.cochilo.domain

import br.gabriel.cochilo.domain.RingLayout.Bubble
import br.gabriel.cochilo.domain.RingLayout.Label
import br.gabriel.cochilo.domain.RingLayout.Mark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingLayoutTest {
    private fun noOverlap(b: List<Bubble>, minSep: Double) {
        for (i in b.indices) for (j in b.indices) if (i < j)
            assertTrue("${b[i]} and ${b[j]} overlap", RingLayout.distance(b[i].angle, b[j].angle) >= minSep - 1e-3)
    }

    @Test fun lone_marks_stay_where_they_happened() {
        val b = RingLayout.bubbles(listOf(Mark("a", 30.0), Mark("b", 120.0)), minSep = 20.0)
        assertEquals(listOf(30.0, 120.0), b.map { it.angle })
    }

    @Test fun two_marks_together_float_apart_around_their_time() {
        val b = RingLayout.bubbles(listOf(Mark("feed", 100.0), Mark("diaper", 104.0)), minSep = 20.0)
        assertEquals(2, b.size)
        noOverlap(b, 20.0)
        // Spread round where they happened, not pushed off to one side.
        assertEquals(102.0, b.sumOf { it.angle } / 2, 0.5)
    }

    @Test fun three_or_more_together_become_one_stack() {
        val b = RingLayout.bubbles(listOf(Mark("a", 50.0), Mark("b", 52.0), Mark("c", 55.0), Mark("d", 200.0)), minSep = 20.0)
        assertEquals(2, b.size)
        assertEquals(listOf("a", "b", "c"), b.first().ids)
    }

    @Test fun neighbours_pushed_by_a_spread_still_dont_overlap() {
        val marks = listOf(Mark("a", 100.0), Mark("b", 105.0), Mark("c", 128.0), Mark("d", 150.0))
        val b = RingLayout.bubbles(marks, minSep = 20.0)
        noOverlap(b, 20.0)
        assertEquals(4, b.sumOf { it.ids.size })
    }

    @Test fun a_full_ring_stacks_instead_of_overlapping() {
        val marks = (0 until 40).map { Mark("m$it", it * 9.0) }
        val b = RingLayout.bubbles(marks, minSep = 20.0)
        assertTrue(b.size * 20.0 <= 360.0)
        noOverlap(b, 20.0)
        assertEquals(40, b.sumOf { it.ids.size })
    }

    @Test fun works_across_midnight() {
        val b = RingLayout.bubbles(listOf(Mark("late", 358.0), Mark("early", 2.0)), minSep = 20.0)
        noOverlap(b, 20.0)
    }

    @Test fun labels_keep_the_important_ones_and_never_overlap() {
        val kept = RingLayout.labels(listOf(
            Label("00", 0.0, 10.0, priority = 2),
            Label("sleep start", 4.0, 14.0, priority = 0),
            Label("sleep end", 60.0, 14.0, priority = 1),
            Label("06", 90.0, 10.0, priority = 2),
        ))
        assertEquals(listOf("sleep start", "sleep end", "06"), kept.map { it.id })
    }

    @Test fun labels_run_along_the_circle_and_are_never_upside_down() {
        assertEquals(0.0, RingLayout.labelRotation(0.0), 1e-9)
        assertEquals(90.0, RingLayout.labelRotation(90.0), 1e-9)
        assertEquals(315.0, RingLayout.labelRotation(135.0), 1e-9)
        assertEquals(0.0, RingLayout.labelRotation(180.0), 1e-9)
        assertEquals(270.0, RingLayout.labelRotation(270.0), 1e-9)
        assertEquals(300.0, RingLayout.labelRotation(300.0), 1e-9)
    }
}
