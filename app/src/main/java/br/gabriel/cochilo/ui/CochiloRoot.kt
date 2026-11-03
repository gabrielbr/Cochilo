package br.gabriel.cochilo.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.domain.lastOf
import br.gabriel.cochilo.domain.rememberedFrom
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.ageWeeks
import br.gabriel.cochilo.ui.kit.BackHeader
import br.gabriel.cochilo.ui.screens.LogKeys
import br.gabriel.cochilo.ui.screens.logMenuItems
import br.gabriel.cochilo.ui.kit.LogButton
import br.gabriel.cochilo.update.AboutUpdates
import br.gabriel.cochilo.update.UpdatePrompt
import br.gabriel.cochilo.ui.kit.EmptyState
import br.gabriel.cochilo.ui.kit.Frame
import br.gabriel.cochilo.ui.kit.Header
import br.gabriel.cochilo.ui.kit.IconButton
import br.gabriel.cochilo.ui.kit.ScreenTitle
import br.gabriel.cochilo.ui.kit.Tab
import br.gabriel.cochilo.ui.kit.TabBar
import br.gabriel.cochilo.ui.screens.BabySheet
import br.gabriel.cochilo.ui.screens.DayScreen
import br.gabriel.cochilo.ui.screens.EventSheet
import br.gabriel.cochilo.ui.screens.PatternsScreen
import br.gabriel.cochilo.ui.screens.ScrollPage
import br.gabriel.cochilo.ui.screens.SettingsScreen
import br.gabriel.cochilo.ui.screens.toExchange
import br.gabriel.cochilo.ui.screens.toItem
import br.gabriel.cochilo.ui.screens.TodayScreen
import br.gabriel.cochilo.ui.screens.newEvent
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.SystemEasing
import br.gabriel.cochilo.ui.theme.Type
import java.time.LocalDate

private data class Editing(val event: Event, val isNew: Boolean)

