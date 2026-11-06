package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import br.gabriel.cochilo.domain.clockSpan
import br.gabriel.cochilo.domain.overlaps
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.isNightSleep
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.domain.startOfDay
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.DayRing
import br.gabriel.cochilo.ui.kit.EmptyState
import br.gabriel.cochilo.ui.kit.FilterChips
import br.gabriel.cochilo.ui.kit.IconButton
import br.gabriel.cochilo.ui.kit.InfoButton
import br.gabriel.cochilo.ui.kit.ScreenTitle
import br.gabriel.cochilo.ui.kit.eventColor
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import java.time.LocalDate
import java.time.ZoneId

/**
 * One day, two ways: a 24-hour ring for the shape of the day, then every entry hour by hour on a
 * vertical rail, sleeps as bars as long as they lasted and the awake time between them.
 */
@Composable
fun DayScreen(
    log: List<Event>, now: Long, zone: ZoneId, onOpen: (Event) -> Unit, initialDay: LocalDate? = null,
    /** The next sleep's expected window (from the prediction), shown on today's ring. */
    expected: Pair<Long, Long>? = null,
    /** The rest of today, projected (DayPlan, 0.49.0): dashed on the ring, and one line under it. */
    plan: br.gabriel.cochilo.domain.DayPlan.Plan? = null,
    /** Drag to adjust: a finished sleep's new end (docs/flows.md). */
    onAdjust: (Event, Long) -> Unit = { _, _ -> },
    /** Sleep pressure today, the model's line (0.54.0); empty to show none. */
    pressure: List<br.gabriel.cochilo.domain.Pressure.Point> = emptyList(),
    /** Drag to adjust: a sleep's new start (0.53.0). */
    onAdjustStart: (Event, Long) -> Unit = { _, _ -> },
    /** For screenshots: one sleep's start shown mid-drag (its index, steps). */
    previewStartDrag: Pair<Int, Int>? = null,
    /** For screenshots: one sleep shown mid-drag (its index among the day's sleeps, steps). */
    previewDrag: Pair<Int, Int>? = null,
    /** The drag hint shows until the first drag (0.43.0). */
    showAdjustHint: Boolean = true,
    /** For screenshots: the kind filter already on. */
    initialFilter: EventType? = null,
) {
    val f = Format(LocalContext.current, zone, now)
    val today = localDate(now, zone)
    var epochDay by rememberSaveable { mutableLongStateOf((initialDay ?: today).toEpochDay()) }
    val day = LocalDate.ofEpochDay(epochDay)
    val from = startOfDay(day, zone)
    val to = startOfDay(day.plusDays(1), zone)
    val events = log.filter { it.overlaps(from, to, now) }.sortedBy { it.start }
    val dayNow = now.takeIf { it in from until to }

    ScrollPage {
        ScreenTitle(stringResource(R.string.title_day), stringResource(R.string.title_day))
        Spacer(Modifier.height(Spacing.space4))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(R.drawable.gg_ic_back, stringResource(R.string.day_previous), { epochDay -= 1 })
            Text(
                f.day(day), style = Type.title, color = CochiloTheme.colors.ink, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (day < today) IconButton(R.drawable.gg_ic_chevron, stringResource(R.string.day_next), { epochDay += 1 })
            else Spacer(Modifier.size(Spacing.space12))
        }

        // No caption over the ring (0.43.0): the ring says what it is; its "i" explains it.
        Spacer(Modifier.height(Spacing.space6))
        val sleepMs = events.filter { it.type == EventType.SLEEP }.sumOf { (minOf(it.end ?: now, to) - maxOf(it.start, from)).coerceAtLeast(0) }
        val naps = events.count { it.type == EventType.SLEEP && !it.isNightSleep(zone) && it.start >= from }
        val feeds = events.count { it.type == EventType.FEED && it.start >= from }
        val diapers = events.count { it.type == EventType.DIAPER && it.start >= from }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(CochiloTheme.colors.surface)
                .padding(vertical = Spacing.space4),
            contentAlignment = Alignment.Center,
        ) {
            val window = expected?.takeIf { day == today }
            val planned = plan?.sleeps?.takeIf { day == today }.orEmpty()
            val spoken = daySummary(f, day, events, from, to, now, zone) +
                (window?.let { " " + stringResource(R.string.ring_expected_spoken, f.time(it.first), f.time(it.second)) } ?: "") +
                (if (planned.size > 1) " " + stringResource(R.string.plan_spoken, planLine(planned.drop(1), f)) else "")
            DayRing(events, from, to, dayNow, zone, spoken, expected = window, planned = planned, time = { f.time(it) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(f.duration(sleepMs), style = Type.title, color = CochiloTheme.colors.ink, maxLines = 1, softWrap = false)
                    Text(stringResource(R.string.ring_sleep), style = Type.meta, color = CochiloTheme.colors.muted)
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(end = Spacing.space1)) {
                InfoButton(stringResource(R.string.caption_ring), stringResource(R.string.info_ring))
            }
        }
        Spacer(Modifier.height(Spacing.space3))
        Text(stringResource(R.string.ring_counts, naps, feeds, diapers), style = Type.bodySmall, color = CochiloTheme.colors.ink)
        // Today: what's still expected, in the order the ring shows it (0.49.0).
        val planned = plan?.sleeps?.takeIf { day == today }.orEmpty()
        if (planned.isNotEmpty()) {
            Text(stringResource(R.string.plan_expected, planLine(planned, f)), style = Type.bodySmall, color = CochiloTheme.colors.muted)
        }
        Spacer(Modifier.height(Spacing.space3))
        EventLegend(events, zone, expected = expected != null && day == today)

        // Sleep pressure, today only (0.54.0, docs/prediction.md): the model's line, said to be one.
        // Not at night (19:00–07:00): night is quieter, nothing extra to read (0.55.1 audit).
        if (day == today && pressure.size >= 2 && !br.gabriel.cochilo.domain.isNightStart(now, zone)) {
            val nowP = pressure.last().percent
            val rising = br.gabriel.cochilo.domain.Pressure.rising(events, now)
            val line = stringResource(if (rising) R.string.pressure_now_rising else R.string.pressure_now_falling, nowP)
            val meta = stringResource(R.string.pressure_meta, 100)
            Caption(stringResource(R.string.caption_pressure))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(CochiloTheme.colors.surface).padding(Spacing.space4)) {
                Column {
                    br.gabriel.cochilo.ui.kit.PressureLine(pressure, from, to, "$line. $meta")
                    Spacer(Modifier.height(Spacing.space1))
                    br.gabriel.cochilo.ui.kit.HourLabels()
                    Spacer(Modifier.height(Spacing.space2))
                    // One "i" per screen (the ring's): the model is said in the line instead (0.55.1 audit).
                    Text(line, style = Type.bodySmall, color = CochiloTheme.colors.ink)
                    Text(meta, style = Type.meta, color = CochiloTheme.colors.muted)
                }
            }
        }

        Caption(stringResource(R.string.caption_vertical))
        if (events.isEmpty()) {
            EmptyState(stringResource(R.string.empty_day_title), stringResource(R.string.empty_day_line))
            return@ScrollPage
        }
        // The kind filter (0.45.0, from Today's old Day by day): one tap shows one kind of entry;
        // "All" is the whole day, asleep and awake. Kept across days and the drag.
        var filter by rememberSaveable { mutableStateOf<String?>(initialFilter?.name) }
        val kinds = EventType.entries.filter { t -> events.any { it.type == t } }
        val shown = filter?.let { EventType.valueOf(it) }?.takeIf { it in kinds }
        if (kinds.size > 1) {
            val all = stringResource(R.string.filter_all)
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    FilterChips(listOf<String?>(null) + kinds.map { it.name }, shown?.name, { k -> k?.let { f.typeName(EventType.valueOf(it)) } ?: all }) { filter = it }
                }
            }
            Spacer(Modifier.height(Spacing.space4))
        }
        if (shown != null) {
            EventList(events.filter { it.type == shown && it.start >= from }.sortedByDescending { it.start }, now, f, zone, onOpen)
            return@ScrollPage
        }
        if (showAdjustHint) Text(stringResource(R.string.adjust_hint), style = Type.meta, color = CochiloTheme.colors.muted, modifier = Modifier.padding(bottom = Spacing.space2))
        VerticalTimeline(
            events, now, f, zone, onOpen, onAdjust = { e, end -> onAdjust(e, end) }, allSleeps = log, previewDrag = previewDrag,
            onAdjustStart = { e, start -> onAdjustStart(e, start) }, previewStartDrag = previewStartDrag,
            // Every sleep still expected today, on top (0.61.0): the plan, like the ring.
            planned = plan?.sleeps?.takeIf { day == today }.orEmpty(),
        )
    }
}

