package br.gabriel.cochilo.ai

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Where Ask Cochilo's model is, as the Settings row and the Ask screen see it. */
sealed interface ModelState {
    /** Not on the phone yet. */
    data object Absent : ModelState
    /**
     * On its way: [progress] 0..1, or null when unknown. [waitingForWifi]: queued until Wi-Fi.
     * [copying]: a file the parent chose is being copied in (not a download).
     */
    data class Getting(val progress: Float?, val waitingForWifi: Boolean = false, val copying: Boolean = false) : ModelState
    data class Ready(val file: File, val bytes: Long) : ModelState
    /** This build can't get it (e.g. a Play build made without the model). */
    data object Unavailable : ModelState
    /** The last try failed; [reason] is shown in plain words. */
    data class Failed(val reason: Reason) : ModelState

    /** [NOT_PUBLISHED]: the model isn't on Cochilo's releases yet. [CHECK]: it arrived, but its checksum didn't match. */
    /** [OUTDATED] (0.56.0): the phone has the older `.task` model, which the app no longer runs. */
    enum class Reason { NOT_A_MODEL, TOO_SMALL, NO_SPACE, DOWNLOAD, NOT_PUBLISHED, CHECK, OTHER, OUTDATED }
}

/**
 * How this build gets the model (docs/assistant.md): the play build asks the Play Store for an
 * on-demand asset pack; the github build downloads it itself from Cochilo's releases (and can still
 * take a file the parent chose). One implementation per flavor, `ModelSources.create` in
 * `src/play` and `src/github`.
 */
interface ModelSource {
    /** [PLAY_DOWNLOAD]: the Play Store fetches it. [APP_DOWNLOAD]: the app fetches it, or imports a file. */
    enum class Kind { PLAY_DOWNLOAD, APP_DOWNLOAD }

    val kind: Kind
    val state: StateFlow<ModelState>
    /** About how many MB the model this phone would get takes (0.55.0). */
    val approxMb: StateFlow<Int> get() = kotlinx.coroutines.flow.MutableStateFlow(ModelDownload.APPROX_MB)

    /** Looks again at what's on the phone. */
    fun refresh()

    /** Start the download (Wi-Fi only on the github build, until [useMobileData]). */
    fun download() {}

    /** Github: a download waiting for Wi-Fi goes now, on any network. */
    fun useMobileData() {}

    /** Github: copy the model from [uri] (a file the parent picked). */
    suspend fun import(uri: Uri) {}

    /** Deletes the model from the phone. */
    fun remove()
}
