package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StretchesTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant().toEpochMilli()
    private val from = at(20, 0)
    private val to = at(21, 0)

    private val night1 = Event(type = EventType.SLEEP, start = at(19, 20), end = at(20, 3, 0))
    private val night2 = Event(type = EventType.SLEEP, start = at(20, 3, 30), end = at(20, 6, 30))
    private val nap = Event(type = EventType.SLEEP, start = at(20, 9), end = at(20, 10))
    private val bedtime = Event(type = EventType.SLEEP, start = at(20, 19, 30), end = at(21, 6))
    private val nightFeed = Event(type = EventType.FEED, start = at(20, 3, 5), end = at(20, 3, 20), kind = Kinds.BREAST_LEFT)
    private val morningFeed = Event(type = EventType.FEED, start = at(20, 7), end = at(20, 7, 20), kind = Kinds.BREAST_RIGHT)
    private val diaperInNap = Event(type = EventType.DIAPER, start = at(20, 9, 30), kind = Kinds.WET)
    private val sleeps = listOf(night1, night2, nap, bedtime)
    private val all = sleeps + listOf(nightFeed, morningFeed, diaperInNap)

    @Test fun the_day_alternates_asleep_and_awake_newest_first() {
        val s = Stretches.of(all, sleeps, from, to, at(22, 12))
        assertEquals(listOf(true, false, true, false, true, false, true), s.map { it.asleep })
        assertEquals(bedtime, s[0].sleep)
        assertEquals(at(20, 10), s[1].start); assertEquals(at(20, 19, 30), s[1].end)
        assertEquals(night1, s.last().sleep)
    }

    @Test fun entries_sit_in_the_stretch_where_they_started() {
        val s = Stretches.of(all, sleeps, from, to, at(22, 12))
        val nightWaking = s.first { !it.asleep && it.start == at(20, 3, 0) }
        assertEquals(listOf(nightFeed), nightWaking.during)
        assertEquals(listOf(morningFeed), s.first { !it.asleep && it.start == at(20, 6, 30) }.during)
        assertEquals(listOf(diaperInNap), s.first { it.sleep == nap }.during)
    }

    @Test fun today_ends_with_the_awake_stretch_still_going() {
        val now = at(20, 12)
        val s = Stretches.of(all.filter { it.start < now }, sleeps.filter { it.start < now }, from, to, now)
        assertEquals(false, s[0].asleep)
        assertEquals(at(20, 10), s[0].start)
        assertNull(s[0].end)
    }

    @Test fun a_running_sleep_is_the_newest_stretch() {
        val running = Event(type = EventType.SLEEP, start = at(20, 13))
        val now = at(20, 13, 40)
        val s = Stretches.of(listOf(running, nap), listOf(night1, night2, nap, running), from, to, now)
        assertEquals(running, s[0].sleep)
        assertNull(s[0].end)
    }

    @Test fun without_any_sleep_logged_everything_is_one_awake_stretch() {
        val now = at(20, 12)
        val s = Stretches.of(listOf(morningFeed), emptyList(), from, to, now)
        assertEquals(1, s.size)
        assertEquals(false, s[0].asleep)
        assertNull(s[0].start)
        assertEquals(listOf(morningFeed), s[0].during)
    }
}
