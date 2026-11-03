package br.gabriel.cochilo.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import br.gabriel.cochilo.R
import kotlinx.coroutines.launch
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.assistant.Assistant
import br.gabriel.cochilo.domain.assistant.Assistant.Topic
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.Field
import br.gabriel.cochilo.ui.kit.FilterChips
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/**
 * One question and its reply, kept only while the screen is open (nothing is stored). [generated]:
 * the on-device model's checked answer (phase 2); [thinking] while it writes.
 */
data class AskExchange(
    val question: String, val reply: Assistant.Reply, val generated: String? = null, val thinking: Boolean = false,
    /** The model answered, but its reply didn't pass the checks (Grounding), so the library's is shown, and says so. */
    val unchecked: Boolean = false,
    /** A reviewed answer as it was shown, so a saved chat keeps its words and numbers (0.40.0). */
    val parts: AnswerParts? = null,
)

/** An exchange as a saved chat keeps it (data.ChatItem); null while the model is still writing. */
fun AskExchange.toItem(): br.gabriel.cochilo.data.ChatItem? {
    if (thinking) return null
    return when {
        generated != null -> br.gabriel.cochilo.data.ChatItem(question, "generated", generated = generated)
        reply is Assistant.Reply.About -> br.gabriel.cochilo.data.ChatItem(
            question, "about", topic = reply.topic.name, data = parts?.data, info = parts?.info, tryIt = parts?.tryIt, source = parts?.source, unchecked = unchecked,
        )
        reply is Assistant.Reply.SeeSomeone -> br.gabriel.cochilo.data.ChatItem(question, "care", care = reply.care.name)
        else -> br.gabriel.cochilo.data.ChatItem(question, "not_sure", unchecked = unchecked)
    }
}

/** A saved exchange back on screen, with its words as they were. */
fun br.gabriel.cochilo.data.ChatItem.toExchange(): AskExchange {
    val topic = topic?.let { t -> Topic.entries.firstOrNull { it.name == t } }
    val careOf = care?.let { c -> Assistant.Care.entries.firstOrNull { it.name == c } }
    val reply = when {
        kind == "about" && topic != null -> Assistant.Reply.About(topic)
        kind == "care" && careOf != null -> Assistant.Reply.SeeSomeone(careOf)
        else -> Assistant.Reply.NotSure(emptyList())
    }
    val parts = if (kind == "about" && info != null) AnswerParts(data, info, tryIt, source) else null
    return AskExchange(question, reply, generated = generated, unchecked = unchecked, parts = parts)
}

/**
 * Ask Cochilo (docs/assistant.md): this week's numbers first, then questions answered from the
 * baby's log and the reviewed answer library, on the phone. Suggested questions are one tap;
 * typing is there for the rest. Health, medicine and the mother's wellbeing get a person, not an
 * answer.
 */
