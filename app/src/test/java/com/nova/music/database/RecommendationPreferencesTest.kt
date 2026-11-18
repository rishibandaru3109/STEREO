package com.nova.music.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nova.music.data.repository.MusicPreferencesRepositoryImpl
import com.nova.music.data.repository.HomeRepositoryImpl
import com.nova.music.data.repository.ListeningProfileRepositoryImpl
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.CandidateProvider
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationPreferencesTest {
    private class InMemoryPreferences : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        var writes = 0
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            writes++
            return transform(data.value).also { data.value = it }
        }
    }

    @Test
    fun explicitLanguageSelectionIsSavedTogetherAndResetRemovesIt() = runTest {
        val store = InMemoryPreferences()
        val settings = MusicPreferencesRepositoryImpl(store)
        assertTrue(settings.recommendationPreferences.first().languages.isEmpty())
        settings.saveRecommendationPreferences(setOf("English"), setOf("Rock"), "Adventurous")
        assertEquals(1, store.writes)
        val reloaded = MusicPreferencesRepositoryImpl(store)
        val preferences = reloaded.recommendationPreferences.first()
        assertEquals(setOf("English"), preferences.languages)
        assertEquals(setOf("Rock"), preferences.genres)
        assertEquals("Adventurous", preferences.discoveryMode)
        assertTrue(reloaded.isOnboardingCompleted.first())
        reloaded.resetRecommendationPreferences()
        assertTrue(settings.recommendationPreferences.first().languages.isEmpty())
    }

    @Test
    fun homeMixesAndDiscoverRejectMismatchedCachedTracksAfterPreferenceChange() = runTest {
        val settings = MusicPreferencesRepositoryImpl(InMemoryPreferences())
        val trackDao = FakeTrackDao()
        val cacheDao = FakeRecommendationDao()
        val profile = ListeningProfileRepositoryImpl(
            FakeHistoryDao(trackDao), FakeLikedSongDao(trackDao), trackDao, cacheDao
        )
        val catalog = object : CandidateProvider {
            var available = true
            var requests = 0

            override suspend fun getCandidates(profile: ListeningProfile, excludedTrackIds: Set<String>): List<Track> {
                requests++
                if (!available) return emptyList()
                return listOf("English", "Hindi").flatMap { language ->
                    (1..30).map { i ->
                        Track("$language-$i", "Song $i", "Artist $i", lyricsLanguage = language)
                    }
                }
            }
        }

        val home = HomeRepositoryImpl(profile, catalog, RecommendationEngineImpl(), cacheDao, settings)
        settings.saveRecommendationPreferences(setOf("English"), emptySet(), "Familiar")
        val englishFeed = home.getHomeFeed(false).getOrThrow()
        assertTrue(englishFeed.any { it.type == SectionType.MADE_FOR_YOU })
        assertTrue(englishFeed.any { it.type == SectionType.DISCOVER_WEEKLY })
        val englishTracks = englishFeed.flatMap { section -> section.tracks + section.mixes.flatMap { it.tracks } }
        assertTrue(englishTracks.isNotEmpty())
        assertTrue(englishTracks.all { it.lyricsLanguage == "English" })

        catalog.available = false
        assertEquals(englishFeed, home.getHomeFeed(false).getOrThrow())
        assertEquals(1, catalog.requests)
        settings.saveRecommendationPreferences(setOf("Hindi"), emptySet(), "Familiar")
        assertTrue(home.getHomeFeed(false).getOrThrow().isEmpty())
        assertEquals(2, catalog.requests)
    }
}
