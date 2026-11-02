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
import br.gabriel.cochilo.domain.suggestedBreast
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.ageWeeks
import br.gabriel.cochilo.ui.kit.BackHeader
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
    val snackbar = remember { SnackbarHostState() }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportJson) }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::exportCsv) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importFile) }
    val live by vm.liveNotification.collectAsState()
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

    Frame {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
                if (settings) {
                    BackHeader(stringResource(R.string.title_settings), stringResource(R.string.back)) { settings = false }
                    Box(Modifier.weight(1f)) {
                        SettingsScreen(
                            baby, theme, zone,
                            onEditBaby = { editingBaby = true },
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
                            BabyState.Missing -> ScrollPage {
                                ScreenTitle(stringResource(R.string.welcome_title), stringResource(R.string.app_name))
                                EmptyState(
                                    stringResource(R.string.add_baby), stringResource(R.string.welcome_line),
                                    stringResource(R.string.add_baby),
                                ) { editingBaby = true }
                            }
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
                                                // The sheet opens on the breast to offer next, so the usual feed is one tap + Save.
                                                val e = newEvent(type, System.currentTimeMillis())
                                                editing = Editing(if (type == EventType.FEED) e.copy(kind = suggestedBreast(log) ?: e.kind) else e, true)
                                            },
                                            onQuick = vm::quickLog,
                                            onSwitch = vm::switchSide,
                                            onOpen = { editing = Editing(it, false) },
                                        )
                                        1 -> DayScreen(log, now, zone, onOpen = { editing = Editing(it, false) })
                                        2 -> HistoryScreen(log, now, zone) { editing = Editing(it, false) }
                                        else -> PatternsScreen(log, now, zone, weeks)
                                    }
                                }
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

    editing?.let { ed ->
        EventSheet(
            initial = ed.event, isNew = ed.isNew, zone = zone,
            onSave = { vm.save(it); editing = null },
            onDelete = { vm.delete(it); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (editingBaby) {
        BabySheet(baby, zone, onSave = { vm.saveBaby(it); editingBaby = false }, onDismiss = { editingBaby = false })
    }
    // The github build offers its own updates here; the play build draws nothing.
    UpdatePrompt()
}
