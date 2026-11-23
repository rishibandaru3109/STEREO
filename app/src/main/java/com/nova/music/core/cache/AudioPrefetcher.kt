package com.nova.music.core.cache

import androidx.media3.datasource.DataSource
import com.nova.music.core.network.NetworkState
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.playback.queue.QueueManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** One bounded next-track job. Queue, quality, settings and network changes cancel obsolete work. */
class AudioPrefetcher(
    private val cache: AudioCacheEngine,
    private val queue: QueueManager,
    private val repository: MusicRepository,
    private val network: StateFlow<NetworkState>,
    private val upstream: DataSource.Factory
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {
        scope.launch {
            combine(queue.snapshot, network, cache.prefetchEnabled, cache.enabled, repository.effectiveAudioQuality) {
                    snapshot, connection, prefetch, enabled, quality ->
                Triple(snapshot.upcomingTracks.firstOrNull()?.takeIf {
                    prefetch && enabled && connection.connected && connection.wifi && !connection.metered && it.streamUrl == null
                }, quality, snapshot.currentTrack?.id)
            }.distinctUntilChanged().collectLatest { (next, quality, _) ->
                if (next == null) return@collectLatest
                delay(5_000)
                try {
                    val stream = repository.resolveStream(next.id).getOrThrow()
                    if (!stream.isLocalCache) cache.prefetch(next.id, quality, stream, upstream) {
                        network.value.let { it.connected && it.wifi && !it.metered } && cache.enabled.value && cache.prefetchEnabled.value
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Optional prefetch never interrupts foreground playback. */ }
            }
        }
    }
}
