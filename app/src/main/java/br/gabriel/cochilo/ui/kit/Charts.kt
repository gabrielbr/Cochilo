package br.gabriel.cochilo.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.isNightSleep
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import java.time.ZoneId

/** The data colour for an event. Data colours only ever appear on data, next to a word. */
fun CTokens.Palette.eventColor(e: Event, zone: ZoneId): Color = when (e.type) {
    EventType.SLEEP -> if (e.isNightSleep(zone)) eventNight else eventNap
    EventType.FEED -> eventFeed
    EventType.PUMP -> eventPump
    EventType.DIAPER -> eventDiaper
    EventType.DAYLIGHT -> eventDaylight
    EventType.ROUTINE, EventType.NOTE, EventType.MEDICINE -> muted
}

/**
 * One day as a 24-hour bar: sleep fills the bar, feeds and pumping mark the top third, diapers and
 * daylight the bottom third. [summary] is what TalkBack reads instead of the picture.
 */
@Composable
fun Timeline(events: List<Event>, from: Long, to: Long, now: Long?, zone: ZoneId, summary: String) {
    val c = CochiloTheme.colors
    val density = LocalDensity.current
    val tick = with(density) { Spacing.space1.toPx() }
    val hair = with(density) { Size.hairline.toPx() }
    val radius = with(density) { Radius.radiusS.toPx() }
    Canvas(
        Modifier.fillMaxWidth().height(Size.timeline)
            .clip(RoundedCornerShape(Radius.radiusS))
            .semantics { contentDescription = summary }
    ) {
        val w = size.width
        val h = size.height
        val span = (to - from).toFloat()
        fun x(t: Long) = ((t - from).coerceIn(0, to - from) / span) * w
        drawRoundRect(c.line, cornerRadius = CornerRadius(radius))
        // Hour guides every 6 hours, on the track only.
        for (i in 1..3) drawRect(c.page, Offset(w * i / 4f - hair / 2, 0f), GSize(hair, h))
        val third = h / 3f
        for (e in events) {
            if (!e.type.hasDuration) continue
            val end = e.end ?: now ?: continue
            if (end < from || e.start >= to) continue
            val col = c.eventColor(e, zone)
            val x0 = x(e.start)
            val x1 = maxOf(x(end), x0 + tick)
            when (e.type) {
                EventType.SLEEP -> drawRect(col, Offset(x0, 0f), GSize(x1 - x0, h))
                EventType.FEED, EventType.PUMP -> drawRect(col, Offset(x0, 0f), GSize(maxOf(x1 - x0, tick), third))
                EventType.DAYLIGHT -> drawRect(col, Offset(x0, h - third), GSize(x1 - x0, third))
                else -> Unit
            }
        }
        for (e in events) {
            if (e.type.hasDuration) continue // instants: diapers, routines, notes
            if (e.start < from || e.start >= to) continue
            val col = c.eventColor(e, zone)
            drawRect(col, Offset(x(e.start), h - third), GSize(tick, third))
        }
        if (now != null && now in from until to) drawRect(c.ink, Offset(x(now) - hair, 0f), GSize(hair * 2, h))
    }
}

/** 00 · 06 · 12 · 18 · 24 under a timeline. */
@Composable
fun HourLabels() {
    val c = CochiloTheme.colors
    Row(Modifier.fillMaxWidth()) {
        listOf("00", "06", "12", "18", "24").forEachIndexed { i, s ->
            Text(
                s, style = Type.meta, color = c.muted,
                textAlign = when (i) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center },
                modifier = Modifier.weight(if (i == 0 || i == 4) 0.5f else 1f),
            )
        }
    }
}

data class LegendItem(val label: String, val color: Color)

@Composable
fun Legend(items: List<LegendItem>) {
    val c = CochiloTheme.colors
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space2),
    ) {
        items.forEach { li ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(li.color)
                Spacer(Modifier.width(Spacing.space1))
                Text(li.label, style = Type.meta, color = c.ink)
            }
        }
    }
}

/** Days stacked under each other, oldest first, so the day/night pattern shows as it forms. */
@Composable
fun Actogram(days: List<Triple<String, Long, Long>>, events: List<Event>, now: Long, zone: ZoneId, summaries: List<String>) {
    val c = CochiloTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        days.forEachIndexed { i, (label, from, to) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = Type.meta, color = c.muted, modifier = Modifier.width(Spacing.space16 + Spacing.space4), maxLines = 1)
                Column(Modifier.weight(1f)) {
                    Timeline(events, from, to, now, zone, summaries[i])
                }
            }
        }
        Row {
            Spacer(Modifier.width(Spacing.space16 + Spacing.space4))
            Column(Modifier.weight(1f)) { HourLabels() }
        }
    }
}
