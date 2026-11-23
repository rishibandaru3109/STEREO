package com.nova.music.network

import com.nova.music.core.cache.AudioCacheIdentity
import com.nova.music.core.cache.AudioCacheLimits
import com.nova.music.domain.model.AudioCachePermission
import com.nova.music.domain.model.AudioStream
import org.junit.Assert.*
import org.junit.Test

class AudioCacheIdentityTest {
    private val permitted = AudioStream(
        url = "https://audio.example.org/song?token=first",
        providerId = "permitted-provider",
        cachePermission = AudioCachePermission.PERMITTED,
        contentLength = 240_000,
        representationRevision = "revision-1"
    )

    @Test fun rotatedUrlsReuseTheSameRepresentationWithoutPersistingTokens() {
        val key = AudioCacheIdentity.key("track", permitted)
        assertNotNull(key)
        assertEquals(key, AudioCacheIdentity.key("track", permitted.copy(url = "https://cdn.example.org/new?token=second")))
        assertFalse(key!!.contains("first"))
        assertFalse(key.contains("audio.example"))
    }

    @Test fun incompatibleRepresentationsCannotShareBytes() {
        val original = AudioCacheIdentity.key("track", permitted)
        for (variant in listOf(
            permitted.copy(formatId = "250"), permitted.copy(codec = "aac"),
            permitted.copy(mimeType = "audio/mp4"), permitted.copy(bitrate = 48_000),
            permitted.copy(audioTrackId = "dub"),
            permitted.copy(representationRevision = "revision-2"), permitted.copy(providerId = "other")
        )) assertNotEquals(original, AudioCacheIdentity.key("track", variant))
        assertNotEquals(original, AudioCacheIdentity.key("another-track", permitted))
    }

    @Test fun deniedUnknownAndYouTubeSourcesBypassAudioStorage() {
        assertNull(AudioCacheIdentity.key("track", permitted.copy(cachePermission = AudioCachePermission.DENIED)))
        assertNull(AudioCacheIdentity.key("track", permitted.copy(providerId = "")))
        assertNull(AudioCacheIdentity.key("track", permitted.copy(providerId = "YouTube")))
        for (host in listOf("googlevideo.com", "r1.googlevideo.com", "youtube.com", "music.youtube.com", "youtu.be")) {
            assertNull(AudioCacheIdentity.key("track", permitted.copy(url = "https://$host/audio")))
        }
    }

    @Test fun invalidOrNonAudioRepresentationsBypassStorage() {
        for (variant in listOf(
            permitted.copy(contentLength = 0), permitted.copy(contentLength = -1),
            permitted.copy(formatId = ""), permitted.copy(codec = ""),
            permitted.copy(isAudioOnly = false), permitted.copy(mimeType = "video/mp4"),
            permitted.copy(url = "file:///local/audio"), permitted.copy(url = "invalid url")
        )) assertNull(AudioCacheIdentity.key("track", variant))
        assertNull(AudioCacheIdentity.key("", permitted))
    }

    @Test fun storageLimitsValidateAndUseLongByteCounts() {
        assertEquals(-1, AudioCacheLimits.validated(-1))
        assertEquals(500, AudioCacheLimits.validated(Int.MAX_VALUE))
        assertEquals(100_000_000L, AudioCacheLimits.bytes(100))
        assertEquals(1_000_000_000L, AudioCacheLimits.bytes(1000))
        assertEquals(0L, AudioCacheLimits.bytes(0))
        assertEquals(Long.MAX_VALUE, AudioCacheLimits.bytes(-1))
    }

    @Test fun personalDebugBuildCachesExistingDirectYouTubeAudioOnly() {
        val youtube = permitted.copy(providerId = "youtube", url = "https://r1.googlevideo.com/videoplayback?expire=1",
            cachePermission = AudioCachePermission.DENIED)
        assertNotNull(AudioCacheIdentity.key("track", youtube, personalBuild = true))
        assertNull(AudioCacheIdentity.key("track", youtube))
        assertNull(AudioCacheIdentity.key("track", youtube.copy(transport = com.nova.music.domain.model.AudioTransport.SABR), true))
        assertNull(AudioCacheIdentity.key("track", youtube.copy(transport = com.nova.music.domain.model.AudioTransport.SEGMENTED), true))
        assertEquals(AudioCacheIdentity.key("track", youtube, true),
            AudioCacheIdentity.key("track", youtube.copy(contentLength = 0), true))
    }
}