@Composable
fun AskScreen(
    facts: Assistant.Facts, f: Format, initial: List<AskExchange> = emptyList(),
    /** Phase 2: the on-device model, when it's ready; it answers only what the library can't. */
    generate: (suspend (ModelAsk) -> ModelAnswer?)? = null,
    /** The model isn't on the phone yet: under a "not sure" reply, one row opens its sheet. */
    onGetModel: (() -> Unit)? = null,
    /** What the model is told about this baby (Digest): the log, day by day, with the age references. */
    digest: List<String> = emptyList(),
    /** The model is on its way: a "not sure" reply says to ask again once it's here. */
    modelGetting: Boolean = false,
    /** The model took the app down last time, so it's paused (0.39.1): a "not sure" reply offers to try again. */
    onResumeModel: (() -> Unit)? = null,
    /** Called after every change, so the chat is saved (0.40.0). */
    onChange: (List<AskExchange>) -> Unit = {},
    /** At the top: the way to the other chats. */
    chats: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val exchanges = remember { mutableStateListOf(*initial.toTypedArray()) }
    var text by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun ask(q: String) {
        if (q.isBlank()) return
        val reply = Assistant.ask(q, facts)
        text = ""
        // Health, medicine and the mother still go to a person; known topics keep their reviewed
        // answers. Only "not sure" questions go to the model, grounded and checked (Grounding).
        val parts = (reply as? Assistant.Reply.About)?.let { answer(context, it.topic, facts, f) }
        if (reply !is Assistant.Reply.NotSure || generate == null) { exchanges += AskExchange(q.trim(), reply, parts = parts); onChange(exchanges.toList()); return }
        val i = exchanges.size
        exchanges += AskExchange(q.trim(), reply, thinking = true)
        scope.launch {
            val answer = generate(groundedAsk(context, q, facts, f, digest))
            val out = answer?.reply?.let(br.gabriel.cochilo.domain.assistant.Grounding::clean)
            val ok = out != null && br.gabriel.cochilo.domain.assistant.Grounding.check(out, answer.prompt) == br.gabriel.cochilo.domain.assistant.Grounding.Verdict.OK
            if (i < exchanges.size) exchanges[i] = exchanges[i].copy(thinking = false, generated = if (ok) out else null, unchecked = !ok)
            onChange(exchanges.toList())
        }
    }
    fun askTopic(t: Topic) {
        val q = context.getString(questionRes(t))
        exchanges += AskExchange(q, Assistant.Reply.About(t), parts = answer(context, t, facts, f))
        onChange(exchanges.toList())
    }
    ScrollPage {
        Text(stringResource(R.string.ask_line), style = Type.meta, color = CochiloTheme.colors.muted)
        if (chats != null) {
            Spacer(Modifier.height(Spacing.space4))
            chats()
        }
        val insights = insights(context, facts, f)
        if (insights.isNotEmpty()) {
            Caption(stringResource(R.string.ask_this_week), top = Spacing.space6)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(CochiloTheme.colors.surface).padding(Spacing.space4),
                verticalArrangement = Arrangement.spacedBy(Spacing.space2),
            ) { insights.forEach { Text(it, style = Type.bodySmall, color = CochiloTheme.colors.ink) } }
        }
        for (x in exchanges) {
            Spacer(Modifier.height(Spacing.space6))
            QuestionBubble(x.question)
            Spacer(Modifier.height(Spacing.space2))
            when {
                x.thinking -> ThinkingCard()
                x.generated != null -> GeneratedCard(x.generated)
                else -> {
                    if (x.unchecked) {
                        Text(stringResource(R.string.ask_unchecked), style = Type.meta, color = CochiloTheme.colors.muted)
                        Spacer(Modifier.height(Spacing.space2))
                    }
                    ReplyCard(x.reply, facts, f, onTopic = ::askTopic, parts = x.parts)
                    if (onResumeModel != null && x.reply is Assistant.Reply.NotSure && x === exchanges.last()) {
                        Spacer(Modifier.height(Spacing.space2))
                        Text(stringResource(R.string.ask_model_paused), style = Type.meta, color = CochiloTheme.colors.muted)
                        Spacer(Modifier.height(Spacing.space2))
                        Button(stringResource(R.string.ask_model_resume), onResumeModel, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
                    }
                    if (modelGetting && x.reply is Assistant.Reply.NotSure && x === exchanges.last()) {
                        Spacer(Modifier.height(Spacing.space2))
                        Text(stringResource(R.string.ask_model_getting), style = Type.meta, color = CochiloTheme.colors.muted)
                    }
                }
            }
            if (onGetModel != null && x.reply is Assistant.Reply.NotSure && x.generated == null && !x.thinking && x === exchanges.last()) {
                Spacer(Modifier.height(Spacing.space2))
                br.gabriel.cochilo.ui.kit.RowGroup {
                    br.gabriel.cochilo.ui.kit.ListRow(
                        stringResource(R.string.ask_get_model), meta = stringResource(R.string.ask_get_model_meta), onClick = onGetModel,
                        trailing = { br.gabriel.cochilo.ui.kit.GIcon(R.drawable.gg_ic_chevron, null, CochiloTheme.colors.muted) },
                    )
                }
            }
        }
        Caption(stringResource(R.string.ask_suggested), top = Spacing.space6)
        val asked = exchanges.mapNotNull { (it.reply as? Assistant.Reply.About)?.topic }.toSet()
        val offer = (Assistant.suggestions(facts) + Topic.entries).filter { it !in asked }.distinct().take(4)
        FilterChips(offer, null, { context.getString(questionRes(it)) }) { askTopic(it) }
        Spacer(Modifier.height(Spacing.space4))
        Field(
            text, { text = it.take(MAX_QUESTION) }, stringResource(R.string.ask_field),
            keyboard = KeyboardOptions(imeAction = ImeAction.Send),
        )
        Spacer(Modifier.height(Spacing.space3))
        Button(stringResource(R.string.ask_send), { ask(text) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
    }
}

private const val MAX_QUESTION = 200

/** The reviewed passages the model may draw on: what stands out this week, plus safe sleep. */
private const val PASSAGES = 4

/** The model's prompt: this week's facts and the most relevant reviewed answers (docs/assistant.md). */
/** What the model is asked: the prompt's parts, so the model's side can trim them to fit its window (Grounding.fit). */
data class ModelAsk(val question: String, val facts: List<String>, val passages: List<String>, val portuguese: Boolean)

/** The model's reply and the exact prompt it answered, which the reply is checked against. */
data class ModelAnswer(val prompt: String, val reply: String)

fun groundedAsk(context: Context, question: String, facts: Assistant.Facts, f: Format, digest: List<String> = emptyList()): ModelAsk {
    val topics = (Assistant.suggestions(facts) + Topic.SAFE_SLEEP).distinct().take(PASSAGES)
    val passages = topics.flatMap { t -> answer(context, t, facts, f).let { listOfNotNull(it.info, it.tryIt) } }
    val portuguese = context.getString(R.string.locale_tag).startsWith("pt")
    // The whole log digest when there is one (0.38.0); this week's four lines otherwise.
    return ModelAsk(question, digest.ifEmpty { insights(context, facts, f) }, passages, portuguese)
}

@Composable
private fun ThinkingCard() {
    val c = CochiloTheme.colors
    Text(
        stringResource(R.string.ask_thinking), style = Type.meta, color = c.muted,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface)
            .border(Size.hairline, c.line, RoundedCornerShape(Radius.radiusM)).padding(Spacing.space4)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** The model's answer, always labelled as such. */
@Composable
private fun GeneratedCard(text: String) {
    val c = CochiloTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface)
            .border(Size.hairline, c.line, RoundedCornerShape(Radius.radiusM)).padding(Spacing.space4)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(text, style = Type.body, color = c.ink)
        Spacer(Modifier.height(Spacing.space3))
        Text(stringResource(R.string.ask_generated_label), style = Type.meta, color = c.muted)
    }
}

/** Under this share of short naps, the answer says most naps already run long. */
private const val MOSTLY_LONG_PERCENT = 34

@Composable
private fun QuestionBubble(q: String) {
    val c = CochiloTheme.colors
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            q, style = Type.body, color = c.onSelected,
            modifier = Modifier.widthIn(max = Size.contentMax / 2 + Spacing.space16)
                .clip(RoundedCornerShape(Radius.radiusM)).background(c.selected)
                .padding(horizontal = Spacing.space4, vertical = Spacing.space3),
        )
    }
}

