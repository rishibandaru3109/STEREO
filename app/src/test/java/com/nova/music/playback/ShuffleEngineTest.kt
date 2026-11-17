package com.nova.music.playback

import com.nova.music.domain.model.Track
import com.nova.music.playback.shuffle.BalancedShuffle
import com.nova.music.playback.shuffle.StandardShuffle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleEngineTest {

    private val trackA1 = Track(id = "a1", title = "Song A1", artist = "Artist A")
    private val trackA2 = Track(id = "a2", title = "Song A2", artist = "Artist A")
    private val trackA3 = Track(id = "a3", title = "Song A3", artist = "Artist A")
    private val trackB1 = Track(id = "b1", title = "Song B1", artist = "Artist B")
    private val trackB2 = Track(id = "b2", title = "Song B2", artist = "Artist B")
    private val trackC1 = Track(id = "c1", title = "Song C1", artist = "Artist C")
    private val trackC2 = Track(id = "c2", title = "Song C2", artist = "Artist C")

    @Test
    fun testStandardShufflePreservesCurrentTrackAtHead() {
        val tracks = listOf(trackA1, trackA2, trackB1, trackC1)
        val engine = StandardShuffle()

        val shuffled = engine.shuffle(tracks, currentTrack = trackB1, seed = 42L)

        assertEquals("Current track must be at index 0", trackB1, shuffled[0])
        assertEquals("Shuffled list must contain all original tracks", tracks.size, shuffled.size)
        assertTrue(shuffled.containsAll(tracks))
    }

    @Test
    fun testBalancedShufflePreventsArtistClustering() {
        // Pool with 3 Artist A, 2 Artist B, 2 Artist C
        val tracks = listOf(trackA1, trackA2, trackA3, trackB1, trackB2, trackC1, trackC2)
        val engine = BalancedShuffle()

        val shuffled = engine.shuffle(tracks, currentTrack = trackA1, seed = 12345L)

        assertEquals("Current track should be at index 0", trackA1, shuffled[0])
        assertEquals("All tracks must be present", tracks.size, shuffled.size)
        assertTrue(shuffled.containsAll(tracks))

        // Check consecutive artist repetitions
        var consecutiveCount = 0
        for (i in 0 until shuffled.size - 1) {
            if (shuffled[i].artist.equals(shuffled[i + 1].artist, ignoreCase = true)) {
                consecutiveCount++
            }
        }

        // With 3 A, 2 B, 2 C (total 7), perfect alternation is mathematically possible (0 consecutive)
        assertEquals("Balanced shuffle should eliminate consecutive tracks by same artist when possible", 0, consecutiveCount)
    }

    @Test
    fun testBalancedShuffleSingleArtistGracefulFallback() {
        val tracks = listOf(trackA1, trackA2, trackA3)
        val engine = BalancedShuffle()

        val shuffled = engine.shuffle(tracks, currentTrack = trackA1)

        assertEquals(trackA1, shuffled[0])
        assertEquals(3, shuffled.size)
        assertTrue(shuffled.containsAll(tracks))
    }
}
