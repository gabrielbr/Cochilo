package br.gabriel.cochilo.update

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import br.gabriel.cochilo.BuildConfig
import br.gabriel.cochilo.R
import br.gabriel.cochilo.ui.kit.CheckForUpdates
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.UpdateSheetBody
import br.gabriel.cochilo.ui.screens.Sheet
import br.gabriel.cochilo.ui.screens.SheetTitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** This build updates itself from GitHub releases. */
const val SELF_UPDATE = true

/** "Later" hides a version until the app next starts from cold; a manual check still shows it. */
private object Session {
    var dismissed: String? = null
}

/** Checks on every return to the foreground (at most every 15 minutes) and offers the update. */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<UpdateInfo?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        scope.launch {
            val r = Updater.check(context, force = false)
            if (r is CheckResult.Available && r.info.versionName != Session.dismissed) offer = r.info
        }
    }
    offer?.let { info ->
        UpdateSheet(info) {
            Session.dismissed = info.versionName
            offer = null
        }
    }
}

/** About's rows for this build: "Check for updates", after Version. */
@Composable
fun AboutUpdates() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<UpdateInfo?>(null) }
    val latest = stringResource(R.string.update_latest, BuildConfig.VERSION_NAME)
    val offline = stringResource(R.string.update_offline)
    val failed = stringResource(R.string.update_check_failed)

    Hairline()
    if (!Updater.isSupported(context)) {
        CheckForUpdates(
            checking = false, result = null,
            releaseOnlyNote = stringResource(R.string.update_release_only, BuildConfig.VERSION_NAME, BuildConfig.APPLICATION_ID),
        ) {}
        return
    }
    CheckForUpdates(checking, result, releaseOnlyNote = null) {
        checking = true
        result = null
        scope.launch {
            when (val r = Updater.check(context, force = true)) {
                is CheckResult.Available -> found = r.info
                is CheckResult.Failed -> result = failed.format(if (r.reason == "offline") offline else r.reason)
                else -> result = latest
            }
            checking = false
        }
    }
    found?.let { UpdateSheet(it) { found = null } }
}

@Composable
private fun UpdateSheet(info: UpdateInfo, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val allow = stringResource(R.string.update_allow_installs)
    val downloadFailed = stringResource(R.string.update_download_failed)
    val checksumFailed = stringResource(R.string.update_checksum_failed)
    val signatureFailed = stringResource(R.string.update_signature_failed)

    Sheet(onDismiss = onClose, dismissible = progress == null) {
        SheetTitle(stringResource(R.string.update_available, info.versionName))
        UpdateSheetBody(
            version = info.versionName,
            notes = info.notes,
            sizeLabel = Formatter.formatShortFileSize(context, info.apkSize),
            progress = progress,
            message = message,
            onUpdate = {
                if (job?.isActive == true) return@UpdateSheetBody
                if (!Updater.canInstall(context)) {
                    message = allow
                    context.startActivity(Updater.installPermissionIntent(context))
                    return@UpdateSheetBody
                }
                message = null
                progress = 0f
                job = scope.launch {
                    val r = Updater.download(context, info) { progress = it }
                    progress = null
                    when (r) {
                        is DownloadResult.Ready -> {
                            Updater.install(context, r.apk)
                            onClose()
                        }
                        DownloadResult.ChecksumMismatch -> message = checksumFailed
                        DownloadResult.SignatureMismatch -> message = signatureFailed
                        DownloadResult.Failed -> message = downloadFailed
                        DownloadResult.Busy -> Unit
                    }
                }
            },
            onLater = onClose,
            onCancel = {
                job?.cancel()
                progress = null
            },
        )
    }
}
