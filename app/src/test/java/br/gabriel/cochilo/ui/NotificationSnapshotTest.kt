package br.gabriel.cochilo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.LiveStretch
import br.gabriel.cochilo.notify.TimelineBar
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import com.android.resources.NightMode
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The live notification as it looks in the shade (docs/notifications.md). The real notification is
 * a RemoteViews layout; this draws the same parts (the real TimelineBar bitmap, title, line, button)
 * on a shade-like card so the design can be seen and kept.
 */
class NotificationSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 0.1)

    private val zone = ZoneId.systemDefault()
    private fun at(h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, 2, h, m).atZone(zone).toInstant().toEpochMilli()

    private val awake = LiveStretch(
        asleep = false, isNight = false, since = at(14, 30), expectedEnd = at(16, 1),
        windowStart = at(15, 51), windowEnd = at(16, 11),
        points = listOf(
            Event(type = EventType.FEED, start = at(14, 35), end = at(14, 55), kind = Kinds.BREAST_LEFT),
            Event(type = EventType.DIAPER, start = at(15, 0), kind = Kinds.WET),
        ),
    )
    private val asleep = LiveStretch(asleep = true, isNight = false, since = at(16, 10), expectedEnd = at(16, 47))

    @Composable
    private fun Card(s: LiveStretch, now: Long, title: String, line: String, action: String, dark: Boolean) {
        val c = if (dark) CTokens.Dark else CTokens.Light
        val shade = if (dark) androidx.compose.ui.graphics.Color(0xFF2B2B2E) else androidx.compose.ui.graphics.Color(0xFFF1F1F4)
        val bar = TimelineBar.draw(s, c, zone, now, 900, 48).asImageBitmap()
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(CTokens.Radius.radiusL)).background(shade)
                .padding(CTokens.Spacing.space4),
        ) {
            Text(stringResource(R.string.app_name) + " · 0:40", style = Type.meta, color = c.muted)
            Spacer(Modifier.height(CTokens.Spacing.space2))
            Text(title, style = Type.label, color = c.ink)
            Text(line, style = Type.bodySmall, color = c.ink)
            Spacer(Modifier.height(CTokens.Spacing.space2))
            Image(bar, null, Modifier.fillMaxWidth().height(CTokens.Spacing.space4), contentScale = ContentScale.FillBounds)
            Spacer(Modifier.height(CTokens.Spacing.space3))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Text(action, style = Type.label, color = c.ink) }
        }
    }

    @Composable
    private fun Shade(dark: Boolean) {
        val f = Format(LocalContext.current, zone, at(16, 20))
        CochiloTheme(dark) {
            Column(
                Modifier.fillMaxSize().background(if (dark) androidx.compose.ui.graphics.Color(0xFF111113) else androidx.compose.ui.graphics.Color(0xFFDADADF))
                    .padding(CTokens.Spacing.space4),
                verticalArrangement = Arrangement.spacedBy(CTokens.Spacing.space3),
            ) {
                Card(
                    asleep, at(16, 20), stringResource(R.string.sleep_nap),
                    stringResource(R.string.notif_asleep_expected, f.time(at(16, 10)), f.time(at(16, 47))),
                    stringResource(R.string.action_woke_up), dark,
                )
                Card(
                    awake, at(15, 20), stringResource(R.string.notif_awake_title),
                    stringResource(R.string.notif_awake_nap, f.time(at(14, 30)), f.time(at(16, 1))),
                    stringResource(R.string.action_start_sleep), dark,
                )
            }
        }
    }

    @Test fun notification_light() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NOTNIGHT))
        paparazzi.snapshot { Shade(false) }
    }

    @Test fun notification_ptBR_dark() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(locale = "pt-rBR", nightMode = NightMode.NIGHT))
        paparazzi.snapshot { Shade(true) }
    }
}