@Composable
private fun ReplyCard(reply: Assistant.Reply, facts: Assistant.Facts, f: Format, onTopic: (Topic) -> Unit, parts: AnswerParts? = null) {
    val c = CochiloTheme.colors
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface)
            .border(Size.hairline, c.line, RoundedCornerShape(Radius.radiusM))
            .padding(Spacing.space4)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        when (reply) {
            is Assistant.Reply.SeeSomeone -> Text(stringResource(careRes(reply.care)), style = Type.body, color = c.ink)
            // The suggestions are the "You could ask" chips just below, so they aren't repeated here.
            is Assistant.Reply.NotSure -> Text(stringResource(R.string.ask_not_sure), style = Type.body, color = c.ink)
            is Assistant.Reply.About -> {
                val a = parts ?: answer(context, reply.topic, facts, f)
                if (a.data != null) {
                    Text(stringResource(R.string.ask_from_log), style = Type.meta, color = c.muted)
                    Text(a.data, style = Type.body, color = c.ink)
                    Spacer(Modifier.height(Spacing.space3))
                }
                Text(a.info, style = Type.body, color = c.ink)
                if (a.tryIt != null) {
                    Spacer(Modifier.height(Spacing.space3))
                    Text(stringResource(R.string.ask_what_helps), style = Type.meta, color = c.muted)
                    Text(a.tryIt, style = Type.body, color = c.ink)
                }
                if (a.source != null) {
                    Spacer(Modifier.height(Spacing.space3))
                    Text(stringResource(R.string.ask_source, a.source), style = Type.meta, color = c.muted)
                }
            }
        }
    }
}

/** An answer's parts, from the reviewed strings and the baby's numbers. Not composable, so it's testable. */
data class AnswerParts(val data: String?, val info: String, val tryIt: String?, val source: String?)

