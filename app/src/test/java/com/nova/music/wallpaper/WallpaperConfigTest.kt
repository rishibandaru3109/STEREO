package com.nova.music.wallpaper

import com.nova.music.core.designsystem.theme.ThemePresets
import com.nova.music.core.designsystem.wallpaper.GradientPreset
import com.nova.music.core.designsystem.wallpaper.WallpaperConfig
import com.nova.music.core.designsystem.wallpaper.WallpaperMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperConfigTest {

    @Test
    fun testDefaultWallpaperConfigValues() {
        val config = WallpaperConfig()
        assertEquals(WallpaperMode.THEME_SOLID, config.mode)
        assertEquals(GradientPreset.CASSETTE_GLOW, config.gradientPreset)
        assertEquals(40f, config.blurRadius, 0.01f)
        assertEquals(0.60f, config.dimmingAlpha, 0.01f)
        assertFalse(config.isDesaturated)
        assertFalse(config.applyToPlayerOnly)
    }

    @Test
    fun testJsonSerializationAndDeserialization() {
        val original = WallpaperConfig(
            mode = WallpaperMode.ALBUM_ART,
            gradientPreset = GradientPreset.SUNSET_FADE,
            blurRadius = 65f,
            dimmingAlpha = 0.75f,
            isDesaturated = true,
            applyToPlayerOnly = true
        )

        val json = original.toJson()
        assertNotNull(json)
        assertTrue(json.contains("ALBUM_ART"))
        assertTrue(json.contains("SUNSET_FADE"))

        val restored = WallpaperConfig.fromJson(json)
        assertEquals(original.mode, restored.mode)
        assertEquals(original.gradientPreset, restored.gradientPreset)
        assertEquals(original.blurRadius, restored.blurRadius, 0.01f)
        assertEquals(original.dimmingAlpha, restored.dimmingAlpha, 0.01f)
        assertEquals(original.isDesaturated, restored.isDesaturated)
        assertEquals(original.applyToPlayerOnly, restored.applyToPlayerOnly)
    }

    @Test
    fun testBoundaryClampingOnDeserialization() {
        // Blur too high (150f) and Dim too high (1.5f)
        val excessiveJson = """
            {
                "mode": "ALBUM_ART",
                "gradientPreset": "DEEP_ABYSS",
                "blurRadius": 150.0,
                "dimmingAlpha": 1.5,
                "isDesaturated": false,
                "applyToPlayerOnly": false
            }
        """.trimIndent()

        val clampedHigh = WallpaperConfig.fromJson(excessiveJson)
        assertEquals(80f, clampedHigh.blurRadius, 0.01f)
        assertEquals(0.90f, clampedHigh.dimmingAlpha, 0.01f)

        // Blur too low (-5f) and Dim too low (-0.5f)
        val negativeJson = """
            {
                "mode": "ALBUM_ART",
                "gradientPreset": "DEEP_ABYSS",
                "blurRadius": -5.0,
                "dimmingAlpha": -0.5,
                "isDesaturated": false,
                "applyToPlayerOnly": false
            }
        """.trimIndent()

        val clampedLow = WallpaperConfig.fromJson(negativeJson)
        assertEquals(10f, clampedLow.blurRadius, 0.01f)
        assertEquals(0.0f, clampedLow.dimmingAlpha, 0.01f)
    }

    @Test
    fun testCorruptedJsonFallback() {
        val corrupted = WallpaperConfig.fromJson("{ invalid json syntax }")
        assertEquals(WallpaperMode.THEME_SOLID, corrupted.mode)
        assertEquals(40f, corrupted.blurRadius, 0.01f)

        val empty = WallpaperConfig.fromJson("")
        assertEquals(WallpaperMode.THEME_SOLID, empty.mode)
    }

    @Test
    fun testGradientPresetBrushes() {
        val colors = ThemePresets.MidnightStereo
        for (preset in GradientPreset.entries) {
            val brush = preset.getBrush(colors)
            assertNotNull("Gradient preset ${preset.name} brush must not be null", brush)
        }
    }
}
