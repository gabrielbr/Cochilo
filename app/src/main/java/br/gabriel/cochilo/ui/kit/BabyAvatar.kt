package br.gabriel.cochilo.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import br.gabriel.cochilo.domain.BabyGender
import br.gabriel.cochilo.domain.BabyLook
import br.gabriel.cochilo.domain.Hair
import br.gabriel.cochilo.domain.HairColor
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CochiloTheme

/** The baby's look, provided once at the root (CochiloRoot), so any screen can draw the avatar. */
val LocalBabyLook = androidx.compose.runtime.staticCompositionLocalOf { BabyLook() }

/** The avatar's expression: awake, asleep (eyes closed), or happy (smiling eyes, for celebrations). */
enum class BabyMood { AWAKE, ASLEEP, HAPPY }

/**
 * The baby avatar (design system, components.md): the parent's own baby, as chosen at onboarding
 * (skin tone, hair, a bow for "girl"), wrapped in a Rosa Chá sling. The brand's illustration style:
 * single-weight line, rounded caps, flat fills from the illustration palette, which is the same in
 * both themes. Drawn on a 100 × 100 grid and scaled, so every combination is one drawing and a
 * celebration can move its parts ([blink], [lift]).
 */
@Composable
fun BabyAvatar(
    look: BabyLook,
    modifier: Modifier = Modifier,
    size: Dp = Size.avatar,
    mood: BabyMood = BabyMood.AWAKE,
    /** 0 = eyes open, 1 = closed (a blink). */
    blink: Float = 0f,
    /** 0..1: the arms raised for a happy moment. */
    lift: Float = 0f,
    description: String? = null,
) {
    val p = CochiloTheme.colors
    val m = if (description != null) modifier.semantics { contentDescription = description } else modifier
    Canvas(m.size(size)) {
        val k = this.size.minDimension / GRID
        scale(k, k, pivot = Offset.Zero) { drawBaby(look, p, mood, blink, lift) }
    }
}

/** The skin colour for a tone 1..6. */
fun CTokens.Palette.skin(tone: Int): Color = when (tone.coerceIn(1, BabyLook.SKIN_TONES)) {
    1 -> illustrationSkin1
    2 -> illustrationSkin2
    3 -> illustrationSkin3
    4 -> illustrationSkin4
    5 -> illustrationSkin5
    else -> illustrationSkin6
}

fun CTokens.Palette.hair(c: HairColor): Color = when (c) {
    HairColor.BLACK -> illustrationHairBlack
    HairColor.BROWN -> illustrationHairBrown
    HairColor.BLOND -> illustrationHairBlond
    HairColor.RED -> illustrationHairRed
}

private const val GRID = 100f
private const val LINE = 2.4f

