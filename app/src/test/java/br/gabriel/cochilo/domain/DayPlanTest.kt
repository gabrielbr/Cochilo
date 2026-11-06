package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DayPlanTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val plan = DayPlan(zone)
    private val weeks = 14 // four naps, bedtime 20:00 for the age

    private fun at(day: Int, h: Int, m: Int = 0): Long = LocalDateTime.of(2026, 10, day, h, m).atZone(zone).toInstant().toEpochMilli()
    private fun sleep(sd: Int, sh: Int, sm: Int, ed: Int, eh: Int, em: Int) = Event(type = EventType.SLEEP, start = at(sd, sh, sm), end = at(ed, eh, em))

    /** The same day, every day: night to 06:30, four naps after 90-minute windows, bed at 20:00. */
    private fun regularDays(days: IntRange): List<Event> = days.flatMap { d ->
        listOf(
            sleep(d, 8, 0, d, 9, 0),
            sleep(d, 10, 30, d, 11, 30),
            sleep(d, 13, 0, d, 14, 30),
            sleep(d, 16, 0, d, 16, 45),
            sleep(d, 20, 0, d + 1, 6, 30),
        )
    }

    @Test fun in_the_morning_it_projects_the_days_naps_then_bedtime() {
        val log = regularDays(10..19) // up at 06:30 on the 20th
        val p = plan.of(log, weeks, at(20, 7, 0))
        assertNotNull(p)
        assertEquals(4, p!!.naps.size)
        assertNotNull(p.bedtime)
        // The first is the card's own guess: about 08:00.
        assertEquals(at(20, 8, 0).toDouble(), p.naps[0].at.toDouble(), 20.0 * MINUTE)
        // In order, each after the one before ends.
        p.sleeps.zipWithNext().forEach { (a, b) -> assertTrue(b.at >= a.end) }
        // Bedtime near the usual 20:00.
        assertEquals(at(20, 20, 0).toDouble(), p.bedtime!!.at.toDouble(), 45.0 * MINUTE)
    }

    @Test fun the_further_ahead_the_wider_the_range() {
        val p = plan.of(regularDays(10..19), weeks, at(20, 7, 0))!!
        val widths = p.sleeps.map { it.windowEnd - it.windowStart }
        widths.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b >= a) }
        assertTrue(widths.last() > widths.first())
    }

    /**
     * The owner's screenshot (2026-11-06): more naps than the age's usual, awake at 16:31. The plan's
     * first sleep is the live guess Today shows (a nap), not bedtime.
     */
    @Test fun the_plan_starts_with_the_live_guess_even_after_many_naps() {
        val log = regularDays(10..19) + listOf(
            sleep(20, 7, 30, 20, 8, 10), sleep(20, 9, 10, 20, 9, 50), sleep(20, 10, 56, 20, 11, 40),
            sleep(20, 12, 5, 20, 12, 50), sleep(20, 13, 15, 20, 14, 0), sleep(20, 14, 50, 20, 15, 20),
        )
        val now = at(20, 16, 31)
        val live = Predictor(zone).predict(log, weeks, now) as Predictor.Result.Awake
        val p = plan.of(log, weeks, now)!!
        assertEquals(live.next, p.sleeps.first().next)
        assertEquals(live.at, p.sleeps.first().at)
        assertEquals("the screenshot's case: a nap is next", Predictor.Next.NAP, live.next)
        assertNotNull("bedtime still follows", p.bedtime)
    }

    @Test fun later_in_the_day_only_whats_left() {
        val log = regularDays(10..19) + listOf(sleep(20, 8, 0, 20, 9, 0), sleep(20, 10, 30, 20, 11, 30), sleep(20, 13, 0, 20, 14, 30))
        val p = plan.of(log, weeks, at(20, 15, 0))!!
        assertEquals(1, p.naps.size)
        assertNotNull(p.bedtime)
    }

    @Test fun a_short_nap_brings_the_rest_of_the_day_sooner() {
        val base = regularDays(10..19)
        val long = plan.of(base + sleep(20, 8, 0, 20, 9, 30), weeks, at(20, 9, 40))!!
        val short = plan.of(base + sleep(20, 8, 0, 20, 8, 30), weeks, at(20, 9, 40))!!
        assertTrue(short.naps.first().at < long.naps.first().at)
    }

    @Test fun a_running_nap_is_expected_to_end_then_the_day_goes_on() {
        val log = regularDays(10..19) + Event(type = EventType.SLEEP, start = at(20, 8, 0))
        val p = plan.of(log, weeks, at(20, 8, 20))!!
        assertTrue(p.naps.first().at > at(20, 9, 0))
    }

    @Test fun nothing_at_night_or_before_anything_is_logged() {
        assertNull(plan.of(emptyList(), weeks, at(20, 9, 0)))
        val log = regularDays(10..19) + Event(type = EventType.SLEEP, start = at(20, 20, 0), night = true)
        assertNull(plan.of(log, weeks, at(20, 22, 0)))
    }

    @Test fun with_little_log_it_says_it_comes_from_the_age() {
        val p = plan.of(listOf(sleep(19, 20, 0, 20, 6, 30)), weeks, at(20, 7, 0))!!
        assertTrue(p.fromAge)
        assertEquals(DayPlan.YOUNG_NAP_MIN.toInt(), p.napMinutes)
    }

    @Test fun usual_nap_length_comes_from_the_same_place_in_the_day() {
        val log = regularDays(10..19)
        assertEquals(60L, plan.napMinutes(log, 0, weeks))
        assertEquals(90L, plan.napMinutes(log, 2, weeks))
        assertEquals(45L, plan.napMinutes(log, 3, weeks))
    }
}
