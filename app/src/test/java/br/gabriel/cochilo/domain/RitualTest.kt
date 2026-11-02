package br.gabriel.cochilo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RitualTest {
    @Test fun roundTripsAndCleans() {
        val steps = listOf(" Bath ", "", "Feed", "A song in arms")
        assertEquals(listOf("Bath", "Feed", "A song in arms"), Ritual.decode(Ritual.encode(steps)))
        assertEquals(emptyList<String>(), Ritual.decode(null))
    }

    @Test fun limitsStepsAndLength() {
        assertEquals(Ritual.MAX_STEPS, Ritual.decode(Ritual.encode((1..20).map { "Step $it" })).size)
        assertEquals(Ritual.MAX_LENGTH, Ritual.decode(Ritual.encode(listOf("x".repeat(100)))).single().length)
    }

    @Test fun noteListsTheTickedStepsInOrder() {
        val steps = listOf("Bath", "Pyjamas", "Feed", "Song")
        assertEquals("Bath · Feed · Song", Ritual.note(steps, setOf(3, 0, 2)))
        assertNull(Ritual.note(steps, emptySet()))
    }
}
