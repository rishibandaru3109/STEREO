package com.nova.music.playback

import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface NovaPlayerController {
    val playbackState: StateFlow<PlaybackState>

    fun playTrack(track: Track)
    fun setQueue(tracks: List<Track>, startIndex: Int = 0)
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
