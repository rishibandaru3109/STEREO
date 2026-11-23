package com.nova.music.network

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import com.nova.music.core.cache.ConfigurableAudioEvictor
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy
import java.util.TreeSet

class AudioCacheEvictionTest {
    private class StoredSpans {
        val spans = mutableListOf<CacheSpan>()
        val removed = mutableListOf<String>()
        val cache = Proxy.newProxyInstance(Cache::class.java.classLoader, arrayOf(Cache::class.java)) { _, method, args ->
            when (method.name) {
                "getKeys" -> spans.map { it.key }.toSet()
                "getCachedSpans" -> TreeSet(spans.filter { it.key == args!![0] })
                "getCacheSpace" -> spans.sumOf { it.length }
                "removeSpan" -> {
                    val span = args!![0] as CacheSpan
                    spans.remove(span)
                    removed.add(span.key)
                    null
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Cache

        fun add(key: String, size: Long, touchedAt: Long) {
            spans += CacheSpan(key, 0L, size, touchedAt, File("$key.cache"))
        }
    }

    @Test fun leastRecentlyUsedBytesAreRemovedFirst() {
        val stored = StoredSpans().apply {
            add("recent", 40, 30)
            add("oldest", 40, 10)
            add("middle", 40, 20)
        }
        ConfigurableAudioEvictor(80) {}.trim(stored.cache)
        assertEquals(listOf("oldest"), stored.removed)
        assertEquals(80L, stored.cache.cacheSpace)
    }

    @Test fun loweringTheLimitImmediatelyTrimsExistingSpans() {
        val stored = StoredSpans().apply { add("old", 60, 1); add("new", 40, 2) }
        val evictor = ConfigurableAudioEvictor(100) {}
        evictor.trim(stored.cache)
        assertTrue(stored.removed.isEmpty())
        evictor.limitBytes = 40
        evictor.trim(stored.cache)
        assertEquals(listOf("old"), stored.removed)
        assertEquals(40L, stored.cache.cacheSpace)
    }

    @Test fun incomingFragmentsReserveSpaceWithinTheLimit() {
        val stored = StoredSpans().apply { add("old", 40, 1); add("new", 40, 2) }
        ConfigurableAudioEvictor(100) {}.trim(stored.cache, incomingBytes = 30)
        assertEquals(listOf("old"), stored.removed)
        assertTrue(stored.cache.cacheSpace + 30 <= 100)
    }

    @Test fun activePlaybackSurvivesEvictionEvenWhenItIsTheOldest() {
        val stored = StoredSpans().apply { add("playing", 60, 1); add("idle", 40, 2) }
        ConfigurableAudioEvictor(60, isPinned = { it == "playing" }) {}.trim(stored.cache)
        assertEquals(listOf("idle"), stored.removed)
        assertEquals(60L, stored.cache.cacheSpace)
    }

    @Test(expected = Cache.CacheException::class) fun exhaustedStorageStopsCachingBeforeConsumingTheReserve() {
        val stored = StoredSpans()
        ConfigurableAudioEvictor(Long.MAX_VALUE, availableBytes = { 32 }, reserveBytes = 64) {}
            .onStartFile(stored.cache, "new", 0, 10)
    }

    @Test fun unlimitedStillHonorsTheFreeSpaceReserve() {
        val stored = StoredSpans().apply { add("old", 40, 1); add("new", 40, 2) }
        ConfigurableAudioEvictor(Long.MAX_VALUE, availableBytes = { 120 - stored.cache.cacheSpace }, reserveBytes = 64) {}
            .trim(stored.cache)
        assertEquals(listOf("old"), stored.removed)
    }
}
