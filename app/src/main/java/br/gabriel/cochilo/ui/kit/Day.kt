package br.gabriel.cochilo.ui.kit

import androidx.compose.foundation.Canvas
import br.gabriel.cochilo.domain.RingLayout
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
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
 * One day as a clock: midnight at the top, clockwise (0.58.0, the owner: "more user-friendly, the
 * elements float and it's less crowded").
 *
 * - **The sleep ring:** one wide track; each sleep is a rounded band in the night or nap colour,
 *   with its moon floating on it, and its start and end times along the outside of the circle.
 * - **Everything else** (feeds, diapers, pumping, daylight, medicine…) floats inside the ring as
 *   its own icon in its round, at the time it happened. Neighbours drift apart so none overlap;
 *   three or more at one spot become one round with "+2" (RingLayout, tested).
 * - The expected window and the rest of today's plan stay faint or dashed: never data.
 *
 * [middle] sits in the middle; [summary] is what TalkBack reads instead of the picture.
 */
@Composable
fun DayRing(
    events: List<Event>, from: Long, to: Long, now: Long?, zone: ZoneId, summary: String,
    /** Today's expected next-sleep window, drawn faint on the sleep ring (docs/flows.md). */
    expected: Pair<Long, Long>? = null,
    /**
     * The rest of today, projected (DayPlan, 0.49.0): each expected sleep as a dashed outline on
     * the sleep ring, never a fill, so it never reads as data. Naps in the nap colour, bedtime in
     * the night's until midnight.
     */
    planned: List<br.gabriel.cochilo.domain.DayPlan.Sleep> = emptyList(),
    /** A clock time as the app writes it (Format.time). */
    time: (Long) -> String = { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalTime().toString().take(5) },
    middle: @Composable () -> Unit,
) {
    val c = CochiloTheme.colors
    val density = LocalDensity.current
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Times run along the outside: one line of meta text, its height capped like an axis.
        val labelBand = Spacing.space6 * density.fontScale.coerceIn(1f, LABEL_SCALE_CAP)
        val diameter = minOf(maxWidth - labelBand * 2, Spacing.space16 * 5)
        val track = Spacing.space6
        val badge = Size.kindBadge
        val gap = Spacing.space2
        val span = (to - from).toDouble()
        fun deg(t: Long) = 360.0 * ((t - from).coerceIn(0, to - from) / span)
        val rTrack = diameter / 2 - track / 2
        val rBubbles = rTrack - track / 2 - gap - badge / 2
        val rLabels = diameter / 2 + labelBand / 2
        val rTrackPx = with(density) { rTrack.toPx() }
        val rBubblesPx = with(density) { rBubbles.toPx() }
        val rLabelsPx = with(density) { rLabels.toPx() }
        val sepTrack = Math.toDegrees(with(density) { (badge + Spacing.space1).toPx() } / rTrackPx.toDouble())
        val sepInside = Math.toDegrees(with(density) { (badge + Spacing.space1).toPx() } / rBubblesPx.toDouble())

        val inDay = events.filter { (it.end ?: now ?: it.start) >= from && it.start < to }
        val sleeps = inDay.filter { it.type == EventType.SLEEP }
        val others = inDay.filter { it.type != EventType.SLEEP }.sortedBy { it.start }
        val byId = inDay.associateBy { it.id }
        // A sleep's moon floats on its band, at its middle.
        val sleepMarks = sleeps.map { e ->
            val end = minOf(e.end ?: now ?: e.start, to)
            RingLayout.Mark(e.id, (deg(maxOf(e.start, from)) + deg(end)) / 2)
        }
        val sleepBubbles = remember(sleepMarks, sepTrack) { RingLayout.bubbles(sleepMarks, sepTrack) }
        val otherBubbles = remember(others, from, to, sepInside) { RingLayout.bubbles(others.map { RingLayout.Mark(it.id, deg(it.start)) }, sepInside) }

        // The times along the outside: each sleep's start and end first, then the four hours.
        val measurer = androidx.compose.ui.text.rememberTextMeasurer()
        val labelStyle = Type.meta
        val capped = androidx.compose.ui.unit.Density(density.density, density.fontScale.coerceAtMost(LABEL_SCALE_CAP))
        fun widthDeg(text: String) = Math.toDegrees(measurer.measure(text, labelStyle, density = capped).size.width.toDouble() / rLabelsPx)
        val texts = buildList {
            sleeps.forEach { e ->
                if (e.start >= from) add(Triple("s" + e.id, e.start, 0))
                e.end?.takeIf { it <= to }?.let { add(Triple("e" + e.id, it, 1)) }
            }
            for (h in 0 until 4) add(Triple("h$h", from + (to - from) * h / 4, 2))
        }
        val labelText = texts.associate { (id, t, _) -> id to if (id.startsWith("h")) "%02d".format(h(id)) else time(t) }
        val labels = RingLayout.labels(texts.map { (id, t, p) -> RingLayout.Label(id, deg(t), widthDeg(labelText.getValue(id)), p) }, gap = 2.0)

        Box(Modifier.size(diameter + labelBand * 2).semantics(mergeDescendants = true) { contentDescription = summary }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(diameter)) {
                val w = track.toPx()
                val r = rTrackPx
                fun angle(t: Long) = (-90.0 + deg(t)).toFloat()
                fun band(color: Color, start: Long, end: Long, width: Float = w, cap: StrokeCap = StrokeCap.Round) {
                    val a0 = angle(start)
                    val sweep = maxOf(angle(end) - a0, 1.5f)
                    drawArc(color, a0, sweep, useCenter = false, topLeft = Offset(center.x - r, center.y - r), size = GSize(r * 2, r * 2), style = Stroke(width, cap = cap))
                }
                drawCircle(c.line, r, style = Stroke(w))
                // Where the next sleep is expected: a faint band of the nap colour, under the real sleeps.
                if (expected != null) band(c.eventNap.copy(alpha = EXPECTED_ALPHA), expected.first, expected.second)
                // The projection, dashed along the track's two edges and closed at both ends.
                val hair = Size.hairline.toPx()
                val dash = expectedDash(Spacing.space1.toPx())
                for (s in planned) {
                    val col = if (s.next == br.gabriel.cochilo.domain.Predictor.Next.BEDTIME) c.eventNight else c.eventNap
                    val a0 = angle(s.at)
                    val sweep = maxOf(angle(s.end) - a0, 1.5f)
                    for (rr in listOf(r - w / 2, r + w / 2)) {
                        drawArc(col, a0, sweep, useCenter = false, topLeft = Offset(center.x - rr, center.y - rr), size = GSize(rr * 2, rr * 2), style = Stroke(hair * 2, pathEffect = dash))
                    }
                    tick(a0, r - w / 2, r + w / 2, col, hair * 2)
                    tick(a0 + sweep, r - w / 2, r + w / 2, col, hair * 2)
                }
                for (e in sleeps) band(c.eventColor(e, zone), maxOf(e.start, from), minOf(e.end ?: now ?: e.start, to))
                // Now: an ink hand across the track.
                if (now != null && now in from until to) tick(angle(now), r - w / 2, r + w / 2, c.ink, hair * 3)
            }
            for (b in sleepBubbles) RingBubble(b, byId, rTrackPx, zone)
            for (b in otherBubbles) RingBubble(b, byId, rBubblesPx, zone)
            CappedFontScale(LABEL_SCALE_CAP) {
                for (l in labels) {
                    val hour = l.id.startsWith("h")
                    RingPlaced(l.angle, rLabelsPx) {
                        Text(
                            labelText.getValue(l.id), style = labelStyle, maxLines = 1, softWrap = false,
                            color = if (hour) c.muted else c.ink,
                            // Times run along the circle; the four hours stay upright, so "06" never reads "90".
                            modifier = Modifier.graphicsLayer { rotationZ = if (hour) 0f else RingLayout.labelRotation(l.angle).toFloat() },
                        )
                    }
                }
            }
            // The middle grows with the font only as far as the ring's own labels, so it never
            // runs into the floating marks.
            Box(Modifier.size((rBubbles - badge / 2 - gap) * 2), contentAlignment = Alignment.Center) { CappedFontScale(LABEL_SCALE_CAP) { middle() } }
        }
    }
}

