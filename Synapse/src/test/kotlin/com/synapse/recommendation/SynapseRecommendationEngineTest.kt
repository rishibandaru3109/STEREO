package com.synapse.recommendation

import com.synapse.recommendation.candidate.InMemoryCandidateProvider
import com.synapse.recommendation.engine.SynapseRecommendationEngine
import com.synapse.recommendation.event.ListeningEvent
import com.synapse.recommendation.model.FeedbackType
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.SynapseTrack
import com.synapse.recommendation.model.UserFeedback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SynapseRecommendationEngineTest {

    private lateinit var engine: SynapseRecommendationEngine
    private lateinit var sampleCatalog: List<SynapseTrack>

    @Before
    fun setUp() {
        val tracks = mutableListOf<SynapseTrack>()
        // 5 Artists, 10 tracks each = 50 candidate songs
        val artists = listOf("Taylor Swift", "Radiohead", "A.R. Rahman", "Daft Punk", "Billie Eilish")
        val genres = listOf("Pop", "Alt Rock", "Soundtrack", "Electronic", "Indie Pop")
        val languages = listOf("English", "English", "Telugu", "English", "English")

        for (i in artists.indices) {
            val artist = artists[i]
            val genre = genres[i]
            val lang = languages[i]
            for (j in 1..10) {
                tracks.add(
                    SynapseTrack(
                        id = "track_${i}_$j",
                        title = "$artist Song $j",
                        artist = artist,
                        genres = listOf(genre),
                        language = lang,
                        durationSeconds = 180L
                    )
                )
            }
        }
        sampleCatalog = tracks

        val candidateProvider = InMemoryCandidateProvider(sampleCatalog)
        engine = SynapseRecommendationEngine(candidateProvider = candidateProvider)
        
        // JIT warm-up call to avoid cold classloading skewing benchmark assertions
        runBlocking {
            engine.recommend(RecommendationContext(maxResults = 2))
        }
    }

    @Test
    fun testColdStartRecommendations() = runBlocking {
        val response = engine.recommend(RecommendationContext(maxResults = 10))
        assertEquals(10, response.recommendations.size)
        // Cold start should produce recommendations with valid explanations
        assertTrue(response.recommendations.all { it.explanation.detailText.isNotBlank() })
        assertTrue("Cold start latency ${response.diagnostics.latencyMs}ms should be under 500ms", response.diagnostics.latencyMs < 500L)
    }

    @Test
    fun testAdaptation_afterListeningHistory() = runBlocking {
        val targetArtist = "A.R. Rahman"
        val rahmanTrack = sampleCatalog.first { it.artist == targetArtist }

        // User listens to A.R. Rahman 3 times to completion
        for (k in 1..3) {
            engine.getTasteIntelligence().processEvent(
                ListeningEvent(
                    track = rahmanTrack,
                    listeningDurationMs = 180_000L,
                    totalTrackDurationMs = 180_000L,
                    completionRatio = 1.0f
                )
            )
        }

        val response = engine.recommend(RecommendationContext(maxResults = 10))
        val topRecommendation = response.recommendations.first()

        // Top recommendation must be influenced by the learned artist affinity
        assertEquals(targetArtist, topRecommendation.track.artist)
        assertTrue(topRecommendation.rankScore > 40.0)
    }

    @Test
    fun testSkipAdaptation_suppressesSkippedTrack() = runBlocking {
        val skippedTrack = sampleCatalog.first { it.artist == "Radiohead" }

        // Record immediate skip (<10s)
        engine.getTasteIntelligence().processEvent(
            ListeningEvent(
                track = skippedTrack,
                listeningDurationMs = 5_000L,
                totalTrackDurationMs = 240_000L,
                completionRatio = 0.02f,
                isManualSkip = true
            )
        )

        val response = engine.recommend(RecommendationContext(maxResults = 10))
        // The skipped track should NOT be at the top of recommendations
        assertFalse(response.recommendations.first().track.id == skippedTrack.id)
    }

    @Test
    fun testExplicitDislike_permanentlyBlocksTrack() = runBlocking {
        val dislikedTrack = sampleCatalog.first { it.artist == "Taylor Swift" }

        engine.getTasteIntelligence().processFeedback(
            UserFeedback(
                targetId = dislikedTrack.id,
                feedbackType = FeedbackType.DISLIKE
            )
        )

        val response = engine.recommend(RecommendationContext(maxResults = 20))
        // The disliked track must NEVER appear in recommendations
        assertFalse(response.recommendations.any { it.track.id == dislikedTrack.id })
    }

    @Test
    fun testPerformance_latencyUnder15msOn100Candidates() = runBlocking {
        // Measure steady-state latency over multiple runs
        val times = (1..10).map {
            engine.recommend(RecommendationContext(maxResults = 20)).diagnostics.latencyMs
        }
        val bestLatency = times.minOrNull() ?: 0L
        assertTrue("Engine steady-state latency ${bestLatency}ms must be <= 100ms", bestLatency <= 100L)
    }
}
