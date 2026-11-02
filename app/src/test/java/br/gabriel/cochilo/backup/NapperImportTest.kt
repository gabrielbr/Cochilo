package br.gabriel.cochilo.backup

import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.OffsetDateTime

class NapperImportTest {
    private val labels = NapperLabels("Held or worn", "Swing", "In bed", "While nursing", "Breast milk", "Formula", "Medicine") { l, r -> "Left $l min, right $r min" }
    private val header = "start,end,category,overallHappiness,babyMoodOnWakeUp,babyMoodOnSleep,diaperWeight,diaperContent,breastLeftSeconds,breastRightSeconds,amountPumpedLeft,amountPumpedRight,feedingAmount,temperature,bottleFeedingType,comment,numberOfPauses,pausedMinutes,units,howBabySlept,howBabyWoke,timeToFallAsleep,createdAt,updatedAt"

    /** One Napper row; only the columns a test cares about. */
    private fun row(
        start: String, end: String = start, cat: String, diaper: String = "", left: String = "", right: String = "",
        amount: String = "", bottle: String = "", comment: String = "", slept: String = "", created: String = "2026-09-15T11:00:00.000Z",
    ) = listOf(start, end, cat, "", "", "", "", diaper, left, right, "", "", amount, "", bottle, q(comment), "", "", "", slept, "", "", created, created)
        .joinToString(",")

