package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds

/**
 * Details after (docs/flows.md, 0.28.0): an entry starts with one tap and nothing else; what it
 * was (where the baby slept, which breast or a bottle, how much was pumped) is asked once it ends,
 * in one tap, with the last answer marked. Pure rules here; the sheet is `ui/screens/Details.kt`.
 */
object Details {
    enum class Ask {
        /** Where the sleep happened: crib, arms, sling, pram, car seat. */
        PLACE,
        /** Which kind it was: a feed's breast, bottle or solids; daylight outdoors or by a window. */
        KIND,
        /** How many ml: pumped milk, or a bottle. */
        AMOUNT,
    }

    /** An entry that ended this long ago is still worth asking about when the app opens. */
    const val ASK_WITHIN_MS = 2 * HOUR

    /** What's still worth asking about [e] once it has ended, or null. */
    fun askAfter(e: Event): Ask? = when {
        e.end == null -> null
        e.type == EventType.SLEEP && e.kind == null -> Ask.PLACE
        e.type == EventType.FEED && e.kind == null -> Ask.KIND
        e.type == EventType.FEED && e.kind == Kinds.BOTTLE && e.amountMl == null -> Ask.AMOUNT
        e.type == EventType.DAYLIGHT && e.kind == null -> Ask.KIND
        e.type == EventType.PUMP && e.amountMl == null -> Ask.AMOUNT
        else -> null
    }

    /** The options for a PLACE or KIND question. */
    fun options(e: Event): List<String> = Kinds.forType(e.type)

    /** The answer used last for this question (remember actions), to mark it. */
    fun lastKind(e: Event, log: List<Event>): String? =
        log.filter { it.type == e.type && it.id != e.id && it.kind != null && it.kind in options(e) }.maxByOrNull { it.start }?.kind

    fun lastAmount(e: Event, log: List<Event>): Int? =
        log.filter { it.type == e.type && it.id != e.id && it.amountMl != null && (e.type != EventType.FEED || it.kind == e.kind) }
            .maxByOrNull { it.start }?.amountMl

    /**
     * The entry to ask about when the app opens: the newest one that ended within
     * [ASK_WITHIN_MS] (from the widget, the notification or the tile, where no sheet can open),
     * still missing its detail, and not asked before ([asked]).
     */
    fun pending(log: List<Event>, now: Long, asked: Set<String>): Event? =
        log.filter { it.end != null && it.end in (now - ASK_WITHIN_MS)..now && it.id !in asked }
            .maxByOrNull { it.end!! }
            ?.takeIf { askAfter(it) != null }
}
