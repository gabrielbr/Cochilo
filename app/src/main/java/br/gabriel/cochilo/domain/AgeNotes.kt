package br.gabriel.cochilo.domain

import java.time.ZoneId

/**
 * Notes from Tatá by age (docs/tata-slings/in-app-marketing.md §3.1): one short, signed note for
 * each age band from birth to two years, in Patterns. Content first: most carry no product. A
 * note shows only once Tatá has written or approved it (the catalog's `approved`), and stays for
 * its band until the parent closes it.
 */
object AgeNotes {
    /** Twelve bands, by (corrected) age in weeks: [fromWeeks, untilWeeks). */
    enum class Band(val fromWeeks: Int, val untilWeeks: Int) {
        M0_1(0, 4), M1_2(4, 8), M2_3(8, 13), M3_4(13, 17), M4_6(17, 26), M6_8(26, 35),
        M8_10(35, 43), M10_12(43, 52), M12_15(52, 65), M15_18(65, 78), M18_21(78, 91), M21_24(91, 104),
    }

    fun bandFor(ageWeeks: Int): Band? = Band.entries.firstOrNull { ageWeeks in it.fromWeeks until it.untilWeeks }

    /**
     * The note to show now, or null: the baby's band, approved, not closed; never while the baby
     * sleeps or at night (19:00–07:00), like every Tatá note.
     */
    fun pick(ageWeeks: Int, now: Long, zone: ZoneId, sleeping: Boolean, approved: Set<Band>, closed: Set<Band>): Band? {
        if (sleeping || isNightStart(now, zone)) return null
        val band = bandFor(ageWeeks) ?: return null
        return band.takeIf { it in approved && it !in closed }
    }
}
