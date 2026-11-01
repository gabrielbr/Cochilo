package br.gabriel.cochilo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.R
import br.gabriel.cochilo.backup.Backup
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

    fun importJson(uri: Uri) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            val text = runCatching {
                ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }.getOrElse { return@withContext str(R.string.file_failed) }
            val file = runCatching { Backup.fromJson(text) }.getOrElse { return@withContext str(R.string.import_failed) }
            val result = Backup.merge(events.all(), file.events)
            val changed = result.merged.filter { m -> file.events.any { it.id == m.id } }
            events.upsertAll(changed)
            if (babies.get() == null && file.baby != null) babies.upsert(file.baby)
            str(R.string.imported, result.added, result.updated)
        }
        refreshNow()
        _messages.send(UiMessage(msg))
    }
}

sealed interface BabyState {
    data object Loading : BabyState
    data object Missing : BabyState
    data class Ready(val baby: Baby) : BabyState
}
