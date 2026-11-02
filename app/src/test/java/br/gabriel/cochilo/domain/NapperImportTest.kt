package br.gabriel.cochilo.domain

import br.gabriel.cochilo.backup.Backup
import br.gabriel.cochilo.backup.FileKind
import br.gabriel.cochilo.backup.NapperImport
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneId

/** Made-up rows in the exact column layout of Napper's "logs" export. */
class NapperImportTest {
    private val header = "start,end,category,overallHappiness,babyMoodOnWakeUp,babyMoodOnSleep,diaperWeight,diaperContent," +
        "breastLeftSeconds,breastRightSeconds,amountPumpedLeft,amountPumpedRight,feedingAmount,temperature,bottleFeedingType," +
        "comment,numberOfPauses,pausedMinutes,units,howBabySlept,howBabyWoke,timeToFallAsleep,createdAt,updatedAt"

    private fun row(start: String, end: String, cat: String, vararg cols: Pair<String, String>): String {
        val names = header.split(",")
        val values = MutableList(names.size) { "" }
        values[0] = start; values[1] = end; values[2] = cat
        cols.forEach { (k, v) -> values[names.indexOf(k)] = v }
        values[names.indexOf("updatedAt")] = "2026-10-21T12:00:00.000Z"
        return values.joinToString(",")
    }

    private fun t(s: String) = "2026-10-$s:00.000-03:00"

    private val csv = listOf(
        header,
        row(t("20T19:55"), t("20T19:55"), "BED_TIME"),
        row(t("20T20:31"), t("20T20:41"), "NIGHT_WAKING"),
        row(t("21T01:14"), t("21T01:15"), "NIGHT_WAKING"),
        row(t("21T05:50"), t("21T06:02"), "NIGHT_WAKING"),
        row(t("21T06:05"), t("21T06:05"), "WOKE_UP"),
        row(t("21T06:54"), t("21T08:37"), "NAP", "howBabySlept" to "WORN_OR_HELD", "numberOfPauses" to "1", "pausedMinutes" to "12"),
        row(t("21T07:00"), t("21T07:20"), "NURSING", "breastLeftSeconds" to "600", "breastRightSeconds" to "300"),
        row(t("21T09:00"), t("21T09:15"), "NURSING", "breastRightSeconds" to "900"),
        row(t("21T09:30"), t("21T09:30"), "CHANGED_DIAPER", "diaperContent" to "MIXED"),
        row(t("21T10:00"), t("21T10:00"), "MEDICINE", "comment" to "\"Luftal, 5 gotas\""),
        row(t("21T11:00"), t("21T11:00"), "BOTTLE", "bottleFeedingType" to "BREAST_MILK", "feedingAmount" to "120", "units" to "ml"),
        row(t("21T19:40"), t("21T19:40"), "BED_TIME"), // no wake-up yet: skipped
    ).joinToString("\n")

    private val labels = object : NapperImport.Labels {
        override fun fellAsleep(code: String) = if (code == "WORN_OR_HELD") "In arms" else null
        override fun pauses(count: Int, minutes: Int?) = "Pauses: $count ($minutes min)"
    }

    private fun ms(s: String) = OffsetDateTime.parse(t(s)).toInstant().toEpochMilli()

    @Test fun detectsTheFiles() {
        assertEquals(FileKind.NAPPER_LOGS, NapperImport.detect("﻿" + csv))
        assertEquals(FileKind.NAPPER_MEASUREMENTS, NapperImport.detect("dateOfMeasurement,weightKg,heightCm,headCircumferenceCm\n"))
        assertEquals(FileKind.COCHILO_BACKUP, NapperImport.detect("{\n  \"format\": \"cochilo-backup\""))
        assertEquals(FileKind.UNKNOWN, NapperImport.detect("a,b,c\n1,2,3"))
    }

    @Test fun rebuildsTheNightBetweenWakings() {
        val r = NapperImport.parseLogs(csv, labels)
        val nights = r.events.filter { it.type == EventType.SLEEP && it.kind == Kinds.NIGHT_SLEEP }
        assertEquals(listOf(ms("20T19:55") to ms("20T20:31"), ms("20T20:41") to ms("21T01:14"),
            ms("21T01:15") to ms("21T05:50"), ms("21T06:02") to ms("21T06:05")), nights.map { it.start to it.end })
        assertEquals(1, r.skipped) // the bedtime with no wake-up
    }

    @Test fun anEarlyNapStaysANap() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val nap = NapperImport.parseLogs(csv, labels).events.single { it.kind == Kinds.NAP_SLEEP }
        assertTrue(nap.isNap(zone)) // 06:54 is inside 19:00–07:00, but Napper said nap
        assertEquals("In arms · Pauses: 1 (12 min)", nap.note)
    }

    @Test fun mapsFeedsDiapersAndMedicine() {
        val e = NapperImport.parseLogs(csv, labels).events
        val feeds = e.filter { it.type == EventType.FEED }
        assertEquals(listOf(Kinds.BREAST_BOTH, Kinds.BREAST_RIGHT, Kinds.BOTTLE), feeds.map { it.kind })
        assertEquals(120, feeds.last().amountMl)
        assertEquals(Kinds.BOTH, e.single { it.type == EventType.DIAPER }.kind)
        val med = e.single { it.type == EventType.MEDICINE }
        assertEquals("Luftal, 5 gotas", med.note)
        assertNull(med.end)
    }

    @Test fun importingTwiceChangesNothing() {
        val first = NapperImport.parseLogs(csv, labels).events
        val again = NapperImport.parseLogs(csv, labels).events
        assertEquals(first.map { it.id }, again.map { it.id })
        val merged = Backup.merge(first, again)
        assertEquals(0, merged.added)
        assertEquals(first.size, merged.skipped)
    }
}
