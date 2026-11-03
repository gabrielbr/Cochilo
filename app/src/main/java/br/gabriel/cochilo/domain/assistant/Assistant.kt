package br.gabriel.cochilo.domain.assistant

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.AgePrior
import br.gabriel.cochilo.domain.Averages
import br.gabriel.cochilo.domain.DAY
import br.gabriel.cochilo.domain.HOUR
import br.gabriel.cochilo.domain.MINUTE
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.Reference
import br.gabriel.cochilo.domain.Stats
import br.gabriel.cochilo.domain.isNap
import br.gabriel.cochilo.domain.isNightSleep
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.domain.minuteOfDay
import br.gabriel.cochilo.domain.startOfDay
import java.text.Normalizer
import java.time.ZoneId

/**
 * Ask Cochilo (docs/assistant.md), phase 1: a local assistant that answers a parent's questions
 * about the baby's sleep from her own log and from the research the app rests on. Everything is
 * computed here, on the phone, in pure Kotlin (no Android imports, so it can move to a shared
 * module for iPhone). It never diagnoses, never doses, never judges: health, medicine and the
 * mother's wellbeing get fixed replies that point to a person, before any topic is matched.
 */
object Assistant {

    /** What a parent can ask about. Each has a reviewed answer (strings `ask_<topic>_*`). */
    enum class Topic {
        NEXT_SLEEP, SHORT_NAPS, NAP_LENGTH, NIGHT_WAKINGS, EARLY_WAKING, BEDTIME, WAKE_WINDOWS,
        ENOUGH_SLEEP, CONTACT_NAPS, DAYLIGHT, ROUTINE, FOUR_MONTHS, FEED_TO_SLEEP, SAFE_SLEEP,
    }

    /** Questions that belong to a person, not an app. */
    enum class Care { BABY_HEALTH, MEDICINE, MOTHER }

    sealed interface Reply {
        data class About(val topic: Topic) : Reply
        data class SeeSomeone(val care: Care) : Reply
        /** Not a question it knows: it says what it can help with and offers [suggestions]. */
        data class NotSure(val suggestions: List<Topic>) : Reply
    }

    /** The numbers the answers use, from the last 7 days. Null where the log has too little. */
    data class Facts(
        val ageWeeks: Int,
        val daysLogged: Int,
        val averages: Averages?,
        val range: Reference.SleepRange,
        val prior: AgePrior,
        val naps: Int,
        val napMedianMin: Int?,
        val napShortestMin: Int?,
        val napLongestMin: Int?,
        val shortNapPercent: Int?,
        val contactNaps: Int,
        val windowMedianMin: Int?,
        val bedtimeMinute: Int?,
        val bedtimeSpreadMin: Int?,
        val earlyWakeDays: Int,
        val routines: Int,
        val prediction: Predictor.Result,
    )

    const val SHORT_NAP_MIN = 45
    const val EARLY_WAKE_MINUTE = 6 * 60
    private const val WEEK = 7

    fun facts(log: List<Event>, ageWeeks: Int, now: Long, zone: ZoneId, prediction: Predictor.Result): Facts {
        val since = now - WEEK * DAY
        val today = localDate(now, zone)
        val stats = Stats(zone)
        val days = stats.days(log, today.minusDays(1), WEEK, now)
        val sleeps = log.filter { it.type == EventType.SLEEP && it.end != null && it.start >= since }
        val naps = sleeps.filter { it.isNap(zone) }
        val napMins = naps.map { ((it.end!! - it.start) / MINUTE).toInt() }.sorted()
        val windows = Predictor(zone, lookbackDays = WEEK).windows(log, now).filter { !it.beforeBed }.map { it.minutes }.sorted()
        val predictor = Predictor(zone, lookbackDays = WEEK)
        val bedtimes = bedtimeMinutes(log, now, zone)
        return Facts(
            ageWeeks = ageWeeks,
            daysLogged = days.count { it.totalSleepMin > 0 },
            averages = stats.averages(days),
            range = Reference.totalSleepRange(ageWeeks),
            prior = Reference.priorFor(ageWeeks),
            naps = naps.size,
            napMedianMin = napMins.takeIf { it.isNotEmpty() }?.let { it[it.size / 2] },
            napShortestMin = napMins.firstOrNull(),
            napLongestMin = napMins.lastOrNull(),
            shortNapPercent = napMins.takeIf { it.size >= 3 }?.let { m -> m.count { it < SHORT_NAP_MIN } * 100 / m.size },
            contactNaps = naps.count { it.kind == Kinds.ARMS || it.kind == Kinds.SLING },
            windowMedianMin = windows.takeIf { it.size >= 3 }?.let { it[it.size / 2].toInt() },
            bedtimeMinute = predictor.bedtimeMinute(log.filter { it.type == EventType.SLEEP }.sortedBy { it.start }, now),
            bedtimeSpreadMin = bedtimes.takeIf { it.size >= 3 }?.let { b -> val s = b.sorted(); (s[s.size * 3 / 4] - s[s.size / 4]) / 2 },
            earlyWakeDays = earlyWakeDays(log, now, zone),
            routines = log.count { it.type == EventType.ROUTINE && it.start >= since },
            prediction = prediction,
        )
    }

