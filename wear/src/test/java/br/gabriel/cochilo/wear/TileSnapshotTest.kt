package br.gabriel.cochilo.wear

import android.widget.FrameLayout
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.renderer.TileRenderer
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/** The tile as the watch draws it (docs/wear.md): awake, asleep, feeding, and with no phone. */
class TileSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND, theme = "android:Theme.DeviceDefault")

    private val since = 1_793_800_000_000L

    private fun snap(s: WearWire.State?) {
        val ctx = paparazzi.context
        val device = DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(192).setScreenHeightDp(192)
            .setScreenDensity(ctx.resources.displayMetrics.density)
            .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
            .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
            .build()
        val layout = LayoutElementBuilders.Layout.Builder().setRoot(TileLayout(ctx).of(device, s, true)).build()
        val parent = FrameLayout(ctx)
        TileRenderer(ctx, { it.run() }, {}).inflateAsync(layout, ResourceBuilders.Resources.Builder().setVersion("1").build(), parent)
            .get(10, TimeUnit.SECONDS)
        paparazzi.snapshot(parent)
    }

    @Test fun tile_awake() = snap(WearWire.State(true, false, since, null, CochiloTile.LEFT, false))
    @Test fun tile_asleep() = snap(WearWire.State(true, true, since, null, CochiloTile.RIGHT, false))
    @Test fun tile_feeding() = snap(WearWire.State(true, false, since, CochiloTile.LEFT, CochiloTile.LEFT, false))
    @Test fun tile_no_phone() = snap(null)
    /** Portuguese, the longer words (0.56.0). */
    @Test fun tile_feeding_ptBR() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.WEAR_OS_SMALL_ROUND.copy(locale = "pt-rBR"))
        snap(WearWire.State(true, false, since, CochiloTile.RIGHT, CochiloTile.RIGHT, false))
    }
    @Test fun tile_awake_ptBR() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.WEAR_OS_SMALL_ROUND.copy(locale = "pt-rBR"))
        snap(WearWire.State(true, false, since, null, CochiloTile.RIGHT, true))
    }
}
