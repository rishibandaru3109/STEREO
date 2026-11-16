package com.nova.music.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.util.UUID

/**
 * Data model for user-authored custom themes in STEREO.
 */
data class CustomTheme(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isDark: Boolean = true,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val controlActive: Color
) {
    fun toStereoColors(): StereoColors {
        return StereoColors(
            id = id,
            name = name,
            isDark = isDark,
            background = background,
            backgroundSecondary = surface,
            surface = surface,
            surfaceElevated = surfaceElevated,
            surfaceHighlight = surfaceElevated,
            primaryAccent = primaryAccent,
            secondaryAccent = secondaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textTertiary = textSecondary.copy(alpha = 0.7f),
            playerBackground = background,
            miniPlayerBackground = surface,
            controlActive = controlActive,
            controlInactive = textSecondary.copy(alpha = 0.5f),
            sliderTrack = surfaceElevated,
            sliderThumb = primaryAccent,
            divider = surfaceElevated
        )
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("isDark", isDark)
        json.put("background", background.value.toLong())
        json.put("surface", surface.value.toLong())
        json.put("surfaceElevated", surfaceElevated.value.toLong())
        json.put("primaryAccent", primaryAccent.value.toLong())
        json.put("secondaryAccent", secondaryAccent.value.toLong())
        json.put("textPrimary", textPrimary.value.toLong())
        json.put("textSecondary", textSecondary.value.toLong())
        json.put("controlActive", controlActive.value.toLong())
        return json.toString()
    }

    companion object {
        fun fromStereoColors(colors: StereoColors, newName: String = "${colors.name} (Custom)"): CustomTheme {
            return CustomTheme(
                id = "custom_" + UUID.randomUUID().toString().take(8),
                name = newName,
                isDark = colors.isDark,
                background = colors.background,
                surface = colors.surface,
                surfaceElevated = colors.surfaceElevated,
                primaryAccent = colors.primaryAccent,
                secondaryAccent = colors.secondaryAccent,
                textPrimary = colors.textPrimary,
                textSecondary = colors.textSecondary,
                controlActive = colors.controlActive
            )
        }

        fun fromJson(jsonStr: String): CustomTheme? {
            return try {
                val json = JSONObject(jsonStr)
                CustomTheme(
                    id = json.optString("id", UUID.randomUUID().toString()),
                    name = json.optString("name", "Custom Theme"),
                    isDark = json.optBoolean("isDark", true),
                    background = Color(json.getLong("background").toULong()),
                    surface = Color(json.getLong("surface").toULong()),
                    surfaceElevated = Color(json.getLong("surfaceElevated").toULong()),
                    primaryAccent = Color(json.getLong("primaryAccent").toULong()),
                    secondaryAccent = Color(json.getLong("secondaryAccent").toULong()),
                    textPrimary = Color(json.getLong("textPrimary").toULong()),
                    textSecondary = Color(json.getLong("textSecondary").toULong()),
                    controlActive = Color(json.getLong("controlActive").toULong())
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}

/**
 * W3C WCAG 2.1 Contrast Validator.
 * Calculates luminance and ensures accessible contrast between text and backgrounds.
 */
object ContrastValidator {

    fun calculateContrast(foreground: Color, background: Color): Float {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    fun isAccessible(foreground: Color, background: Color, minRatio: Float = 3.0f): Boolean {
        return calculateContrast(foreground, background) >= minRatio
    }

    private fun relativeLuminance(color: Color): Float {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun linearize(channel: Float): Float {
        return if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            Math.pow(((channel + 0.055) / 1.055), 2.4).toFloat()
        }
    }
}
