package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType

/**
 * Repairs the log after two phones meet (0.64.0, docs/sync.md). Pure, tested.
 *
 * The owner's screenshot: both phones started the night at 19:15, so the merged log held two
 * sleeps for one night; one got its Woke up, the other stayed open. Cochilo then said the baby had
 * slept 21 hours, and the Day ring summed 25.
 * - **Twins:** two sleeps that start within [TWIN_MS] of each other and overlap are one sleep. The
 *   finished one stays (else the most recently edited); the other is deleted, and the deletion
 *   syncs, so both phones keep the same one.
 * - **Stale:** a sleep still open when a later sleep has started can't still be running. It ends
 *   where the next sleep begins.
 */
object SleepRepair {
    const val TWIN_MS = 5 * MINUTE

    data class Fix(val delete: List<Event>, val update: List<Event>) {
        val none get() = delete.isEmpty() && update.isEmpty()
    }

    fun fix(events: List<Event>, now: Long): Fix {
        val sleeps = events.filter { it.type == EventType.SLEEP }.sortedWith(compareBy({ it.start }, { it.id }))
        val gone = mutableSetOf<String>()
        for (i in sleeps.indices) {
            val a = sleeps[i]
            if (a.id in gone) continue
            for (j in i + 1 until sleeps.size) {
                val b = sleeps[j]
                if (b.start - a.start > TWIN_MS) break
                if (b.id in gone) continue
                val overlap = b.start < (a.end ?: Long.MAX_VALUE)
                if (!overlap) continue
                val loser = loser(a, b)
                gone += loser.id
                if (loser.id == a.id) break
            }
        }
        val kept = sleeps.filter { it.id !in gone }
        val update = kept.withIndex()
            .filter { (i, e) -> e.end == null && i < kept.lastIndex }
            .map { (i, e) -> e.copy(end = maxOf(e.start + MINUTE, kept[i + 1].start), updatedAt = now) }
        return Fix(sleeps.filter { it.id in gone }, update)
    }

    /** Of two twins, the one to drop: the open one, else the older edit, else the larger id. */
    private fun loser(a: Event, b: Event): Event = when {
        (a.end == null) != (b.end == null) -> if (a.end == null) a else b
        a.updatedAt != b.updatedAt -> if (a.updatedAt < b.updatedAt) a else b
        else -> if (a.id > b.id) a else b
    }

    /** How long the sleeps in [from, to) cover, overlaps counted once; a running one up to [now]. */
    fun sleptIn(events: List<Event>, from: Long, to: Long, now: Long): Long {
        val spans = events.filter { it.type == EventType.SLEEP }
            .map { maxOf(it.start, from) to minOf(it.end ?: now, to, now) }
            .filter { (a, b) -> b > a }.sortedBy { it.first }
        var total = 0L
        var curStart = Long.MIN_VALUE
        var curEnd = Long.MIN_VALUE
        for ((a, b) in spans) {
            if (a > curEnd) { if (curEnd > curStart) total += curEnd - curStart; curStart = a; curEnd = b }
            else curEnd = maxOf(curEnd, b)
        }
        if (curEnd > curStart) total += curEnd - curStart
        return total
    }
}
