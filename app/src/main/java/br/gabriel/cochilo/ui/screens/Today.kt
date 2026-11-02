package br.gabriel.cochilo.ui.screens

import br.gabriel.cochilo.domain.clockSpan
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.otherBreast
import br.gabriel.cochilo.domain.lastKindAmong
import br.gabriel.cochilo.domain.lastOf
import br.gabriel.cochilo.ui.kit.SlideOption
import br.gabriel.cochilo.ui.kit.SlideRow
import br.gabriel.cochilo.ui.kit.kindColor
import br.gabriel.cochilo.ui.kit.leads
import br.gabriel.cochilo.domain.overlaps
import br.gabriel.cochilo.ui.kit.InfoButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.MINUTE
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.domain.startOfDay
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.Chip
import br.gabriel.cochilo.ui.kit.ChipKind
import br.gabriel.cochilo.ui.kit.EmptyState
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.HourLabels
import br.gabriel.cochilo.ui.kit.ScreenTitle
import br.gabriel.cochilo.ui.kit.Timeline
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import java.time.ZoneId

@Composable
fun TodayScreen(
    log: List<Event>,
    prediction: Predictor.Result,
    now: Long,
    zone: ZoneId,
    onStartSleep: () -> Unit,
    onWokeUp: () -> Unit,
    onFinish: (Event) -> Unit,
    onNew: (EventType) -> Unit,
    onOpen: (Event) -> Unit,
    onQuick: (EventType, String?) -> Unit = { _, _ -> },
    onSwitch: (Event) -> Unit = {},
) {
    val f = Format(LocalContext.current, zone, now)
    val today = localDate(now, zone)
    val from = startOfDay(today, zone)
    val to = startOfDay(today.plusDays(1), zone)
    val todays = log.filter { it.overlaps(from, to, now) }

    ScrollPage {
        ScreenTitle(stringResource(R.string.tab_today), stringResource(R.string.tab_today))
        Spacer(Modifier.height(Spacing.space6))
        SleepPanel(prediction, now, f, onStartSleep, onWokeUp)

        if (prediction is Predictor.Result.Awake) {
            Spacer(Modifier.height(Spacing.space4))
            PredictionCard(prediction, now, f)
        } else if (prediction is Predictor.Result.NeedData) {
            Spacer(Modifier.height(Spacing.space4))
            Text(stringResource(R.string.prediction_need_data), style = Type.bodySmall, color = CochiloTheme.colors.muted)
        }

        val running = log.filter { it.running && it.type != EventType.SLEEP }
        if (running.isNotEmpty()) {
            Caption(stringResource(R.string.caption_running))
            br.gabriel.cochilo.ui.kit.RowGroup {
                running.forEachIndexed { i, e ->
                    if (i > 0) Hairline()
                    val since = stringResource(R.string.panel_since, f.time(e.start)) + " · " + f.duration(clockSpan(e.start, now))
                    val other = otherBreast(e.kind)
                    if (e.type == EventType.FEED && other != null) {
                        // A breastfeed: slide either way to end this side and start the other.
                        val switchTo = stringResource(R.string.slide_switch, f.kindName(other) ?: "")
                        SlideRow(
                            label = eventLabel(e, f, zone),
                            meta = since + "\n" + stringResource(R.string.slide_switch_hint),
                            leading = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) },
                            options = listOf(SlideOption(other, switchTo, switchTo)),
                            onTap = { onOpen(e) },
                            onPick = { onSwitch(e) },
                        ) { Button(stringResource(R.string.action_finish), { onFinish(e) }) }
                    } else {
                        br.gabriel.cochilo.ui.kit.ListRow(
                            label = eventLabel(e, f, zone),
                            meta = since,
                            leading = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) },
                            onClick = { onOpen(e) },
                            prominent = e.type.leads,
                        ) { Button(stringResource(R.string.action_finish), { onFinish(e) }) }
                    }
                }
            }
        }

        Caption(stringResource(R.string.caption_log))
        QuickLog(
            onNew, onQuick,
            last = EventType.entries.associateWith { lastOf(log, it) },
            lastKinds = EventType.entries.associateWith { t -> lastKindAmong(log, t, slideKinds(t)) },
        )

        Caption(stringResource(R.string.caption_timeline))
        Timeline(todays, from, to, now, zone, daySummary(f, today, todays, from, to, now, zone))
        Spacer(Modifier.height(Spacing.space1))
        HourLabels()
        Spacer(Modifier.height(Spacing.space3))
        EventLegend()

        Caption(stringResource(R.string.caption_today_log))
        if (todays.isEmpty()) {
            EmptyState(stringResource(R.string.empty_today_title), stringResource(R.string.empty_today_line))
        } else {
            EventList(todays.sortedByDescending { it.start }, now, f, zone, onOpen)
        }
    }
}

