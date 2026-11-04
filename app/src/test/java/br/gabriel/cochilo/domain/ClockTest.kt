package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.random.Random

/**
 * The baby's clock (0.54.0, docs/prediction.md): the fit on hand-made days, and the first nap on
 * two synthetic half-years: a baby whose first nap keeps a time, and one whose first nap only
 * follows waking.
 */
class ClockTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val first = LocalDate.of(2026, 3, 2)
    private fun at(d: Int, h: Int, m: Int = 0) = startOfDay(first.plusDays(d.toLong()), zone) + h * HOUR + m * MINUTE

    @Test fun the_rhythm_forms_between_6_and_16_weeks() {
        assertEquals(0.0, Clock.ageShare(4), 0.0)
        assertEquals(0.5, Clock.ageShare(11), 0.001)
        assertEquals(1.0, Clock.ageShare(30), 0.0)
    }

    @Test fun regular_days_make_a_strong_clock_and_scattered_ones_none() {
        fun days(firstNap: (Int) -> Int): List<Event> = (0 until 10).flatMap { d ->
            listOf(
                // Wakes vary by an hour, so the clock can be told from the window.
                Event(type = EventType.SLEEP, start = at(d - 1, 19), end = at(d, 6) + (d * 23 % 60) * MINUTE, night = true),
                Event(type = EventType.SLEEP, start = at(d, 0) + firstNap(d) * MINUTE, end = at(d, 0) + (firstNap(d) + 60) * MINUTE, night = false),
            )
        }
        val now = at(10, 8)
        val regular = Clock.fit(days { 9 * 60 + (it % 3) * 5 }, 20, now, zone)
        assertEquals(9 * 60 + 5, regular.firstNapMinute)
        assertTrue("strength ${regular.firstNapStrength}", regular.firstNapStrength > 0.2)
        val scattered = Clock.fit(days { 7 * 60 + (it * 37) % 180 }, 20, now, zone)
        assertTrue("strength ${scattered.firstNapStrength}", scattered.firstNapStrength < regular.firstNapStrength / 2)
        // Too young: no clock, however regular.
        assertEquals(0.0, Clock.fit(days { 9 * 60 }, 5, now, zone).firstNapStrength, 0.0)
        // Too few mornings: nothing.
        assertNull(Clock.fit(days { 9 * 60 }.takeLast(6), 20, now, zone).firstNapMinute)
    }

    @Test fun lean_moves_by_the_strength_and_stops_at_the_cap() {
        assertEquals(30 * MINUTE, Clock.lean(0, 60 * MINUTE, 0.5, 45 * MINUTE))
        assertEquals(20 * MINUTE, Clock.lean(0, 60 * MINUTE, 0.5, 20 * MINUTE))
        assertEquals(0L, Clock.lean(0, 60 * MINUTE, 0.0, 20 * MINUTE))
    }

    /** First-nap errors on a synthetic half-year, in minutes. */
    private fun firstNapErrors(seed: Int, clockAnchored: Boolean, predictor: Predictor): List<Double> {
        val r = Random(seed)
        fun noise(sd: Double) = (r.nextDouble() + r.nextDouble() + r.nextDouble() - 1.5) * 2 * sd
        val events = mutableListOf<Event>()
        val cases = mutableListOf<Pair<Long, Long>>() // (woke, first nap start)
        for (d in 0 until 180) {
            val wake = at(d, 6, 30) + (noise(40.0) * MINUTE).toLong()
            events += Event(type = EventType.SLEEP, start = at(d - 1, 19) + (noise(10.0) * MINUTE).toLong(), end = wake, night = true)
            val window = 120.0
            val napStart = if (clockAnchored) at(d, 9) + (noise(10.0) * MINUTE).toLong() else wake + ((window + noise(10.0)) * MINUTE).toLong()
            cases += wake to napStart
            // The rest of the day: two naps, then bed.
            var t = napStart
            repeat(3) {
                val end = t + 60 * MINUTE
                events += Event(type = EventType.SLEEP, start = t, end = end, night = false)
                t = end + 150 * MINUTE
                if (t > at(d, 18)) return@repeat
            }
        }
        val all = events.sortedBy { it.start }
        return cases.drop(14).mapIndexed { i, (woke, actual) ->
            val log = all.filter { it.start < woke && it.start >= woke - 15 * DAY }
            val p = predictor.predict(log, 16 + (i + 14) / 7, woke) as Predictor.Result.Awake
            abs(p.at - actual).toDouble() / MINUTE
        }
    }

    @Test fun a_baby_whose_first_nap_keeps_a_time_is_predicted_better_with_the_clock() {
        val on = firstNapErrors(7, clockAnchored = true, Predictor(zone))
        val off = firstNapErrors(7, clockAnchored = true, Predictor(zone, clock = false))
        println("clock-anchored first nap: with ${on.average()}, without ${off.average()}")
        assertTrue("with ${on.average()}, without ${off.average()}", on.average() < off.average() - 5)
    }

    @Test fun and_one_whose_first_nap_only_follows_waking_isnt_harmed() {
        val on = firstNapErrors(8, clockAnchored = false, Predictor(zone))
        val off = firstNapErrors(8, clockAnchored = false, Predictor(zone, clock = false))
        println("window-driven first nap: with ${on.average()}, without ${off.average()}")
        assertTrue("with ${on.average()}, without ${off.average()}", on.average() <= off.average() + 2)
    }

    @Test fun pressure_builds_awake_drains_asleep_and_reaches_the_line_at_the_usual_window() {
        val nap = Event(type = EventType.SLEEP, start = at(0, 8), end = at(0, 9), night = false)
        val c = Pressure.curve(listOf(nap), at(0, 8), at(0, 12), at(0, 12), usualWindowMinutes = 120.0, usualNapMinutes = 60.0)
        val atWake = c.first { it.at == at(0, 9) }.percent
        val twoHoursLater = c.first { it.at == at(0, 11) }.percent
        assertTrue("after the nap $atWake%", atWake in 20..35) // a usual nap drains it to a quarter
        assertTrue("two hours on $twoHoursLater%", twoHoursLater in 95..110) // back near the line
        assertTrue(c.zipWithNext().filter { (a, _) -> a.at >= at(0, 9) }.all { (a, b) -> b.percent >= a.percent })
        assertTrue(Pressure.curve(emptyList(), 0, 1, 1, 120.0, 60.0).isEmpty())
    }
}
