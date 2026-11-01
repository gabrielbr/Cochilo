package br.gabriel.cochilo.ui.screens

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
) {
    val f = Format(LocalContext.current, zone, now)
    val today = localDate(now, zone)
    val from = startOfDay(today, zone)
    val to = startOfDay(today.plusDays(1), zone)
    val todays = log.filter { (it.end ?: now) >= from && it.start < to }

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
                    br.gabriel.cochilo.ui.kit.ListRow(
                        label = eventLabel(e, f, zone),
                        meta = stringResource(R.string.panel_since, f.time(e.start)) + " · " + f.duration(e.durationMs(now)),
                        leading = { br.gabriel.cochilo.ui.kit.Swatch(CochiloTheme.colors.eventColorOf(e, zone)) },
                        onClick = { onOpen(e) },
                    ) { Button(stringResource(R.string.action_finish), { onFinish(e) }) }
                }
            }
        }

        Caption(stringResource(R.string.caption_log))
        QuickLog(onNew)

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
    val bg = if (sleeping != null) c.panel else c.surface
    val fg = if (sleeping != null) c.onPanel else c.ink
    val fgMuted = if (sleeping != null) c.onPanelMuted else c.muted
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(bg).padding(Spacing.space6)
    ) {
        val label = when {
            sleeping != null && sleeping.isNight -> stringResource(R.string.panel_asleep_night)
            sleeping != null -> stringResource(R.string.panel_asleep_nap)
            awake != null -> stringResource(R.string.panel_awake_for)
            else -> stringResource(R.string.panel_ready)
        }
        Text(label.uppercase(), style = Type.caption, color = fgMuted)
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
private fun QuickLog(onNew: (EventType) -> Unit) {
    val f = Format(LocalContext.current)
    val types = listOf(EventType.FEED, EventType.DIAPER, EventType.SLEEP, EventType.PUMP, EventType.DAYLIGHT, EventType.ROUTINE, EventType.NOTE)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        types.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                pair.forEach { t ->
                    Button(
                        f.typeName(t), { onNew(t) }, ButtonKind.SECONDARY, Modifier.weight(1f),
                        icon = if (t == EventType.DAYLIGHT) R.drawable.gg_ic_sun else R.drawable.gg_ic_plus,
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
