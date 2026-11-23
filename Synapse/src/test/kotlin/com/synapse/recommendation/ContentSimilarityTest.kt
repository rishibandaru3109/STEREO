package com.synapse.recommendation

import com.synapse.recommendation.model.SynapseTrack
import com.synapse.recommendation.similarity.ContentSimilarityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentSimilarityTest {

    private lateinit var similarityEngine: ContentSimilarityEngine

    @Before
    fun setUp() {
        similarityEngine = ContentSimilarityEngine()
    }

    @Test
    fun testSameTrack_similarityIsOne() {
        val t = SynapseTrack(id = "1", title = "Comfortably Numb", artist = "Pink Floyd", durationSeconds = 380L)
        assertEquals(1.0, similarityEngine.computeSimilarity(t, t), 0.001)
    }

    @Test
    fun testArtistSimilarity_exactAndTokenOverlap() {
        assertEquals(1.0, similarityEngine.computeArtistSimilarity("A.R. Rahman", "A.R. Rahman"), 0.001)
        // Token overlap with punctuation variance
        val sim = similarityEngine.computeArtistSimilarity("Daft Punk", "Daft Punk ft. Pharrell")
        assertTrue(sim > 0.50)
    }

    @Test
    fun testGenreSimilarity_jaccard() {
        val g1 = listOf("Rock", "Progressive Rock", "Classic Rock")
        val g2 = listOf("Classic Rock", "Blues Rock")
        val sim = similarityEngine.computeGenreSimilarity(g1, g2)
        // 1 match out of 4 union = 0.25
        assertEquals(0.25, sim, 0.01)
    }

    @Test
    fun testTitleSimilarity_stripsStopWordsAndHandlesVersions() {
        // "Time (Official Music Video)" vs "Time (Live 1974)"
        // Stop words ("official", "music", "video") are removed, leaving "time"
        val sim = similarityEngine.computeTitleSimilarity("Time (Official Music Video)", "Time")
        assertTrue(sim > 0.90)
    }

    @Test
    fun testDurationSimilarity() {
        // Two tracks of 180s and 190s (10s diff): high similarity
        val simClose = similarityEngine.computeDurationSimilarity(180L, 190L)
        assertTrue(simClose > 0.80)

        // 180s vs 600s (420s diff): zero similarity
        val simFar = similarityEngine.computeDurationSimilarity(180L, 600L)
        assertEquals(0.0, simFar, 0.01)
    }
}
