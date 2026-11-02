package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds

// "Remember actions" (a project rule, AGENTS.md): what was done last is shown and offered first,
// so the person sees it (which breast was last, to decide whether to continue) and repeating it
// costs the fewest taps.

/** The latest entry of [type], or null. */
fun lastOf(log: List<Event>, type: EventType): Event? =
    log.filter { it.type == type }.maxByOrNull { it.start }

/** The kind used in the latest entry of [type] that has one (e.g. the breast of the last feed). */
fun lastKind(log: List<Event>, type: EventType): String? =
    log.filter { it.type == type && it.kind != null }.maxByOrNull { it.start }?.kind

/** The latest kind of [type] among [kinds] (e.g. the last breast, ignoring a later bottle). */
fun lastKindAmong(log: List<Event>, type: EventType, kinds: Set<String>): String? =
    if (kinds.isEmpty()) null
    else log.filter { it.type == type && it.kind in kinds }.maxByOrNull { it.start }?.kind

/**
 * A new entry prefilled from the last one of its type: the same kind (breast side, bottle, diaper
 * kind, daylight, routine), the same amount for a bottle or pumping, the same medicine name.
 * Times stay the new entry's own.
 */
fun Event.rememberedFrom(log: List<Event>): Event {
    val kind = lastKind(log, type) ?: this.kind
    val sameKind = log.filter { it.type == type && it.kind == kind }.maxByOrNull { it.start }
    val amount = when {
        type == EventType.PUMP -> lastOf(log, type)?.amountMl
        type == EventType.FEED && kind == Kinds.BOTTLE -> sameKind?.amountMl
        else -> null
    } ?: amountMl
    val note = if (type == EventType.MEDICINE) lastOf(log, type)?.note ?: note else note
    return copy(kind = kind, amountMl = amount, note = note)
}

fun otherBreast(kind: String?): String? = when (kind) {
    Kinds.BREAST_LEFT -> Kinds.BREAST_RIGHT
    Kinds.BREAST_RIGHT -> Kinds.BREAST_LEFT
    else -> null
}

/**
 * Slide-to-log: which of [count] options, laid side by side across a row [width] wide, the finger
 * at [x] is over, once it has moved at least [threshold] from where it went down ([startX]).
 * Null before that, and again if it slides back to where it started, which cancels.
 */
fun slideOption(x: Float, startX: Float, width: Float, threshold: Float, count: Int): Int? {
    if (count <= 0 || width <= 0f) return null
    if (kotlin.math.abs(x - startX) < threshold) return null
    return ((x.coerceIn(0f, width - 0.001f) / width) * count).toInt().coerceIn(0, count - 1)
}
