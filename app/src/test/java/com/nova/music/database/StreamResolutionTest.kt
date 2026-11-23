package com.nova.music.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.nova.music.data.repository.MusicPreferencesRepositoryImpl
import com.nova.music.data.repository.MusicRepositoryImpl
import com.nova.music.domain.model.AudioQualityMode
import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class StreamResolutionTest {
    private class PreferencesStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }

    private class Extractor : MusicExtractor {
        val requests = java.util.concurrent.atomic.AtomicInteger()
        var expired = false
        var fail = false
        var lastQuality: AudioQualityMode? = null
        override suspend fun resolveAudioStream(trackId: String): Result<AudioStream> = error("Quality must be passed")
        override suspend fun resolveAudioStream(trackId: String, quality: AudioQualityMode): Result<AudioStream> {
            requests.incrementAndGet()
            lastQuality = quality
            delay(20)
            return if (fail) Result.failure(java.io.IOException("offline"))
            else Result.success(AudioStream("https://example.com/audio" + if (expired) "?expire=1" else ""))
        }
        override suspend fun search(query: String): Result<List<Track>> = Result.success(emptyList())
        override suspend fun getTrackDetails(trackId: String): Result<Track> = error("Not used")
    }

    @Test
    fun concurrentResolutionAndReplayReuseUrlMetadataAndInvalidationRefreshesIt() = runTest {
        val extractor = Extractor()
        val repository = MusicRepositoryImpl(extractor)
        val results = List(8) { async { repository.resolveStream("song").getOrThrow() } }.awaitAll()
        assertEquals(8, results.size)
        assertEquals(1, extractor.requests.get())
        repository.resolveStream("song").getOrThrow()
        assertEquals(1, extractor.requests.get())
        repository.invalidateStream("song")
        repository.resolveStream("song").getOrThrow()
        assertEquals(2, extractor.requests.get())
    }

    @Test
    fun preferenceChangeDoesNotReuseAnotherQualityAndExpiredUrlsAndFailuresAreRetried() = runTest {
        val extractor = Extractor()
        val preferences = MusicPreferencesRepositoryImpl(PreferencesStore())
        val repository = MusicRepositoryImpl(extractor, preferences)
        repository.resolveStream("song").getOrThrow()
        assertEquals(AudioQualityMode.DATA_SAVER, extractor.lastQuality) // unknown network is conservatively metered
        preferences.setAudioQualityMode(AudioQualityMode.HIGH)
        repository.resolveStream("song").getOrThrow()
        assertEquals(AudioQualityMode.HIGH, extractor.lastQuality)
        assertEquals(2, extractor.requests.get())
        extractor.expired = true
        repeat(2) { repository.resolveStream("expired").getOrThrow() }
        assertEquals(4, extractor.requests.get())
        extractor.fail = true
        repeat(2) { assertTrue(repository.resolveStream("offline").isFailure) }
        assertEquals(6, extractor.requests.get())
    }

    @Test
    fun networkChangesUpdateAutoWhileOverridesRemainStable() = runTest {
        val preferences = MusicPreferencesRepositoryImpl(PreferencesStore())
        val network = MutableStateFlow(com.nova.music.core.network.NetworkState(true, false))
        val repository = MusicRepositoryImpl(Extractor(), preferences, network)
        assertEquals(AudioQualityMode.HIGH, repository.effectiveAudioQuality.first())
        network.value = network.value.copy(metered = true)
        assertEquals(AudioQualityMode.DATA_SAVER, repository.effectiveAudioQuality.first())
        preferences.setAudioQualityMode(AudioQualityMode.BALANCED)
        assertEquals(AudioQualityMode.BALANCED, repository.effectiveAudioQuality.first())
        network.value = network.value.copy(metered = false)
        assertEquals(AudioQualityMode.BALANCED, repository.effectiveAudioQuality.first())
    }

    @Test
    fun cancellingTheOnlyRequesterCancelsResolutionAndReleasesTheLock() = runTest {
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val cancelled = kotlinx.coroutines.CompletableDeferred<Unit>()
        val extractor = object : MusicExtractor {
            override suspend fun resolveAudioStream(trackId: String): Result<AudioStream> {
                started.complete(Unit)
                try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
            override suspend fun search(query: String): Result<List<Track>> = Result.success(emptyList())
            override suspend fun getTrackDetails(trackId: String): Result<Track> = error("Not used")
        }
        val repository = MusicRepositoryImpl(extractor)
        val pending = async { repository.resolveStream("song") }
        started.await()
        pending.cancel()
        pending.join()
        assertTrue(cancelled.isCompleted)
    }
}
