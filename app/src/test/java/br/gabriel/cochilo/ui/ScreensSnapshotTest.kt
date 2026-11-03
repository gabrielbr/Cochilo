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
import br.gabriel.cochilo.ui.screens.QuickLog
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
                Column(Modifier.fillMaxSize()) {
                    if (settings) {
                        BackHeader(stringResource(R.string.title_settings), stringResource(R.string.back)) {}
                        Box(Modifier.weight(1f)) { SettingsScreen(baby, ThemeMode.SYSTEM, zone, {}, {}, {}, {}, {}, {}, fabric = br.gabriel.cochilo.ui.kit.LocalFabric.current) }
                        return@Column
                    }
                    val f = Format(androidx.compose.ui.platform.LocalContext.current, zone, now)
                    val status = when (p) {
                        is Predictor.Result.Sleeping -> stringResource(R.string.status_sleeping, f.duration(now - p.since))
                        is Predictor.Result.Awake -> stringResource(R.string.status_awake, f.duration(now - p.since))
                        else -> stringResource(R.string.status_weeks, weeks)
                    }
                    Header(baby.name, status) { IconButton(R.drawable.gg_ic_gear, stringResource(R.string.settings), {}) }
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> TodayScreen(log, p, now, zone, {}, {}, {}, {}, {})
                            1 -> DayScreen(log, now, zone, onOpen = {}, initialDay = LocalDateTime.of(2026, 10, 30, 12, 0).toLocalDate())
                            2 -> HistoryScreen(log, now, zone) {}
                            else -> PatternsScreen(log, now, zone, weeks)
                        }
                        LogButton(
                            logMenuItems(androidx.compose.ui.platform.LocalContext.current, f, log, p is Predictor.Result.Sleeping),
                            stringResource(R.string.log_button), stringResource(R.string.log_close),
                            {}, { _, _ -> }, preview = logHover,
                        )
                    }
                    TabBar(
                        listOf(
                            Tab(stringResource(R.string.tab_today), R.drawable.gg_ic_home),
                            Tab(stringResource(R.string.tab_day), R.drawable.gg_ic_day),
                            Tab(stringResource(R.string.tab_history), R.drawable.gg_ic_list),
                            Tab(stringResource(R.string.tab_patterns), R.drawable.gg_ic_chart),
                        ), tab,
                    ) {}
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

    /** What was logged last of each type, as Today passes it (remember actions). */
    private val lastEntries get() = EventType.entries.associateWith { t -> log.filter { it.type == t }.maxByOrNull { it.start } }

    private val lastKinds get() = EventType.entries.associateWith { t ->
        br.gabriel.cochilo.domain.lastKindAmong(log, t, br.gabriel.cochilo.ui.screens.slideKinds(t))
    }

    private val sleepingLog get() = log + Event(type = EventType.SLEEP, start = at(31, 14, 50))

    private fun light() = paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NOTNIGHT))
    private fun dark() = paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))
    private fun pt(night: Boolean) = paparazzi.unsafeUpdateConfig(
        DeviceConfig.PIXEL_5.copy(locale = "pt-rBR", nightMode = if (night) NightMode.NIGHT else NightMode.NOTNIGHT)
    )

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
                        VerticalTimeline(log.filter { it.overlaps(from, to, now) }.sortedBy { it.start }, now, f, zone) {}
                    }
                }
            }
        }
    }
    /** The log buttons, each with its "i", in Portuguese (the longer labels). */
    @Test fun quick_log_ptBR_light() {
        pt(false)
        paparazzi.snapshot {
            CochiloTheme(false) {
                Frame { Column(Modifier.fillMaxSize().padding(CTokens.Spacing.space4)) { QuickLog({}, last = lastEntries, lastKinds = lastKinds) } }
            }
        }
    }
    /** Mid-slide on Feed: the row has become Left | Right, with Right under the finger. */
    @Test fun quick_log_sliding_light() {
        light()
        paparazzi.snapshot {
            CochiloTheme(false) {
                Frame {
                    Column(Modifier.fillMaxSize().padding(CTokens.Spacing.space4)) {
                        QuickLog({}, last = lastEntries, lastKinds = lastKinds, previewActive = EventType.FEED to 1)
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
