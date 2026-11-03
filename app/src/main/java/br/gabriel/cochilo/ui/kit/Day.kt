package br.gabriel.cochilo.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import kotlinx.coroutines.launch
import java.time.ZoneId

// The Day view's pieces: the "i" button, the 24-hour ring and the vertical timeline's rows.

/**
 * An "i" next to an action: a 48dp target that opens a short explanation and stays until the
 * person taps elsewhere. [topic] is read as "About <topic>".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoButton(topic: String, text: String, tint: Color = CochiloTheme.colors.muted) {
    val c = CochiloTheme.colors
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = { Text(topic, style = Type.label) },
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = c.panel, contentColor = c.onPanel, titleContentColor = c.onPanel,
                ),
                shape = RoundedCornerShape(Radius.radiusM),
            ) { Text(text, style = Type.bodySmall, color = c.onPanel) }
        },
        state = state,
    ) {
        IconButton(R.drawable.gg_ic_info, stringResource(R.string.info_about, topic), { scope.launch { state.show() } }, tint = tint)
    }
}

/**
 * One day as a clock: midnight at the top, clockwise. Sleep fills the outer ring (night and nap
 * colours); feeds, pumping and daylight the inner ring; diapers are dots on the inner ring.
 * [middle] sits in the middle; [summary] is what TalkBack reads instead of the picture.
 */
@Composable
fun DayRing(
    events: List<Event>, from: Long, to: Long, now: Long?, zone: ZoneId, summary: String,
    /** Today's expected next-sleep window, drawn faint on the sleep ring (docs/flows.md). */
    expected: Pair<Long, Long>? = null,
    middle: @Composable () -> Unit,
) {
    val c = CochiloTheme.colors
    val density = LocalDensity.current
    val outerW = with(density) { Spacing.space4.toPx() }
    val innerW = with(density) { Spacing.space2.toPx() }
    val gap = with(density) { Spacing.space2.toPx() }
    val dot = with(density) { Spacing.space1.toPx() }
    val hair = with(density) { Size.hairline.toPx() }
    val diameter: Dp = Spacing.space16 * 4
    val labelBand = Spacing.space6
    Box(Modifier.size(diameter + labelBand * 2).semantics(mergeDescendants = true) { contentDescription = summary }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val span = (to - from).toFloat()
            fun angle(t: Long) = -90f + 360f * ((t - from).coerceIn(0, to - from) / span)
            val rOuter = size.minDimension / 2 - outerW / 2
            val rInner = rOuter - outerW / 2 - gap - innerW / 2
            fun arc(color: Color, start: Long, end: Long, r: Float, w: Float) {
                val a0 = angle(start)
                val sweep = maxOf(angle(end) - a0, 1.5f)
                drawArc(color, a0, sweep, useCenter = false, topLeft = Offset(center.x - r, center.y - r), size = GSize(r * 2, r * 2), style = Stroke(w, cap = StrokeCap.Butt))
            }
            drawCircle(c.line, rOuter, style = Stroke(outerW))
            drawCircle(c.line, rInner, style = Stroke(innerW))
            // Where the next sleep is expected: a faint band of the nap colour, under the real sleeps.
            if (expected != null) arc(c.eventNap.copy(alpha = EXPECTED_ALPHA), expected.first, expected.second, rOuter, outerW)
            for (e in events) {
                if (!e.type.hasDuration) continue
                val end = e.end ?: now ?: continue
                if (end < from || e.start >= to) continue
                val col = c.eventColor(e, zone)
                if (e.type == EventType.SLEEP) arc(col, e.start, end, rOuter, outerW) else arc(col, e.start, end, rInner, innerW)
            }
            for (e in events) {
                if (e.type != EventType.DIAPER || e.start !in from until to) continue
                val a = Math.toRadians(angle(e.start).toDouble())
                drawCircle(c.eventDiaper, dot * 1.5f, Offset(center.x + rInner * kotlin.math.cos(a).toFloat(), center.y + rInner * kotlin.math.sin(a).toFloat()))
            }
            // Six-hour ticks across both rings, and "now" as an ink hand on the outer ring.
            for (h in 0 until 4) tick(angle(from + (to - from) * h / 4), rInner - innerW, rOuter + outerW / 2, c.page, hair * 2)
            if (now != null && now in from until to) tick(angle(now), rOuter - outerW / 2, rOuter + outerW / 2, c.ink, hair * 3)
        }
        HourLabel("00", Alignment.TopCenter)
        HourLabel("06", Alignment.CenterEnd)
        HourLabel("12", Alignment.BottomCenter)
        HourLabel("18", Alignment.CenterStart)
        Box(Modifier.size(diameter - Spacing.space16), contentAlignment = Alignment.Center) { middle() }
    }
}

