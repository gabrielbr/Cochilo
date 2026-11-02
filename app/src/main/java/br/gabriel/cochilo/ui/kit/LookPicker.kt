package br.gabriel.cochilo.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.gabriel.cochilo.R
import br.gabriel.cochilo.domain.BabyGender
import br.gabriel.cochilo.domain.BabyLook
import br.gabriel.cochilo.domain.Hair
import br.gabriel.cochilo.domain.HairColor
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/**
 * The look picker (design system, components.md): how the baby looks, for the avatar. A live
 * preview on top, then skin tone, hair, hair colour, and boy / girl / rather not say. Every choice
 * is one tap and shows at once; all have defaults, so the parent can skip it.
 */
@Composable
fun LookPicker(look: BabyLook, onChange: (BabyLook) -> Unit, name: String) {
    val c = CochiloTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BabyAvatar(look, description = stringResource(R.string.avatar_of, name))
        Spacer(Modifier.height(Spacing.space4))

        PickerLabel(stringResource(R.string.look_skin))
        Swatches(
            (1..BabyLook.SKIN_TONES).toList(), look.skin, { c.skin(it) },
            { stringResource(R.string.look_skin_n, it) },
        ) { onChange(look.copy(skin = it)) }

        PickerLabel(stringResource(R.string.look_hair))
        val hairNames = mapOf(
            Hair.NONE to stringResource(R.string.hair_none), Hair.LITTLE to stringResource(R.string.hair_little),
            Hair.CURLY to stringResource(R.string.hair_curly), Hair.STRAIGHT to stringResource(R.string.hair_straight),
            Hair.WAVY to stringResource(R.string.hair_wavy),
        )
        Box(Modifier.fillMaxWidth()) { FilterChips(Hair.entries, look.hair, { hairNames.getValue(it) }) { onChange(look.copy(hair = it)) } }

        if (look.hair != Hair.NONE) {
            PickerLabel(stringResource(R.string.look_hair_color))
            val colourNames = mapOf(
                HairColor.BLACK to stringResource(R.string.hair_black), HairColor.BROWN to stringResource(R.string.hair_brown),
                HairColor.BLOND to stringResource(R.string.hair_blond), HairColor.RED to stringResource(R.string.hair_red),
            )
            Swatches(HairColor.entries, look.hairColor, { c.hair(it) }, { colourNames.getValue(it) }) { onChange(look.copy(hairColor = it)) }
        }

        PickerLabel(stringResource(R.string.look_gender))
        val genderNames = mapOf(
            BabyGender.GIRL to stringResource(R.string.gender_girl), BabyGender.BOY to stringResource(R.string.gender_boy),
            BabyGender.UNSAID to stringResource(R.string.gender_unsaid),
        )
        Box(Modifier.fillMaxWidth()) { FilterChips(BabyGender.entries, look.gender, { genderNames.getValue(it) }) { onChange(look.copy(gender = it)) } }
    }
}

@Composable
private fun PickerLabel(text: String) {
    Text(
        text, style = Type.caption, color = CochiloTheme.colors.muted,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.space4, bottom = Spacing.space1),
    )
}

/** Round colour swatches in a row, 48dp targets; the picked one has the outline ring. */
@Composable
private fun <T> Swatches(options: List<T>, selected: T, color: @Composable (T) -> Color, name: @Composable (T) -> String, onPick: (T) -> Unit) {
    val c = CochiloTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        options.forEach { o ->
            val on = o == selected
            val src = remember { MutableInteractionSource() }
            val spoken = name(o)
            Box(
                Modifier.size(Spacing.space12)
                    .semantics { contentDescription = spoken }
                    .selectable(on, interactionSource = src, indication = null, role = Role.RadioButton) { onPick(o) }
                    .pressScale(src)
                    .border(if (on) Size.outline else Size.hairline, animatedColor(if (on) c.ink else c.line), RoundedCornerShape(Radius.radiusRound))
                    .padding(Spacing.space1),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(Spacing.space10).clip(RoundedCornerShape(Radius.radiusRound)).background(color(o))) }
        }
    }
}
