package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.domain.overlaps
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.domain.Reference
import br.gabriel.cochilo.domain.Stats
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.domain.startOfDay
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Actogram
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.EmptyState
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.HourLabels
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.kit.ScreenTitle
import br.gabriel.cochilo.ui.kit.Timeline
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import java.time.ZoneId
import java.util.Locale

/** Day by day, newest first: a summary line, the day's timeline, then its entries. */
@Composable
fun HistoryScreen(log: List<Event>, now: Long, zone: ZoneId, onOpen: (Event) -> Unit) {
    val f = Format(LocalContext.current, zone, now)
    val stats = Stats(zone)
    val days = log.map { localDate(it.start, zone) }.distinct().sortedDescending().take(60)
    ScrollPage {
        ScreenTitle(stringResource(R.string.title_history), stringResource(R.string.title_history))
        if (days.isEmpty()) {
            EmptyState(stringResource(R.string.empty_history_title), stringResource(R.string.empty_history_line))
            return@ScrollPage
        }
        days.forEachIndexed { i, d ->
            val from = startOfDay(d, zone)
            val to = startOfDay(d.plusDays(1), zone)
            val dayEvents = log.filter { it.overlaps(from, to, now) }
            val s = stats.day(log, d, now)
            Caption(f.day(d), top = if (i == 0) Spacing.space8 else Spacing.space10)
            Text(
                stringResource(R.string.day_summary, f.durationMin(s.totalSleepMin), s.naps, s.feeds),
                style = Type.bodySmall, color = CochiloTheme.colors.ink,
            )
            Spacer(Modifier.height(Spacing.space3))
            Timeline(dayEvents, from, to, now, zone, daySummary(f, d, dayEvents, from, to, now, zone))
            Spacer(Modifier.height(Spacing.space1))
            HourLabels()
            Spacer(Modifier.height(Spacing.space3))
            EventList(log.filter { it.start in from until to }.sortedByDescending { it.start }, now, f, zone, onOpen)
        }
    }
}

