package br.gabriel.cochilo.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import br.gabriel.cochilo.domain.slideOption
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import kotlin.math.abs

/** One thing a slide can log, e.g. "Left" for a left-breast feed. [a11y] is what TalkBack offers. */
data class SlideOption(
    val key: String,
    val label: String,
    val a11y: String,
    val last: Boolean = false,
    /** An icon beside the label where a word alone would be long (the bottle in "🍼 120 ml"). */
    @DrawableRes val icon: Int? = null,
)

/**
 * A log row with a shortcut, after marking menus: tap opens the full sheet; slide sideways and the
 * row turns into its options side by side (left breast on the left, right on the right), the one
 * under the finger lit, a tick as it changes; let go to log it at once. Sliding back to where the
 * finger started cancels. Experienced hands just flick. [meta] should say the slide exists, and
 * TalkBack gets each option as an action, so the shortcut is never hidden.
 */
@Composable
fun SlideRow(
    label: String,
    meta: String?,
    @DrawableRes icon: Int?,
    options: List<SlideOption>,
    onTap: () -> Unit,
    onPick: (SlideOption) -> Unit,
    previewActive: Int? = null,
    /** The word that marks the option used last time ("LAST"). */
    lastTag: String = "",
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = CochiloTheme.colors
    val haptic = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { Spacing.space12.toPx() }
    var width by remember { mutableIntStateOf(0) }
    var sliding by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf<Int?>(null) }
    var startX by remember { mutableFloatStateOf(0f) }
    val tap by rememberUpdatedState(onTap)
    val pick by rememberUpdatedState(onPick)
    val opts by rememberUpdatedState(options)

    Box(
        Modifier.fillMaxWidth()
            .onSizeChanged { width = it.width }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label) { tap(); true }
                customActions = options.map { o -> CustomAccessibilityAction(o.a11y) { pick(o); true } }
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    startX = down.position.x
                    var cancelled = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) {
                            if (sliding) {
                                active?.let { i ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pick(opts[i])
                                }
                                ch.consume()
                            } else if (!cancelled && !ch.isConsumed) {
                                tap() // a tap on a child (the "i") is consumed there
                            }
                            break
                        }
                        val dx = ch.position.x - down.position.x
                        val dy = ch.position.y - down.position.y
                        if (!sliding && !cancelled) {
                            if (opts.isNotEmpty() && abs(dx) > slop && abs(dx) > abs(dy)) sliding = true
                            else if (abs(dy) > slop || abs(dx) > slop) cancelled = true
                        }
                        if (sliding) {
                            val a = slideOption(ch.position.x, startX, width.toFloat(), threshold, opts.size)
                            if (a != active && a != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            active = a
                            ch.consume()
                        } else {
                            // A parent that scrolled took this gesture: it isn't a tap any more.
                            val fin = awaitPointerEvent(PointerEventPass.Final)
                            if (fin.changes.any { it.id == down.id && it.isConsumed }) cancelled = true
                        }
                    }
                    sliding = false
                    active = null
                }
            }
    ) {
        ListRow(label = label, meta = meta, leading = icon?.let { { GIcon(it, null) } }, trailing = trailing)
        val shown = if (sliding) active else previewActive
        if (sliding || previewActive != null) {
            Row(Modifier.matchParentSize().padding(Spacing.space1).clip(RoundedCornerShape(Radius.radiusM)).background(c.surface)) {
                options.forEachIndexed { i, o ->
                    val on = i == shown
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(Spacing.space1)
                            .clip(RoundedCornerShape(Radius.radiusM))
                            .background(if (on) c.ink else c.line),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                o.label, style = Type.label, color = if (on) c.page else c.ink, textAlign = TextAlign.Center,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = Spacing.space2),
                            )
                            // Remember actions: the option used last time is marked.
                            if (o.last) Text(lastTag, style = Type.caption, color = if (on) c.page else c.muted)
                        }
                    }
                }
            }
        }
    }
}
