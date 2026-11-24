package com.nova.music.library

import com.nova.music.domain.model.Track
import com.nova.music.feature.library.importing.MatchStatus
import com.nova.music.feature.library.importing.RawImportTrack
import com.nova.music.feature.library.importing.TrackMatchingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatchingEngineTest {

    @Test
    fun testNormalizeTitleStripsNoise() {
        val raw = "Viva La Vida (Official Music Video) [4K Remastered]"
        val normalized = TrackMatchingEngine.normalizeTitle(raw)
        assertEquals("viva la vida", normalized)
    }

    @Test
    fun testNormalizeTitleStripsFeaturedArtists() {
        val raw = "Stay (feat. Justin Bieber)"
        val normalized = TrackMatchingEngine.normalizeTitle(raw)
        assertEquals("stay", normalized)
    }

    @Test
    fun testExactMatchScoring() {
        val raw = RawImportTrack(
            title = "Viva La Vida",
            artist = "Coldplay",
            durationSeconds = 242L
        )
        val candidate = Track(
            id = "track_1",
            title = "Viva La Vida",
            artist = "Coldplay",
            durationSeconds = 242L
        )

        val score = TrackMatchingEngine.scoreCandidate(raw, candidate)
        assertTrue("Exact match score should be >= 0.95, was $score", score >= 0.95f)

        val matched = TrackMatchingEngine.matchTrack(raw, listOf(candidate))
        assertEquals(MatchStatus.EXACT_MATCH, matched.status)
        assertEquals("track_1", matched.matchedTrack?.id)
    }

    @Test
    fun testFuzzyMatchWithPunctuationAndNoise() {
        val raw = RawImportTrack(
            title = "Blinding Lights",
            artist = "The Weeknd",
            durationSeconds = 200L
        )
        val candidate = Track(
            id = "track_2",
            title = "Blinding Lights (Official Video)",
            artist = "The Weeknd",
            durationSeconds = 203L
        )

        val score = TrackMatchingEngine.scoreCandidate(raw, candidate)
        assertTrue("Fuzzy match score should be >= 0.85, was $score", score >= 0.85f)

        val matched = TrackMatchingEngine.matchTrack(raw, listOf(candidate))
        assertTrue(matched.status == MatchStatus.EXACT_MATCH || matched.status == MatchStatus.HIGH_CONFIDENCE)
        assertEquals("track_2", matched.matchedTrack?.id)
    }

    @Test
    fun testCoverAndKaraokePenaltyPreventsHijacking() {
        val raw = RawImportTrack(
            title = "Yesterday",
            artist = "The Beatles",
            durationSeconds = 125L
        )
        val originalCandidate = Track(
            id = "orig_beatles",
            title = "Yesterday",
            artist = "The Beatles",
            durationSeconds = 125L
        )
        val coverCandidate = Track(
            id = "cover_track",
            title = "Yesterday (Acoustic Cover)",
            artist = "The Beatles Tribute Band",
            durationSeconds = 125L
        )
        val karaokeCandidate = Track(
            id = "karaoke_track",
            title = "Yesterday (Karaoke Version)",
            artist = "The Beatles",
            durationSeconds = 125L
        )

        val origScore = TrackMatchingEngine.scoreCandidate(raw, originalCandidate)
        val coverScore = TrackMatchingEngine.scoreCandidate(raw, coverCandidate)
        val karaokeScore = TrackMatchingEngine.scoreCandidate(raw, karaokeCandidate)

        assertTrue("Original score ($origScore) should be higher than cover score ($coverScore)", origScore > coverScore)
        assertTrue("Original score ($origScore) should be higher than karaoke score ($karaokeScore)", origScore > karaokeScore)

        val matched = TrackMatchingEngine.matchTrack(raw, listOf(coverCandidate, karaokeCandidate, originalCandidate))
        assertEquals("orig_beatles", matched.matchedTrack?.id)
    }

    @Test
    fun testDurationMismatchPenalty() {
        val raw = RawImportTrack(
            title = "Track A",
            artist = "Artist A",
            durationSeconds = 180L
        )
        val closeDurationCandidate = Track(
            id = "close_dur",
            title = "Track A",
            artist = "Artist A",
            durationSeconds = 184L
        )
        val hugeMismatchCandidate = Track(
            id = "huge_mismatch",
            title = "Track A",
            artist = "Artist A",
            durationSeconds = 600L // 10 minutes vs 3 minutes
        )

        val closeScore = TrackMatchingEngine.scoreCandidate(raw, closeDurationCandidate)
        val hugeMismatchScore = TrackMatchingEngine.scoreCandidate(raw, hugeMismatchCandidate)

        assertTrue("Close duration score ($closeScore) should be higher than huge mismatch ($hugeMismatchScore)", closeScore > hugeMismatchScore)
    }

    @Test
    fun testEmptyCandidatesReturnsUnmatched() {
        val raw = RawImportTrack(title = "Unknown Song", artist = "Unknown Artist")
        val result = TrackMatchingEngine.matchTrack(raw, emptyList())
        assertEquals(MatchStatus.UNMATCHED, result.status)
        assertNull(result.matchedTrack)
        assertEquals(0f, result.confidence)
    }
}
