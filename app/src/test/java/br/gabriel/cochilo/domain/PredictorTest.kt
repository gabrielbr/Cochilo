package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PredictorTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val predictor = Predictor(zone)

    private fun at(day: Int, h: Int, m: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, h, m).atZone(zone).toInstant().toEpochMilli()

    private fun sleep(startDay: Int, sh: Int, sm: Int, endDay: Int, eh: Int, em: Int) =
        Event(type = EventType.SLEEP, start = at(startDay, sh, sm), end = at(endDay, eh, em))

    /** A 3-month-old's day, repeated: night until 06:30, naps after 90-minute windows, bed at 20:00. */
    private fun regularDays(days: IntRange): List<Event> = days.flatMap { d ->
        listOf(
            sleep(d, 8, 0, d, 9, 0),     // window 0: 06:30 → 08:00 = 90 min
            sleep(d, 10, 30, d, 11, 30), // window 1: 90 min
            sleep(d, 13, 0, d, 14, 30),  // window 2: 90 min
            sleep(d, 16, 0, d, 16, 45),  // window 3: 90 min
            sleep(d, 20, 0, d + 1, 6, 30), // bedtime window: 195 min
        )
    }

    @Test fun needsDataWhenEmpty() {
        assertEquals(Predictor.Result.NeedData, predictor.predict(emptyList(), 13, at(20, 10)))
    }

    @Test fun reportsSleepingWhileATimerRuns() {
        val events = listOf(Event(type = EventType.SLEEP, start = at(20, 13)))
        val r = predictor.predict(events, 13, at(20, 13, 30))
        assertTrue(r is Predictor.Result.Sleeping)
        assertEquals(false, (r as Predictor.Result.Sleeping).isNight)
    }

    @Test fun windowsAreSlottedFromTheMorningWake() {
        val w = predictor.windows(regularDays(10..11), at(12, 12))
        // Day 10's morning wake comes from a night not in the log, so its first window is missing.
        val slots = w.map { it.slot to it.beforeBed }
        assertTrue(slots.contains(0 to false))
        assertTrue(slots.contains(3 to false))
        assertTrue(slots.contains(4 to true))
        assertTrue(w.filter { !it.beforeBed }.all { it.minutes == 90.0 })
    }

    @Test fun learnsTheBabysOwnWindow() {
        // Ten regular days, then this morning's wake at 06:30.
        val events = regularDays(10..19)
        val r = predictor.predict(events, 13, at(20, 7)) as Predictor.Result.Awake
        assertEquals(Predictor.Next.NAP, r.next)
        assertEquals(0, r.slot)
        // Personal 90 min (10 samples) blended with the prior for 12–16 weeks, slot 0 (86 min).
        assertEquals(90, r.personalMinutes)
        assertTrue("expected ~89 min, was ${r.wakeMinutes}", r.wakeMinutes in 87..90)
        assertTrue(r.windowStart < r.at && r.at < r.windowEnd)
    }

    @Test fun switchesToBedtimeInTheEvening() {
        val events = regularDays(10..19) + listOf(
            sleep(20, 8, 0, 20, 9, 0), sleep(20, 10, 30, 20, 11, 30),
            sleep(20, 13, 0, 20, 14, 30), sleep(20, 16, 0, 20, 17, 45),
        )
        val r = predictor.predict(events, 13, at(20, 18)) as Predictor.Result.Awake
        assertEquals(Predictor.Next.BEDTIME, r.next)
        // Learned bedtime is 20:00; the answer stays within 30 minutes of it.
        assertTrue(r.at in at(20, 19, 30)..at(20, 20, 30))
    }

    @Test fun aLateNapStillLeavesTimeBeforeBedtime() {
        // Today's last nap ends at 20:10, after the learned 20:00 bedtime.
        val events = regularDays(10..19) + listOf(
            sleep(20, 8, 0, 20, 9, 0), sleep(20, 10, 30, 20, 11, 30),
            sleep(20, 13, 0, 20, 14, 30), sleep(20, 18, 40, 20, 20, 10),
        )
        val r = predictor.predict(events, 13, at(20, 20, 10)) as Predictor.Result.Awake
        assertEquals(Predictor.Next.BEDTIME, r.next)
        assertTrue("bedtime ${r.wakeMinutes} min after waking", r.wakeMinutes >= 60)
    }

    @Test fun learnsBedtime() {
        val events = regularDays(10..19)
        assertEquals(20 * 60, predictor.bedtimeMinute(events.sortedBy { it.start }, at(20, 12)))
        assertNull(predictor.bedtimeMinute(emptyList(), at(20, 12)))
    }

    @Test fun usesThePriorWithLittleData() {
        val events = listOf(sleep(19, 20, 0, 20, 6, 0))
        val r = predictor.predict(events, 13, at(20, 6, 5)) as Predictor.Result.Awake
        assertNull(r.personalMinutes)
        assertEquals(Reference.priorFor(13).wakeForSlot(0, false), r.wakeMinutes)
    }

    @Test fun correctedAgeUsesTheDueDate() {
        val birth = LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val due = LocalDate.of(2026, 7, 29).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = LocalDate.of(2026, 10, 21).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(16, ageWeeks(birth, null, now))
        assertEquals(12, ageWeeks(birth, due, now))
    }
}
