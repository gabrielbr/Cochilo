package br.gabriel.cochilo.domain.assistant

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.MINUTE
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.Stats
import br.gabriel.cochilo.domain.isNap
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.domain.startOfDay
import java.time.Instant
import java.time.ZoneId

/**
 * What the on-device model is told about this baby (docs/assistant.md, 0.38.0): the name and age,
 * the last seven days one by one, today's entries, this week's naps, windows and bedtime, the
 * next-sleep guess, and the age references, all as plain lines with their numbers. The model may
 * only repeat numbers that are here (Grounding.check), so the more of the log is here, the more it
 * can actually answer. Pure Kotlin, tested in `DigestTest`; it never leaves the phone.
 */
object Digest {
    fun lines(
        log: List<Event>, name: String?, ageWeeks: Int, now: Long, zone: ZoneId, facts: Assistant.Facts, pt: Boolean,
    ): List<String> {
        fun t(en: String, ptBr: String) = if (pt) ptBr else en
        fun d(min: Int) = "$min min (${min / 60}h ${"%02d".format(min % 60)}m)"
        fun clock(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) }
        fun clockMin(m: Int) = "%02d:%02d".format((m / 60) % 24, m % 60)
        val out = mutableListOf<String>()
        val months = ageWeeks * 7 / 30
        out += t(
            "Baby: ${name?.ifBlank { null } ?: "the baby"}, $ageWeeks weeks old (about $months months).",
            "Bebê: ${name?.ifBlank { null } ?: "o bebê"}, $ageWeeks semanas (cerca de $months meses).",
        )

        // The last seven days, today first.
        val today = localDate(now, zone)
        val days = Stats(zone).days(log, today, 7, now)
        days.sortedByDescending { it.date }.forEachIndexed { i, s ->
            if (s.totalSleepMin == 0 && s.feeds == 0) return@forEachIndexed
            val label = when (i) { 0 -> t("Today so far", "Hoje até agora"); 1 -> t("Yesterday", "Ontem"); else -> t("$i days ago", "Há $i dias") }
            out += t(
                "$label: sleep ${d(s.totalSleepMin)}; naps ${s.naps} (${d(s.napSleepMin)}); night ${d(s.nightSleepMin)}; longest stretch ${d(s.longestStretchMin)}; night wakings ${s.nightWakings}; feeds ${s.feeds}; diapers ${s.diapers}; daylight ${d(s.daylightMin)}.",
                "$label: sono ${d(s.totalSleepMin)}; sonecas ${s.naps} (${d(s.napSleepMin)}); noite ${d(s.nightSleepMin)}; maior trecho ${d(s.longestStretchMin)}; despertares à noite ${s.nightWakings}; mamadas ${s.feeds}; fraldas ${s.diapers}; luz do dia ${d(s.daylightMin)}.",
            )
        }

        // Today's sleeps, one by one.
        val from = startOfDay(today, zone)
        log.filter { it.type == EventType.SLEEP && (it.end ?: now) > from }.sortedBy { it.start }.forEach { e ->
            val kind = if (e.isNap(zone)) t("nap", "soneca") else t("night sleep", "sono da noite")
            val place = e.kind?.let { t(", place: $it", ", lugar: $it") } ?: ""
            val end = e.end
            out += if (end == null) t("Asleep since ${clock(e.start)} ($kind$place).", "Dormindo desde ${clock(e.start)} ($kind$place).")
            else t(
                "Slept ${clock(e.start)}–${clock(end)}, ${d(((end - e.start) / MINUTE).toInt())} ($kind$place).",
                "Dormiu ${clock(e.start)}–${clock(end)}, ${d(((end - e.start) / MINUTE).toInt())} ($kind$place).",
            )
        }

