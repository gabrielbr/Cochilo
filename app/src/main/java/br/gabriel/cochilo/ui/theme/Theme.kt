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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.gabriel.cochilo.R

/**
 * The app's one family. Cochilo wears the Tatá Slings brand (CTokens.Brand, the design system's
 * docs/brands.md), whose family is Libre Franklin, the one on tataslings.com.br: regular,
 * semibold, bold. The faces ship in res/font under the SIL Open Font License (licenses/).
 */
val BrandFamily = FontFamily(
    Font(R.font.libre_franklin_regular, FontWeight.Normal),
    Font(R.font.libre_franklin_semibold, FontWeight.SemiBold),
    Font(R.font.libre_franklin_bold, FontWeight.Bold),
)

private fun role(size: Int, line: Int, bold: Boolean, tracking: Float = 0f, tabular: Boolean = false) = TextStyle(
    fontFamily = BrandFamily,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    letterSpacing = tracking.sp,
    // Times and durations in tabular figures, so a running timer doesn't jitter.
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/** The layer's type roles. Screens use these, never an inline size. */
object Type {
    val display = role(CTokens.Type.displaySize, CTokens.Type.displayLine, CTokens.Type.displayBold)
    val timer = role(CTokens.Type.timerSize, CTokens.Type.timerLine, CTokens.Type.timerBold, CTokens.Type.timerTracking, tabular = true)
    val title = role(CTokens.Type.titleSize, CTokens.Type.titleLine, CTokens.Type.titleBold)
    val label = role(CTokens.Type.labelSize, CTokens.Type.labelLine, CTokens.Type.labelBold)
    val body = role(CTokens.Type.bodySize, CTokens.Type.bodyLine, CTokens.Type.bodyBold)
    val bodySmall = role(CTokens.Type.bodySmallSize, CTokens.Type.bodySmallLine, CTokens.Type.bodySmallBold)
    val meta = role(CTokens.Type.metaSize, CTokens.Type.metaLine, CTokens.Type.metaBold, tabular = true)
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
    val isNight: Boolean @Composable get() = LocalIsNight.current
}

/** True while the night light is on (docs/night-light.md): amber on warm black, no blue or teal. */
val LocalIsNight = staticCompositionLocalOf { false }

/**
 * [night]: the night light (from 19:00 to 07:00 when it's on): the design system's night theme,
 * warm amber and red on near-black, because light at night is what the research says to dim.
 */
@Composable
fun CochiloTheme(dark: Boolean, night: Boolean = false, content: @Composable () -> Unit) {
    val p = when {
        night -> CTokens.Night
        dark -> CTokens.Dark
        else -> CTokens.Light
    }
    val context = LocalContext.current
    val reduce = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    // Material components (sheets, pickers) read their colours from these roles.
    val scheme = (if (dark || night) darkColorScheme() else lightColorScheme()).copy(
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
        LocalIsDark provides (dark || night),
        LocalIsNight provides night,
        LocalReduceMotion provides reduce,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
