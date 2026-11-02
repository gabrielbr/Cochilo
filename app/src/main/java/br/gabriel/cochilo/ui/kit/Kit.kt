@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package br.gabriel.cochilo.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.SystemEasing
import br.gabriel.cochilo.ui.theme.Type

// The Cochilo kit: the Gabriel Design System's components in Compose (docs/components.md there).
// Built from CTokens only; no literal dp, sp or colour below this line.

/** Press scales to 97% under the finger; with animations off it doesn't move. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val reduce = CochiloTheme.reduceMotion
    val s by animateFloatAsState(
        targetValue = if (pressed && !reduce) 0.97f else 1f,
        animationSpec = tween(CTokens.Motion.motionStandard, easing = SystemEasing),
        label = "press",
    )
    return this.scale(s)
}

/** The signature: a 4dp mint border round every screen. */
@Composable
fun Frame(content: @Composable () -> Unit) {
    val c = CochiloTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.page)
            // A soft hem round the screen (frame-line: Rosa Chá under the brand), not a teal rule.
            .border(Size.frame, c.frameLine)
            .padding(Size.frame)
    ) { content() }
}

@Composable
fun GIcon(@DrawableRes id: Int, contentDescription: String?, tint: Color = CochiloTheme.colors.ink, modifier: Modifier = Modifier) {
    // The line set is drawn at 22dp; its vectors carry that size, so no size here.
    Icon(painterResource(id), contentDescription, modifier, tint = tint)
}

/** 48dp target round a 22dp line icon. */
@Composable
fun IconButton(@DrawableRes id: Int, contentDescription: String, onClick: () -> Unit, tint: Color = CochiloTheme.colors.ink) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(Spacing.space12)
            .clip(RoundedCornerShape(Radius.radiusS))
            .clickable(src, indication = null, role = Role.Button, onClick = onClick)
            .pressScale(src),
        contentAlignment = Alignment.Center,
    ) { GIcon(id, contentDescription, tint) }
}

/**
 * The app's mark in the header: Cochilo, da Tatá Slings, a crescent moon resting in a sling's curve
 * on a teal tile (the design system's assets/logos/cochilo-tata-mark.svg). Decorative: the app's
 * name sits beside it.
 */
@Composable
fun Monogram() {
    androidx.compose.foundation.Image(
        painterResource(br.gabriel.cochilo.R.drawable.mark_cochilo), contentDescription = null,
        modifier = Modifier.size(Size.monogram),
    )
}

/** Header: monogram, app name over one status line, one trailing icon. */
@Composable
fun Header(name: String, status: String, statusColor: Color = CochiloTheme.colors.muted, trailing: @Composable () -> Unit) {
    val c = CochiloTheme.colors
    Row(
        Modifier.fillMaxWidth().height(Size.header).background(c.surface).padding(start = Spacing.space4, end = Spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Monogram()
        Spacer(Modifier.width(Spacing.space3))
        Column(Modifier.weight(1f)) {
            Text(name, style = Type.label, color = c.ink, maxLines = 1)
            Text(
                status, style = Type.meta, color = statusColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        trailing()
    }
}

/** A pushed screen's header: back, then the screen name. No tabs below it. */
@Composable
fun BackHeader(title: String, backLabel: String, onBack: () -> Unit) {
    val c = CochiloTheme.colors
    Row(
        Modifier.fillMaxWidth().height(Size.header).background(c.surface).padding(horizontal = Spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(br.gabriel.cochilo.R.drawable.gg_ic_back, backLabel, onBack)
        Spacer(Modifier.width(Spacing.space2))
        Text(title, style = Type.label, color = c.ink, modifier = Modifier.semantics { heading() })
    }
}

/**
 * Screen title: the display role in ink, plain. Cochilo dropped the system's mint highlighter
 * (0.11.0, docs/design-review.md): a teal block behind the word read loud and technical; the
 * title now speaks by size and weight alone. [highlight] is kept for callers and ignored.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun ScreenTitle(text: String, highlight: String = text) {
    Text(text, style = Type.display, color = CochiloTheme.colors.ink, modifier = Modifier.semantics { heading() })
}

/** Section caption: 13/18 semibold, sentence case, muted; 40 above, 12 below, no divider (0.11.0: no caps). */
@Composable
fun Caption(text: String, top: Dp = Spacing.space10) {
    Text(
        text, style = Type.caption, color = CochiloTheme.colors.muted,
        modifier = Modifier.padding(top = top, bottom = Spacing.space3).semantics { heading() },
    )
}

/** Rows that follow each other: one group on surface, radius 8, hairlines between rows. */
@Composable
fun RowGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        // Grows with its rows: a new entry slides the list open instead of jumping in.
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.radiusM)).background(CochiloTheme.colors.surface).growsWithContent(),
        content = content,
    )
}

