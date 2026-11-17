package com.nova.music.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.LikedSongsRepository
import com.nova.music.playback.NovaPlayerController
import com.nova.music.playback.queue.QueueSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playerController: NovaPlayerController,
    private val likedSongsRepository: LikedSongsRepository? = null
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState
    val queueSnapshot: StateFlow<QueueSnapshot> = playerController.queueSnapshot

    val likedTrackIds: StateFlow<List<String>> = likedSongsRepository?.getAllLikedTrackIds()
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    fun toggleLike(track: Track) {
        likedSongsRepository?.let { repo ->
            viewModelScope.launch {
                repo.toggleLike(track)
            }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()

    fun next() = playerController.next()

    fun previous() = playerController.previous()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun toggleShuffle() = playerController.toggleShuffle()

    fun playNext(track: Track) = playerController.playNext(track)

    fun addToQueue(track: Track) = playerController.addToQueue(track)

    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)

    fun moveTrack(fromIndex: Int, toIndex: Int) = playerController.moveTrack(fromIndex, toIndex)

    fun jumpToQueueIndex(index: Int) = playerController.jumpToQueueIndex(index)

    fun clearUpcoming() = playerController.clearUpcoming()

    fun undoQueueChange(): Boolean = playerController.undoQueueChange()

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
