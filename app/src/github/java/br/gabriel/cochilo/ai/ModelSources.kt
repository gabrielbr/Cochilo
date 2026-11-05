package br.gabriel.cochilo.ai

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import br.gabriel.cochilo.BuildConfig
import br.gabriel.cochilo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The family build: one tap downloads the model from Cochilo's own GitHub releases
 * ([ModelDownload]), through Android's download manager, so it keeps going with the app closed,
 * shows its progress in the system's notification, waits for Wi-Fi unless the parent says
 * otherwise, and resumes after a dropped connection. What arrives is checked against the
 * published SHA-256 before it's used. A file the parent already has can still be chosen instead.
 * The request carries nothing about the person or the baby.
 *
 * Which model (0.55.0): Gemma 3n on a phone with the memory for it, if it's published; else the 1B
 * for LiteRT-LM (0.56.0: the older `.task` is no longer offered).
 *
 * Resuming (0.57.0): the model comes down in pieces of about 100 MB (`<file>.pieces`), one after
 * the other, and every piece that finished stays on the phone. A dropped connection resumes inside
 * the piece (the download manager does that); anything worse (the download manager giving up, the
 * phone restarting, the parent switching to mobile data) loses at most the piece in flight, and
 * getting it again carries on from the first piece that's missing. Only Stop, Remove, a damaged
 * piece or a newer published model start over. The pieces are joined, each deleted as it's added,
 * and the whole file is checked against the published SHA-256. Before pieces were published, the
 * model's own parts (one for the 1B, two for Gemma 3n) play the same role.
 */
object ModelSources {
    fun create(context: Context): ModelSource = DownloadSource(context.applicationContext)
}

private const val PREFS = "assistant_model"
private const val KEY_ID = "download_id"
private const val KEY_SHA = "sha256"
/** Which model is on its way (ModelDownload.Choice), and which of its pieces is in flight (0.55.0). */
private const val KEY_CHOICE = "choice"
private const val KEY_PART = "part"
/** The pieces of the model on its way (ModelDownload.encode), and how many finished (0.57.0). */
private const val KEY_PIECES = "pieces"
private const val KEY_DONE = "done"
private const val KEY_METERED = "metered"

