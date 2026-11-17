package com.nova.music.core.designsystem.wallpaper

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nova.music.core.designsystem.theme.StereoColors
import org.json.JSONObject

enum class WallpaperMode {
    THEME_SOLID,
    GRADIENT,
    ALBUM_ART,
    CUSTOM_IMAGE
}

enum class GradientPreset {
    CASSETTE_GLOW,
    STAGE_LIGHT,
    SUNSET_FADE,
    DEEP_ABYSS;

    fun getBrush(colors: StereoColors): Brush {
        return when (this) {
            CASSETTE_GLOW -> Brush.linearGradient(
                colors = listOf(
                    colors.primaryAccent.copy(alpha = 0.4f),
                    colors.background,
                    colors.background
                )
            )
            STAGE_LIGHT -> Brush.radialGradient(
                colors = listOf(
                    colors.secondaryAccent.copy(alpha = 0.5f),
                    colors.backgroundSecondary,
                    colors.background
                )
            )
            SUNSET_FADE -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE58B24).copy(alpha = 0.45f),
                    Color(0xFF781D2E).copy(alpha = 0.35f),
                    colors.background
                )
            )
            DEEP_ABYSS -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F2B48).copy(alpha = 0.5f),
                    Color(0xFF0A1118),
                    colors.background
                )
            )
        }
    }
}

data class WallpaperConfig @JvmOverloads constructor(
    val mode: WallpaperMode = WallpaperMode.THEME_SOLID,
    val gradientPreset: GradientPreset = GradientPreset.CASSETTE_GLOW,
    val blurRadius: Float = 40f,          // 10f to 80f
    val dimmingAlpha: Float = 0.60f,       // 0.0f to 0.90f
    val isDesaturated: Boolean = false,
    val applyToPlayerOnly: Boolean = false,
    val customImageUri: String? = null
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("mode", mode.name)
        json.put("gradientPreset", gradientPreset.name)
        customImageUri?.let { json.put("customImageUri", it) }
        json.put("blurRadius", blurRadius.toDouble())
        json.put("dimmingAlpha", dimmingAlpha.toDouble())
        json.put("isDesaturated", isDesaturated)
        json.put("applyToPlayerOnly", applyToPlayerOnly)
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): WallpaperConfig {
            return try {
                val json = JSONObject(jsonStr)
                val mode = WallpaperMode.valueOf(json.optString("mode", WallpaperMode.THEME_SOLID.name))
                val gradient = GradientPreset.valueOf(json.optString("gradientPreset", GradientPreset.CASSETTE_GLOW.name))
                val customUri = if (json.has("customImageUri")) json.getString("customImageUri").ifEmpty { null } else null
                val blur = json.optDouble("blurRadius", 40.0).toFloat().coerceIn(10f, 80f)
                val dim = json.optDouble("dimmingAlpha", 0.60).toFloat().coerceIn(0f, 0.9f)
                val desaturated = json.optBoolean("isDesaturated", false)
                val playerOnly = json.optBoolean("applyToPlayerOnly", false)
                WallpaperConfig(
                    mode = mode,
                    gradientPreset = gradient,
                    blurRadius = blur,
                    dimmingAlpha = dim,
                    isDesaturated = desaturated,
                    applyToPlayerOnly = playerOnly,
                    customImageUri = customUri
                )
            } catch (_: Throwable) {
                WallpaperConfig()
            }
        }
    }
}