fun answer(context: Context, t: Topic, facts: Assistant.Facts, f: Format): AnswerParts {
    fun s(id: Int, vararg a: Any) = context.getString(id, *a)
    val noData = s(R.string.ask_no_data)
    val avg = facts.averages
    fun hours(h: Double) = if (h % 1.0 == 0.0) h.toInt().toString() else String.format(f.locale, "%.1f", h)
    return when (t) {
        Topic.NEXT_SLEEP -> {
            val data = when (val p = facts.prediction) {
                is Predictor.Result.Sleeping -> s(R.string.ask_next_sleeping_data, f.time(p.since))
                is Predictor.Result.Awake -> s(
                    if (p.next == Predictor.Next.NAP) R.string.ask_next_nap_data else R.string.ask_next_bed_data,
                    f.time(p.at), f.time(p.windowStart), f.time(p.windowEnd),
                )
                Predictor.Result.NeedData -> noData
            }
            AnswerParts(data, s(R.string.ask_next_sleep_info), s(R.string.ask_next_sleep_try), null)
        }
        Topic.SHORT_NAPS -> AnswerParts(
            when {
                facts.napMedianMin == null || facts.shortNapPercent == null -> noData
                // Most naps already run long: say so rather than echo the premise.
                facts.shortNapPercent < MOSTLY_LONG_PERCENT -> s(R.string.ask_short_naps_data_long, f.durationMin(facts.napMedianMin))
                else -> s(R.string.ask_short_naps_data, f.durationMin(facts.napMedianMin), facts.shortNapPercent)
            },
            s(R.string.ask_short_naps_info), s(R.string.ask_short_naps_try), s(R.string.ask_short_naps_source),
        )
        Topic.NAP_LENGTH -> AnswerParts(
            if (facts.napMedianMin == null || facts.napShortestMin == null || facts.napLongestMin == null) noData
            else s(R.string.ask_nap_length_data, f.durationMin(facts.napMedianMin), f.durationMin(facts.napShortestMin), f.durationMin(facts.napLongestMin), facts.naps),
            s(if (facts.ageWeeks < 26) R.string.ask_nap_length_info_young else R.string.ask_nap_length_info),
            s(R.string.ask_nap_length_try), s(R.string.ask_nap_length_source),
        )
        Topic.NIGHT_WAKINGS -> AnswerParts(
            avg?.let { s(R.string.ask_night_wakings_data, String.format(f.locale, "%.1f", it.nightWakings), f.durationMin(it.longestStretchMin)) } ?: noData,
            s(R.string.ask_night_wakings_info), s(R.string.ask_night_wakings_try), s(R.string.ask_night_wakings_source),
        )
        Topic.EARLY_WAKING -> AnswerParts(
            if (facts.daysLogged >= 3) s(R.string.ask_early_waking_data, facts.earlyWakeDays) else noData,
            s(R.string.ask_early_waking_info), s(R.string.ask_early_waking_try), s(R.string.ask_early_waking_source),
        )
        Topic.BEDTIME -> AnswerParts(
            if (facts.bedtimeMinute != null) s(R.string.ask_bedtime_data, f.clock(facts.bedtimeMinute), f.durationMin(facts.bedtimeSpreadMin ?: 0)) else noData,
            facts.prior.bedtimeMinute?.let { s(R.string.ask_bedtime_info, f.clock(it)) } ?: s(R.string.ask_bedtime_info_young),
            s(R.string.ask_bedtime_try), s(R.string.ask_bedtime_source),
        )
        Topic.WAKE_WINDOWS -> AnswerParts(
            facts.windowMedianMin?.let { s(R.string.ask_wake_windows_data, f.durationMin(it), f.durationMin(facts.prior.wakeMinMinutes), f.durationMin(facts.prior.wakeMaxMinutes)) } ?: noData,
            s(R.string.ask_wake_windows_info), s(R.string.ask_wake_windows_try), s(R.string.ask_wake_windows_source),
        )
        Topic.ENOUGH_SLEEP -> AnswerParts(
            avg?.let { s(R.string.ask_enough_sleep_data, f.durationMin(it.totalSleepMin), hours(facts.range.lowHours), hours(facts.range.highHours), facts.range.source) } ?: noData,
            s(R.string.ask_enough_sleep_info), s(R.string.ask_enough_sleep_try), s(R.string.ask_enough_sleep_source),
        )
        Topic.CONTACT_NAPS -> AnswerParts(
            if (facts.naps > 0) s(R.string.ask_contact_naps_data, facts.contactNaps, facts.naps) else noData,
            s(R.string.ask_contact_naps_info), s(R.string.ask_contact_naps_try), s(R.string.ask_contact_naps_source),
        )
        Topic.DAYLIGHT -> AnswerParts(
            avg?.let { s(R.string.ask_daylight_data, f.durationMin(it.daylightMin)) } ?: noData,
            s(R.string.ask_daylight_info), s(R.string.ask_daylight_try), s(R.string.ask_daylight_source),
        )
        Topic.ROUTINE -> AnswerParts(
            s(R.string.ask_routine_data, facts.routines),
            s(R.string.ask_routine_info), s(R.string.ask_routine_try), s(R.string.ask_routine_source),
        )
        Topic.FOUR_MONTHS -> AnswerParts(
            s(R.string.ask_four_months_data, facts.ageWeeks),
            s(R.string.ask_four_months_info), s(R.string.ask_four_months_try), s(R.string.ask_four_months_source),
        )
        Topic.FEED_TO_SLEEP -> AnswerParts(null, s(R.string.ask_feed_to_sleep_info), s(R.string.ask_feed_to_sleep_try), s(R.string.ask_feed_to_sleep_source))
        Topic.SAFE_SLEEP -> AnswerParts(null, s(R.string.ask_safe_sleep_info), null, s(R.string.ask_safe_sleep_source))
    }
}