private class DownloadSource(private val context: Context) : ModelSource {
    /** A chosen file is copied here. */
    private val imported = File(context.filesDir, "models/assistant.task")
    /** A download lands in the app's own external folder (no permission; other apps can't read it). */
    private val downloadedDir get() = File(context.getExternalFilesDir(null), "models")
    private val downloaded get() = File(downloadedDir, "assistant.task")
    private val partial get() = File(downloadedDir, "assistant.task.download")
    private fun partFile(i: Int) = File(downloadedDir, "assistant.task.download.$i")
    private val totalMemory: Long = runCatching {
        android.app.ActivityManager.MemoryInfo().also { context.getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(it) }.totalMem
    }.getOrDefault(0L)
    private val _approxMb = MutableStateFlow(ModelDownload.APPROX_MB)
    override val approxMb: StateFlow<Int> = _approxMb
    /** The model chosen for this phone, once its checksum was read. */
    private var chosen: Pair<ModelDownload.Choice, String>? = null
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val dm = context.getSystemService(DownloadManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watching: Job? = null
    private val _state = MutableStateFlow<ModelState>(ModelState.Absent)
    override val kind = ModelSource.Kind.APP_DOWNLOAD
    override val state: StateFlow<ModelState> = _state

    init { look() }

    private fun isLiteRt(f: File) = runCatching { f.inputStream().use { s -> ByteArray(ModelFile.LITERTLM_MAGIC.size).also { s.read(it) } } }
        .getOrNull()?.let(ModelFile::isLiteRtLm) == true

    private fun present(): File? = listOf(imported, downloaded).firstOrNull { it.exists() && isLiteRt(it) }

    /** A `.task` from before 0.56.0, which the app no longer runs. */
    private fun outdated(): Boolean = listOf(imported, downloaded).any { it.exists() && !isLiteRt(it) }

    /** What's on the phone, without the network. */
    private fun look() {
        if (_state.value is ModelState.Getting) return
        val f = present()
        when {
            f != null -> _state.value = ModelState.Ready(f, f.length())
            prefs.getLong(KEY_ID, -1L) >= 0 -> watch()
            keptProgress() != null -> _state.value = ModelState.Failed(ModelState.Reason.DOWNLOAD, keptProgress())
            outdated() -> _state.value = ModelState.Failed(ModelState.Reason.OUTDATED)
            else -> _state.value = ModelState.Absent
        }
    }

    /**
     * The sheet opened. On a phone that could take Gemma 3n, read which model is published, so
     * the size it promises is the one it downloads (one request, no data in it). Never at start.
     */
    override fun refresh() {
        look()
        if ((_state.value == ModelState.Absent || _state.value == ModelState.Failed(ModelState.Reason.OUTDATED)) && totalMemory >= ModelDownload.BIG_PHONE_BYTES && chosen == null) scope.launch { choose() }
    }

    override fun download() {
        if (_state.value is ModelState.Getting) return
        start(wifiOnly = true)
    }

    override fun useMobileData() {
        cancelDownload(keep = true)
        start(wifiOnly = false)
    }

    /** The best model for this phone that's published, and its checksum (0.55.0). */
    private fun choose(): Pair<ModelDownload.Choice, String>? {
        chosen?.let { return it }
        for (c in ModelDownload.choices(totalMemory)) {
            val sha = fetchSum(c) ?: continue
            return (c to sha).also { chosen = it; _approxMb.value = c.approxMb }
        }
        return null
    }

    private fun start(wifiOnly: Boolean) {
        _state.value = ModelState.Getting(keptProgress())
        scope.launch {
            val (choice, sha) = choose() ?: run { _state.value = ModelState.Failed(lastFailure, keptProgress()); return@launch }
            downloadedDir.mkdirs()
            // What finished before is kept, unless it belongs to another model or another publish.
            val same = prefs.getString(KEY_CHOICE, null) == choice.name && prefs.getString(KEY_SHA, null) == sha
            val pieces = (if (same) ModelDownload.decode(prefs.getString(KEY_PIECES, null)) else null)
                ?: fetchPieces(choice) ?: ModelDownload.legacyPieces(choice)
            if (!same) { deletePieces(); prefs.edit().remove(KEY_DONE).apply() }
            val done = prefs.getInt(KEY_DONE, 0)
            val from = ModelDownload.resumeAt(pieces) { i, p -> partFile(i).let { f -> f.exists() && if (p.bytes > 0) f.length() == p.bytes else i < done } }
            // Pieces are joined one by one, each deleted once added: about the model's size, plus a piece.
            val left = pieces.drop(from).sumOf { it.bytes.coerceAtLeast(0) }.takeIf { pieces.all { it.bytes > 0 } }
                ?: (choice.approxMb * 1024L * 1024 * (pieces.size - from) / pieces.size)
            val need = left + (pieces.maxOf { it.bytes }.takeIf { it > 0 } ?: (choice.approxMb * 1024L * 1024 / pieces.size)) + ModelFile.MIN_BYTES
            if (downloadedDir.usableSpace < need) {
                _state.value = ModelState.Failed(ModelState.Reason.NO_SPACE, keptProgress()); return@launch
            }
            partial.delete()
            // The old model goes now: it can't run, and its space is needed.
            listOf(imported, downloaded).filter { it.exists() && !isLiteRt(it) }.forEach { it.delete() }
            prefs.edit().putString(KEY_CHOICE, choice.name).putString(KEY_SHA, sha).putBoolean(KEY_METERED, !wifiOnly)
                .putString(KEY_PIECES, ModelDownload.encode(pieces)).putInt(KEY_DONE, from).apply()
            if (from >= pieces.size) finish() else enqueue(pieces, from, wifiOnly)
        }
    }

    /** The published pieces of [choice] (0.57.0), or null before they were published. */
    private fun fetchPieces(choice: ModelDownload.Choice): List<ModelDownload.Piece>? = runCatching {
        val conn = (URL(ModelDownload.piecesUrl(BuildConfig.GITHUB_REPO, choice)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 15_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Cochilo/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode != 200) null
            else ModelDownload.parsePieces(conn.inputStream.bufferedReader().use { it.readText().take(64 * 1024) })
        } finally { conn.disconnect() }
    }.getOrNull()

    private fun pieces(): List<ModelDownload.Piece> =
        ModelDownload.decode(prefs.getString(KEY_PIECES, null)) ?: ModelDownload.legacyPieces(currentChoice())

    /** How much of an interrupted download is on the phone, or null when nothing is. */
    private fun keptProgress(): Float? {
        val done = prefs.getInt(KEY_DONE, 0)
        if (done <= 0 || prefs.getString(KEY_SHA, null) == null) return null
        val p = pieces()
        if (!(0 until done.coerceAtMost(p.size)).all { partFile(it).exists() }) return null
        return ModelDownload.progress(p, done, 0, 0)
    }

    /** Downloads piece [index]; [watch] starts the next one when it ends. */
    private fun enqueue(pieces: List<ModelDownload.Piece>, index: Int, wifiOnly: Boolean) {
        partFile(index).delete() // the download manager won't write over a file
        val request = DownloadManager.Request(Uri.parse(ModelDownload.url(BuildConfig.GITHUB_REPO, pieces[index].name)))
            .setTitle(context.getString(R.string.model_notification_title))
            .setDescription(context.getString(R.string.model_notification_line, android.text.format.Formatter.formatShortFileSize(context, currentChoice().approxMb * 1_000_000L)))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(partFile(index)))
            .setAllowedOverMetered(!wifiOnly)
            .setAllowedOverRoaming(false)
            .addRequestHeader("User-Agent", "Cochilo/${BuildConfig.VERSION_NAME}")
        val id = runCatching { dm.enqueue(request) }.getOrNull()
        if (id == null) { _state.value = ModelState.Failed(ModelState.Reason.DOWNLOAD, keptProgress()); return }
        prefs.edit().putLong(KEY_ID, id).putInt(KEY_PART, index).apply()
        watch()
    }

