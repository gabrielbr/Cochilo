package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import java.time.ZoneId

/**
 * Tatá's suggestions (docs/tata-slings/in-app-marketing.md §2, §3.2): when the baby's own log or
 * age makes one of the shop's products genuinely useful, one quiet note in Patterns, with the
 * reason first. Pure rules, unit-tested like the predictor, so every ground rule of the
 * non-intrusive contract is a test, not a hope.
 *
 * A moment shows only once Tatá has approved it (the catalog's `approved`), and only for a week;
 * then it retires for good, as does one the parent dismissed.
 */
object Suggestions {
    /** The moments a note can be about. Order is priority when more than one fits. */
    enum class Moment {
        /** 0–12 weeks, 5+ naps in arms this week: the sling keeps hands free. */
        ARMS_NAPS,
        /** A steady bedtime routine (5+ this week): the Naninha can join it. */
        ROUTINE_STEADY,
        /** Little daylight (under 20 min a day over 5+ logged days): a walk in the sling counts. */
        LITTLE_DAYLIGHT,
        /** 26–30 weeks: play and exploring (the TatáT.O. line). */
        SIX_MONTHS,
    }

    data class Suggestion(val moment: Moment, val product: String, val count: Int? = null)

    /** What the rules read. [shown]: when each moment first showed. [approved]/[productOf]: the catalog. */
    data class Input(
        val log: List<Event>,
        val ageWeeks: Int,
        val now: Long,
        val zone: ZoneId,
        val sleeping: Boolean,
        val firstSeen: Long,
        val enabled: Boolean,
        val approved: Set<Moment>,
        val productOf: Map<Moment, String>,
        val dismissed: Set<Moment>,
        val shown: Map<Moment, Long>,
    )

    const val WEEK = 7 * DAY
    const val ARMS_NAPS_MIN = 5
    const val ARMS_NAPS_UNTIL_WEEKS = 13
    const val ROUTINES_MIN = 5
    const val DAYLIGHT_LOW_MIN = 20
    const val DAYLIGHT_DAYS_MIN = 5
    val SIX_MONTHS_WEEKS = 26..30

    /** The one note to show now, or null. Every ground rule is checked before any moment. */
    fun pick(i: Input): Suggestion? {
        // Never off, never while the baby sleeps, never at night, never in the first week.
        if (!i.enabled || i.sleeping || isNightStart(i.now, i.zone) || i.now - i.firstSeen < WEEK) return null
        // A note that started less than a week ago keeps its place (unless dismissed).
        val active = i.shown.filter { (m, at) -> i.now - at < WEEK && m !in i.dismissed && m in i.approved }
            .maxByOrNull { it.value }?.key
        if (active != null) return fires(active, i)?.copy(product = i.productOf[active] ?: return null)
            ?: Suggestion(active, i.productOf[active] ?: return null)
        // At most one new note a week, even after a dismissal.
        val lastStart = i.shown.maxByOrNull { it.value }
        if (lastStart != null && i.now - lastStart.value < WEEK) return null
        val lastProduct = lastStart?.key?.let { i.productOf[it] }
        return Moment.entries.asSequence()
            .filter { it in i.approved && it !in i.dismissed && it !in i.shown }
            .filter { i.productOf[it] != null && i.productOf[it] != lastProduct }
            .mapNotNull { m -> fires(m, i)?.copy(product = i.productOf.getValue(m)) }
            .firstOrNull()
    }

    /** Whether [m]'s rule holds for this log and age (the product is filled in by [pick]). */
    fun fires(m: Moment, i: Input): Suggestion? {
        val since = i.now - WEEK
        return when (m) {
            Moment.ARMS_NAPS -> {
                if (i.ageWeeks >= ARMS_NAPS_UNTIL_WEEKS) return null
                val n = i.log.count { it.type == EventType.SLEEP && it.kind == Kinds.ARMS && it.start >= since && it.isNap(i.zone) }
                if (n >= ARMS_NAPS_MIN) Suggestion(m, "", n) else null
            }
            Moment.ROUTINE_STEADY -> {
                val n = i.log.count { it.type == EventType.ROUTINE && it.kind == Kinds.BEDTIME_ROUTINE && it.start >= since }
                if (n >= ROUTINES_MIN) Suggestion(m, "", n) else null
            }
            Moment.LITTLE_DAYLIGHT -> {
                val days = Stats(i.zone).days(i.log, localDate(i.now, i.zone).minusDays(1), 7, i.now)
                val logged = days.filter { it.totalSleepMin > 0 }
                if (logged.size < DAYLIGHT_DAYS_MIN) return null
                val avg = logged.sumOf { it.daylightMin } / logged.size
                if (avg < DAYLIGHT_LOW_MIN) Suggestion(m, "", avg) else null
            }
            Moment.SIX_MONTHS -> if (i.ageWeeks in SIX_MONTHS_WEEKS) Suggestion(m, "") else null
        }
    }
}
