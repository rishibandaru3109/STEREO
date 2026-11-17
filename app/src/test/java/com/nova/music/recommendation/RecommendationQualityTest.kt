package com.nova.music.recommendation

import com.nova.music.domain.model.ArtistAffinity
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import com.nova.music.domain.model.TrackAffinity
import com.nova.music.domain.recommendation.ContentFilter
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import com.nova.music.domain.repository.ContentFilterMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecommendationQualityTest {

    private lateinit var engine: RecommendationEngineImpl

    @Before
    fun setUp() {
        engine = RecommendationEngineImpl()
    }

    // =========================================================================
    // CONTENT FILTERING TESTS
    // =========================================================================

    @Test
    fun testContentFilterPurgesNonMusicVideosInMusicOnlyMode() {
        val mixedCatalog = listOf(
            Track("1", "Levitating", "Dua Lipa", durationSeconds = 203),
            Track("2", "Dua Lipa Exclusive Interview with Zane Lowe", "Apple Music", durationSeconds = 3450),
            Track("3", "The Joe Rogan Experience Podcast #2100", "Podcasts", durationSeconds = 7200),
            Track("4", "Reacting to Levitating for the First Time!", "MusicVlogger", durationSeconds = 850),
            Track("5", "Harry Potter Chapter 1 Audiobook", "Narrator", durationSeconds = 1800),
            Track("6", "Blinding Lights", "The Weeknd", durationSeconds = 200),
            Track("7", "Short Notification Sound", "SFX", durationSeconds = 5),
            Track("8", "Electronic Essentials Continuous DJ Mix", "Ultra Records", durationSeconds = 3600), // Preserved 1h DJ mix
            Track("9", "Coldplay Live in São Paulo Full Concert", "Coldplay", durationSeconds = 7500) // Preserved 2h live concert
        )

        val musicOnly = ContentFilter.filterTracks(mixedCatalog, ContentFilterMode.MUSIC_ONLY)

        assertEquals(4, musicOnly.size)
        assertTrue(musicOnly.any { it.title == "Levitating" })
        assertTrue(musicOnly.any { it.title == "Blinding Lights" })
        assertTrue(musicOnly.any { it.title.contains("Continuous DJ Mix") })
        assertTrue(musicOnly.any { it.title.contains("Live in São Paulo") })
        assertFalse(musicOnly.any { it.title.contains("Interview") })
        assertFalse(musicOnly.any { it.title.contains("Podcast") })
        assertFalse(musicOnly.any { it.title.contains("Reacting") })
        assertFalse(musicOnly.any { it.title.contains("Audiobook") })
        assertFalse(musicOnly.any { it.durationSeconds < 15 })
    }

    @Test
    fun testContentFilterPreservesAllInAllContentMode() {
        val mixedCatalog = listOf(
            Track("1", "Levitating", "Dua Lipa", durationSeconds = 203),
            Track("2", "Interview with Director", "Film Companion", durationSeconds = 1200),
            Track("3", "Tech Talk Podcast Episode 4", "Tech Channel", durationSeconds = 2500)
        )

        val allContent = ContentFilter.filterTracks(mixedCatalog, ContentFilterMode.ALL_CONTENT)
        assertEquals(3, allContent.size)
    }

    // =========================================================================
    // PROFILE A: ENGLISH POP
    // =========================================================================

    @Test
    fun testProfileA_EnglishPopEnthusiast() {
        val profile = ListeningProfile(
            topArtists = listOf(
                ArtistAffinity("Dua Lipa", score = 25.0, playCount = 12, completionCount = 10),
                ArtistAffinity("The Weeknd", score = 20.0, playCount = 9, completionCount = 8),
                ArtistAffinity("Taylor Swift", score = 15.0, playCount = 6, completionCount = 5)
            ),
            topTracks = listOf(
                TrackAffinity("dl_1", "Levitating", "Dua Lipa", 15.0, 8, true),
                TrackAffinity("tw_1", "Blinding Lights", "The Weeknd", 12.0, 6, true)
            ),
            likedTrackIds = setOf("dl_1", "tw_1"),
            recentTracks = listOf(
                Track("dl_1", "Levitating", "Dua Lipa", durationSeconds = 203),
                Track("tw_1", "Blinding Lights", "The Weeknd", durationSeconds = 200)
            ),
            totalListens = 25
        )

        val candidates = (1..20).map { i ->
            when (i) {
                1 -> Track("dl_2", "Don't Start Now", "Dua Lipa", durationSeconds = 183)
                2 -> Track("tw_2", "Save Your Tears", "The Weeknd", durationSeconds = 215)
                3 -> Track("ts_1", "Cruel Summer", "Taylor Swift", durationSeconds = 178)
                4 -> Track("ts_2", "Anti-Hero", "Taylor Swift", durationSeconds = 200)
                5 -> Track("ol_1", "Vampire", "Olivia Rodrigo", durationSeconds = 219)
                6 -> Track("dl_interview", "Dua Lipa Interview at Grammy Museum", "Interviews", durationSeconds = 1800)
                7 -> Track("tw_3", "Starboy", "The Weeknd", durationSeconds = 230)
                8 -> Track("dl_3", "Physical", "Dua Lipa", durationSeconds = 193)
                9 -> Track("ts_3", "Blank Space", "Taylor Swift", durationSeconds = 231)
                else -> Track("pop_$i", "Pop Anthem $i", "Pop Artist $i", durationSeconds = 200)
            }
        }

        val filteredCandidates = ContentFilter.filterTracks(candidates, ContentFilterMode.MUSIC_ONLY)
        val sections = engine.generateSections(profile, filteredCandidates, excludedTrackIds = emptySet())

        // 1. Verify nonmusic was filtered
        val allSectionTracks = sections.flatMap { it.tracks } + sections.flatMap { it.mixes.flatMap { m -> m.tracks } }
        assertFalse(allSectionTracks.any { it.title.contains("Interview") })

        // 2. Quick Picks should prioritize favorite pop tracks
        val quickPicks = sections.find { it.type == SectionType.QUICK_PICKS }
        assertNotNull(quickPicks)
        assertTrue(quickPicks!!.tracks.any { it.artist == "Dua Lipa" || it.artist == "The Weeknd" })

        // 3. Verify cross-section deduplication
        verifyStrictCrossSectionDeduplication(sections)
    }

    // =========================================================================
    // PROFILE B: TELUGU FILM MUSIC
    // =========================================================================

    @Test
    fun testProfileB_TeluguFilmMusicFan() {
        val profile = ListeningProfile(
            topArtists = listOf(
                ArtistAffinity("Anirudh Ravichander", score = 30.0, playCount = 15, completionCount = 12),
                ArtistAffinity("Devi Sri Prasad", score = 22.0, playCount = 10, completionCount = 8),
                ArtistAffinity("Sid Sriram", score = 18.0, playCount = 7, completionCount = 6)
            ),
            topTracks = listOf(
                TrackAffinity("ani_1", "Fear Song", "Anirudh Ravichander", 18.0, 9, true),
                TrackAffinity("dsp_1", "Pushpa Pushpa", "Devi Sri Prasad", 14.0, 7, true)
            ),
            likedTrackIds = setOf("ani_1", "dsp_1"),
            recentTracks = listOf(
                Track("ani_1", "Fear Song", "Anirudh Ravichander", durationSeconds = 195),
                Track("dsp_1", "Pushpa Pushpa", "Devi Sri Prasad", durationSeconds = 230)
            ),
            totalListens = 35
        )

        val candidates = (1..20).map { i ->
            when (i) {
                1 -> Track("ani_2", "Chuttamalle", "Anirudh Ravichander", durationSeconds = 220)
                2 -> Track("ani_3", "Hukum", "Anirudh Ravichander", durationSeconds = 205)
                3 -> Track("dsp_2", "Oo Antava", "Devi Sri Prasad", durationSeconds = 228)
                4 -> Track("sid_1", "Samajavaragamana", "Sid Sriram", durationSeconds = 214)
                5 -> Track("sid_2", "Srivalli", "Sid Sriram", durationSeconds = 221)
                6 -> Track("ani_4", "Naa Ready", "Anirudh Ravichander", durationSeconds = 248)
                7 -> Track("tel_podcast", "Telugu Cinema Review Podcast Ep 12", "Tollywood Podcasts", durationSeconds = 3400)
                else -> Track("telugu_$i", "Telugu Melody $i", "Telugu Singer $i", durationSeconds = 210)
            }
        }

        val filteredCandidates = ContentFilter.filterTracks(candidates, ContentFilterMode.MUSIC_ONLY)
        val sections = engine.generateSections(profile, filteredCandidates, excludedTrackIds = emptySet())

        // Verify Telugu artists dominate Quick Picks
        val quickPicks = sections.find { it.type == SectionType.QUICK_PICKS }
        assertNotNull(quickPicks)
        assertTrue(quickPicks!!.tracks.any { it.artist == "Anirudh Ravichander" || it.artist == "Devi Sri Prasad" })
        assertFalse(sections.flatMap { it.tracks }.any { it.title.contains("Podcast") })

        verifyStrictCrossSectionDeduplication(sections)
    }

    // =========================================================================
    // PROFILE C: ELECTRONIC / EDM PURIST
    // =========================================================================

    @Test
    fun testProfileC_ElectronicEdmPurist() {
        val profile = ListeningProfile(
            topArtists = listOf(
                ArtistAffinity("Deadmau5", score = 25.0, playCount = 10, completionCount = 9),
                ArtistAffinity("Rufus Du Sol", score = 20.0, playCount = 8, completionCount = 7),
                ArtistAffinity("Eric Prydz", score = 15.0, playCount = 6, completionCount = 5)
            ),
            topTracks = listOf(
                TrackAffinity("dm_1", "Strobe", "Deadmau5", 15.0, 6, true),
                TrackAffinity("rds_1", "Innerbloom", "Rufus Du Sol", 12.0, 5, true)
            ),
            likedTrackIds = setOf("dm_1", "rds_1"),
            recentTracks = listOf(
                Track("dm_1", "Strobe", "Deadmau5", durationSeconds = 637),
                Track("rds_1", "Innerbloom", "Rufus Du Sol", durationSeconds = 578)
            ),
            totalListens = 20
        )

        val candidates = (1..20).map { i ->
            when (i) {
                1 -> Track("dm_2", "Ghosts 'n' Stuff", "Deadmau5", durationSeconds = 328)
                2 -> Track("rds_2", "On My Knees", "Rufus Du Sol", durationSeconds = 265)
                3 -> Track("ep_1", "Opus", "Eric Prydz", durationSeconds = 543)
                4 -> Track("lane8_1", "Brightest Lights", "Lane 8", durationSeconds = 280)
                5 -> Track("dj_mix", "Anjunadeep Live Set 2024 Continuous Mix", "Anjunadeep", durationSeconds = 4200)
                6 -> Track("dm_3", "Raise Your Weapon", "Deadmau5", durationSeconds = 480)
                7 -> Track("rds_3", "Alive", "Rufus Du Sol", durationSeconds = 310)
                else -> Track("edm_$i", "Electronic Track $i", "EDM Producer $i", durationSeconds = 300)
            }
        }

        val filtered = ContentFilter.filterTracks(candidates, ContentFilterMode.MUSIC_ONLY)
        val sections = engine.generateSections(profile, filtered, excludedTrackIds = emptySet())

        val quickPicks = sections.find { it.type == SectionType.QUICK_PICKS }
        assertNotNull(quickPicks)
        assertTrue(quickPicks!!.tracks.any { it.artist == "Deadmau5" || it.artist == "Rufus Du Sol" })

        verifyStrictCrossSectionDeduplication(sections)
    }

    // =========================================================================
    // PROFILE D: MIXED-LANGUAGE EXPLORER
    // =========================================================================

    @Test
    fun testProfileD_MixedLanguageExplorer() {
        val profile = ListeningProfile(
            topArtists = listOf(
                ArtistAffinity("A.R. Rahman", score = 25.0, playCount = 10, completionCount = 8),
                ArtistAffinity("Coldplay", score = 20.0, playCount = 8, completionCount = 7),
                ArtistAffinity("Diljit Dosanjh", score = 15.0, playCount = 6, completionCount = 5)
            ),
            topTracks = listOf(
                TrackAffinity("arr_1", "Kun Faya Kun", "A.R. Rahman", 15.0, 7, true),
                TrackAffinity("cp_1", "Viva La Vida", "Coldplay", 12.0, 5, true)
            ),
            likedTrackIds = setOf("arr_1", "cp_1"),
            recentTracks = listOf(
                Track("arr_1", "Kun Faya Kun", "A.R. Rahman", durationSeconds = 473),
                Track("cp_1", "Viva La Vida", "Coldplay", durationSeconds = 242)
            ),
            totalListens = 28
        )

        val candidates = (1..20).map { i ->
            when (i) {
                1 -> Track("arr_2", "Jai Ho", "A.R. Rahman", durationSeconds = 319)
                2 -> Track("cp_2", "Fix You", "Coldplay", durationSeconds = 295)
                3 -> Track("diljit_1", "Lover", "Diljit Dosanjh", durationSeconds = 191)
                4 -> Track("diljit_2", "Born to Shine", "Diljit Dosanjh", durationSeconds = 214)
                5 -> Track("arr_3", "Tere Bina", "A.R. Rahman", durationSeconds = 305)
                6 -> Track("cp_3", "Yellow", "Coldplay", durationSeconds = 269)
                else -> Track("world_$i", "Global Beat $i", "World Artist $i", durationSeconds = 240)
            }
        }

        val sections = engine.generateSections(profile, candidates, excludedTrackIds = emptySet())

        // Quick Picks should blend favorite artists
        val quickPicks = sections.find { it.type == SectionType.QUICK_PICKS }
        assertNotNull(quickPicks)
        assertTrue(quickPicks!!.tracks.size >= 2)

        verifyStrictCrossSectionDeduplication(sections)
    }

    // =========================================================================
    // PROFILE E: NEW USER (COLD START)
    // =========================================================================

    @Test
    fun testProfileE_NewUserColdStart() {
        val emptyProfile = ListeningProfile()
        assertTrue(emptyProfile.isColdStart)

        val trendingCandidates = (1..25).map { i ->
            Track(
                id = "trend_$i",
                title = "Trending Track $i",
                artist = if (i % 2 == 0) "Trending Artist A" else "Trending Artist B",
                durationSeconds = (180 + i).toLong()
            )
        }

        val sections = engine.generateSections(emptyProfile, trendingCandidates, excludedTrackIds = emptySet())

        assertTrue("Sections must not be empty on cold start", sections.isNotEmpty())
        val quickPicks = sections.find { it.type == SectionType.QUICK_PICKS }
        assertNotNull("Quick Picks must be populated on cold start", quickPicks)
        assertTrue(quickPicks!!.tracks.isNotEmpty())

        val madeForYou = sections.find { it.type == SectionType.MADE_FOR_YOU }
        assertNotNull("Made For You starter mixes must exist", madeForYou)
        assertTrue(madeForYou!!.mixes.isNotEmpty())

        verifyStrictCrossSectionDeduplication(sections)
    }

    // =========================================================================
    // HELPER: VERIFY STRICT CROSS-SECTION DEDUPLICATION
    // =========================================================================

    private fun verifyStrictCrossSectionDeduplication(sections: List<com.nova.music.domain.model.RecommendationSection>) {
        val seenTrackIds = mutableSetOf<String>()

        for (section in sections) {
            // Note: RECENTLY_PLAYED is by definition the user's playback history and can overlap
            // with what's in other sections, but QUICK_PICKS, MADE_FOR_YOU, DISCOVER_WEEKLY,
            // and BECAUSE_YOU_LISTENED must never overlap with each other.
            if (section.type == SectionType.RECENTLY_PLAYED) continue

            val sectionTrackIds = section.tracks.map { it.id } + section.mixes.flatMap { m -> m.tracks.map { it.id } }

            for (id in sectionTrackIds) {
                assertFalse(
                    "Track $id in section ${section.type} was already consumed in an earlier recommendation section!",
                    seenTrackIds.contains(id)
                )
                seenTrackIds.add(id)
            }
        }
    }
}