private fun h(id: String) = (id.removePrefix("h").toInt()) * 6

/** Places [content] centred on the point at [angle] degrees and [radiusPx] from the middle. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.RingPlaced(angle: Double, radiusPx: Float, content: @Composable () -> Unit) {
    val a = Math.toRadians(angle - 90.0)
    val x = (radiusPx * kotlin.math.cos(a)).toFloat()
    val y = (radiusPx * kotlin.math.sin(a)).toFloat()
    Box(Modifier.align(Alignment.Center).offset { androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt()) }) { content() }
}

/**
 * A floating mark on the ring: the entry's icon in its round, ringed in the card's colour so it
 * lifts off what's under it. A stack shows the newest entry and how many more ("+2").
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.RingBubble(b: RingLayout.Bubble, byId: Map<String, Event>, radiusPx: Float, zone: ZoneId) {
    val c = CochiloTheme.colors
    val newest = b.ids.mapNotNull { byId[it] }.maxByOrNull { it.start } ?: return
    RingPlaced(b.angle, radiusPx) {
        Box {
            KindIcon(
                newest, zone,
                Modifier.size(Size.kindBadge).clip(RoundedCornerShape(Radius.radiusRound))
                    .border(Size.outline, c.surface, RoundedCornerShape(Radius.radiusRound)),
            )
            if (b.ids.size > 1) {
                Text(
                    "+${b.ids.size - 1}", style = Type.meta, color = c.ink, maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(Spacing.space2, Spacing.space1)
                        .clip(RoundedCornerShape(Radius.radiusRound)).background(c.surface)
                        .border(Size.hairline, c.line, RoundedCornerShape(Radius.radiusRound))
                        .padding(horizontal = Spacing.space1),
                )
            }
        }
    }
}

private fun DrawScope.tick(deg: Float, r0: Float, r1: Float, color: Color, width: Float) {
    val a = Math.toRadians(deg.toDouble())
    val cos = kotlin.math.cos(a).toFloat()
    val sin = kotlin.math.sin(a).toFloat()
    drawLine(color, Offset(center.x + r0 * cos, center.y + r0 * sin), Offset(center.x + r1 * cos, center.y + r1 * sin), width)
}

/**
 * The timeline's two tracks (0.48.0, docs/flows.md), like the day ring's two rings.
 *
 * - **Main track:** one block per stretch of the day, a sleep or an awake stretch. Its bar sits
 *   in the main lane, as tall as its length suggests (TimelineScale): filled in the sleep's colour,
 *   or outlined for awake. Its title sits beside the bar (badge, label role, meta).
 * - **Secondary track:** what happened during the stretch, set in beside the same bar
 *   ([TimelineEntry]).
 *
 * The bar runs alongside everything that happened during it. A finished sleep's title takes a
 * press, hold and drag to move its end ([adjust]).
 */
