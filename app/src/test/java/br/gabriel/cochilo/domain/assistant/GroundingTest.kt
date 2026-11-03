package br.gabriel.cochilo.domain.assistant

import br.gabriel.cochilo.domain.assistant.Grounding.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundingTest {
    private val facts = listOf("Sleep: 14h 16m a day on average (usual for this age 9.7–15.9 h).", "Naps: 4.0 a day, 1h 05m at the median.")
    private val passages = listOf("Contact naps are normal and good in the first months.")
    private val prompt = Grounding.prompt("Does teething change naps?", facts, passages, portuguese = false)

    @Test fun the_prompt_holds_the_rules_the_facts_and_the_question() {
        assertTrue(prompt.contains("Use ONLY the facts"))
        assertTrue(prompt.contains("Never give medical advice"))
        assertTrue(prompt.contains("- Naps: 4.0 a day"))
        assertTrue(prompt.contains("- Contact naps are normal"))
        assertTrue(prompt.trimEnd().endsWith("Answer:"))
        assertTrue(Grounding.prompt("Ela dorme bem?", facts, passages, portuguese = true).contains("Use SOMENTE os fatos"))
    }

    @Test fun a_grounded_reply_passes() {
        assertEquals(Verdict.OK, Grounding.check("This week the baby napped 4.0 times a day, 1h 05m at the median. Contact naps are normal.", prompt))
    }

    @Test fun a_number_counts_however_it_is_written() {
        // "1h 05m" in the facts: "5 minutes" and "1 hour" are the same numbers.
        assertEquals(Verdict.OK, Grounding.check("Naps last about 1 hour and 5 minutes.", prompt))
        assertEquals(Verdict.OK, Grounding.check("Dorme 9,7 a 15,9 horas.", prompt))
    }

    @Test fun invented_numbers_are_refused() {
        assertEquals(Verdict.INVENTED_NUMBER, Grounding.check("Most babies sleep 18 hours at this age.", prompt))
    }

    @Test fun judging_and_medicine_are_refused() {
        assertEquals(Verdict.JUDGES, Grounding.check("You should stop rocking the baby.", prompt))
        assertEquals(Verdict.JUDGES, Grounding.check("Isso é uma regressão.", prompt))
        assertEquals(Verdict.MEDICAL, Grounding.check("A little melatonin can help.", prompt))
        assertEquals(Verdict.MEDICAL, Grounding.check("Talvez seja uma doença.", prompt))
    }

    @Test fun empty_or_rambling_replies_are_refused() {
        assertEquals(Verdict.EMPTY, Grounding.check("   ", prompt))
        assertEquals(Verdict.TOO_LONG, Grounding.check("Calm. ".repeat(200), prompt))
    }

    @Test fun the_reply_is_cleaned() {
        assertEquals("Naps vary.", Grounding.clean("  Answer: \"Naps vary.\" "))
    }
}
