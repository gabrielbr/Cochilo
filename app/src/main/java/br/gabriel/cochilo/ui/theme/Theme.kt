package br.gabriel.cochilo.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.gabriel.cochilo.R

/** Ubuntu Mono, the system's one family: regular, bold, bold italic. */
val UbuntuMono = FontFamily(
    Font(R.font.ubuntu_mono, FontWeight.Normal),
    Font(R.font.ubuntu_mono_bold, FontWeight.Bold),
    Font(R.font.ubuntu_mono_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

private fun role(size: Int, line: Int, bold: Boolean, tracking: Float = 0f) = TextStyle(
    fontFamily = UbuntuMono,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    letterSpacing = tracking.sp,
)

/** The layer's type roles. Screens use these, never an inline size. */
object Type {
    val display = role(CTokens.Type.displaySize, CTokens.Type.displayLine, CTokens.Type.displayBold)
    val timer = role(CTokens.Type.timerSize, CTokens.Type.timerLine, CTokens.Type.timerBold, CTokens.Type.timerTracking)
    val title = role(CTokens.Type.titleSize, CTokens.Type.titleLine, CTokens.Type.titleBold)
    val label = role(CTokens.Type.labelSize, CTokens.Type.labelLine, CTokens.Type.labelBold)
    val body = role(CTokens.Type.bodySize, CTokens.Type.bodyLine, CTokens.Type.bodyBold)
    val bodySmall = role(CTokens.Type.bodySmallSize, CTokens.Type.bodySmallLine, CTokens.Type.bodySmallBold)
    val meta = role(CTokens.Type.metaSize, CTokens.Type.metaLine, CTokens.Type.metaBold)
    /** Upper-case the text yourself (or use [kit.Caption]); TextStyle has no text-transform. */
    val caption = role(CTokens.Type.captionSize, CTokens.Type.captionLine, CTokens.Type.captionBold, CTokens.Type.captionTracking)
}

/** The system's one curve: quick out, gentle in, no bounce. */
val SystemEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

val LocalPalette = staticCompositionLocalOf { CTokens.Light }
val LocalIsDark = staticCompositionLocalOf { false }

/** True when the phone's "Remove animations" is on: things appear instead of moving. */
val LocalReduceMotion = staticCompositionLocalOf { false }

object CochiloTheme {
    val colors: CTokens.Palette @Composable get() = LocalPalette.current
    val reduceMotion: Boolean @Composable get() = LocalReduceMotion.current
    val isDark: Boolean @Composable get() = LocalIsDark.current
}

@Composable
fun CochiloTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) CTokens.Dark else CTokens.Light
    val context = LocalContext.current
    val reduce = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    // Material components (sheets, pickers) read their colours from these roles.
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.mint, onPrimary = p.onMint,
        primaryContainer = p.mint, onPrimaryContainer = p.onMint,
        secondary = p.ink, onSecondary = p.page,
        secondaryContainer = p.line, onSecondaryContainer = p.ink,
        tertiary = p.ink, onTertiary = p.page,
        background = p.page, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink,
        surfaceVariant = p.page, onSurfaceVariant = p.muted,
        surfaceContainerLowest = p.surface, surfaceContainerLow = p.surface,
        surfaceContainer = p.surface, surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surface,
        surfaceBright = p.surface, surfaceDim = p.page,
        outline = p.muted, outlineVariant = p.line,
        error = p.coralText, onError = p.page, errorContainer = p.coralSoft, onErrorContainer = p.coralText,
        inverseSurface = p.snackbar, inverseOnSurface = p.page, inversePrimary = p.snackbarAction,
        scrim = p.panel,
    )
    val typography = MaterialTheme.typography.let { t ->
        t.copy(
            displayLarge = Type.timer, displayMedium = Type.timer, displaySmall = Type.display,
            headlineLarge = Type.display, headlineMedium = Type.display, headlineSmall = Type.title,
            titleLarge = Type.title, titleMedium = Type.label, titleSmall = Type.label,
            bodyLarge = Type.body, bodyMedium = Type.bodySmall, bodySmall = Type.meta,
            labelLarge = Type.label, labelMedium = Type.caption, labelSmall = Type.caption,
        )
    }
    CompositionLocalProvider(
        LocalPalette provides p,
        LocalIsDark provides dark,
        LocalReduceMotion provides reduce,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
