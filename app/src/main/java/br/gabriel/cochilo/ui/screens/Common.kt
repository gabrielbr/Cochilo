package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.domain.clockSpan
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.isNightSleep
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.Legend
import br.gabriel.cochilo.ui.kit.LegendItem
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.kit.eventColor
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import java.time.LocalDate
import java.time.ZoneId

/** A scrolling page: gutter 24 (32 from 600dp), 32 under the header, room at the bottom. */
@Composable
fun ScrollPage(content: @Composable ColumnScope.() -> Unit) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val gutter = if (wide) Spacing.space8 else Spacing.space6
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = gutter, end = gutter, top = Spacing.space8, bottom = Spacing.space10)
    ) {
        content()
        Spacer(Modifier.height(Spacing.space8))
    }
}

/** What the "i" beside an entry type says. */
fun infoText(t: EventType): Int = when (t) {
    EventType.SLEEP -> R.string.info_sleep
    EventType.FEED -> R.string.info_feed
    EventType.PUMP -> R.string.info_pump
    EventType.DIAPER -> R.string.info_diaper
    EventType.DAYLIGHT -> R.string.info_daylight
    EventType.ROUTINE -> R.string.info_routine
    EventType.NOTE -> R.string.info_note
    EventType.MEDICINE -> R.string.info_medicine
}

fun CTokens.Palette.eventColorOf(e: Event, zone: ZoneId): Color = eventColor(e, zone)

/** "Nap", "Feed · Left breast", "Pump · 90 ml". */
@Composable
fun eventLabel(e: Event, f: Format, zone: ZoneId): String {
    val base = if (e.type == EventType.SLEEP) {
        stringResource(if (e.isNightSleep(zone)) R.string.sleep_night else R.string.sleep_nap)
    } else f.typeName(e.type)
    val parts = listOfNotNull(base, f.kindName(e.kind), e.amountMl?.let { stringResource(R.string.ml, it) })
    return parts.joinToString(" · ")
}

/** "07:00–07:20 · 20m", "08:00", or "07:00 · running". */
@Composable
fun eventMeta(e: Event, now: Long, f: Format): String {
    val times = when {
        !e.type.hasDuration -> f.time(e.start)
        e.end == null -> stringResource(R.string.panel_since, f.time(e.start)) + " · " + f.duration(clockSpan(e.start, now))
        else -> f.time(e.start) + "–" + f.time(e.end) + " · " + f.duration(clockSpan(e.start, e.end))
    }
    return if (e.note.isNullOrBlank()) times else "$times · ${e.note}"
}

@Composable
fun EventList(events: List<Event>, now: Long, f: Format, zone: ZoneId, onOpen: (Event) -> Unit) {
    RowGroup {
        events.forEachIndexed { i, e ->
            if (i > 0) Hairline()
            ListRow(
                label = eventLabel(e, f, zone),
                meta = eventMeta(e, now, f),
                leading = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) },
                onClick = { onOpen(e) },
            )
        }
    }
}

@Composable
fun EventLegend() {
    val c = CochiloTheme.colors
    Legend(
        listOf(
            LegendItem(stringResource(R.string.sleep_night), c.eventNight),
            LegendItem(stringResource(R.string.sleep_nap), c.eventNap),
            LegendItem(stringResource(R.string.type_feed), c.eventFeed),
            LegendItem(stringResource(R.string.type_pump), c.eventPump),
            LegendItem(stringResource(R.string.type_diaper), c.eventDiaper),
            LegendItem(stringResource(R.string.type_daylight), c.eventDaylight),
        )
    )
}

/** What TalkBack reads for a day's timeline. */
@Composable
fun daySummary(f: Format, date: LocalDate, events: List<Event>, from: Long, to: Long, now: Long, zone: ZoneId): String {
    val sleepMs = events.filter { it.type == EventType.SLEEP }.sumOf { e ->
        (minOf(e.end ?: now, to) - maxOf(e.start, from)).coerceAtLeast(0)
    }
    val naps = events.count { it.type == EventType.SLEEP && !it.isNightSleep(zone) && it.start >= from }
    val feeds = events.count { it.type == EventType.FEED && it.start >= from }
    val diapers = events.count { it.type == EventType.DIAPER && it.start >= from }
    return stringResource(R.string.timeline_summary, f.day(date), f.duration(sleepMs), naps, feeds, diapers)
}
