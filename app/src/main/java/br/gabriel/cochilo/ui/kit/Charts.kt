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
fun CTokens.Palette.eventColor(e: Event, zone: ZoneId): Color =
    kindColor(e.type, e.type != EventType.SLEEP || e.isNightSleep(zone))

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

/**
 * Sleep pressure today (0.54.0, docs/prediction.md): a line from midnight to now, on the same
 * 24-hour axis as the timeline, with a dashed guide at 100% (about when the baby usually falls
 * asleep). The line is `ink`, the guide `muted` (3:1 on surface, as a graphic that means
 * something must be; 0.55.1 audit): it's a model, so no data colour. [summary] is what
 * TalkBack reads.
 */
@Composable
fun PressureLine(points: List<br.gabriel.cochilo.domain.Pressure.Point>, from: Long, to: Long, summary: String) {
    val c = CochiloTheme.colors
    val density = LocalDensity.current
    val stroke = with(density) { (Size.hairline * 2).toPx() }
    val hair = with(density) { Size.hairline.toPx() }
    val dot = with(density) { Spacing.space1.toPx() }
    Canvas(Modifier.fillMaxWidth().height(Spacing.space16).semantics { contentDescription = summary }) {
        val w = size.width
        val h = size.height
        val span = (to - from).toFloat()
        // 0% at the bottom, 125% at the top, so the 100% guide sits a fifth down.
        fun y(p: Int) = h - (p.coerceIn(0, 125) / 125f) * h
        fun x(t: Long) = ((t - from).coerceIn(0, to - from) / span) * w
        drawLine(c.muted, Offset(0f, y(100)), Offset(w, y(100)), strokeWidth = EXPECTED_DOT.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = expectedDash(dot * 2))
        if (points.size >= 2) {
            val path = androidx.compose.ui.graphics.Path()
            points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(p.at), y(p.percent)) else path.lineTo(x(p.at), y(p.percent)) }
            drawPath(path, c.ink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
        }
        points.lastOrNull()?.let { drawCircle(c.ink, dot, Offset(x(it.at), y(it.percent))) }
    }
}

/** 00 · 06 · 12 · 18 · 24 under a timeline. */
@Composable
fun HourLabels() {
    val c = CochiloTheme.colors
    // Axis ticks grow with the font size only to LABEL_SCALE_CAP, so "00" never breaks in two.
    CappedFontScale(LABEL_SCALE_CAP) { Row(Modifier.fillMaxWidth()) {
        listOf("00", "06", "12", "18", "24").forEachIndexed { i, s ->
            Text(
                s, style = Type.meta, color = c.muted, maxLines = 1, softWrap = false,
                textAlign = when (i) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center },
                modifier = Modifier.weight(if (i == 0 || i == 4) 0.5f else 1f),
            )
        }
    } }
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
    // The day names are the chart's axis: they grow to LABEL_SCALE_CAP, and their column with them.
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(1f, LABEL_SCALE_CAP)
    val labelWidth = (Spacing.space16 + Spacing.space4) * scale
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        days.forEachIndexed { i, (label, from, to) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CappedFontScale(LABEL_SCALE_CAP) {
                    Text(label, style = Type.meta, color = c.muted, modifier = Modifier.width(labelWidth), maxLines = 1, softWrap = false)
                }
                Column(Modifier.weight(1f)) {
                    Timeline(events, from, to, now, zone, summaries[i])
                }
            }
        }
        Row {
            Spacer(Modifier.width(labelWidth))
            Column(Modifier.weight(1f)) { HourLabels() }
        }
    }
}
