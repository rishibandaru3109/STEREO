package com.synapse.recommendation

import com.synapse.recommendation.event.EventIntelligence
import com.synapse.recommendation.event.ListeningEvent
import com.synapse.recommendation.event.SkipNature
import com.synapse.recommendation.model.SynapseTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventIntelligenceTest {

    private lateinit var eventIntelligence: EventIntelligence

    @Before
    fun setUp() {
        eventIntelligence = EventIntelligence(
            minDurationNoiseMs = 5_000L,
            maxDurationSaturationMs = 180_000L
        )
    }

    @Test
    fun testDurationClamping_shortTrackVsLongTrack() {
        // A 2-minute pop song (120s): 100s listened = ~83% complete
        val scoreShort = eventIntelligence.computeAppreciationScore(
            listeningDurationMs = 100_000L,
            totalTrackDurationMs = 120_000L
        )

        // A 15-minute symphony (900s): 180s listened = 20% complete, but hits saturation threshold
        val scoreLong = eventIntelligence.computeAppreciationScore(
            listeningDurationMs = 180_000L,
            totalTrackDurationMs = 900_000L
        )

        // Both should be normalized strictly in [0.0, 1.0]
        assertTrue(scoreShort in 0.0..1.0)
        assertTrue(scoreLong in 0.0..1.0)
        // Short song with high completion ratio should have strong appreciation
        assertTrue(scoreShort > 0.80)
        // Saturation caps long listen at 1.0, avoiding unbounded infinite weight
        assertEquals(1.0, scoreLong, 0.01)
    }

    @Test
    fun testSkipClassification_immediateDislike() {
        // Skipped in 4 seconds
        val nature = eventIntelligence.classifySkip(
            listeningDurationMs = 4_000L,
            totalTrackDurationMs = 210_000L,
            isManualSkip = true
        )
        assertEquals(SkipNature.IMMEDIATE_DISLIKE, nature)
    }

    @Test
    fun testSkipClassification_moodMismatch() {
        // Skipped in 22 seconds
        val nature = eventIntelligence.classifySkip(
            listeningDurationMs = 22_000L,
            totalTrackDurationMs = 210_000L,
            isManualSkip = true
        )
        assertEquals(SkipNature.MOOD_MISMATCH, nature)
    }

    @Test
    fun testSkipClassification_outroTransition() {
        // Skipped at 190s of 200s track (95% complete) -> Outro transition
        val nature = eventIntelligence.classifySkip(
            listeningDurationMs = 190_000L,
            totalTrackDurationMs = 200_000L,
            isManualSkip = true
        )
        assertEquals(SkipNature.OUTRO_TRANSITION, nature)
    }

    @Test
    fun testSkipClassification_queueSeekBurst() {
        // Rapid skip in 2 seconds while skipping through queue
        val nature = eventIntelligence.classifySkip(
            listeningDurationMs = 2_000L,
            totalTrackDurationMs = 200_000L,
            isManualSkip = true,
            recentSkipBurstCount = 3
        )
        assertEquals(SkipNature.QUEUE_SEEK, nature)
    }

    @Test
    fun testEventDeduplication_preventsSpam() {
        val now = 1000000L
        // First event accepted
        assertTrue(eventIntelligence.shouldRecordEvent("track_1", 15_000L, now))
        // Immediate duplicate for same track within 500ms rejected
        assertFalse(eventIntelligence.shouldRecordEvent("track_1", 15_200L, now + 500L))
        // Event after 2000ms accepted
        assertTrue(eventIntelligence.shouldRecordEvent("track_1", 20_000L, now + 2500L))
        // Different track accepted immediately
        assertTrue(eventIntelligence.shouldRecordEvent("track_2", 10_000L, now + 2600L))
    }

    @Test
    fun testDiscardTrivialNoiseUnder2Seconds() {
        assertFalse(eventIntelligence.shouldRecordEvent("track_noise", 1_200L, 1000000L))
    }
}
