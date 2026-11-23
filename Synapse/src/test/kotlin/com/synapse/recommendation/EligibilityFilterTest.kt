package com.synapse.recommendation

import com.synapse.recommendation.filter.EligibilityFilter
import com.synapse.recommendation.model.CandidateSource
import com.synapse.recommendation.model.LongTermProfile
import com.synapse.recommendation.model.RecommendationCandidate
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.SynapseTrack
import com.synapse.recommendation.model.TasteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EligibilityFilterTest {

    private lateinit var filter: EligibilityFilter

    @Before
    fun setUp() {
        filter = EligibilityFilter()
    }

    private fun candidate(id: String, artist: String, lang: String? = null, isSpoken: Boolean = false): RecommendationCandidate {
        return RecommendationCandidate(
            track = SynapseTrack(
                id = id,
                title = "Title $id",
                artist = artist,
                language = lang,
                isSpokenWord = isSpoken,
                durationSeconds = 180L
            ),
            source = CandidateSource.SONG_RADIO
        )
    }

    @Test
    fun testBlacklistAndSuppressedArtists_areFiltered() {
        val candidates = listOf(
            candidate("t1", "Allowed Artist"),
            candidate("t2", "Hated Artist"),
            candidate("t3", "Excluded Track")
        )

        val profile = TasteProfile(
            longTerm = LongTermProfile(
                suppressedArtistNames = setOf("hated artist"),
                dislikedTrackIds = setOf("t3")
            )
        )

        val result = filter.filter(candidates, profile, RecommendationContext())
        assertEquals(1, result.eligible.size)
        assertEquals("t1", result.eligible.first().track.id)
        assertEquals(2, result.rejectedBlacklistCount)
    }

    @Test
    fun testSpokenWord_isFiltered() {
        val candidates = listOf(
            candidate("t1", "Music Artist", isSpoken = false),
            candidate("t2", "Podcast Host", isSpoken = true)
        )

        val result = filter.filter(candidates, TasteProfile(), RecommendationContext())
        assertEquals(1, result.eligible.size)
        assertEquals("t1", result.eligible.first().track.id)
        assertEquals(1, result.rejectedSpokenWordCount)
    }

    @Test
    fun testLanguageFilter_strictFiltering() {
        val candidates = listOf(
            candidate("t1", "Artist Telugu", lang = "Telugu"),
            candidate("t2", "Artist Hindi", lang = "Hindi"),
            candidate("t3", "Artist English", lang = "English")
        )

        val context = RecommendationContext(preferredLanguages = setOf("Telugu", "Hindi"))
        val result = filter.filter(candidates, TasteProfile(), context)

        assertEquals(2, result.eligible.size)
        assertTrue(result.eligible.any { it.track.id == "t1" })
        assertTrue(result.eligible.any { it.track.id == "t2" })
        assertFalse(result.eligible.any { it.track.id == "t3" })
        assertFalse(result.isFallbackActivated)
    }

    @Test
    fun testLanguageFilter_safeFallbackWhenPoolExhausted() {
        val candidates = listOf(
            candidate("t1", "Artist English 1", lang = "English"),
            candidate("t2", "Artist English 2", lang = "English")
        )

        // User requested Telugu, but candidate pool only has English tracks
        val context = RecommendationContext(preferredLanguages = setOf("Telugu"))
        val result = filter.filter(candidates, TasteProfile(), context)

        // Fallback should activate to prevent blank feed!
        assertTrue(result.isFallbackActivated)
        assertEquals(2, result.eligible.size)
    }
}
