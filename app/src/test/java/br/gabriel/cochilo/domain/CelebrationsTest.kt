package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CelebrationsTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, d, h, m).atZone(zone).toInstant().toEpochMilli()
    private val today = LocalDate.of(2026, 11, 10)
    private val nothingShown = emptyMap<Moment, LocalDate>()

    /** Three earlier naps of an hour, so the expected nap is about an hour. */
    private val pastNaps = (6..8).map { d -> Event(type = EventType.SLEEP, start = at(d, 10), end = at(d, 11)) }

    @Test fun aNapAsLongAsExpectedIsAFullNap() {
        val nap = Event(type = EventType.SLEEP, start = at(10, 10), end = at(10, 11, 2))
        val c = Celebrations.after(Trigger.WokeUp(nap, Predictor.Result.NeedData), pastNaps + nap, zone, at(10, 11, 2), nothingShown)
        assertEquals(Moment.FULL_NAP, c?.moment)
        assertFalse(c!!.quiet)
    }

    @Test fun aShortNapIsNotCelebrated_andNothingScolds() {
        val nap = Event(type = EventType.SLEEP, start = at(10, 10), end = at(10, 10, 25))
        assertNull(Celebrations.after(Trigger.WokeUp(nap, Predictor.Result.NeedData), pastNaps + nap, zone, at(10, 10, 25), nothingShown))
    }

    @Test fun theSameMomentShowsOnceADay() {
        val nap = Event(type = EventType.SLEEP, start = at(10, 10), end = at(10, 11, 2))
        val shown = mapOf(Moment.FULL_NAP to today)
        assertNull(Celebrations.after(Trigger.WokeUp(nap, Predictor.Result.NeedData), pastNaps + nap, zone, at(10, 11, 2), shown))
    }

    @Test fun atMostThreeADay() {
        val nap = Event(type = EventType.SLEEP, start = at(10, 10), end = at(10, 11, 2))
        val shown = mapOf(Moment.ON_TIME to today, Moment.DAYLIGHT to today, Moment.ROUTINE_BEFORE_BED to today)
        assertNull(Celebrations.after(Trigger.WokeUp(nap, Predictor.Result.NeedData), pastNaps + nap, zone, at(10, 11, 2), shown))
    }

    @Test fun theLongestNightOfTheWeek_onlyWithNightsToCompare() {
        val nights = (6..9).map { d -> Event(type = EventType.SLEEP, start = at(d, 20), end = at(d + 1, 2)) }
        val tonight = Event(type = EventType.SLEEP, start = at(9, 20, 30), end = at(10, 5))
        val earlier = nights.dropLast(1)
        val c = Celebrations.after(Trigger.WokeUp(tonight, Predictor.Result.NeedData), earlier + tonight, zone, at(10, 5), nothingShown)
        assertEquals(Moment.LONGEST_NIGHT, c?.moment)
        // With only one night before it there is nothing to compare: no celebration.
        val c2 = Celebrations.after(Trigger.WokeUp(tonight, Predictor.Result.NeedData), earlier.take(1) + tonight, zone, at(10, 5), nothingShown)
        assertNull(c2)
    }

    @Test fun startingSleepInTheWindowIsOnTime() {
        val window = Predictor.Result.Awake(
            since = at(10, 9), next = Predictor.Next.NAP, at = at(10, 10, 30), windowStart = at(10, 10, 20), windowEnd = at(10, 10, 40),
            slot = 0, wakeMinutes = 90, personalMinutes = 90, priorMinutes = 90, samples = 10,
        )
        val sleep = Event(type = EventType.SLEEP, start = at(10, 10, 25))
        assertEquals(Moment.ON_TIME, Celebrations.after(Trigger.StartedSleep(sleep, window), listOf(sleep), zone, at(10, 10, 25), nothingShown)?.moment)
        val late = sleep.copy(start = at(10, 11))
        assertNull(Celebrations.after(Trigger.StartedSleep(late, window), listOf(late), zone, at(10, 11), nothingShown))
    }

    @Test fun aRoutineBeforeBedIsCelebrated_quietlyAtNight() {
        val routine = Event(type = EventType.ROUTINE, start = at(10, 19, 10), kind = Kinds.BEDTIME_ROUTINE)
        val night = Event(type = EventType.SLEEP, start = at(10, 19, 45))
        val c = Celebrations.after(Trigger.StartedSleep(night, Predictor.Result.NeedData), listOf(routine, night), zone, at(10, 19, 45), nothingShown)
        assertEquals(Moment.ROUTINE_BEFORE_BED, c?.moment)
        assertTrue("words only at night", c!!.quiet)
    }

    @Test fun daylightCelebratesOnlyTheWalkThatCrossesHalfAnHour() {
        val first = Event(type = EventType.DAYLIGHT, start = at(10, 12), end = at(10, 12, 20), kind = Kinds.OUTDOORS)
        val second = Event(type = EventType.DAYLIGHT, start = at(10, 14), end = at(10, 14, 15), kind = Kinds.OUTDOORS)
        assertNull(Celebrations.after(Trigger.Logged(first), listOf(first), zone, at(10, 12, 20), nothingShown))
        assertEquals(Moment.DAYLIGHT, Celebrations.after(Trigger.Logged(second), listOf(first, second), zone, at(10, 14, 15), nothingShown)?.moment)
        // Morning light doesn't count toward the 12:00–16:00 goal.
        val morning = Event(type = EventType.DAYLIGHT, start = at(10, 8), end = at(10, 9), kind = Kinds.OUTDOORS)
        assertNull(Celebrations.after(Trigger.Logged(morning), listOf(morning), zone, at(10, 9), nothingShown))
    }

    @Test fun aWeekOfLoggingShowsOnceAWeek() {
        val week = (4..10).map { d -> Event(type = EventType.DIAPER, start = at(d, 9), kind = Kinds.WET) }
        val e = week.last()
        assertEquals(Moment.WEEK_LOGGED, Celebrations.after(Trigger.Logged(e), week, zone, at(10, 9), nothingShown)?.moment)
        assertNull(Celebrations.after(Trigger.Logged(e), week, zone, at(10, 9), mapOf(Moment.WEEK_LOGGED to today.minusDays(3))))
        // A gap in the week: nothing (and nothing about the gap either).
        assertNull(Celebrations.after(Trigger.Logged(e), week.filterIndexed { i, _ -> i != 3 }, zone, at(10, 9), nothingShown))
    }
}
