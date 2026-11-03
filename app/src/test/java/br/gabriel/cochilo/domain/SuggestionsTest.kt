package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.Suggestions.Moment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SuggestionsTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant().toEpochMilli()
    private val now = at(20, 14)
    private val products = mapOf(
        Moment.ARMS_NAPS to "sling", Moment.ROUTINE_STEADY to "naninha",
        Moment.LITTLE_DAYLIGHT to "sling", Moment.SIX_MONTHS to "tata_to",
    )

    /** A week of naps in arms and a nightly bedtime routine, plenty of daylight. */
    private val log = (13..19).flatMap { d ->
        listOf(
            Event(type = EventType.SLEEP, start = at(d, 9), end = at(d, 10), kind = Kinds.ARMS),
            Event(type = EventType.SLEEP, start = at(d, 13), end = at(d, 14), kind = Kinds.CRIB),
            Event(type = EventType.ROUTINE, start = at(d, 19, 10), end = at(d, 19, 30), kind = Kinds.BEDTIME_ROUTINE),
            Event(type = EventType.SLEEP, start = at(d, 19, 40), end = at(d + 1, 6, 30)),
            Event(type = EventType.DAYLIGHT, start = at(d, 12), end = at(d, 13), kind = Kinds.OUTDOORS),
        )
    }

    private fun input(
        log: List<Event> = this.log, age: Int = 8, now: Long = this.now, sleeping: Boolean = false,
        firstSeen: Long = at(1, 8), enabled: Boolean = true, approved: Set<Moment> = Moment.entries.toSet(),
        dismissed: Set<Moment> = emptySet(), shown: Map<Moment, Long> = emptyMap(),
    ) = Suggestions.Input(log, age, now, zone, sleeping, firstSeen, enabled, approved, products, dismissed, shown)

    @Test fun naps_in_arms_bring_the_sling_with_the_count() {
        val s = Suggestions.pick(input())!!
        assertEquals(Moment.ARMS_NAPS, s.moment)
        assertEquals("sling", s.product)
        assertEquals(6, s.count) // the 13th's 9:00 nap is just over a week ago
    }

    @Test fun nothing_shows_until_tata_approves_it() {
        assertNull(Suggestions.pick(input(approved = emptySet())))
        assertEquals(Moment.ROUTINE_STEADY, Suggestions.pick(input(approved = setOf(Moment.ROUTINE_STEADY)))!!.moment)
    }

    @Test fun never_off_asleep_at_night_or_in_the_first_week() {
        assertNull(Suggestions.pick(input(enabled = false)))
        assertNull(Suggestions.pick(input(sleeping = true)))
        assertNull(Suggestions.pick(input(now = at(20, 21))))
        assertNull(Suggestions.pick(input(now = at(20, 6))))
        assertNull(Suggestions.pick(input(firstSeen = at(16, 8))))
    }

    @Test fun a_shown_note_stays_its_week_then_retires_for_good() {
        val shown = mapOf(Moment.ARMS_NAPS to at(18, 10))
        assertEquals(Moment.ARMS_NAPS, Suggestions.pick(input(shown = shown))!!.moment)
        // Nine days later it's gone, and the next one (a different product) may come.
        val later = input(now = at(27, 14), shown = shown, log = log.map { it.copy(start = it.start + 7 * DAY, end = it.end?.plus(7 * DAY)) })
        assertEquals(Moment.ROUTINE_STEADY, Suggestions.pick(later)!!.moment)
    }

    @Test fun at_most_one_new_note_a_week_even_after_a_dismissal() {
        val shown = mapOf(Moment.ARMS_NAPS to at(18, 10))
        assertNull(Suggestions.pick(input(shown = shown, dismissed = setOf(Moment.ARMS_NAPS))))
    }

    @Test fun dismissed_means_never_again() {
        val s = Suggestions.pick(input(dismissed = setOf(Moment.ARMS_NAPS)))!!
        assertEquals(Moment.ROUTINE_STEADY, s.moment)
    }

    @Test fun never_the_same_product_two_notes_in_a_row() {
        // The sling was last; little daylight would bring the sling again, so it waits.
        val shown = mapOf(Moment.ARMS_NAPS to at(5, 10))
        val dark = log.filter { it.type != EventType.DAYLIGHT }
        val s = Suggestions.pick(input(log = dark, shown = shown, approved = setOf(Moment.ARMS_NAPS, Moment.LITTLE_DAYLIGHT)))
        assertNull(s)
    }

    @Test fun each_rule_on_its_own() {
        val i = input(age = 20)
        assertNull(Suggestions.fires(Moment.ARMS_NAPS, i))
        assertEquals(7, Suggestions.fires(Moment.ROUTINE_STEADY, i)!!.count)
        assertNull(Suggestions.fires(Moment.LITTLE_DAYLIGHT, i))
        assertEquals(0, Suggestions.fires(Moment.LITTLE_DAYLIGHT, input(log = log.filter { it.type != EventType.DAYLIGHT }))!!.count)
        assertNull(Suggestions.fires(Moment.SIX_MONTHS, i))
        assertEquals(Moment.SIX_MONTHS, Suggestions.fires(Moment.SIX_MONTHS, input(age = 27))!!.moment)
    }
}