private fun DrawScope.tick(deg: Float, r0: Float, r1: Float, color: Color, width: Float) {
    val a = Math.toRadians(deg.toDouble())
    val cos = kotlin.math.cos(a).toFloat()
    val sin = kotlin.math.sin(a).toFloat()
    drawLine(color, Offset(center.x + r0 * cos, center.y + r0 * sin), Offset(center.x + r1 * cos, center.y + r1 * sin), width)
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HourLabel(text: String, at: Alignment) {
    Text(text, style = Type.meta, color = CochiloTheme.colors.muted, modifier = Modifier.align(at))
}

/**
 * A row of the vertical timeline: the time, the rail with this entry's mark, then the entry.
 * A lasting entry (sleep, a feed) is a bar as tall as [barHeight]; an instant is a dot.
 */
@Composable
fun TimelineRow(time: String, color: Color, barHeight: Dp?, label: String, meta: String?, onClick: () -> Unit, icon: (@Composable () -> Unit)? = null, prominent: Boolean = false) {
    val c = CochiloTheme.colors
    val src = remember { MutableInteractionSource() }
    val base = Modifier.fillMaxWidth()
    // At least as tall as the bar, and as tall as its words need (sleep's bigger round and two lines).
    val sized = base.heightIn(min = if (barHeight != null) maxOf(barHeight, Size.rowMin) else Size.rowMin).height(IntrinsicSize.Min)
    Row(sized.clickable(src, indication = null, role = Role.Button, onClick = onClick).pressScale(src)) {
        Text(time, style = Type.meta, color = c.muted, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(Spacing.space16 + Spacing.space2).padding(top = Spacing.space1))
        Box(Modifier.width(Spacing.space8).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(Size.hairline).fillMaxHeight().background(c.line))
            if (barHeight != null) {
                // Sleep leads: its bar is wider than a feed's or a walk's.
                Box(
                    Modifier.padding(vertical = Spacing.space1).width(if (prominent) Spacing.space3 else Spacing.space2).fillMaxHeight()
                        .clip(RoundedCornerShape(Radius.radiusS)).background(color)
                )
            } else {
                Box(Modifier.padding(top = Spacing.space2).size(Spacing.space3).clip(RoundedCornerShape(Radius.radiusRound)).background(color))
            }
        }
        Column(Modifier.weight(1f).padding(start = Spacing.space2, end = Spacing.space2)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(Spacing.space2))
                }
                Text(label, style = if (prominent) Type.label else Type.body, color = c.ink)
            }
            if (meta != null) Text(meta, style = Type.meta, color = c.muted)
            Spacer(Modifier.height(Spacing.space3))
        }
    }
}

/** Between two sleeps: the rail continues and says how long the baby was awake. */
@Composable
fun TimelineGap(text: String) {
    val c = CochiloTheme.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Spacer(Modifier.width(Spacing.space16 + Spacing.space2))
        Box(Modifier.width(Spacing.space8).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(Size.hairline).fillMaxHeight().background(c.line))
        }
        Text(text, style = Type.meta, color = c.muted, modifier = Modifier.padding(start = Spacing.space2, top = Spacing.space1, bottom = Spacing.space3))
    }
}

/** How strongly the expected window shows on the ring: a hint, never data. */
private const val EXPECTED_ALPHA = 0.35f
