package br.gabriel.cochilo.domain

import kotlin.math.abs

/**
 * Where the day ring's floating marks go (0.58.0, docs/flows.md). Pure, so it's unit-tested.
 * Angles are degrees, clockwise from the top (midnight).
 *
 * - **Bubbles** (an entry's icon in its round) start at their entry's time and are pushed apart
 *   along the circle until none overlap. Three or more that would still crowd one spot become one
 *   stack bubble ("+2"), so the ring never turns into a pile.
 * - **Labels** (a sleep's start and end time, the hours) are kept only where they don't overlap
 *   one already kept, most important first.
 */
object RingLayout {
    /** An entry to place, at [angle]. */
    data class Mark(val id: String, val angle: Double)

    /** One bubble: one entry, or a stack of several (newest last), drawn at [angle]. */
    data class Bubble(val ids: List<String>, val angle: Double)

    /** A label to place: [width] in degrees along the circle; lower [priority] wins. */
    data class Label(val id: String, val angle: Double, val width: Double, val priority: Int)

    /** The shorter way round between two angles, 0..180. */
    fun distance(a: Double, b: Double): Double {
        val d = abs(norm(a) - norm(b)) % 360.0
        return if (d > 180) 360 - d else d
    }

    fun norm(a: Double): Double = ((a % 360.0) + 360.0) % 360.0

    /**
     * Bubbles for [marks], each at least [minSep] degrees from the next. Up to [maxSpread] marks
     * that fall together are spread side by side; more become one stack. When the ring has no
     * room left, the two closest bubbles are stacked until it has.
     */
    fun bubbles(marks: List<Mark>, minSep: Double, maxSpread: Int = 2): List<Bubble> {
        if (marks.isEmpty()) return emptyList()
        val sorted = marks.map { it.copy(angle = norm(it.angle)) }.sortedBy { it.angle }
        // Marks closer than minSep to the one before them fall together.
        val groups = mutableListOf(mutableListOf(sorted.first()))
        for (m in sorted.drop(1)) {
            if (m.angle - groups.last().last().angle < minSep) groups.last() += m else groups += mutableListOf(m)
        }
        var out = groups.flatMap { g ->
            val centre = g.sumOf { it.angle } / g.size
            if (g.size > maxSpread) listOf(Bubble(g.map { it.id }, centre))
            else g.mapIndexed { i, m -> Bubble(listOf(m.id), centre + (i - (g.size - 1) / 2.0) * minSep) }
        }.sortedBy { it.angle }
        // No room for all of them: stack the closest two until there is.
        while (out.size > 1 && out.size * minSep > 360.0) {
            val i = out.indices.minBy { distance(out[it].angle, out[(it + 1) % out.size].angle) }
            val j = (i + 1) % out.size
            val merged = Bubble(out[i].ids + out[j].ids, midpoint(out[i].angle, out[j].angle))
            out = (out.filterIndexed { k, _ -> k != i && k != j } + merged).sortedBy { norm(it.angle) }
        }
        // Push neighbours apart, a few passes, until no two overlap.
        val angles = out.map { it.angle }.toMutableList()
        repeat(RELAX_PASSES) {
            var moved = false
            for (i in angles.indices) {
                if (angles.size < 2) break
                val j = (i + 1) % angles.size
                if (i == j) continue
                val gap = norm(angles[j] - angles[i])
                if (gap < minSep) {
                    val push = (minSep - gap) / 2 + EPS
                    angles[i] -= push; angles[j] += push; moved = true
                }
            }
            if (!moved) return out.mapIndexed { i, b -> b.copy(angle = norm(angles[i])) }
        }
        return out.mapIndexed { i, b -> b.copy(angle = norm(angles[i])) }
    }

    /** The labels that fit, by priority, never overlapping one kept before. */
    fun labels(candidates: List<Label>, gap: Double = 0.0): List<Label> {
        val kept = mutableListOf<Label>()
        for (l in candidates.sortedWith(compareBy({ it.priority }, { it.angle }))) {
            if (kept.none { distance(it.angle, l.angle) < (it.width + l.width) / 2 + gap }) kept += l
        }
        return kept.sortedBy { norm(it.angle) }
    }

    /**
     * How a label at [angle] is turned so it runs along the circle and is never upside down:
     * along the tangent, flipped on the lower half.
     */
    fun labelRotation(angle: Double): Double {
        val a = norm(angle)
        return if (a > 90 && a < 270) norm(a + 180) else a
    }

    private fun midpoint(a: Double, b: Double): Double {
        val d = norm(b - a)
        return norm(a + d / 2)
    }

    private const val RELAX_PASSES = 40
    private const val EPS = 1e-6
}