    /** Minutes after midnight (late evenings past 24:00) of each evening's first night sleep, last 7 days. */
    private fun bedtimeMinutes(log: List<Event>, now: Long, zone: ZoneId): List<Int> {
        val sleeps = log.filter { it.type == EventType.SLEEP && it.start >= now - WEEK * DAY }.sortedBy { it.start }
        return sleeps.filterIndexed { i, s -> s.isNightSleep(zone) && (i == 0 || !sleeps[i - 1].isNightSleep(zone)) }
            .map { s -> minuteOfDay(s.start, zone).let { if (it < 12 * 60) it + 24 * 60 else it } }
    }

    /** Days (of the last 7) whose morning wake, the last night sleep's end before 10:00, came before 06:00. */
    private fun earlyWakeDays(log: List<Event>, now: Long, zone: ZoneId): Int {
        val today = localDate(now, zone)
        return (1..WEEK).count { back ->
            val day = today.minusDays(back.toLong() - 1)
            val from = startOfDay(day, zone)
            val wake = log.filter { it.type == EventType.SLEEP && it.isNightSleep(zone) && it.end != null && it.end in from until from + 10 * HOUR }
                .maxOfOrNull { it.end!! } ?: return@count false
            minuteOfDay(wake, zone) < EARLY_WAKE_MINUTE
        }
    }

    // ---- Understanding the question -------------------------------------------------------

    /** Lower case, no accents, letters and digits only: "Por que acorda às 5h?" → "por que acorda as 5h". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ").replace(Regex(" +"), " ").trim()

    /** Words that mean the question is for a doctor, a pharmacist or a person, in English and Portuguese. */
    private val care: Map<Care, List<String>> = mapOf(
        Care.MOTHER to listOf(
            "depress", "ansiedade", "anxious", "anxiety", "overwhelm", "nao aguento", "cant cope", "can t cope",
            "triste", "sad ", "chorando muito eu", "me machucar", "hurt myself", "suicid", "sozinha", "exausta demais",
            "burnout", "baby blues", "puerper",
        ),
        Care.MEDICINE to listOf(
            "dose", "dosagem", "remedio", "medicine", "medication", "paracetamol", "dipirona", "ibuprofen", "melatonin",
            "melatonina", "antialergico", "xarope", "gotas", "drops",
        ),
        Care.BABY_HEALTH to listOf(
            "febre", "fever", "respira", "breath", "apneia", "apnea", "roxo", "blue lips", "convuls", "seizure",
            "vomit", "desidrat", "dehydrat", "nao mama", "not feeding", "wont feed", "won t feed", "caiu da", "caiu do", "queda", "fell off", "fell from",
            "bateu a cabeca", "hit his head", "hit her head", "manchas", "rash", "peso", "weight", "doente", "sick",
            "refluxo", "reflux", "ronco", "snor", "diarreia", "diarrh", "letargic", "lethargic", "mole demais",
            "doctor", "medico", "pediatra emergencia", "emergencia", "emergency", "sids", "morte subita",
        ),
    )

