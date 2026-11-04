package br.gabriel.cochilo.wear

import android.text.format.DateFormat
import androidx.compose.ui.graphics.toArgb
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import br.gabriel.cochilo.ui.theme.CTokens
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Cochilo's tile (0.53.0, docs/wear.md): asleep or awake and since when, one big button (Start
 * sleep / Woke up, or Back to sleep in a night waking) and the two breasts below it. A tap goes to
 * the phone, which logs it through the same rules as every other button. The colours are the
 * design's own (CTokens, the dark theme: a watch face is dark).
 */
class CochiloTile : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onTileRequest(req: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        CallbackToFutureAdapter.getFuture { done ->
            scope.launch {
                val tapped = req.currentState.lastClickableId.takeIf { it.isNotEmpty() }
                    ?.let { id -> WearWire.Action.entries.firstOrNull { it.name == id } }
                var near = true
                if (tapped != null) {
                    near = Watch.send(this@CochiloTile, WearWire.ACTION, tapped.name.toByteArray())
                    // Shown at once; the phone's answer replaces it a moment later.
                    if (near) Watch.state(this@CochiloTile)?.let { Watch.save(this@CochiloTile, expect(it, tapped)) }
                } else if (Watch.state(this@CochiloTile) == null) {
                    near = Watch.send(this@CochiloTile, WearWire.ASK)
                }
                done.set(tile(req.deviceConfiguration, Watch.state(this@CochiloTile), near))
            }
            "cochilo-tile"
        }

    override fun onTileResourcesRequest(req: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        CallbackToFutureAdapter.getFuture { done -> done.set(ResourceBuilders.Resources.Builder().setVersion(RES).build()); "res" }

    /** What the tap will do, so the tile answers before the phone does. */
    private fun expect(s: WearWire.State, a: WearWire.Action): WearWire.State {
        val now = System.currentTimeMillis()
        return when (a) {
            WearWire.Action.START_SLEEP -> s.copy(asleep = true, since = now, nightWaking = false)
            WearWire.Action.WOKE_UP -> s.copy(asleep = false, since = now)
            WearWire.Action.FEED_LEFT -> s.copy(feeding = LEFT, lastBreast = LEFT)
            WearWire.Action.FEED_RIGHT -> s.copy(feeding = RIGHT, lastBreast = RIGHT)
            WearWire.Action.FINISH_FEED -> s.copy(feeding = null)
        }
    }

    private fun tile(device: DeviceParameters, s: WearWire.State?, near: Boolean): TileBuilders.Tile =
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RES)
            // "Since 14:20" doesn't tick, but it's redrawn each minute and after every change.
            .setFreshnessIntervalMillis(60_000)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(TileLayout(this).of(device, s, near)))
            .build()

    companion object {
        private const val RES = "1"
        const val LEFT = "BREAST_LEFT"
        const val RIGHT = "BREAST_RIGHT"
    }
}
