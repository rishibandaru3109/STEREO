package com.nova.music.recommendation

import com.nova.music.domain.model.LanguageRegion
import com.nova.music.domain.model.StereoLanguages
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.LyricsLanguage
import com.nova.music.domain.recommendation.RecommendationEligibility
import com.nova.music.domain.repository.RecommendationPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StereoLanguageTest {

    @Test
    fun exactly40LanguagesRegisteredAcross5Regions() {
        assertEquals(40, StereoLanguages.ALL.size)

        val byRegion = StereoLanguages.byRegion()
        assertEquals(11, byRegion[LanguageRegion.SOUTH_ASIA]?.size)
        assertEquals(11, byRegion[LanguageRegion.EUROPE_AMERICAS]?.size)
        assertEquals(10, byRegion[LanguageRegion.EAST_SOUTHEAST_ASIA]?.size)
        assertEquals(6, byRegion[LanguageRegion.MIDDLE_EAST_AFRICA]?.size)
        assertEquals(2, byRegion[LanguageRegion.ADDITIONAL]?.size)

        val total = byRegion.values.sumOf { it.size }
        assertEquals(40, total)
    }

    @Test
    fun allLanguageIdsAndNamesAreUniqueAndNonBlank() {
        val ids = StereoLanguages.ALL.map { it.id }
        val names = StereoLanguages.ALL.map { it.name }
        val nativeNames = StereoLanguages.ALL.map { it.nativeName }

        assertEquals(40, ids.distinct().size)
        assertEquals(40, names.distinct().size)
        assertEquals(40, nativeNames.distinct().size)

        StereoLanguages.ALL.forEach { lang ->
            assertTrue("ID must not be blank", lang.id.isNotBlank())
            assertTrue("Name must not be blank", lang.name.isNotBlank())
            assertTrue("Native name must not be blank", lang.nativeName.isNotBlank())
            assertTrue("Search keyword must not be blank", lang.searchKeyword.isNotBlank())
        }
    }

    @Test
    fun languageIdentifierMapping_resolvesCaseInsensitively() {
        // Resolve by ID
        val hiById = StereoLanguages.resolve("hi")
        assertNotNull(hiById)
        assertEquals("Hindi", hiById?.name)

        val teById = StereoLanguages.resolve("TE")
        assertNotNull(teById)
        assertEquals("Telugu", teById?.name)

        val koById = StereoLanguages.resolve("ko")
        assertNotNull(koById)
        assertEquals("Korean", koById?.name)

        // Resolve by Name
        val esByName = StereoLanguages.resolve("spanish")
        assertNotNull(esByName)
        assertEquals("es", esByName?.id)

        val jaByName = StereoLanguages.resolve("JAPANESE")
        assertNotNull(jaByName)
        assertEquals("ja", jaByName?.id)

        val swByName = StereoLanguages.resolve("Swahili")
        assertNotNull(swByName)
        assertEquals("sw", swByName?.id)

        assertNull(StereoLanguages.resolve("unknown_lang_xyz"))
    }

    @Test
    fun preferencePersistenceAndMigration_normalizesLegacyNames() {
        val legacyPreferences = setOf("English", "hindi", "te", "korean")
        val normalized = StereoLanguages.normalize(legacyPreferences)

        assertTrue(normalized.contains("English"))
        assertTrue(normalized.contains("Hindi"))
        assertTrue(normalized.contains("Telugu"))
        assertTrue(normalized.contains("Korean"))
        assertEquals(4, normalized.size)
    }

    @Test
    fun filtering_acceptsMatchingScriptsAndRejectsMismatchedScripts() {
        val teluguTrack = Track("1", "శ్రీరామ", "Devotional")
        val tamilTrack = Track("2", "வாத்தி", "Anirudh")
        val koreanTrack = Track("3", "봄날", "BTS")
        val japaneseTrack = Track("4", "夜に駆ける", "YOASOBI")
        val englishTrack = Track("5", "Save Your Tears", "The Weeknd")

        // English only
        val englishPrefs = RecommendationPreferences(languages = setOf("English"))
        assertTrue(RecommendationEligibility.accepts(englishTrack, englishPrefs))
        assertFalse(RecommendationEligibility.accepts(teluguTrack, englishPrefs))
        assertFalse(RecommendationEligibility.accepts(tamilTrack, englishPrefs))
        assertFalse(RecommendationEligibility.accepts(koreanTrack, englishPrefs))
        assertFalse(RecommendationEligibility.accepts(japaneseTrack, englishPrefs))

        // Telugu preference
        val teluguPrefs = RecommendationPreferences(languages = setOf("Telugu"))
        assertTrue(RecommendationEligibility.accepts(teluguTrack, teluguPrefs))
        assertFalse(RecommendationEligibility.accepts(koreanTrack, teluguPrefs))

        // Korean preference
        val koreanPrefs = RecommendationPreferences(languages = setOf("Korean"))
        assertTrue(RecommendationEligibility.accepts(koreanTrack, koreanPrefs))
        assertFalse(RecommendationEligibility.accepts(japaneseTrack, koreanPrefs))
    }

    @Test
    fun filtering_supportsMultipleLanguageSelections() {
        val englishTrack = Track("1", "Levitating", "Dua Lipa")
        val teluguTrack = Track("2", "శ్రీరామ", "Devotional")
        val spanishTrack = Track("3", "Despacito (Reggaeton)", "Luis Fonsi")
        val japaneseTrack = Track("4", "夜に駆ける", "YOASOBI")

        val multiPrefs = RecommendationPreferences(languages = setOf("English", "Telugu", "Spanish"))
        assertTrue(RecommendationEligibility.accepts(englishTrack, multiPrefs))
        assertTrue(RecommendationEligibility.accepts(teluguTrack, multiPrefs))
        assertTrue(RecommendationEligibility.accepts(spanishTrack, multiPrefs))
        assertFalse(RecommendationEligibility.accepts(japaneseTrack, multiPrefs))
    }

    @Test
    fun unknownLanguage_doesNotCauseHomeToBeEmpty() {
        // Track with standard title and no lyrics language
        val standardTrack = Track("1", "Starlight", "Muse", lyricsLanguage = null)
        val englishPrefs = RecommendationPreferences(languages = setOf("English"))

        // Standard track must be accepted for English/compatible listener
        assertTrue(RecommendationEligibility.accepts(standardTrack, englishPrefs))

        // Empty preferences accepts everything
        val emptyPrefs = RecommendationPreferences(languages = emptySet())
        assertTrue(RecommendationEligibility.accepts(standardTrack, emptyPrefs))
    }

    @Test
    fun multilingualClassification_identifiesMultipleLanguages() {
        val bilingualLyrics = "You and I will be there when they call. Este amor es para siempre con el corazon de mi vida."
        val identified = LyricsLanguage.identify(bilingualLyrics)
        assertNotNull(identified)
        assertTrue("Should detect English and Spanish", identified!!.contains("English") && identified.contains("Spanish"))
    }
}
