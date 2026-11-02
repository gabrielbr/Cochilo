package br.gabriel.cochilo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.gabriel.cochilo.CochiloApp
import br.gabriel.cochilo.R
import br.gabriel.cochilo.backup.Backup
import br.gabriel.cochilo.backup.MergeResult
import br.gabriel.cochilo.backup.NapperImport
import br.gabriel.cochilo.backup.NapperLabels
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
    private val entries = br.gabriel.cochilo.data.Entries(events)

    val log: StateFlow<List<Event>> = events.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Loading until the first read finishes; then the baby, or Missing. */
    val baby: StateFlow<BabyState> = babies.observe()
        .map { if (it == null) BabyState.Missing else BabyState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BabyState.Loading)

    val theme: StateFlow<ThemeMode> = ctx.prefs.theme
    val liveNotification: StateFlow<Boolean> = ctx.prefs.liveNotification

    fun setLiveNotification(on: Boolean) = ctx.prefs.setLiveNotification(on)

    /** Ask for the notification permission once, the first time the app has a baby (Android 13+). */
    fun shouldAskNotifications(): Boolean = !ctx.prefs.askedNotificationPermission
    fun askedNotifications() { ctx.prefs.askedNotificationPermission = true }
    fun notificationsAnswered() = ctx.refreshNotifications()

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

    // With no arguments, read the text as it is: Context.getString(id, *emptyArray()) still
    // formats, and throws on a template like "Left %1${'$'}d min" that is filled in later.
    private fun str(id: Int, vararg args: Any) = if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)

    fun setTheme(mode: ThemeMode) = ctx.prefs.setTheme(mode)

    fun saveBaby(baby: Baby) = viewModelScope.launch { babies.upsert(baby) }

    fun startSleep() = viewModelScope.launch {
        entries.startSleep()
        refreshNow()
    }

    fun wakeUp() = viewModelScope.launch {
        entries.wakeUp()
        refreshNow()
    }

    fun finish(e: Event) = viewModelScope.launch {
        entries.finish(e.id)
        refreshNow()
    }

    /**
     * The slide shortcut: logs [type] with [kind] now, with no sheet. A timer type starts running.
     * The snackbar says what was logged and offers Undo, so a slip costs one tap to take back.
     */
    fun quickLog(type: EventType, kind: String?) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val e = br.gabriel.cochilo.ui.screens.newEvent(type, now).copy(kind = kind, updatedAt = now)
        events.upsert(e)
        refreshNow()
        val name = Format(ctx, zone).let { f -> listOfNotNull(f.typeName(type), f.kindName(kind)).joinToString(" · ") }
        _messages.send(UiMessage(str(if (type.hasDuration) R.string.quick_started else R.string.quick_logged, name), str(R.string.undo)) {
            viewModelScope.launch { events.delete(e); refreshNow() }
        })
    }

    /** During a breastfeed: end this side now and start the other, in one move. */
    fun switchSide(e: Event) = viewModelScope.launch {
        val (_, next) = entries.switchSide(e.id) ?: return@launch
        refreshNow()
        _messages.send(UiMessage(str(R.string.quick_switched, Format(ctx, zone).kindName(next.kind) ?: next.kind.orEmpty()), str(R.string.undo)) {
            viewModelScope.launch { events.delete(next); events.upsert(e); refreshNow() }
        })
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

    /** Either import row takes either file: a Cochilo backup (JSON) or Napper's logs export (CSV). */
    fun importFile(uri: Uri) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            val text = runCatching {
                ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }.getOrElse { return@withContext str(R.string.file_failed) }
            if (NapperImport.looksLikeNapper(text)) {
                val parsed = runCatching { NapperImport.parse(text, napperLabels()) }
                    .getOrElse { return@withContext str(R.string.import_napper_failed) }
                val result = mergeIn(parsed.events)
                return@withContext str(R.string.imported_napper, result.added, result.updated)
            }
            val file = runCatching { Backup.fromJson(text) }.getOrElse { return@withContext str(R.string.import_failed) }
            val result = mergeIn(file.events)
            if (babies.get() == null && file.baby != null) babies.upsert(file.baby)
            str(R.string.imported, result.added, result.updated)
        }
        refreshNow()
        _messages.send(UiMessage(msg))
    }

    /** Import never deletes: only the added and newer entries are written. */
    private suspend fun mergeIn(incoming: List<br.gabriel.cochilo.data.Event>): MergeResult {
        val result = Backup.merge(events.all(), incoming)
        val ids = incoming.mapTo(HashSet()) { it.id }
        events.upsertAll(result.merged.filter { it.id in ids })
        return result
    }

    private fun napperLabels() = NapperLabels(
        heldOrWorn = str(R.string.napper_held),
        swing = str(R.string.napper_swing),
        inBed = str(R.string.napper_in_bed),
        nursing = str(R.string.napper_nursing),
        breastMilk = str(R.string.napper_breast_milk),
        formula = str(R.string.napper_formula),
        medicine = str(R.string.napper_medicine),
        sides = { left, right -> str(R.string.napper_sides, left, right) },
    )
}

sealed interface BabyState {
    data object Loading : BabyState
    data object Missing : BabyState
    data class Ready(val baby: Baby) : BabyState
}
