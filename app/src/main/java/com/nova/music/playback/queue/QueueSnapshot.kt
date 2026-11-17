package com.nova.music.playback.queue

import com.nova.music.domain.model.Track
import com.nova.music.playback.shuffle.ShuffleMode

data class QueueSnapshot(
    val currentTrack: Track? = null,
    val activeQueue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffle: Boolean = false,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val userQueueCount: Int = 0
) {
    val upcomingTracks: List<Track>
        get() = if (currentIndex in activeQueue.indices && currentIndex < activeQueue.size - 1) {
            activeQueue.subList(currentIndex + 1, activeQueue.size)
        } else {
            emptyList()
        }

    val previousTracks: List<Track>
        get() = if (currentIndex in activeQueue.indices && currentIndex > 0) {
            activeQueue.subList(0, currentIndex)
        } else {
            emptyList()
        }

    val hasNext: Boolean
        get() = currentIndex in activeQueue.indices && currentIndex < activeQueue.size - 1

    val hasPrevious: Boolean
        get() = currentIndex in activeQueue.indices && currentIndex > 0
}
