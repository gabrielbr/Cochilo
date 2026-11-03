package br.gabriel.cochilo.ai

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * The play build: the model is an on-demand Play asset pack (`:assistant_model`). The Play Store
 * downloads it; the app, which has no INTERNET permission, only asks for it and reads the file.
 */
object ModelSources {
    fun create(context: Context): ModelSource = PlayAssetSource(context.applicationContext)
}

private const val PACK = "assistant_model"
private const val MODEL = "model.task"

private class PlayAssetSource(context: Context) : ModelSource {
    private val manager: AssetPackManager = AssetPackManagerFactory.getInstance(context)
    private val _state = MutableStateFlow<ModelState>(ModelState.Absent)
    override val kind = ModelSource.Kind.PLAY_DOWNLOAD
    override val state: StateFlow<ModelState> = _state

    private val listener = AssetPackStateUpdateListener { s ->
        if (s.name() != PACK) return@AssetPackStateUpdateListener
        _state.value = when (s.status()) {
            AssetPackStatus.PENDING, AssetPackStatus.DOWNLOADING, AssetPackStatus.TRANSFERRING, AssetPackStatus.WAITING_FOR_WIFI ->
                ModelState.Getting(if (s.totalBytesToDownload() > 0) s.bytesDownloaded().toFloat() / s.totalBytesToDownload() else null)
            AssetPackStatus.COMPLETED -> located() ?: ModelState.Unavailable
            AssetPackStatus.FAILED -> ModelState.Failed(ModelState.Reason.DOWNLOAD)
            AssetPackStatus.CANCELED, AssetPackStatus.NOT_INSTALLED -> ModelState.Absent
            else -> _state.value
        }
    }

    init {
        manager.registerListener(listener)
        refresh()
    }

    /** The model inside the downloaded pack, or null when the pack isn't there or was built without it. */
    private fun located(): ModelState? {
        val dir = manager.getPackLocation(PACK)?.assetsPath() ?: return null
        val f = File(dir, MODEL)
        return if (f.exists()) ModelState.Ready(f, f.length()) else ModelState.Unavailable
    }

    override fun refresh() {
        if (_state.value is ModelState.Getting) return
        _state.value = located() ?: ModelState.Absent
    }

    override fun download() {
        _state.value = ModelState.Getting(null)
        manager.fetch(listOf(PACK)).addOnFailureListener { _state.value = ModelState.Failed(ModelState.Reason.DOWNLOAD) }
    }

    override fun remove() {
        manager.removePack(PACK)
        _state.value = ModelState.Absent
    }
}
