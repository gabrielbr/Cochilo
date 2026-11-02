package br.gabriel.cochilo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.domain.BabyLook
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.BabyAvatar
import br.gabriel.cochilo.ui.kit.BabyMood
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.DatePickerSheet
import br.gabriel.cochilo.ui.kit.Field
import br.gabriel.cochilo.ui.kit.LookPicker
import br.gabriel.cochilo.ui.kit.PickerField
import br.gabriel.cochilo.ui.kit.ScreenTitle
import br.gabriel.cochilo.ui.kit.fadeInMotion
import br.gabriel.cochilo.ui.kit.fadeOutMotion
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import java.time.LocalDate
import java.time.ZoneId

/**
 * The first run (docs/onboarding.md): two short steps, the second skippable. 1) Meet the baby:
 * name, birth date and, if born early, the due date. 2) How the baby looks: the avatar the
 * celebrations use. Then Today. Nothing is asked twice (remember actions); everything can change
 * later in Settings.
 */
@Composable
fun Onboarding(zone: ZoneId, onDone: (Baby) -> Unit, startStep: Int = 0, previewName: String = "") {
    var step by remember { mutableIntStateOf(startStep) }
    var name by remember { mutableStateOf(previewName) }
    var birth by remember { mutableStateOf(LocalDate.now(zone)) }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    var look by remember { mutableStateOf(BabyLook()) }
    var picking by remember { mutableStateOf<String?>(null) }
    val f = Format(LocalContext.current, zone)
    val ok = stringResource(R.string.ok)
    val cancel = stringResource(R.string.cancel)

    fun baby(withLook: Boolean) = Baby(
        name = name.trim(),
        birthDate = birth.atStartOfDay(zone).toInstant().toEpochMilli(),
        dueDate = due?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
        look = if (withLook) look.encode() else "",
    )

    val enter = fadeInMotion()
    val exit = fadeOutMotion()
    AnimatedContent(step, transitionSpec = { enter togetherWith exit }, label = "onboarding") { s ->
        ScrollPage {
            if (s == 0) {
                ScreenTitle(stringResource(R.string.welcome_title))
                Spacer(Modifier.height(Spacing.space6))
                BabyAvatar(BabyLook(), mood = BabyMood.ASLEEP, modifier = Modifier.padding(bottom = Spacing.space4))
                Text(stringResource(R.string.welcome_line), style = Type.body, color = CochiloTheme.colors.muted)
                Spacer(Modifier.height(Spacing.space6))
                Field(name, { name = it.take(40) }, stringResource(R.string.baby_name))
                Spacer(Modifier.height(Spacing.space4))
                PickerField(stringResource(R.string.birth_date), f.day(birth), { picking = "birth" }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.space4))
                PickerField(stringResource(R.string.due_date), due?.let { f.day(it) } ?: stringResource(R.string.not_set), { picking = "due" }, Modifier.fillMaxWidth())
                Text(stringResource(R.string.due_date_meta), style = Type.meta, color = CochiloTheme.colors.muted, modifier = Modifier.padding(top = Spacing.space1))
                Spacer(Modifier.height(Spacing.space8))
                Button(stringResource(R.string.onboarding_next), { step = 1 }, ButtonKind.PRIMARY, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
            } else {
                ScreenTitle(stringResource(R.string.look_title, name.trim()))
                Spacer(Modifier.height(Spacing.space2))
                Text(stringResource(R.string.look_line, name.trim()), style = Type.body, color = CochiloTheme.colors.muted)
                Spacer(Modifier.height(Spacing.space6))
                LookPicker(look, { look = it }, name.trim())
                Spacer(Modifier.height(Spacing.space8))
                Button(stringResource(R.string.onboarding_done), { onDone(baby(withLook = true)) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.space2))
                Button(stringResource(R.string.onboarding_skip), { onDone(baby(withLook = false)) }, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
            }
        }
    }
    when (picking) {
        "birth" -> DatePickerSheet(birth, ok, cancel, { birth = it }, { picking = null })
        "due" -> DatePickerSheet(due ?: birth, ok, cancel, { due = it }, { picking = null })
    }
}

/** Settings › Baby › Appearance: the same picker in a sheet. */
@Composable
fun LookSheet(baby: Baby, onSave: (Baby) -> Unit, onDismiss: () -> Unit) {
    var look by remember { mutableStateOf(BabyLook.decode(baby.look)) }
    Sheet(onDismiss) {
        SheetTitle(stringResource(R.string.look_title, baby.name))
        LookPicker(look, { look = it }, baby.name)
        Spacer(Modifier.height(Spacing.space6))
        Button(stringResource(R.string.save), { onSave(baby.copy(look = look.encode())) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.cancel), onDismiss, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}