@Composable
internal fun VerticalTimeline(
    events: List<Event>, now: Long, f: Format, zone: ZoneId, onOpen: (Event) -> Unit,
    onAdjust: ((Event, Long) -> Unit)? = null,
    /** Every sleep, so a drag stops at the next one even across midnight. */
    allSleeps: List<Event> = events,
    previewDrag: Pair<Int, Int>? = null,
    onAdjustStart: ((Event, Long) -> Unit)? = null,
    previewStartDrag: Pair<Int, Int>? = null,
    /**
     * The rest of today, projected (DayPlan): every expected sleep above what's happening now,
     * latest at the top (0.61.0; only the next one before). The next one is teal and breathes.
     */
    planned: List<br.gabriel.cochilo.domain.DayPlan.Sleep> = emptyList(),
    /** For the screenshot test: the next one's breath, still at this point. */
    breathOverride: Float? = null,
) {
    val c = CochiloTheme.colors
    val sleepsSorted = allSleeps.filter { it.type == EventType.SLEEP }.sortedBy { it.start }
    val later = stringResource(R.string.adjust_later)
    val earlier = stringResource(R.string.adjust_earlier)
    val previewWords = stringResource(R.string.adjust_preview)
    val startWords = stringResource(R.string.adjust_start_preview)
    val startLater = stringResource(R.string.adjust_start_later)
    val startEarlier = stringResource(R.string.adjust_start_earlier)
    // The day's bounds: the day of the newest entry (the screen passes one day's entries).
    val day = br.gabriel.cochilo.domain.localDate(events.maxOfOrNull { it.start } ?: now, zone)
    val from = br.gabriel.cochilo.domain.startOfDay(day, zone)
    val to = br.gabriel.cochilo.domain.startOfDay(day.plusDays(1), zone)
    // Asleep and awake (0.35.0): the two main states, newest first; what happened during each is set in under it.
    val stretches = br.gabriel.cochilo.domain.Stretches.of(events, allSleeps, from, to, now)
    val placed = stretches.flatMap { s -> s.during.map { it.id } + listOfNotNull(s.sleep?.id) }.toSet()
    val leftovers = events.filter { it.id !in placed }.sortedByDescending { it.start }
    var sleepIndex = 0

    fun bar(ms: Long) = Spacing.space1 * br.gabriel.cochilo.domain.TimelineScale.units((ms / br.gabriel.cochilo.domain.MINUTE).toInt())

    // The secondary track (0.48.0): an outline in the entry's colour, a plain icon, smaller words.
    @Composable
    fun Secondary(e: Event) {
        br.gabriel.cochilo.ui.kit.TimelineEntry(
            color = c.eventColor(e, zone),
            barHeight = if (e.type.hasDuration) bar(e.durationMs(now)) else null,
            label = eventLabel(e, f, zone),
            meta = eventMeta(e, now, f),
            icon = br.gabriel.cochilo.ui.kit.entryIcon(e),
            onClick = { onOpen(e) },
        )
    }

    // The main track: one block per sleep or awake stretch, its bar as tall as its length suggests
    // (TimelineScale), with what happened during it beside the same bar.
    @Composable
    fun Sleep(e: Event, during: List<Event>) {
        br.gabriel.cochilo.ui.kit.TimelineStretch(
            time = f.time(e.start),
            color = c.eventColor(e, zone),
            barHeight = bar(e.durationMs(now)),
            label = eventLabel(e, f, zone),
            meta = eventMeta(e, now, f),
            icon = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) },
            onClick = { onOpen(e) },
            durationMinutes = (e.durationMs(now) / br.gabriel.cochilo.domain.MINUTE).toInt(),
            adjust = if (onAdjust != null && br.gabriel.cochilo.domain.Adjust.canAdjust(e)) {
                val next = sleepsSorted.firstOrNull { it.start > e.start && it.id != e.id }?.start
                fun end(steps: Int) = br.gabriel.cochilo.domain.Adjust.newEnd(e, steps, now, next)
                br.gabriel.cochilo.ui.kit.TimelineAdjust(
                    preview = { steps ->
                        val to = end(steps) ?: e.end!!
                        val delta = ((to - e.end!!) / br.gabriel.cochilo.domain.MINUTE).toInt()
                        val sign = if (delta > 0) "+" else if (delta < 0) "−" else ""
                        previewWords.format(f.time(to), sign + f.durationMin(kotlin.math.abs(delta)))
                    },
                    deltaMinutes = { steps -> end(steps)?.let { ((it - e.end!!) / br.gabriel.cochilo.domain.MINUTE).toInt() } ?: 0 },
                    onCommit = { steps -> end(steps)?.let { onAdjust(e, it) } },
                    laterLabel = later, earlierLabel = earlier,
                )
            } else null,
            previewSteps = if (previewDrag?.first == sleepIndex) previewDrag.second else null,
            adjustStart = if (onAdjustStart != null && br.gabriel.cochilo.domain.Adjust.canAdjustStart(e)) {
                val prev = sleepsSorted.lastOrNull { it.start < e.start && it.id != e.id }?.end
                fun start(steps: Int) = br.gabriel.cochilo.domain.Adjust.newStart(e, steps, now, prev)
                br.gabriel.cochilo.ui.kit.TimelineAdjust(
                    preview = { steps ->
                        val at = start(steps) ?: e.start
                        val d = ((at - e.start) / br.gabriel.cochilo.domain.MINUTE).toInt()
                        val sign = if (d > 0) "+" else if (d < 0) "−" else ""
                        startWords.format(f.time(at), sign + f.durationMin(kotlin.math.abs(d)))
                    },
                    deltaMinutes = { steps -> start(steps)?.let { ((it - e.start) / br.gabriel.cochilo.domain.MINUTE).toInt() } ?: 0 },
                    onCommit = { steps -> start(steps)?.let { onAdjustStart(e, it) } },
                    laterLabel = startLater, earlierLabel = startEarlier,
                    previewTime = { steps -> f.time(start(steps) ?: e.start) },
                )
            } else null,
            previewStartSteps = if (previewStartDrag?.first == sleepIndex) previewStartDrag.second else null,
        ) { during.forEach { Secondary(it) } }
        sleepIndex++
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface)
            .padding(vertical = Spacing.space3)
    ) {
        for ((i, expected) in planned.withIndex().reversed()) {
            val next = i == 0
            val bed = expected.next == br.gabriel.cochilo.domain.Predictor.Next.BEDTIME
            val minutes = ((expected.end - expected.at) / br.gabriel.cochilo.domain.MINUTE).toInt()
            val window = if (bed) stringResource(R.string.expected_meta_bed, f.time(expected.windowStart), f.time(expected.windowEnd))
            else stringResource(R.string.expected_meta, f.time(expected.windowStart), f.time(expected.windowEnd), f.durationMin(minutes))
            br.gabriel.cochilo.ui.kit.TimelineExpected(
                time = stringResource(R.string.expected_time, f.time(expected.at)),
                // The next sleep is the brand's teal, as on the ring (0.59.0); the later ones keep
                // their own colour, dotted (0.61.0).
                color = if (next) c.mint else if (bed) c.eventNight else c.eventNap,
                breathes = next, breathOverride = breathOverride,
                // Bedtime's bar is short: the night's length isn't the point here.
                barHeight = if (bed) Spacing.space6 else bar(minutes * br.gabriel.cochilo.domain.MINUTE),
                label = stringResource(if (bed) R.string.expected_bedtime else R.string.expected_nap),
                // Past its window with nothing logged: said plainly, never in red.
                meta = if (now > expected.windowEnd) stringResource(R.string.expected_past) + " · " + window else window,
            )
        }
        for (s in stretches) {
            if (s.asleep) Sleep(s.sleep!!, s.during)
            else br.gabriel.cochilo.ui.kit.TimelineStretch(
                time = s.start?.let { f.time(it) } ?: "",
                color = null,
                // Awake on the same scale, as an outline: the gaps between sleeps read too.
                barHeight = s.start?.let { st -> bar((s.end ?: now) - st) },
                label = stringResource(R.string.stretch_awake),
                meta = awakeMeta(s, now, f),
                icon = null,
            ) { s.during.forEach { Secondary(it) } }
        }
        for (e in leftovers) Secondary(e)
    }
}

/** "09:10–11:40 · 2h 30m", "Since 14:20 · 45m", or null when it began before the log knows. */
@Composable
internal fun awakeMeta(s: br.gabriel.cochilo.domain.Stretches.Stretch, now: Long, f: Format): String? {
    val start = s.start ?: return null
    return if (s.end == null) stringResource(R.string.panel_since, f.time(start)) + " · " + f.duration(clockSpan(start, now))
    else f.time(start) + "–" + f.time(s.end) + " · " + f.duration(clockSpan(start, s.end))
}
