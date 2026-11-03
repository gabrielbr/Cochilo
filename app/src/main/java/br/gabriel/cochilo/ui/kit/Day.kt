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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
    // The hour ticks' band widens with their (capped) font size, so "06" never touches the ring.
    val labelBand = Spacing.space6 * density.fontScale.coerceIn(1f, LABEL_SCALE_CAP)
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
    // Clock ticks, like a chart's axis: they grow only to LABEL_SCALE_CAP so they stay outside the ring.
    CappedFontScale(LABEL_SCALE_CAP) {
        Text(text, style = Type.meta, color = CochiloTheme.colors.muted, maxLines = 1, softWrap = false, modifier = Modifier.align(at))
    }
}

/**
 * A row of the vertical timeline: the time, the rail with this entry's mark, then the entry.
 * A lasting entry (sleep, a feed) is a bar as tall as [barHeight]; an instant is a dot.
 */
@Composable
fun TimelineRow(
    time: String, color: Color, barHeight: Dp?, label: String, meta: String?, onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null, prominent: Boolean = false,
    /** A finished sleep: press, hold and drag to move its end (docs/flows.md). */
    adjust: TimelineAdjust? = null,
    /** For screenshots: show the row as if dragged this many steps. */
    previewSteps: Int? = null,
) {
    val c = CochiloTheme.colors
    val src = remember { MutableInteractionSource() }
    var dragPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var holding by remember { androidx.compose.runtime.mutableStateOf(previewSteps != null) }
    val stepPx = with(LocalDensity.current) { ADJUST_STEP.toPx() }
    val steps = previewSteps ?: br.gabriel.cochilo.domain.Adjust.steps(dragPx, stepPx)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val gesture = if (adjust == null) Modifier else Modifier
        .pointerInput(adjust) {
            detectDragGesturesAfterLongPress(
                onDragStart = { holding = true; dragPx = 0f; haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) },
                onDragEnd = {
                    val s = br.gabriel.cochilo.domain.Adjust.steps(dragPx, stepPx)
                    holding = false; dragPx = 0f
                    if (s != 0) adjust.onCommit(s)
                },
                onDragCancel = { holding = false; dragPx = 0f },
                onDrag = { change, amount ->
                    change.consume()
                    val before = br.gabriel.cochilo.domain.Adjust.steps(dragPx, stepPx)
                    dragPx += amount.y
                    // A light tick at each 5-minute step, so the hand feels the count.
                    if (br.gabriel.cochilo.domain.Adjust.steps(dragPx, stepPx) != before) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                },
            )
        }
        .semantics {
            customActions = listOf(
                androidx.compose.ui.semantics.CustomAccessibilityAction(adjust.laterLabel) { adjust.onCommit(1); true },
                androidx.compose.ui.semantics.CustomAccessibilityAction(adjust.earlierLabel) { adjust.onCommit(-1); true },
            )
        }
    val preview = if (holding && adjust != null) adjust.preview(steps) else null
    // While held, the bar follows the drag (4dp per 10 minutes, like the rest of the timeline).
    val shownBar = if (holding && barHeight != null && adjust != null) (barHeight + Spacing.space1 * (adjust.deltaMinutes(steps) / 10f)).coerceAtLeast(Spacing.space2) else barHeight
    val base = Modifier.fillMaxWidth()
    // At least as tall as the bar, and as tall as its words need (sleep's bigger round and two lines).
    val sized = base.heightIn(min = if (shownBar != null) maxOf(shownBar, Size.rowMin) else Size.rowMin).height(IntrinsicSize.Min)
    Row(
        sized.background(animatedColor(if (holding) c.selected else c.surface))
            .then(gesture)
            .clickable(src, indication = null, role = Role.Button, onClick = onClick).pressScale(src)
    ) {
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
            when {
                // While dragging: the new end, live, in ink.
                preview != null -> Text(preview, style = Type.meta, color = c.onSelected, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
                meta != null -> Text(meta, style = Type.meta, color = c.muted)
            }
            Spacer(Modifier.height(Spacing.space3))
        }
    }
}

/** 12dp of drag per 5-minute step: fine enough to aim, coarse enough not to jitter. */
private val ADJUST_STEP = Spacing.space3

/**
 * What a draggable sleep row needs: [preview] says the new end for a number of steps (null when it
 * can't move), [deltaMinutes] how far the end moved, [onCommit] saves it; the two labels are
 * TalkBack's actions ("Ended 5 minutes later / sooner").
 */
class TimelineAdjust(
    val preview: (Int) -> String?,
    val deltaMinutes: (Int) -> Int,
    val onCommit: (Int) -> Unit,
    val laterLabel: String,
    val earlierLabel: String,
)

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
