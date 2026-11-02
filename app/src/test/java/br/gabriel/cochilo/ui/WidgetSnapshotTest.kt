package br.gabriel.cochilo.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.toArgb
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.WidgetState
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.widget.CochiloWidget
import com.android.resources.NightMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * The home-screen widget (docs/widget.md): the real RemoteViews, applied and drawn on a
 * wallpaper-like page, so what ships is what's kept. And its colours, copied into colors.xml
 * because a RemoteViews layout can't read CTokens, checked against the tokens.
 */
class WidgetSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 0.1)

    private val now = 1_800_000_000_000L
    private val min = 60_000L

    private fun shot(s: WidgetState, dark: Boolean, locale: String? = null) {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = if (dark) NightMode.NIGHT else NightMode.NOTNIGHT, locale = locale))
        val ctx = paparazzi.context
        val page = FrameLayout(ctx).apply {
            setBackgroundColor((if (dark) CTokens.Dark.page else CTokens.Light.page).toArgb())
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val widget = android.view.LayoutInflater.from(ctx).inflate(br.gabriel.cochilo.R.layout.widget_cochilo, page, false)
        CochiloWidget.bind(ctx, s, now, ViewTarget(widget))
        // About a 4 × 2 widget on a phone: the full width, 110dp (the provider's minimum height).
        val h = (110 * ctx.resources.displayMetrics.density).toInt()
        page.addView(widget, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h))
        paparazzi.snapshot(page)
    }

    @Test fun widget_awake_last_left_light() = shot(
        WidgetState(hasBaby = true, asleep = false, since = now - 40 * min, feeding = null, lastBreast = Kinds.BREAST_LEFT), dark = false,
    )

    @Test fun widget_asleep_dark() = shot(
        WidgetState(hasBaby = true, asleep = true, since = now - 65 * min, feeding = null, lastBreast = Kinds.BREAST_RIGHT), dark = true,
    )

    @Test fun widget_feeding_right_light() = shot(
        WidgetState(
            hasBaby = true, asleep = false, since = now - 30 * min,
            feeding = Event(type = EventType.FEED, start = now - 8 * min, kind = Kinds.BREAST_RIGHT), lastBreast = Kinds.BREAST_RIGHT,
        ), dark = false,
    )

    @Test fun widget_awake_ptBR_light() = shot(
        WidgetState(hasBaby = true, asleep = false, since = now - 40 * min, feeding = null, lastBreast = Kinds.BREAST_RIGHT), dark = false, locale = "pt-rBR",
    )

    @Test fun widget_no_baby_light() = shot(WidgetState(false, false, null, null, null), dark = false)

    @Test fun colorsMatchTheTokens() {
        fun colours(dir: String): Map<String, String> = Regex("""<color name="(widget_[a-z_]+)">#([0-9A-F]{6})</color>""")
            .findAll(File("src/main/res/$dir/colors.xml").readText()).associate { it.groupValues[1] to it.groupValues[2] }
        fun hex(c: androidx.compose.ui.graphics.Color) = "%06X".format(c.toArgb() and 0xFFFFFF)
        for ((dir, p) in listOf("values" to CTokens.Light, "values-night" to CTokens.Dark)) {
            val x = colours(dir)
            assertEquals(dir, hex(p.surface), x["widget_surface"])
            assertEquals(dir, hex(p.ink), x["widget_ink"])
            assertEquals(dir, hex(p.muted), x["widget_muted"])
            assertEquals(dir, hex(p.line), x["widget_line"])
            assertEquals(dir, hex(p.mint), x["widget_mint"])
            assertEquals(dir, hex(p.onMint), x["widget_on_mint"])
            assertEquals(dir, hex(p.eventFeed), x["widget_feed"])
            assertEquals(dir, hex(p.eventFeedSoft), x["widget_feed_soft"])
        }
    }
}

/** Fills the inflated layout the way RemoteViews would on the home screen. */
private class ViewTarget(val root: android.view.View) : br.gabriel.cochilo.widget.WidgetTarget {
    private fun <T : android.view.View> v(id: Int): T = root.findViewById(id)
    override fun text(id: Int, text: CharSequence) { v<android.widget.TextView>(id).text = text }
    override fun visible(id: Int, shown: Boolean) { v<android.view.View>(id).visibility = if (shown) android.view.View.VISIBLE else android.view.View.GONE }
    override fun background(id: Int, res: Int) { v<android.view.View>(id).setBackgroundResource(res) }
    override fun describe(id: Int, words: String) { v<android.view.View>(id).contentDescription = words }
    override fun clock(id: Int, elapsed: Long?) {
        val c = v<android.widget.Chronometer>(id)
        if (elapsed == null) { c.visibility = android.view.View.GONE; return }
        c.text = android.text.format.DateUtils.formatElapsedTime(elapsed / 1000)
    }
    override fun click(id: Int, intent: () -> android.app.PendingIntent) = Unit
}
