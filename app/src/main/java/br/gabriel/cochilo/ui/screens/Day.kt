package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.background
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
import br.gabriel.cochilo.ui.kit.IconButton
import br.gabriel.cochilo.ui.kit.InfoButton
import br.gabriel.cochilo.ui.kit.ScreenTitle
import br.gabriel.cochilo.ui.kit.TimelineGap
import br.gabriel.cochilo.ui.kit.TimelineRow
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
    /** Drag to adjust: a finished sleep's new end (docs/flows.md). */
    onAdjust: (Event, Long) -> Unit = { _, _ -> },
    /** For screenshots: one sleep shown mid-drag (its index among the day's sleeps, steps). */
    previewDrag: Pair<Int, Int>? = null,
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

        Caption(stringResource(R.string.caption_ring), top = Spacing.space6)
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
            val spoken = daySummary(f, day, events, from, to, now, zone) +
                (window?.let { " " + stringResource(R.string.ring_expected_spoken, f.time(it.first), f.time(it.second)) } ?: "")
            DayRing(events, from, to, dayNow, zone, spoken, expected = window) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(f.duration(sleepMs), style = Type.title, color = CochiloTheme.colors.ink)
                    Text(stringResource(R.string.ring_sleep), style = Type.meta, color = CochiloTheme.colors.muted)
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(end = Spacing.space1)) {
                InfoButton(stringResource(R.string.caption_ring), stringResource(R.string.info_ring))
            }
        }
        Spacer(Modifier.height(Spacing.space3))
        Text(stringResource(R.string.ring_counts, naps, feeds, diapers), style = Type.bodySmall, color = CochiloTheme.colors.ink)
        Spacer(Modifier.height(Spacing.space3))
        EventLegend(expected = expected != null && day == today)

        Caption(stringResource(R.string.caption_vertical))
        if (events.isEmpty()) {
            EmptyState(stringResource(R.string.empty_day_title), stringResource(R.string.empty_day_line))
            return@ScrollPage
        }
        Text(stringResource(R.string.adjust_hint), style = Type.meta, color = CochiloTheme.colors.muted, modifier = Modifier.padding(bottom = Spacing.space2))
        VerticalTimeline(events, now, f, zone, onOpen, onAdjust = { e, end -> onAdjust(e, end) }, allSleeps = log, previewDrag = previewDrag)
    }
}

@Composable
internal fun VerticalTimeline(
    events: List<Event>, now: Long, f: Format, zone: ZoneId, onOpen: (Event) -> Unit,
    onAdjust: ((Event, Long) -> Unit)? = null,
    /** Every sleep, so a drag stops at the next one even across midnight. */
    allSleeps: List<Event> = events,
    previewDrag: Pair<Int, Int>? = null,
) {
    val c = CochiloTheme.colors
    var lastSleepEnd: Long? = null
    val sleepsSorted = allSleeps.filter { it.type == EventType.SLEEP }.sortedBy { it.start }
    var sleepIndex = 0
    val later = stringResource(R.string.adjust_later)
    val earlier = stringResource(R.string.adjust_earlier)
    val previewWords = stringResource(R.string.adjust_preview)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(c.surface)
            .padding(vertical = Spacing.space3)
    ) {
        for (e in events) {
            if (e.type == EventType.SLEEP) {
                lastSleepEnd?.let { end -> if (e.start > end) TimelineGap(stringResource(R.string.awake_gap, f.duration(clockSpan(end, e.start)))) }
                lastSleepEnd = e.end ?: now
            }
            // A sleep's bar grows 4dp for every 10 minutes, so long sleeps look long.
            val bar = if (e.type.hasDuration) Spacing.space1 * (e.durationMs(now) / 600_000f).coerceAtMost(80f) else null
            TimelineRow(
                time = f.time(e.start),
                color = c.eventColor(e, zone),
                barHeight = bar,
                label = eventLabel(e, f, zone),
                meta = eventMeta(e, now, f),
                onClick = { onOpen(e) },
                icon = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) },
                prominent = e.type == br.gabriel.cochilo.data.EventType.SLEEP,
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
                previewSteps = if (e.type == EventType.SLEEP && previewDrag?.first == sleepIndex) previewDrag.second else null,
            )
            if (e.type == EventType.SLEEP) sleepIndex++
        }
    }
}
