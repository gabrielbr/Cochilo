package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType

/**
 * "Fell asleep a few minutes ago" (docs/flows.md, Earlier): the baby fell asleep in your arms, or
 * woke, before you could reach the phone. Under Start sleep / Woke up, small chips log it 5, 10 or
 * 15 minutes ago in one tap. Only the chips that make sense are offered: a sleep can't start
 * before the last wake, and a wake can't come before the sleep began. Pure, so it is tested.
 */
object Earlier {
    val MINUTES = listOf(5, 10, 15)

    /** The minutes-ago a new sleep could start at, now (after the last sleep's end). */
    fun forStart(log: List<Event>, now: Long): List<Int> {
        if (log.any { it.type == EventType.SLEEP && it.end == null }) return emptyList()
        val lastEnd = log.filter { it.type == EventType.SLEEP }.mapNotNull { it.end }.maxOrNull() ?: Long.MIN_VALUE
        return MINUTES.filter { now - it * MINUTE > lastEnd }
    }

    /** The minutes-ago the running sleep could end at (after it began, leaving a minute). */
    fun forWake(log: List<Event>, now: Long): List<Int> {
        val running = log.filter { it.type == EventType.SLEEP && it.end == null }.maxByOrNull { it.start } ?: return emptyList()
        return MINUTES.filter { now - it * MINUTE > running.start + MINUTE }
    }
}
