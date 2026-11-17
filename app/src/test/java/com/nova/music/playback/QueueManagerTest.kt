package com.nova.music.playback

import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.playback.queue.QueueManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QueueManagerTest {

    private lateinit var queueManager: QueueManager

    private val track1 = Track(id = "1", title = "Song 1", artist = "Artist 1")
    private val track2 = Track(id = "2", title = "Song 2", artist = "Artist 2")
    private val track3 = Track(id = "3", title = "Song 3", artist = "Artist 3")
    private val track4 = Track(id = "4", title = "Song 4", artist = "Artist 4")

    @Before
    fun setup() {
        queueManager = QueueManager()
    }

    @Test
    fun testSetQueueInitializesCorrectly() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)

        val snapshot = queueManager.snapshot.value
        assertEquals(track1, snapshot.currentTrack)
        assertEquals(0, snapshot.currentIndex)
        assertEquals(3, snapshot.activeQueue.size)
        assertEquals(listOf(track2, track3), snapshot.upcomingTracks)
        assertEquals(emptyList<Track>(), snapshot.previousTracks)
        assertTrue(snapshot.hasNext)
        assertFalse(snapshot.hasPrevious)
    }

    @Test
    fun testPlayNow() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)

        // Play now an unqueued track
        val result = queueManager.playNow(track3)
        assertEquals(track3, result)
        assertEquals(track3, queueManager.getCurrentTrack())
        assertEquals(3, queueManager.getActiveQueue().size)

        // Play now an already queued track
        queueManager.playNow(track1)
        assertEquals(track1, queueManager.getCurrentTrack())
        assertEquals(0, queueManager.getCurrentIndex())
    }

    @Test
    fun testPlayNextPriority() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)

        // Currently on track1, upcoming: [track2, track3]
        queueManager.playNext(track4)

        // Track 4 should be inserted immediately after track 1 at index 1
        val snapshot = queueManager.snapshot.value
        assertEquals(track1, snapshot.currentTrack)
        assertEquals(4, snapshot.activeQueue.size)
        assertEquals(track4, snapshot.activeQueue[1])
        assertEquals(listOf(track4, track2, track3), snapshot.upcomingTracks)
        assertEquals(1, snapshot.userQueueCount)
    }

    @Test
    fun testAddToQueueAppendsToEnd() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)

        queueManager.addToQueue(track3)

        val active = queueManager.getActiveQueue()
        assertEquals(3, active.size)
        assertEquals(track3, active[2])
    }

    @Test
    fun testRemoveFromQueue() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)

        val removed = queueManager.removeFromQueue(1) // Remove track2
        assertEquals(track2, removed)
        assertEquals(listOf(track1, track3), queueManager.getActiveQueue())
        assertEquals(0, queueManager.getCurrentIndex())
        assertEquals(listOf(track3), queueManager.snapshot.value.upcomingTracks)
    }

    @Test
    fun testMoveTrackReordering() {
        queueManager.setQueue(listOf(track1, track2, track3), startIndex = 0)

        // Move track 1 (index 0) to index 2 -> Order becomes [track2, track3, track1]
        queueManager.moveTrack(fromIndex = 0, toIndex = 2)

        val active = queueManager.getActiveQueue()
        assertEquals(listOf(track2, track3, track1), active)
        // Current index should track track1 to index 2
        assertEquals(2, queueManager.getCurrentIndex())
        assertEquals(track1, queueManager.getCurrentTrack())
    }

    @Test
    fun testNextAndPreviousWithRepeatModes() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)

        // RepeatMode.OFF: advances to track2, then null at end
        val next1 = queueManager.next(RepeatMode.OFF)
        assertEquals(track2, next1)
        val next2 = queueManager.next(RepeatMode.OFF)
        assertNull(next2)

        // RepeatMode.ALL: loops back to track1
        val loopNext = queueManager.next(RepeatMode.ALL)
        assertEquals(track1, loopNext)

        // RepeatMode.ONE: stays on track1
        val repeatOneNext = queueManager.next(RepeatMode.ONE)
        assertEquals(track1, repeatOneNext)

        // Previous
        val prev = queueManager.previous(RepeatMode.OFF)
        assertEquals(track1, prev) // Already at 0, stays
    }

    @Test
    fun testToggleShufflePreservesOriginalQueue() {
        val tracks = listOf(track1, track2, track3, track4)
        queueManager.setQueue(tracks, startIndex = 1) // track2 active
        assertEquals(track2, queueManager.getCurrentTrack())

        // Enable shuffle
        queueManager.toggleShuffle(enable = true)
        assertTrue(queueManager.isShuffleEnabled())
        assertEquals(track2, queueManager.getCurrentTrack()) // Preserves current track
        assertEquals(4, queueManager.getActiveQueue().size)

        // Disable shuffle -> restores exact original order
        queueManager.toggleShuffle(enable = false)
        assertFalse(queueManager.isShuffleEnabled())
        assertEquals(tracks, queueManager.getActiveQueue())
        assertEquals(track2, queueManager.getCurrentTrack())
        assertEquals(1, queueManager.getCurrentIndex())
    }

    @Test
    fun testUndoQueueChange() {
        queueManager.setQueue(listOf(track1, track2), startIndex = 0)
        queueManager.addToQueue(track3)
        assertEquals(3, queueManager.getActiveQueue().size)

        val undone = queueManager.undo()
        assertTrue(undone)
        assertEquals(2, queueManager.getActiveQueue().size)
        assertEquals(listOf(track1, track2), queueManager.getActiveQueue())
    }
}
