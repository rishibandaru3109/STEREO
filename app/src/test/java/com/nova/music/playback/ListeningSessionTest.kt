package com.nova.music.playback

import com.nova.music.domain.model.Track
import org.junit.Assert.*
import org.junit.Test

class ListeningSessionTest {
    private val track = Track("track", "Song", "Artist", durationSeconds = 180)

    @Test
    fun listeningTimeExcludesPausedIntervals() {
        val session = ListeningSession(track)
        session.updatePlaying(true, 1000)
        session.updatePlaying(false, 6000)
        session.updatePlaying(true, 30000)
        val sample = session.finish(55000, 180000, manuallySkipped = false)!!

        assertEquals(30000L, sample.listenedMs)
        assertFalse(sample.isSkipped)
    }

    @Test
    fun earlyManualSkipStillProducesHistorySample() {
        val session = ListeningSession(track)
        session.updatePlaying(true, 1000)
        val sample = session.finish(6500, 0, manuallySkipped = true)!!

        assertEquals(5500L, sample.listenedMs)
        assertEquals(180000L, sample.durationMs)
        assertTrue(sample.isSkipped)
        assertNull(session.finish(2500, 0, manuallySkipped = true))
    }
}
