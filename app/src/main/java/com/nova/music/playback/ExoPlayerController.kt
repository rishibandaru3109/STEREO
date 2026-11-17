package com.nova.music.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.PlayerStatus
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.playback.queue.QueueManager
import com.nova.music.playback.queue.QueueSnapshot
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
import java.io.IOException

class ExoPlayerController(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val exoPlayer: ExoPlayer,
    private val queueManager: QueueManager = QueueManager()
) : NovaPlayerController {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    override val queueSnapshot: StateFlow<QueueSnapshot> = queueManager.snapshot

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
            val rootCause = error.cause
            Log.e("NOVA_DIAGNOSTICS", "ExoPlayer onPlayerError: errorCode=${error.errorCodeName}(${error.errorCode}), message=${error.message}, cause=${rootCause?.javaClass?.name}: ${rootCause?.message}")
            if (rootCause is HttpDataSource.InvalidResponseCodeException) {
                Log.e("NOVA_DIAGNOSTICS", "HTTP Error Code: ${rootCause.responseCode}, responseMessage=${rootCause.responseMessage}")
                val safeHeaders = rootCause.headerFields.filterKeys {
                    it.contains("content", ignoreCase = true) || it.contains("x-", ignoreCase = true) || it.equals("server", ignoreCase = true)
                }
                Log.e("NOVA_DIAGNOSTICS", "Response headers: $safeHeaders")
            }
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
        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onLoadStarted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData
            ) {
                val host = loadEventInfo.uri.host ?: "unknown"
                Log.i("NOVA_DIAGNOSTICS", "ExoPlayer load started: host=$host, dataType=${mediaLoadData.dataType}")
            }

            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData
            ) {
                val host = loadEventInfo.uri.host ?: "unknown"
                Log.i("NOVA_DIAGNOSTICS", "ExoPlayer load completed: host=$host, bytesLoaded=${loadEventInfo.bytesLoaded}, durationMs=${loadEventInfo.loadDurationMs}")
            }

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
                error: IOException,
                wasCanceled: Boolean
            ) {
                val host = loadEventInfo.uri.host ?: "unknown"
                val responseHeaders = loadEventInfo.responseHeaders
                val server = responseHeaders["Server"]?.firstOrNull() ?: "unknown"
                val xGvs = responseHeaders["x-goog-generation"]?.firstOrNull()
                Log.e("NOVA_DIAGNOSTICS", "ExoPlayer load error: host=$host, error=${error.javaClass.simpleName}: ${error.message}, server=$server, xGvs=$xGvs, wasCanceled=$wasCanceled")
            }
        })

        // Synchronize QueueManager snapshot with PlaybackState
        scope.launch {
            queueManager.snapshot.collect { snapshot ->
                _playbackState.update { current ->
                    current.copy(
                        currentTrack = snapshot.currentTrack,
                        queue = snapshot.activeQueue,
                        queueIndex = snapshot.currentIndex,
                        isShuffle = snapshot.isShuffle
                    )
                }
            }
        }
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
        _playbackState.update {
            it.copy(
                status = PlayerStatus.BUFFERING,
                errorMessage = null,
                currentPositionMs = 0L
            )
        }
        val target = queueManager.playNow(track)
        resolveAndPlay(target)
    }

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        ensureServiceStarted()
        _playbackState.update {
            it.copy(
                status = PlayerStatus.BUFFERING,
                errorMessage = null,
                currentPositionMs = 0L
            )
        }
        queueManager.setQueue(tracks, startIndex, isShuffle = _playbackState.value.isShuffle)
        queueManager.getCurrentTrack()?.let { resolveAndPlay(it) }
    }

    override fun playNext(track: Track) {
        queueManager.playNext(track)
    }

    override fun addToQueue(track: Track) {
        queueManager.addToQueue(track)
    }

    override fun removeFromQueue(index: Int) {
        queueManager.removeFromQueue(index)
    }

    override fun moveTrack(fromIndex: Int, toIndex: Int) {
        queueManager.moveTrack(fromIndex, toIndex)
    }

    override fun jumpToQueueIndex(index: Int) {
        val track = queueManager.jumpToIndex(index)
        if (track != null) {
            ensureServiceStarted()
            resolveAndPlay(track)
        }
    }

    override fun clearUpcoming() {
        queueManager.clearUpcoming()
    }

    override fun undoQueueChange(): Boolean {
        return queueManager.undo()
    }

    private fun resolveAndPlay(track: Track) {
        val requestStartTime = System.currentTimeMillis()
        scope.launch {
            if (track.streamUrl != null) {
                Log.i("NOVA_DIAGNOSTICS", "Playing cached stream URL for track: ${track.id}")
                playMediaUri(track, track.streamUrl)
            } else {
                _playbackState.update { it.copy(status = PlayerStatus.BUFFERING) }
                val streamResult = musicRepository.resolveStream(track.id)
                val extractionDuration = System.currentTimeMillis() - requestStartTime
                streamResult.onSuccess { stream ->
                    val host = runCatching { Uri.parse(stream.url).host }.getOrNull() ?: "unknown"
                    Log.i("NOVA_DIAGNOSTICS", "Stream resolved in ${extractionDuration}ms: host=$host, codec=${stream.codec}, bitrate=${stream.bitrate}")
                    val playStart = System.currentTimeMillis()
                    playMediaUri(track, stream.url)
                    val timeBetweenExtractionAndPlay = System.currentTimeMillis() - playStart
                    Log.i("NOVA_DIAGNOSTICS", "Playback request dispatched to ExoPlayer in ${timeBetweenExtractionAndPlay}ms")
                }.onFailure { error ->
                    Log.e("NOVA_DIAGNOSTICS", "Stream resolution failed in ${extractionDuration}ms: ${error.message}")
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
        val nextTrack = queueManager.next(_playbackState.value.repeatMode)
        if (nextTrack != null) {
            resolveAndPlay(nextTrack)
        }
    }

    override fun previous() {
        if (exoPlayer.currentPosition > 3000) {
            seekTo(0)
            return
        }
        val prevTrack = queueManager.previous(_playbackState.value.repeatMode)
        if (prevTrack != null) {
            resolveAndPlay(prevTrack)
        }
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
        queueManager.toggleShuffle(newShuffle)
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                play()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                val nextTrack = queueManager.next(state.repeatMode)
                if (nextTrack != null) {
                    resolveAndPlay(nextTrack)
                } else {
                    _playbackState.update { it.copy(isPlaying = false, status = PlayerStatus.ENDED) }
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
