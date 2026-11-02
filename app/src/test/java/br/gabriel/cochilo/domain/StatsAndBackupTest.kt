package br.gabriel.cochilo.domain

import br.gabriel.cochilo.backup.Backup
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsAndBackupTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val stats = Stats(zone)

    private fun at(day: Int, h: Int, m: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, h, m).atZone(zone).toInstant().toEpochMilli()

    private val log = listOf(
        Event(type = EventType.SLEEP, start = at(19, 20), end = at(20, 2)),   // night, crosses midnight
        Event(type = EventType.SLEEP, start = at(20, 2, 30), end = at(20, 6)), // night, after a waking
        Event(type = EventType.SLEEP, start = at(20, 9), end = at(20, 10)),    // nap
        Event(type = EventType.SLEEP, start = at(20, 13), end = at(20, 14, 30)), // nap
        Event(type = EventType.SLEEP, start = at(20, 20), end = at(21, 5)),     // night
        Event(type = EventType.FEED, start = at(20, 7), end = at(20, 7, 20), kind = Kinds.BREAST_LEFT),
        Event(type = EventType.FEED, start = at(20, 11), kind = Kinds.BOTTLE, amountMl = 120, end = at(20, 11, 15)),
        Event(type = EventType.DIAPER, start = at(20, 8), kind = Kinds.WET),
        Event(type = EventType.PUMP, start = at(20, 15), end = at(20, 15, 20), amountMl = 90),
        Event(type = EventType.DAYLIGHT, start = at(20, 11, 30), end = at(20, 12, 30)),
    )

    @Test fun splitsSleepAcrossMidnight() {
        val d = stats.day(log, LocalDate.of(2026, 10, 20), at(22, 0))
        // Night: 00:00–02:00 + 02:30–06:00 + 20:00–24:00 = 2h + 3.5h + 4h
        assertEquals(570, d.nightSleepMin)
        assertEquals(150, d.napSleepMin)
        assertEquals(2, d.naps)
        assertEquals(540, d.longestStretchMin) // 20:00 → 05:00 started today
        assertEquals(2, d.feeds)
        assertEquals(1, d.diapers)
        assertEquals(90, d.pumpedMl)
        assertEquals(60, d.daylightMin)
        assertEquals(30, d.afternoonDaylightMin)
    }

    @Test fun aDayHoldsItsOwnInstantsAndTheSleepsThatCrossIt() {
        val day = LocalDate.of(2026, 10, 30)
        val from = day.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val to = from + 86_400_000
        val now = to + 10 * 3_600_000 // the next morning
        fun at(h: Int) = from + h * 3_600_000L
        // Yesterday's diaper must not show up today just because it has no end time.
        assertEquals(false, Event(type = EventType.DIAPER, start = at(-5)).overlaps(from, to, now))
        assertEquals(true, Event(type = EventType.DIAPER, start = at(7)).overlaps(from, to, now))
        assertEquals(true, Event(type = EventType.SLEEP, start = at(-1), end = at(3)).overlaps(from, to, now))
        assertEquals(false, Event(type = EventType.SLEEP, start = at(-8), end = at(-2)).overlaps(from, to, now))
        // A timer still running yesterday reaches into today.
        assertEquals(true, Event(type = EventType.FEED, start = at(-1)).overlaps(from, to, now))
    }

    @Test fun countsNightWakings() {
        assertEquals(1, stats.nightWakings(log.filter { it.type == EventType.SLEEP }, LocalDate.of(2026, 10, 19)))
    }

    @Test fun averagesSkipEmptyDays() {
        val days = stats.days(log, LocalDate.of(2026, 10, 21), 7, at(22, 0))
        val avg = stats.averages(days)!!
        assertEquals(3, avg.days) // 19th, 20th and 21st have sleep
    }

    @Test fun backupRoundTrips() {
        val baby = Baby(name = "Bebê", birthDate = at(1, 0))
        val json = Backup.toJson(baby, log, at(22, 0))
        val back = Backup.fromJson(json)
        assertEquals(baby, back.baby)
        assertEquals(log.sortedBy { it.start }, back.events)
    }

    @Test fun rejectsOtherFiles() {
        assertThrows(IllegalArgumentException::class.java) { Backup.fromJson("{\"hello\": 1}") }
        assertThrows(IllegalArgumentException::class.java) { Backup.fromJson("not json") }
    }

    @Test fun mergeIsIdempotentAndKeepsNewerEdits() {
        val first = Backup.merge(emptyList(), log)
        assertEquals(log.size, first.added)
        val again = Backup.merge(first.merged, log)
        assertEquals(0, again.added)
        assertEquals(log.size, again.skipped)
        val edited = log[0].copy(note = "fussy", updatedAt = log[0].updatedAt + 1)
        val third = Backup.merge(again.merged, listOf(edited))
        assertEquals(1, third.updated)
        assertTrue(third.merged.any { it.note == "fussy" })
    }

    @Test fun csvHasOneRowPerEvent() {
        val csv = Backup.toCsv(log, zone)
        assertEquals(log.size + 1, csv.trim().lines().size)
        assertTrue(csv.lines()[1].contains("2026-10-19 20:00"))
    }
}
