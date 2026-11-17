package com.nova.music.recommendation

import com.nova.music.domain.model.ArtistAffinity
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import com.nova.music.domain.model.TrackAffinity
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationEngineTest {

    private lateinit var engine: RecommendationEngineImpl

    @Before
    fun setUp() {
        engine = RecommendationEngineImpl()
    }

    @Test
    fun testColdStartGeneratesStarterMixesAndQuickPicks() {
        val emptyProfile = ListeningProfile()
        assertTrue(emptyProfile.isColdStart)

        val candidates = (1..20).map { i ->
            Track(
                id = "track_$i",
                title = "Popular Song $i",
                artist = if (i % 2 == 0) "Artist A" else "Artist B"
            )
        }

        val sections = engine.generateSections(
            profile = emptyProfile,
            candidates = candidates,
            excludedTrackIds = emptySet()
        )

        assertTrue(sections.isNotEmpty())
        val quickPicks = sections.find { it.type == SectionType.QUICK_PICKS }
        val madeForYou = sections.find { it.type == SectionType.MADE_FOR_YOU }

        assertTrue("Quick picks must be generated on cold start", quickPicks != null && quickPicks.tracks.isNotEmpty())
        assertTrue("Made For You starter mixes must be generated", madeForYou != null && madeForYou.mixes.isNotEmpty())
        assertEquals("Trending Hits", madeForYou?.mixes?.first()?.title)
    }

    @Test
    fun testEstablishedProfileGeneratesPersonalizedSections() {
        val coldplay = ArtistAffinity(
            artistName = "Coldplay",
            score = 15.0,
            playCount = 10,
            completionCount = 8
        )
        val vivaLaVida = Track(
            id = "viva_1",
            title = "Viva La Vida",
            artist = "Coldplay"
        )
        val yellow = Track(
            id = "yellow_1",
            title = "Yellow",
            artist = "Coldplay"
        )

        val profile = ListeningProfile(
            topArtists = listOf(coldplay),
            topTracks = listOf(
                TrackAffinity("viva_1", "Viva La Vida", "Coldplay", 10.0, 5, true),
                TrackAffinity("yellow_1", "Yellow", "Coldplay", 8.0, 3, false)
            ),
            likedTrackIds = setOf("viva_1"),
            recentTracks = listOf(vivaLaVida, yellow),
            totalListens = 15,
            totalSkips = 1
        )
        assertFalse(profile.isColdStart)

        val candidates = mutableListOf(
            vivaLaVida,
            yellow
        ).apply {
            for (i in 3..15) {
                add(Track("cp_$i", "Coldplay Track $i", "Coldplay"))
            }
            for (i in 1..10) {
                add(Track("oa_$i", "Oasis Track $i", "Oasis"))
            }
            for (i in 1..10) {
                add(Track("ke_$i", "Keane Track $i", "Keane"))
            }
        }

        val sections = engine.generateSections(
            profile = profile,
            candidates = candidates,
            excludedTrackIds = emptySet()
        )

        val quickPicks = sections.find { it.type == SectionType.QUICK_PICKS }
        val recentlyPlayed = sections.find { it.type == SectionType.RECENTLY_PLAYED }
        val madeForYou = sections.find { it.type == SectionType.MADE_FOR_YOU }
        val becauseYouListened = sections.find { it.type == SectionType.BECAUSE_YOU_LISTENED }

        assertTrue(quickPicks != null && quickPicks.tracks.isNotEmpty())
        assertTrue(recentlyPlayed != null && recentlyPlayed.tracks.size == 2)
        assertTrue(madeForYou != null && madeForYou.mixes.isNotEmpty())
        assertTrue("Daily Mix 1 should center on top artist", madeForYou?.mixes?.first()?.title == "Daily Mix 1")
        assertTrue(becauseYouListened != null && becauseYouListened.title.contains("Coldplay"))
    }

    @Test
    fun testArtistDiversityConstraint() {
        val repetitiveTracks = listOf(
            Track("1", "Song 1", "Artist A"),
            Track("2", "Song 2", "Artist A"),
            Track("3", "Song 3", "Artist A"),
            Track("4", "Song 4", "Artist A"),
            Track("5", "Song 5", "Artist B"),
            Track("6", "Song 6", "Artist B"),
            Track("7", "Song 7", "Artist C"),
            Track("8", "Song 8", "Artist D")
        )

        val diversified = engine.diversifyTracks(repetitiveTracks, maxConsecutivePerArtist = 2)

        assertEquals(repetitiveTracks.size, diversified.size)

        // Verify that no 3 consecutive songs are from the same artist
        for (i in 0 until diversified.size - 2) {
            val a1 = diversified[i].artist
            val a2 = diversified[i + 1].artist
            val a3 = diversified[i + 2].artist
            val threeInARow = a1.equals(a2, ignoreCase = true) && a2.equals(a3, ignoreCase = true)
            assertFalse("Found 3 consecutive songs from artist $a1 at index $i", threeInARow)
        }
    }

    @Test
    fun testExcludedTracksAreOmitted() {
        val profile = ListeningProfile()
        val candidates = listOf(
            Track("good_1", "Good Song 1", "Artist A"),
            Track("blocked_1", "Blocked Song", "Artist B"),
            Track("good_2", "Good Song 2", "Artist C")
        )

        val sections = engine.generateSections(
            profile = profile,
            candidates = candidates,
            excludedTrackIds = setOf("blocked_1")
        )

        for (section in sections) {
            for (track in section.tracks) {
                assertFalse("Blocked track should not be in section", track.id == "blocked_1")
            }
            for (mix in section.mixes) {
                for (track in mix.tracks) {
                    assertFalse("Blocked track should not be in mix", track.id == "blocked_1")
                }
            }
        }
    }
}