@Composable
fun CochiloRoot(vm: MainViewModel) {
    val log by vm.log.collectAsState()
    val babyState by vm.baby.collectAsState()
    val theme by vm.theme.collectAsState()
    val now by vm.now.collectAsState()
    val zone = vm.zone
    val context = LocalContext.current
    val f = Format(context, zone, now)

    var tab by rememberSaveable { mutableStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var editingBaby by remember { mutableStateOf(false) }
    var editingLook by remember { mutableStateOf(false) }
    var editingRitual by remember { mutableStateOf(false) }
    var editingPassphrase by remember { mutableStateOf(false) }
    var asking by rememberSaveable { mutableStateOf(false) }
    // Ask's chats (0.40.0): the open one (a new id until its first question), and the list.
    var chatId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var showingChats by rememberSaveable { mutableStateOf(false) }
    var deletedChat by androidx.compose.runtime.remember { mutableStateOf<br.gabriel.cochilo.data.Chat?>(null) }
    fun newChat() { chatId = java.util.UUID.randomUUID().toString(); showingChats = false }
    var showingModel by remember { mutableStateOf(false) }
    val pickModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importModel) }
    val snackbar = remember { SnackbarHostState() }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportJson) }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::exportCsv) }
    val exportReport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { it?.let(vm::exportReport) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importFile) }
    val live by vm.liveNotification.collectAsState()
    val fabric = br.gabriel.cochilo.ui.kit.Fabric.of(vm.fabric.collectAsState().value)
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.notificationsAnswered() }
    fun requestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            vm.askedNotifications()
            askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            val r = snackbar.showSnackbar(m.text, actionLabel = m.action, withDismissAction = false,
                duration = androidx.compose.material3.SnackbarDuration.Long)
            if (r == SnackbarResult.ActionPerformed) m.onAction?.invoke()
        }
    }
    BackHandler(enabled = settings) { settings = false }
    // Once there's a baby, ask once whether the live notification may show (Android 13+).
    LaunchedEffect(babyState is BabyState.Ready) {
        if (babyState is BabyState.Ready && live && vm.shouldAskNotifications() &&
            !br.gabriel.cochilo.notify.LiveNotifications.canPost(context)) requestNotifications()
    }

    val baby = (babyState as? BabyState.Ready)?.baby
    val weeks = baby?.let { ageWeeks(it.birthDate, it.dueDate, now) } ?: 0
    val prediction = remember(log, now, weeks) { Predictor(zone).predict(log, weeks, now) }

    // The baby's look, for the avatar anywhere below (docs/onboarding.md).
    val look = remember(baby?.look) { br.gabriel.cochilo.domain.BabyLook.decode(baby?.look) }
    androidx.compose.runtime.CompositionLocalProvider(br.gabriel.cochilo.ui.kit.LocalBabyLook provides look) {
    Frame {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            val tabs = listOf(
                Tab(stringResource(R.string.tab_today), R.drawable.gg_ic_home),
                Tab(stringResource(R.string.tab_day), R.drawable.gg_ic_day),
                Tab(stringResource(R.string.tab_patterns), R.drawable.gg_ic_chart),
            )
            // Phone: tab bar; from 600dp a rail; from 840dp Today beside a second tab (docs/tablet.md).
            run {
                if (asking && showingChats) {
                    androidx.activity.compose.BackHandler { showingChats = false; deletedChat = null }
                    br.gabriel.cochilo.ui.kit.AppShell(
                        tabs, tab, { tab = it }, showTabs = false,
                        header = { BackHeader(stringResource(R.string.chats_title), stringResource(R.string.back)) { showingChats = false; deletedChat = null } },
                    ) {
                        br.gabriel.cochilo.ui.screens.ChatsScreen(
                            vm.chats.collectAsState().value, f,
                            onNew = { newChat(); deletedChat = null },
                            onOpen = { chatId = it; showingChats = false; deletedChat = null },
                            onDelete = { chat ->
                                vm.deleteChat(chat.id); deletedChat = chat
                                if (chat.id == chatId) chatId = java.util.UUID.randomUUID().toString()
                            },
                            deleted = deletedChat,
                            onUndo = { deletedChat?.let(vm::restoreChat); deletedChat = null },
                        )
                    }
                } else if (asking) {
                    androidx.activity.compose.BackHandler { asking = false }
                    val savedChats = vm.chats.collectAsState().value
                    val current = savedChats.firstOrNull { it.id == chatId }
                    br.gabriel.cochilo.ui.kit.AppShell(
                        tabs, tab, { tab = it }, showTabs = false,
                        header = { BackHeader(current?.title ?: stringResource(R.string.ask_title), stringResource(R.string.back)) { asking = false } },
                    ) {
                        val facts = remember(log, now, weeks) { br.gabriel.cochilo.domain.assistant.Assistant.facts(log, weeks, now, zone, prediction) }
                        val model = vm.modelState.collectAsState().value
                        val ready = model is br.gabriel.cochilo.ai.ModelState.Ready
                        // When the list can't answer and the model isn't here yet: one tap to get it (0.37.0).
                        val canGet = model is br.gabriel.cochilo.ai.ModelState.Absent || model is br.gabriel.cochilo.ai.ModelState.Failed
                        // Everything the model may draw on about this baby (0.38.0): the log, day by day.
                        val pt = stringResource(R.string.locale_tag).startsWith("pt")
                        val digest = remember(log, now, weeks, facts, pt) {
                            br.gabriel.cochilo.domain.assistant.Digest.lines(log, baby?.name, weeks, now, zone, facts, pt)
                        }
                        val paused = vm.modelPaused.collectAsState().value
                        // A fresh screen for each chat: its own questions, saved as they come.
                        androidx.compose.runtime.key(chatId) {
                            val id = chatId
                            br.gabriel.cochilo.ui.screens.AskScreen(
                                facts, f, initial = current?.items?.map { it.toExchange() } ?: emptyList(),
                                generate = if (ready && !paused) vm::generate else null,
                                onResumeModel = if (ready && paused) vm::resumeModel else null,
                                onGetModel = if (canGet) ({ showingModel = true }) else null,
                                digest = digest,
                                modelGetting = model is br.gabriel.cochilo.ai.ModelState.Getting,
                                onChange = { list -> vm.saveChat(id, list.mapNotNull { it.toItem() }) },
                                chats = {
                                    br.gabriel.cochilo.ui.kit.RowGroup {
                                        br.gabriel.cochilo.ui.kit.ListRow(
                                            stringResource(R.string.ask_chats_row), meta = stringResource(R.string.ask_chats_row_meta, savedChats.size),
                                            onClick = { showingChats = true },
                                            trailing = { br.gabriel.cochilo.ui.kit.GIcon(R.drawable.gg_ic_chevron, null, CochiloTheme.colors.muted) },
                                        )
                                    }
                                },
                            )
                        }
                    }
                } else if (settings) {
                    br.gabriel.cochilo.ui.kit.AppShell(
                        tabs, tab, { tab = it }, showTabs = false,
                        header = { BackHeader(stringResource(R.string.title_settings), stringResource(R.string.back)) { settings = false } },
                    ) {
                        SettingsScreen(
                            baby, theme, zone,
                            onEditBaby = { editingBaby = true },
                            onEditLook = { editingLook = true },
                            onEditRitual = { editingRitual = true },
                            onExportReport = { exportReport.launch("cochilo-report-${LocalDate.now(zone)}.pdf") },
                            passphraseSet = vm.backupPassphrase.collectAsState().value != null,
                            modelStatus = br.gabriel.cochilo.ui.screens.modelStatus(vm.modelState.collectAsState().value),
                            onModel = { vm.refreshModel(); showingModel = true },
                            reminders = vm.reminders.collectAsState().value,
                            onReminders = { on ->
                                vm.setReminders(on)
                                if (on && !br.gabriel.cochilo.notify.LiveNotifications.canPost(context)) requestNotifications()
                            },
                            reminderLeads = vm.reminderLeads.collectAsState().value,
                            onReminderLeads = vm::setReminderLeads,
                            tataSuggestions = if (vm.catalog.approved.isEmpty()) null else vm.tataSuggestions.collectAsState().value,
                            onTataSuggestions = vm::setTataSuggestions,
                            onPassphrase = { editingPassphrase = true },
                            celebrations = vm.celebrations.collectAsState().value,
                            onCelebrations = vm::setCelebrations,
                            nightLight = vm.nightLight.collectAsState().value,
                            onNightLight = vm::setNightLight,
                            onTheme = vm::setTheme,
                            onExportJson = { exportJson.launch("cochilo-${LocalDate.now(zone)}.json") },
                            onExportCsv = { exportCsv.launch("cochilo-${LocalDate.now(zone)}.csv") },
                            onImport = { import.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                            // Napper's export is a .csv; some file pickers label it text/plain or octet-stream.
                            onImportNapper = { import.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/octet-stream")) },
                            liveNotification = live,
                            onLiveNotification = { on ->
                                vm.setLiveNotification(on)
                                if (on && !br.gabriel.cochilo.notify.LiveNotifications.canPost(context)) requestNotifications()
                            },
                            fabric = fabric,
                            onFabric = { vm.setFabric(it.key) },
                            about = { AboutUpdates() },
                        )
                    }
                } else {
                    val status = when (prediction) {
                        is Predictor.Result.Sleeping -> stringResource(R.string.status_sleeping, f.duration(now - prediction.since))
                        is Predictor.Result.Awake -> stringResource(R.string.status_awake, f.duration(now - prediction.since))
                        Predictor.Result.NeedData -> if (baby != null) stringResource(R.string.status_weeks, weeks) else stringResource(R.string.status_no_data)
                    }
                    val headerName = baby?.name?.ifBlank { null } ?: stringResource(R.string.app_name)
                    br.gabriel.cochilo.ui.kit.AppShell(
                        tabs, tab, { tab = it }, showTabs = babyState is BabyState.Ready,
                        header = {
                            Header(headerName, status) {
                                // Ask Cochilo from every tab, one tap (0.38.0): it used to be a row deep in Patterns.
                                if (babyState is BabyState.Ready) IconButton(R.drawable.gg_ic_chat, stringResource(R.string.ask_open), { newChat(); asking = true })
                                IconButton(R.drawable.gg_ic_gear, stringResource(R.string.settings), { settings = true })
                            }
                        },
                    ) { width ->
                        when (babyState) {
                            BabyState.Loading -> Unit
                            // The first run: meet the baby, then how the baby looks (docs/onboarding.md).
                            BabyState.Missing -> br.gabriel.cochilo.ui.screens.Onboarding(zone, onDone = { vm.saveBaby(it) })
                            is BabyState.Ready -> {
                                val reduce = CochiloTheme.reduceMotion
                                val screen: @Composable (Int) -> Unit = { t ->
                                    when (t) {
                                        0 -> TodayScreen(
                                            log, prediction, now, zone,
                                            onStartSleep = { vm.startSleep(it) },
                                            onWokeUp = { vm.wakeUp(it) },
                                            onPlace = { e, k -> vm.setPlace(e, k) },
                                            onFinish = { vm.finish(it) },
                                            onNew = { type ->
                                                // Remember actions: the sheet opens on what was done last (the same
                                                // breast, diaper kind, bottle amount, medicine), so a repeat is one tap + Save.
                                                editing = Editing(newEvent(type, System.currentTimeMillis()).rememberedFrom(log), true)
                                            },
                                            onQuick = { type, kind -> vm.quickLog(type, kind) },
                                            onSwitch = vm::switchSide,
                                            onOpen = { editing = Editing(it, false) },
                                        )
                                        1 -> DayScreen(
                                            log, now, zone, onOpen = { editing = Editing(it, false) },
                                            expected = (prediction as? Predictor.Result.Awake)?.let { it.windowStart to it.windowEnd },
                                            onAdjust = vm::adjustEnd,
                                        )
                                        else -> {
                                            // A note from Tatá when one is due (in-app-marketing.md): rules in Suggestions.
                                            vm.suggestionTick.collectAsState().value
                                            vm.tataSuggestions.collectAsState().value
                                            val sleeping = prediction is Predictor.Result.Sleeping
                                            // One Tatá card at most: her note for the baby's age first (content), else a suggestion.
                                            val band = vm.ageNote(weeks, now, sleeping)
                                            val due = if (band == null) vm.suggestion(log, weeks, now, sleeping) else null
                                            if (due != null) androidx.compose.runtime.LaunchedEffect(due.moment) { vm.suggestionShown(due.moment) }
                                            PatternsScreen(
                                                log, now, zone, weeks, onAsk = { newChat(); asking = true },
                                                tataNote = when {
                                                    band != null -> { { br.gabriel.cochilo.ui.screens.TataAgeNote(band, vm.catalog.noteProduct(band), vm.catalog.noteUrl(band), onClose = { vm.closeAgeNote(band) }) } }
                                                    due != null -> { { br.gabriel.cochilo.ui.screens.TataSuggestion(due, vm.catalog.url(due.moment), onDismiss = { vm.dismissSuggestion(due.moment) }) } }
                                                    else -> null
                                                },
                                            )
                                        }
                                    }
                                }
                                val animated: @Composable (Int) -> Unit = { target ->
                                    AnimatedContent(
                                        targetState = target,
                                        transitionSpec = {
                                            val d = if (reduce) 0 else CTokens.Motion.motionStandard
                                            fadeIn(tween(d, easing = SystemEasing)) togetherWith fadeOut(tween(d, easing = SystemEasing))
                                        },
                                        label = "tab",
                                    ) { screen(it) }
                                }
                                // Wide: Today stays at the start, the picked tab beside it (Day when Today is picked).
                                if (width == br.gabriel.cochilo.ui.kit.WidthClass.EXPANDED) {
                                    br.gabriel.cochilo.ui.kit.TwoPanes({ screen(0) }, { animated(if (tab == 0) 1 else tab) })
                                } else animated(tab)
                                // A little celebration when what she was trying for happens (docs/celebrations.md).
                                val cheer = vm.celebration.collectAsState().value
                                if (cheer != null) {
                                    val who = baby?.name ?: ""
                                    val words = when (cheer.moment) {
                                        br.gabriel.cochilo.domain.Moment.FULL_NAP -> stringResource(R.string.cheer_full_nap)
                                        br.gabriel.cochilo.domain.Moment.ON_TIME -> stringResource(R.string.cheer_on_time, who)
                                        br.gabriel.cochilo.domain.Moment.LONGEST_NIGHT -> stringResource(R.string.cheer_longest_night, who)
                                        br.gabriel.cochilo.domain.Moment.DAYLIGHT -> stringResource(R.string.cheer_daylight)
                                        br.gabriel.cochilo.domain.Moment.ROUTINE_BEFORE_BED -> stringResource(R.string.cheer_routine)
                                        br.gabriel.cochilo.domain.Moment.WEEK_LOGGED -> stringResource(R.string.cheer_week, who)
                                    }
                                    br.gabriel.cochilo.ui.kit.CelebrationCard(
                                        words, look, cheer.quiet, onDone = vm::celebrationSeen,
                                        modifier = Modifier.align(Alignment.TopCenter).padding(br.gabriel.cochilo.ui.theme.CTokens.Spacing.space4),
                                    )
                                }
                                // The Log button: the main action, under the right thumb, on every tab
                                // (docs/flows.md). Press, drag to a kind, slide to a choice, let go.
                                val newOf = { type: EventType -> editing = Editing(newEvent(type, System.currentTimeMillis()).rememberedFrom(log), true) }
                                LogButton(
                                    items = logMenuItems(context, f, log, prediction is Predictor.Result.Sleeping),
                                    label = stringResource(R.string.log_button),
                                    closeLabel = stringResource(R.string.log_close),
                                    onItem = { item -> newOf(EventType.valueOf(item.key)) },
                                    onOption = { item, o ->
                                        val type = EventType.valueOf(item.key)
                                        when (o.key) {
                                            LogKeys.START_SLEEP -> vm.startSleep()
                                            LogKeys.WOKE_UP -> vm.wakeUp()
                                            LogKeys.START -> vm.quickLog(type, null)
                                            LogKeys.REPEAT_MEDICINE -> vm.quickLog(type, null, note = o.label)
                                            br.gabriel.cochilo.data.Kinds.BOTTLE -> vm.quickLog(
                                                type, o.key,
                                                amountMl = log.filter { it.type == EventType.FEED && it.kind == o.key && it.amountMl != null }
                                                    .maxByOrNull { it.start }?.amountMl,
                                            )
                                            else -> vm.quickLog(type, o.key)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(Spacing.space4)) { data ->
                val c = CochiloTheme.colors
                Snackbar(
                    modifier = Modifier.widthIn(max = Spacing.space16 * 8).fillMaxWidth(),
                    shape = RoundedCornerShape(Radius.radiusS),
                    containerColor = c.snackbar,
                    contentColor = c.page,
                    action = data.visuals.actionLabel?.let { label ->
                        {
                            androidx.compose.material3.TextButton(onClick = { data.performAction() }) {
                                Text(label, style = Type.label, color = c.snackbarAction)
                            }
                        }
                    },
                ) { Text(data.visuals.message, style = Type.bodySmall, color = c.page) }
            }
        }
    }
    }

    editing?.let { ed ->
        EventSheet(
            initial = ed.event, isNew = ed.isNew, zone = zone,
            last = if (ed.isNew) lastOf(log, ed.event.type) else null,
            rituals = vm.rituals.collectAsState().value,
            onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(it); editing = null },
            onDismiss = { editing = null },
        )
    }
    // Details after: the one question an entry has once it ends (docs/flows.md).
    vm.details.collectAsState().value?.let { pending ->
        br.gabriel.cochilo.ui.screens.DetailsSheet(
            pending, log, baby?.name ?: "",
            onKind = { vm.answerDetails(pending, kind = it) },
            onAmount = { vm.answerDetails(pending, amountMl = it) },
            onSkip = vm::skipDetails,
        )
    }
    if (vm.ticks.collectAsState().value) {
        br.gabriel.cochilo.ui.screens.TicksSheet(onDismiss = vm::ticksSeen)
    }
    if (showingModel) {
        br.gabriel.cochilo.ui.screens.ModelSheet(
            vm.modelState.collectAsState().value, vm.modelKind,
            onDownload = vm::downloadModel,
            // Model files come as .task (zip) or a generic type, depending on the file picker.
            onImport = { pickModel.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) },
            onRemove = vm::removeModel,
            onDismiss = { showingModel = false },
            onMobileData = vm::modelOnMobileData,
        )
    }
    if (editingPassphrase) {
        br.gabriel.cochilo.ui.screens.PassphraseSheet(vm.backupPassphrase.collectAsState().value, onSave = vm::setBackupPassphrase, onDismiss = { editingPassphrase = false })
    }
    vm.sealedImport.collectAsState().value?.let { pending ->
        br.gabriel.cochilo.ui.screens.UnlockSheet(pending.wrong, onOpen = vm::openSealed, onDismiss = vm::cancelSealed)
    }
    if (editingRitual) {
        br.gabriel.cochilo.ui.screens.RitualSheet(vm.rituals.collectAsState().value, onSave = vm::setRitual, onDismiss = { editingRitual = false })
    }
    if (editingLook && baby != null) {
        br.gabriel.cochilo.ui.screens.LookSheet(baby, onSave = { vm.saveBaby(it); editingLook = false }, onDismiss = { editingLook = false })
    }
    if (editingBaby) {
        BabySheet(baby, zone, onSave = { vm.saveBaby(it); editingBaby = false }, onDismiss = { editingBaby = false })
    }
    // The github build offers its own updates here; the play build draws nothing.
    UpdatePrompt()
}
