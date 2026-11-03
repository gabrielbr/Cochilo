package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import br.gabriel.cochilo.R
import br.gabriel.cochilo.backup.Sealed
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.Field
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

private val passphraseKeyboard = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrect = false, imeAction = ImeAction.Done)

/**
 * Settings › Data › Protect backups (docs/backup.md): set, change or remove the passphrase that
 * seals every exported backup. Shown as typed, so it can be checked once instead of typed twice;
 * the line under it says plainly that a lost passphrase can't be recovered.
 */
@Composable
fun PassphraseSheet(current: String?, onSave: (String?) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) { PassphraseBody(current, onSave = { onSave(it); onDismiss() }, onCancel = onDismiss) }
}

/** The sheet's content, apart so it can be screenshotted (modal sheets can't). */
@Composable
fun PassphraseBody(current: String?, onSave: (String?) -> Unit, onCancel: () -> Unit, initial: String = current ?: "") {
    var text by remember { mutableStateOf(initial) }
    val ok = text.trim().length >= Sealed.MIN_LENGTH
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.passphrase_title))
        Text(stringResource(R.string.passphrase_line), style = Type.body, color = CochiloTheme.colors.muted)
        Spacer(Modifier.height(Spacing.space4))
        Field(text, { text = it.take(MAX_LENGTH) }, stringResource(R.string.passphrase_field), keyboard = passphraseKeyboard)
        Text(stringResource(R.string.passphrase_meta), style = Type.meta, color = CochiloTheme.colors.muted, modifier = Modifier.padding(top = Spacing.space1))
        Spacer(Modifier.height(Spacing.space6))
        Button(stringResource(R.string.save), { onSave(text.trim()) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth(), enabled = ok)
        Spacer(Modifier.height(Spacing.space2))
        if (current != null) {
            Button(stringResource(R.string.passphrase_remove), { onSave(null) }, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.space2))
        }
        Button(stringResource(R.string.cancel), onCancel, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}

/** Importing a sealed backup this phone has no passphrase for: ask once; a wrong one says so. */
@Composable
fun UnlockSheet(wrong: Boolean, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) { UnlockBody(wrong, onOpen, onDismiss) }
}

@Composable
fun UnlockBody(wrong: Boolean, onOpen: (String) -> Unit, onCancel: () -> Unit, initial: String = "") {
    var text by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.unlock_title))
        Text(stringResource(R.string.unlock_line), style = Type.body, color = CochiloTheme.colors.muted)
        Spacer(Modifier.height(Spacing.space4))
        Field(text, { text = it.take(MAX_LENGTH) }, stringResource(R.string.passphrase_field), keyboard = passphraseKeyboard)
        if (wrong) {
            Text(
                stringResource(R.string.unlock_wrong), style = Type.meta, color = CochiloTheme.colors.ink,
                modifier = Modifier.padding(top = Spacing.space1).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(Spacing.space6))
        Button(stringResource(R.string.unlock_open), { onOpen(text.trim()) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.cancel), onCancel, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}

private const val MAX_LENGTH = 64