/** This week's numbers, in plain lines, before any question. */
fun insights(context: Context, facts: Assistant.Facts, f: Format): List<String> {
    val avg = facts.averages ?: return emptyList()
    fun hours(h: Double) = if (h % 1.0 == 0.0) h.toInt().toString() else String.format(f.locale, "%.1f", h)
    return listOfNotNull(
        context.getString(R.string.ask_insight_sleep, f.durationMin(avg.totalSleepMin), hours(facts.range.lowHours), hours(facts.range.highHours)),
        facts.napMedianMin?.let { context.getString(R.string.ask_insight_naps, String.format(f.locale, "%.1f", avg.naps), f.durationMin(it)) },
        context.getString(R.string.ask_insight_night, f.durationMin(avg.longestStretchMin)),
        context.getString(R.string.ask_insight_daylight, f.durationMin(avg.daylightMin)),
    )
}

fun questionRes(t: Topic): Int = when (t) {
    Topic.NEXT_SLEEP -> R.string.ask_next_sleep_q
    Topic.SHORT_NAPS -> R.string.ask_short_naps_q
    Topic.NAP_LENGTH -> R.string.ask_nap_length_q
    Topic.NIGHT_WAKINGS -> R.string.ask_night_wakings_q
    Topic.EARLY_WAKING -> R.string.ask_early_waking_q
    Topic.BEDTIME -> R.string.ask_bedtime_q
    Topic.WAKE_WINDOWS -> R.string.ask_wake_windows_q
    Topic.ENOUGH_SLEEP -> R.string.ask_enough_sleep_q
    Topic.CONTACT_NAPS -> R.string.ask_contact_naps_q
    Topic.DAYLIGHT -> R.string.ask_daylight_q
    Topic.ROUTINE -> R.string.ask_routine_q
    Topic.FOUR_MONTHS -> R.string.ask_four_months_q
    Topic.FEED_TO_SLEEP -> R.string.ask_feed_to_sleep_q
    Topic.SAFE_SLEEP -> R.string.ask_safe_sleep_q
}

private fun careRes(c: Assistant.Care): Int = when (c) {
    Assistant.Care.BABY_HEALTH -> R.string.ask_care_baby_health
    Assistant.Care.MEDICINE -> R.string.ask_care_medicine
    Assistant.Care.MOTHER -> R.string.ask_care_mother
}

/**
 * Ask's chats (0.40.0): a new one first (the screen's one action), then the saved ones, newest
 * first, each one tap to open. Deleting is one tap, with Undo instead of "are you sure?".
 */
