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
    val id: String = "",
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

val LocalStereoColors = staticCompositionLocalOf { ThemePresets.MonochromeMinimal }
val LocalCardStyleMode = staticCompositionLocalOf { CardStyleMode.DEFAULT }
val LocalAdvancedAppearance = staticCompositionLocalOf { AdvancedAppearanceConfig() }

object StereoTheme {
    val colors: StereoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStereoColors.current

    val cardMode: CardStyleMode
        @Composable
        @ReadOnlyComposable
        get() = LocalCardStyleMode.current

    val advancedConfig: AdvancedAppearanceConfig
        @Composable
        @ReadOnlyComposable
        get() = LocalAdvancedAppearance.current

    @Composable
    operator fun invoke(
        colors: StereoColors = ThemePresets.MonochromeMinimal,
        cardMode: CardStyleMode = CardStyleMode.DEFAULT,
        advancedConfig: AdvancedAppearanceConfig = AdvancedAppearanceConfig(),
        content: @Composable () -> Unit
    ) {
        // Apply batch playback control color override if defined
        val effectiveColors = if (advancedConfig.batchPlaybackControlColor != null) {
            val batchColor = Color(advancedConfig.batchPlaybackControlColor)
            colors.copy(
                controlActive = batchColor,
                sliderThumb = batchColor
            )
        } else {
            colors
        }

        val materialColorScheme = if (effectiveColors.isDark) {
            darkColorScheme(
                primary = effectiveColors.primaryAccent,
                onPrimary = if (effectiveColors.isDark) PureBlack else PaperSurface,
                primaryContainer = effectiveColors.surfaceElevated,
                onPrimaryContainer = effectiveColors.primaryAccent,
                secondary = effectiveColors.secondaryAccent,
                onSecondary = PureBlack,
                secondaryContainer = effectiveColors.surfaceHighlight,
                onSecondaryContainer = effectiveColors.secondaryAccent,
                background = effectiveColors.background,
                onBackground = effectiveColors.textPrimary,
                surface = effectiveColors.surface,
                onSurface = effectiveColors.textPrimary,
                surfaceVariant = effectiveColors.surfaceElevated,
                onSurfaceVariant = effectiveColors.textSecondary,
                error = effectiveColors.error,
                onError = PureBlack
            )
        } else {
            lightColorScheme(
                primary = effectiveColors.primaryAccent,
                onPrimary = PaperSurface,
                primaryContainer = effectiveColors.surfaceElevated,
                onPrimaryContainer = effectiveColors.primaryAccent,
                secondary = effectiveColors.secondaryAccent,
                onSecondary = PaperSurface,
                secondaryContainer = effectiveColors.surfaceHighlight,
                onSecondaryContainer = effectiveColors.secondaryAccent,
                background = effectiveColors.background,
                onBackground = effectiveColors.textPrimary,
                surface = effectiveColors.surface,
                onSurface = effectiveColors.textPrimary,
                surfaceVariant = effectiveColors.surfaceElevated,
                onSurfaceVariant = effectiveColors.textSecondary,
                error = effectiveColors.error,
                onError = PaperSurface
            )
        }

        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = effectiveColors.background.toArgb()
                window.navigationBarColor = effectiveColors.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !effectiveColors.isDark
                controller.isAppearanceLightNavigationBars = !effectiveColors.isDark
            }
        }

        CompositionLocalProvider(
            LocalStereoColors provides effectiveColors,
            LocalCardStyleMode provides cardMode,
            LocalAdvancedAppearance provides advancedConfig
        ) {
            MaterialTheme(
                colorScheme = materialColorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}

@Composable
fun StereoTheme(
    colors: StereoColors = ThemePresets.MonochromeMinimal,
    cardMode: CardStyleMode = CardStyleMode.DEFAULT,
    advancedConfig: AdvancedAppearanceConfig = AdvancedAppearanceConfig(),
    content: @Composable () -> Unit
) {
    StereoTheme.invoke(
        colors = colors,
        cardMode = cardMode,
        advancedConfig = advancedConfig,
        content = content
    )
}

// Backward Compatibility for NovaTheme
@Composable
fun NovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    StereoTheme(
        colors = ThemePresets.PureBlack,
        content = content
    )
}
