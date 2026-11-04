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
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
    // Pumped milk says when it was expressed: day milk or night milk (research §2.4).
    val milk = if (e.type == EventType.PUMP) stringResource(if (br.gabriel.cochilo.domain.Milk.isNight(e, zone)) R.string.milk_night else R.string.milk_day) else null
    val parts = listOfNotNull(base, f.kindName(e.kind), e.amountMl?.let { stringResource(R.string.ml, it) }, milk)
    return parts.joinToString(" · ")
}

/** "07:00–07:20 · 20m", "08:00", or "07:00 · running". */
/**
 * This parent's own initial, when the phone syncs (0.52.0): entries logged by someone else show
 * theirs ("by A"); this parent's own show nothing. Unpaired, nothing shows.
 */
val LocalSyncMe = androidx.compose.runtime.compositionLocalOf<String?> { null }

/** "by A" for an entry the partner logged, or null (docs/sync.md, 0.52.0). */
@Composable
fun loggedByLabel(e: Event): String? {
    val me = LocalSyncMe.current ?: return null
    val by = e.loggedBy?.takeIf { it.isNotEmpty() && it != me } ?: return null
    return stringResource(R.string.sync_by, by)
}

@Composable
fun eventMeta(e: Event, now: Long, f: Format): String {
    val times = when {
        !e.type.hasDuration -> f.time(e.start)
        e.end == null -> stringResource(R.string.panel_since, f.time(e.start)) + " · " + f.duration(clockSpan(e.start, now))
        else -> f.time(e.start) + "–" + f.time(e.end) + " · " + f.duration(clockSpan(e.start, e.end))
    }
    val base = if (e.note.isNullOrBlank()) times else "$times · ${e.note}"
    return loggedByLabel(e)?.let { "$base · $it" } ?: base
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
                // Sleep leads: what Cochilo is for is a size up in every list.
                prominent = e.type == br.gabriel.cochilo.data.EventType.SLEEP,
                editable = true,
            )
        }
    }
}

/**
 * A day's entries as its asleep and awake stretches, newest first (docs/flows.md, "Asleep and
 * awake"): each stretch is a main row, and what happened during it is set in under it.
 */
@Composable
fun StretchList(events: List<Event>, allSleeps: List<Event>, from: Long, to: Long, now: Long, f: Format, zone: ZoneId, onOpen: (Event) -> Unit) {
    val stretches = br.gabriel.cochilo.domain.Stretches.of(events, allSleeps, from, to, now)
    val placed = stretches.flatMap { s -> s.during.map { it.id } + listOfNotNull(s.sleep?.id) }.toSet()
    val awake = stringResource(R.string.stretch_awake)
    RowGroup {
        var first = true
        fun gap() = if (first) { first = false; false } else true
        for (s in stretches) {
            // An awake stretch with nothing in it and no known start says nothing: leave it out.
            if (!s.asleep && s.during.isEmpty() && s.start == null) continue
            if (gap()) Hairline()
            if (s.asleep) {
                val e = s.sleep!!
                ListRow(eventLabel(e, f, zone), meta = eventMeta(e, now, f), leading = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) }, onClick = { onOpen(e) }, prominent = true, editable = true)
            } else {
                Box(Modifier.semantics(mergeDescendants = true) { heading() }) {
                    ListRow(awake, meta = awakeMeta(s, now, f), leading = { br.gabriel.cochilo.ui.kit.AwakeMark() })
                }
            }
            for (e in s.during) {
                Hairline()
                ListRow(eventLabel(e, f, zone), meta = eventMeta(e, now, f), leading = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) }, onClick = { onOpen(e) }, indent = true, editable = true)
            }
        }
        for (e in events.filter { it.id !in placed && it.type != EventType.SLEEP }.sortedByDescending { it.start }) {
            if (gap()) Hairline()
            ListRow(eventLabel(e, f, zone), meta = eventMeta(e, now, f), leading = { br.gabriel.cochilo.ui.kit.KindIcon(e, zone) }, onClick = { onOpen(e) }, editable = true)
        }
    }
}

/**
 * The colour key, only for the kinds [events] has (less is more, 0.51.0): a day with no pump
 * doesn't explain pump's colour. With nothing logged it shows nothing.
 */
@Composable
fun EventLegend(events: List<Event>, zone: ZoneId, expected: Boolean = false) {
    val c = CochiloTheme.colors
    val sleeps = events.filter { it.type == EventType.SLEEP }
    val types = events.map { it.type }.toSet()
    val items = listOfNotNull(
        if (sleeps.any { it.isNightSleep(zone) }) LegendItem(stringResource(R.string.sleep_night), c.eventNight) else null,
        if (sleeps.any { !it.isNightSleep(zone) }) LegendItem(stringResource(R.string.sleep_nap), c.eventNap) else null,
        if (EventType.FEED in types) LegendItem(stringResource(R.string.type_feed), c.eventFeed) else null,
        if (EventType.PUMP in types) LegendItem(stringResource(R.string.type_pump), c.eventPump) else null,
        if (EventType.DIAPER in types) LegendItem(stringResource(R.string.type_diaper), c.eventDiaper) else null,
        if (EventType.DAYLIGHT in types) LegendItem(stringResource(R.string.type_daylight), c.eventDaylight) else null,
        if (expected) LegendItem(stringResource(R.string.ring_expected), c.eventNap.copy(alpha = 0.35f)) else null,
    )
    if (items.isNotEmpty()) Legend(items)
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
