package br.gabriel.cochilo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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
import br.gabriel.cochilo.ui.kit.Frame
import br.gabriel.cochilo.ui.kit.Header
import br.gabriel.cochilo.ui.kit.IconButton
import br.gabriel.cochilo.ui.kit.Tab
import br.gabriel.cochilo.ui.kit.TabBar
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
    ) {
        CochiloTheme(dark) {
            val weeks = ageWeeks(baby.birthDate, null, now)
            val p = Predictor(zone).predict(log, weeks, now)
            Frame {
                Column(Modifier.fillMaxSize()) {
                    if (settings) {
                        BackHeader(stringResource(R.string.title_settings), stringResource(R.string.back)) {}
                        Box(Modifier.weight(1f)) { SettingsScreen(baby, ThemeMode.SYSTEM, zone, {}, {}, {}, {}, {}) }
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
                            1 -> HistoryScreen(log, now, zone) {}
                            else -> PatternsScreen(log, now, zone, weeks)
                        }
                    }
                    TabBar(
                        listOf(
                            Tab(stringResource(R.string.tab_today), R.drawable.gg_ic_home),
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
    @Test fun history_light() { light(); paparazzi.snapshot { Chrome(false, 1) } }
    @Test fun patterns_light() { light(); paparazzi.snapshot { Chrome(false, 2) } }
    @Test fun patterns_dark() { dark(); paparazzi.snapshot { Chrome(true, 2) } }
    @Test fun patterns_ptBR_dark() { pt(true); paparazzi.snapshot { Chrome(true, 2) } }
    @Test fun settings_light() { light(); paparazzi.snapshot { Chrome(false, 0, settings = true) } }
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
