package com.nova.music.network

import com.nova.music.core.network.*
import com.nova.music.core.artwork.sizedArtworkUrl
import com.nova.music.domain.model.*
import com.nova.music.data.extractor.YouTubeMusicExtractor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class DataEfficiencyTest {
    @Test
    fun autoTracksMeteringAndExplicitModesOverrideIt() {
        assertEquals(AudioQualityMode.DATA_SAVER, AudioQualityMode.AUTO.effective(true))
        assertEquals(AudioQualityMode.HIGH, AudioQualityMode.AUTO.effective(false))
        for (mode in listOf(AudioQualityMode.DATA_SAVER, AudioQualityMode.BALANCED, AudioQualityMode.HIGH)) {
            assertEquals(mode, mode.effective(true))
            assertEquals(mode, mode.effective(false))
        }
        assertEquals(AudioQualityMode.AUTO, AudioQualityMode.fromStored("obsolete"))
    }

    @Test
    fun selectsRealAvailableBitratesWithSparseAndUnknownFormats() {
        val formats = listOf(160000, 48000, 128000, 70000)
        assertEquals(48000, selectAudioFormat(formats, AudioQualityMode.DATA_SAVER) { it })
        assertEquals(70000, selectAudioFormat(formats, AudioQualityMode.BALANCED) { it })
        assertEquals(160000, selectAudioFormat(formats, AudioQualityMode.HIGH) { it })
        assertEquals(128000, selectAudioFormat(listOf(128000), AudioQualityMode.DATA_SAVER) { it })
        assertEquals(64000, selectAudioFormat(listOf(0, 64000), AudioQualityMode.DATA_SAVER) { it })
        assertNull(selectAudioFormat(emptyList<Int>(), AudioQualityMode.BALANCED) { it })
    }

    @Test
    fun fallbackParserSelectsAudioAndPreservesDefaultLanguage() {
        val extractor = YouTubeMusicExtractor(OkHttpClient())
        val response = """{"streamingData":{"adaptiveFormats":[
            {"itag":18,"mimeType":"video/mp4","bitrate":10000,"url":"https://example.com/video"},
            {"itag":249,"mimeType":"audio/webm; codecs=opus","bitrate":48000,"url":"https://example.com/low",
             "audioTrack":{"audioIsDefault":true}},
            {"itag":250,"mimeType":"audio/webm; codecs=opus","bitrate":70000,"url":"https://example.com/mid",
             "audioTrack":{"audioIsDefault":true}},
            {"itag":251,"mimeType":"audio/webm; codecs=opus","bitrate":160000,"url":"https://example.com/high",
             "audioTrack":{"audioIsDefault":true}},
            {"itag":599,"mimeType":"audio/mp4","bitrate":24000,"url":"https://example.com/dub",
             "audioTrack":{"audioIsDefault":false}}
        ]}}"""
        assertEquals("249", extractor.selectAndDecipherAudioStream(response, AudioQualityMode.DATA_SAVER)?.formatId)
        assertEquals("250", extractor.selectAndDecipherAudioStream(response, AudioQualityMode.BALANCED)?.formatId)
        assertEquals("251", extractor.selectAndDecipherAudioStream(response, AudioQualityMode.HIGH)?.formatId)
    }

    @Test
    fun byteMeterCountsConsumptionNotAdvertisedSizeAndSeparatesKinds() {
        val counters = TrafficCounters()
        counters.request(80)
        val body = MeteredResponseBody(ByteArray(20000).toResponseBody(), counters, TrafficKind.AUDIO)
        val sink = Buffer()
        body.source().read(sink, 100)
        val partial = counters.snapshot().audioBytes
        assertTrue(partial in 100L..19999L) // buffered reads count bytes actually taken from the socket source
        body.close()
        assertEquals(partial, counters.snapshot().audioBytes)
        MeteredResponseBody(ByteArray(300).toResponseBody(), counters, TrafficKind.ARTWORK).use { it.bytes() }
        MeteredResponseBody(ByteArray(150).toResponseBody(), counters, TrafficKind.API).use { it.bytes() }
        val snapshot = counters.snapshot()
        assertEquals(300L, snapshot.artworkBytes)
        assertEquals(150L, snapshot.apiBytes)
        assertEquals(80L, snapshot.declaredUploadBytes)
        counters.add(TrafficKind.AUDIO, -1)
        assertEquals(partial, counters.snapshot().audioBytes)
        assertEquals(TrafficKind.AUDIO, TrafficMeter.classify("r1.googlevideo.com", "application/octet-stream"))
        assertEquals(TrafficKind.ARTWORK, TrafficMeter.classify("lh3.googleusercontent.com", "image/webp"))
        assertEquals(TrafficKind.API, TrafficMeter.classify("googlevideo.com.example.com", "application/json"))
    }

    @Test
    fun syntheticThirtyMinuteReplayAndSkippingFixturesAreReproducible() {
        val bytesPerSecondHigh = 160000L / 8
        val bytesPerSecondSaver = 48000L / 8
        // Synthetic fixed-rate payload fixtures, not a live network benchmark.
        fun consume(seconds: Int, bytesPerSecond: Long, counters: TrafficCounters) {
            repeat(seconds) {
                MeteredResponseBody(ByteArray(bytesPerSecond.toInt()).toResponseBody(),
                    counters, TrafficKind.AUDIO).use { it.bytes() }
            }
        }
        val high = TrafficCounters()
        val saver = TrafficCounters()
        consume(1800, bytesPerSecondHigh, high)
        consume(1800, bytesPerSecondSaver, saver)
        assertEquals(36_000_000L, high.snapshot().audioBytes)
        assertEquals(10_800_000L, saver.snapshot().audioBytes)
        consume(1800, bytesPerSecondSaver, saver)
        assertEquals(21_600_000L, saver.snapshot().audioBytes) // replay requires new bytes without authorized audio caching
        val beforeSkip = 30 * (5 + 50) * bytesPerSecondHigh
        val afterSkip = 30 * (5 + 15) * bytesPerSecondHigh
        assertEquals(33_000_000L, beforeSkip)
        assertEquals(12_000_000L, afterSkip) // arithmetic upper-buffer scenario, not observed ExoPlayer downloads
    }

    @Test
    fun artworkSizingKeepsCredentialsAndUnknownHostsIntact() {
        assertEquals("https://lh3.googleusercontent.com/id=w128-h128-l90",
            sizedArtworkUrl("https://lh3.googleusercontent.com/id=w1200-h1200-l90", 96))
        assertEquals("https://lh3.googleusercontent.com/id=w768-h768-l90",
            sizedArtworkUrl("https://lh3.googleusercontent.com/id=w1200-h1200-l90", 1000))
        val signed = "https://example.com/id=w1200-h1200?token=1"
        assertEquals(signed, sizedArtworkUrl(signed, 48))
        val small = "https://i.ytimg.com/vi/id/hqdefault.jpg"
        assertEquals(small, sizedArtworkUrl(small, 64))
    }
}