    private var lastFailure = ModelState.Reason.DOWNLOAD

    /** The published checksum, or null (and why) when it can't be read. */
    private fun fetchSum(choice: ModelDownload.Choice): String? = runCatching {
        val conn = (URL(ModelDownload.sumUrl(BuildConfig.GITHUB_REPO, choice)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 15_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Cochilo/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode == 404) { lastFailure = ModelState.Reason.NOT_PUBLISHED; return null }
            if (conn.responseCode != 200) { lastFailure = ModelState.Reason.DOWNLOAD; return null }
            ModelDownload.parseSum(conn.inputStream.bufferedReader().use { it.readText().take(512) })
                .also { if (it == null) lastFailure = ModelState.Reason.NOT_PUBLISHED }
        } finally { conn.disconnect() }
    }.getOrElse { lastFailure = ModelState.Reason.DOWNLOAD; null }

    /** Follows the download manager's progress until the download ends. */
    private fun watch() {
        if (watching?.isActive == true) return
        watching = scope.launch {
            while (isActive) {
                val id = prefs.getLong(KEY_ID, -1L)
                if (id < 0) return@launch
                val row = dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                    if (!c.moveToFirst()) null else Triple(
                        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                        c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
                        c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) to
                            c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    )
                }
                val pieces = pieces()
                val part = prefs.getInt(KEY_PART, 0)
                // Removed from the Downloads app: an interruption like any other, the finished pieces stay.
                if (row == null) { interrupted(part, ModelState.Reason.DOWNLOAD); return@launch }
                val (status, reason, sizes) = row
                // Progress runs across all the pieces.
                val progress = ModelDownload.progress(pieces, part, sizes.first, sizes.second)
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        prefs.edit().remove(KEY_ID).apply()
                        val piece = pieces.getOrNull(part)
                        // A download from before 0.57.0 may have landed in [partial] directly.
                        val file = if (!partFile(part).exists() && pieces.size == 1 && partial.exists()) partial else partFile(part)
                        if (piece == null || !ModelDownload.pieceIsGood(file, piece)) {
                            // A damaged piece is fetched again; the others stay.
                            interrupted(part, ModelState.Reason.CHECK); return@launch
                        }
                        prefs.edit().putInt(KEY_DONE, part + 1).apply()
                        if (part + 1 < pieces.size) enqueue(pieces, part + 1, wifiOnly = !prefs.getBoolean(KEY_METERED, false))
                        else finish()
                        return@launch
                    }
                    DownloadManager.STATUS_FAILED -> {
                        interrupted(part, if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) ModelState.Reason.NO_SPACE else ModelState.Reason.DOWNLOAD)
                        return@launch
                    }
                    DownloadManager.STATUS_PAUSED -> _state.value = ModelState.Getting(progress, waitingForWifi = reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI)
                    else -> _state.value = ModelState.Getting(progress)
                }
                delay(1_000)
            }
        }
    }

    private fun currentChoice(): ModelDownload.Choice =
        prefs.getString(KEY_CHOICE, null)?.let { n -> ModelDownload.Choice.entries.firstOrNull { it.name == n } } ?: ModelDownload.Choice.GEMMA_1B

    /** The piece in flight is lost, the finished ones stay: getting it again carries on (0.57.0). */
    private fun interrupted(part: Int, reason: ModelState.Reason) {
        prefs.edit().remove(KEY_ID).remove(KEY_PART).apply()
        partFile(part).delete()
        _state.value = ModelState.Failed(reason, keptProgress())
    }

    private fun finish() {
        val sha = prefs.getString(KEY_SHA, null)
        val pieces = pieces()
        forget()
        // The pieces are joined into one file, each deleted as it's added, then the whole is checked.
        // (A download from before 0.57.0 may have landed in [partial] directly.)
        if (partFile(0).exists() || !partial.exists()) {
            partial.delete()
            val joined = runCatching {
                if (pieces.size == 1) check(partFile(0).renameTo(partial))
                else partial.outputStream().use { out -> pieces.indices.forEach { i -> partFile(i).inputStream().use { it.copyTo(out, 1 shl 20) }; partFile(i).delete() } }
            }.isSuccess
            if (!joined) { deletePieces(); partial.delete(); _state.value = ModelState.Failed(ModelState.Reason.NO_SPACE); return }
        }
        val problem = if (sha == null || !partial.exists()) ModelState.Reason.DOWNLOAD else ModelDownload.verify(partial, sha)
        if (problem != null) {
            partial.delete()
            // A bad checksum may mean the published file changed: read it again next time.
            if (problem == ModelState.Reason.CHECK) prefs.edit().remove(KEY_SHA).apply()
            _state.value = ModelState.Failed(problem)
            return
        }
        downloaded.delete()
        _state.value = if (partial.renameTo(downloaded)) ModelState.Ready(downloaded, downloaded.length()) else ModelState.Failed(ModelState.Reason.OTHER)
    }

    private fun forget() = prefs.edit().remove(KEY_ID).remove(KEY_PART).remove(KEY_DONE).remove(KEY_PIECES).apply()

    /** Every piece file there is, whatever model it was for. */
    private fun deletePieces() {
        downloadedDir.listFiles { f -> f.name.startsWith("assistant.task.download.") }?.forEach { it.delete() }
    }

    /** Stops the piece in flight; [keep]: the finished pieces stay for later. */
    private fun cancelDownload(keep: Boolean = false) {
        val id = prefs.getLong(KEY_ID, -1L)
        val part = prefs.getInt(KEY_PART, 0)
        // Wait for the old watcher to stop, so it doesn't read the removed row as an interruption.
        watching?.cancel(); watching = null
        prefs.edit().remove(KEY_ID).remove(KEY_PART).apply()
        if (id >= 0) runCatching { dm.remove(id) }
        if (keep) { partFile(part).delete(); return }
        forget()
        partial.delete()
        deletePieces()
    }

    override suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        cancelDownload()
        val resolver = context.contentResolver
        val size = runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()?.takeIf { it > 0 }
        val head = runCatching { resolver.openInputStream(uri)?.use { s -> ByteArray(16).also { s.read(it) } } }.getOrNull() ?: ByteArray(0)
        ModelFile.check(head, size ?: Long.MAX_VALUE)?.let { _state.value = ModelState.Failed(it); return@withContext }
        if (size != null && context.filesDir.usableSpace < size + ModelFile.MIN_BYTES) {
            _state.value = ModelState.Failed(ModelState.Reason.NO_SPACE); return@withContext
        }
        _state.value = ModelState.Getting(if (size != null) 0f else null, copying = true)
        val copied = runCatching {
            resolver.openInputStream(uri)!!.use { input ->
                ModelFile.copy(input, imported) { done -> if (size != null) _state.value = ModelState.Getting((done.toFloat() / size).coerceIn(0f, 1f), copying = true) }
            }
        }
        _state.value = copied.fold(
            onSuccess = { bytes ->
                if (bytes < ModelFile.MIN_BYTES) { imported.delete(); ModelState.Failed(ModelState.Reason.TOO_SMALL) }
                else { downloaded.delete(); ModelState.Ready(imported, bytes) }
            },
            onFailure = { ModelState.Failed(ModelState.Reason.OTHER) },
        )
    }

    override fun remove() {
        cancelDownload()
        imported.delete()
        downloaded.delete()
        _state.value = ModelState.Absent
    }
}
