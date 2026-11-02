package br.gabriel.cochilo.domain

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

class EarlierTest {
    private val now = 1_800_000_000_000L
    private fun ago(min: Int) = now - min * MINUTE

    @Test fun startOffersAllWhenTheLastWakeIsLongAgo() {
        val log = listOf(Event(type = EventType.SLEEP, start = ago(200), end = ago(120)))
        assertEquals(listOf(5, 10, 15), Earlier.forStart(log, now))
    }

    @Test fun startNeverBeforeTheLastWake() {
        val log = listOf(Event(type = EventType.SLEEP, start = ago(90), end = ago(12)))
        assertEquals(listOf(5, 10), Earlier.forStart(log, now))
    }

    @Test fun nothingToStartWhileASleepRuns() {
        assertEquals(emptyList<Int>(), Earlier.forStart(listOf(Event(type = EventType.SLEEP, start = ago(30))), now))
    }

    @Test fun wakeNeverBeforeTheSleepBegan() {
        assertEquals(listOf(5), Earlier.forWake(listOf(Event(type = EventType.SLEEP, start = ago(8))), now))
        assertEquals(listOf(5, 10, 15), Earlier.forWake(listOf(Event(type = EventType.SLEEP, start = ago(60))), now))
        assertEquals(emptyList<Int>(), Earlier.forWake(emptyList(), now))
    }
}
