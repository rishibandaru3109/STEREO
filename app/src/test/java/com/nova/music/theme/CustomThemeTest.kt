package com.nova.music.theme

import androidx.compose.ui.graphics.Color
import com.nova.music.core.designsystem.theme.ContrastValidator
import com.nova.music.core.designsystem.theme.CustomTheme
import com.nova.music.core.designsystem.theme.ThemePresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThemeTest {

    @Test
    fun testCustomThemeJsonSerializationAndDeserialization() {
        val original = CustomTheme(
            id = "custom_test_01",
            name = "Studio Obsidian",
            isDark = true,
            background = Color(0xFF0F0F14),
            surface = Color(0xFF1B1B22),
            surfaceElevated = Color(0xFF262630),
            primaryAccent = Color(0xFFFF6B35),
            secondaryAccent = Color(0xFFFF9F1C),
            textPrimary = Color(0xFFF5F2EB),
            textSecondary = Color(0xFFA09C94),
            controlActive = Color(0xFFFF6B35)
        )

        val jsonString = original.toJson()
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("Studio Obsidian"))
        assertTrue(jsonString.contains("custom_test_01"))

        val restored = CustomTheme.fromJson(jsonString)
        assertNotNull(restored)
        assertEquals(original.id, restored!!.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.isDark, restored.isDark)
        assertEquals(original.background.value, restored.background.value)
        assertEquals(original.primaryAccent.value, restored.primaryAccent.value)
        assertEquals(original.textPrimary.value, restored.textPrimary.value)
    }

    @Test
    fun testCorruptedJsonReturnsNullSafely() {
        assertNull(CustomTheme.fromJson(""))
        assertNull(CustomTheme.fromJson("{ invalid json syntax }"))
        assertNull(CustomTheme.fromJson("""{"name": "No Colors"}"""))
    }

    @Test
    fun testConversionToStereoColors() {
        val custom = CustomTheme(
            id = "custom_02",
            name = "Crimson Tape",
            isDark = true,
            background = Color(0xFF1A0A0E),
            surface = Color(0xFF281018),
            surfaceElevated = Color(0xFF381822),
            primaryAccent = Color(0xFFE05A78),
            secondaryAccent = Color(0xFFC73E60),
            textPrimary = Color(0xFFFDE8ED),
            textSecondary = Color(0xFFB89CA4),
            controlActive = Color(0xFFE05A78)
        )

        val stereoColors = custom.toStereoColors()
        assertEquals(custom.id, stereoColors.id)
        assertEquals(custom.name, stereoColors.name)
        assertEquals(custom.primaryAccent, stereoColors.primaryAccent)
        assertEquals(custom.background, stereoColors.background)
        assertEquals(custom.controlActive, stereoColors.controlActive)
        assertEquals(stereoColors.background, stereoColors.playerBackground)
    }

    @Test
    fun testFromStereoColorsDuplication() {
        val preset = ThemePresets.BurgundyVinyl
        val duplicated = CustomTheme.fromStereoColors(preset, "My Burgundy")

        assertEquals("My Burgundy", duplicated.name)
        assertEquals(preset.isDark, duplicated.isDark)
        assertEquals(preset.primaryAccent, duplicated.primaryAccent)
        assertEquals(preset.background, duplicated.background)
        assertTrue(duplicated.id.startsWith("custom_"))
    }

    @Test
    fun testContrastValidatorAccurateCalculations() {
        // High Contrast: White text on Pitch Black background -> max ratio ~21:1
        val white = Color(0xFFFFFFFF)
        val black = Color(0xFF000000)
        val maxContrast = ContrastValidator.calculateContrast(white, black)
        assertTrue("White on black should be close to 21:1, found $maxContrast", maxContrast > 20.0f)
        assertTrue(ContrastValidator.isAccessible(white, black, 4.5f))

        // Identical colors -> ratio 1:1
        val zeroContrast = ContrastValidator.calculateContrast(black, black)
        assertEquals(1.0f, zeroContrast, 0.01f)
        assertFalse(ContrastValidator.isAccessible(black, black, 3.0f))

        // Low Contrast: Dark gray on black -> ratio < 2.0:1
        val darkGray = Color(0xFF1A1A1A)
        val lowContrast = ContrastValidator.calculateContrast(darkGray, black)
        assertTrue("Dark gray on black should have low contrast, found $lowContrast", lowContrast < 2.0f)
        assertFalse(ContrastValidator.isAccessible(darkGray, black, 3.0f))

        // Test all 12 presets text-to-background contrast
        for (preset in ThemePresets.allPresets) {
            val ratio = ContrastValidator.calculateContrast(preset.textPrimary, preset.background)
            assertTrue(
                "Preset '${preset.name}' textPrimary should have readable contrast (>= 3.0:1), found $ratio",
                ratio >= 3.0f
            )
        }
    }
}
