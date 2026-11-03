package br.gabriel.cochilo.domain.assistant

/**
 * Ask Cochilo, phase 2 (docs/assistant.md): the rules around the optional on-device model. Pure
 * Kotlin, so they're unit-tested and move to iPhone with the rest of the assistant.
 *
 * The model never sees a question about health, medicine or the mother's wellbeing (Assistant.ask
 * routes those to a person first). It gets only the facts computed from the log and the reviewed
 * answers, is told to answer only from them, and its reply is checked before anyone sees it:
 * a reply that judges, mentions medicine, invents a number, or runs long is thrown away and the
 * library's answer is shown instead.
 */
object Grounding {
    /** The model's answer may be at most this long; longer means it wandered. */
    const val MAX_REPLY_CHARS = 700

    /** Rules the model is given, in the app's language. */
    fun rules(portuguese: Boolean): String = if (portuguese) {
        """Você é o Cochilo, um diário de sono de bebê. Responda à pergunta dos pais em até 4 frases curtas, em português do Brasil.
Use SOMENTE os fatos e os textos abaixo, e chame o bebê pelo nome. Use os números exatamente como aparecem nos fatos; não invente números, horários ou estudos.
Nunca dê orientação médica, nunca fale de remédios ou doses, nunca diagnostique.
Nunca julgue nem culpe os pais ou o bebê. Descreva, sugira com calma, sem promessas.
Se os fatos e textos não respondem à pergunta, diga apenas: "Não sei responder isso com os seus registros." """
    } else {
        """You are Cochilo, a baby sleep diary. Answer the parent's question in at most 4 short sentences, in English.
Use ONLY the facts and texts below, and call the baby by name. Use numbers exactly as the facts write them; do not invent numbers, times or studies.
Never give medical advice, never mention medicines or doses, never diagnose.
Never judge or blame the parents or the baby. Describe, suggest gently, make no promises.
If the facts and texts don't answer the question, say only: "I can't answer that from your log." """
    }

    /** The whole prompt: rules, this week's facts, the reviewed passages, then the question. */
    fun prompt(question: String, facts: List<String>, passages: List<String>, portuguese: Boolean): String = buildString {
        appendLine(rules(portuguese))
        appendLine()
        appendLine(if (portuguese) "Fatos dos registros do bebê:" else "Facts from the baby's log:")
        facts.forEach { appendLine("- $it") }
        appendLine()
        appendLine(if (portuguese) "Textos revisados:" else "Reviewed texts:")
        passages.forEach { appendLine("- $it") }
        appendLine()
        append(if (portuguese) "Pergunta: " else "Question: ")
        appendLine(question.trim().take(MAX_QUESTION_CHARS))
        append(if (portuguese) "Resposta:" else "Answer:")
    }

    private const val MAX_QUESTION_CHARS = 200

    /**
     * The fullest prompt that [fits] (0.39.1): a prompt longer than the model's window crashed the
     * app natively. Facts come most important first (Digest), so the last ones go first, then the
     * passages. Null when even the rules and the question alone don't fit.
     */
    fun fit(question: String, facts: List<String>, passages: List<String>, portuguese: Boolean, fits: (String) -> Boolean): String? {
        var f = facts
        var p = passages
        while (true) {
            val prompt = prompt(question, f, p, portuguese)
            if (fits(prompt)) return prompt
            when {
                f.size > 1 -> f = f.dropLast(1)
                p.isNotEmpty() -> p = p.dropLast(1)
                f.isNotEmpty() -> f = emptyList()
                else -> return null
            }
        }
    }

    /** Why a reply was thrown away, or OK. */
    enum class Verdict { OK, EMPTY, TOO_LONG, JUDGES, MEDICAL, INVENTED_NUMBER }

    private val judging = Regex(
        "\\b(should|must|wrong|bad habit|regression|fault|spoil\\w*|deveria|deve |errad\\w*|ruim|regressão|culpa|mania|vício|mimad\\w*)",
        RegexOption.IGNORE_CASE,
    )
    private val medical = Regex(
        "\\b(dose\\w*|dosage|mg\\b|ml of|medic\\w*|medicine|remédio\\w*|paracetamol|ibuprofen\\w*|dipirona|melatonin\\w*|diagnos\\w*|disease|doença\\w*|prescri\\w*)",
        RegexOption.IGNORE_CASE,
    )
    private val number = Regex("\\d+(?:[.,:]\\d+)?")

    /**
     * Checks [reply] against the prompt it came from: every number in it must appear in the
     * prompt (so it can only repeat the facts, never make one up).
     */
    fun check(reply: String, prompt: String): Verdict {
        val r = reply.trim()
        if (r.isEmpty()) return Verdict.EMPTY
        if (r.length > MAX_REPLY_CHARS) return Verdict.TOO_LONG
        if (judging.containsMatchIn(r)) return Verdict.JUDGES
        if (medical.containsMatchIn(r)) return Verdict.MEDICAL
        val known = number.findAll(prompt).map { norm(it.value) }.toSet()
        if (number.findAll(r).any { norm(it.value) !in known }) return Verdict.INVENTED_NUMBER
        return Verdict.OK
    }

    /** "05" is "5", "1,5" is "1.5", "19:40" stays a time: so a fact's number counts however it's written. */
    private fun norm(n: String): String = n.replace(',', '.').split('.', ':').joinToString(if (':' in n) ":" else ".") { part ->
        part.trimStart('0').ifEmpty { "0" }
    }

    /** The model's text, trimmed of a repeated "Answer:" and surrounding quotes. */
    fun clean(reply: String): String =
        reply.trim().removePrefix("Answer:").removePrefix("Resposta:").trim().trim('"').trim()
}
