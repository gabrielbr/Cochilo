package br.gabriel.cochilo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RemindersTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, 20, h, m).atZone(zone).toInstant().toEpochMilli()
    private fun awake(windowStart: Long, next: Predictor.Next = Predictor.Next.NAP) = Predictor.Result.Awake(
        since = windowStart - 90 * MINUTE, next = next, at = windowStart + 10 * MINUTE,
        windowStart = windowStart, windowEnd = windowStart + 20 * MINUTE,
        slot = 1, wakeMinutes = 100, personalMinutes = 100, priorMinutes = 90, samples = 10,
    )
    private val both = setOf(30, 15)

    @Test fun thirty_then_fifteen_minutes_before_the_window() {
        val p = awake(at(16))
        val first = Reminders.next(p, at(15), both, zone, emptyList())!!
        assertEquals(30, first.lead); assertEquals(at(15, 30), first.at)
        val second = Reminders.next(p, at(15, 31), both, zone, listOf(Reminders.Sent(30, at(16))))!!
        assertEquals(15, second.lead); assertEquals(at(15, 45), second.at)
        assertNull(Reminders.next(p, at(15, 46), both, zone, listOf(Reminders.Sent(30, at(16)), Reminders.Sent(15, at(16)))))
    }

    @Test fun only_the_leads_chosen() {
        assertEquals(15, Reminders.next(awake(at(16)), at(15), setOf(15), zone, emptyList())!!.lead)
        assertNull(Reminders.next(awake(at(16)), at(15), emptySet(), zone, emptyList()))
    }

    @Test fun a_late_reminder_is_skipped_not_sent() {
        // 20 minutes before the window: the 30-minute one is 10+ minutes late, the 15 is ahead.
        assertEquals(15, Reminders.next(awake(at(16)), at(15, 41), both, zone, emptyList())!!.lead)
    }

    @Test fun none_while_asleep_or_without_a_guess() {
        assertNull(Reminders.next(Predictor.Result.Sleeping(at(14), false), at(15), both, zone, emptyList()))
        assertNull(Reminders.next(Predictor.Result.NeedData, at(15), both, zone, emptyList()))
    }

    @Test fun none_in_the_quiet_night() {
        assertNull(Reminders.next(awake(at(21, 40)), at(20, 30), both, zone, emptyList()))
        // Bedtime at 20:30: 20:00 and 20:15 are before the quiet hours.
        assertEquals(30, Reminders.next(awake(at(20, 30), Predictor.Next.BEDTIME), at(19), both, zone, emptyList())!!.lead)
    }

    @Test fun a_window_moved_a_little_is_the_same_window() {
        val sent = listOf(Reminders.Sent(30, at(16)))
        assertEquals(15, Reminders.next(awake(at(16, 8)), at(15), both, zone, sent)!!.lead)
        // A really different window (a new nap) gets its own.
        assertEquals(30, Reminders.next(awake(at(18)), at(17), both, zone, sent)!!.lead)
    }

    @Test fun due_within_the_alarms_slack() {
        val d = Reminders.Due(at(15, 30), 30, Predictor.Next.NAP, at(16), at(16, 20))
        assertTrue(Reminders.isDue(d, at(15, 29)))
        assertTrue(Reminders.isDue(d, at(15, 38)))
        assertFalse(Reminders.isDue(d, at(15, 20)))
        assertFalse(Reminders.isDue(d, at(15, 45)))
    }
}
