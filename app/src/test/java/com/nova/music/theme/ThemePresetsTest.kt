package com.nova.music.theme

import com.nova.music.core.designsystem.theme.ThemePresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePresetsTest {

    @Test
    fun testAllTwelvePresetsExist() {
        val presets = ThemePresets.allPresets
        assertTrue("Expected at least 12 presets, found ${presets.size}", presets.size >= 12)
    }

    @Test
    fun testOfficialSevenThemesExist() {
        val official = ThemePresets.officialThemes
        assertEquals(7, official.size)
        val expectedOfficialIds = listOf(
            "monochrome_minimal",
            "frosted_glass",
            "black_yellow",
            "bluey",
            "starry_glow",
            "ivory_beige",
            "pure_black"
        )
        for (id in expectedOfficialIds) {
            val found = ThemePresets.getById(id)
            assertNotNull("Official theme '$id' must exist", found)
            assertEquals("Found theme should match requested id", id, found.id)
        }
    }

    @Test
    fun testAllPresetIdsAreUniqueAndNonEmpty() {
        val presets = ThemePresets.allPresets
        val ids = presets.map { it.id }
        val uniqueIds = ids.toSet()

        assertEquals("Preset IDs must be completely unique", ids.size, uniqueIds.size)
        for (preset in presets) {
            assertTrue("Preset ID should not be blank: ${preset.name}", preset.id.isNotBlank())
            assertTrue("Preset name should not be blank", preset.name.isNotBlank())
        }
    }

    @Test
    fun testExpectedPresetIdsPresent() {
        val expectedIds = listOf(
            "monochrome_minimal",
            "frosted_glass",
            "black_yellow",
            "bluey",
            "starry_glow",
            "ivory_beige",
            "pure_black",
            "midnight_stereo",
            "vintage_cream",
            "burgundy_vinyl",
            "olive_radio",
            "cassette_sunset",
            "paper_and_ink"
        )

        for (id in expectedIds) {
            val found = ThemePresets.getById(id)
            assertNotNull("Theme preset with id '$id' must exist", found)
            assertEquals("Found theme should match requested id", id, found.id)
        }
    }

    @Test
    fun testDefaultFallbackOnUnknownOrNullId() {
        val defaultPreset = ThemePresets.MidnightStereo

        val nullLookup = ThemePresets.getById(null)
        assertEquals("Null ID must fall back to Midnight Stereo", defaultPreset.id, nullLookup.id)

        val unknownLookup = ThemePresets.getById("unknown_custom_404")
        assertEquals("Unknown ID must fall back to Midnight Stereo", defaultPreset.id, unknownLookup.id)

        val emptyLookup = ThemePresets.getById("")
        assertEquals("Empty ID must fall back to Midnight Stereo", defaultPreset.id, emptyLookup.id)
    }

    @Test
    fun testLightAndDarkModesRepresented() {
        val presets = ThemePresets.allPresets
        val darkCount = presets.count { it.isDark }
        val lightCount = presets.count { !it.isDark }

        assertTrue("Expected dark presets", darkCount > 0)
        assertTrue("Expected light presets (e.g. Vintage Cream, Paper & Ink)", lightCount >= 2)

        assertTrue(ThemePresets.MidnightStereo.isDark)
        assertFalse(ThemePresets.VintageCream.isDark)
        assertFalse(ThemePresets.PaperAndInk.isDark)
    }

    @Test
    fun testTokenContrastAndIntegrity() {
        for (preset in ThemePresets.allPresets) {
            // Background and Accent should not be identical
            assertNotEquals(
                "Accent must differ from background in ${preset.name}",
                preset.background,
                preset.primaryAccent
            )
            // Text and Background should not be identical
            assertNotEquals(
                "Text primary must differ from background in ${preset.name}",
                preset.background,
                preset.textPrimary
            )
        }
    }
}