/** The sleep panel: dark while the baby sleeps, a surface card while awake. One primary action. */
@Composable
private fun SleepPanel(p: Predictor.Result, now: Long, f: Format, onStart: () -> Unit, onWoke: () -> Unit) {
    val c = CochiloTheme.colors
    val sleeping = p as? Predictor.Result.Sleeping
    val awake = p as? Predictor.Result.Awake
    // Start sleep and the card cross-fades to the night panel; Woke up and it fades back to day.
    val bg = br.gabriel.cochilo.ui.kit.animatedColor(if (sleeping != null) c.panel else c.surface, emphasised = true)
    val fg = br.gabriel.cochilo.ui.kit.animatedColor(if (sleeping != null) c.onPanel else c.ink, emphasised = true)
    val fgMuted = br.gabriel.cochilo.ui.kit.animatedColor(if (sleeping != null) c.onPanelMuted else c.muted, emphasised = true)
    // Wrapped in the fabric she picked (Settings › Fabric); the words stay on the card's colour.
    br.gabriel.cochilo.ui.kit.FabricFrame(bg, Spacing.space6) {
        val label = when {
            sleeping != null && sleeping.isNight -> stringResource(R.string.panel_asleep_night)
            sleeping != null -> stringResource(R.string.panel_asleep_nap)
            awake != null -> stringResource(R.string.panel_awake_for)
            else -> stringResource(R.string.panel_ready)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = Type.caption, color = fgMuted, modifier = Modifier.weight(1f))
            InfoButton(stringResource(R.string.type_sleep), stringResource(R.string.info_sleep_panel), tint = fgMuted)
        }
        val since = sleeping?.since ?: awake?.since
        if (since != null) {
            Text(
                f.duration(now - since), style = Type.timer, color = fg,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(stringResource(R.string.panel_since, f.time(since)), style = Type.meta, color = fgMuted)
        }
        Spacer(Modifier.height(Spacing.space6))
        if (sleeping != null) {
            Button(stringResource(R.string.action_woke_up), onWoke, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        } else {
            Button(stringResource(R.string.action_start_sleep), onStart, ButtonKind.PRIMARY, Modifier.fillMaxWidth(), icon = R.drawable.gg_ic_moon)
        }
    }
}

@Composable
private fun PredictionCard(p: Predictor.Result.Awake, now: Long, f: Format) {
    val c = CochiloTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface).padding(Spacing.space4)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (p.next == Predictor.Next.NAP) R.string.next_nap else R.string.next_bedtime).uppercase(),
                style = Type.caption, color = c.muted, modifier = Modifier.weight(1f),
            )
            when {
                now > p.windowEnd -> Chip(stringResource(R.string.chip_past), ChipKind.CORAL)
                now >= p.windowStart -> Chip(stringResource(R.string.chip_due), ChipKind.MINT)
            }
        }
        Spacer(Modifier.height(Spacing.space1))
        Text(f.time(p.at), style = Type.title, color = c.ink)
        Text(stringResource(R.string.prediction_window, f.time(p.windowStart), f.time(p.windowEnd)), style = Type.bodySmall, color = c.ink)
        Spacer(Modifier.height(Spacing.space2))
        val why = if (p.personalMinutes != null) stringResource(
            R.string.prediction_why_personal,
            f.durationMin(p.wakeMinutes), f.durationMin(p.personalMinutes), p.samples, f.durationMin(p.priorMinutes),
        ) else stringResource(R.string.prediction_why_prior, f.durationMin(p.wakeMinutes))
        Text(why, style = Type.meta, color = c.muted)
        if (p.next == Predictor.Next.BEDTIME && p.at - now <= 60 * MINUTE) {
            Spacer(Modifier.height(Spacing.space2))
            Text(stringResource(R.string.wind_down), style = Type.meta, color = c.ink)
        }
    }
}

