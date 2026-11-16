package com.nova.music.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Coherent design tokens for STEREO visual identity and theme presets.
 */
data class StereoColors(
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val playerBackground: Color,
    val miniPlayerBackground: Color,
    val controlActive: Color,
    val controlInactive: Color,
    val sliderTrack: Color,
    val sliderThumb: Color,
    val divider: Color,
    val error: Color = ColorError,
    val statusPlaying: Color = ColorPlaying,
    val statusBuffering: Color = ColorBuffering
)

// ============================================================================
// Baseline Theme Definitions
// ============================================================================

val MidnightStereoColors = StereoColors(
    name = "Midnight Stereo",
    isDark = true,
    background = DarkMidnight,
    backgroundSecondary = DarkBackground,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceHighlight = DarkSurfaceHighlight,
    primaryAccent = StereoAmber,
    secondaryAccent = StereoGold,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textTertiary = TextTertiary,
    playerBackground = DarkMidnight,
    miniPlayerBackground = DarkSurface,
    controlActive = StereoAmber,
    controlInactive = TextTertiary,
    sliderTrack = DarkSurfaceHighlight,
    sliderThumb = StereoAmber,
    divider = DarkDivider
)

val PureAmoledColors = StereoColors(
    name = "Pure AMOLED",
    isDark = true,
    background = PureBlack,
    backgroundSecondary = Color(0xFF080808),
    surface = Color(0xFF121212),
    surfaceElevated = Color(0xFF1B1B1B),
    surfaceHighlight = Color(0xFF262626),
    primaryAccent = StereoAmber,
    secondaryAccent = StereoGold,
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB3B3B3),
    textTertiary = Color(0xFF707070),
    playerBackground = PureBlack,
    miniPlayerBackground = Color(0xFF101010),
    controlActive = StereoAmber,
    controlInactive = Color(0xFF707070),
    sliderTrack = Color(0xFF222222),
    sliderThumb = StereoAmber,
    divider = Color(0xFF181818)
)

val VintageCreamColors = StereoColors(
    name = "Vintage Cream",
    isDark = false,
    background = PaperCream,
    backgroundSecondary = PaperCreamDim,
    surface = PaperSurface,
    surfaceElevated = PaperSurfaceElevated,
    surfaceHighlight = PaperSurfaceHighlight,
    primaryAccent = StereoTerracotta,
    secondaryAccent = StereoCopper,
    textPrimary = PaperInk,
    textSecondary = PaperInkSecondary,
    textTertiary = PaperInkTertiary,
    playerBackground = PaperCream,
    miniPlayerBackground = PaperSurface,
    controlActive = StereoTerracotta,
    controlInactive = PaperInkTertiary,
    sliderTrack = PaperSurfaceHighlight,
    sliderThumb = StereoTerracotta,
    divider = PaperDivider
)

val LocalStereoColors = staticCompositionLocalOf { MidnightStereoColors }

object StereoTheme {
    val colors: StereoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStereoColors.current
}

@Composable
fun StereoTheme(
    colors: StereoColors = MidnightStereoColors,
    content: @Composable () -> Unit
) {
    val materialColorScheme = if (colors.isDark) {
        darkColorScheme(
            primary = colors.primaryAccent,
            onPrimary = if (colors.isDark) PureBlack else PaperSurface,
            primaryContainer = colors.surfaceElevated,
            onPrimaryContainer = colors.primaryAccent,
            secondary = colors.secondaryAccent,
            onSecondary = PureBlack,
            secondaryContainer = colors.surfaceHighlight,
            onSecondaryContainer = colors.secondaryAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            error = colors.error,
            onError = PureBlack
        )
    } else {
        lightColorScheme(
            primary = colors.primaryAccent,
            onPrimary = PaperSurface,
            primaryContainer = colors.surfaceElevated,
            onPrimaryContainer = colors.primaryAccent,
            secondary = colors.secondaryAccent,
            onSecondary = PaperSurface,
            secondaryContainer = colors.surfaceHighlight,
            onSecondaryContainer = colors.secondaryAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            error = colors.error,
            onError = PaperSurface
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !colors.isDark
            controller.isAppearanceLightNavigationBars = !colors.isDark
        }
    }

    CompositionLocalProvider(LocalStereoColors provides colors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward Compatibility for NovaTheme
@Composable
fun NovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    StereoTheme(
        colors = PureAmoledColors,
        content = content
    )
}
