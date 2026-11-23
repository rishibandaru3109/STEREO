package com.nova.music.core.cache

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan

/** Evicts whole representations, preserving active readers and the current player. */
internal class ConfigurableAudioEvictor(
    @Volatile var limitBytes: Long,
    private val isPinned: (String) -> Boolean = { false },
    private val availableBytes: () -> Long = { Long.MAX_VALUE },
    private val reserveBytes: Long = 64L * 1024 * 1024,
    private val onSpaceChanged: (Long) -> Unit
) : CacheEvictor {
    override fun requiresCacheSpanTouches() = true
    override fun onCacheInitialized() = Unit
    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        trim(cache, length.coerceAtLeast(0))
        if (availableBytes() < reserveBytes + length.coerceAtLeast(0)) {
            throw Cache.CacheException("Free storage reserve reached")
        }
    }
    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        trim(cache)
        onSpaceChanged(cache.cacheSpace)
    }
    override fun onSpanRemoved(cache: Cache, span: CacheSpan) { onSpaceChanged(cache.cacheSpace) }
    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit

    fun trim(cache: Cache, incomingBytes: Long = 0) {
        synchronized(cache) {
            val groups = cache.keys.associateWith { cache.getCachedSpans(it) }
                .entries.sortedBy { entry -> entry.value.maxOfOrNull { it.lastTouchTimestamp } ?: 0 }
            for ((key, spans) in groups) {
                if (cache.cacheSpace <= (limitBytes - incomingBytes).coerceAtLeast(0) &&
                    availableBytes() >= reserveBytes + incomingBytes) break
                if (!isPinned(key)) spans.toList().forEach(cache::removeSpan)
            }
        }
    }
}
