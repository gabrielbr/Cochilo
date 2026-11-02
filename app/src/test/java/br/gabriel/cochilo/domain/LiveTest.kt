package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LiveTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(day: Int, h: Int, m: Int = 0, s: Int = 0) =
        LocalDateTime.of(2026, 10, day, h, m, s).atZone(zone).toInstant().toEpochMilli()
    private fun sleep(start: Long, end: Long?) = Event(type = EventType.SLEEP, start = start, end = end)

    @Test fun durationsFollowTheClockNotTheSeconds() {
        // The screenshot's "Acordado 0min": 01:53:40 → 01:54:10 reads as 01:53 → 01:54.
        assertEquals(1, clockMinutes(at(2, 1, 53, 40), at(2, 1, 54, 10)))
        assertEquals(0, clockMinutes(at(2, 1, 53, 10), at(2, 1, 53, 50)))
        // Across midnight: 20:25 → 01:53 is 5h 28m.
        assertEquals(5 * 60 + 28L, clockMinutes(at(1, 20, 25), at(2, 1, 53)))
        assertEquals(0, clockMinutes(at(2, 2, 0), at(2, 1, 0))) // never negative
    }

    @Test fun aNapIsExpectedToLastItsUsualLength() {
        val log = (10..14).map { d -> sleep(at(d, 13), at(d, 14, 10)) } // 70-minute naps
        val now = at(15, 13, 20)
        assertEquals(at(15, 14, 20), expectedWake(log, at(15, 13, 10), isNight = false, zone = zone, now = now))
        // Too few examples: no guess.
        assertNull(expectedWake(log.take(2), at(15, 13, 10), false, zone, now))
    }

    @Test fun aNightIsExpectedToEndAtTheUsualMorning() {
        val log = (10..14).map { d -> sleep(at(d, 20), at(d + 1, 6, 30)) }
        val since = at(15, 20, 15)
        assertEquals(at(16, 6, 30), expectedWake(log, since, isNight = true, zone = zone, now = since + HOUR))
    }

    @Test fun theBarCoversTheStretchAndCarriesItsEntries() {
        val log = (10..14).map { d -> sleep(at(d, 13), at(d, 14, 10)) } +
            sleep(at(15, 9), at(15, 10)) +
            Event(type = EventType.FEED, start = at(15, 10, 30), end = at(15, 10, 45), kind = Kinds.BREAST_LEFT) +
            Event(type = EventType.DIAPER, start = at(15, 11))
        val now = at(15, 11, 30)
        val awake = Predictor.Result.Awake(
            since = at(15, 10), next = Predictor.Next.NAP, at = at(15, 12), windowStart = at(15, 11, 45), windowEnd = at(15, 12, 15),
            slot = 1, wakeMinutes = 120, personalMinutes = 120, priorMinutes = 110, samples = 5,
        )
        val s = liveStretch(log, awake, zone, now)!!
        assertEquals(false, s.asleep)
        assertEquals(listOf(EventType.FEED, EventType.DIAPER), s.points.map { it.type })
        assertEquals(at(15, 12, 15), s.barEnd(now))
        assertEquals(666, s.progress(now)) // 90 of 135 minutes
        // Asleep: the expected wake is the bar's end; past it, the bar is full, never beyond.
        val asleep = liveStretch(log + sleep(at(15, 13), null), Predictor.Result.Sleeping(at(15, 13), false), zone, at(15, 15))!!
        assertTrue(asleep.asleep)
        assertEquals(1000, asleep.progress(at(15, 15)))
    }

    @Test fun theBarIsAwakeThenTheWindow() {
        val s = LiveStretch(asleep = false, isNight = false, since = at(15, 10), expectedEnd = at(15, 12),
            windowStart = at(15, 11, 30), windowEnd = at(15, 12, 30))
        val now = at(15, 11)
        val parts = s.segments(now)
        assertEquals(listOf(BarPart.AWAKE, BarPart.WINDOW), parts.map { it.part })
        assertEquals(0.6f, parts[0].to, 0.001f) // 90 of 150 minutes
        assertEquals(1f, parts[1].to, 0.001f)
        // Asleep: one piece in the night or nap colour.
        val night = LiveStretch(asleep = true, isNight = true, since = at(15, 20), expectedEnd = null)
        assertEquals(listOf(BarSegment(0f, 1f, BarPart.NIGHT)), night.segments(at(15, 21)))
    }
}
