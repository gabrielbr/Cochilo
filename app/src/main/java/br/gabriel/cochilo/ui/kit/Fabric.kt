package br.gabriel.cochilo.ui.kit

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import br.gabriel.cochilo.R
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/**
 * Tatá's fabrics, photographed from her slings on tataslings.com.br (© Tatá Slings, used with the
 * owner's permission; crops in docs/tata-slings/textures.md). The person picks one in Settings and
 * it wraps the Today card. [PLAIN] is no fabric.
 */
enum class Fabric(val key: String, @StringRes val label: Int, @DrawableRes val texture: Int?) {
    PLAIN("plain", R.string.fabric_plain, null),
    PEROLA("perola", R.string.fabric_perola, R.drawable.tex_perola),
    ROSA_CHA("rosa_cha", R.string.fabric_rosa_cha, R.drawable.tex_rosa_cha),
    CAPUCCINO("capuccino", R.string.fabric_capuccino, R.drawable.tex_capuccino),
    MEL("mel", R.string.fabric_mel, R.drawable.tex_mel),
    TREVO("trevo", R.string.fabric_trevo, R.drawable.tex_trevo),
    CEREJA("cereja", R.string.fabric_cereja, R.drawable.tex_cereja),
    CREPUSCULO("crepusculo", R.string.fabric_crepusculo, R.drawable.tex_crepusculo),
    CHUMBO("chumbo", R.string.fabric_chumbo, R.drawable.tex_chumbo),
    BALAO("balao", R.string.fabric_balao, R.drawable.tex_balao),
    FOLHAGEM("folhagem", R.string.fabric_folhagem, R.drawable.tex_folhagem),
    TROPICAL("tropical", R.string.fabric_tropical, R.drawable.tex_tropical);

    companion object {
        /** The saved key's fabric; anything unknown is plain. */
        fun of(key: String?): Fabric = entries.firstOrNull { it.key == key } ?: PLAIN
    }
}

/** The fabric the person picked, provided once at the root. */
val LocalFabric = staticCompositionLocalOf { Fabric.PLAIN }

/**
 * The fabric as a brush. Since 0.26.0 each photo is a true repeating tile
 * (scripts/seamless_tile.py), so it repeats like cloth instead of mirroring into a kaleidoscope.
 */
@Composable
fun fabricBrush(@DrawableRes texture: Int): Brush {
    val img = ImageBitmap.imageResource(texture)
    return remember(img) { ShaderBrush(ImageShader(img, TileMode.Repeated, TileMode.Repeated)) }
}

/**
 * Fabric frame (design system, components.md): a card wrapped in the picked fabric, the way a
 * sling wraps the baby. The fabric shows as a band around the card, wider on top like the sling's
 * fold ([Spacing.space8] on top, [Spacing.space3] on the other sides); every word
 * stays on the card's own [inner] colour, so no text ever sits on the photo. With no fabric, it is
 * just the card.
 */
@Composable
fun FabricFrame(inner: Color, padding: Dp, modifier: Modifier = Modifier, fabric: Fabric = LocalFabric.current, content: @Composable () -> Unit) {
    // A new fabric cross-fades in (motion-emphasised; a snap with Remove animations).
    androidx.compose.animation.Crossfade(fabric, modifier, animationSpec = motionSpec(emphasised = true), label = "fabric") { f ->
        FabricFrameNow(inner, padding, f, content)
    }
}

@Composable
private fun FabricFrameNow(inner: Color, padding: Dp, fabric: Fabric, content: @Composable () -> Unit) {
    val modifier = Modifier
    val texture = fabric.texture
    if (texture == null) {
        Column(modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(inner).padding(horizontal = padding, vertical = Spacing.space8)) { content() }
        return
    }
    // A wider band since 0.45.0 (the owner): more of her fabric shows, like a sling's fold on top.
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(fabricBrush(texture))
        .padding(start = Spacing.space4, end = Spacing.space4, bottom = Spacing.space4, top = Spacing.space16)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusS)).background(inner).padding(horizontal = padding, vertical = Spacing.space8)) { content() }
    }
}

/**
 * The fabric picker: her fabrics as round swatches in one row, the name under each; one tap
 * picks and the Today card changes at once. The picked one has an ink ring. The row scrolls, and
 * the next swatch always peeks in at the edge so the rest are found.
 */
@Composable
fun FabricPicker(selected: Fabric, onPick: (Fabric) -> Unit) {
    val c = CochiloTheme.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = Spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
    ) {
        Fabric.entries.forEach { f ->
            val on = f == selected
            val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Column(
                Modifier.width(Size.button + Spacing.space3)
                    .selectable(on, interactionSource = src, indication = null, role = Role.RadioButton) { onPick(f) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The ring cross-fades to the picked swatch; the swatch dips under the finger.
                val ring = animatedColor(if (on) c.ink else c.line)
                val swatch = Modifier.size(Size.button).clip(RoundedCornerShape(Radius.radiusRound))
                Box(
                    Modifier.pressScale(src).size(Size.button).border(if (on) Size.outline else Size.hairline, ring, RoundedCornerShape(Radius.radiusRound))
                        .padding(if (on) Spacing.space1 else Size.hairline),
                    contentAlignment = Alignment.Center,
                ) {
                    val t = f.texture
                    if (t != null) Box(swatch.background(fabricBrush(t)))
                    else Box(swatch.background(c.page))
                }
                Text(
                    stringResource(f.label), style = Type.meta, color = animatedColor(if (on) c.ink else c.muted),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.space1),
                )
            }
        }
    }
}
