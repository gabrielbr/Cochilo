package br.gabriel.cochilo.ui

import br.gabriel.cochilo.domain.isNightSleep
import br.gabriel.cochilo.ui.screens.TicksBody
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import br.gabriel.cochilo.domain.overlaps
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.CheckForUpdates
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.UpdateSheetBody
import br.gabriel.cochilo.ui.screens.SheetTitle
import br.gabriel.cochilo.ui.theme.CTokens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.data.ThemeMode
import br.gabriel.cochilo.domain.Predictor
import br.gabriel.cochilo.domain.ageWeeks
import br.gabriel.cochilo.ui.kit.BackHeader
import br.gabriel.cochilo.ui.kit.LogButton
import br.gabriel.cochilo.ui.screens.logMenuItems
import br.gabriel.cochilo.ui.kit.Frame
import br.gabriel.cochilo.ui.kit.Header
import br.gabriel.cochilo.ui.kit.IconButton
import br.gabriel.cochilo.ui.kit.Tab
import br.gabriel.cochilo.ui.kit.TabBar
import br.gabriel.cochilo.ui.screens.DayScreen
import br.gabriel.cochilo.ui.screens.VerticalTimeline
import br.gabriel.cochilo.ui.screens.HistoryScreen
import br.gabriel.cochilo.ui.screens.PatternsScreen
import br.gabriel.cochilo.ui.screens.SettingsScreen
import br.gabriel.cochilo.ui.screens.TodayScreen
import br.gabriel.cochilo.ui.theme.CochiloTheme
import com.android.resources.NightMode
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Real-size screenshots of every screen, light and dark, English and Portuguese, rendered on the
 * JVM. `./gradlew :app:recordPaparazziDebug` writes them to app/src/test/snapshots/images.
 */
class ScreensSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 0.1)

    private val zone = ZoneId.systemDefault()
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, day, h, m).atZone(zone).toInstant().toEpochMilli()
    private val now = at(31, 15, 10)
    private val baby = Baby(name = "Bebê", birthDate = at(1, 0) - 12L * 7 * 86_400_000)

    private val log: List<Event> = buildList {
        for (d in 17..30) {
            val j = (d % 3) * 5 // a little day-to-day variation
            add(Event(type = EventType.SLEEP, start = at(d, 8, j), end = at(d, 9, 10)))
            add(Event(type = EventType.SLEEP, start = at(d, 10, 40 + j / 2), end = at(d, 11, 50)))
            add(Event(type = EventType.SLEEP, start = at(d, 13, 20), end = at(d, 14, 40 + j)))
            add(Event(type = EventType.SLEEP, start = at(d, 16, 15), end = at(d, 16, 55)))
            add(Event(type = EventType.SLEEP, start = at(d, 19, 50 + j / 2), end = at(d, 23, 30)))
            add(Event(type = EventType.SLEEP, start = at(d, 23, 50), end = at(d + 1, 3, 40)))
            add(Event(type = EventType.SLEEP, start = at(d + 1, 4, 10), end = at(d + 1, 6, 40)))
            listOf(7, 10, 13, 16, 19, 23).forEach { h ->
                add(Event(type = EventType.FEED, start = at(d, h, 0), end = at(d, h, 20), kind = if (h % 2 == 0) Kinds.BREAST_LEFT else Kinds.BREAST_RIGHT))
            }
            listOf(7, 12, 17, 22).forEach { h -> add(Event(type = EventType.DIAPER, start = at(d, h, 30), kind = Kinds.WET)) }
            add(Event(type = EventType.DAYLIGHT, start = at(d, 12, 0), end = at(d, 12, 45), kind = Kinds.OUTDOORS))
        }
        add(Event(type = EventType.SLEEP, start = at(31, 8, 0), end = at(31, 9, 5)))
        add(Event(type = EventType.SLEEP, start = at(31, 10, 35), end = at(31, 11, 40)))
        add(Event(type = EventType.SLEEP, start = at(31, 13, 15), end = at(31, 14, 30)))
        add(Event(type = EventType.FEED, start = at(31, 7, 0), end = at(31, 7, 18), kind = Kinds.BREAST_LEFT))
        add(Event(type = EventType.FEED, start = at(31, 11, 45), end = at(31, 12, 0), kind = Kinds.BOTTLE, amountMl = 120))
        add(Event(type = EventType.DIAPER, start = at(31, 7, 30), kind = Kinds.DIRTY))
        add(Event(type = EventType.DAYLIGHT, start = at(31, 14, 45), kind = Kinds.OUTDOORS))
        add(Event(type = EventType.PUMP, start = at(31, 9, 30), end = at(31, 9, 50), amountMl = 90))
    }

    @Composable
    private fun Chrome(
        dark: Boolean, tab: Int, log: List<Event> = this.log, settings: Boolean = false,
        logHover: br.gabriel.cochilo.ui.kit.LogHover? = null,
        night: Boolean = false,
    ) {
        CochiloTheme(dark, night) {
            val weeks = ageWeeks(baby.birthDate, null, now)
            val p = Predictor(zone).predict(log, weeks, now)
            Frame {
                val tabs = listOf(
                    Tab(stringResource(R.string.tab_today), R.drawable.gg_ic_home),
                    Tab(stringResource(R.string.tab_day), R.drawable.gg_ic_day),
                    Tab(stringResource(R.string.tab_history), R.drawable.gg_ic_list),
                    Tab(stringResource(R.string.tab_patterns), R.drawable.gg_ic_chart),
                )
                if (settings) {
                    br.gabriel.cochilo.ui.kit.AppShell(tabs, tab, {}, showTabs = false, header = {
                        BackHeader(stringResource(R.string.title_settings), stringResource(R.string.back)) {}
                    }) { SettingsScreen(baby, ThemeMode.SYSTEM, zone, {}, {}, {}, {}, {}, {}, fabric = br.gabriel.cochilo.ui.kit.LocalFabric.current) }
                    return@Frame
                }
                val f = Format(androidx.compose.ui.platform.LocalContext.current, zone, now)
                val status = when (p) {
                    is Predictor.Result.Sleeping -> stringResource(R.string.status_sleeping, f.duration(now - p.since))
                    is Predictor.Result.Awake -> stringResource(R.string.status_awake, f.duration(now - p.since))
                    else -> stringResource(R.string.status_weeks, weeks)
                }
                br.gabriel.cochilo.ui.kit.AppShell(tabs, tab, {}, header = {
                    Header(baby.name, status) { IconButton(R.drawable.gg_ic_chat, stringResource(R.string.ask_open), {}); IconButton(R.drawable.gg_ic_gear, stringResource(R.string.settings), {}) }
                }) { width ->
                    val screen: @Composable (Int) -> Unit = { t ->
                        when (t) {
                            0 -> TodayScreen(log, p, now, zone, {}, {}, {}, {}, {})
                            1 -> DayScreen(log, now, zone, onOpen = {}, initialDay = LocalDateTime.of(2026, 10, 30, 12, 0).toLocalDate())
                            2 -> HistoryScreen(log, now, zone) {}
                            else -> PatternsScreen(log, now, zone, weeks)
                        }
                    }
                    if (width == br.gabriel.cochilo.ui.kit.WidthClass.EXPANDED) {
                        br.gabriel.cochilo.ui.kit.TwoPanes({ screen(0) }, { screen(if (tab == 0) 1 else tab) })
                    } else screen(tab)
                    LogButton(
                        logMenuItems(androidx.compose.ui.platform.LocalContext.current, f, log, p is Predictor.Result.Sleeping),
                        stringResource(R.string.log_button), stringResource(R.string.log_close),
                        {}, { _, _ -> }, preview = logHover,
                    )
                }
            }
        }
    }

    /** The update sheet as it rises over Today (the github build only; same pixels in both builds). */
    @Composable
    private fun UpdateSheetShot(dark: Boolean, progress: Float?, notes: String) {
        CochiloTheme(dark) {
            Frame {
                Box(Modifier.fillMaxSize().background(CochiloTheme.colors.page), contentAlignment = Alignment.BottomCenter) {
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = CTokens.Radius.radiusL, topEnd = CTokens.Radius.radiusL))
                            .background(CochiloTheme.colors.surface)
                            .padding(CTokens.Spacing.space6)
                    ) {
                        SheetTitle(stringResource(R.string.update_available, "0.2.0"))
                        UpdateSheetBody(
                            version = "0.2.0",
                            notes = notes,
                            sizeLabel = "6.8 MB",
                            progress = progress,
                            message = null,
                            onUpdate = {}, onLater = {}, onCancel = {},
                        )
                    }
                }
            }
        }
    }

    private val sleepingLog get() = log + Event(type = EventType.SLEEP, start = at(31, 14, 50))

    private fun light() = paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NOTNIGHT))
    private fun dark() = paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))
    private fun pt(night: Boolean) = paparazzi.unsafeUpdateConfig(
        DeviceConfig.PIXEL_5.copy(locale = "pt-rBR", nightMode = if (night) NightMode.NIGHT else NightMode.NOTNIGHT)
    )

    /** 200% font scale (Android 14's largest): nothing may clip or ellipsize a word that matters. */
    private fun big() = paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NOTNIGHT, fontScale = 2f))
    @Test fun today_font200_light() { big(); paparazzi.snapshot { Chrome(false, 0) } }
    @Test fun today_sleeping_font200_light() { big(); paparazzi.snapshot { Chrome(false, 0, sleepingLog) } }
    @Test fun log_button_open_font200_light() { big(); paparazzi.snapshot { Chrome(false, 0, logHover = br.gabriel.cochilo.ui.kit.LogHover(1)) } }
    @Test fun history_font200_light() { big(); paparazzi.snapshot { Chrome(false, 2) } }
    @Test fun patterns_font200_light() { big(); paparazzi.snapshot { Chrome(false, 3) } }
    @Test fun day_font200_light() { big(); paparazzi.snapshot { Chrome(false, 1) } }
    @Test fun settings_font200_light() { big(); paparazzi.snapshot { Chrome(false, 0, settings = true) } }

    /** Naps that vary, and windows that follow them (phase 2): today's last nap was short. */
    private val shortNapLog: List<Event> = buildList {
        val naps = listOf(40, 100, 70, 50, 90, 60, 110, 45)
        var k = 0
        for (d in 17..30) {
            var t = at(d, 6, 30)
            repeat(4) {
                val nap = naps[k % naps.size]
                val prev = if (it == 0) 70 else naps[(k - 1 + naps.size) % naps.size]
                val start = t + (90 + (prev - 70) / 2) * 60_000L
                add(Event(type = EventType.SLEEP, start = start, end = start + nap * 60_000L))
                t = start + nap * 60_000L
                k++
            }
            add(Event(type = EventType.SLEEP, start = at(d, 19, 45), end = at(d + 1, 6, 30)))
        }
        add(Event(type = EventType.SLEEP, start = at(31, 8, 0), end = at(31, 9, 10)))
        add(Event(type = EventType.SLEEP, start = at(31, 10, 40), end = at(31, 11, 50)))
        add(Event(type = EventType.SLEEP, start = at(31, 14, 0), end = at(31, 14, 35)))
    }
    @Test fun today_after_a_short_nap_light() { light(); paparazzi.snapshot { Chrome(false, 0, shortNapLog) } }

    // Wide screens (docs/tablet.md): a tablet in portrait is MEDIUM (the rail), an unfolded
    // foldable or a tablet on its side is EXPANDED (Today beside a second tab).
    private val tabletPortrait = DeviceConfig.PIXEL_5.copy(screenWidth = 1600, screenHeight = 2560, xdpi = 320, ydpi = 320, density = com.android.resources.Density.XHIGH, nightMode = NightMode.NOTNIGHT)
    private val foldOpen = DeviceConfig.PIXEL_5.copy(screenWidth = 2208, screenHeight = 1840, xdpi = 420, ydpi = 420, density = com.android.resources.Density.create(420), nightMode = NightMode.NOTNIGHT, orientation = com.android.resources.ScreenOrientation.LANDSCAPE)
    @Test fun today_tablet_light() { paparazzi.unsafeUpdateConfig(tabletPortrait); paparazzi.snapshot { Chrome(false, 0) } }
    @Test fun patterns_tablet_light() { paparazzi.unsafeUpdateConfig(tabletPortrait); paparazzi.snapshot { Chrome(false, 3) } }
    @Test fun today_foldable_light() { paparazzi.unsafeUpdateConfig(foldOpen); paparazzi.snapshot { Chrome(false, 0) } }
    @Test fun history_foldable_dark() { paparazzi.unsafeUpdateConfig(foldOpen.copy(nightMode = NightMode.NIGHT)); paparazzi.snapshot { Chrome(true, 2) } }
    @Test fun settings_foldable_light() { paparazzi.unsafeUpdateConfig(foldOpen); paparazzi.snapshot { Chrome(false, 0, settings = true) } }

    @Test fun today_light() { light(); paparazzi.snapshot { Chrome(false, 0) } }
    @Test fun today_dark() { dark(); paparazzi.snapshot { Chrome(true, 0) } }
    @Test fun today_sleeping_dark() { dark(); paparazzi.snapshot { Chrome(true, 0, sleepingLog) } }
    @Test fun today_ptBR_light() { pt(false); paparazzi.snapshot { Chrome(false, 0) } }
    @Test fun history_light() { light(); paparazzi.snapshot { Chrome(false, 2) } }
    @Test fun patterns_light() { light(); paparazzi.snapshot { Chrome(false, 3) } }
    @Test fun patterns_dark() { dark(); paparazzi.snapshot { Chrome(true, 3) } }
    @Test fun patterns_ptBR_dark() { pt(true); paparazzi.snapshot { Chrome(true, 3) } }
    @Test fun day_light() { light(); paparazzi.snapshot { Chrome(false, 1) } }
    /** The Day tab's hour-by-hour list on its own, so its start is in the picture. */
    @Test fun day_hour_by_hour_light() {
        light()
        paparazzi.snapshot {
            CochiloTheme(false) {
                Frame {
                    val day = LocalDateTime.of(2026, 10, 30, 0, 0).atZone(zone)
                    val from = day.toInstant().toEpochMilli()
                    val to = day.plusDays(1).toInstant().toEpochMilli()
                    val f = Format(androidx.compose.ui.platform.LocalContext.current, zone, now)
                    Column(Modifier.fillMaxSize().padding(CTokens.Spacing.space4)) {
                        VerticalTimeline(log.filter { it.overlaps(from, to, now) }.sortedBy { it.start }, now, f, zone, onOpen = {})
                    }
                }
            }
        }
    }
    /** Drag to adjust: the second sleep of the day held and dragged three steps down (+15m). */
    @Test fun day_dragging_a_sleep_light() {
        light()
        paparazzi.snapshot {
            CochiloTheme(false) {
                Frame {
                    val day = LocalDateTime.of(2026, 10, 30, 0, 0).atZone(zone)
                    val from = day.toInstant().toEpochMilli()
                    val to = day.plusDays(1).toInstant().toEpochMilli()
                    val f = Format(androidx.compose.ui.platform.LocalContext.current, zone, now)
                    Column(Modifier.fillMaxSize().padding(CTokens.Spacing.space4)) {
                        VerticalTimeline(log.filter { it.overlaps(from, to, now) }.sortedBy { it.start }, now, f, zone, {}, onAdjust = { _, _ -> }, allSleeps = log, previewDrag = 1 to 3)
                    }
                }
            }
        }
    }
    /** The Log button mid-drag: on Feed, slid onto Right (the menu rises from the bottom-right). */
    @Test fun log_button_feed_right_light() { light(); paparazzi.snapshot { Chrome(false, 0, logHover = br.gabriel.cochilo.ui.kit.LogHover(0, 1)) } }
    @Test fun log_button_open_ptBR_dark() { pt(true); paparazzi.snapshot { Chrome(true, 0, logHover = br.gabriel.cochilo.ui.kit.LogHover(1)) } }
    @Test fun day_ptBR_dark() { pt(true); paparazzi.snapshot { Chrome(true, 1) } }
    @Test fun today_night_light_sleeping() { dark(); paparazzi.snapshot { Chrome(true, 0, sleepingLog, night = true) } }
    @Test fun day_night_light_ptBR() { pt(true); paparazzi.snapshot { Chrome(true, 1, night = true) } }
    @Test fun today_daylight_nudge_light() { light(); paparazzi.snapshot { Chrome(false, 0, log.filter { it.type != EventType.DAYLIGHT }) } }
    @Test fun today_sleeping_in_the_sling_light() {
        light()
        paparazzi.snapshot { Chrome(false, 0, log + Event(type = EventType.SLEEP, start = at(31, 14, 50), kind = Kinds.SLING)) }
    }
    @Test fun patterns_places_ptBR_light() {
        pt(false)
        val places = listOf(Kinds.SLING, Kinds.CRIB, Kinds.SLING, Kinds.ARMS)
        var i = 0
        val withPlaces = log.map { if (it.type == EventType.SLEEP && !it.isNightSleep(zone)) it.copy(kind = places[i++ % places.size]) else it }
        paparazzi.snapshot { Chrome(false, 3, withPlaces) }
    }
    @Test fun ticks_sheet_light() {
        light()
        paparazzi.snapshot { CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { androidx.compose.foundation.layout.Column(Modifier.padding(br.gabriel.cochilo.ui.theme.CTokens.Spacing.space6)) { TicksBody() } } } }
    }
    /** Details after: the baby woke; where did she sleep? Sling was used last, so it's marked. */
    @Test fun details_where_light() {
        light()
        val nap = Event(type = EventType.SLEEP, start = at(31, 13, 15), end = at(31, 14, 30))
        val withSling = log + Event(type = EventType.SLEEP, start = at(31, 10, 35), end = at(31, 11, 40), kind = Kinds.SLING) + nap
        paparazzi.snapshot { CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { androidx.compose.foundation.layout.Column(Modifier.padding(br.gabriel.cochilo.ui.theme.CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.DetailsBody(nap, withSling, "Bebê", {}, {}, {})
        } } } }
    }
    @Test fun details_pumped_ptBR_dark() {
        pt(true)
        val pump = Event(type = EventType.PUMP, start = at(31, 15, 0), end = at(31, 15, 20))
        paparazzi.snapshot { CochiloTheme(true) { br.gabriel.cochilo.ui.kit.Frame { androidx.compose.foundation.layout.Column(Modifier.padding(br.gabriel.cochilo.ui.theme.CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.DetailsBody(pump, log + pump, "Bebê", {}, {}, {})
        } } } }
    }
    /** Ask Cochilo: this week's numbers, a question answered from the log, and a health question sent to a person. */
    @Composable
    private fun AskShot(dark: Boolean, questions: List<String>, getModel: Boolean = false) {
        CochiloTheme(dark) {
            val weeks = ageWeeks(baby.birthDate, null, now)
            val p = Predictor(zone).predict(log, weeks, now)
            val facts = br.gabriel.cochilo.domain.assistant.Assistant.facts(log, weeks, now, zone, p)
            val f = Format(androidx.compose.ui.platform.LocalContext.current, zone, now)
            Frame {
                Column(Modifier.fillMaxSize()) {
                    BackHeader(stringResource(R.string.ask_title), stringResource(R.string.back)) {}
                    Box(Modifier.weight(1f)) {
                        br.gabriel.cochilo.ui.screens.AskScreen(facts, f, questions.map { br.gabriel.cochilo.ui.screens.AskExchange(it, br.gabriel.cochilo.domain.assistant.Assistant.ask(it, facts)) }, onGetModel = if (getModel) ({}) else null)
                    }
                }
            }
        }
    }
    @Test fun ask_light() { light(); paparazzi.snapshot { AskShot(false, listOf("Why are the naps so short?")) } }
    @Test fun ask_care_ptBR_dark() { pt(true); paparazzi.snapshot { AskShot(true, listOf("Ela está com febre, o que faço?")) } }
    /** The owner's own question (0.38.0): now answered from the log. */
    @Test fun ask_nap_length_ptBR_light() { pt(false); paparazzi.snapshot { AskShot(false, listOf("o quão devem demorar as sonecas do Dante?")) } }
    @Test fun ask_not_sure_light() { light(); paparazzi.snapshot { AskShot(false, listOf("What colour is the sky?"), getModel = true) } }

    /** On-device AI (docs/assistant.md): the sheet in its states, and an answer from the model. */
    @Composable
    private fun ModelShot(dark: Boolean, state: br.gabriel.cochilo.ai.ModelState, kind: br.gabriel.cochilo.ai.ModelSource.Kind) {
        CochiloTheme(dark) { br.gabriel.cochilo.ui.kit.Frame { androidx.compose.foundation.layout.Column(Modifier.padding(br.gabriel.cochilo.ui.theme.CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.ModelBody(state, kind, {}, {}, {}, {})
        } } }
    }
    @Test fun model_sheet_play_light() { light(); paparazzi.snapshot { ModelShot(false, br.gabriel.cochilo.ai.ModelState.Absent, br.gabriel.cochilo.ai.ModelSource.Kind.PLAY_DOWNLOAD) } }
    @Test fun model_sheet_app_downloading_light() { light(); paparazzi.snapshot { ModelShot(false, br.gabriel.cochilo.ai.ModelState.Getting(0.42f), br.gabriel.cochilo.ai.ModelSource.Kind.APP_DOWNLOAD) } }
    @Test fun model_sheet_app_download_light() { light(); paparazzi.snapshot { ModelShot(false, br.gabriel.cochilo.ai.ModelState.Absent, br.gabriel.cochilo.ai.ModelSource.Kind.APP_DOWNLOAD) } }
    @Test fun model_sheet_waiting_wifi_ptBR_light() { pt(false); paparazzi.snapshot { ModelShot(false, br.gabriel.cochilo.ai.ModelState.Getting(0.12f, waitingForWifi = true), br.gabriel.cochilo.ai.ModelSource.Kind.APP_DOWNLOAD) } }
    @Test fun model_sheet_ready_ptBR_dark() { pt(true); paparazzi.snapshot { ModelShot(true, br.gabriel.cochilo.ai.ModelState.Ready(java.io.File("model.task"), 555L * 1024 * 1024), br.gabriel.cochilo.ai.ModelSource.Kind.PLAY_DOWNLOAD) } }
    @Test fun ask_generated_light() {
        light()
        paparazzi.snapshot {
            CochiloTheme(false) {
                val weeks = ageWeeks(baby.birthDate, null, now)
                val facts = br.gabriel.cochilo.domain.assistant.Assistant.facts(log, weeks, now, zone, Predictor(zone).predict(log, weeks, now))
                val f = Format(androidx.compose.ui.platform.LocalContext.current, zone, now)
                Frame { Column(Modifier.fillMaxSize()) {
                    BackHeader(stringResource(R.string.ask_title), stringResource(R.string.back)) {}
                    Box(Modifier.weight(1f)) {
                        br.gabriel.cochilo.ui.screens.AskScreen(facts, f, listOf(
                            br.gabriel.cochilo.ui.screens.AskExchange("Does a warm bath help before bed?", br.gabriel.cochilo.domain.assistant.Assistant.Reply.NotSure(emptyList()),
                                generated = "A calm, repeated routine before bed is linked to easier bedtimes, and a bath can be one of its steps. This week the baby slept 14h 16m a day on average."),
                            br.gabriel.cochilo.ui.screens.AskExchange("And a story?", br.gabriel.cochilo.domain.assistant.Assistant.Reply.NotSure(emptyList()), thinking = true),
                        ))
                    }
                } }
            }
        }
    }

    /** Tatá's suggestion as it would read once she approves the moment (none is approved yet). */
    @Composable
    private fun TataNoteShot(dark: Boolean, s: br.gabriel.cochilo.domain.Suggestions.Suggestion) {
        CochiloTheme(dark) { br.gabriel.cochilo.ui.kit.Frame { Column(Modifier.padding(CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.TataSuggestion(s, "https://tataslings.com.br/", onDismiss = {})
        } } }
    }
    /** Tatá's note by age, as it would read once she approves it: content only, and one with the sling. */
    @Test fun age_note_3_4_months_light() {
        light()
        paparazzi.snapshot { CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { Column(Modifier.padding(CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.TataAgeNote(br.gabriel.cochilo.domain.AgeNotes.Band.M3_4, null, null, onClose = {})
        } } } }
    }
    @Test fun age_note_1_2_months_ptBR_dark() {
        pt(true)
        paparazzi.snapshot { CochiloTheme(true) { br.gabriel.cochilo.ui.kit.Frame { Column(Modifier.padding(CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.TataAgeNote(br.gabriel.cochilo.domain.AgeNotes.Band.M1_2, "tata_facil_sling", "https://tataslings.com.br/", onClose = {})
        } } } }
    }
    @Test fun tata_note_arms_naps_light() {
        light(); paparazzi.snapshot { TataNoteShot(false, br.gabriel.cochilo.domain.Suggestions.Suggestion(br.gabriel.cochilo.domain.Suggestions.Moment.ARMS_NAPS, "tata_facil_sling", 6)) }
    }
    @Test fun tata_note_routine_ptBR_dark() {
        pt(true); paparazzi.snapshot { TataNoteShot(true, br.gabriel.cochilo.domain.Suggestions.Suggestion(br.gabriel.cochilo.domain.Suggestions.Moment.ROUTINE_STEADY, "tata_naninha", 5)) }
    }

    /** Settings › Notifications once a Tatá moment is live (0.36.0): her switch shows, last. */
    @Test fun tata_switch_ptBR_light() {
        pt(false)
        paparazzi.snapshot {
            CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { Column(Modifier.padding(CTokens.Spacing.space6)) {
                br.gabriel.cochilo.ui.kit.RowGroup {
                    br.gabriel.cochilo.ui.kit.SwitchRow(stringResource(R.string.celebrations), stringResource(R.string.celebrations_meta), true, {})
                    Hairline()
                    br.gabriel.cochilo.ui.kit.SwitchRow(stringResource(R.string.tata_suggestions), stringResource(R.string.tata_suggestions_meta), true, {})
                }
            } } }
        }
    }

    /** Settings › Notifications with the window reminders (30 and 15 min before the next window). */
    @Test fun reminders_settings_ptBR_light() {
        pt(false)
        paparazzi.snapshot {
            CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { Column(Modifier.padding(CTokens.Spacing.space6)) {
                br.gabriel.cochilo.ui.kit.RowGroup {
                    br.gabriel.cochilo.ui.kit.SwitchRow(stringResource(R.string.live_notification), stringResource(R.string.live_notification_meta), true, {})
                    Hairline()
                    br.gabriel.cochilo.ui.screens.ReminderRows(true, {}, setOf(30, 15), {})
                }
            } } }
        }
    }

    @Test fun passphrase_sheet_light() {
        light()
        paparazzi.snapshot { CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { androidx.compose.foundation.layout.Column(Modifier.padding(br.gabriel.cochilo.ui.theme.CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.PassphraseBody("lua cheia", {}, {})
        } } } }
    }
    @Test fun unlock_sheet_wrong_ptBR_dark() {
        pt(true)
        paparazzi.snapshot { CochiloTheme(true) { br.gabriel.cochilo.ui.kit.Frame { androidx.compose.foundation.layout.Column(Modifier.padding(br.gabriel.cochilo.ui.theme.CTokens.Spacing.space6)) {
            br.gabriel.cochilo.ui.screens.UnlockBody(wrong = true, onOpen = {}, onCancel = {}, initial = "lua nova")
        } } } }
    }
    @Test fun patterns_changes_light() {
        light()
        // 21 whole days: nights of 9h, then the last week 10h30 with fewer wakings: "What changed" appears.
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val synthetic = buildList {
            for (back in 0..22) {
                val d = today.minusDays(back.toLong())
                val hours = if (back <= 7) 10.5 else 9.0
                val start = d.minusDays(1).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
                add(Event(type = EventType.SLEEP, start = start, end = start + (hours * 3_600_000).toLong()))
                if (back > 0) for (i in 0..1) add(Event(type = EventType.SLEEP, start = d.atTime(10 + 3 * i, 0).atZone(zone).toInstant().toEpochMilli(), end = d.atTime(11 + 3 * i, 0).atZone(zone).toInstant().toEpochMilli()))
                add(Event(type = EventType.PUMP, start = d.atTime(if (back % 2 == 0) 22 else 9, 0).atZone(zone).toInstant().toEpochMilli(), end = d.atTime(if (back % 2 == 0) 22 else 9, 15).atZone(zone).toInstant().toEpochMilli(), amountMl = 90))
            }
        }
        paparazzi.snapshot { Chrome(false, 3, synthetic) }
    }
    @Test fun day_today_expected_light() {
        light()
        paparazzi.snapshot {
            CochiloTheme(false) {
                br.gabriel.cochilo.ui.kit.Frame {
                    val p = Predictor(zone).predict(log, ageWeeks(baby.birthDate, null, now), now) as Predictor.Result.Awake
                    DayScreen(log, now, zone, onOpen = {}, expected = p.windowStart to p.windowEnd)
                }
            }
        }
    }
    @Test fun settings_light() { light(); paparazzi.snapshot { Chrome(false, 0, settings = true) } }
    @Test fun today_fabric_balao_light() { light(); paparazzi.snapshot { Fabric(br.gabriel.cochilo.ui.kit.Fabric.BALAO) { Chrome(false, 0) } } }
    @Test fun today_sleeping_fabric_tropical_dark() { dark(); paparazzi.snapshot { Fabric(br.gabriel.cochilo.ui.kit.Fabric.TROPICAL) { Chrome(true, 0, sleepingLog) } } }
    /** The fabric group with Rosa Chá picked: the quiet line to the shop under the swatches (in-app-marketing.md §3.3). */
    @Test fun fabric_shop_line_ptBR_light() {
        pt(false)
        paparazzi.snapshot {
            CochiloTheme(false) { br.gabriel.cochilo.ui.kit.Frame { Column(Modifier.padding(CTokens.Spacing.space6)) {
                br.gabriel.cochilo.ui.kit.RowGroup { Column(Modifier.padding(horizontal = CTokens.Spacing.space4, vertical = CTokens.Spacing.space3)) {
                    br.gabriel.cochilo.ui.kit.FabricPicker(br.gabriel.cochilo.ui.kit.Fabric.ROSA_CHA) {}
                    br.gabriel.cochilo.ui.kit.QuietLink(
                        stringResource(R.string.fabric_in_shop_line), stringResource(R.string.fabric_in_shop_action),
                        stringResource(R.string.fabric_in_shop_spoken, "Rosa Chá"), onClick = {},
                    )
                } }
            } } }
        }
    }
    @Test fun fabric_picker_ptBR_light() { pt(false); paparazzi.snapshot { Fabric(br.gabriel.cochilo.ui.kit.Fabric.ROSA_CHA) { Chrome(false, 0, settings = true) } } }

    /** The fabric she picked, as MainActivity provides it. */
    @Composable
    private fun Fabric(f: br.gabriel.cochilo.ui.kit.Fabric, content: @Composable () -> Unit) =
        androidx.compose.runtime.CompositionLocalProvider(br.gabriel.cochilo.ui.kit.LocalFabric provides f, content = content)
    /** Settings › About in the github build, after a manual check. */
    @Test fun about_updates_light() {
        light()
        paparazzi.snapshot {
            CochiloTheme(false) {
                Frame {
                    Column(Modifier.fillMaxSize().padding(CTokens.Spacing.space4)) {
                        Caption(stringResource(R.string.caption_about), top = CTokens.Spacing.space1)
                        RowGroup {
                            ListRow(stringResource(R.string.privacy), meta = stringResource(R.string.privacy_meta))
                            Hairline()
                            ListRow(stringResource(R.string.version), value = "0.2.0")
                            Hairline()
                            CheckForUpdates(checking = false, result = stringResource(R.string.update_latest, "0.2.0"), releaseOnlyNote = null) {}
                        }
                    }
                }
            }
        }
    }
    @Test fun update_sheet_light() {
        light()
        paparazzi.snapshot { UpdateSheetShot(false, null, "Cochilo now updates itself: it tells you when a new version is out and installs it after Android asks.") }
    }
    @Test fun update_sheet_downloading_ptBR_dark() {
        pt(true)
        paparazzi.snapshot { UpdateSheetShot(true, 0.41f, "O Cochilo agora se atualiza sozinho: avisa quando sai uma versão nova e instala depois que o Android pergunta.") }
    }
    @Test fun empty_today_light() { light(); paparazzi.snapshot { Chrome(false, 0, emptyList()) } }
}
