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
            // The round screen's chord at this height is about 98% of its width; 84% leaves a margin,
            // so "Voltou a dormir" fits on one line. Centred, as Wear's primary chip is.
            .setWidth(dp(device.screenWidthDp * WIDE))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setChipColors(ChipColors(argb(c.mint.toArgb()), argb(c.onMint.toArgb())))
            .build()
        val breasts = LayoutElementBuilders.Row.Builder()
            .addContent(breast(device, s, CochiloTile.LEFT, R.string.left, WearWire.Action.FEED_LEFT))
            .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(8f)).build())
            .addContent(breast(device, s, CochiloTile.RIGHT, R.string.right, WearWire.Action.FEED_RIGHT))
            .build()
        // The status sits in the content, on up to two lines, inside the circle's chord near the
        // top ("Acordado desde 13:46" doesn't fit one line on a small watch).
        val statusText = LayoutElementBuilders.Box.Builder().setWidth(dp(device.screenWidthDp * 0.80f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(Text.Builder(context, status).setTypography(Typography.TYPOGRAPHY_CAPTION1).setColor(argb(c.ink.toArgb())).setMaxLines(2).build())
            .build()
        return PrimaryLayout.Builder(device)
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(statusText)
                    .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
                    .addContent(sleep)
                    .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
                    .addContent(breasts)
                    .build()
            )
            .build()
    }

    /**
     * One breast (0.56.0). Feeding on it now: "Finish Left" on the feed colour. The one used last:
     * the Last mark, as on the phone: an outline and a drawn dot in the feed colour ("last" is
     * only spoken). Otherwise a quiet surface pill. At least 48dp to tap.
     */
    private fun breast(device: DeviceParameters, s: WearWire.State, side: String, name: Int, start: WearWire.Action): LayoutElementBuilders.LayoutElement {
        val feedingHere = s.feeding == side
        val last = !feedingHere && s.feeding == null && s.lastBreast == side
        // Feeding: the side's chip reads "Finish"; where it sits and its colour say which side.
        val label = if (feedingHere) context.getString(R.string.finish) else context.getString(name)
        val full = if (feedingHere) context.getString(R.string.finish_side, context.getString(name)) else label
        val fill = if (feedingHere) c.eventFeed else c.surface
        val ink = if (feedingHere) c.page else c.ink
        val spoken = if (last) context.getString(R.string.last_spoken, full) else full
        val text = LayoutElementBuilders.Text.Builder().setText(label).setMaxLines(1)
            .setFontStyle(LayoutElementBuilders.FontStyle.Builder().setSize(androidx.wear.protolayout.DimensionBuilders.sp(14f))
                .setWeight(LayoutElementBuilders.FONT_WEIGHT_NORMAL).setColor(argb(ink.toArgb())).build())
            .build()
        val row = LayoutElementBuilders.Row.Builder().setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        if (last) {
            // The Last dot: a drawn circle, not a character.
            row.addContent(LayoutElementBuilders.Box.Builder().setWidth(dp(6f)).setHeight(dp(6f))
                .setModifiers(ModifiersBuilders.Modifiers.Builder().setBackground(
                    ModifiersBuilders.Background.Builder().setColor(argb(c.eventFeed.toArgb()))
                        .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(3f)).build()).build()).build())
                .build())
            row.addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(6f)).build())
        }
        row.addContent(text)
        val modifiers = ModifiersBuilders.Modifiers.Builder()
            .setClickable(click(if (feedingHere) WearWire.Action.FINISH_FEED.name else start.name))
            .setSemantics(ModifiersBuilders.Semantics.Builder().setContentDescription(spoken).setRole(ModifiersBuilders.SEMANTICS_ROLE_BUTTON).build())
            .setBackground(ModifiersBuilders.Background.Builder().setColor(argb(fill.toArgb()))
                .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(18f)).build()).build())
            .setPadding(ModifiersBuilders.Padding.Builder().setStart(dp(4f)).setEnd(dp(4f)).build())
        if (last) modifiers.setBorder(ModifiersBuilders.Border.Builder().setWidth(dp(1.5f)).setColor(argb(c.eventFeed.toArgb())).build())
        // Both breasts share the row equally, inside the round screen.
        return LayoutElementBuilders.Box.Builder()
            .setWidth(dp((device.screenWidthDp * WIDE - 8f) / 2f))
            .setHeight(dp(36f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(modifiers.build())
            .addContent(row.build())
            .build()
    }

    // Every target at least 48dp, whatever its drawn size.
    private fun click(id: String) = ModifiersBuilders.Clickable.Builder()
        .setId(id)
        .setMinimumClickableWidth(dp(48f)).setMinimumClickableHeight(dp(48f))
        .setOnClick(ActionBuilders.LoadAction.Builder().build())
        .build()


    private companion object {
        /** How much of the screen's width the chips use: the round screen is about 92% wide at the breasts' row. */
        const val WIDE = 0.84f
    }
}
