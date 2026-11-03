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

/** One question and its reply, kept only while the screen is open (nothing is stored). */
data class AskExchange(val question: String, val reply: Assistant.Reply)

/**
 * Ask Cochilo (docs/assistant.md): this week's numbers first, then questions answered from the
 * baby's log and the reviewed answer library, on the phone. Suggested questions are one tap;
 * typing is there for the rest. Health, medicine and the mother's wellbeing get a person, not an
 * answer.
 */
@Composable
fun AskScreen(facts: Assistant.Facts, f: Format, initial: List<AskExchange> = emptyList()) {
    val context = LocalContext.current
    val exchanges = remember { mutableStateListOf(*initial.toTypedArray()) }
    var text by remember { mutableStateOf("") }
    fun ask(q: String) {
        if (q.isBlank()) return
        exchanges += AskExchange(q.trim(), Assistant.ask(q, facts))
        text = ""
    }
    fun askTopic(t: Topic) {
        val q = context.getString(questionRes(t))
        exchanges += AskExchange(q, Assistant.Reply.About(t))
    }
    ScrollPage {
        Text(stringResource(R.string.ask_line), style = Type.meta, color = CochiloTheme.colors.muted)
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
            ReplyCard(x.reply, facts, f, onTopic = ::askTopic)
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
private fun ReplyCard(reply: Assistant.Reply, facts: Assistant.Facts, f: Format, onTopic: (Topic) -> Unit) {
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
                val a = answer(context, reply.topic, facts, f)
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
