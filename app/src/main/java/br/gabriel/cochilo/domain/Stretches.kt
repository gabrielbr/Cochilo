package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType

/**
 * The day as its two main states (docs/flows.md, "Asleep and awake"): it alternates between
 * sleeps and awake stretches, and every other entry (a feed, a diaper, daylight…) happened
 * inside one of them. The timelines show each stretch as a main row with what happened during it
 * indented underneath. Pure, tested in `StretchesTest`.
 */
object Stretches {
    /**
     * One stretch. [start] null: awake since before the log knows. [end] null: still going.
     * [sleep] is the sleep entry for an asleep stretch. [during]: entries that started inside it,
     * newest first.
     */
    data class Stretch(val asleep: Boolean, val start: Long?, val end: Long?, val sleep: Event?, val during: List<Event>)

    /**
     * The stretches that touch [from]..[to] (a day), newest first, with each non-sleep entry of
     * [events] in the stretch where it started. [allSleeps]: every sleep in the log, so an awake
     * stretch can begin before the day (at the night's end) or run on after it.
     */
    fun of(events: List<Event>, allSleeps: List<Event>, from: Long, to: Long, now: Long): List<Stretch> {
        val sleeps = allSleeps.filter { it.type == EventType.SLEEP }.sortedBy { it.start }
        val horizon = minOf(to, now)
        val inDay = sleeps.filter { it.start < horizon && (it.end ?: now) > from }
        val frames = mutableListOf<Triple<Boolean, Long?, Long?>>() // asleep, start, end
        fun awake(start: Long?, end: Long?) {
            val s = start ?: Long.MIN_VALUE
            val e = end ?: Long.MAX_VALUE
            if (e > s && e > from && s < horizon) frames += Triple(false, start, end)
        }
        val before = sleeps.lastOrNull { it.start < (inDay.firstOrNull()?.start ?: horizon) && it !in inDay }
        if (inDay.isEmpty()) {
            val after = sleeps.firstOrNull { it.start >= horizon }
            awake(before?.end, after?.start?.takeIf { it < now })
        } else {
            awake(before?.end, inDay.first().start)
            inDay.forEachIndexed { i, s ->
                frames += Triple(true, s.start, s.end)
                val next = inDay.getOrNull(i + 1) ?: sleeps.firstOrNull { it.start > s.start && it !in inDay }
                if (s.end != null) awake(s.end, next?.start?.takeIf { it < now })
            }
        }
        val others = events.filter { it.type != EventType.SLEEP }
        val placed = mutableSetOf<String>()
        val stretches = frames.map { (asleep, start, end) ->
            val lo = start ?: Long.MIN_VALUE
            val hi = end ?: Long.MAX_VALUE
            val during = others.filter { it.id !in placed && it.start >= lo && it.start < hi }
            placed += during.map { it.id }
            Stretch(asleep, start, end, if (asleep) inDay.first { it.start == start } else null, during.sortedByDescending { it.start })
        }
        return stretches.sortedByDescending { it.start ?: Long.MIN_VALUE }
    }
}
