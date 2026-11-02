package br.gabriel.cochilo.ui.kit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import br.gabriel.cochilo.domain.BabyLook
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import kotlinx.coroutines.delay

/** How long a celebration stays: long enough to read one line, short enough not to wait for. */
private const val SHOW_MS = 2600L

/** At night: words only, and gone sooner. */
private const val QUIET_SHOW_MS = 1600L

/**
 * Celebration (design system, components.md; docs/celebrations.md): one small moment when what the
 * parent was trying for happens. The baby rises into a card under the header, lifts its arms and
 * smiles, beside one warm line; then it settles away by itself, or at a tap. It never blocks a tap
 * and never waits for one. [quiet] (night): the words only, dim, still. Remove animations: a still
 * picture and the words. TalkBack reads the words once.
 */
@Composable
fun CelebrationCard(words: String, look: BabyLook, quiet: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier, preview: Boolean = false) {
    val c = CochiloTheme.colors
    val reduce = CochiloTheme.reduceMotion
    val shown = remember { MutableTransitionState(preview || reduce || quiet) }
    val lift = remember { Animatable(if (preview) 1f else 0f) }
    val spec = motionSpec<Float>(emphasised = true)
    LaunchedEffect(words) {
        shown.targetState = true
        if (!quiet && !reduce && !preview) {
            // Arms up and a smile, then a little settle: one authored moment.
            lift.animateTo(1f, spec)
            delay(SHOW_MS / 2)
            lift.animateTo(0.6f, spec)
        }
        if (!preview) {
            delay(if (quiet) QUIET_SHOW_MS else SHOW_MS / 2)
            shown.targetState = false
            delay(motionEmphasisedMs())
            onDone()
        }
    }
    AnimatedVisibility(
        shown, modifier = modifier,
        enter = if (quiet) fadeInMotion() else riseIn(), exit = if (quiet) fadeOutMotion() else sinkOut(),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(Radius.radiusL))
                // A blush card with a hairline: it sits over the screen, so it needs its own edge.
                .background(if (quiet) c.page else c.selected)
                .border(Size.hairline, c.line, RoundedCornerShape(Radius.radiusL))
                .clickable(remember { MutableInteractionSource() }, indication = null) { shown.targetState = false; onDone() }
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                .padding(Spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!quiet) {
                BabyAvatar(look, size = Size.kindBadgeLead + Spacing.space6, mood = BabyMood.HAPPY, lift = lift.value)
                Spacer(Modifier.width(Spacing.space3))
            }
            Text(words, style = if (quiet) Type.bodySmall else Type.label, color = if (quiet) c.muted else c.onSelected, modifier = Modifier.weight(1f))
        }
    }
}

private fun motionEmphasisedMs(): Long = br.gabriel.cochilo.ui.theme.CTokens.Motion.motionEmphasised.toLong()