@Composable
fun Hairline() {
    Box(Modifier.fillMaxWidth().padding(start = Spacing.space4).height(Size.hairline).background(CochiloTheme.colors.line))
}

/**
 * A row: label and at most one meta line; one thing on the right. A [prominent] row (sleep, what
 * Cochilo is for) has its label in the label role and a little more air. [last] is what was done
 * last of this kind, shown after a Last dot in [lastColor]; [lastSpoken] is what TalkBack says.
 */
@Composable
fun ListRow(
    label: String,
    meta: String? = null,
    value: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    prominent: Boolean = false,
    last: String? = null,
    lastColor: Color = CochiloTheme.colors.muted,
    lastSpoken: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = CochiloTheme.colors
    val src = remember { MutableInteractionSource() }
    val base = Modifier.fillMaxWidth().heightIn(min = Size.rowMin)
    val m = if (onClick != null) base.clickable(src, indication = null, role = Role.Button, onClick = onClick).pressScale(src) else base
    val vertical = if (prominent) Spacing.space4 else Spacing.space3
    Row(m.padding(horizontal = Spacing.space4, vertical = vertical), verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Spacing.space3))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = if (prominent) Type.label else Type.body, color = c.ink)
            if (meta != null) Text(meta, style = Type.meta, color = c.muted)
            if (last != null) LastLine(last, lastColor, lastSpoken)
        }
        if (value != null) {
            Spacer(Modifier.width(Spacing.space2))
            Text(value, style = Type.bodySmall, color = c.muted)
        }
        if (trailing != null) {
            Spacer(Modifier.width(Spacing.space2))
            trailing()
        }
    }
}

/**
 * Last mark (design system, components.md): a small dot in the kind's colour. Before an option it
 * says "this one last time"; the word is only spoken (TalkBack), never written.
 */
@Composable
fun LastDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(Spacing.space2).clip(RoundedCornerShape(Radius.radiusRound)).background(color))
}

/** "What was done last" as a line: the Last dot, then the words (right breast · 14:20). */
@Composable
fun LastLine(text: String, color: Color, spoken: String? = null, style: androidx.compose.ui.text.TextStyle = Type.meta) {
    val c = CochiloTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (spoken != null) Modifier.semantics(mergeDescendants = true) { contentDescription = spoken } else Modifier,
    ) {
        LastDot(color)
        Spacer(Modifier.width(Spacing.space1))
        Text(text, style = style, color = c.ink)
    }
}

/** A switch row: the whole row toggles. */
@Composable
fun SwitchRow(label: String, meta: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = CochiloTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = Size.rowMin)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = Spacing.space4, vertical = Spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Type.body, color = c.ink)
            if (meta != null) Text(meta, style = Type.meta, color = c.muted)
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = null)
    }
}

enum class ButtonKind { PRIMARY, SECONDARY, DANGER }

/**
 * 52dp, radius 4, label 16 bold. PRIMARY is mint (one per screen); SECONDARY an ink outline;
 * DANGER a coral-text outline, always behind a confirm.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    kind: ButtonKind = ButtonKind.SECONDARY,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val c = CochiloTheme.colors
    val src = remember { MutableInteractionSource() }
    val (bg, fg, border) = when (kind) {
        ButtonKind.PRIMARY -> Triple(c.mint, c.onMint, null)
        ButtonKind.SECONDARY -> Triple(Color.Transparent, c.ink, BorderStroke(Size.hairline, c.ink))
        ButtonKind.DANGER -> Triple(Color.Transparent, c.coralText, BorderStroke(Size.hairline, c.coralText))
    }
    // Pills: a soft, round shape for every button (0.11.0), like the Log menu.
    val shape = RoundedCornerShape(Radius.radiusRound)
    var m = modifier
        .defaultMinSize(minHeight = Size.button)
        .pressScale(src)
        .clip(shape)
        .background(if (enabled) bg else c.line)
    if (border != null) m = m.border(border, shape)
    Row(
        m.clickable(src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.space4, vertical = Spacing.space3),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) fg else c.muted
        if (icon != null) {
            GIcon(icon, null, tint)
            Spacer(Modifier.width(Spacing.space2))
        }
        Text(text, style = Type.label, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

enum class ChipKind { MINT, CORAL, MUTED }

/** Read-only state, 12/16 bold caps. Ready is the absence of a chip. */
@Composable
fun Chip(text: String, kind: ChipKind) {
    val c = CochiloTheme.colors
    val (bg, fg) = when (kind) {
        ChipKind.MINT -> c.mint to c.onMint
        ChipKind.CORAL -> c.coralSoft to c.coralText
        ChipKind.MUTED -> c.line to c.ink
    }
    Text(
        text, style = Type.caption, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(Radius.radiusS)).background(bg)
            .padding(horizontal = Spacing.space2, vertical = Spacing.space1),
    )
}

