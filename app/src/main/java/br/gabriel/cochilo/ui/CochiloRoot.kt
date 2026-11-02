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
import br.gabriel.cochilo.ui.screens.HistoryScreen
import br.gabriel.cochilo.ui.screens.PatternsScreen
import br.gabriel.cochilo.ui.screens.ScrollPage
import br.gabriel.cochilo.ui.screens.SettingsScreen
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
    val snackbar = remember { SnackbarHostState() }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportJson) }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::exportCsv) }
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
            Column(Modifier.fillMaxSize()) {
                if (settings) {
                    BackHeader(stringResource(R.string.title_settings), stringResource(R.string.back)) { settings = false }
                    Box(Modifier.weight(1f)) {
                        SettingsScreen(
                            baby, theme, zone,
                            onEditBaby = { editingBaby = true },
                            onEditLook = { editingLook = true },
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
                    Header(baby?.name?.ifBlank { null } ?: stringResource(R.string.app_name), status) {
                        IconButton(R.drawable.gg_ic_gear, stringResource(R.string.settings), { settings = true })
                    }
                    Box(Modifier.weight(1f)) {
                        when (babyState) {
                            BabyState.Loading -> Unit
                            // The first run: meet the baby, then how the baby looks (docs/onboarding.md).
                            BabyState.Missing -> br.gabriel.cochilo.ui.screens.Onboarding(zone, onDone = { vm.saveBaby(it) })
                            is BabyState.Ready -> {
                                val reduce = CochiloTheme.reduceMotion
                                AnimatedContent(
                                    targetState = tab,
                                    transitionSpec = {
                                        val d = if (reduce) 0 else CTokens.Motion.motionStandard
                                        fadeIn(tween(d, easing = SystemEasing)) togetherWith fadeOut(tween(d, easing = SystemEasing))
                                    },
                                    label = "tab",
                                ) { t ->
                                    when (t) {
                                        0 -> TodayScreen(
                                            log, prediction, now, zone,
                                            onStartSleep = { vm.startSleep() },
                                            onWokeUp = { vm.wakeUp() },
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
                                        1 -> DayScreen(log, now, zone, onOpen = { editing = Editing(it, false) })
                                        2 -> HistoryScreen(log, now, zone) { editing = Editing(it, false) }
                                        else -> PatternsScreen(log, now, zone, weeks)
                                    }
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
                    if (babyState is BabyState.Ready) {
                        TabBar(
                            listOf(
                                Tab(stringResource(R.string.tab_today), R.drawable.gg_ic_home),
                                Tab(stringResource(R.string.tab_day), R.drawable.gg_ic_day),
                                Tab(stringResource(R.string.tab_history), R.drawable.gg_ic_list),
                                Tab(stringResource(R.string.tab_patterns), R.drawable.gg_ic_chart),
                            ),
                            tab,
                        ) { tab = it }
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
            onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(it); editing = null },
            onDismiss = { editing = null },
        )
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
