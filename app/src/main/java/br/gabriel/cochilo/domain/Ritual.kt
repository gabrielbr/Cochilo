package br.gabriel.cochilo.domain

/**
 * The ritual builder (docs/rituals.md): the steps of a nap or bedtime routine, written once by the
 * parent (preventive parent education has the strongest evidence, research §4) and ticked off in
 * the routine's sheet. Stored one step per line; pure, so it is tested.
 */
object Ritual {
    const val MAX_STEPS = 10
    const val MAX_LENGTH = 40

    fun decode(text: String?): List<String> =
        text.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_STEPS)

    fun encode(steps: List<String>): String =
        steps.map { it.trim().take(MAX_LENGTH) }.filter { it.isNotEmpty() }.take(MAX_STEPS).joinToString("\n")

    /** The routine's note from the ticked steps, in the order they were written ("Bath · Feed · Song"). */
    fun note(steps: List<String>, done: Set<Int>): String? =
        steps.filterIndexed { i, _ -> i in done }.joinToString(" · ").ifEmpty { null }
}