@Composable
fun TimelineStretch(
    time: String,
    /** The bar's colour; null for awake, which is an outline in `muted`. */
    color: Color?,
    barHeight: Dp?,
    label: String,
    meta: String?,
    /** The sleep's round; none for awake, whose outlined bar already says what it is. */
    icon: (@Composable () -> Unit)?,
    onClick: (() -> Unit)? = null,
    /** A finished sleep: press, hold and drag to move its end (docs/flows.md). */
    adjust: TimelineAdjust? = null,
    /** The entry's length, so the bar follows a drag on the timeline's scale. */
    durationMinutes: Int? = null,
    /** For screenshots: show the stretch as if dragged this many steps. */
    previewSteps: Int? = null,
    /** A sleep: press, hold and drag its start time to move its start (0.53.0). */
    adjustStart: TimelineAdjust? = null,
    /** For screenshots: the start as if dragged this many steps. */
    previewStartSteps: Int? = null,
    during: @Composable ColumnScope.() -> Unit = {},
) {
    val c = CochiloTheme.colors
    val editLabel = androidx.compose.ui.res.stringResource(br.gabriel.cochilo.R.string.edit_entry)
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
    // The start (0.53.0): the same press, hold and drag, on the time on the left.
    var startPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var holdingStart by remember { androidx.compose.runtime.mutableStateOf(previewStartSteps != null) }
    val startSteps = previewStartSteps ?: br.gabriel.cochilo.domain.Adjust.steps(startPx, stepPx)
    val startGesture = if (adjustStart == null) Modifier else Modifier
        .pointerInput(adjustStart) {
            detectDragGesturesAfterLongPress(
                onDragStart = { holdingStart = true; startPx = 0f; haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) },
                onDragEnd = {
                    val s = br.gabriel.cochilo.domain.Adjust.steps(startPx, stepPx)
                    holdingStart = false; startPx = 0f
                    if (s != 0) adjustStart.onCommit(s)
                },
                onDragCancel = { holdingStart = false; startPx = 0f },
                onDrag = { change, amount ->
                    change.consume()
                    val before = br.gabriel.cochilo.domain.Adjust.steps(startPx, stepPx)
                    startPx += amount.y
                    if (br.gabriel.cochilo.domain.Adjust.steps(startPx, stepPx) != before) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                },
            )
        }
        .semantics {
            customActions = listOf(
                androidx.compose.ui.semantics.CustomAccessibilityAction(adjustStart.laterLabel) { adjustStart.onCommit(1); true },
                androidx.compose.ui.semantics.CustomAccessibilityAction(adjustStart.earlierLabel) { adjustStart.onCommit(-1); true },
            )
        }
    val preview = when {
        holding && adjust != null -> adjust.preview(steps)
        holdingStart && adjustStart != null -> adjustStart.preview(startSteps)
        else -> null
    }
    // While held, the bar follows the drag, on the same scale as the rest of the timeline. A later
    // start shortens it, as a sooner end does.
    val delta = when {
        holding && adjust != null -> adjust.deltaMinutes(steps)
        holdingStart && adjustStart != null -> -adjustStart.deltaMinutes(startSteps)
        else -> null
    }
    val shownBar = if (delta != null && barHeight != null && durationMinutes != null)
        Spacing.space1 * br.gabriel.cochilo.domain.TimelineScale.units(durationMinutes + delta) else barHeight
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = if (shownBar != null) maxOf(shownBar + Spacing.space2, Size.rowMin) else Size.rowMin)
            .height(IntrinsicSize.Min)
    ) {
        // The start time; on a sleep it takes the drag that moves the start (0.53.0).
        Text(
            if (holdingStart) adjustStart?.previewTime?.invoke(startSteps) ?: time else time, style = Type.meta, color = if (holdingStart) c.onSelected else c.muted, textAlign = TextAlign.End, maxLines = 1,
            // As tall as the row (56dp): the drag handle is a full touch target (0.55.1 audit).
            modifier = Modifier.width(Spacing.space16 + Spacing.space2).heightIn(min = Size.rowMin)
                .background(animatedColor(if (holdingStart) c.selected else Color.Transparent), RoundedCornerShape(Radius.radiusS))
                .then(startGesture)
                .padding(top = Spacing.space2, end = Spacing.space1),
        )
        // The main lane: the rail, and this stretch's bar from its top.
        Box(Modifier.width(Spacing.space8).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(Size.hairline).fillMaxHeight().background(c.line))
            val bar = Modifier.padding(vertical = Spacing.space1).width(Spacing.space3)
            when {
                shownBar != null && color != null ->
                    Box(bar.height(shownBar).clip(RoundedCornerShape(Radius.radiusS)).background(color))
                shownBar != null ->
                    Box(bar.height(shownBar).clip(RoundedCornerShape(Radius.radiusS)).background(c.surface).border(Size.hairline, c.muted, RoundedCornerShape(Radius.radiusS)))
                else ->
                    Box(Modifier.padding(top = Spacing.space2).size(Spacing.space3).clip(RoundedCornerShape(Radius.radiusRound)).background(c.surface).border(Size.hairline, c.muted, RoundedCornerShape(Radius.radiusRound)))
            }
        }
        Column(Modifier.weight(1f).padding(start = Spacing.space2, end = Spacing.space2)) {
            // The title: the whole block's one big line. It takes the tap (edit) and the drag.
            val title = Modifier.fillMaxWidth()
                .background(animatedColor(if (holding || holdingStart) c.selected else c.surface), RoundedCornerShape(Radius.radiusS))
                .then(gesture)
                .then(if (onClick != null) Modifier.clickable(src, indication = null, role = Role.Button, onClickLabel = editLabel, onClick = onClick).pressScale(src) else Modifier.semantics(mergeDescendants = true) { heading() })
            Row(title.padding(bottom = Spacing.space2), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            icon()
                            Spacer(Modifier.width(Spacing.space2))
                        }
                        Text(label, style = if (color != null) Type.label else Type.body, color = c.ink)
                    }
                    when {
                        // While dragging: the new end, live, in ink.
                        preview != null -> Text(preview, style = Type.meta, color = c.onSelected, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
                        meta != null -> Text(meta, style = Type.meta, color = c.muted)
                    }
                }
                // Tap to edit (0.45.0): the pencil says so; the title is the target.
                if (onClick != null) GIcon(br.gabriel.cochilo.R.drawable.gg_ic_note, null, c.muted, Modifier.padding(top = Spacing.space2))
            }
            during()
            Spacer(Modifier.height(Spacing.space2))
        }
    }
}

