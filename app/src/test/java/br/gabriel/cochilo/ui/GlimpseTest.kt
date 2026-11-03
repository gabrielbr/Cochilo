package br.gabriel.cochilo.ui

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.assistant.Assistant
import br.gabriel.cochilo.domain.assistant.Assistant.Topic
import br.gabriel.cochilo.ui.screens.glimpseTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** Today's Ask Cochilo glimpse (0.45.0): what stands out first, never the next sleep, never empty. */
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

    @Test fun what_stands_out_comes_first() {
        assertEquals(Topic.SHORT_NAPS, glimpseTopic(facts(shortNaps), 0))
    }

    @Test fun otherwise_an_everyday_question_with_her_numbers_a_different_one_each_day() {
        val f = facts(calm)
        val a = glimpseTopic(f, 0)
        val b = glimpseTopic(f, 1)
        assertTrue("$a", a in setOf(Topic.NIGHT_WAKINGS, Topic.ENOUGH_SLEEP, Topic.WAKE_WINDOWS, Topic.BEDTIME, Topic.NAP_LENGTH))
        assertNotEquals(a, b)
    }

    @Test fun never_the_next_sleep_or_the_age_alone() {
        for (d in 0L..10L) {
            val t = glimpseTopic(facts(calm, weeks = 16), d)
            assertNotEquals(Topic.NEXT_SLEEP, t)
            assertNotEquals(Topic.FOUR_MONTHS, t)
        }
    }

    @Test fun silent_until_a_few_days_are_logged() {
        assertNull(glimpseTopic(facts(calm.filter { it.start >= at(19, 0) }), 0))
    }
}
