package br.gabriel.cochilo.ui

import br.gabriel.cochilo.domain.isNightSleep
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
import kotlinx.coroutines.sync.withLock
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

    /** Window reminders (docs/notifications.md): 30 and 15 minutes before the next window. */
    val reminders: StateFlow<Boolean> = ctx.prefs.reminders
    fun setReminders(on: Boolean) = ctx.prefs.setReminders(on)
    val reminderLeads: StateFlow<Set<Int>> = ctx.prefs.reminderLeads
    fun setReminderLeads(leads: Set<Int>) = ctx.prefs.setReminderLeads(leads)

    /** The fabric that wraps the Today card (remembered: the picker opens on it). */
    val fabric: StateFlow<String?> = ctx.prefs.fabric

    fun setFabric(key: String) = ctx.prefs.setFabric(key)

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
        ctx.prefs.firstSeen // starts the first-week clock for Tatá's suggestions
        // Details after: something ended from the widget, the notification or the tile? Ask now.
        viewModelScope.launch {
            br.gabriel.cochilo.domain.Details.pending(events.all(), System.currentTimeMillis(), ctx.prefs.detailsAsked.toSet())
                ?.let { _details.value = it }
        }
        viewModelScope.launch {
            while (true) {
                _now.value = System.currentTimeMillis()
                delay(20_000)
            }
        }
    }

    fun refreshNow() { _now.value = System.currentTimeMillis() }

    // Celebrations (docs/celebrations.md): after the tap, the pure rules decide; the root shows it.
    private val _celebration = MutableStateFlow<br.gabriel.cochilo.domain.Celebration?>(null)
    val celebration: StateFlow<br.gabriel.cochilo.domain.Celebration?> = _celebration
    val celebrations: StateFlow<Boolean> = ctx.prefs.celebrations
    val nightLight: StateFlow<Boolean> = ctx.prefs.nightLight
    val rituals: StateFlow<Map<String, String?>> = ctx.prefs.rituals
    fun setRitual(kind: String, steps: List<String>) = ctx.prefs.setRitual(kind, br.gabriel.cochilo.domain.Ritual.encode(steps))
    fun setNightLight(on: Boolean) = ctx.prefs.setNightLight(on)
    fun setCelebrations(on: Boolean) = ctx.prefs.setCelebrations(on)
    fun celebrationSeen() { _celebration.value = null }

    private suspend fun celebrate(trigger: br.gabriel.cochilo.domain.Trigger) {
        if (!ctx.prefs.celebrations.value) return
        val now = System.currentTimeMillis()
        val c = br.gabriel.cochilo.domain.Celebrations.after(trigger, events.all(), zone, now, ctx.prefs.celebrationsShown) ?: return
        ctx.prefs.celebrationsShown = ctx.prefs.celebrationsShown + (c.moment to java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
        _celebration.value = c
    }

    /** The prediction as it stood before a tap (for "on time" and "a full nap"). */
    private suspend fun predictionNow(now: Long): br.gabriel.cochilo.domain.Predictor.Result {
        val b = babies.get() ?: return br.gabriel.cochilo.domain.Predictor.Result.NeedData
        return br.gabriel.cochilo.domain.Predictor(zone).predict(events.all(), br.gabriel.cochilo.domain.ageWeeks(b.birthDate, b.dueDate, now), now)
    }

    // With no arguments, read the text as it is: Context.getString(id, *emptyArray()) still
    // formats, and throws on a template like "Left %1${'$'}d min" that is filled in later.
    private fun str(id: Int, vararg args: Any) = if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)

    fun setTheme(mode: ThemeMode) = ctx.prefs.setTheme(mode)

    fun saveBaby(baby: Baby) = viewModelScope.launch { babies.upsert(baby) }

    /** Start sleep now, or [minutesAgo] minutes ago (the Earlier chips). */
    fun startSleep(minutesAgo: Int = 0) = viewModelScope.launch {
        val before = predictionNow(System.currentTimeMillis())
        val sleep = entries.startSleep(System.currentTimeMillis() - minutesAgo * 60_000L)
        refreshNow()
        if (sleep != null) celebrate(br.gabriel.cochilo.domain.Trigger.StartedSleep(sleep, before))
    }

    /** Woke up now, or [minutesAgo] minutes ago (the Earlier chips). */
    fun wakeUp(minutesAgo: Int = 0) = viewModelScope.launch {
        val before = predictionNow(System.currentTimeMillis())
        val running = events.all().filter { it.type == EventType.SLEEP && it.end == null }.map { it.id }
        entries.wakeUp(System.currentTimeMillis() - minutesAgo * 60_000L)
        refreshNow()
        events.all().filter { it.id in running }.maxByOrNull { it.start }?.let {
            // A night waking isn't the end of the night (docs/nights.md): no "where did she sleep?"
            // in the dark; the night's place is asked at Up for the day.
            if (!it.isNightSleep(zone)) askDetails(it)
            celebrate(br.gabriel.cochilo.domain.Trigger.WokeUp(it, before))
        }
    }

    /** Up for the day (docs/nights.md): the night ends now, or [minutesAgo] minutes ago. */
    fun upForDay(minutesAgo: Int = 0) = viewModelScope.launch {
        val before = predictionNow(System.currentTimeMillis())
        val running = events.all().firstOrNull { it.type == EventType.SLEEP && it.end == null }
        entries.upForDay(System.currentTimeMillis() - minutesAgo * 60_000L)
        refreshNow()
        // The night's first sleep carries where she slept (one question for the whole night).
        val sleeps = events.all().filter { it.type == EventType.SLEEP }
        sleeps.maxByOrNull { it.start }?.let { last -> br.gabriel.cochilo.domain.Nights.containing(sleeps, last, zone).firstOrNull()?.let { askDetails(it) } }
        running?.let { r -> events.all().firstOrNull { it.id == r.id }?.let { celebrate(br.gabriel.cochilo.domain.Trigger.WokeUp(it, before)) } }
    }

    /** Nap or the night, one tap on Today's sleep card. */
    fun setNight(e: Event, night: Boolean) = viewModelScope.launch {
        entries.setNight(e.id, night)
        refreshNow()
    }

    // Details after (docs/flows.md): the one question an entry still has once it ends.
    private val _details = MutableStateFlow<Event?>(null)
    val details: StateFlow<Event?> = _details

    private fun askDetails(e: Event) {
        if (br.gabriel.cochilo.domain.Details.askAfter(e) == null) return
        ctx.prefs.detailsAsked = ctx.prefs.detailsAsked + e.id
        _details.value = e
    }

    /** The answer: a kind (place, breast, daylight) or an amount. */
    fun answerDetails(e: Event, kind: String? = null, amountMl: Int? = null) = viewModelScope.launch {
        _details.value = null
        val current = events.byId(e.id) ?: return@launch
        events.upsert(current.copy(kind = kind ?: current.kind, amountMl = amountMl ?: current.amountMl, updatedAt = System.currentTimeMillis()))
        refreshNow()
        if (e.type == EventType.SLEEP) maybeTicks(kind)
        // A bottle picked after a feed still needs its amount: ask that next.
        events.byId(e.id)?.let { askDetailsAgain(it) }
    }

    private fun askDetailsAgain(e: Event) {
        if (br.gabriel.cochilo.domain.Details.askAfter(e) != null) _details.value = e
    }

    fun skipDetails() { _details.value = null }

    fun finish(e: Event) = viewModelScope.launch {
        entries.finish(e.id)
        refreshNow()
        events.byId(e.id)?.let { askDetails(it); celebrate(br.gabriel.cochilo.domain.Trigger.Logged(it)) }
    }

    /**
     * The slide shortcut: logs [type] with [kind] now, with no sheet. A timer type starts running.
     * The snackbar says what was logged and offers Undo, so a slip costs one tap to take back.
     */
    fun quickLog(type: EventType, kind: String?, amountMl: Int? = null, note: String? = null) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val e = br.gabriel.cochilo.ui.screens.newEvent(type, now).copy(kind = kind, amountMl = amountMl, note = note, updatedAt = now)
        events.upsert(e)
        refreshNow()
        val name = Format(ctx, zone).let { f ->
            listOfNotNull(f.typeName(type), f.kindName(kind), amountMl?.let { str(R.string.ml, it) }, note).joinToString(" · ")
        }
        _messages.send(UiMessage(str(if (type.hasDuration) R.string.quick_started else R.string.quick_logged, name), str(R.string.undo)) {
            viewModelScope.launch { events.delete(e); refreshNow() }
        })
        celebrate(br.gabriel.cochilo.domain.Trigger.Logged(e))
    }

    /** During a breastfeed: end this side now and start the other, in one move. */
    fun switchSide(e: Event) = viewModelScope.launch {
        val (_, next) = entries.switchSide(e.id) ?: return@launch
        refreshNow()
        _messages.send(UiMessage(str(R.string.quick_switched, Format(ctx, zone).kindName(next.kind) ?: next.kind.orEmpty()), str(R.string.undo)) {
            viewModelScope.launch { events.delete(next); events.upsert(e); refreshNow() }
        })
    }

    // The safe-babywearing check (T.I.C.K.S., docs/places.md): once, the first time "sling" is picked.
    private val _ticks = MutableStateFlow(false)
    val ticks: StateFlow<Boolean> = _ticks
    fun ticksSeen() { _ticks.value = false }
    private fun maybeTicks(kind: String?) {
        if (kind == br.gabriel.cochilo.data.Kinds.SLING && !ctx.prefs.ticksShown) {
            ctx.prefs.ticksShown = true
            _ticks.value = true
        }
    }

    /** Where the running sleep is happening (the panel's place chips), one tap. */
    fun setPlace(e: Event, kind: String) = viewModelScope.launch {
        events.upsert(e.copy(kind = kind, updatedAt = System.currentTimeMillis()))
        refreshNow()
        maybeTicks(kind)
    }

    fun save(e: Event) = viewModelScope.launch {
        if (e.type == EventType.SLEEP) maybeTicks(e.kind)
        events.upsert(e.copy(updatedAt = System.currentTimeMillis()))
        refreshNow()
        celebrate(br.gabriel.cochilo.domain.Trigger.Logged(e))
    }

    /** Drag to adjust: [e] now ended at [end]; Undo puts it back. */
    fun adjustEnd(e: Event, end: Long) = viewModelScope.launch {
        events.upsert(e.copy(end = end, updatedAt = System.currentTimeMillis()))
        refreshNow()
        _messages.send(UiMessage(str(R.string.adjusted, Format(ctx, zone).time(end)), str(R.string.undo)) {
            viewModelScope.launch { events.upsert(e.copy(updatedAt = System.currentTimeMillis())); refreshNow() }
        })
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
                val plain = Backup.toJson(babies.get(), events.all(), System.currentTimeMillis())
                // With a passphrase set, the file is sealed (docs/backup.md); otherwise plain JSON.
                val text = ctx.prefs.backupPassphrase.value?.let { br.gabriel.cochilo.backup.Sealed.seal(plain, it) } ?: plain
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            }.isSuccess
        }
        _messages.send(UiMessage(str(if (ok) R.string.exported else R.string.file_failed)))
    }

    /** The one-page PDF for the paediatrician (docs/report.md), written where the parent chose. */
    fun exportReport(uri: Uri) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val m = br.gabriel.cochilo.report.Report.build(ctx, babies.get(), events.all(), System.currentTimeMillis(), zone)
                ctx.contentResolver.openOutputStream(uri, "wt")!!.use { br.gabriel.cochilo.report.Report.write(ctx, m, it) }
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
            importContent(text)
        } ?: return@launch
        refreshNow()
        _messages.send(UiMessage(msg))
    }

    /** Text shared to Cochilo (0.41.0): a Napper CSV or a backup pasted as text, imported like a file. */
    fun importText(text: String) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) { importContent(text) } ?: return@launch
        refreshNow()
        _messages.send(UiMessage(msg))
    }

    /** What any import reads: a locked backup, a Napper CSV or a Cochilo backup. Null when it waits for a passphrase. */
    private suspend fun importContent(text: String): String? {
        if (br.gabriel.cochilo.backup.Sealed.isSealed(text)) {
            // Try the passphrase this phone keeps; otherwise ask for it (UnlockSheet).
            val saved = ctx.prefs.backupPassphrase.value
            val opened = saved?.let { p -> runCatching { br.gabriel.cochilo.backup.Sealed.open(text, p) }.getOrNull() }
                ?: run { _sealed.value = SealedImport(text, wrong = false); return null }
            return importBackup(opened)
        }
        if (NapperImport.looksLikeNapper(text)) {
            val parsed = runCatching { NapperImport.parse(text, napperLabels()) }
                .getOrElse { return str(R.string.import_napper_failed) }
            val result = mergeIn(parsed.events)
            return str(R.string.imported_napper, result.added, result.updated)
        }
        return importBackup(text)
    }

    private suspend fun importBackup(text: String): String {
        val file = runCatching { Backup.fromJson(text) }.getOrElse { return str(R.string.import_failed) }
        val result = mergeIn(file.events)
        if (babies.get() == null && file.baby != null) babies.upsert(file.baby)
        return str(R.string.imported, result.added, result.updated)
    }

    // Tatá's suggestions (docs/tata-slings/in-app-marketing.md): the catalog ships in the app.
    val catalog: br.gabriel.cochilo.store.Catalog = runCatching {
        br.gabriel.cochilo.store.Catalog.parse(ctx.assets.open("tata/catalog.json").bufferedReader().use { it.readText() })
    }.getOrDefault(br.gabriel.cochilo.store.Catalog.EMPTY)
    val tataSuggestions: StateFlow<Boolean> = ctx.prefs.tataSuggestions
    fun setTataSuggestions(on: Boolean) = ctx.prefs.setTataSuggestions(on)
    private val _suggestionTick = MutableStateFlow(0)
    /** Bumps when a note is shown or dismissed, so the screen asks again. */
    val suggestionTick: StateFlow<Int> = _suggestionTick

    /** The note due now, by the rules (Suggestions.pick), or null. */
    fun suggestion(log: List<Event>, ageWeeks: Int, now: Long, sleeping: Boolean): br.gabriel.cochilo.domain.Suggestions.Suggestion? {
        val known = br.gabriel.cochilo.domain.Suggestions.Moment.entries.associateBy { it.name }
        return br.gabriel.cochilo.domain.Suggestions.pick(
            br.gabriel.cochilo.domain.Suggestions.Input(
                log, ageWeeks, now, zone, sleeping, ctx.prefs.firstSeen, tataSuggestions.value,
                catalog.approved, catalog.productOf,
                ctx.prefs.suggestionsDismissed.mapNotNull { known[it] }.toSet(),
                ctx.prefs.suggestionsShown.mapNotNull { (k, v) -> known[k]?.let { it to v } }.toMap(),
            ),
        )
    }

    /** Tatá's note for the baby's age (§3.1), or null. Content first: it takes the slot before a suggestion. */
    fun ageNote(ageWeeks: Int, now: Long, sleeping: Boolean): br.gabriel.cochilo.domain.AgeNotes.Band? {
        val known = br.gabriel.cochilo.domain.AgeNotes.Band.entries.associateBy { it.name }
        return br.gabriel.cochilo.domain.AgeNotes.pick(
            ageWeeks, now, zone, sleeping, catalog.approvedNotes, ctx.prefs.ageNotesClosed.mapNotNull { known[it] }.toSet(),
        )
    }

    fun closeAgeNote(b: br.gabriel.cochilo.domain.AgeNotes.Band) {
        ctx.prefs.ageNotesClosed = ctx.prefs.ageNotesClosed + b.name
        _suggestionTick.value++
    }

    fun suggestionShown(m: br.gabriel.cochilo.domain.Suggestions.Moment) {
        if (m.name in ctx.prefs.suggestionsShown) return
        ctx.prefs.suggestionsShown = ctx.prefs.suggestionsShown + (m.name to System.currentTimeMillis())
        _suggestionTick.value++
    }

    fun dismissSuggestion(m: br.gabriel.cochilo.domain.Suggestions.Moment) {
        ctx.prefs.suggestionsDismissed = ctx.prefs.suggestionsDismissed + m.name
        _suggestionTick.value++
    }

    // Ask Cochilo, phase 2 (docs/assistant.md): the optional on-device model.
    private val modelSource = br.gabriel.cochilo.ai.ModelSources.create(ctx)
    val modelKind: br.gabriel.cochilo.ai.ModelSource.Kind = modelSource.kind
    val modelState: StateFlow<br.gabriel.cochilo.ai.ModelState> = modelSource.state
    private var model: br.gabriel.cochilo.ai.LocalModel? = null
    private var modelFile: java.io.File? = null

    fun refreshModel() = modelSource.refresh()
    fun downloadModel() = modelSource.download()
    fun modelOnMobileData() = modelSource.useMobileData()
    fun importModel(uri: Uri) = viewModelScope.launch { modelSource.import(uri) }
    fun removeModel() = viewModelScope.launch {
        // Never close the engine while it's writing an answer.
        modelLock.withLock { model?.close(); model = null; modelFile = null }
        modelSource.remove()
    }

    // Ask's chats (0.40.0): saved on the phone, one per topic.
    val chats: StateFlow<List<br.gabriel.cochilo.data.Chat>> = ctx.chats.chats

    fun saveChat(id: String, items: List<br.gabriel.cochilo.data.ChatItem>) {
        val old = ctx.chats.chats.value.firstOrNull { it.id == id }
        val now = System.currentTimeMillis()
        ctx.chats.save(br.gabriel.cochilo.data.Chat(id, old?.created ?: now, now, br.gabriel.cochilo.data.ChatStore.title(items.firstOrNull()?.question ?: ""), items))
    }

    fun deleteChat(id: String) = ctx.chats.delete(id)
    fun restoreChat(chat: br.gabriel.cochilo.data.Chat) = ctx.chats.restore(chat)

    /**
     * The model took the app down last time (0.39.1): a native crash can't be caught, so the flag
     * set before writing is still on at the next start. The model is paused until she tries again.
     */
    private val _modelPaused = MutableStateFlow(ctx.prefs.modelWasRunning)
    val modelPaused: StateFlow<Boolean> = _modelPaused
    fun resumeModel() { ctx.prefs.setModelRunning(false); _modelPaused.value = false }

    /** The model's answer to a grounded question, trimmed to fit its window, or null when there's no model or it failed. */
    suspend fun generate(ask: br.gabriel.cochilo.ui.screens.ModelAsk): br.gabriel.cochilo.ui.screens.ModelAnswer? = modelLock.withLock { generateLocked(ask) }

    /** One answer at a time: the engine isn't safe to run twice at once. */
    private val modelLock = kotlinx.coroutines.sync.Mutex()

    private suspend fun generateLocked(ask: br.gabriel.cochilo.ui.screens.ModelAsk): br.gabriel.cochilo.ui.screens.ModelAnswer? = withContext(Dispatchers.Default) {
        if (_modelPaused.value) return@withContext null
        val ready = modelState.value as? br.gabriel.cochilo.ai.ModelState.Ready ?: return@withContext null
        ctx.prefs.setModelRunning(true)
        try {
            runCatching {
                val m = model?.takeIf { modelFile == ready.file }
                    ?: br.gabriel.cochilo.ai.MediaPipeModel(ctx, ready.file).also { model?.close(); model = it; modelFile = ready.file }
                val budget = br.gabriel.cochilo.ai.MediaPipeModel.MAX_TOKENS - br.gabriel.cochilo.ai.MediaPipeModel.REPLY_TOKENS
                val prompt = br.gabriel.cochilo.domain.assistant.Grounding.fit(ask.question, ask.facts, ask.passages, ask.portuguese) { m.tokens(it) <= budget }
                    ?: return@runCatching null
                br.gabriel.cochilo.ui.screens.ModelAnswer(prompt, m.generate(prompt))
            }.getOrNull()
        } finally {
            ctx.prefs.setModelRunning(false)
        }
    }

    override fun onCleared() {
        model?.close()
        super.onCleared()
    }

    /** A sealed backup waiting for its passphrase; [wrong] after a passphrase that didn't open it. */
    data class SealedImport(val text: String, val wrong: Boolean)

    private val _sealed = MutableStateFlow<SealedImport?>(null)
    val sealedImport: StateFlow<SealedImport?> = _sealed

    /** Opens the waiting sealed backup with [passphrase] and imports it; a wrong one asks again. */
    fun openSealed(passphrase: String) = viewModelScope.launch {
        val pending = _sealed.value ?: return@launch
        val msg = withContext(Dispatchers.IO) {
            val opened = runCatching { br.gabriel.cochilo.backup.Sealed.open(pending.text, passphrase) }.getOrElse { e ->
                if (e is br.gabriel.cochilo.backup.WrongPassphrase) { _sealed.value = pending.copy(wrong = true); return@withContext null }
                _sealed.value = null
                return@withContext str(R.string.import_failed)
            }
            _sealed.value = null
            importBackup(opened)
        } ?: return@launch
        refreshNow()
        _messages.send(UiMessage(msg))
    }

    fun cancelSealed() { _sealed.value = null }

    val backupPassphrase: StateFlow<String?> = ctx.prefs.backupPassphrase
    fun setBackupPassphrase(p: String?) {
        ctx.prefs.setBackupPassphrase(p)
        viewModelScope.launch { _messages.send(UiMessage(str(if (p == null) R.string.passphrase_removed else R.string.passphrase_saved))) }
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
        sides = { left, right -> str(R.string.napper_sides, left, right) },
    )
}

sealed interface BabyState {
    data object Loading : BabyState
    data object Missing : BabyState
    data class Ready(val baby: Baby) : BabyState
}
