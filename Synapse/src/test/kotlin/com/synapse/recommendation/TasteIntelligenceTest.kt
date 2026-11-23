package com.synapse.recommendation

import com.synapse.recommendation.event.ListeningEvent
import com.synapse.recommendation.model.FeedbackType
import com.synapse.recommendation.model.SynapseTrack
import com.synapse.recommendation.model.UserFeedback
import com.synapse.recommendation.taste.TasteIntelligence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TasteIntelligenceTest {

    private lateinit var tasteIntelligence: TasteIntelligence

    @Before
    fun setUp() {
        tasteIntelligence = TasteIntelligence()
    }

    private fun sampleTrack(id: String, artist: String, genres: List<String> = listOf("Rock")): SynapseTrack {
        return SynapseTrack(
            id = id,
            title = "Song $id",
            artist = artist,
            genres = genres,
            durationSeconds = 180L
        )
    }

    @Test
    fun testCategoryInterestNormalization_LuAndWuEq11() {
        val rockTrack = sampleTrack("t1", "Artist A", listOf("Rock"))
        val jazzTrack = sampleTrack("t2", "Artist B", listOf("Jazz"))

        // Listen to Rock for 60 seconds
        tasteIntelligence.processEvent(
            ListeningEvent(
                track = rockTrack,
                listeningDurationMs = 60_000L,
                totalTrackDurationMs = 180_000L,
                completionRatio = 0.33f
            )
        )

        // Listen to Jazz for 40 seconds
        tasteIntelligence.processEvent(
            ListeningEvent(
                track = jazzTrack,
                listeningDurationMs = 40_000L,
                totalTrackDurationMs = 180_000L,
                completionRatio = 0.22f
            )
        )

        val profile = tasteIntelligence.getTasteProfile().longTerm
        val rockInterest = profile.getCategoryWeight("Rock")
        val jazzInterest = profile.getCategoryWeight("Jazz")

        // Total = 100s. Rock = 60/100 = 0.60. Jazz = 40/100 = 0.40
        assertEquals(0.60, rockInterest, 0.05)
        assertEquals(0.40, jazzInterest, 0.05)
        // Sum of all normalized category weights should equal 1.0
        val sum = profile.categoryInterests.sumOf { it.normalizedWeight }
        assertEquals(1.0, sum, 0.01)
    }

    @Test
    fun testDualHorizonSeparation_shortTermDoesNotOverwriteLongTerm() {
        val persistentArtist = "Pink Floyd"
        val tempArtist = "Workout EDM"

        // Build 5 persistent listens for Pink Floyd
        for (i in 1..5) {
            tasteIntelligence.processEvent(
                ListeningEvent(
                    track = sampleTrack("pf_$i", persistentArtist, listOf("Prog Rock")),
                    listeningDurationMs = 180_000L,
                    totalTrackDurationMs = 180_000L,
                    completionRatio = 1.0f
                )
            )
        }

        val preProfile = tasteIntelligence.getTasteProfile()
        val initialPfAffinity = preProfile.longTerm.getArtistAffinity(persistentArtist)
        assertTrue(initialPfAffinity > 50.0)

        // User goes for a workout session: listens to 3 tracks of Workout EDM
        for (i in 1..3) {
            tasteIntelligence.processEvent(
                ListeningEvent(
                    track = sampleTrack("edm_$i", tempArtist, listOf("EDM")),
                    listeningDurationMs = 150_000L,
                    totalTrackDurationMs = 180_000L,
                    completionRatio = 0.83f
                )
            )
        }

        val postProfile = tasteIntelligence.getTasteProfile()

        // Short-term session should reflect Workout EDM
        assertEquals(tempArtist, postProfile.shortTerm.currentArtistCentroid)

        // But Long-term profile must PRESERVE Pink Floyd as top artist!
        val topLongTermArtist = postProfile.longTerm.topArtists.first().artistName
        assertEquals(persistentArtist, topLongTermArtist)
    }

    @Test
    fun testExplicitDislike_addsToBlacklistAndPenalizes() {
        val track = sampleTrack("bad_track", "Annoying Artist")

        tasteIntelligence.processFeedback(
            UserFeedback(
                targetId = track.id,
                feedbackType = FeedbackType.DISLIKE
            )
        )

        val profile = tasteIntelligence.getTasteProfile()
        assertTrue(profile.longTerm.dislikedTrackIds.contains(track.id))
        assertTrue(profile.activePenalties[track.id] ?: 0.0 >= 100.0)
    }

    @Test
    fun testProfileReset() {
        val track = sampleTrack("t1", "Artist A")
        tasteIntelligence.processEvent(
            ListeningEvent(
                track = track,
                listeningDurationMs = 180_000L,
                totalTrackDurationMs = 180_000L,
                completionRatio = 1.0f
            )
        )

        tasteIntelligence.resetShortTermProfile()
        assertTrue(tasteIntelligence.getTasteProfile().shortTerm.activeSessionTracks.isEmpty())
        assertFalse(tasteIntelligence.getTasteProfile().longTerm.topArtists.isEmpty())

        tasteIntelligence.resetLongTermProfile()
        assertTrue(tasteIntelligence.getTasteProfile().longTerm.topArtists.isEmpty())
    }

    @Test
    fun testSingleSkipDoesNotPenalizeArtist_MultipleSkipsPenalize_RecoveryOnPositive() {
        val artist = "Taylor Swift"
        val track1 = sampleTrack("ts_1", artist)
        val track2 = sampleTrack("ts_2", artist)
        val track3 = sampleTrack("ts_3", artist)

        // 1. Initial complete listen establishes affinity
        tasteIntelligence.processEvent(
            ListeningEvent(
                track = track1,
                listeningDurationMs = 180_000L,
                totalTrackDurationMs = 180_000L,
                completionRatio = 1.0f
            )
        )
        val initialAffinity = tasteIntelligence.getTasteProfile().longTerm.getArtistAffinity(artist)
        assertTrue(initialAffinity > 0.0)

        // 2. Single early skip (e.g., Love Story skipped at 3.5s)
        tasteIntelligence.processEvent(
            ListeningEvent(
                track = track2,
                listeningDurationMs = 3_500L,
                totalTrackDurationMs = 180_000L,
                completionRatio = 0.02f,
                isManualSkip = true
            )
        )
        val profileAfterSingleSkip = tasteIntelligence.getTasteProfile()
        // Track 2 has a skip penalty
        assertTrue(profileAfterSingleSkip.activePenalties.containsKey(track2.id))
        // But artist-level penalty must NOT be applied for a single accidental skip
        assertFalse(profileAfterSingleSkip.activePenalties.containsKey(artist.lowercase()))
        // Artist remains in top artists
        assertTrue(profileAfterSingleSkip.longTerm.topArtists.any { it.artistName.equals(artist, ignoreCase = true) })

        // 3. Second early skip establishes repeated skip pattern
        tasteIntelligence.processEvent(
            ListeningEvent(
                track = track3,
                listeningDurationMs = 4_000L,
                totalTrackDurationMs = 180_000L,
                completionRatio = 0.02f,
                isManualSkip = true
            )
        )
        val profileAfterDoubleSkip = tasteIntelligence.getTasteProfile()
        // Now artist-level penalty is applied
        assertTrue(profileAfterDoubleSkip.activePenalties.containsKey(artist.lowercase()))

        // 4. Recovery: User likes a track by the artist
        tasteIntelligence.processFeedback(
            UserFeedback(
                targetId = track1.id,
                feedbackType = FeedbackType.LIKE
            )
        )
        val profileAfterRecovery = tasteIntelligence.getTasteProfile()
        // Artist penalty is cleared
        assertFalse(profileAfterRecovery.activePenalties.containsKey(artist.lowercase()))
    }
}
