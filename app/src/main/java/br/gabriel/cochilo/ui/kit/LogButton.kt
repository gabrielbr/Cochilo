package br.gabriel.cochilo.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import br.gabriel.cochilo.R
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import kotlin.math.abs

/** One kind of entry in the Log button's menu, and the choices a slide can make on it. */
data class LogMenuItem(
    val key: String,
    val label: String,
    @DrawableRes val icon: Int,
    val options: List<SlideOption>,
    /** The kind of entry, for its colours (KindIcon). */
    val type: br.gabriel.cochilo.data.EventType,
)

/** What the finger is over: an item, or one of its options. */
data class LogHover(val item: Int, val option: Int? = null)

/**
 * The Log button (docs/flows.md): the app's main action, in the bottom-right corner where a right
 * thumb rests, on every tab. Press and hold: the kinds of entry rise in a column above it, the most
 * frequent nearest the thumb. Without lifting, drag up: the row under the finger lights, with a
 * tick; on a kind with choices, they appear to its left (Left · Right for a feed) and sliding onto
 * one lights it. Let go on a choice: logged at once. On the kind itself: its sheet. Anywhere else:
 * nothing. A plain tap opens the menu to use with taps (and TalkBack): tap a kind to show its
 * choices, tap it again for the sheet. After marking menus: the same move whether you look or not.
 */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun BoxScope.LogButton(
    items: List<LogMenuItem>,
    label: String,
    closeLabel: String,
    onItem: (LogMenuItem) -> Unit,
    onOption: (LogMenuItem, SlideOption) -> Unit,
    /** Screenshots only: draw the menu open with this hover. */
    preview: LogHover? = null,
) {
    val c = CochiloTheme.colors
    val haptic = LocalHapticFeedback.current
    var open by remember { mutableStateOf(preview != null) }
    var dragging by remember { mutableStateOf(false) }
    var hover by remember { mutableStateOf(preview) }
    var fab by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val rects = remember { mutableStateMapOf<String, Rect>() }
    val itemsNow by rememberUpdatedState(items)
    val onItemNow by rememberUpdatedState(onItem)
    val onOptionNow by rememberUpdatedState(onOption)

    fun close() { open = false; dragging = false; hover = null }
    fun commit(h: LogHover?) {
        val it = h?.let { itemsNow.getOrNull(it.item) }
        close()
        if (it == null) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val o = h.option?.let { i -> it.options.getOrNull(i) }
        if (o != null) onOptionNow(it, o) else onItemNow(it)
    }
    /** What's under a point in root coordinates: options first (they sit over the row), then items. */
    fun hit(p: Offset): LogHover? {
        // Choices count only for the row showing them (others may linger in the map after it hides).
        rects.forEach { (k, r) ->
            if (':' in k && r.contains(p)) {
                val (i, j) = k.split(':').map { it.toInt() }
                if (i == hover?.item) return LogHover(i, j)
            }
        }
        rects.forEach { (k, r) -> if (':' !in k && r.contains(p)) return LogHover(k.toInt()) }
        // Beside an item's row, to its left: still that row (the finger is on its way to a choice).
        rects.forEach { (k, r) -> if (':' !in k && p.y in r.top..r.bottom && p.x < r.left) return LogHover(k.toInt()) }
        return null
    }

    // The scrim: the screen steps back so the menu reads; a tap on it closes. It fades in and out.
    androidx.compose.animation.AnimatedVisibility(open, enter = fadeInMotion(), exit = fadeOutMotion()) {
        Box(
            Modifier.fillMaxSize().background(c.page.copy(alpha = SCRIM_ALPHA))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = closeLabel) { close() }
        )
    }
    // The menu pours out of the button item by item (cascadeIn, below) and sinks back into it at once.
    androidx.compose.animation.AnimatedVisibility(
        open, enter = androidx.compose.animation.EnterTransition.None, exit = sinkOut(),
        modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth()
            .padding(start = Spacing.space4, end = Spacing.space4, bottom = Spacing.space4 + Size.button + Spacing.space3),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.space2),
        ) {
            // Bottom-up: items[0] sits nearest the button, under the thumb.
            val arc = with(androidx.compose.ui.platform.LocalDensity.current) { Spacing.space3.roundToPx() }
            for (i in items.indices.reversed()) {
                val item = items[i]
                val showChoices = hover?.item == i
                // A soft arc instead of a rigid column (deepest in the middle), and a cascade: each
                // item rises in after the one nearer the button (design system: Cascade).
                val bow = arcOffset(i, items.size, arc)
                Row(
                    Modifier.offset { androidx.compose.ui.unit.IntOffset(-bow, 0) }.then(cascadeIn(i, still = preview != null)),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
                ) {
                    // The choices share what's left of the row at their own widths.
                    // A flow, so at large font sizes the choices wrap to a second line instead of losing the last.
                    if (showChoices) androidx.compose.foundation.layout.FlowRow(
                        Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.space1, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(Spacing.space1),
                    ) { item.options.forEachIndexed { j, o ->
                        val on = hover?.option == j
                        // The Last mark: the option used last time is outlined in the kind's colour.
                        val edge = when {
                            on -> BorderStroke(Size.hairline, c.fab)
                            o.last -> BorderStroke(Size.outline, c.kindColor(item.type))
                            else -> BorderStroke(Size.hairline, c.line)
                        }
                        val press = remember { MutableInteractionSource() }
                        Box(
                            Modifier.pressScale(press).defaultMinSize(minHeight = Size.button).clip(RoundedCornerShape(Radius.radiusRound))
                                .background(animatedColor(if (on) c.fab else c.surface))
                                .border(edge, RoundedCornerShape(Radius.radiusRound))
                                .onGloballyPositioned { rects["$i:$j"] = it.boundsInRoot() }
                                .clickable(press, indication = null, role = Role.Button, onClickLabel = o.a11y) { commit(LogHover(i, j)) }
                                .padding(horizontal = Spacing.space2, vertical = Spacing.space2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // …and a dot of that colour before its name; the word is only spoken.
                                if (o.last) { LastDot(if (on) c.onFab else c.kindColor(item.type)); Spacer(Modifier.width(Spacing.space1)) }
                                if (o.icon != null) { GIcon(o.icon, null, if (on) c.onFab else c.kindColor(item.type)); Spacer(Modifier.width(Spacing.space1)) }
                                Text(o.label, style = Type.label, color = animatedColor(if (on) c.onFab else c.ink), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    } }
                    val on = hover?.item == i && hover?.option == null
                    val press = remember { MutableInteractionSource() }
                    Row(
                        Modifier.pressScale(press).defaultMinSize(minHeight = Size.button).clip(RoundedCornerShape(Radius.radiusRound))
                            .background(animatedColor(if (on) c.fab else c.surface))
                            .border(BorderStroke(Size.hairline, animatedColor(if (on) c.fab else c.line)), RoundedCornerShape(Radius.radiusRound))
                            .onGloballyPositioned { rects["$i"] = it.boundsInRoot() }
                            .clickable(press, indication = null, role = Role.Button) {
                                // Tap mode: the first tap shows the choices, the second opens the sheet.
                                if (hover?.item == i || item.options.isEmpty()) commit(LogHover(i)) else hover = LogHover(i)
                            }
                            .padding(start = Spacing.space2, end = Spacing.space4, top = Spacing.space1, bottom = Spacing.space1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The kind's colours; on the lit row, the round fades into the Amora fill.
                        // Sleep leads: its round is a size up.
                        val badge = kindBadgeSize(item.type)
                        if (on) KindIcon(item.icon, c.onFab, c.onFab.copy(alpha = LIT_ROUND_ALPHA), size = badge)
                        else KindIcon(item.icon, c.kindColor(item.type), c.kindSoft(item.type), size = badge)
                        Spacer(Modifier.width(Spacing.space2))
                        Text(item.label, style = Type.label, color = animatedColor(if (on) c.onFab else c.ink), maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }

    // The button: `fab` (Amora under the brand), the one filled shape that isn't the screen's teal action.
    Box(
        Modifier.align(Alignment.BottomEnd).padding(Spacing.space4).size(Size.button)
            .scale(androidx.compose.animation.core.animateFloatAsState(if (dragging && !CochiloTheme.reduceMotion) PRESSED_SCALE else 1f, motionSpec(), label = "held").value)
            .clip(RoundedCornerShape(Radius.radiusRound)).background(c.fab)
            .onGloballyPositioned { fab = it }
            .semantics {
                role = Role.Button
                contentDescription = if (open) closeLabel else label
                onClick { if (open) close() else open = true; true }
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val wasOpen = open
                    open = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    var moved = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) {
                            when {
                                moved -> commit(hover)          // drag: let go on the choice
                                wasOpen -> close()              // tap on an open menu: close it
                                else -> { dragging = false }    // tap: the menu stays open for taps
                            }
                            break
                        }
                        val d = ch.position - down.position
                        if (!moved && (abs(d.x) > slop || abs(d.y) > slop)) { moved = true; dragging = true }
                        if (moved) {
                            val root = fab?.localToRoot(ch.position) ?: break
                            val h = hit(root)
                            if (h != hover && h != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            hover = h
                            ch.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // A plus that turns into a cross while the menu is open.
        val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 45f else 0f, motionSpec(emphasised = true), label = "turn")
        Box(Modifier.rotate(turn)) { GIcon(R.drawable.gg_ic_plus, null, c.onFab) }
    }
}

/** How much of the screen shows through behind the open menu. */
private const val SCRIM_ALPHA = 0.94f

/** The button dips under a held thumb, like every pressed target (pressScale). */
private const val PRESSED_SCALE = 0.97f

/** How much of on-fab tints the kind's round on the lit row (components.md, Kind icon: 18%). */
private const val LIT_ROUND_ALPHA = 0.18f

/** How far item [i] of [n] bows left of the column, in px: 0 at both ends, [depth] in the middle. */
internal fun arcOffset(i: Int, n: Int, depth: Int): Int =
    if (n < 3) 0 else (depth * kotlin.math.sin(Math.PI * i / (n - 1))).toInt()
