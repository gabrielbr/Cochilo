package br.gabriel.cochilo.ai

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Where Ask Cochilo's model is, as the Settings row and the Ask screen see it. */
sealed interface ModelState {
    /** Not on the phone yet. */
    data object Absent : ModelState
    /** On its way: [progress] 0..1, or null when unknown. */
    data class Getting(val progress: Float?) : ModelState
    data class Ready(val file: File, val bytes: Long) : ModelState
    /** This build can't get it (e.g. a Play build made without the model). */
    data object Unavailable : ModelState
    /** The last try failed; [reason] is shown in plain words. */
    data class Failed(val reason: Reason) : ModelState

    enum class Reason { NOT_A_MODEL, TOO_SMALL, NO_SPACE, DOWNLOAD, OTHER }
}

/**
 * How this build gets the model (docs/assistant.md): the play build asks the Play Store for an
 * on-demand asset pack; the github build imports a file the parent chose. One implementation per
 * flavor, `ModelSources.create` in `src/play` and `src/github`.
 */
interface ModelSource {
    enum class Kind { PLAY_DOWNLOAD, FILE_IMPORT }

    val kind: Kind
    val state: StateFlow<ModelState>

    /** Looks again at what's on the phone. */
    fun refresh()

    /** Play: start the download. */
    fun download() {}

    /** Github: copy the model from [uri] (a file the parent picked). */
    suspend fun import(uri: Uri) {}

    /** Deletes the model from the phone. */
    fun remove()
}
