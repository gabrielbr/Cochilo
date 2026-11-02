package br.gabriel.cochilo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.R
import br.gabriel.cochilo.backup.Backup
import br.gabriel.cochilo.backup.FileKind
import br.gabriel.cochilo.backup.NapperImport
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

/** A one-line message for the snackbar, optionally with one action. */
data class UiMessage(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app as CochiloApp
    private val events = ctx.db.events()
    private val babies = ctx.db.baby()

    val log: StateFlow<List<Event>> = events.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Loading until the first read finishes; then the baby, or Missing. */
    val baby: StateFlow<BabyState> = babies.observe()
        .map { if (it == null) BabyState.Missing else BabyState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BabyState.Loading)

    val theme: StateFlow<ThemeMode> = ctx.prefs.theme

    /** Ticks every 20 s so timers and "awake for" stay current without a per-second redraw. */
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val zone: ZoneId get() = ZoneId.systemDefault()

    init {
        viewModelScope.launch {
            while (true) {
                _now.value = System.currentTimeMillis()
                delay(20_000)
            }
        }
    }

    fun refreshNow() { _now.value = System.currentTimeMillis() }

    private fun str(id: Int, vararg args: Any) = ctx.getString(id, *args)

    fun setTheme(mode: ThemeMode) = ctx.prefs.setTheme(mode)

    fun saveBaby(baby: Baby) = viewModelScope.launch { babies.upsert(baby) }

    fun startSleep() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        // Only one sleep runs at a time.
        if (log.value.none { it.type == EventType.SLEEP && it.end == null }) {
            events.upsert(Event(type = EventType.SLEEP, start = now, updatedAt = now))
        }
        refreshNow()
    }

    fun wakeUp() = viewModelScope.launch {
        val now = System.currentTimeMillis()
        log.value.filter { it.type == EventType.SLEEP && it.end == null }.forEach {
            events.upsert(it.copy(end = now, updatedAt = now))
        }
        refreshNow()
    }

    fun finish(e: Event) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        events.upsert(e.copy(end = now, updatedAt = now))
        refreshNow()
    }

    fun save(e: Event) = viewModelScope.launch {
        events.upsert(e.copy(updatedAt = System.currentTimeMillis()))
        refreshNow()
    }

    fun delete(e: Event) = viewModelScope.launch {
        events.delete(e)
        _messages.send(UiMessage(str(R.string.deleted), str(R.string.undo)) {
            viewModelScope.launch { events.upsert(e) }
        })
    }

    fun exportJson(uri: Uri) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val text = Backup.toJson(babies.get(), events.all(), System.currentTimeMillis())
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            }.isSuccess
        }
        _messages.send(UiMessage(str(if (ok) R.string.exported else R.string.file_failed)))
    }

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val text = Backup.toCsv(events.all(), zone)
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            }.isSuccess
        }
        _messages.send(UiMessage(str(if (ok) R.string.exported else R.string.file_failed)))
    }

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    /** A file someone shared or opened, read and waiting for "Import" in the sheet. */
    val pendingImport: StateFlow<PendingImport?> = _pendingImport

    private val napperLabels = object : NapperImport.Labels {
        override fun fellAsleep(code: String): String? = when (code) {
            "SWING" -> str(R.string.asleep_swing)
            "WORN_OR_HELD" -> str(R.string.asleep_held)
            "IN_BED" -> str(R.string.asleep_bed)
            "NURSING" -> str(R.string.asleep_nursing)
            else -> null
        }
        override fun pauses(count: Int, minutes: Int?) =
            if (minutes != null && minutes > 0) str(R.string.pauses_note_min, count, minutes) else str(R.string.pauses_note, count)
    }

    /**
     * Reads shared or opened files (a Cochilo backup, or Napper's logs CSV) and shows what they hold.
     * Nothing is saved until [confirmImport].
     */
    fun prepareImport(uris: List<Uri>) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            var source: ImportSource? = null
            val found = mutableListOf<Event>()
            var baby: Baby? = null
            var skipped = 0
            var problem: Int? = null
            for (uri in uris) {
                val text = runCatching {
                    ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                }.getOrNull()
                if (text == null) { problem = R.string.file_failed; continue }
                when (NapperImport.detect(text)) {
                    FileKind.COCHILO_BACKUP -> runCatching { Backup.fromJson(text) }
                        .onSuccess { found += it.events; baby = baby ?: it.baby; source = source ?: ImportSource.BACKUP }
                        .onFailure { problem = R.string.import_failed }
                    FileKind.NAPPER_LOGS -> {
                        val r = NapperImport.parseLogs(text, napperLabels)
                        found += r.events; skipped += r.skipped; source = ImportSource.NAPPER
                    }
                    FileKind.NAPPER_MEASUREMENTS -> if (problem == null) problem = R.string.import_measurements
                    FileKind.UNKNOWN -> problem = R.string.import_failed
                }
            }
            if (found.isEmpty()) Result.failure(IllegalStateException(str(problem ?: R.string.import_nothing)))
            else Result.success(PendingImport(source ?: ImportSource.BACKUP, found.distinctBy { it.id }, baby, skipped))
        }
        result.onSuccess { _pendingImport.value = it }.onFailure { _messages.send(UiMessage(it.message ?: str(R.string.import_failed))) }
    }

    fun cancelImport() { _pendingImport.value = null }

    /** Merges the pending file: adds what's new, keeps the newest edit, never deletes. */
    fun confirmImport() = viewModelScope.launch {
        val pending = _pendingImport.value ?: return@launch
        _pendingImport.value = null
        val msg = withContext(Dispatchers.IO) {
            val result = Backup.merge(events.all(), pending.events)
            val ids = pending.events.map { it.id }.toSet()
            events.upsertAll(result.merged.filter { it.id in ids })
            if (babies.get() == null && pending.baby != null) babies.upsert(pending.baby)
            str(R.string.imported, result.added, result.updated)
        }
        refreshNow()
        _messages.send(UiMessage(msg))
    }
}

enum class ImportSource { BACKUP, NAPPER }

data class PendingImport(val source: ImportSource, val events: List<Event>, val baby: Baby?, val skipped: Int) {
    val sleeps get() = events.count { it.type == EventType.SLEEP }
    val feeds get() = events.count { it.type == EventType.FEED }
    val diapers get() = events.count { it.type == EventType.DIAPER }
    val medicine get() = events.count { it.type == EventType.MEDICINE }
    val other get() = events.size - sleeps - feeds - diapers - medicine
    val from get() = events.minOf { it.start }
    val to get() = events.maxOf { it.end ?: it.start }
}

sealed interface BabyState {
    data object Loading : BabyState
    data object Missing : BabyState
    data class Ready(val baby: Baby) : BabyState
}