    private fun q(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    private fun t(s: String) = OffsetDateTime.parse(s).toInstant().toEpochMilli()
    private fun csv(vararg rows: String) = (listOf(header) + rows).joinToString("\n")

    @Test fun recognises_the_export() {
        assertTrue(NapperImport.looksLikeNapper("\uFEFF" + csv()))
        assertFalse(NapperImport.looksLikeNapper("{\"format\":\"cochilo-backup\"}"))
        assertFalse(NapperImport.looksLikeNapper("id,type,kind,start,end"))
    }

    @Test fun a_nap_is_a_sleep_with_where_it_happened() {
        val r = NapperImport.parse(csv(row("2026-09-04T10:00:00.000-03:00", "2026-09-04T12:30:00.000-03:00", "NAP", slept = "WORN_OR_HELD", comment = "Começou no sling, precisou de colo")), labels)
        val e = r.events.single()
        assertEquals(EventType.SLEEP, e.type)
        assertEquals(t("2026-09-04T10:00:00.000-03:00"), e.start)
        assertEquals(t("2026-09-04T12:30:00.000-03:00"), e.end)
        assertEquals("Held or worn · Começou no sling, precisou de colo", e.note)
    }

    @Test fun a_night_is_bedtime_to_wake_up_split_by_wakings() {
        val r = NapperImport.parse(
            csv(
                row("2026-09-14T21:00:00.000-03:00", cat = "BED_TIME", slept = "IN_BED"),
                row("2026-09-14T23:30:00.000-03:00", "2026-09-14T23:45:00.000-03:00", "NIGHT_WAKING", comment = "Voltou a dormir no colo"),
                row("2026-09-15T03:00:00.000-03:00", "2026-09-15T03:30:00.000-03:00", "NIGHT_WAKING"),
                row("2026-09-15T06:30:00.000-03:00", cat = "WOKE_UP"),
            ),
            labels,
        )
        val sleeps = r.events.filter { it.type == EventType.SLEEP }
        assertEquals(3, sleeps.size)
        assertEquals(t("2026-09-14T21:00:00.000-03:00") to t("2026-09-14T23:30:00.000-03:00"), sleeps[0].start to sleeps[0].end)
        assertEquals(t("2026-09-14T23:45:00.000-03:00") to t("2026-09-15T03:00:00.000-03:00"), sleeps[1].start to sleeps[1].end)
        assertEquals(t("2026-09-15T03:30:00.000-03:00") to t("2026-09-15T06:30:00.000-03:00"), sleeps[2].start to sleeps[2].end)
        assertEquals("In bed", sleeps[0].note)
        assertNull(sleeps[1].note)
        // The waking's own words stay, as a note at its time.
        assertEquals("Voltou a dormir no colo", r.events.single { it.type == EventType.NOTE }.note)
    }

    @Test fun a_bedtime_without_a_wake_up_is_skipped_not_guessed() {
        val r = NapperImport.parse(csv(row("2026-09-14T21:00:00.000-03:00", cat = "BED_TIME")), labels)
        assertTrue(r.events.isEmpty())
        assertEquals(1, r.skipped)
    }

    @Test fun nursing_takes_the_longer_side_and_notes_both() {
        val r = NapperImport.parse(
            csv(
                row("2026-09-04T09:45:00.000-03:00", "2026-09-04T09:55:00.000-03:00", "NURSING", left = "300", right = "540"),
                row("2026-09-04T12:45:00.000-03:00", "2026-09-04T13:20:00.000-03:00", "NURSING", left = "2100", right = "0"),
                row("2026-09-04T15:00:00.000-03:00", "2026-09-04T15:10:00.000-03:00", "NURSING"),
            ),
            labels,
        )
        val (both, left, none) = r.events
        assertEquals(Kinds.BREAST_RIGHT, both.kind)
        assertEquals("Left 5 min, right 9 min", both.note)
        assertEquals(Kinds.BREAST_LEFT, left.kind)
        assertNull(left.note)
        assertNull(none.kind)
        assertEquals(t("2026-09-04T13:20:00.000-03:00"), left.end)
    }

    @Test fun bottles_diapers_and_medicine() {
        val r = NapperImport.parse(
            csv(
                row("2026-09-14T07:35:00.000-03:00", cat = "BOTTLE", bottle = "BREAST_MILK", amount = "90"),
                row("2026-09-15T07:00:00.000-03:00", cat = "CHANGED_DIAPER", diaper = "MIXED"),
                row("2026-09-15T08:00:00.000-03:00", cat = "CHANGED_DIAPER", diaper = "WET"),
                row("2026-09-15T09:00:00.000-03:00", cat = "CHANGED_DIAPER"),
                row("2026-09-16T17:15:07.000-03:00", cat = "MEDICINE", comment = "Kolikdoron "),
            ),
            labels,
        )
        val (bottle, mixed, wet, plain, med) = r.events
        assertEquals(EventType.FEED, bottle.type)
        assertEquals(Kinds.BOTTLE, bottle.kind)
        assertEquals(90, bottle.amountMl)
        assertEquals(bottle.start, bottle.end) // a duration type always has an end, never a running timer
        assertEquals("Breast milk", bottle.note)
        assertEquals(Kinds.BOTH, mixed.kind)
        assertEquals(Kinds.WET, wet.kind)
        assertNull(plain.kind)
        assertNull(plain.end)
        assertEquals(EventType.NOTE, med.type)
        assertEquals("Medicine · Kolikdoron", med.note)
    }

    @Test fun importing_twice_adds_nothing() {
        val text = csv(
            row("2026-09-04T08:30:00.000-03:00", "2026-09-04T09:00:00.000-03:00", "NAP", slept = "SWING"),
            row("2026-09-15T07:00:00.000-03:00", cat = "CHANGED_DIAPER", diaper = "MIXED"),
        )
        val first = Backup.merge(emptyList(), NapperImport.parse(text, labels).events)
        val again = Backup.merge(first.merged, NapperImport.parse(text, labels).events)
        assertEquals(2, first.added)
        assertEquals(0, again.added)
        assertEquals(0, again.updated)
    }

    @Test fun quoted_fields_keep_commas_quotes_and_line_breaks() {
        val rows = Csv.parse("a,b,c\n1,\"x, \"\"y\"\"\nz\",3\r\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("1", "x, \"y\"\nz", "3")), rows)
    }

    /**
     * A real export, if one is given (NAPPER_CSV=/path/to/…-logs-….csv). Family data never goes in
     * the repo, so CI skips this.
     */
    @Test fun a_real_export_imports_cleanly() {
        val path = System.getenv("NAPPER_CSV")
        assumeTrue(path != null && File(path).exists())
        val text = File(path!!).readText()
        assertTrue(NapperImport.looksLikeNapper(text))
        val r = NapperImport.parse(text, labels)
        val byType = r.events.groupingBy { it.type }.eachCount()
        println("Napper import: ${r.events.size} entries $byType, skipped ${r.skipped}")
        assertTrue(r.events.isNotEmpty())
        assertTrue(r.events.none { it.type.hasDuration && it.end == null })
        assertTrue(r.events.all { it.end == null || it.end!! >= it.start })
        assertEquals(r.events.size, r.events.map { it.id }.toSet().size)
    }
}
