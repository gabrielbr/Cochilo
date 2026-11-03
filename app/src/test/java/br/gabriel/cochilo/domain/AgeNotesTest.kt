package br.gabriel.cochilo.domain

import br.gabriel.cochilo.domain.AgeNotes.Band
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

class AgeNotesTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val day = LocalDateTime.of(2026, 10, 20, 14, 0).atZone(zone).toInstant().toEpochMilli()
    private val night = LocalDateTime.of(2026, 10, 20, 22, 0).atZone(zone).toInstant().toEpochMilli()
    private val all = Band.entries.toSet()

    @Test fun twelve_bands_cover_two_years_without_gaps() {
        assertEquals(12, Band.entries.size)
        assertEquals(0, Band.entries.first().fromWeeks)
        assertEquals(104, Band.entries.last().untilWeeks)
        Band.entries.zipWithNext().forEach { (a, b) -> assertEquals(a.untilWeeks, b.fromWeeks) }
        for (w in 0 until 104) assertTrue("week $w", AgeNotes.bandFor(w) != null)
        assertNull(AgeNotes.bandFor(104))
    }

    @Test fun the_babys_band_once_approved_until_closed() {
        assertEquals(Band.M1_2, AgeNotes.pick(6, day, zone, false, all, emptySet()))
        assertNull(AgeNotes.pick(6, day, zone, false, emptySet(), emptySet()))
        assertNull(AgeNotes.pick(6, day, zone, false, all, setOf(Band.M1_2)))
        assertEquals(Band.M2_3, AgeNotes.pick(9, day, zone, false, all, setOf(Band.M1_2)))
    }

    @Test fun never_asleep_or_at_night() {
        assertNull(AgeNotes.pick(6, day, zone, true, all, emptySet()))
        assertNull(AgeNotes.pick(6, night, zone, false, all, emptySet()))
    }

    /** Every band has its note in both languages, and none promises sleep or judges. */
    @Test fun the_notes_exist_and_never_promise_or_judge() {
        val banned = Regex("\\b(should|must|wrong|bad habit|regression|fault|spoil|will sleep|sleep through|guarantee|deveria|errad|ruim|regressão|culpa|mania|vício|mimad|vai dormir|garant)", RegexOption.IGNORE_CASE)
        for (path in listOf("src/main/res/values/strings.xml", "src/main/res/values-pt-rBR/strings.xml")) {
            val xml = File(path).readText()
            for (b in Band.entries) {
                val m = Regex("<string name=\"age_note_${b.name.lowercase()}\">([^<]*)</string>").find(xml)
                assertTrue("$path has age_note_${b.name.lowercase()}", m != null)
                assertTrue("${b.name} in $path: ${m!!.groupValues[1]}", !banned.containsMatchIn(m.groupValues[1]))
            }
        }
    }
}
