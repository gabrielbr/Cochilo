package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import kotlin.math.roundToInt

/**
 * Drag to adjust (docs/flows.md): on the Day tab's timeline, press and hold a finished sleep, then
 * drag down to make it end later or up to make it end sooner, in 5-minute steps. Since 0.53.0 its
 * start time (on the left) drags the same way, for a sleep that began earlier or later than logged.
 * Pure rules here; the gesture is in `ui/kit/Day.kt`.
 */
object Adjust {
    const val STEP_MINUTES = 5

    /** A sleep is never shorter than this after adjusting. */
    const val MIN_SLEEP_MINUTES = 5

    /** How many steps a drag of [dragPx] is, at [stepPx] per step (down = positive = later). */
    fun steps(dragPx: Float, stepPx: Float): Int = if (stepPx <= 0f) 0 else (dragPx / stepPx).roundToInt()

    /** Only a finished sleep can be dragged. */
    fun canAdjust(e: Event): Boolean = e.type == EventType.SLEEP && e.end != null

    /**
     * The new end after moving [e]'s end by [steps] steps: never before start + [MIN_SLEEP_MINUTES],
     * never after [now], never into the next sleep ([nextStart]). Null when nothing changes.
     */
    fun newEnd(e: Event, steps: Int, now: Long, nextStart: Long?): Long? {
        val end = e.end ?: return null
        if (steps == 0) return null
        val earliest = e.start + MIN_SLEEP_MINUTES * MINUTE
        val latest = minOf(now, nextStart ?: Long.MAX_VALUE)
        val wanted = end + steps * STEP_MINUTES * MINUTE
        val clamped = wanted.coerceIn(earliest, maxOf(earliest, latest))
        return clamped.takeIf { it != end }
    }

    /** Any sleep's start can be dragged, a running one too (0.53.0). */
    fun canAdjustStart(e: Event): Boolean = e.type == EventType.SLEEP

    /**
     * The new start after moving [e]'s start by [steps] steps (0.53.0): never into the sleep before
     * it (its end, [prevEnd]), never later than its end (or [now], while it runs) minus
     * [MIN_SLEEP_MINUTES]. Null when nothing changes.
     */
    fun newStart(e: Event, steps: Int, now: Long, prevEnd: Long?): Long? {
        if (steps == 0) return null
        val latest = (e.end ?: now) - MIN_SLEEP_MINUTES * MINUTE
        val earliest = minOf(prevEnd ?: Long.MIN_VALUE, latest)
        val wanted = e.start + steps * STEP_MINUTES * MINUTE
        return wanted.coerceIn(earliest, latest).takeIf { it != e.start }
    }
}
