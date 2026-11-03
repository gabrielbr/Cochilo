package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds

/**
 * What the home-screen widget shows (docs/widget.md): asleep or awake and since when, the
 * breastfeed running now (if any), and the breast used last (remember actions: it gets the Last
 * mark). Pure, so it is tested without a phone.
 */
data class WidgetState(
    val hasBaby: Boolean,
    val asleep: Boolean,
    /** When the current sleep began, or when the last one ended; null before any sleep. */
    val since: Long?,
    /** The breastfeed running now, if any. */
    val feeding: Event?,
    /** The breast used last (left or right), even if a bottle came after. */
    val lastBreast: String?,
    /** Awake in the night (docs/nights.md): the sleep button reads Back to sleep. */
    val nightWaking: Boolean = false,
)

private val BREASTS = setOf(Kinds.BREAST_LEFT, Kinds.BREAST_RIGHT)

fun widgetState(events: List<Event>, hasBaby: Boolean, now: Long = System.currentTimeMillis()): WidgetState {
    val sleeping = events.filter { it.type == EventType.SLEEP && it.end == null }.maxByOrNull { it.start }
    val lastWake = events.filter { it.type == EventType.SLEEP }.mapNotNull { it.end }.maxOrNull()
    val feeding = events.filter { it.type == EventType.FEED && it.end == null && it.kind in BREASTS }.maxByOrNull { it.start }
    return WidgetState(
        hasBaby = hasBaby,
        asleep = sleeping != null,
        since = sleeping?.start ?: lastWake,
        feeding = feeding,
        lastBreast = lastKindAmong(events, EventType.FEED, BREASTS),
        nightWaking = sleeping == null && Nights.openNight(events, now) != null,
    )
}
