package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.random.Random

/**
 * The predictor on a synthetic year (docs/prediction.md): a baby from 8 weeks to about a year,
 * whose wake windows grow with age, whose naps vary, and (for the main baby) whose window after a
 * nap follows the nap's length the way the two-process model says. Every rule is checked on the
 * whole year, not one day.
 */
class PredictorYearTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val firstDay = LocalDate.of(2026, 1, 5)
    private val birthWeeks = 8

    private data class Day(val events: List<Event>, val napEnds: List<Pair<Long, Long>>) // (awake since, next sleep start)

    /** [napSlope]: minutes of window per minute of nap above the usual (0 = no effect). */
    private fun year(seed: Int, napSlope: Double, days: Int = 330): List<Day> {
        val r = Random(seed)
        fun noise(sd: Double) = (r.nextDouble() + r.nextDouble() + r.nextDouble() - 1.5) * 2 * sd // ~normal
        val out = mutableListOf<Day>()
        var morning = startOfDay(firstDay, zone) + 6 * HOUR + 30 * MINUTE
        for (d in 0 until days) {
            val age = birthWeeks + d / 7
            val base = 50.0 + 3.0 * age // 74 min at 8 weeks, ~215 at a year
            val usualNap = 70.0
            val events = mutableListOf<Event>()
            val ends = mutableListOf<Pair<Long, Long>>()
            var t = morning
            var prevNap: Double? = null
            val dayStart = startOfDay(firstDay.plusDays(d.toLong()), zone)
            while (true) {
                val window = base + (prevNap?.let { napSlope * (it - usualNap) } ?: 0.0) + noise(8.0)
                val start = t + (window * MINUTE).toLong()
                if (start >= dayStart + 18 * HOUR + 45 * MINUTE) {
                    // Bedtime, never before 19:00, then the night to tomorrow's wake.
                    val bed = maxOf(start, dayStart + 19 * HOUR + (noise(10.0).coerceAtLeast(0.0) * MINUTE).toLong())
                    if (prevNap != null) ends += t to bed
                    val wake = dayStart + DAY + 6 * HOUR + 30 * MINUTE + (noise(15.0) * MINUTE).toLong()
                    events += Event(type = EventType.SLEEP, start = bed, end = wake)
                    morning = wake
                    break
                }
                if (prevNap != null) ends += t to start
                val nap = (30 + r.nextDouble() * 80) // 30..110 min: short and long naps both happen
                val end = start + (nap * MINUTE).toLong()
                events += Event(type = EventType.SLEEP, start = start, end = end)
                prevNap = nap
                t = end
            }
            out += Day(events, ends)
        }
        return out
    }

    private data class Run(val errors: List<Double>, val sane: Boolean)

    private fun run(days: List<Day>, predictor: Predictor): Run {
        val all = days.flatMap { it.events }.sortedBy { it.start }
        val errors = mutableListOf<Double>()
        var sane = true
        for ((i, day) in days.withIndex()) {
            if (i < 14) continue // two weeks to learn
            for ((awake, actual) in day.napEnds) {
                val log = all.filter { it.start < awake && it.start >= awake - 15 * DAY }
                val p = predictor.predict(log, birthWeeks + i / 7, awake) as Predictor.Result.Awake
                if (p.at < awake + 20 * MINUTE || p.at > awake + 6 * HOUR) sane = false
                errors += abs(p.at - actual).toDouble() / MINUTE
            }
        }
        return Run(errors, sane)
    }

    private fun List<Double>.pct(q: Double) = sorted()[((size - 1) * q).toInt()]

    @Test fun a_year_of_naps_is_predicted_within_minutes() {
        val days = year(seed = 1, napSlope = 0.5)
        val on = run(days, Predictor(zone))
        assertTrue(on.sane)
        assertTrue("predictions: ${on.errors.size}", on.errors.size > 600)
        assertTrue("median error ${on.errors.pct(0.5)} min", on.errors.pct(0.5) <= 15.0)
        assertTrue("90th percentile ${on.errors.pct(0.9)} min", on.errors.pct(0.9) <= 40.0)
    }

    @Test fun the_last_naps_length_makes_it_better_when_the_baby_has_that_effect() {
        val days = year(seed = 2, napSlope = 0.5)
        val on = run(days, Predictor(zone)).errors
        val off = run(days, Predictor(zone, napEffect = false)).errors
        assertTrue("with ${on.pct(0.5)}, without ${off.pct(0.5)}", on.pct(0.5) < off.pct(0.5))
        assertTrue("mean with ${on.average()}, without ${off.average()}", on.average() < off.average())
    }

    @Test fun and_does_no_harm_when_the_baby_has_none() {
        val days = year(seed = 3, napSlope = 0.0)
        val on = run(days, Predictor(zone)).errors
        val off = run(days, Predictor(zone, napEffect = false)).errors
        assertTrue("with ${on.average()}, without ${off.average()}", on.average() <= off.average() + 2.0)
    }

    // napEffectFor on hand-made pairs.

    private val p = Predictor(zone)
    private val now = startOfDay(firstDay, zone) + 12 * HOUR

    private fun pairs(vararg napToWindow: Pair<Int, Int>) = napToWindow.mapIndexed { i, (nap, w) ->
        Predictor.Window(endedAt = now - i * HOUR, minutes = w.toDouble(), slot = 1, beforeBed = false, afterNapMinutes = nap.toDouble())
    }

    @Test fun a_short_nap_brings_the_next_sleep_sooner() {
        val w = pairs(40 to 80, 100 to 110, 40 to 80, 100 to 110, 40 to 80, 100 to 110, 40 to 80, 100 to 110, 70 to 95, 70 to 95)
        val e = p.napEffectFor(w, lastNap = 40.0, now = now)
        assertNotNull(e)
        assertTrue("shift ${e!!.shiftMinutes}", e.shiftMinutes < 0)
        assertEquals(40, e.lastNapMinutes)
        assertEquals(70, e.usualNapMinutes)
    }

    @Test fun a_long_nap_pushes_it_later_but_never_past_the_cap() {
        val w = pairs(40 to 60, 100 to 140, 40 to 60, 100 to 140, 40 to 60, 100 to 140, 40 to 60, 100 to 140)
        val e = p.napEffectFor(w, lastNap = 200.0, now = now)!!
        assertTrue(e.shiftMinutes > 0)
        // 20% of the usual 100-minute window.
        assertTrue("shift ${e.shiftMinutes}", e.shiftMinutes <= 20)
    }

    @Test fun too_few_pairs_or_naps_all_alike_change_nothing() {
        assertNull(p.napEffectFor(pairs(40 to 80, 100 to 110, 40 to 80), 40.0, now))
        assertNull(p.napEffectFor(pairs(*Array(10) { 70 to (80 + it) }), 40.0, now))
    }

    @Test fun a_nap_that_seems_to_shorten_the_window_is_ignored() {
        // Longer naps followed by shorter windows: the slope is held at 0, so no effect.
        val w = pairs(40 to 120, 100 to 70, 40 to 120, 100 to 70, 40 to 120, 100 to 70, 40 to 120, 100 to 70)
        assertNull(p.napEffectFor(w, 40.0, now))
    }

    @Test fun a_usual_nap_moves_nothing() {
        val w = pairs(40 to 80, 100 to 110, 40 to 80, 100 to 110, 40 to 80, 100 to 110, 40 to 80, 100 to 110)
        assertNull(p.napEffectFor(w, 72.0, now))
    }
}
