package br.gabriel.cochilo.domain.assistant

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.assistant.Grounding.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DigestTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant().toEpochMilli()
    private val now = at(20, 15)
    private val log = (12..19).flatMap { d ->
        listOf(
            Event(type = EventType.SLEEP, start = at(d, 7, 30), end = at(d, 8, 5), kind = Kinds.ARMS),
            Event(type = EventType.SLEEP, start = at(d, 13, 0), end = at(d, 14, 20), kind = Kinds.SLING),
            Event(type = EventType.FEED, start = at(d, 9, 0), end = at(d, 9, 20)),
            Event(type = EventType.SLEEP, start = at(d, 19, 30), end = at(d + 1, 5, 30)),
        )
    } + Event(type = EventType.SLEEP, start = at(20, 9, 0), end = at(20, 9, 40), kind = Kinds.CRIB)
    private val facts = Assistant.facts(log, 14, now, zone, Predictor(zone).predict(log, 14, now))
    private val pt = Digest.lines(log, "Dante", 14, now, zone, facts, pt = true)
    private val en = Digest.lines(log, "Dante", 14, now, zone, facts, pt = false)

    @Test fun it_names_the_baby_and_the_age() {
        assertEquals("Bebê: Dante, 14 semanas (cerca de 3 meses).", pt.first())
        assertTrue(en.first().startsWith("Baby: Dante, 14 weeks"))
    }

    @Test fun it_has_every_day_today_entries_this_week_and_the_references() {
        assertTrue(pt.any { it.startsWith("Hoje até agora:") })
        assertTrue(pt.any { it.startsWith("Ontem:") })
        assertTrue(pt.any { it.startsWith("Há 6 dias:") })
        assertTrue(pt.any { it == "Dormiu 09:00–09:40, 40 min (0h 40m) (soneca, lugar: ${Kinds.CRIB})." })
        assertTrue(pt.any { it.startsWith("Sonecas nos últimos 7 dias:") && it.contains("a mais longa 80 min (1h 20m)") })
        assertTrue(pt.any { it.startsWith("Usual para a idade:") && it.contains("9.7 a 15.9 horas") })
        assertTrue(pt.any { it.startsWith("Duração das sonecas nessa idade:") && it.contains("30 a 45 min") })
    }

    /** The point of it: an answer about nap length, with her numbers and the age's, passes the check. */
    @Test fun a_real_answer_about_naps_can_pass_the_check() {
        val prompt = Grounding.prompt("o quão devem demorar as sonecas do Dante?", pt, emptyList(), portuguese = true)
        val reply = "Nesta semana as sonecas do Dante duraram 40 min na mediana, de 35 a 80 min. Nessa idade, sonecas de 30 a 45 min são comuns, e de 30 min a 2 horas também."
        assertEquals(Verdict.OK, Grounding.check(reply, prompt))
        assertEquals(Verdict.INVENTED_NUMBER, Grounding.check("Sonecas de 3,5 horas são o ideal.", prompt))
    }
}
