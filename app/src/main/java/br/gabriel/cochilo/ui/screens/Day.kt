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
fun DayScreen(log: List<Event>, now: Long, zone: ZoneId, onOpen: (Event) -> Unit, initialDay: LocalDate? = null) {
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
            DayRing(events, from, to, dayNow, zone, daySummary(f, day, events, from, to, now, zone)) {
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
        EventLegend()

        Caption(stringResource(R.string.caption_vertical))
        if (events.isEmpty()) {
            EmptyState(stringResource(R.string.empty_day_title), stringResource(R.string.empty_day_line))
            return@ScrollPage
        }
        VerticalTimeline(events, now, f, zone, onOpen)
    }
}

@Composable
internal fun VerticalTimeline(events: List<Event>, now: Long, f: Format, zone: ZoneId, onOpen: (Event) -> Unit) {
    val c = CochiloTheme.colors
    var lastSleepEnd: Long? = null
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
            )
        }
    }
}
