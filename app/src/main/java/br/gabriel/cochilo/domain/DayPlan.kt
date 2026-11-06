package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import java.time.ZoneId

/**
 * The rest of today, projected (0.49.0, docs/prediction.md): every nap still to come and bedtime,
 * each with a range. Pure Kotlin, tested in DayPlanTest.
 *
 * It repeats the predictor the way the day would go:
 * 1. Predict the next sleep from the log (the card's own guess, unchanged).
 * 2. Suppose a nap of the baby's usual length for that place in the day happens then.
 * 3. Predict again from its end.
 * 4. Stop at bedtime, at the age's usual number of naps (then bedtime), or at the end of the day.
 *
 * The further ahead, the wider the range: each step adds half of its own range to the next one,
 * so a guess for the afternoon says how unsure it is. Every new log entry replaces the projection
 * with what really happened.
 */
class DayPlan(private val zone: ZoneId, private val lookbackDays: Int = 14) {

    /** One projected sleep. [end] is the expected end for a nap, the day's end for bedtime. */
    data class Sleep(val next: Predictor.Next, val at: Long, val windowStart: Long, val windowEnd: Long, val end: Long)

    /** The projection, and whether it comes from the age alone (too little log yet). */
    data class Plan(val sleeps: List<Sleep>, val napMinutes: Int, val fromAge: Boolean) {
        val naps get() = sleeps.filter { it.next == Predictor.Next.NAP }
        val bedtime get() = sleeps.firstOrNull { it.next == Predictor.Next.BEDTIME }
    }

    /** Null at night (asleep for the night, or awake in the night) and before anything is logged. */
    fun of(events: List<Event>, ageWeeks: Int, now: Long): Plan? {
        if (Nights.openNight(events, now) != null) return null
        val predictor = Predictor(zone, lookbackDays)
        val prior = Reference.priorFor(ageWeeks)
        val dayEnd = startOfDay(localDate(now, zone).plusDays(1), zone)
        val sim = events.filter { it.type == EventType.SLEEP }.toMutableList()
        val out = mutableListOf<Sleep>()
        var t = now
        var widen = 0L
        var fromAge = false
        // The first sleep is always the live guess, the one Today's card shows (0.62.0): no cap on
        // the day's naps for it. Capped, a baby who had more naps than the age's usual got bedtime
        // projected while the card said "next nap", and the ring breathed the night's moon.
        var r = predictor.predict(sim, ageWeeks, t)
        // After it, the cap: the age's naps, or one more than already happened today.
        var cap = prior.naps
        for (step in 0 until prior.naps + 2) {
            when (val p = r) {
                Predictor.Result.NeedData -> return null
                is Predictor.Result.Sleeping -> {
                    if (p.isNight) return null
                    // A nap is running: it ends after its usual length (or in a little while).
                    val running = sim.last { it.end == null }
                    val end = maxOf(p.since + napMinutes(sim, slotOf(sim, running), ageWeeks) * MINUTE, t + RUNNING_MIN * MINUTE)
                    sim[sim.indexOf(running)] = running.copy(end = end, night = false)
                    t = end
                }
                is Predictor.Result.Awake -> {
                    if (step == 0 || out.isEmpty()) fromAge = p.personalMinutes == null
                    if (p.at >= dayEnd) break
                    if (out.isNotEmpty() && p.next == Predictor.Next.NAP && p.slot >= cap && prior.bedtimeMinute == null) break
                    if (out.isEmpty() && p.next == Predictor.Next.NAP) cap = maxOf(cap, p.slot + 1)
                    val half = (p.windowEnd - p.windowStart) / 2
                    if (p.next == Predictor.Next.BEDTIME) {
                        out += Sleep(p.next, p.at, p.windowStart - widen, p.windowEnd + widen, dayEnd)
                        return Plan(out, napMinutes(sim, p.slot, ageWeeks).toInt(), fromAge)
                    }
                    val len = napMinutes(sim, p.slot, ageWeeks) * MINUTE
                    out += Sleep(p.next, p.at, p.windowStart - widen, p.windowEnd + widen, p.at + len)
                    sim += Event(type = EventType.SLEEP, start = p.at, end = p.at + len, night = false)
                    widen = (widen + half / 2).coerceAtMost(MAX_WIDEN_MIN * MINUTE)
                    t = p.at + len
                }
            }
            r = predictor.predict(sim, ageWeeks, t, cap)
        }
        return if (out.isEmpty()) null else Plan(out, napMinutes(sim, 0, ageWeeks).toInt(), fromAge)
    }

    /** Which nap of the day [nap] is: 0 for the first after the morning wake. */
    private fun slotOf(sleeps: List<Event>, nap: Event): Int {
        val sorted = sleeps.sortedBy { it.start }
        var n = 0
        for (s in sorted) {
            if (s.id == nap.id) return n
            if (s.isNightSleep(zone)) n = 0 else n++
        }
        return n
    }

    /**
     * The baby's usual nap for this place in the day: the median of the last [lookbackDays] days'
     * naps in the same slot, else of all naps, else the age's usual length.
     */
    fun napMinutes(sleeps: List<Event>, slot: Int, ageWeeks: Int): Long {
        val sorted = sleeps.filter { it.type == EventType.SLEEP }.sortedBy { it.start }
        val since = (sorted.lastOrNull()?.start ?: 0L) - lookbackDays * DAY
        val bySlot = mutableListOf<Long>()
        val all = mutableListOf<Long>()
        var n = 0
        for (s in sorted) {
            if (s.isNightSleep(zone)) { n = 0; continue }
            val end = s.end
            if (end != null && s.start >= since) {
                val m = (end - s.start) / MINUTE
                all += m
                if (n == slot) bySlot += m
            }
            n++
        }
        fun median(xs: List<Long>) = xs.sorted().let { it[it.size / 2] }
        return when {
            bySlot.size >= MIN_SAMPLES -> median(bySlot)
            all.size >= MIN_SAMPLES -> median(all)
            ageWeeks < SIX_MONTHS_WEEKS -> YOUNG_NAP_MIN
            else -> OLDER_NAP_MIN
        }
    }

    companion object {
        /** A running nap is expected to last at least this much longer. */
        const val RUNNING_MIN = 10L
        /** A later guess's range grows by at most this much on each side. */
        const val MAX_WIDEN_MIN = 45L
        const val MIN_SAMPLES = 3
        /** With too little log: one sleep cycle before about 6 months, two after (research.md §3). */
        const val YOUNG_NAP_MIN = 45L
        const val OLDER_NAP_MIN = 75L
        const val SIX_MONTHS_WEEKS = 26
    }
}