private fun DrawScope.drawBaby(look: BabyLook, p: CTokens.Palette, mood: BabyMood, blink: Float, lift: Float) {
    val skin = p.skin(look.skin)
    val line = p.illustrationFaceLine
    val stroke = Stroke(width = LINE, cap = StrokeCap.Round)

    // Arms, raised a little when happy: two small skin ovals peeking from the wrap.
    if (lift > 0f) {
        val up = 10f * lift
        drawOval(skin, Offset(14f, 60f - up), GSize(14f, 12f))
        drawOval(skin, Offset(72f, 60f - up), GSize(14f, 12f))
    }

    // The sling: a rounded wrap with two diagonal folds, like the brand mark's curve.
    val wrap = Path().apply {
        moveTo(14f, 100f)
        cubicTo(14f, 74f, 24f, 62f, 50f, 62f)
        cubicTo(76f, 62f, 86f, 74f, 86f, 100f)
        close()
    }
    drawPath(wrap, p.illustrationWrap)
    drawPath(Path().apply { moveTo(22f, 80f); quadraticBezierTo(50f, 70f, 78f, 90f) }, p.illustrationWrapEdge, style = stroke)
    drawPath(Path().apply { moveTo(26f, 94f); quadraticBezierTo(52f, 82f, 80f, 100f) }, p.illustrationWrapEdge, style = stroke)

    // Ears, then the head.
    drawCircle(skin, 6f, Offset(23f, 42f))
    drawCircle(skin, 6f, Offset(77f, 42f))
    drawCircle(skin, 27f, Offset(50f, 40f))

    drawHair(look, p.hair(look.hairColor), stroke)

    // Cheeks.
    drawCircle(p.illustrationCheek.copy(alpha = 0.7f), 5f, Offset(35f, 50f))
    drawCircle(p.illustrationCheek.copy(alpha = 0.7f), 5f, Offset(65f, 50f))

    // Eyes: open dots, closed curves (asleep or mid-blink), or happy arches.
    val closed = mood == BabyMood.ASLEEP || blink >= 0.5f
    for (x in listOf(40f, 60f)) {
        when {
            mood == BabyMood.HAPPY -> drawArc(line, 200f, 140f, false, Offset(x - 4f, 39f), GSize(8f, 7f), style = stroke)
            closed -> drawArc(line, 20f, 140f, false, Offset(x - 4f, 38f), GSize(8f, 6f), style = stroke)
            else -> {
                drawOval(line, Offset(x - 2.6f, 40f - 3f * (1f - blink)), GSize(5.2f, 6f * (1f - blink) + 0.8f))
                // A catchlight, so the eyes read on every skin tone.
                if (blink < 0.3f) drawCircle(p.illustrationCatchlight, 1.1f, Offset(x + 0.9f, 39.2f))
            }
        }
    }

    // Mouth: a small smile; a wider one when happy; a soft "o" asleep.
    when (mood) {
        BabyMood.ASLEEP -> drawCircle(line, 1.6f, Offset(50f, 53f))
        BabyMood.HAPPY -> drawArc(line, 20f, 140f, false, Offset(43f, 48f), GSize(14f, 9f), style = stroke)
        BabyMood.AWAKE -> drawArc(line, 30f, 120f, false, Offset(45f, 49f), GSize(10f, 6f), style = stroke)
    }

    // A bow, for "girl": small details only.
    if (look.gender == BabyGender.GIRL) {
        val bow = p.illustrationBow
        drawPath(Path().apply { moveTo(66f, 17f); lineTo(58f, 11f); lineTo(58f, 23f); close() }, bow)
        drawPath(Path().apply { moveTo(66f, 17f); lineTo(74f, 11f); lineTo(74f, 23f); close() }, bow)
        drawCircle(bow, 2.6f, Offset(66f, 17f))
    }
}

private fun DrawScope.drawHair(look: BabyLook, color: Color, stroke: Stroke) {
    when (look.hair) {
        Hair.NONE -> Unit
        Hair.LITTLE -> {
            // One curl on top.
            drawArc(color, 180f, 270f, false, Offset(45f, 9f), GSize(10f, 10f), style = stroke)
        }
        Hair.CURLY -> {
            for ((x, y) in listOf(28f to 26f, 36f to 18f, 46f to 14f, 56f to 14f, 65f to 18f, 72f to 26f)) {
                drawCircle(color, 7f, Offset(x, y))
            }
        }
        Hair.STRAIGHT -> {
            // A cap over the top of the head with a soft fringe.
            val cap = Path().apply {
                arcTo(Rect(Offset(23f, 13f), GSize(54f, 54f)), 190f, 160f, true)
                quadraticBezierTo(66f, 26f, 58f, 30f)
                quadraticBezierTo(50f, 24f, 42f, 30f)
                quadraticBezierTo(34f, 26f, 25f, 34f)
                close()
            }
            drawPath(cap, color)
        }
        Hair.WAVY -> {
            val cap = Path().apply {
                arcTo(Rect(Offset(23f, 13f), GSize(54f, 54f)), 195f, 150f, true)
                cubicTo(70f, 34f, 64f, 22f, 58f, 30f)
                cubicTo(54f, 22f, 46f, 22f, 42f, 30f)
                cubicTo(36f, 22f, 30f, 34f, 26f, 32f)
                close()
            }
            drawPath(cap, color)
        }
    }
}