/**
 * The dash of anything expected, not logged (0.49.0 ring, 0.50.0 timeline): [on] drawn, [on] off.
 * Callers pass `space1` in pixels, so the ring and the timeline dash the same way.
 */
fun expectedDash(on: Float) = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(on, on))

/**
 * The next expected sleep on the timeline's main track (0.50.0, docs/prediction.md): today only,
 * above what's happening now. It's drawn like the ring's projection: a dashed outline in the nap
 * colour (the night's for bedtime), never filled, so it never reads as data. It has no badge and
 * no pencil; its words are muted and regular weight. Not a target: TalkBack reads it as one
 * heading.
 */
@Composable
fun TimelineExpected(time: String, color: Color, barHeight: Dp, label: String, meta: String) {
    val c = CochiloTheme.colors
    val density = LocalDensity.current
    val on = with(density) { Spacing.space1.toPx() }
    val stroke = with(density) { (Size.hairline * 2).toPx() }
    val radius = with(density) { Radius.radiusS.toPx() }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = maxOf(barHeight + Spacing.space2, Size.rowMin))
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) { heading() }
    ) {
        Text(time, style = Type.meta, color = c.muted, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(Spacing.space16 + Spacing.space2).padding(top = Spacing.space2))
        Box(Modifier.width(Spacing.space8).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(Size.hairline).fillMaxHeight().background(c.line))
            Box(
                Modifier.padding(vertical = Spacing.space1).width(Spacing.space3).height(barHeight)
                    .background(c.surface)
                    .drawBehind {
                        drawRoundRect(
                            color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                            style = Stroke(stroke, pathEffect = expectedDash(on)),
                        )
                    }
            )
        }
        Column(Modifier.weight(1f).padding(start = Spacing.space2, end = Spacing.space2, bottom = Spacing.space2)) {
            Text(label, style = Type.body, color = c.muted)
            Text(meta, style = Type.meta, color = c.muted)
        }
    }
}

