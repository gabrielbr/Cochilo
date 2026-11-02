package br.gabriel.cochilo.ui.kit

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.SystemEasing

// Motion (design system, components.md "Motion"): small, quick and always the same two durations.
// A press scales (pressScale), a state change of colour cross-fades, a list grows instead of
// jumping, a menu rises from its button. With Remove animations on, every one of them snaps.

/** The spec for one motion: `motion-standard` (150ms) or, for bigger moves, `motion-emphasised` (250ms). */
@Composable
fun <T> motionSpec(emphasised: Boolean = false): FiniteAnimationSpec<T> =
    if (CochiloTheme.reduceMotion) snap()
    else tween(if (emphasised) CTokens.Motion.motionEmphasised else CTokens.Motion.motionStandard, easing = SystemEasing)

/** A colour that cross-fades to [target] when the state changes (a lit option, a chip, the night panel). */
@Composable
fun animatedColor(target: Color, emphasised: Boolean = false, label: String = "colour"): Color =
    animateColorAsState(target, motionSpec(emphasised), label = label).value

/** A container that grows and shrinks with its content (a list getting a new entry) instead of jumping. */
@Composable
fun Modifier.growsWithContent(): Modifier =
    if (CochiloTheme.reduceMotion) this else this.animateContentSize(motionSpec(emphasised = true))

/** How something rises into view from below (the Log menu over its button). */
@Composable
fun riseIn(): EnterTransition =
    if (CochiloTheme.reduceMotion) EnterTransition.None
    else fadeIn(motionSpec(emphasised = true)) + expandVertically(motionSpec(emphasised = true), expandFrom = Alignment.Bottom)

/** How it sinks back. */
@Composable
fun sinkOut(): ExitTransition =
    if (CochiloTheme.reduceMotion) ExitTransition.None
    else fadeOut(motionSpec()) + shrinkVertically(motionSpec(), shrinkTowards = Alignment.Bottom)

/** A plain fade (a strip of choices over a row, the scrim). */
@Composable
fun fadeInMotion(): EnterTransition = if (CochiloTheme.reduceMotion) EnterTransition.None else fadeIn(motionSpec())

@Composable
fun fadeOutMotion(): ExitTransition = if (CochiloTheme.reduceMotion) ExitTransition.None else fadeOut(motionSpec())