        // This week, summed up.
        if (facts.napMedianMin != null) {
            out += t(
                "Naps in the last 7 days: ${facts.naps}; length at the median ${d(facts.napMedianMin)}, shortest ${d(facts.napShortestMin ?: 0)}, longest ${d(facts.napLongestMin ?: 0)}" +
                    (facts.shortNapPercent?.let { "; under 45 min: $it%" } ?: "") + "; in arms or sling: ${facts.contactNaps}.",
                "Sonecas nos últimos 7 dias: ${facts.naps}; duração na mediana ${d(facts.napMedianMin)}, a mais curta ${d(facts.napShortestMin ?: 0)}, a mais longa ${d(facts.napLongestMin ?: 0)}" +
                    (facts.shortNapPercent?.let { "; abaixo de 45 min: $it%" } ?: "") + "; no colo ou sling: ${facts.contactNaps}.",
            )
        }
        facts.windowMedianMin?.let { out += t("Daytime wake windows this week: ${d(it)} at the median.", "Janelas acordado de dia nesta semana: ${d(it)} na mediana.") }
        facts.bedtimeMinute?.let { out += t("Bedtime lately: about ${clockMin(it)}.", "Hora de dormir recente: por volta de ${clockMin(it)}.") }
        if (facts.daysLogged >= 3) out += t("Mornings before 06:00 in the last 7 days: ${facts.earlyWakeDays}.", "Manhãs antes das 06:00 nos últimos 7 dias: ${facts.earlyWakeDays}.")
        out += t("Bedtime routines logged this week: ${facts.routines}.", "Rotinas de dormir registradas nesta semana: ${facts.routines}.")
        when (val p = facts.prediction) {
            is Predictor.Result.Sleeping -> out += t("Now: asleep since ${clock(p.since)}.", "Agora: dormindo desde ${clock(p.since)}.")
            is Predictor.Result.Awake -> {
                val what = if (p.next == Predictor.Next.NAP) t("next nap", "próxima soneca") else t("bedtime", "hora de dormir")
                out += t(
                    "Now: awake since ${clock(p.since)}; $what expected about ${clock(p.at)}, between ${clock(p.windowStart)} and ${clock(p.windowEnd)}.",
                    "Agora: acordado desde ${clock(p.since)}; $what prevista por volta de ${clock(p.at)}, entre ${clock(p.windowStart)} e ${clock(p.windowEnd)}.",
                )
            }
            Predictor.Result.NeedData -> Unit
        }

        // What's usual for the age (Reference; research.md §3). Ranges, never targets.
        fun h(x: Double) = if (x % 1.0 == 0.0) x.toInt().toString() else x.toString()
        out += t(
            "Usual for this age: total sleep ${h(facts.range.lowHours)} to ${h(facts.range.highHours)} hours a day (${facts.range.source}); daytime wake windows about ${facts.prior.wakeMinMinutes} to ${facts.prior.wakeMaxMinutes} min; about ${facts.prior.naps} naps a day.",
            "Usual para a idade: sono total de ${h(facts.range.lowHours)} a ${h(facts.range.highHours)} horas por dia (${facts.range.source}); janelas acordado de dia de cerca de ${facts.prior.wakeMinMinutes} a ${facts.prior.wakeMaxMinutes} min; cerca de ${facts.prior.naps} sonecas por dia.",
        )
        out += if (ageWeeks < 26) t(
            "Nap length at this age: before 5 to 6 months many naps are one sleep cycle, about 30 to 45 min; naps from 30 min to 2 hours are all common. There is no right length; the whole day's sleep matters more.",
            "Duração das sonecas nessa idade: antes dos 5 a 6 meses muitas sonecas são um só ciclo de sono, cerca de 30 a 45 min; sonecas de 30 min a 2 horas são todas comuns. Não existe duração certa; o sono do dia inteiro importa mais.",
        ) else t(
            "Nap length at this age: from about 6 months naps often join two sleep cycles and last 1 to 2 hours, with fewer naps a day. There is no right length; the whole day's sleep matters more.",
            "Duração das sonecas nessa idade: a partir de uns 6 meses as sonecas costumam juntar dois ciclos e durar de 1 a 2 horas, com menos sonecas por dia. Não existe duração certa; o sono do dia inteiro importa mais.",
        )
        return out
    }
}