    /** Stems that point to each topic. Several hits beat one; the order breaks ties. */
    private val topics: Map<Topic, List<String>> = mapOf(
        Topic.SAFE_SLEEP to listOf("seguro", "safe", "brucos", "tummy", "stomach", "travesseiro", "pillow", "protetor", "bumper", "cobertor", "blanket", "cama compartilhada", "co sleep", "cosleep", "bed share", "bedshare", "sofa"),
        Topic.FOUR_MONTHS to listOf("4 meses", "quatro meses", "4 month", "four month", "regress", "salto", "leap", "piorou", "got worse", "worse"),
        Topic.EARLY_WAKING to listOf("cedo", "early", "5h", "5 h", "5am", "5 am", "madrugada", "5 da manha", "dawn", "4h", "4am"),
        Topic.NIGHT_WAKINGS to listOf("noite", "night", "acorda muito", "acordando", "wakes up", "waking", "despert", "madrugada"),
        Topic.SHORT_NAPS to listOf("curta", "curtas", "short", "30 min", "40 min", "acorda logo", "wakes after", "pouco tempo"),
        // Counts only next to a nap word (see [napWords]): "how long should naps last", "quanto tempo duram as sonecas".
        Topic.NAP_LENGTH to listOf("dura", "duram", "duracao", "demor", "how long", "quanto tempo", "length", "last", "lasts", "comprid", "longa", "longas", "tempo de soneca"),
        Topic.CONTACT_NAPS to listOf("colo", "arms", "held", "sling", "carreg", "carry", "contact", "contato", "so dorme no", "only sleeps"),
        Topic.FEED_TO_SLEEP to listOf("mamando", "mamar para dormir", "peito para dormir", "feed to sleep", "nurse to sleep", "nursing to sleep", "dorme mamando", "falls asleep feeding"),
        Topic.BEDTIME to listOf("hora de dormir", "horario de dormir", "bedtime", "que horas", "what time", "dormir a noite", "colocar para dormir", "put down"),
        Topic.WAKE_WINDOWS to listOf("janela", "window", "acordad", "awake", "quanto tempo acordado", "how long awake"),
        Topic.ENOUGH_SLEEP to listOf("suficiente", "enough", "pouco", "too little", "normal", "quanto dorme", "how much sleep", "total", "dorme pouco", "dorme muito", "too much"),
        Topic.DAYLIGHT to listOf("luz", "light", "sol", "sun", "passeio", "walk", "outside", "fora de casa", "escuro", "dark"),
        Topic.ROUTINE to listOf("rotina", "routine", "ritual", "banho", "bath", "musica", "song", "massagem"),
        Topic.NEXT_SLEEP to listOf("proxima", "next", "quando", "when", "soneca agora", "nap now", "cansado", "tired", "sono agora"),
    )

    /**
     * A key matches at the start of a word ("acord" in "acordando"); a short key (4 letters or
     * fewer) only as a whole word, so "colo" (arms) never matches "colour".
     */
    private fun matches(q: String, key: String): Boolean {
        val k = key.trim()
        var i = q.indexOf(" $k")
        while (i >= 0) {
            val end = i + 1 + k.length
            if (k.length > SHORT_KEY || end >= q.length || q[end] == ' ') return true
            i = q.indexOf(" $k", i + 1)
        }
        return false
    }

    private const val SHORT_KEY = 4

    /** NAP_LENGTH needs one of these too, so "how long awake" stays a wake-window question. */
    private val napWords = listOf("soneca", "sonecas", "cochilo", "cochilos", "nap", "naps", "napping")

    /** What [question] is about, safety first. */
    fun ask(question: String, facts: Facts): Reply {
        val q = " " + normalize(question) + " "
        if (q.isBlank()) return Reply.NotSure(suggestions(facts))
        for (c in listOf(Care.MOTHER, Care.MEDICINE, Care.BABY_HEALTH)) {
            if (care.getValue(c).any { matches(q, it) }) return Reply.SeeSomeone(c)
        }
        val napWord = napWords.any { matches(q, it) }
        val scored = topics.entries.map { (t, keys) ->
            val hits = keys.count { k -> matches(q, k) }
            t to if (t == Topic.NAP_LENGTH && !napWord) 0 else hits
        }
        val best = scored.filter { it.second > 0 }.maxWithOrNull(compareBy<Pair<Topic, Int>> { it.second }.thenBy { -topics.keys.indexOf(it.first) })
            ?: return Reply.NotSure(suggestions(facts))
        return Reply.About(best.first)
    }

    /**
     * The three questions most worth offering now, from her own log: what stands out first, then
     * the everyday ones. Never more than three.
     */
    fun suggestions(facts: Facts): List<Topic> {
        val out = mutableListOf<Topic>()
        if ((facts.shortNapPercent ?: 0) >= 50) out += Topic.SHORT_NAPS
        if (facts.earlyWakeDays >= 3) out += Topic.EARLY_WAKING
        if (facts.averages != null && facts.averages.daylightMin < 20) out += Topic.DAYLIGHT
        if (facts.ageWeeks in 13..22) out += Topic.FOUR_MONTHS
        if (facts.naps > 0 && facts.contactNaps * 2 >= facts.naps) out += Topic.CONTACT_NAPS
        out += listOf(Topic.NEXT_SLEEP, Topic.NIGHT_WAKINGS, Topic.ENOUGH_SLEEP, Topic.WAKE_WINDOWS)
        return out.distinct().take(3)
    }
}