@Composable
fun ChatsScreen(
    chats: List<br.gabriel.cochilo.data.Chat>, f: Format, onNew: () -> Unit, onOpen: (String) -> Unit,
    onDelete: (br.gabriel.cochilo.data.Chat) -> Unit, deleted: br.gabriel.cochilo.data.Chat?, onUndo: () -> Unit,
) {
    val c = CochiloTheme.colors
    ScrollPage {
        Text(stringResource(R.string.chats_line), style = Type.meta, color = c.muted)
        Spacer(Modifier.height(Spacing.space4))
        Button(stringResource(R.string.chats_new), onNew, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        if (deleted != null) {
            Spacer(Modifier.height(Spacing.space4))
            br.gabriel.cochilo.ui.kit.RowGroup {
                br.gabriel.cochilo.ui.kit.ListRow(stringResource(R.string.chats_deleted, deleted.title)) {
                    Button(stringResource(R.string.undo), onUndo)
                }
            }
        }
        if (chats.isEmpty()) {
            Spacer(Modifier.height(Spacing.space6))
            Text(stringResource(R.string.chats_empty), style = Type.bodySmall, color = c.muted)
            return@ScrollPage
        }
        br.gabriel.cochilo.ui.kit.Caption(stringResource(R.string.chats_saved), top = Spacing.space8)
        br.gabriel.cochilo.ui.kit.RowGroup {
            chats.forEachIndexed { i, chat ->
                if (i > 0) br.gabriel.cochilo.ui.kit.Hairline()
                br.gabriel.cochilo.ui.kit.ListRow(
                    chat.title, meta = stringResource(R.string.chats_meta, f.dayTime(chat.updated), chat.items.size),
                    onClick = { onOpen(chat.id) },
                    trailing = { br.gabriel.cochilo.ui.kit.IconButton(R.drawable.gg_ic_close, stringResource(R.string.chats_delete, chat.title), { onDelete(chat) }) },
                )
            }
        }
    }
}

/**
 * The glimpse (0.45.0, docs/assistant.md): the one question most worth asking now, from her own
 * log, for the end of Today. Null when the log is too short to say anything, or when the only
 * question left is the next sleep (Today's card already says it).
 */
fun glimpseTopic(facts: Assistant.Facts, day: Long): Topic? {
    if (facts.daysLogged < GLIMPSE_DAYS) return null
    // What stands out in her log first (short naps, early mornings, little daylight, contact naps).
    val standsOut = Assistant.suggestions(facts).firstOrNull { it in GLIMPSE_STANDOUT }
    if (standsOut != null) return standsOut
    // Otherwise one of the everyday questions with her numbers, a different one each day.
    val everyday = GLIMPSE_EVERYDAY.filter { hasData(it, facts) }
    return everyday.getOrNull(Math.floorMod(day, everyday.size.coerceAtLeast(1).toLong()).toInt())
}

private val GLIMPSE_STANDOUT = setOf(Topic.SHORT_NAPS, Topic.EARLY_WAKING, Topic.DAYLIGHT, Topic.CONTACT_NAPS)
private val GLIMPSE_EVERYDAY = listOf(Topic.NIGHT_WAKINGS, Topic.ENOUGH_SLEEP, Topic.WAKE_WINDOWS, Topic.BEDTIME, Topic.NAP_LENGTH)

/** Whether [t]'s answer has her own numbers in it (the glimpse never shows "not enough data"). */
private fun hasData(t: Topic, facts: Assistant.Facts): Boolean = when (t) {
    Topic.NIGHT_WAKINGS, Topic.ENOUGH_SLEEP -> facts.averages != null
    Topic.WAKE_WINDOWS -> facts.windowMedianMin != null
    Topic.BEDTIME -> facts.bedtimeMinute != null
    Topic.NAP_LENGTH -> facts.napMedianMin != null && facts.napShortestMin != null && facts.napLongestMin != null
    else -> true
}

/** Days of log before the glimpse speaks: like Ask's own "This week", it needs a few days. */
const val GLIMPSE_DAYS = 3

/**
 * The glimpse on Today: the question, her numbers, and what to try (Ask Cochilo's reviewed
 * answer, on the phone), then "Read the answer", which opens Ask on it.
 */
@Composable
fun AskGlimpse(topic: Topic, facts: Assistant.Facts, f: Format, onOpen: (Topic) -> Unit) {
    val context = LocalContext.current
    val c = CochiloTheme.colors
    val parts = answer(context, topic, facts, f)
    val noData = stringResource(R.string.ask_no_data)
    val question = stringResource(questionRes(topic))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface)
            .padding(start = Spacing.space4, end = Spacing.space4, top = Spacing.space4),
    ) {
        Text(question, style = Type.label, color = c.ink)
        val data = parts.data?.takeIf { it != noData }
        if (data != null) {
            Spacer(Modifier.height(Spacing.space2))
            Text(data, style = Type.bodySmall, color = c.ink)
        }
        parts.tryIt?.let {
            Spacer(Modifier.height(Spacing.space2))
            Text(it, style = Type.meta, color = c.muted)
        }
        br.gabriel.cochilo.ui.kit.QuietLink(
            stringResource(R.string.glimpse_line), stringResource(R.string.glimpse_open),
            stringResource(R.string.glimpse_spoken, question), { onOpen(topic) },
        )
    }
}