/**
 * The secondary track (0.48.0): a feed, a walk, a diaper, set in beside its stretch's bar. Less
 * weight on purpose:
 * - its mark is an outline in its own colour (a bar as long as it lasted, or a ring for an
 *   instant), never filled;
 * - its icon is the plain line icon in `muted`, not a round badge;
 * - its words are a size down (bodySmall, then meta).
 * The whole row is the edit target; a pencil says so.
 */
@Composable
fun TimelineEntry(
    color: Color, barHeight: Dp?, label: String, meta: String?,
    @androidx.annotation.DrawableRes icon: Int, onClick: () -> Unit,
) {
    val c = CochiloTheme.colors
    val editLabel = androidx.compose.ui.res.stringResource(br.gabriel.cochilo.R.string.edit_entry)
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = if (barHeight != null) maxOf(barHeight + Spacing.space2, Size.rowMin) else Size.rowMin)
            .clickable(src, indication = null, role = Role.Button, onClickLabel = editLabel, onClick = onClick).pressScale(src),
        verticalAlignment = Alignment.Top,
    ) {
        // The secondary lane: an outline in the entry's colour.
        Box(Modifier.width(Spacing.space6).padding(top = Spacing.space1), contentAlignment = Alignment.TopCenter) {
            if (barHeight != null) {
                Box(Modifier.width(Spacing.space2).height(barHeight).clip(RoundedCornerShape(Radius.radiusS)).border(Size.outline, color, RoundedCornerShape(Radius.radiusS)))
            } else {
                Box(Modifier.padding(top = Spacing.space1).size(Spacing.space3).clip(RoundedCornerShape(Radius.radiusRound)).border(Size.outline, color, RoundedCornerShape(Radius.radiusRound)))
            }
        }
        GIcon(icon, null, c.muted, Modifier.padding(end = Spacing.space2))
        Column(Modifier.weight(1f)) {
            Text(label, style = Type.bodySmall, color = c.ink)
            if (meta != null) Text(meta, style = Type.meta, color = c.muted)
        }
        GIcon(br.gabriel.cochilo.R.drawable.gg_ic_note, null, c.muted)
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
    /** For the start (0.53.0): the time on the left while held, live. */
    val previewTime: ((Int) -> String)? = null,
)

/** How strongly the expected window shows on the ring: a hint, never data. */
private const val EXPECTED_ALPHA = 0.35f
