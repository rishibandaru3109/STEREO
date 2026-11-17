package com.nova.music.playback

import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.playback.queue.QueueSnapshot
import kotlinx.coroutines.flow.StateFlow

interface NovaPlayerController {
    val playbackState: StateFlow<PlaybackState>
    val queueSnapshot: StateFlow<QueueSnapshot>

    fun playTrack(track: Track)
    fun setQueue(tracks: List<Track>, startIndex: Int = 0)
    fun playNext(track: Track)
    fun addToQueue(track: Track)
    fun removeFromQueue(index: Int)
    fun moveTrack(fromIndex: Int, toIndex: Int)
    fun jumpToQueueIndex(index: Int)
    fun clearUpcoming()
    fun undoQueueChange(): Boolean
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun next()
    fun previous()
    fun setRepeatMode(repeatMode: RepeatMode)
    fun toggleShuffle()
    fun release()
}