/** Every other log type: secondary outlines, two to a row (the one mint action is Start sleep). */
@Composable
internal fun QuickLog(
    onNew: (EventType) -> Unit,
    onQuick: (EventType, String?) -> Unit = { _, _ -> },
    /** The last entry of each type: remember actions (AGENTS.md). */
    last: Map<EventType, Event?> = emptyMap(),
    /** The last kind used among each row's slide options (e.g. the last breast). */
    lastKinds: Map<EventType, String?> = emptyMap(),
    previewActive: Pair<EventType, Int>? = null,
) {
    val f = Format(LocalContext.current)
    val types = listOf(EventType.FEED, EventType.DIAPER, EventType.SLEEP, EventType.MEDICINE, EventType.PUMP, EventType.DAYLIGHT, EventType.ROUTINE, EventType.NOTE)
    // One row per kind of entry. Tap: the full sheet, opened on what was done last. Slide: log at
    // once (feed left or right, diaper wet/both/dirty…). Each row says what was done last, and
    // the slide marks that option, so the person sees it before choosing.
    br.gabriel.cochilo.ui.kit.RowGroup {
        types.forEachIndexed { i, t ->
            if (i > 0) Hairline()
            val prev = last[t]
            val base = slideOptions(t, f)
            // The last of this row's own options: for Feed, the last breast, even if a bottle came after.
            val lastOption = lastKinds[t]
            val spokenLast = stringResource(R.string.slide_last_spoken, "%s")
            val options = base.map {
                val isLast = lastOption != null && it.key == lastOption
                // The Last mark is seen (outline and dot); TalkBack says it.
                it.copy(last = isLast, a11y = if (isLast) spokenLast.replace("%s", it.a11y) else it.a11y)
            }
            val lastWords = when {
                lastOption != null && base.any { it.key == lastOption } -> (f.kindName(lastOption) ?: lastOption).lowercase()
                t == EventType.PUMP -> prev?.amountMl?.let { stringResource(R.string.ml, it) }
                t == EventType.MEDICINE -> prev?.let { e -> listOfNotNull(e.note, f.dayTime(e.start)).joinToString(" · ") }
                else -> null
            }
            // The slide hint; what was done last gets its own line after a Last dot.
            val hint = if (options.isNotEmpty()) stringResource(R.string.slide_hint, options.joinToString(" · ") { it.label }) else null
            SlideRow(
                label = f.typeName(t),
                meta = hint,
                leading = { br.gabriel.cochilo.ui.kit.KindIcon(t) },
                options = options,
                onTap = { onNew(t) },
                onPick = { o -> onQuick(t, o.key.takeIf { it.isNotEmpty() }) },
                previewActive = previewActive?.takeIf { it.first == t }?.second,
                accent = CochiloTheme.colors.kindColor(t),
                last = lastWords,
                lastSpoken = lastWords?.let { stringResource(R.string.row_last, it) },
                prominent = t.leads,
            ) { InfoButton(f.typeName(t), stringResource(infoText(t))) }
        }
    }
}

/** The kinds each log row's slide offers (keys of slideOptions). */
internal fun slideKinds(t: EventType): Set<String> = when (t) {
    EventType.FEED -> setOf(Kinds.BREAST_LEFT, Kinds.BREAST_RIGHT)
    EventType.DIAPER -> setOf(Kinds.WET, Kinds.BOTH, Kinds.DIRTY)
    EventType.DAYLIGHT -> setOf(Kinds.OUTDOORS, Kinds.WINDOW)
    EventType.ROUTINE -> setOf(Kinds.NAP_ROUTINE, Kinds.BEDTIME_ROUTINE)
    else -> emptySet()
}

/** What a slide on each log row can log, left to right. */
@Composable
private fun slideOptions(t: EventType, f: Format): List<SlideOption> {
    val ctx = LocalContext.current
    fun opt(kind: String, label: String) = SlideOption(kind, label, ctx.getString(R.string.a11y_log, listOfNotNull(f.typeName(t), f.kindName(kind)).joinToString(" · ")))
    return when (t) {
        EventType.FEED -> listOf(
            opt(Kinds.BREAST_LEFT, stringResource(R.string.slide_left)),
            opt(Kinds.BREAST_RIGHT, stringResource(R.string.slide_right)),
        )
        EventType.DIAPER -> listOf(Kinds.WET, Kinds.BOTH, Kinds.DIRTY).map { opt(it, f.kindName(it) ?: it) }
        EventType.DAYLIGHT -> listOf(Kinds.OUTDOORS, Kinds.WINDOW).map { opt(it, f.kindName(it) ?: it) }
        EventType.ROUTINE -> listOf(Kinds.NAP_ROUTINE, Kinds.BEDTIME_ROUTINE).map { opt(it, f.kindName(it) ?: it) }
        EventType.PUMP -> listOf(SlideOption("", stringResource(R.string.slide_start), stringResource(R.string.a11y_log, f.typeName(t))))
        else -> emptyList()
    }
}
