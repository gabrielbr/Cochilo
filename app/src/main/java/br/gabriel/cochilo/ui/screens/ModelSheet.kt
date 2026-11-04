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
    is ModelState.Failed -> stringResource(if (state.reason == ModelState.Reason.OUTDATED) R.string.model_status_outdated else R.string.model_status_failed)
}

private fun megabytes(bytes: Long) = (bytes / (1024 * 1024)).toInt()

/**
 * Settings › Ask Cochilo › On-device AI, also opened from Ask's "get it" offer (docs/assistant.md):
 * what it is, what it costs, and one action for the state it's in. Both builds: Download, one tap
 * (the Play Store fetches it on play; the app itself on the family build, on Wi-Fi, in the
 * background, with the system's progress notification). The family build can also take a file
 * already on the phone. Then Remove.
 */
@Composable
fun ModelSheet(
    state: ModelState, kind: ModelSource.Kind, onDownload: () -> Unit, onImport: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit,
    onMobileData: () -> Unit = {},
    /** About how big this phone's model is (0.55.0). */
    sizeMb: Int = br.gabriel.cochilo.ai.ModelDownload.APPROX_MB,
) {
    Sheet(onDismiss) { ModelBody(state, kind, onDownload, onImport, onRemove, onDismiss, onMobileData, sizeMb) }
}

@Composable
fun ModelBody(
    state: ModelState, kind: ModelSource.Kind, onDownload: () -> Unit, onImport: () -> Unit, onRemove: () -> Unit, onClose: () -> Unit,
    onMobileData: () -> Unit = {},
    sizeMb: Int = br.gabriel.cochilo.ai.ModelDownload.APPROX_MB,
) {
    val c = CochiloTheme.colors
    val app = kind == ModelSource.Kind.APP_DOWNLOAD
    val size = modelSize(sizeMb)
    Column(Modifier.fillMaxWidth()) {
        SheetTitle(stringResource(R.string.model_title))
        Text(stringResource(R.string.model_line), style = Type.body, color = c.ink)
        Spacer(Modifier.height(Spacing.space2))
        Text(stringResource(R.string.model_cost, size), style = Type.meta, color = c.muted)
        Spacer(Modifier.height(Spacing.space1))
        Text(stringResource(R.string.model_terms), style = Type.meta, color = c.muted)
        Spacer(Modifier.height(Spacing.space6))
        when (state) {
            is ModelState.Getting -> {
                val pct = state.progress?.let { (it * 100).toInt() }
                val words = when {
                    state.waitingForWifi -> stringResource(R.string.model_waiting_wifi)
                    state.copying -> if (pct != null) stringResource(R.string.model_copying_pct, pct) else stringResource(R.string.model_copying)
                    !app -> if (pct != null) stringResource(R.string.model_downloading_pct, pct) else stringResource(R.string.model_downloading)
                    else -> if (pct != null) stringResource(R.string.model_app_downloading_pct, pct) else stringResource(R.string.model_app_downloading)
                }
                Text(words, style = Type.bodySmall, color = c.ink, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Spacer(Modifier.height(Spacing.space2))
                ProgressLine(state.progress ?: 0f)
                if (app && !state.copying) {
                    Spacer(Modifier.height(Spacing.space2))
                    Text(stringResource(R.string.model_background), style = Type.meta, color = c.muted)
                }
                Spacer(Modifier.height(Spacing.space6))
                if (app && state.waitingForWifi) {
                    Button(stringResource(R.string.model_mobile_data), onMobileData, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Spacing.space2))
                }
                if (app) {
                    Button(stringResource(R.string.model_stop), onRemove, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Spacing.space2))
                }
            }
            is ModelState.Ready -> {
                Text(stringResource(R.string.model_ready, megabytes(state.bytes)), style = Type.bodySmall, color = c.ink)
                Spacer(Modifier.height(Spacing.space6))
                Button(stringResource(R.string.model_remove), onRemove, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.space2))
            }
            ModelState.Unavailable -> {
                Text(stringResource(R.string.model_unavailable), style = Type.bodySmall, color = c.ink)
                Spacer(Modifier.height(Spacing.space6))
            }
            ModelState.Absent, is ModelState.Failed -> {
                if (state is ModelState.Failed) {
                    Text(stringResource(failedRes(state.reason)), style = Type.bodySmall, color = c.ink, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Spacer(Modifier.height(Spacing.space4))
                }
                if (app) {
                    Text(stringResource(R.string.model_wifi_line), style = Type.meta, color = c.muted)
                    Spacer(Modifier.height(Spacing.space4))
                }
                Button(stringResource(R.string.model_download, size), onDownload, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.space2))
                if (app) {
                    Button(stringResource(R.string.model_import), onImport, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Spacing.space2))
                }
            }
        }
        Button(stringResource(R.string.close), onClose, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}

private fun failedRes(r: ModelState.Reason): Int = when (r) {
    ModelState.Reason.NOT_A_MODEL -> R.string.model_failed_not_model
    ModelState.Reason.TOO_SMALL -> R.string.model_failed_too_small
    ModelState.Reason.NO_SPACE -> R.string.model_failed_no_space
    ModelState.Reason.DOWNLOAD -> R.string.model_failed_download
    ModelState.Reason.NOT_PUBLISHED -> R.string.model_failed_not_published
    ModelState.Reason.CHECK -> R.string.model_failed_check
    ModelState.Reason.OTHER -> R.string.model_failed_other
    ModelState.Reason.OUTDATED -> R.string.model_failed_outdated
}

/** "550 MB", "3.5 GB": the model's size in the phone's own units (0.55.0). */
@Composable
fun modelSize(mb: Int): String = android.text.format.Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, mb * 1_000_000L)
