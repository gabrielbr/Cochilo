package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import java.time.ZoneId

/**
 * Where the baby slept (docs/places.md): crib, arms, sling, pram, car. Contact naps are normal in
 * the first months and babywearing is the brand's subject, so they're shown side by side, never
 * judged. A new sleep starts in the place used last (remember actions). Pure, so it is tested.
 */
object Places {
    val ALL = listOf(Kinds.CRIB, Kinds.ARMS, Kinds.SLING, Kinds.PRAM, Kinds.CAR)

    /** The place of the latest sleep that has one. */
    fun last(log: List<Event>): String? =
        log.filter { it.type == EventType.SLEEP && it.kind in ALL }.maxByOrNull { it.start }?.kind

    /** Naps (not nights) by place over the last [days] days, most first; places with none left out. */
    fun napsByPlace(log: List<Event>, zone: ZoneId, now: Long, days: Int = 7): List<Pair<String, Int>> =
        log.asSequence()
            .filter { it.type == EventType.SLEEP && it.kind in ALL && it.start >= now - days * DAY && !it.isNightSleep(zone) }
            .groupingBy { it.kind!! }.eachCount()
            .toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { ALL.indexOf(it.first) })
}
