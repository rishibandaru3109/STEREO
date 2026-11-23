package com.synapse.recommendation

import com.synapse.recommendation.discovery.DiscoveryIntelligence
import com.synapse.recommendation.model.CandidateSource
import com.synapse.recommendation.model.ExplanationType
import com.synapse.recommendation.model.RecommendationExplanation
import com.synapse.recommendation.model.RecommendationResult
import com.synapse.recommendation.model.SynapseTrack
import com.synapse.recommendation.model.TasteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiscoveryIntelligenceTest {

    private lateinit var discoveryIntelligence: DiscoveryIntelligence

    @Before
    fun setUp() {
        discoveryIntelligence = DiscoveryIntelligence()
    }

    private fun sampleResult(id: String, artist: String, score: Double): RecommendationResult {
        return RecommendationResult(
            track = SynapseTrack(id = id, title = "Title $id", artist = artist, durationSeconds = 180L),
            rankScore = score,
            explanation = RecommendationExplanation(ExplanationType.TOP_ARTIST_AFFINITY, "Test"),
            source = CandidateSource.SONG_RADIO
        )
    }

    @Test
    fun testArtistDiversification_preventsThreeConsecutiveTracks() {
        // Pool has 5 tracks by Artist A with high scores, and 2 tracks by Artist B with slightly lower scores
        val candidates = listOf(
            sampleResult("a1", "Coldplay", 90.0),
            sampleResult("a2", "Coldplay", 89.0),
            sampleResult("a3", "Coldplay", 88.0),
            sampleResult("a4", "Coldplay", 87.0),
            sampleResult("b1", "Keane", 85.0),
            sampleResult("b2", "Travis", 84.0)
        )

        val reranked = discoveryIntelligence.rerankWithDiversity(
            rankedResults = candidates,
            profile = TasteProfile(),
            discoveryAlpha = 0.80,
            maxResults = 6,
            maxConsecutivePerArtist = 2
        )

        // Verify that no 3 consecutive tracks are by the same artist
        for (i in 0 until reranked.size - 2) {
            val a1 = reranked[i].track.artist
            val a2 = reranked[i + 1].track.artist
            val a3 = reranked[i + 2].track.artist
            val allThreeEqual = (a1 == a2 && a2 == a3)
            assertTrue("Detected 3 consecutive tracks by artist $a1 at index $i", !allThreeEqual)
        }
    }

    @Test
    fun testDiscoverySlider_adventurousModeElevatesVariety() {
        val candidates = listOf(
            sampleResult("c1", "Radiohead", 95.0),
            sampleResult("c2", "Radiohead", 94.0),
            sampleResult("c3", "Radiohead", 93.0),
            sampleResult("d1", "The Smile", 80.0),
            sampleResult("d2", "Thom Yorke", 79.0),
            sampleResult("e1", "Arcade Fire", 75.0),
            sampleResult("f1", "Sigur Ros", 70.0)
        )

        val familiar = discoveryIntelligence.rerankWithDiversity(
            rankedResults = candidates,
            profile = TasteProfile(),
            discoveryAlpha = 0.90, // Familiar
            maxResults = 4
        )

        val adventurous = discoveryIntelligence.rerankWithDiversity(
            rankedResults = candidates,
            profile = TasteProfile(),
            discoveryAlpha = 0.30, // Adventurous
            maxResults = 4
        )

        // In familiar mode, top scores dominate
        assertEquals("Radiohead", familiar[0].track.artist)
        assertEquals("Radiohead", familiar[1].track.artist)

        // In adventurous mode, diversity penalty pushes novel artists higher up
        val adventurousArtists = adventurous.map { it.track.artist }.distinct()
        assertTrue(adventurousArtists.size >= 3)
    }
}
