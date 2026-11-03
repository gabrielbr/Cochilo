package br.gabriel.cochilo.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/** Android's window width classes, at the design system's `width-medium` and `width-expanded`. */
enum class WidthClass {
    COMPACT, MEDIUM, EXPANDED;

    companion object {
        fun of(width: Dp) = when {
            width >= Size.widthExpanded -> EXPANDED
            width >= Size.widthMedium -> MEDIUM
            else -> COMPACT
        }
    }
}

/**
 * The app's frame on every width (design system, components.md: Navigation rail; docs/tablet.md).
 * A phone: the header, the screen, the tab bar under the thumb. From 600dp (a small tablet, a
 * phone on its side, a foldable half open): the tabs stand on their side as a rail at the start,
 * and the screen's column stays at most `content-max` wide, centred, so lines stay readable. From
 * 840dp, [content] gets EXPANDED and may set two panes side by side ([TwoPanes]).
 */
@Composable
fun AppShell(
    tabs: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    header: @Composable () -> Unit,
    showTabs: Boolean = true,
    content: @Composable BoxScope.(WidthClass) -> Unit,
) {
    // The window's width decides the class (as Android's window classes do), not the space left
    // inside the frame.
    val cls = WidthClass.of(androidx.compose.ui.unit.Dp(androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()))
    Box(Modifier.fillMaxSize()) {
        if (cls == WidthClass.COMPACT) {
            Column(Modifier.fillMaxSize()) {
                header()
                Box(Modifier.weight(1f)) { content(cls) }
                if (showTabs) TabBar(tabs, selected, onSelect)
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                if (showTabs) NavRail(tabs, selected, onSelect)
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    header()
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        val max = if (cls == WidthClass.EXPANDED) Size.contentMax * 2 else Size.contentMax
                        Box(Modifier.widthIn(max = max).fillMaxSize()) { content(cls) }
                    }
                }
            }
        }
    }
}

/** Two screens side by side, each at most `content-max`, a hairline between (EXPANDED only). */
@Composable
fun TwoPanes(start: @Composable BoxScope.() -> Unit, end: @Composable BoxScope.() -> Unit) {
    val c = CochiloTheme.colors
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight(), content = start)
        Box(Modifier.width(Size.hairline).fillMaxHeight().background(c.line))
        Box(Modifier.weight(1f).fillMaxHeight(), content = end)
    }
}

/** The tab bar on its side: `rail` wide, the same pill and words, from the top. */
@Composable
fun NavRail(tabs: List<Tab>, selected: Int, onSelect: (Int) -> Unit) {
    val c = CochiloTheme.colors
    Column(
        Modifier.width(Size.rail).fillMaxHeight().background(c.surface).padding(top = Spacing.space4).selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.space3),
    ) {
        tabs.forEachIndexed { i, t ->
            val on = i == selected
            val src = remember { MutableInteractionSource() }
            Column(
                Modifier.fillMaxWidth().heightIn(min = Spacing.space12)
                    .selectable(on, interactionSource = src, indication = null, role = Role.Tab) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.pressScale(src).widthIn(min = Spacing.space12 + Spacing.space2).height(Spacing.space8)
                        .clip(RoundedCornerShape(Radius.radiusL)).background(animatedColor(if (on) c.selected else c.surface)),
                    contentAlignment = Alignment.Center,
                ) { GIcon(t.icon, null, animatedColor(if (on) c.onSelected else c.muted)) }
                CappedFontScale(LABEL_SCALE_CAP) {
                    Text(t.label, style = Type.caption, color = animatedColor(if (on) c.ink else c.muted), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
