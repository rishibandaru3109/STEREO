package com.nova.music.feature.player

import androidx.lifecycle.ViewModel
import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.RepeatMode
import com.nova.music.playback.NovaPlayerController
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(
    private val playerController: NovaPlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    fun togglePlayPause() = playerController.togglePlayPause()

    fun next() = playerController.next()

    fun previous() = playerController.previous()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun toggleShuffle() = playerController.toggleShuffle()

    fun cycleRepeatMode() {
        val current = playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerController.setRepeatMode(next)
    }
}
