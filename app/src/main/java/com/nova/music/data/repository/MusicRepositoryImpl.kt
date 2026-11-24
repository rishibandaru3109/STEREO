package com.nova.music.data.repository

import com.nova.music.core.cache.AudioCacheEngine
import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.AudioQualityMode
import com.nova.music.domain.model.withRepresentationMetadata
import com.nova.music.core.network.NetworkState
import com.nova.music.core.network.TrafficMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URI
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.ContentFilter
import com.nova.music.domain.repository.ContentFilterMode
import com.nova.music.domain.repository.DownloadRepository
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val extractor: MusicExtractor,
    private val preferencesRepository: MusicPreferencesRepository? = null,
    private val networkState: Flow<NetworkState> = flowOf(NetworkState()),
    private val audioCache: AudioCacheEngine? = null,
    private val downloadRepository: DownloadRepository? = null
) : MusicRepository {
    override val networkConnected: Flow<Boolean> = networkState.map { it.connected }.distinctUntilChanged()

    override val effectiveAudioQuality: Flow<AudioQualityMode> = combine(
        preferencesRepository?.audioQualityMode ?: flowOf(AudioQualityMode.AUTO),
        networkState
    ) { mode, network -> mode.effective(network.metered) }.distinctUntilChanged()

    // Short-lived URL metadata is distinct from provider-permitted audio bytes.
    private val resolutionLocks = List(16) { Mutex() }
    private val resolved = object : LinkedHashMap<Pair<String, AudioQualityMode>, Pair<Long, AudioStream>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<String, AudioQualityMode>, Pair<Long, AudioStream>>): Boolean =
            size > 32
    }


    override fun invalidateStream(trackId: String) {
        audioCache?.markInvalid(trackId)
        synchronized(resolved) { resolved.keys.removeAll { it.first == trackId } }
    }

    override fun search(query: String): Flow<Result<List<Track>>> = flow {
        val result = extractor.search(query)
        if (result.isSuccess) {
            val tracks = result.getOrNull() ?: emptyList()
            val mode = preferencesRepository?.contentFilterMode?.first() ?: ContentFilterMode.MUSIC_ONLY
            val filtered = ContentFilter.filterTracks(tracks, mode)
            emit(Result.success(filtered))
        } else {
            emit(result)
        }
    }

    override suspend fun resolveStream(trackId: String): Result<AudioStream> {
        val quality = effectiveAudioQuality.first()
        val connected = networkState.first().connected

        // 1. Check persistent offline downloads first
        val downloaded = downloadRepository?.getCompletedDownloadStream(trackId)
        if (downloaded != null) {
            TrafficMeter.counters.metadataCacheHit()
            return Result.success(downloaded)
        }

        // 2. Check automatic LRU cache
        audioCache?.fullyCached(trackId, quality, allowAnyQuality = !connected)?.let {
            TrafficMeter.counters.metadataCacheHit()
            return Result.success(it)
        }
        if (!connected && (audioCache != null || downloadRepository != null)) {
            return Result.failure(java.io.IOException("Offline: this song is not downloaded or cached"))
        }
        val key = trackId to quality
        val lock = resolutionLocks[(key.hashCode() and Int.MAX_VALUE) % resolutionLocks.size]
        return withContext(Dispatchers.IO) {
            lock.withLock {
                val now = System.currentTimeMillis()
                val cached = synchronized(resolved) { resolved[key] }
                if (cached != null && cached.first > now) {
                    TrafficMeter.counters.metadataCacheHit()
                    return@withLock Result.success(cached.second)
                }
                val result = extractor.resolveAudioStream(trackId, quality).map { it.withRepresentationMetadata() }
                result.getOrNull()?.let { stream ->
                    val expiry = runCatching {
                        URI(stream.url).rawQuery?.split('&')?.firstOrNull { it.startsWith("expire=") }
                            ?.substringAfter('=')?.toLongOrNull()?.times(1000L)?.minus(60_000L)
                    }.getOrNull() ?: (now + 120_000L)
                    synchronized(resolved) { resolved[key] = minOf(expiry, now + 120_000L) to stream }
                }
                result
            }
        }
    }

    override suspend fun getTrack(trackId: String): Result<Track> {
        return extractor.getTrackDetails(trackId)
    }
}
