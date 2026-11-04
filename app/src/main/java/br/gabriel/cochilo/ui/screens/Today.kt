package br.gabriel.cochilo.ui.screens

import br.gabriel.cochilo.domain.clockSpan
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.otherBreast
import br.gabriel.cochilo.ui.kit.SlideOption
import br.gabriel.cochilo.ui.kit.SlideRow
import br.gabriel.cochilo.ui.kit.kindColor
import br.gabriel.cochilo.ui.kit.leads
import br.gabriel.cochilo.domain.overlaps
import br.gabriel.cochilo.ui.kit.InfoButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    /** Start sleep now (0) or that many minutes ago (the Earlier chips). */
    onStartSleep: (Int) -> Unit,
    onWokeUp: (Int) -> Unit,
    onFinish: (Event) -> Unit,
    onNew: (EventType) -> Unit,
    onOpen: (Event) -> Unit,
    onQuick: (EventType, String?) -> Unit = { _, _ -> },
    onSwitch: (Event) -> Unit = {},
    /** Where the running sleep is (crib, arms, sling…). */
    onPlace: (Event, String) -> Unit = { _, _ -> },
    /** The night is over (docs/nights.md); [Int] minutes ago (Earlier chips). */
    onUpForDay: (Int) -> Unit = {},
    /** Nap or the night, one tap on the sleep card. */
    onSetNight: (Event, Boolean) -> Unit = { _, _ -> },
    /** The rest of today, projected (DayPlan, 0.49.0): the card's "Then:" line. */
    plan: br.gabriel.cochilo.domain.DayPlan.Plan? = null,
    /** The glimpse from Ask Cochilo at the end (0.45.0); by day only, never in a night waking. */
    glimpse: (@Composable () -> Unit)? = null,
) {
    val f = Format(LocalContext.current, zone, now)
    val today = localDate(now, zone)
    val from = startOfDay(today, zone)
    val to = startOfDay(today.plusDays(1), zone)
    val todays = log.filter { it.overlaps(from, to, now) }

    ScrollPage {
        // No "Today" title (0.43.0, less is more): the tab and the header already name it; the card leads.
        // Awake in the night (docs/nights.md): between two sleeps of one night, no next-nap guess.
        val openNight = if (prediction is Predictor.Result.Awake) br.gabriel.cochilo.domain.Nights.openNight(log, now) else null
        SleepPanel(prediction, now, f, log, onStartSleep, onWokeUp, onPlace, openNight, onUpForDay, onSetNight)

        if (prediction is Predictor.Result.Awake && openNight == null) {
            Spacer(Modifier.height(Spacing.space4))
            PredictionCard(prediction, now, f, plan)
        } else if (prediction is Predictor.Result.NeedData) {
            Spacer(Modifier.height(Spacing.space4))
            Text(stringResource(R.string.prediction_need_data), style = Type.bodySmall, color = CochiloTheme.colors.muted)
        }

        // The daylight nudge: 14:00–16:00 with no daylight yet (research §2.3), one tap to start a walk. Never during a night waking: the night is quiet (0.43.0).
        if (prediction !is Predictor.Result.Sleeping && openNight == null && br.gabriel.cochilo.domain.Daylight.nudge(log, zone, now)) {
            Spacer(Modifier.height(Spacing.space4))
            br.gabriel.cochilo.ui.kit.RowGroup {
                br.gabriel.cochilo.ui.kit.ListRow(
                    stringResource(R.string.daylight_nudge), meta = stringResource(R.string.daylight_nudge_meta),
                    leading = { br.gabriel.cochilo.ui.kit.KindIcon(EventType.DAYLIGHT) },
                ) { Button(stringResource(R.string.daylight_go), { onQuick(EventType.DAYLIGHT, br.gabriel.cochilo.data.Kinds.OUTDOORS) }) }
            }
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

        // No "Day by day" here since 0.45.0 (the owner): past days are the Day tab's arrows, and its
        // kind filter moved there, on top of the hour-by-hour list.
        if (glimpse != null && openNight == null && !br.gabriel.cochilo.domain.isNightStart(now, zone)) {
            Caption(stringResource(R.string.glimpse_caption))
            glimpse()
        }
    }
}

/** The sleep panel: dark while the baby sleeps, a surface card while awake. One primary action. */
@Composable
private fun SleepPanel(
    p: Predictor.Result, now: Long, f: Format, log: List<Event>, onStart: (Int) -> Unit, onWoke: (Int) -> Unit, onPlace: (Event, String) -> Unit,
    openNight: Event? = null, onUpForDay: (Int) -> Unit = {}, onSetNight: (Event, Boolean) -> Unit = { _, _ -> },
) {
    val c = CochiloTheme.colors
    val sleeping = p as? Predictor.Result.Sleeping
    val awake = (p as? Predictor.Result.Awake)?.takeIf { openNight == null }
    val running = if (sleeping != null) log.firstOrNull { it.type == EventType.SLEEP && it.end == null } else null
    // Start sleep and the card cross-fades to the night panel; Woke up and it fades back to day.
    val bg = br.gabriel.cochilo.ui.kit.animatedColor(if (sleeping != null) c.panel else c.surface, emphasised = true)
    val fg = br.gabriel.cochilo.ui.kit.animatedColor(if (sleeping != null) c.onPanel else c.ink, emphasised = true)
    val fgMuted = br.gabriel.cochilo.ui.kit.animatedColor(if (sleeping != null) c.onPanelMuted else c.muted, emphasised = true)
    // Wrapped in the fabric she picked (Settings › Fabric); the words stay on the card's colour.
    br.gabriel.cochilo.ui.kit.FabricFrame(bg, Spacing.space6) {
        val label = when {
            sleeping != null && sleeping.isNight -> stringResource(R.string.panel_asleep_night)
            sleeping != null -> stringResource(R.string.panel_asleep_nap)
            openNight != null -> stringResource(R.string.panel_awake_night)
            awake != null -> stringResource(R.string.panel_awake_for)
            else -> stringResource(R.string.panel_ready)
        }
        // No "i" on the main card (0.43.0): it's read every time, explained once (less is more).
        Text(label, style = Type.caption, color = fgMuted)
        val since = sleeping?.since ?: openNight?.end ?: awake?.since
        if (since == null) {
            // Before the first sleep: the baby, asleep in the sling, waiting with her.
            Box(Modifier.fillMaxWidth().padding(top = Spacing.space4), contentAlignment = Alignment.Center) {
                br.gabriel.cochilo.ui.kit.BabyAvatar(br.gabriel.cochilo.ui.kit.LocalBabyLook.current, mood = br.gabriel.cochilo.ui.kit.BabyMood.ASLEEP)
            }
        }
        if (since != null) {
            Text(
                f.duration(now - since), style = Type.timer, color = fg,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            val nightLine = openNight?.let { last ->
                val night = br.gabriel.cochilo.domain.Nights.containing(log, last, f.zone)
                stringResource(R.string.panel_night_since, f.time(night.first().start), night.size)
            }
            Text(nightLine ?: stringResource(R.string.panel_since, f.time(since)), style = Type.meta, color = fgMuted)
            // Nap or the night was guessed at Start sleep; one tap changes it (docs/nights.md).
            if (running != null && sleeping != null) {
                br.gabriel.cochilo.ui.kit.SwapAction(
                    stringResource(if (sleeping.isNight) R.string.panel_make_nap else R.string.panel_make_night), fgMuted,
                ) { onSetNight(running, !sleeping.isNight) }
            }
        }
        // Where the baby slept is asked when she wakes (details after, 0.28.0), not here.
        Spacer(Modifier.height(Spacing.space6))
        if (sleeping != null) {
            Button(stringResource(R.string.action_woke_up), { onWoke(0) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        } else if (openNight != null) {
            // A night waking: back to sleep continues the night; up for the day ends it. From 05:00
            // the morning leads (the one mint action), before it going back to sleep does.
            val morning = br.gabriel.cochilo.domain.minuteOfDay(now, f.zone) >= br.gabriel.cochilo.domain.Nights.EARLY_MINUTE
            val back: @Composable (ButtonKind) -> Unit = { k -> Button(stringResource(R.string.action_back_to_sleep), { onStart(0) }, k, Modifier.fillMaxWidth(), icon = R.drawable.gg_ic_moon) }
            val up: @Composable (ButtonKind) -> Unit = { k -> Button(stringResource(R.string.action_up_for_day), { onUpForDay(0) }, k, Modifier.fillMaxWidth(), icon = R.drawable.gg_ic_sun) }
            if (morning) { up(ButtonKind.PRIMARY); Spacer(Modifier.height(Spacing.space2)); back(ButtonKind.SECONDARY) }
            else { back(ButtonKind.PRIMARY); Spacer(Modifier.height(Spacing.space2)); up(ButtonKind.SECONDARY) }
            return@FabricFrame
        } else {
            Button(stringResource(R.string.action_start_sleep), { onStart(0) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth(), icon = R.drawable.gg_ic_moon)
        }
        // Earlier: the baby fell asleep (or woke) before you reached the phone. One tap, 5/10/15 min ago.
        val earlier = if (sleeping != null) br.gabriel.cochilo.domain.Earlier.forWake(log, now) else br.gabriel.cochilo.domain.Earlier.forStart(log, now)
        if (earlier.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.space2))
            br.gabriel.cochilo.ui.kit.EarlierChips(
                label = stringResource(if (sleeping != null) R.string.earlier_woke else R.string.earlier_asleep),
                minutes = earlier,
                chipLabel = { stringResource(R.string.earlier_min, it) },
                spoken = { stringResource(if (sleeping != null) R.string.earlier_woke_spoken else R.string.earlier_asleep_spoken, it) },
                after = stringResource(R.string.earlier_after),
                onDark = sleeping != null,
            ) { m -> if (sleeping != null) onWoke(m) else onStart(m) }
        }
    }
}

/** A day plan's sleeps as one line: "13:50 · 16:05 · bed 18:45" (0.49.0). */
@Composable
fun planLine(sleeps: List<br.gabriel.cochilo.domain.DayPlan.Sleep>, f: Format): String {
    val bed = stringResource(R.string.plan_bed, "%s")
    return sleeps.joinToString(" · ") { s -> if (s.next == Predictor.Next.BEDTIME) bed.format(f.time(s.at)) else f.time(s.at) }
}

@Composable
private fun PredictionCard(p: Predictor.Result.Awake, now: Long, f: Format, plan: br.gabriel.cochilo.domain.DayPlan.Plan? = null) {
    val c = CochiloTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface).padding(Spacing.space4)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (p.next == Predictor.Next.NAP) R.string.next_nap else R.string.next_bedtime),
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
        // The rest of the day (0.49.0): one line, the sleeps after this one, never a promise ("Then").
        val later = plan?.sleeps?.drop(1).orEmpty()
        if (later.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.space1))
            Text(stringResource(R.string.plan_then, planLine(later, f)), style = Type.bodySmall, color = c.muted)
        }
        // How it's guessed (predictions explain themselves): one tap away since 0.43.0, not always open.
        var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        br.gabriel.cochilo.ui.kit.SwapAction(stringResource(if (open) R.string.prediction_why_hide else R.string.prediction_why_show), c.muted) { open = !open }
        if (open) {
        val why = if (p.personalMinutes != null) stringResource(
            R.string.prediction_why_personal,
            f.durationMin(p.wakeMinutes), f.durationMin(p.personalMinutes), p.samples, f.durationMin(p.priorMinutes),
        ) else stringResource(R.string.prediction_why_prior, f.durationMin(p.wakeMinutes))
        Text(why, style = Type.meta, color = c.muted)
        if (later.isNotEmpty() && plan != null) Text(
            stringResource(if (plan.fromAge) R.string.plan_why_age else R.string.plan_why, f.durationMin(plan.napMinutes)),
            style = Type.meta, color = c.muted, modifier = Modifier.padding(top = Spacing.space1),
        )
        // The clock (0.54.0) says what it did, with its numbers.
        p.clockEffect?.let { e ->
            val at = f.time(br.gabriel.cochilo.domain.startOfDay(br.gabriel.cochilo.domain.localDate(now, f.zone), f.zone) + e.towardsMinute * MINUTE)
            val bed = p.next == Predictor.Next.BEDTIME
            val res = when {
                bed && e.shiftMinutes < 0 -> R.string.prediction_clock_bed_sooner
                bed -> R.string.prediction_clock_bed_later
                e.shiftMinutes < 0 -> R.string.prediction_clock_nap_sooner
                else -> R.string.prediction_clock_nap_later
            }
            Text(stringResource(res, at, f.durationMin(kotlin.math.abs(e.shiftMinutes))), style = Type.meta, color = c.muted, modifier = Modifier.padding(top = Spacing.space1))
        }
        // Phase 2 says what the last nap did, with its numbers (predictions explain themselves).
        p.napEffect?.let { e ->
            val words = stringResource(
                if (e.shiftMinutes < 0) R.string.prediction_nap_shorter else R.string.prediction_nap_longer,
                f.durationMin(e.lastNapMinutes), f.durationMin(e.usualNapMinutes), f.durationMin(kotlin.math.abs(e.shiftMinutes)),
            )
            Text(words, style = Type.meta, color = c.muted, modifier = Modifier.padding(top = Spacing.space1))
        }
        }
        if (p.next == Predictor.Next.BEDTIME && p.at - now <= 60 * MINUTE) {
            Spacer(Modifier.height(Spacing.space2))
            Text(stringResource(R.string.wind_down), style = Type.meta, color = c.ink)
        }
    }
}

