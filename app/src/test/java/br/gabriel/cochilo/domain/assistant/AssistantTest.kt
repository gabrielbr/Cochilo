package br.gabriel.cochilo.domain.assistant

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.assistant.Assistant.Care
import br.gabriel.cochilo.domain.assistant.Assistant.Reply
import br.gabriel.cochilo.domain.assistant.Assistant.Topic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId

class AssistantTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant().toEpochMilli()
    private val now = at(20, 15)

    /** A week of short naps, some in arms, nights that end at 05:30. */
    private val log = (12..19).flatMap { d ->
        listOf(
            Event(type = EventType.SLEEP, start = at(d, 7, 30), end = at(d, 8, 5), kind = Kinds.ARMS),
            Event(type = EventType.SLEEP, start = at(d, 10, 0), end = at(d, 10, 35), kind = Kinds.CRIB),
            Event(type = EventType.SLEEP, start = at(d, 13, 0), end = at(d, 14, 20), kind = Kinds.SLING),
            Event(type = EventType.SLEEP, start = at(d, 19, 30), end = at(d + 1, 5, 30)),
        )
    }
    private val facts = Assistant.facts(log, 14, now, zone, Predictor(zone).predict(log, 14, now))

    private fun topic(q: String) = (Assistant.ask(q, facts) as? Reply.About)?.topic

    @Test fun health_medicine_and_the_mother_always_go_to_a_person_first() {
        assertEquals(Reply.SeeSomeone(Care.BABY_HEALTH), Assistant.ask("Ela está com febre e não dorme", facts))
        assertEquals(Reply.SeeSomeone(Care.BABY_HEALTH), Assistant.ask("he is breathing fast while asleep", facts))
        assertEquals(Reply.SeeSomeone(Care.MEDICINE), Assistant.ask("Qual a dose de melatonina?", facts))
        assertEquals(Reply.SeeSomeone(Care.MOTHER), Assistant.ask("Não aguento mais, estou muito triste", facts))
        assertEquals(Reply.SeeSomeone(Care.MOTHER), Assistant.ask("I feel so overwhelmed", facts))
    }

    /** The owner's own question (0.38.0), and its neighbours that must keep their topics. */
    @Test fun how_long_naps_last_is_its_own_topic() {
        assertEquals(Topic.NAP_LENGTH, topic("o quão devem demorar as sonecas do Dante?"))
        assertEquals(Topic.NAP_LENGTH, topic("Quanto tempo duram as sonecas dele?"))
        assertEquals(Topic.NAP_LENGTH, topic("how long should naps last?"))
        assertEquals(Topic.WAKE_WINDOWS, topic("quanto tempo acordado entre sonecas?"))
        assertEquals(Topic.WAKE_WINDOWS, topic("how long awake between naps"))
        assertEquals(Topic.SHORT_NAPS, topic("Por que as sonecas são tão curtas?"))
        assertEquals(Topic.NEXT_SLEEP, topic("quando é a próxima soneca?"))
        assertEquals(35, facts.napShortestMin)
        assertEquals(80, facts.napLongestMin)
    }

    @Test fun falling_asleep_is_not_a_fall() {
        assertEquals(Topic.FEED_TO_SLEEP, topic("Ela sempre dorme mamando, caiu no sono no peito"))
        assertEquals(Topic.CONTACT_NAPS, topic("he only sleeps in arms, fell asleep on me again"))
    }

    @Test fun understands_portuguese_and_english() {
        assertEquals(Topic.SHORT_NAPS, topic("Por que as sonecas são tão curtas?"))
        assertEquals(Topic.SHORT_NAPS, topic("why are her naps so short"))
        assertEquals(Topic.EARLY_WAKING, topic("Acorda às 5h todo dia"))
        assertEquals(Topic.NIGHT_WAKINGS, topic("acorda muito à noite"))
        assertEquals(Topic.BEDTIME, topic("What time should bedtime be?"))
        assertEquals(Topic.WAKE_WINDOWS, topic("qual a janela de sono dela"))
        assertEquals(Topic.DAYLIGHT, topic("passeio no sol ajuda?"))
        assertEquals(Topic.ROUTINE, topic("como montar uma rotina"))
        assertEquals(Topic.FOUR_MONTHS, topic("regressão dos 4 meses"))
        assertEquals(Topic.SAFE_SLEEP, topic("pode dormir de bruços?"))
        assertEquals(Topic.ENOUGH_SLEEP, topic("ele dorme o suficiente?"))
        assertEquals(Topic.NEXT_SLEEP, topic("quando é a próxima soneca?"))
    }

    @Test fun says_so_when_it_doesnt_know() {
        assertTrue(Assistant.ask("What colour is the sky?", facts) is Reply.NotSure) // "colour" is not "colo"
        val r = Assistant.ask("qual a cor do céu", facts)
        assertTrue(r is Reply.NotSure)
        assertTrue((r as Reply.NotSure).suggestions.size in 1..3)
        assertTrue(Assistant.ask("   ", facts) is Reply.NotSure)
    }

    @Test fun reads_the_facts_from_her_log() {
        assertEquals(7, facts.daysLogged)
        assertEquals(35, facts.napMedianMin)
        assertEquals(66, facts.shortNapPercent) // two of three naps a day are 35 minutes
        assertTrue(facts.earlyWakeDays >= 6)
        assertTrue(facts.contactNaps >= 12)
    }

    @Test fun offers_what_stands_out_first() {
        val s = Assistant.suggestions(facts)
        assertEquals(3, s.size)
        assertEquals(Topic.SHORT_NAPS, s[0])
        assertEquals(Topic.EARLY_WAKING, s[1])
    }

    /** Never judge: no answer may scold, blame or call a phase a step back (brand-voice.md). */
    @Test fun the_answers_never_judge() {
        val banned = Regex("\\b(should|must|wrong|bad habit|regression|fault|spoil|deveria|errad|ruim|regressão|culpa|mania|vício|mimad)", RegexOption.IGNORE_CASE)
        for (path in listOf("src/main/res/values/strings.xml", "src/main/res/values-pt-rBR/strings.xml")) {
            val xml = File(path).readText()
            val answers = Regex("<string name=\"(ask_[a-z0-9_]+)\">([^<]*)</string>").findAll(xml).toList()
            assertTrue("$path has the answers", answers.size > 40)
            for (m in answers) {
                if (m.groupValues[1].endsWith("_q")) continue // the suggested questions are the parent's words
                assertTrue("${m.groupValues[1]} judges: ${m.groupValues[2]}", !banned.containsMatchIn(m.groupValues[2]))
            }
        }
    }
}
