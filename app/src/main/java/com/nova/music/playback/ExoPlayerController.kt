package com.nova.music.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.PlayerStatus
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExoPlayerController(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val exoPlayer: ExoPlayer
) : NovaPlayerController {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playbackState.update { it.copy(status = PlayerStatus.BUFFERING) }
                }
                Player.STATE_READY -> {
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    _playbackState.update {
                        it.copy(
                            status = if (exoPlayer.isPlaying) PlayerStatus.PLAYING else PlayerStatus.PAUSED,
                            durationMs = duration
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    _playbackState.update { it.copy(status = PlayerStatus.ENDED, isPlaying = false) }
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _playbackState.update { it.copy(status = PlayerStatus.IDLE) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update {
                it.copy(
                    isPlaying = isPlaying,
                    status = if (isPlaying) PlayerStatus.PLAYING else if (it.status == PlayerStatus.PLAYING) PlayerStatus.PAUSED else it.status
                )
            }
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    status = PlayerStatus.ERROR,
                    isPlaying = false,
                    errorMessage = error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
                )
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    private fun ensureServiceStarted() {
        try {
            val intent = Intent(context, NovaMediaSessionService::class.java)
            Util.startForegroundService(context, intent)
        } catch (_: Exception) {
            // Foreground service launch handled gracefully
        }
    }

    override fun playTrack(track: Track) {
        ensureServiceStarted()
        val currentQueue = _playbackState.value.queue
        val existingIndex = currentQueue.indexOfFirst { it.id == track.id }
        val newQueue = if (existingIndex >= 0) currentQueue else currentQueue + track
        val newIndex = if (existingIndex >= 0) existingIndex else newQueue.size - 1

        _playbackState.update {
            it.copy(
                currentTrack = track,
                queue = newQueue,
                queueIndex = newIndex,
                status = PlayerStatus.BUFFERING,
                errorMessage = null,
                currentPositionMs = 0L
            )
        }

        resolveAndPlay(track)
    }

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        ensureServiceStarted()
        val validIndex = startIndex.coerceIn(0, tracks.size - 1)
        val track = tracks[validIndex]

        _playbackState.update {
            it.copy(
                queue = tracks,
                queueIndex = validIndex,
                currentTrack = track,
                status = PlayerStatus.BUFFERING,
                errorMessage = null,
                currentPositionMs = 0L
            )
        }

        resolveAndPlay(track)
    }

    private fun resolveAndPlay(track: Track) {
        scope.launch {
            if (track.streamUrl != null) {
                playMediaUri(track, track.streamUrl)
            } else {
                _playbackState.update { it.copy(status = PlayerStatus.BUFFERING) }
                val streamResult = musicRepository.resolveStream(track.id)
                streamResult.onSuccess { stream ->
                    playMediaUri(track, stream.url)
                }.onFailure { error ->
                    _playbackState.update {
                        it.copy(
                            status = PlayerStatus.ERROR,
                            errorMessage = "Stream resolution failed: ${error.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    private fun playMediaUri(track: Track, uriString: String) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.thumbnailUrl.isNotBlank()) Uri.parse(track.thumbnailUrl) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uriString)
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun play() {
        if (exoPlayer.playbackState == Player.STATE_IDLE && _playbackState.value.currentTrack != null) {
            _playbackState.value.currentTrack?.let { resolveAndPlay(it) }
        } else {
            exoPlayer.play()
        }
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(safePosition)
        _playbackState.update { it.copy(currentPositionMs = safePosition) }
    }

    override fun next() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        val nextIndex = (state.queueIndex + 1) % state.queue.size
        val nextTrack = state.queue[nextIndex]
        _playbackState.update { it.copy(queueIndex = nextIndex, currentTrack = nextTrack) }
        resolveAndPlay(nextTrack)
    }

    override fun previous() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        // If current position > 3 seconds, replay current song
        if (exoPlayer.currentPosition > 3000) {
            seekTo(0)
            return
        }
        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else state.queue.size - 1
        val prevTrack = state.queue[prevIndex]
        _playbackState.update { it.copy(queueIndex = prevIndex, currentTrack = prevTrack) }
        resolveAndPlay(prevTrack)
    }

    override fun setRepeatMode(repeatMode: RepeatMode) {
        _playbackState.update { it.copy(repeatMode = repeatMode) }
        exoPlayer.repeatMode = when (repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    override fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffle
        _playbackState.update { it.copy(isShuffle = newShuffle) }
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                play()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    next()
                }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _playbackState.update {
                    it.copy(
                        currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
    }

    override fun release() {
        stopProgressUpdates()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
