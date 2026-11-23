package com.synapse.recommendation

import com.synapse.recommendation.candidate.InMemoryCandidateProvider
import com.synapse.recommendation.engine.SynapseRecommendationEngine
import com.synapse.recommendation.event.ListeningEvent
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.SynapseTrack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [SYNTHETIC TEST SUITE]
 *
 * NOTE: These synthetic listening sequences demonstrate programmatic state evolution
 * across multi-day listening patterns. Synthetic tests verify algorithmic invariants
 * and state transitions; they do not establish real-user recommendation quality.
 */
class SyntheticListeningSequenceTest {

    private lateinit var engine: SynapseRecommendationEngine
    private lateinit var catalog: List<SynapseTrack>

    @Before
    fun setUp() {
        catalog = listOf(
            SynapseTrack("t_indie_1", "Ocean Eyes", "Billie Eilish", genres = listOf("Indie Pop"), durationSeconds = 200L),
            SynapseTrack("t_indie_2", "Bad Guy", "Billie Eilish", genres = listOf("Indie Pop"), durationSeconds = 194L),
            SynapseTrack("t_indie_3", "Bellyache", "Billie Eilish", genres = listOf("Indie Pop"), durationSeconds = 210L),
            SynapseTrack("t_rock_1", "Karma Police", "Radiohead", genres = listOf("Alt Rock"), durationSeconds = 260L),
            SynapseTrack("t_rock_2", "Creep", "Radiohead", genres = listOf("Alt Rock"), durationSeconds = 238L),
            SynapseTrack("t_edm_1", "One More Time", "Daft Punk", genres = listOf("Electronic"), durationSeconds = 320L),
            SynapseTrack("t_edm_2", "Harder Better", "Daft Punk", genres = listOf("Electronic"), durationSeconds = 224L),
            SynapseTrack("t_pop_1", "Blank Space", "Taylor Swift", genres = listOf("Pop"), durationSeconds = 231L),
            SynapseTrack("t_pop_2", "Anti-Hero", "Taylor Swift", genres = listOf("Pop"), durationSeconds = 200L)
        )

        engine = SynapseRecommendationEngine(candidateProvider = InMemoryCandidateProvider(catalog))
    }

    @Test
    fun testSyntheticListeningEvolution_multiDayJourney() = runBlocking {
        // --- STEP 1: Day 1 Cold Start ---
        val day1Resp = engine.recommend(RecommendationContext(maxResults = 5))
        assertEquals(5, day1Resp.recommendations.size)
        assertTrue(engine.getTasteIntelligence().getTasteProfile().longTerm.isColdStart)

        // --- STEP 2: Day 2 - User explores Billie Eilish (Indie Pop) ---
        val eilishTrack = catalog.first { it.artist == "Billie Eilish" }
        val day2BaseTime = 1700000000000L
        for (i in 1..4) {
            engine.getTasteIntelligence().processEvent(
                ListeningEvent(
                    track = eilishTrack,
                    timestamp = day2BaseTime + (i * 3600_000L), // Advancing hours
                    listeningDurationMs = 190_000L,
                    totalTrackDurationMs = 200_000L,
                    completionRatio = 0.95f
                )
            )
        }

        val day2Resp = engine.recommend(RecommendationContext(maxResults = 5))
        assertFalse(engine.getTasteIntelligence().getTasteProfile().longTerm.isColdStart)
        // Top recommendation must be Billie Eilish
        assertEquals("Billie Eilish", day2Resp.recommendations.first().track.artist)
        // Explanation must identify top artist affinity
        assertTrue(day2Resp.recommendations.first().explanation.detailText.contains("Billie Eilish"))

        // --- STEP 3: Day 3 - User skips Electronic (Daft Punk) early ---
        val edmTrack = catalog.first { it.artist == "Daft Punk" }
        engine.getTasteIntelligence().processEvent(
            ListeningEvent(
                track = edmTrack,
                timestamp = day2BaseTime + (24 * 3600_000L), // Next day
                listeningDurationMs = 4_000L,
                totalTrackDurationMs = 320_000L,
                completionRatio = 0.01f,
                isManualSkip = true
            )
        )

        val day3Resp = engine.recommend(RecommendationContext(maxResults = 5))
        // Daft Punk must NOT be the top recommendation
        assertFalse(day3Resp.recommendations.first().track.artist == "Daft Punk")

        // --- STEP 4: Day 4 - User adjusts discovery mode to Adventurous ---
        val day4Adventurous = engine.recommend(
            RecommendationContext(
                discoveryAlpha = 0.30, // Adventurous exploration
                maxResults = 5
            )
        )
        // Check artist variety
        val uniqueArtists = day4Adventurous.recommendations.map { it.track.artist }.distinct()
        assertTrue("Adventurous mode must promote artist variety", uniqueArtists.size >= 3)

        // --- STEP 5: Day 5 - User resets taste profile (Privacy Reset) ---
        engine.getTasteIntelligence().clearAll()
        val day5Resp = engine.recommend(RecommendationContext(maxResults = 5))
        assertTrue("Taste profile must return to cold start after reset", engine.getTasteIntelligence().getTasteProfile().longTerm.isColdStart)
        assertEquals(5, day5Resp.recommendations.size)
    }
}
