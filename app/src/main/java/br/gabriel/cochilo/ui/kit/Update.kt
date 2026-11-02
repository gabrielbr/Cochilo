package br.gabriel.cochilo.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.R
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

// The design system's update sheet and "Check for updates" row (docs/self-update.md there).
// Stateless, so both builds render and screenshot them the same way; only the github build
// shows them.

/**
 * The body of the "Update available" sheet. [progress] null = not downloading: notes, then
 * Update · size (the sheet's one primary action) and Later. While downloading: "Downloading… n%",
 * the frame's line at n%, and Later becomes Cancel.
 */
@Composable
fun UpdateSheetBody(
    version: String,
    notes: String,
    sizeLabel: String,
    progress: Float?,
    message: String?,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = CochiloTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text(notes.ifBlank { stringResource(R.string.update_notes_default) }, style = Type.bodySmall, color = c.muted)
        if (message != null) {
            Spacer(Modifier.height(Spacing.space3))
            Text(message, style = Type.bodySmall, color = c.coralText, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.height(Spacing.space6))
        if (progress != null) {
            val pct = (progress.coerceIn(0f, 1f) * 100).toInt()
            Text(
                stringResource(R.string.update_downloading, pct), style = Type.label, color = c.ink,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(Spacing.space2))
            ProgressLine(progress)
            Spacer(Modifier.height(Spacing.space6))
            Button(stringResource(R.string.update_cancel), onCancel, modifier = Modifier.fillMaxWidth())
        } else {
            Button(
                stringResource(R.string.update_action, sizeLabel), onUpdate,
                kind = ButtonKind.PRIMARY, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.space3))
            Button(stringResource(R.string.update_later), onLater, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The frame's line as a progress bar: mint on a hairline track, frame-thick. */
@Composable
fun ProgressLine(progress: Float) {
    val c = CochiloTheme.colors
    val p = progress.coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxWidth().height(Size.frame).background(c.line)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) }
    ) {
        Box(Modifier.fillMaxWidth(p).height(Size.frame).background(c.mint))
    }
}

/**
 * About's "Check for updates": a secondary button and one result line. [releaseOnlyNote] replaces
 * both on a debug build, which can't update itself.
 */
@Composable
fun CheckForUpdates(checking: Boolean, result: String?, releaseOnlyNote: String?, onCheck: () -> Unit) {
    val c = CochiloTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.space4, vertical = Spacing.space3)) {
        Text(stringResource(R.string.update_check), style = Type.body, color = c.ink)
        Text(releaseOnlyNote ?: stringResource(R.string.update_check_meta), style = Type.meta, color = c.muted)
        if (releaseOnlyNote == null) {
            Spacer(Modifier.height(Spacing.space3))
            Button(
                stringResource(if (checking) R.string.update_checking else R.string.update_check),
                onCheck, enabled = !checking, modifier = Modifier.fillMaxWidth(),
            )
            if (result != null) {
                Spacer(Modifier.height(Spacing.space2))
                Text(result, style = Type.meta, color = c.muted, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}
