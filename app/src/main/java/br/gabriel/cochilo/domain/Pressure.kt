package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import kotlin.math.exp
import kotlin.math.ln

/**
 * Sleep pressure, drawn live on the Day tab (0.54.0, docs/prediction.md): process S of the
 * two-process model (research §2.1, §7.2). It builds while awake and drains while asleep:
 * - awake: `S = 1 − (1 − S₀)·e^(−t/τw)`;
 * - asleep: `S = S₀·e^(−t/τs)`.
 *
 * The time constants come from this baby's own usual window and nap: τw so that pressure goes
 * from empty to [ASLEEP_AT] in one usual window, τs so that a usual nap drains it to a quarter.
 * The line is shown as a share of [ASLEEP_AT], so 100% is "about when the baby usually falls
 * asleep". It's a model, said so on screen, never a measurement.
 */
object Pressure {
    /** The level at which, in the model, the baby usually falls asleep. */
    const val ASLEEP_AT = 0.8

    data class Point(val at: Long, val percent: Int)

    fun tauWake(usualWindowMinutes: Double) = usualWindowMinutes / -ln(1 - ASLEEP_AT)
    fun tauSleep(usualNapMinutes: Double) = usualNapMinutes / ln(4.0)

    /**
     * The curve from [from] to [to] (and no later than [now]), every [stepMinutes]. It starts
     * empty after the first sleep it sees; before any sleep it has nothing to say.
     */
    fun curve(events: List<Event>, from: Long, to: Long, now: Long, usualWindowMinutes: Double, usualNapMinutes: Double, stepMinutes: Int = 5): List<Point> {
        val sleeps = events.filter { it.type == EventType.SLEEP }.sortedBy { it.start }
        if (sleeps.isEmpty()) return emptyList()
        val tw = tauWake(usualWindowMinutes)
        val ts = tauSleep(usualNapMinutes)
        val end = minOf(to, now)
        // Walk from the first sleep that starts before [from] (or the first sleep), so the level at
        // [from] already carries the night before.
        var t = sleeps.lastOrNull { it.start <= from }?.start ?: sleeps.first().start
        var s = ASLEEP_AT
        val out = mutableListOf<Point>()
        val step = stepMinutes * MINUTE
        while (t <= end) {
            if (t >= from) out += Point(t, (s / ASLEEP_AT * 100).toInt().coerceIn(0, 150))
            val asleep = sleeps.any { it.start <= t && (it.end ?: now) > t }
            val dt = stepMinutes.toDouble()
            s = if (asleep) s * exp(-dt / ts) else 1 - (1 - s) * exp(-dt / tw)
            t += step
        }
        return out
    }

    /** Whether the line is going up (awake) at [now]. */
    fun rising(events: List<Event>, now: Long) = events.none { it.type == EventType.SLEEP && it.start <= now && (it.end ?: Long.MAX_VALUE) > now }

    /**
     * Today's curve, from midnight to [now], with this baby's usual window (the median of its
     * daytime windows over 14 days, else the age's) and usual nap ([DayPlan.napMinutes]).
     */
    fun today(events: List<Event>, ageWeeks: Int, now: Long, zone: java.time.ZoneId): List<Point> {
        val from = startOfDay(localDate(now, zone), zone)
        val windows = Predictor(zone).windows(events, now).filter { !it.beforeBed }.map { it.minutes }
        val window = windows.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }
            ?: Reference.priorFor(ageWeeks).wakeForSlot(1, false).toDouble()
        val nap = DayPlan(zone).napMinutes(events.filter { it.type == EventType.SLEEP }, 1, ageWeeks).toDouble()
        // The line starts at this morning's wake (0.56.0): before it is the night, whose wakings
        // made the midnight level look arbitrary. No morning wake yet: nothing to draw.
        val wake = morningWake(events, from, now, zone) ?: return emptyList()
        return curve(events, wake, from + DAY, now, window, nap)
    }

    /** When the night ended today: the end of today's last night sleep, if it has ended. */
    fun morningWake(events: List<Event>, from: Long, now: Long, zone: java.time.ZoneId): Long? =
        events.filter { it.type == EventType.SLEEP && it.isNightSleep(zone) && it.end != null && it.end in from..now }
            .maxOfOrNull { it.end!! }
}
