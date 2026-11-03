package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import br.gabriel.cochilo.R
import br.gabriel.cochilo.ai.ModelSource
import br.gabriel.cochilo.ai.ModelState
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.ProgressLine
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/** The Settings row's words for each state. */
@Composable
fun modelStatus(state: ModelState): String = when (state) {
    ModelState.Absent -> stringResource(R.string.model_status_absent)
    is ModelState.Getting -> stringResource(R.string.model_status_getting)
    is ModelState.Ready -> stringResource(R.string.model_status_ready, megabytes(state.bytes))
    ModelState.Unavailable -> stringResource(R.string.model_status_unavailable)
    is ModelState.Failed -> stringResource(R.string.model_status_failed)
}

private fun megabytes(bytes: Long) = (bytes / (1024 * 1024)).toInt()

/**
 * Settings › Ask Cochilo › On-device AI (docs/assistant.md, phase 2): what it is, what it costs,
 * and one action for the state it's in. Play: Download (the Play Store fetches it). Family build:
 * Choose the model file. Then Remove.
 */
@Composable
fun ModelSheet(state: ModelState, kind: ModelSource.Kind, onDownload: () -> Unit, onImport: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) { ModelBody(state, kind, onDownload, onImport, onRemove, onDismiss) }
}

@Composable
fun ModelBody(state: ModelState, kind: ModelSource.Kind, onDownload: () -> Unit, onImport: () -> Unit, onRemove: () -> Unit, onClose: () -> Unit) {
    val c = CochiloTheme.colors
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.model_title))
        Text(stringResource(R.string.model_line), style = Type.body, color = c.ink)
        Spacer(Modifier.height(Spacing.space2))
        Text(stringResource(R.string.model_cost), style = Type.meta, color = c.muted)
        Spacer(Modifier.height(Spacing.space6))
        when (state) {
            is ModelState.Getting -> {
                val words = if (state.progress != null) stringResource(
                    if (kind == ModelSource.Kind.PLAY_DOWNLOAD) R.string.model_downloading_pct else R.string.model_copying_pct,
                    (state.progress * 100).toInt(),
                ) else stringResource(if (kind == ModelSource.Kind.PLAY_DOWNLOAD) R.string.model_downloading else R.string.model_copying)
                Text(words, style = Type.bodySmall, color = c.ink, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Spacer(Modifier.height(Spacing.space2))
                ProgressLine(state.progress ?: 0f)
                Spacer(Modifier.height(Spacing.space4))
            }
            is ModelState.Ready -> {
                Text(stringResource(R.string.model_ready, megabytes(state.bytes)), style = Type.bodySmall, color = c.ink)
                Spacer(Modifier.height(Spacing.space6))
                Button(stringResource(R.string.model_remove), onRemove, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
            }
            ModelState.Unavailable -> {
                Text(stringResource(R.string.model_unavailable), style = Type.bodySmall, color = c.ink)
                Spacer(Modifier.height(Spacing.space4))
            }
            ModelState.Absent, is ModelState.Failed -> {
                if (state is ModelState.Failed) {
                    Text(stringResource(failedRes(state.reason)), style = Type.bodySmall, color = c.ink, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Spacer(Modifier.height(Spacing.space4))
                }
                if (kind == ModelSource.Kind.PLAY_DOWNLOAD) {
                    Button(stringResource(R.string.model_download), onDownload, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
                } else {
                    Text(stringResource(R.string.model_import_how), style = Type.meta, color = c.muted)
                    Spacer(Modifier.height(Spacing.space4))
                    Button(stringResource(R.string.model_import), onImport, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.close), onClose, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}

private fun failedRes(r: ModelState.Reason): Int = when (r) {
    ModelState.Reason.NOT_A_MODEL -> R.string.model_failed_not_model
    ModelState.Reason.TOO_SMALL -> R.string.model_failed_too_small
    ModelState.Reason.NO_SPACE -> R.string.model_failed_no_space
    ModelState.Reason.DOWNLOAD -> R.string.model_failed_download
    ModelState.Reason.OTHER -> R.string.model_failed_other
}
