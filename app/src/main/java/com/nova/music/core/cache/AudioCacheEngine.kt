package com.nova.music.core.cache

import android.content.Context
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.*
import com.nova.music.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

data class AudioCacheStats(
    val usedBytes: Long? = null,
    val maxBytes: Long = AudioCacheLimits.bytes(AudioCacheLimits.DEFAULT_MB),
    val error: String? = null,
    val completeSongs: Int = 0,
    val partialSongs: Int = 0,
    val hits: Long = 0,
    val misses: Long = 0
)

/** Persistent range store. Complete audio is atomically indexed in place, without copying spans. */
class AudioCacheEngine(
    context: Context,
    private val directory: File = File(context.noBackupFilesDir, "automatic_audio_v2"),
    testLimitBytes: Long? = null,
    private val fragmentBytes: Long = 512 * 1024L,
    private val personalBuild: Boolean = false,
    preferencesName: String = "stereo_audio_cache"
) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val database = StandaloneDatabaseProvider(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableStats = MutableStateFlow(AudioCacheStats())
    val stats = mutableStats.asStateFlow()
    private val mutableEnabled = MutableStateFlow(preferences.getBoolean("enabled", true))
    val enabled = mutableEnabled.asStateFlow()
    private val mutablePrefetch = MutableStateFlow(preferences.getBoolean("wifi_prefetch", false))
    val prefetchEnabled = mutablePrefetch.asStateFlow()
    private val mutableLimit = MutableStateFlow(AudioCacheLimits.validated(preferences.getInt("limit_mb", 500)))
    val limitMb = mutableLimit.asStateFlow()
    private val readers = ConcurrentHashMap<String, Int>()
    private val retired = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var playbackKey: String? = null
    @Volatile private var generation = 0L
    private val runtimeHeaders = object : LinkedHashMap<String, Map<String, String>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, String>>) = size > 32
    }
    private val evictor = ConfigurableAudioEvictor(
        testLimitBytes ?: AudioCacheLimits.bytes(mutableLimit.value),
        isPinned = { isProtected(it) },
        availableBytes = { directory.usableSpace.takeIf { it > 0 } ?: directory.parentFile!!.usableSpace }
    ) { bytes -> mutableStats.value = mutableStats.value.copy(usedBytes = bytes) }
    @Volatile private var cache: SimpleCache? = null
    @Volatile private var closed = false
    private fun isProtected(key: String) = playbackKey == key || (readers[key] ?: 0) > 0

    @Synchronized private fun store(): SimpleCache {
        check(!closed) { "Audio cache is closed" }
        cache?.let { return it }
        check(directory.mkdirs() || directory.isDirectory) { "Cannot create audio cache directory" }
        if (SimpleCache.isCacheFolderLocked(directory)) throw IOException("Audio cache is already open")
        val created = SimpleCache(directory, evictor, database)
        try { created.checkInitialization() } catch (error: IOException) {
            runCatching { created.release() }; throw error
        }
        cache = created
        for (key in created.keys) if (created.getContentMetadata(key).get(RETIRED, 0L) != 0L) retired.add(key)
        synchronized(created) { reclaim(created) }
        return created
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        try {
            val store = store()
            synchronized(store) {
                val complete = mutableSetOf<String>()
                val partial = mutableSetOf<String>()
                for (key in store.keys) {
                    if (key in retired || store.getCachedSpans(key).isEmpty()) continue
                    val json = descriptor(store, key) ?: continue
                    val hash = store.getContentMetadata(key).get(HASH, "").orEmpty()
                    if (hash.isNotBlank() && validFiles(store, key, json.optLong("length")) &&
                        AudioFileRanges.digest(pieces(store, key), json.optLong("length")) == hash)
                        complete.add(json.optString("track")) else partial.add(json.optString("track"))
                }
                mutableStats.value = mutableStats.value.copy(usedBytes = store.cacheSpace,
                    maxBytes = evictor.limitBytes, completeSongs = complete.size,
                    partialSongs = (partial - complete).size, error = null)
            }
        } catch (error: Exception) {
            mutableStats.value = mutableStats.value.copy(error = error.message ?: "Cache unavailable")
        }
    }

    suspend fun setEnabled(value: Boolean) = withContext(Dispatchers.IO) {
        check(preferences.edit().putBoolean("enabled", value).commit())
        mutableEnabled.value = value
        if (!value) clear()
        refresh()
    }
    suspend fun setPrefetch(value: Boolean) = withContext(Dispatchers.IO) {
        check(preferences.edit().putBoolean("wifi_prefetch", value).commit())
        mutablePrefetch.value = value
    }
    suspend fun setLimitMb(mb: Int) = withContext(Dispatchers.IO) {
        require(mb in AudioCacheLimits.optionsMb)
        check(preferences.edit().putInt("limit_mb", mb).commit())
        mutableLimit.value = mb
        evictor.limitBytes = AudioCacheLimits.bytes(mb)
        setEnabled(mb != 0)
        val store = store()
        synchronized(store) { evictor.trim(store) }
        refresh()
    }
    suspend fun clear() = withContext(Dispatchers.IO) {
        val store = store()
        synchronized(store) {
            generation++
            retired.addAll(store.keys)
            for (key in store.keys) store.applyContentMetadataMutations(key,
                ContentMetadataMutations().set(RETIRED, 1L).remove(HASH))
            reclaim(store)
        }
        refresh()
    }
    fun setPlaybackKey(key: String?) {
        playbackKey = key
        scope.launch {
            cache?.let { synchronized(it) { reclaim(it); evictor.trim(it) } }
            refresh()
        }
    }
    private fun reclaim(store: Cache) {
        for (key in retired.toList()) if (!isProtected(key)) {
            store.removeResource(key); retired.remove(key)
        }
    }
    fun markInvalid(trackId: String) {
        // Expired HTTP URLs do not invalidate already received bytes.
        scope.launch {
            val store = store()
            synchronized(store) {
                for (key in store.keys) {
                    val json = descriptor(store, key) ?: continue
                    if (json.optString("track") == trackId &&
                        store.getContentMetadata(key).get(HASH, "").orEmpty().isNotBlank() &&
                        !validFiles(store, key, json.optLong("length")))
                        retired.add(key)
                }
                reclaim(store)
            }
            refresh()
        }
    }

    suspend fun prepare(trackId: String, quality: AudioQualityMode, stream: AudioStream): String? =
        withContext(Dispatchers.IO) {
            synchronized(runtimeHeaders) { runtimeHeaders[stream.url] = stream.headers }
            if (!enabled.value || limitMb.value == 0) return@withContext null
            val key = AudioCacheIdentity.key(trackId, stream, personalBuild) ?: return@withContext null
            try {
                val store = store()
                synchronized(store) {
                    if (key in retired) return@withContext null
                    var previous = descriptor(store, key)
                    if (previous != null && previous.optLong("length") > 0 && stream.contentLength > 0 &&
                        previous.optLong("length") != stream.contentLength) {
                        if (isProtected(key)) return@withContext null
                        store.removeResource(key); previous = null
                    }
                    val savedQualities = previous?.optString("qualities").orEmpty().split(',').filter(String::isNotBlank).toSet()
                    val qualities = if (stream.isLocalCache) savedQualities else savedQualities + quality.name
                    val json = JSONObject().apply {
                        put("track", trackId); put("provider", stream.providerId); put("format", stream.formatId)
                        put("codec", stream.codec); put("mime", stream.mimeType); put("bitrate", stream.bitrate)
                        put("length", stream.contentLength.takeIf { it > 0 } ?: previous?.optLong("length") ?: 0L)
                        put("revision", stream.representationRevision); put("audioTrack", stream.audioTrackId)
                        put("qualities", qualities.joinToString(","))
                    }
                    val mutations = ContentMetadataMutations().set(DESCRIPTOR, json.toString())
                    if (json.optLong("length") > 0) ContentMetadataMutations.setContentLength(mutations, json.getLong("length"))
                    store.applyContentMetadataMutations(key, mutations)
                }
                key
            } catch (error: Exception) {
                mutableStats.value = mutableStats.value.copy(error = error.message); null
            }
        }

    suspend fun fullyCached(trackId: String, quality: AudioQualityMode, allowAnyQuality: Boolean = false): AudioStream? = withContext(Dispatchers.IO) {
        if (!enabled.value || limitMb.value == 0) return@withContext null
        try {
            val store = store()
            synchronized(store) {
                for (key in store.keys.sorted()) {
                    if (key in retired) continue
                    val json = descriptor(store, key) ?: continue
                    if (json.optString("track") != trackId ||
                        (!allowAnyQuality && quality.name !in json.optString("qualities").split(','))) continue
                    val stream = runCatching { streamFrom(json, key) }.getOrNull() ?: continue
                    if (AudioCacheIdentity.key(trackId, stream, personalBuild) != key) continue
                    if (store.getContentMetadata(key).get(HASH, "").orEmpty().isBlank() &&
                        validFiles(store, key, stream.contentLength)) runCatching { validateAndPublish(key, generation, store) }
                    val expectedHash = store.getContentMetadata(key).get(HASH, "").orEmpty()
                    if (expectedHash.isBlank()) continue
                    if (!validFiles(store, key, stream.contentLength) ||
                        AudioFileRanges.digest(pieces(store, key), stream.contentLength) != expectedHash) {
                        retired.add(key); reclaim(store); continue
                    }
                    mutableStats.value = mutableStats.value.copy(hits = mutableStats.value.hits + 1)
                    Log.i(TAG, "hit track=$trackId bytes=" + stream.contentLength)
                    return@withContext stream
                }
            }
        } catch (error: Exception) { Log.w(TAG, "Local audio lookup failed", error) }
        mutableStats.value = mutableStats.value.copy(misses = mutableStats.value.misses + 1)
        null
    }
    private fun descriptor(cache: Cache, key: String): JSONObject? = runCatching {
        cache.getContentMetadata(key).get(DESCRIPTOR, "")?.takeIf(String::isNotBlank)?.let(::JSONObject)
    }.getOrNull()
    private fun streamFrom(json: JSONObject, key: String) = AudioStream(
        url = "https://stereo-cache.invalid/$key", providerId = json.getString("provider"),
        cachePermission = AudioCachePermission.PERMITTED, formatId = json.getString("format"),
        codec = json.getString("codec"), mimeType = json.getString("mime"), bitrate = json.getInt("bitrate"),
        contentLength = json.getLong("length"), representationRevision = json.optString("revision"),
        audioTrackId = json.optString("audioTrack"), isLocalCache = true
    )
    private fun pieces(store: Cache, key: String) = store.getCachedSpans(key).map {
        AudioRangeFile(it.position, it.length, requireNotNull(it.file))
    }
    private fun validFiles(store: Cache, key: String, length: Long) = AudioFileRanges.complete(pieces(store, key), length)

    private fun validateAndPublish(key: String, epoch: Long, store: SimpleCache = store()) {
        synchronized(store) {
            if (epoch != generation || key in retired || !enabled.value) return
            val json = descriptor(store, key) ?: return
            var length = json.optLong("length")
            if (length <= 0) {
                length = ContentMetadata.getContentLength(store.getContentMetadata(key))
                if (length <= 0) return
                json.put("length", length)
            }
            if (!validFiles(store, key, length)) return
            val pieces = pieces(store, key)
            val hash = AudioFileRanges.digest(pieces, length)
            if (store.getContentMetadata(key).get(HASH, "") == hash) return
            val finalLength = length
            val data = object : MediaDataSource() {
                override fun getSize() = finalLength
                override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int) =
                    AudioFileRanges.read(pieces, finalLength, position, buffer, offset, size)
                override fun close() = Unit
            }
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(data)
                val audio = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: throw IOException("No readable audio track")
                extractor.selectTrack(audio)
                val format = extractor.getTrackFormat(audio)
                val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
                check(duration > 0) { "Unknown or invalid audio duration" }
                check(extractor.sampleTime >= 0) { "No audio samples" }
                extractor.seekTo((duration - 500_000L).coerceAtLeast(0), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                check(extractor.sampleTime >= 0) { "Unreadable audio tail" }
            } finally { extractor.release(); data.close() }
            store.applyContentMetadataMutations(key, ContentMetadataMutations().set(DESCRIPTOR, json.toString()).set(HASH, hash))
            Log.i(TAG, "complete track=" + json.optString("track") + " bytes=$length sha256=$hash")
        }
    }

    suspend fun prefetch(trackId: String, quality: AudioQualityMode, stream: AudioStream, upstream: DataSource.Factory,
                         allowed: () -> Boolean) = withContext(Dispatchers.IO) {
        if (!allowed() || !enabled.value || !prefetchEnabled.value) return@withContext
        val key = prepare(trackId, quality, stream) ?: return@withContext
        val source = factory(upstream).createDataSource()
        try {
            currentCoroutineContext().ensureActive()
            source.open(DataSpec.Builder().setUri(stream.url).setKey(key)
                .setLength(minOf(1_048_576L, stream.contentLength.takeIf { it > 0 } ?: 1_048_576L)).build())
            val buffer = ByteArray(32 * 1024)
            var remaining = 1_048_576L
            while (remaining > 0 && allowed()) {
                currentCoroutineContext().ensureActive()
                val count = source.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
                if (count < 0) break
                remaining -= count
            }
        } finally { runCatching { source.close() } }
    }

    fun factory(upstream: DataSource.Factory): DataSource.Factory = DataSource.Factory {
        object : DataSource {
            private val listeners = mutableListOf<TransferListener>()
            private var source: DataSource? = null
            private lateinit var spec: DataSpec
            private var consumed = 0L
            private var leasedKey: String? = null
            private var cached = false
            private var epoch = 0L
            override fun addTransferListener(listener: TransferListener) { listeners.add(listener); source?.addTransferListener(listener) }
            private fun attach(next: DataSource) = next.also { source = it; listeners.forEach(it::addTransferListener) }
            override fun open(dataSpec: DataSpec): Long {
                consumed = 0
                epoch = generation
                spec = dataSpec.withAdditionalHeaders(synchronized(runtimeHeaders) { runtimeHeaders[dataSpec.uri.toString()].orEmpty() })
                val key = spec.key
                val attachedPlayback = key != null && playbackKey == key
                val store = if (key == null || (!enabled.value && !attachedPlayback) || (key in retired && !attachedPlayback))
                    null else runCatching { store() }.getOrNull()
                val json = if (store != null && key != null) descriptor(store, key) else null
                cached = json != null && runCatching {
                    AudioCacheIdentity.key(json.optString("track"), streamFrom(json, key!!), personalBuild) == key
                }.getOrDefault(false)
                if (cached) synchronized(store!!) {
                    if (key in retired && playbackKey != key) cached = false else {
                        readers.compute(key!!) { _, count -> (count ?: 0) + 1 }; leasedKey = key
                    }
                }
                val local = spec.uri.host == "stereo-cache.invalid"
                val offlineUpstream = DataSource.Factory { object : DataSource {
                    override fun addTransferListener(listener: TransferListener) = Unit
                    override fun open(dataSpec: DataSpec): Long = throw IOException("Cached audio is no longer available; reconnect to stream")
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1
                    override fun getUri(): Uri? = null
                    override fun close() = Unit
                } }
                val next = if (cached) CacheDataSource.Factory().setCache(store!!)
                    .setUpstreamDataSourceFactory(if (local) offlineUpstream else upstream)
                    .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(store).setFragmentSize(fragmentBytes))
                    .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .createDataSource() else (if (local) offlineUpstream else upstream).createDataSource()
                return try { attach(next).open(spec) } catch (error: IOException) {
                    if (!cached || local || !isCacheFailure(error)) throw error
                    recover()
                }
            }
            private fun isCacheFailure(error: IOException): Boolean =
                error is Cache.CacheException || error is FileNotFoundException ||
                    generateSequence<Throwable>(error) { it.cause }.any { it is Cache.CacheException }
            private fun recover(): Long {
                runCatching { source?.close() }
                leasedKey?.let { retired.add(it) }; cached = false
                return attach(upstream.createDataSource()).open(spec.subrange(consumed))
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                val count = try { checkNotNull(source).read(buffer, offset, length) } catch (error: IOException) {
                    if (!cached || spec.uri.host == "stereo-cache.invalid" || !isCacheFailure(error)) throw error
                    recover(); checkNotNull(source).read(buffer, offset, length)
                }
                if (count > 0) consumed += count
                return count
            }
            override fun getUri(): Uri? = source?.uri
            override fun getResponseHeaders(): Map<String, List<String>> = source?.responseHeaders.orEmpty()
            override fun close() {
                try { source?.close() } finally {
                    source = null
                    val key = leasedKey
                    leasedKey = null
                    if (key != null) {
                        readers.computeIfPresent(key) { _, count -> (count - 1).takeIf { it > 0 } }
                        val capturedEpoch = epoch
                        scope.launch {
                            try { validateAndPublish(key, capturedEpoch) }
                            catch (error: Exception) { Log.w(TAG, "Audio validation deferred: " + error.message) }
                            cache?.let { synchronized(it) { reclaim(it); evictor.trim(it) } }; refresh()
                        }
                    }
                }
            }
        }
    }
    @Synchronized fun close() { closed = true; scope.cancel(); cache?.release(); cache = null; database.close() }
    private companion object {
        const val DESCRIPTOR = "stereo_descriptor_v2"
        const val HASH = "stereo_complete_sha256_v2"
        const val RETIRED = "stereo_removed_v2"
        const val TAG = "STEREO_CACHE"
    }
}
