package br.gabriel.cochilo.wear

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.graphics.toArgb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import br.gabriel.cochilo.ui.theme.CTokens
import java.util.Date

/** The tile's layout for a state (0.53.0, docs/wear.md); also drawn by the screenshot test. */
class TileLayout(private val context: Context) {
    private val c = CTokens.Dark

    fun of(device: DeviceParameters, s: WearWire.State?, near: Boolean): LayoutElementBuilders.LayoutElement = when {
        s == null -> message(device, context.getString(R.string.no_phone))
        !s.hasBaby -> message(device, context.getString(R.string.no_baby))
        else -> main(device, s)
    }

    private fun message(device: DeviceParameters, text: String): LayoutElementBuilders.LayoutElement =
        PrimaryLayout.Builder(device)
            .setContent(Text.Builder(context, text).setTypography(Typography.TYPOGRAPHY_BODY1).setColor(argb(c.ink.toArgb())).setMaxLines(3).build())
            .build()

    private fun main(device: DeviceParameters, s: WearWire.State): LayoutElementBuilders.LayoutElement {
        val time = s.since?.let { DateFormat.getTimeFormat(context).format(Date(it)) }
        val status = when {
            time == null -> context.getString(R.string.app_name)
            s.asleep -> context.getString(R.string.asleep_since, time)
            else -> context.getString(R.string.awake_since, time)
        }
        val (action, label) = when {
            s.asleep -> WearWire.Action.WOKE_UP to R.string.woke_up
            s.nightWaking -> WearWire.Action.START_SLEEP to R.string.back_to_sleep
            else -> WearWire.Action.START_SLEEP to R.string.start_sleep
        }
        // The one mint action: the design's mint fill with onMint text.
        val sleep = Chip.Builder(context, click(action.name), device)
            .setPrimaryLabelContent(context.getString(label))
            // Inside the round screen's safe area.
            .setWidth(dp(device.screenWidthDp * 0.72f))
            .setChipColors(ChipColors(argb(c.mint.toArgb()), argb(c.onMint.toArgb())))
            .build()
        val breasts = LayoutElementBuilders.Row.Builder()
            .addContent(breast(device, s, CochiloTile.LEFT, R.string.left, WearWire.Action.FEED_LEFT))
            .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(8f)).build())
            .addContent(breast(device, s, CochiloTile.RIGHT, R.string.right, WearWire.Action.FEED_RIGHT))
            .build()
        return PrimaryLayout.Builder(device)
            .setPrimaryLabelTextContent(Text.Builder(context, status).setTypography(Typography.TYPOGRAPHY_CAPTION1).setColor(argb(c.ink.toArgb())).build())
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(sleep)
                    .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())
                    .addContent(breasts)
                    .build()
            )
            .build()
    }

    /**
     * One breast. Feeding on it now: "Finish" in the feed colour. The one used last: a dot before
     * its name (the Last mark; "last" is only spoken). Otherwise a quiet outline-like surface chip.
     */
    private fun breast(device: DeviceParameters, s: WearWire.State, side: String, name: Int, start: WearWire.Action): LayoutElementBuilders.LayoutElement {
        val feedingHere = s.feeding == side
        val text = when {
            feedingHere -> context.getString(R.string.finish_side, context.getString(name))
            s.feeding == null && s.lastBreast == side -> "• " + context.getString(name)
            else -> context.getString(name)
        }
        val colors = if (feedingHere) ChipColors(argb(c.eventFeed.toArgb()), argb(c.page.toArgb()))
        else ChipColors(argb(c.surface.toArgb()), argb(c.ink.toArgb()))
        return CompactChip.Builder(context, text, click(if (feedingHere) WearWire.Action.FINISH_FEED.name else start.name), device)
            .setChipColors(colors)
            .build()
    }

    private fun click(id: String) = ModifiersBuilders.Clickable.Builder()
        .setId(id)
        .setOnClick(ActionBuilders.LoadAction.Builder().build())
        .build()

}