/** Filter chips: 32dp pills inside 48dp targets, single selection. */
@Composable
fun <T> FilterChips(options: List<T>, selected: T?, label: (T) -> String, onSelect: (T) -> Unit) {
    val c = CochiloTheme.colors
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        options.forEach { o ->
            val on = o == selected
            val src = remember { MutableInteractionSource() }
            Box(
                Modifier.heightIn(min = Spacing.space12)
                    .selectable(on, interactionSource = src, indication = null, role = Role.RadioButton) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                // The pick cross-fades to mint; the chip dips under the finger.
                // The pick is a blush (selected), so teal stays the screen's one action.
                val bg = animatedColor(if (on) c.selected else c.surface)
                Box(
                    Modifier
                        .pressScale(src)
                        .heightIn(min = Spacing.space8)
                        .clip(RoundedCornerShape(Radius.radiusL))
                        .background(bg)
                        .border(Size.hairline, animatedColor(if (on) c.selected else c.line), RoundedCornerShape(Radius.radiusL))
                        .padding(horizontal = Spacing.space3),
                    contentAlignment = Alignment.Center,
                ) { Text(label(o), style = Type.bodySmall, color = animatedColor(if (on) c.onSelected else c.ink)) }
            }
        }
    }
}

/** Field: 52dp, radius 4, line border, label above. */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    val c = CochiloTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = Type.meta, color = c.muted)
        Spacer(Modifier.height(Spacing.space1))
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = singleLine,
            textStyle = Type.body.copy(color = c.ink), keyboardOptions = keyboard,
            cursorBrush = SolidColor(c.ink),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = Size.button)
                        .clip(RoundedCornerShape(Radius.radiusS)).background(c.surface)
                        .border(Size.hairline, c.line, RoundedCornerShape(Radius.radiusS))
                        .padding(horizontal = Spacing.space4, vertical = Spacing.space3),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = Type.body, color = c.muted)
                    inner()
                }
            },
        )
    }
}

/** A tappable value that opens a picker (date, time), drawn like a field. */
@Composable
fun PickerField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = CochiloTheme.colors
    val src = remember { MutableInteractionSource() }
    Column(modifier) {
        Text(label, style = Type.meta, color = c.muted)
        Spacer(Modifier.height(Spacing.space1))
        Box(
            Modifier.fillMaxWidth().heightIn(min = Size.button)
                .pressScale(src)
                .clip(RoundedCornerShape(Radius.radiusS)).background(c.surface)
                .border(Size.hairline, c.line, RoundedCornerShape(Radius.radiusS))
                .clickable(src, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.space4, vertical = Spacing.space3)
                .semantics { contentDescription = "$label, $value" },
            contentAlignment = Alignment.CenterStart,
        ) { Text(value, style = Type.body, color = c.ink) }
    }
}

/** Empty state: title 22/28, one line, at most one primary. */
@Composable
fun EmptyState(title: String, line: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = CochiloTheme.colors
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.space10)) {
        Text(title, style = Type.title, color = c.ink)
        Spacer(Modifier.height(Spacing.space2))
        Text(line, style = Type.body, color = c.muted)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(Spacing.space6))
            Button(action, onAction, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        }
    }
}

data class Tab(val label: String, @DrawableRes val icon: Int)

/** Tab bar: 64dp, the active tab is a mint pill behind the icon. */
@Composable
fun TabBar(tabs: List<Tab>, selected: Int, onSelect: (Int) -> Unit) {
    val c = CochiloTheme.colors
    Row(Modifier.fillMaxWidth().height(Size.tabbar).background(c.surface), verticalAlignment = Alignment.CenterVertically) {
        tabs.forEachIndexed { i, t ->
            val on = i == selected
            val src = remember { MutableInteractionSource() }
            Column(
                Modifier.weight(1f).heightIn(min = Spacing.space12)
                    .selectable(on, interactionSource = src, indication = null, role = Role.Tab) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // The pill cross-fades from tab to tab.
                Box(
                    Modifier.pressScale(src).widthIn(min = Spacing.space12 + Spacing.space2).height(Spacing.space8)
                        .clip(RoundedCornerShape(Radius.radiusL)).background(animatedColor(if (on) c.selected else c.surface)),
                    contentAlignment = Alignment.Center,
                ) { GIcon(t.icon, null, animatedColor(if (on) c.onSelected else c.muted)) }
                Text(t.label, style = Type.caption, color = animatedColor(if (on) c.ink else c.muted))
            }
        }
    }
}

/** A coloured square that sits next to a word on a legend (never colour alone). */
@Composable
fun Swatch(color: Color) {
    Box(Modifier.size(Spacing.space3).clip(RoundedCornerShape(Radius.radiusS)).background(color))
}