/** Rhythm (14-day actogram) and the 7-day averages against the reference ranges. */
@Composable
fun PatternsScreen(log: List<Event>, now: Long, zone: ZoneId, ageWeeks: Int) {
    val f = Format(LocalContext.current, zone, now)
    val stats = Stats(zone)
    val today = localDate(now, zone)
    val week = stats.days(log, today, 7, now)
    val avg = stats.averages(week)
    ScrollPage {
        ScreenTitle(stringResource(R.string.title_patterns), stringResource(R.string.title_patterns))
        if (avg == null) {
            EmptyState(stringResource(R.string.empty_patterns_title), stringResource(R.string.empty_patterns_line))
            return@ScrollPage
        }
        // What changed (research §2.5): neutral sentences with both numbers, only real changes.
        val changes = br.gabriel.cochilo.domain.Changes.detect(log, today, now, stats)
        if (changes.isNotEmpty()) {
            Caption(stringResource(R.string.caption_changes), top = Spacing.space8)
            RowGroup {
                changes.forEachIndexed { i, ch ->
                    if (i > 0) Hairline()
                    val (head, values) = changeRow(ch, f)
                    ListRow(head, meta = values, leading = { br.gabriel.cochilo.ui.kit.KindIcon(br.gabriel.cochilo.data.EventType.SLEEP) })
                }
            }
            Spacer(Modifier.height(Spacing.space2))
            Text(stringResource(R.string.changes_meta), style = Type.meta, color = CochiloTheme.colors.muted)
        }
        Caption(stringResource(R.string.caption_rhythm), top = if (changes.isEmpty()) Spacing.space8 else Spacing.space10)
        val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
        val rows = days.map { Triple(f.day(it), startOfDay(it, zone), startOfDay(it.plusDays(1), zone)) }
        val summaries = days.map { d ->
            val from = startOfDay(d, zone)
            val to = startOfDay(d.plusDays(1), zone)
            daySummary(f, d, log.filter { it.overlaps(from, to, now) }, from, to, now, zone)
        }
        Actogram(rows, log, now, zone, summaries)
        Spacer(Modifier.height(Spacing.space3))
        EventLegend()
        Spacer(Modifier.height(Spacing.space2))
        Text(stringResource(R.string.rhythm_meta), style = Type.meta, color = CochiloTheme.colors.muted)

        Caption(stringResource(R.string.caption_week))
        val range = Reference.totalSleepRange(ageWeeks)
        fun h(x: Double) = String.format(Locale.getDefault(), "%.1f", x)
        RowGroup {
            ListRow(
                stringResource(R.string.total_sleep),
                meta = stringResource(R.string.typical_range, h(range.lowHours), h(range.highHours), range.source),
                value = f.durationMin(avg.totalSleepMin),
            )
            Hairline()
            ListRow(stringResource(R.string.night_sleep), value = f.durationMin(avg.nightSleepMin))
            Hairline()
            ListRow(stringResource(R.string.naps), value = stringResource(R.string.naps_value, avg.naps, f.durationMin(avg.napSleepMin)))
            Hairline()
            ListRow(stringResource(R.string.longest_stretch), value = f.durationMin(avg.longestStretchMin))
            Hairline()
            ListRow(stringResource(R.string.night_wakings), value = String.format(Locale.getDefault(), "%.1f", avg.nightWakings))
            Hairline()
            ListRow(stringResource(R.string.feeds), value = stringResource(R.string.per_day, avg.feeds))
            Hairline()
            ListRow(stringResource(R.string.diapers), value = stringResource(R.string.per_day, avg.diapers))
            Hairline()
            ListRow(stringResource(R.string.daylight), meta = stringResource(R.string.daylight_meta), value = f.durationMin(avg.daylightMin))
            val (dayMl, nightMl) = br.gabriel.cochilo.domain.Milk.pumped(log, zone, now)
            if (dayMl + nightMl > 0) {
                Hairline()
                ListRow(
                    stringResource(R.string.pumped_week), meta = stringResource(R.string.pumped_week_meta),
                    value = stringResource(R.string.pumped_split, dayMl, nightMl),
                )
            }
        }

        // Where naps happened (docs/places.md): side by side, never judged; contact naps are normal.
        val places = br.gabriel.cochilo.domain.Places.napsByPlace(log, zone, now)
        if (places.isNotEmpty()) {
            Caption(stringResource(R.string.caption_places))
            RowGroup {
                places.forEachIndexed { i, (kind, n) ->
                    if (i > 0) Hairline()
                    ListRow(f.kindName(kind) ?: kind, value = stringResource(R.string.naps_count, n))
                }
            }
            Spacer(Modifier.height(Spacing.space2))
            Text(stringResource(R.string.places_meta), style = Type.meta, color = CochiloTheme.colors.muted)
        }
    }
}

/** One change as a short neutral head ("Night sleep is longer") and its two numbers. */
@Composable
private fun changeRow(c: br.gabriel.cochilo.domain.Change, f: Format): Pair<String, String> {
    fun d(x: Double) = f.durationMin(x.toInt())
    fun n(x: Double) = String.format(Locale.getDefault(), "%.1f", x)
    val head = stringResource(when (c.metric) {
        br.gabriel.cochilo.domain.Metric.NIGHT_SLEEP -> if (c.up) R.string.change_night_up else R.string.change_night_down
        br.gabriel.cochilo.domain.Metric.NAP_SLEEP -> if (c.up) R.string.change_naptime_up else R.string.change_naptime_down
        br.gabriel.cochilo.domain.Metric.LONGEST_STRETCH -> if (c.up) R.string.change_stretch_up else R.string.change_stretch_down
        br.gabriel.cochilo.domain.Metric.NIGHT_WAKINGS -> if (c.up) R.string.change_wakings_up else R.string.change_wakings_down
        br.gabriel.cochilo.domain.Metric.NAPS -> if (c.up) R.string.change_naps_up else R.string.change_naps_down
    })
    val values = when (c.metric) {
        br.gabriel.cochilo.domain.Metric.NIGHT_SLEEP, br.gabriel.cochilo.domain.Metric.LONGEST_STRETCH -> stringResource(R.string.change_values, d(c.now), d(c.before))
        br.gabriel.cochilo.domain.Metric.NAP_SLEEP -> stringResource(R.string.change_values_day, d(c.now), d(c.before))
        br.gabriel.cochilo.domain.Metric.NIGHT_WAKINGS -> stringResource(R.string.change_values_night, n(c.now), n(c.before))
        br.gabriel.cochilo.domain.Metric.NAPS -> stringResource(R.string.change_values_day, n(c.now), n(c.before))
    }
    return head to values
}
