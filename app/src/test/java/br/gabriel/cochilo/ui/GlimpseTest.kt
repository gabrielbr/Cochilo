package br.gabriel.cochilo.ui

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.assistant.Assistant
import br.gabriel.cochilo.domain.assistant.Assistant.Topic
import br.gabriel.cochilo.ui.screens.Glimpse
import br.gabriel.cochilo.ui.screens.glimpse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** Today's Ask Cochilo glimpse (0.45.0): what stands out in her log first, else a tip from the studies. */
class GlimpseTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant().toEpochMilli()
    private val now = at(20, 15)
    private fun facts(log: List<Event>, weeks: Int = 30) = Assistant.facts(log, weeks, now, zone, Predictor(zone).predict(log, weeks, now))

    /** Short naps, a third in arms, nights to 05:30, daylight logged. */
    private val shortNaps = (12..19).flatMap { d ->
        listOf(
            Event(type = EventType.SLEEP, start = at(d, 7, 30), end = at(d, 8, 5), kind = Kinds.ARMS),
            Event(type = EventType.SLEEP, start = at(d, 10, 0), end = at(d, 10, 35), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(d, 13, 0), end = at(d, 13, 35), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(d, 19, 30), end = at(d + 1, 6, 30)),
            Event(type = EventType.DAYLIGHT, start = at(d, 14, 0), end = at(d, 15, 0), kind = Kinds.OUTDOORS),
        )
    }

    /** Long naps in the crib, an hour of daylight: nothing stands out. */
    private val calm = (12..19).flatMap { d ->
        listOf(
            Event(type = EventType.SLEEP, start = at(d, 9, 0), end = at(d, 10, 30), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(d, 13, 30), end = at(d, 15, 0), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(d, 19, 30), end = at(d + 1, 6, 30)),
            Event(type = EventType.DAYLIGHT, start = at(d, 15, 30), end = at(d, 16, 30), kind = Kinds.OUTDOORS),
        )
    }

    @Test fun what_stands_out_in_her_log_comes_first() {
        assertEquals(Glimpse(Topic.SHORT_NAPS, tip = false), glimpse(facts(shortNaps), 0))
    }

    @Test fun otherwise_a_tip_from_the_studies_a_different_one_each_day() {
        val f = facts(calm)
        val a = glimpse(f, 0)
        val b = glimpse(f, 1)
        assertTrue(a.tip && b.tip)
        assertNotEquals(a.topic, b.topic)
    }

    @Test fun a_tip_even_before_a_few_days_are_logged_never_the_next_sleep() {
        for (d in 0L..20L) {
            val g = glimpse(facts(emptyList()), d)
            assertTrue(g.tip)
            assertNotEquals(Topic.NEXT_SLEEP, g.topic)
        }
    }

    @Test fun around_four_months_only_near_that_age() {
        val far = (0L..30L).map { glimpse(facts(calm, weeks = 30), it).topic }
        val near = (0L..30L).map { glimpse(facts(calm, weeks = 16), it).topic }
        assertTrue(Topic.FOUR_MONTHS !in far)
        assertTrue(Topic.FOUR_MONTHS in near)
    }
}
