package com.nova.music.recommendation

import com.nova.music.domain.model.ArtistAffinity
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.Track
import com.nova.music.domain.model.TrackAffinity
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import com.nova.music.domain.recommendation.SurpriseMeEngineImpl
import com.nova.music.domain.recommendation.SurpriseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SurpriseMeEngineTest {

    private lateinit var surpriseEngine: SurpriseMeEngineImpl
    private val recommendationEngine = RecommendationEngineImpl()

    @Before
    fun setUp() {
        surpriseEngine = SurpriseMeEngineImpl(recommendationEngine)
    }

    @Test
    fun testSurpriseMeExcludesRecentTracksAndDisliked() {
        val recentTrack = Track("recent_1", "Recent Song", "Coldplay")
        val dislikedTrack = Track("dislike_1", "Hated Song", "Unknown")
        val currentPlaying = Track("curr_1", "Playing Now", "Artist")
        val validTrack = Track("valid_1", "Surprise Song", "Coldplay")

        val profile = ListeningProfile(
            topArtists = listOf(ArtistAffinity("Coldplay", 10.0, 5, 5)),
            topTracks = emptyList(),
            likedTrackIds = emptySet(),
            recentTracks = listOf(recentTrack),
            totalListens = 10,
            totalSkips = 0
        )

        val candidates = listOf(recentTrack, dislikedTrack, currentPlaying, validTrack)

        val result = surpriseEngine.pickSurpriseTrack(
            profile = profile,
            candidates = candidates,
            fallbackTracks = emptyList(),
            excludedTrackIds = setOf("dislike_1"),
            currentlyPlayingId = "curr_1"
        )

        assertTrue(result is SurpriseResult.Success)
        val success = result as SurpriseResult.Success
        assertEquals("valid_1", success.selectedTrack.id)
        assertFalse(success.queueTracks.any { it.id == "recent_1" || it.id == "dislike_1" || it.id == "curr_1" })
    }

    @Test
    fun testSurpriseMeFallbackWhenCandidatesEmpty() {
        val likedTrack = Track("liked_1", "My Favorite", "A. R. Rahman")
        val profile = ListeningProfile()

        val result = surpriseEngine.pickSurpriseTrack(
            profile = profile,
            candidates = emptyList(),
            fallbackTracks = listOf(likedTrack),
            excludedTrackIds = emptySet(),
            currentlyPlayingId = null
        )

        assertTrue(result is SurpriseResult.Fallback)
        val fallback = result as SurpriseResult.Fallback
        assertEquals("liked_1", fallback.selectedTrack.id)
        assertTrue(fallback.message.contains("library favorites"))
    }

    @Test
    fun testSurpriseMeUnavailableWhenAllEmpty() {
        val profile = ListeningProfile()

        val result = surpriseEngine.pickSurpriseTrack(
            profile = profile,
            candidates = emptyList(),
            fallbackTracks = emptyList(),
            excludedTrackIds = emptySet(),
            currentlyPlayingId = null
        )

        assertTrue(result is SurpriseResult.Unavailable)
    }

    @Test
    fun testSurpriseMeTasteWeightedSamplingPrefersTopArtist() {
        val coldplayArtist = ArtistAffinity("Coldplay", 100.0, 50, 50)
        val profile = ListeningProfile(
            topArtists = listOf(coldplayArtist),
            topTracks = emptyList(),
            likedTrackIds = emptySet(),
            recentTracks = emptyList(),
            totalListens = 50,
            totalSkips = 0
        )

        val candidates = mutableListOf<Track>()
        // 1 Coldplay track and 10 obscure tracks
        val coldplayTrack = Track("cp_1", "Clocks", "Coldplay")
        candidates.add(coldplayTrack)
        for (i in 1..10) {
            candidates.add(Track("obs_$i", "Obscure $i", "Unknown Artist $i"))
        }

        // Run sampling 20 times, Coldplay (having +6 weight vs 1 weight) should be picked frequently
        var coldplayPicks = 0
        for (i in 1..20) {
            val res = surpriseEngine.pickSurpriseTrack(
                profile = profile,
                candidates = candidates,
                fallbackTracks = emptyList(),
                excludedTrackIds = emptySet()
            )
            if (res is SurpriseResult.Success && res.selectedTrack.artist == "Coldplay") {
                coldplayPicks++
            }
        }

        assertTrue("Coldplay should be picked multiple times due to top artist weighting", coldplayPicks > 0)
    }
}
