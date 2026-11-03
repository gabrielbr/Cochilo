package br.gabriel.cochilo.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** The family build: the parent picks the model file, which is copied into the app's own storage. */
object ModelSources {
    fun create(context: Context): ModelSource = FileImportSource(context.applicationContext)
}

private class FileImportSource(private val context: Context) : ModelSource {
    private val file = File(context.filesDir, "models/assistant.task")
    private val _state = MutableStateFlow<ModelState>(ModelState.Absent)
    override val kind = ModelSource.Kind.FILE_IMPORT
    override val state: StateFlow<ModelState> = _state

    init { refresh() }

    override fun refresh() {
        if (_state.value is ModelState.Getting) return
        _state.value = if (file.exists()) ModelState.Ready(file, file.length()) else ModelState.Absent
    }

    override suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()?.takeIf { it > 0 }
        val head = runCatching { resolver.openInputStream(uri)?.use { s -> ByteArray(2).also { s.read(it) } } }.getOrNull() ?: ByteArray(0)
        ModelFile.check(head, size ?: Long.MAX_VALUE)?.let { _state.value = ModelState.Failed(it); return@withContext }
        if (size != null && context.filesDir.usableSpace < size + ModelFile.MIN_BYTES) {
            _state.value = ModelState.Failed(ModelState.Reason.NO_SPACE); return@withContext
        }
        _state.value = ModelState.Getting(if (size != null) 0f else null)
        val copied = runCatching {
            resolver.openInputStream(uri)!!.use { input ->
                ModelFile.copy(input, file) { done -> if (size != null) _state.value = ModelState.Getting((done.toFloat() / size).coerceIn(0f, 1f)) }
            }
        }
        _state.value = copied.fold(
            onSuccess = { bytes -> if (bytes < ModelFile.MIN_BYTES) { file.delete(); ModelState.Failed(ModelState.Reason.TOO_SMALL) } else ModelState.Ready(file, bytes) },
            onFailure = { ModelState.Failed(ModelState.Reason.OTHER) },
        )
    }

    override fun remove() {
        file.delete()
        _state.value = ModelState.Absent
    }
}
