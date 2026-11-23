package com.nova.music.playback

import android.content.Context
import com.nova.music.core.cache.AudioCacheEngine
import com.nova.music.core.database.dao.SynapseDao
import com.nova.music.core.database.entity.SynapseEventEntity
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.nova.music.domain.model.AudioQualityMode
import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.PlayerStatus
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.domain.repository.HistoryRepository
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.playback.queue.QueueManager
import com.nova.music.playback.queue.QueueSnapshot
import com.synapse.recommendation.engine.SynapseRecommendationEngine
import com.synapse.recommendation.event.ListeningEvent
import com.synapse.recommendation.model.SynapseTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

class ExoPlayerController(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val exoPlayer: ExoPlayer,
    private val queueManager: QueueManager = QueueManager(),
    private val historyRepository: HistoryRepository? = null,
    private val preferencesRepository: MusicPreferencesRepository? = null,
    private val audioCache: AudioCacheEngine? = null,
    private val synapseEngine: SynapseRecommendationEngine? = null,
    private val synapseDao: SynapseDao? = null
) : NovaPlayerController {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var resolveJob: Job? = null
    private var qualityObservationJob: Job? = null
    private var connectivityObservationJob: Job? = null
    private var playbackRequest = 0L
    private var playWhenResolved = true
    private var qualityChangePending = false
    private var resolutionResumePositionMs = 0L
    private var refreshedUrlTrack: String? = null
    private var listeningSession: ListeningSession? = null

    private fun finishListening(manuallySkipped: Boolean = false) {
        val session = listeningSession ?: return
        listeningSession = null
        val sample = session.finish(SystemClock.elapsedRealtime(), exoPlayer.duration, manuallySkipped) ?: return
        scope.launch {
            try {
                if (preferencesRepository?.isHistoryEnabled?.first() != false) {
                    historyRepository?.recordPlayback(
                        sample.track, sample.listenedMs, sample.durationMs, sample.isSkipped, "player"
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("NOVA_DIAGNOSTICS", "Could not save listening history", error)
            }

            // Autonomous SYNAPSE Event Intelligence Ingestion
            try {
                if (preferencesRepository?.isSynapseEnabled?.first() != false && synapseEngine != null) {
                    val synapseTrack = SynapseTrack(
                        id = sample.track.id,
                        title = sample.track.title,
                        artist = sample.track.artist,
                        album = sample.track.album,
                        durationSeconds = sample.track.durationSeconds,
                        thumbnailUrl = sample.track.thumbnailUrl,
                        language = sample.track.lyricsLanguage,
                        isSpokenWord = sample.track.isSpokenWord
                    )
                    val event = ListeningEvent(
                        track = synapseTrack,
                        listeningDurationMs = sample.listenedMs,
                        totalTrackDurationMs = sample.durationMs,
                        completionRatio = if (sample.durationMs > 0) (sample.listenedMs.toFloat() / sample.durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        isManualSkip = sample.isSkipped,
                        playbackContext = "player"
                    )
                    synapseEngine.getTasteIntelligence().processEvent(event)
                    synapseDao?.insertEvent(
                        SynapseEventEntity(
                            eventId = event.eventId,
                            trackId = sample.track.id,
                            timestamp = event.timestamp,
                            listeningDurationMs = sample.listenedMs,
                            totalTrackDurationMs = sample.durationMs,
                            completionRatio = event.completionRatio,
                            isManualSkip = sample.isSkipped,
                            playbackContext = "player"
                        )
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("SYNAPSE_EVENTS", "Could not record SYNAPSE listening event", error)
            }
        }
    }

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
                    finishListening()
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _playbackState.update { if (it.status == PlayerStatus.ERROR) it else it.copy(status = PlayerStatus.IDLE) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listeningSession?.updatePlaying(isPlaying, SystemClock.elapsedRealtime())
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
            queueManager.getCurrentTrack()?.let { musicRepository.invalidateStream(it.id) }
            val response = generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
            val current = queueManager.getCurrentTrack()
            if (response?.responseCode in listOf(401, 403, 410) && current != null &&
                current.streamUrl == null && refreshedUrlTrack != current.id) {
                refreshedUrlTrack = current.id
                val position = exoPlayer.currentPosition.coerceAtLeast(0)
                val requested = playWhenResolved
                scope.launch {
                    resolveAndPlay(current, resumePositionMs = position)
                    playWhenResolved = requested
                }
                return
            }
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
        qualityObservationJob = scope.launch {
            var previous: AudioQualityMode? = null
            musicRepository.effectiveAudioQuality.collect { quality ->
                val changed = previous != null && previous != quality
                previous = quality
                val track = queueManager.getCurrentTrack()
                if (changed && track != null && track.streamUrl == null) {
                    qualityChangePending = true
                    if (exoPlayer.isPlaying || (resolveJob?.isActive == true && playWhenResolved)) {
                        val position = if (resolveJob?.isActive == true) resolutionResumePositionMs
                            else exoPlayer.currentPosition.coerceAtLeast(0)
                        resolveAndPlay(track, resumePositionMs = position)
                    }
                }
            }
        }
        connectivityObservationJob = scope.launch {
            var disconnected = false
            musicRepository.networkConnected.collect { connected ->
                val reconnected = disconnected && connected
                disconnected = !connected
                val track = queueManager.getCurrentTrack()
                if (reconnected && playWhenResolved && track != null &&
                    _playbackState.value.status == PlayerStatus.ERROR
                ) {
                    musicRepository.invalidateStream(track.id)
                    val position = if (exoPlayer.currentMediaItem?.mediaId == track.id) {
                        _playbackState.value.currentPositionMs
                    } else resolutionResumePositionMs
                    resolveAndPlay(track, resumePositionMs = position)
                }
            }
        }
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
            context.startService(intent)
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
        queueManager.setQueue(tracks, startIndex, isShuffle = queueManager.snapshot.value.isShuffle)
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

    private fun resolveAndPlay(track: Track, manuallySkipped: Boolean = false, resumePositionMs: Long? = null) {
        if (resumePositionMs == null) finishListening(manuallySkipped)
        resolveJob?.cancel()
        val request = ++playbackRequest
        playWhenResolved = true
        qualityChangePending = false
        resolutionResumePositionMs = resumePositionMs ?: 0L
        if (resumePositionMs == null) refreshedUrlTrack = null
        exoPlayer.pause()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        audioCache?.setPlaybackKey(null)
        _playbackState.update {
            it.copy(status = PlayerStatus.BUFFERING, isPlaying = false, errorMessage = null,
                currentPositionMs = resolutionResumePositionMs, durationMs = 0L, bufferedPositionMs = 0L)
        }

        resolveJob = scope.launch {
            try {
                val stream = if (track.streamUrl == null) musicRepository.resolveStream(track.id).getOrThrow() else null
                val key = stream?.let {
                    audioCache?.prepare(track.id, musicRepository.effectiveAudioQuality.first(), it)
                }
                val url = track.streamUrl ?: requireNotNull(stream).url
                if (request == playbackRequest) {
                    audioCache?.setPlaybackKey(key)
                    playMediaUri(track, url, resumePositionMs, key)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == playbackRequest) {
                    Log.e("NOVA_DIAGNOSTICS", "Stream resolution failed", error)
                    _playbackState.update {
                        it.copy(status = PlayerStatus.ERROR,
                            errorMessage = error.localizedMessage ?: "Could not load this track")
                    }
                }
            }
        }
    }

    private fun playMediaUri(track: Track, uriString: String, resumePositionMs: Long? = null, cacheKey: String? = null) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.thumbnailUrl.isNotBlank()) Uri.parse(track.thumbnailUrl) else null)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uriString)
            .setCustomCacheKey(cacheKey)
            .setMediaMetadata(mediaMetadata)
            .build()

        if (resumePositionMs == null) listeningSession = ListeningSession(track)
        exoPlayer.setMediaItem(mediaItem, resumePositionMs ?: 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = playWhenResolved
    }

    override fun play() {
        playWhenResolved = true
        val current = queueManager.getCurrentTrack()
        if (qualityChangePending && current != null && current.streamUrl == null) {
            val position = if (resolveJob?.isActive == true) resolutionResumePositionMs
                else exoPlayer.currentPosition.coerceAtLeast(0)
            resolveAndPlay(current, resumePositionMs = position)
            return
        }
        if (resolveJob?.isActive == true) return
        if (current != null && (exoPlayer.playbackState == Player.STATE_IDLE ||
                exoPlayer.currentMediaItem?.mediaId != current.id)
        ) {
            ensureServiceStarted()
            resolveAndPlay(current)
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
                listeningSession = current?.let(::ListeningSession)
            }
            exoPlayer.play()
        }
    }

    override fun pause() {
        playWhenResolved = false
        exoPlayer.pause()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying || (resolveJob?.isActive == true && playWhenResolved)) pause() else play()
    }

    override fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        exoPlayer.seekTo(safePosition)
        _playbackState.update { it.copy(currentPositionMs = safePosition) }
    }

    override fun next() {
        val nextTrack = queueManager.next(_playbackState.value.repeatMode)
        if (nextTrack != null) {
            resolveAndPlay(nextTrack, manuallySkipped = true)
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
            RepeatMode.ALL -> Player.REPEAT_MODE_OFF
        }
    }

    override fun toggleShuffle() {
        val newShuffle = !queueManager.snapshot.value.isShuffle
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
        finishListening()
        qualityObservationJob?.cancel()
        connectivityObservationJob?.cancel()
        resolveJob?.cancel()
        stopProgressUpdates()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        audioCache?.setPlaybackKey(null)
    }
}
