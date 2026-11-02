package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds

/**
 * The breast to offer next: the other side from the last breastfeed, as most parents alternate.
 * Null when no breastfeed is logged yet.
 */
fun suggestedBreast(log: List<Event>): String? =
    log.filter { it.type == EventType.FEED && (it.kind == Kinds.BREAST_LEFT || it.kind == Kinds.BREAST_RIGHT) }
        .maxByOrNull { it.start }
        ?.let { if (it.kind == Kinds.BREAST_LEFT) Kinds.BREAST_RIGHT else Kinds.BREAST_LEFT }

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
