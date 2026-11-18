package com.nova.music.recommendation

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.TrackAffinity
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.CatalogCandidateProvider
import com.nova.music.domain.recommendation.LyricsLanguage
import com.nova.music.domain.recommendation.RecommendationEligibility
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import com.nova.music.domain.recommendation.SurpriseMeEngineImpl
import com.nova.music.domain.recommendation.SurpriseResult
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.RecommendationPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PersonalizedCatalogTest {
    private class Catalog : MusicExtractor {
        val seeds = mutableListOf<String>()
        var homeRequests = 0

        override suspend fun getSongRadio(trackId: String): Result<List<Track>> {
            seeds += trackId
            return Result.success(listOf(
                Track("$trackId-english", "First song", "Artist $trackId", lyricsLanguage = "English"),
                Track("$trackId-copy", "First Song!", "Artist $trackId", lyricsLanguage = "English"),
                Track("$trackId-telugu", "Second song", "Artist $trackId", lyricsLanguage = "Telugu"),
                Track("$trackId-unknown", "Third song", "Artist $trackId"),
                Track("$trackId-episode", "Road stories", "Artist $trackId",
                    lyricsLanguage = "English", isSpokenWord = true)
            ))
        }

        override suspend fun getHomeShelves(): Result<List<Track>> {
            homeRequests++
            return Result.success(emptyList())
        }

        override suspend fun search(query: String): Result<List<Track>> = Result.success(emptyList())
        override suspend fun getTrackDetails(trackId: String): Result<Track> = error("Unexpected lookup")
        override suspend fun resolveAudioStream(trackId: String): Result<AudioStream> = error("Unexpected stream")
    }

    @Test
    fun differentHistoriesSeedDifferentRecommendations() = runTest {
        val catalog = Catalog()
        val provider = CatalogCandidateProvider(catalog)
        val preferences = RecommendationPreferences(languages = setOf("English"))

        suspend fun idsFor(seed: String): List<String> {
            val profile = ListeningProfile(topTracks = listOf(
                TrackAffinity("skipped", "Skipped", "Artist", -20.0, 5, false),
                TrackAffinity(seed, "Seed song", "Artist $seed", 40.0, 2, false)
            ))
            return provider.getCandidates(profile, emptySet(), preferences).map { it.id }
        }

        assertEquals(listOf("one-english"), idsFor("one"))
        assertEquals(listOf("two-english"), idsFor("two"))
        assertEquals(listOf("one", "two"), catalog.seeds)
        assertEquals(0, catalog.homeRequests)
    }

    @Test
    fun lyricsPolicyRejectsUnknownAndUnselectedPartsOfMixedLyrics() {
        val track = Track("1", "A song", "Any artist")
        val preferences = RecommendationPreferences(languages = setOf("English"))
        assertFalse(RecommendationEligibility.accepts(track, preferences))
        assertFalse(RecommendationEligibility.accepts(track.copy(lyricsLanguage = "English+Spanish"), preferences))
        assertEquals("English", LyricsLanguage.identify("You and I will be there when they call. This is what we know."))
        assertNull(LyricsLanguage.identify("tum mere saath ho mera dil kya hai nahin mujhe"))
    }

    @Test
    fun surpriseFallbackCannotReintroduceExcludedSongs() {
        val blocked = Track("blocked", "Blocked song", "Artist")
        val current = Track("current", "Current song", "Artist")
        val liked = Track("liked", "Liked song", "Artist")
        val result = SurpriseMeEngineImpl(RecommendationEngineImpl()).pickSurpriseTrack(
            ListeningProfile(), listOf(blocked, current), listOf(liked, liked, blocked, current),
            excludedTrackIds = setOf(blocked.id), currentlyPlayingId = current.id
        ) as SurpriseResult.Fallback

        assertEquals(listOf(liked), result.queueTracks)
    }
}
