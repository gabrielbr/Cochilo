package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class NightsTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, d, h, m).atZone(zone).toInstant().toEpochMilli()
    private fun sleep(s: Long, e: Long?, night: Boolean? = null, up: Boolean? = null) =
        Event(type = EventType.SLEEP, start = s, end = e, night = night, upForDay = up)

    @Test fun start_sleep_guesses_by_the_hour_without_a_learned_bedtime() {
        assertFalse(Nights.guess(emptyList(), at(3, 13), zone, null))      // a nap
        assertFalse(Nights.guess(emptyList(), at(3, 17, 30), zone, null))  // a late nap, before 18:30
        assertTrue(Nights.guess(emptyList(), at(3, 19, 15), zone, null))   // the evening
        assertTrue(Nights.guess(emptyList(), at(3, 2), zone, null))        // the small hours
        assertFalse(Nights.guess(emptyList(), at(3, 6, 50), zone, null))   // an early first nap
    }

    @Test fun a_learned_bedtime_moves_the_guess() {
        // Bedtime around 21:30: a 19:20 catnap is a nap; 20:30 is the night (75 min before).
        assertFalse(Nights.guess(emptyList(), at(3, 19, 20), zone, 21 * 60 + 30))
        assertTrue(Nights.guess(emptyList(), at(3, 20, 30), zone, 21 * 60 + 30))
    }

    @Test fun during_a_night_waking_the_next_sleep_continues_the_night() {
        val log = listOf(sleep(at(3, 21), at(4, 2, 10), night = true))
        assertEquals(log[0], Nights.openNight(log, at(4, 2, 40)))
        // Back to sleep at 06:20, after 05:00: still the night, because one is open.
        val late = listOf(sleep(at(3, 21), at(4, 5, 50), night = true))
        assertTrue(Nights.guess(late, at(4, 6, 20), zone, null))
        // Up for the day closes it; so do 4 hours without sleep.
        assertNull(Nights.openNight(listOf(sleep(at(3, 21), at(4, 6), night = true, up = true)), at(4, 6, 30)))
        assertNull(Nights.openNight(log, at(4, 6, 30)))
    }

    @Test fun a_night_is_its_sleeps_until_up_for_the_day_and_wakings_are_the_gaps() {
        val nap = sleep(at(3, 19, 20), at(3, 19, 50), night = false)
        val a = sleep(at(3, 21, 30), at(4, 1, 0), night = true)
        val b = sleep(at(4, 1, 25), at(4, 4, 40), night = true)
        val c = sleep(at(4, 5, 0), at(4, 6, 30), night = true, up = true)
        val morningNap = sleep(at(4, 8, 0), at(4, 8, 40), night = false)
        val log = listOf(nap, a, b, c, morningNap)
        assertEquals(listOf(a, b, c), Nights.of(log, LocalDate.of(2026, 11, 3), zone))
        assertEquals(listOf(25, 20), Nights.wakings(Nights.of(log, LocalDate.of(2026, 11, 3), zone)))
        assertEquals(listOf(a, b, c), Nights.containing(log, c, zone))
        assertEquals(2, Stats(zone).nightWakings(log, LocalDate.of(2026, 11, 3)))
    }

    @Test fun an_evening_catnap_is_no_longer_bedtime_or_a_waking() {
        // The clock rule would call the 19:20 catnap the night, and count a waking before 21:30.
        val log = listOf(sleep(at(3, 19, 20), at(3, 19, 50), night = false), sleep(at(3, 21, 30), at(4, 6, 30), night = true, up = true))
        assertEquals(0, Stats(zone).nightWakings(log, LocalDate.of(2026, 11, 3)))
        assertTrue(log[0].isNap(zone))
    }

    @Test fun old_entries_keep_the_clock_rule() {
        assertTrue(sleep(at(3, 20), at(4, 6)).isNightSleep(zone))
        assertTrue(sleep(at(3, 13), at(3, 14)).isNap(zone))
    }
}
