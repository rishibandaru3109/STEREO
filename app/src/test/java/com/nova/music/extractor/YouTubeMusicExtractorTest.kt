package com.nova.music.extractor

import com.nova.music.data.extractor.YouTubeMusicExtractor
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class YouTubeMusicExtractorTest {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val extractor = YouTubeMusicExtractor(httpClient)

    @Test
    fun testSearchReturnsRealTracks() = runBlocking {
        val query = "Get Lucky Daft Punk"
        val result = extractor.search(query)

        if (result.isFailure) {
            System.err.println("=== SEARCH FAILED ===")
            result.exceptionOrNull()?.printStackTrace()
        }

        assertTrue("Search should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val tracks = result.getOrNull()
        assertNotNull("Track list should not be null", tracks)
        assertFalse("Track list should not be empty", tracks!!.isEmpty())

        val firstTrack = tracks.first()
        println("=== FIRST TRACK FOUND ===")
        println("ID: ${firstTrack.id}")
        println("Title: ${firstTrack.title}")
        println("Artist: ${firstTrack.artist}")
        println("Thumbnail: ${firstTrack.thumbnailUrl}")

        assertTrue("Video ID must not be blank", firstTrack.id.isNotBlank())
        assertTrue("Title must not be blank", firstTrack.title.isNotBlank())
    }

    @Test
    fun testResolveAudioStream() = runBlocking {
        // Standard video ID (e.g., Daft Punk - Get Lucky: 5NV6Rdv1a3I)
        val trackId = "5NV6Rdv1a3I"
        val result = extractor.resolveAudioStream(trackId)

        if (result.isFailure) {
            System.err.println("=== RESOLVE FAILED ===")
            result.exceptionOrNull()?.printStackTrace()
        }

        assertTrue("Audio stream resolution should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val stream = result.getOrNull()
        assertNotNull("Stream should not be null", stream)

        println("=== RESOLVED AUDIO STREAM ===")
        println("URL: ${stream!!.url.take(80)}...")
        println("MIME: ${stream.mimeType}")
        println("Codec: ${stream.codec}")
        println("Bitrate: ${stream.bitrate} bps")
        println("Format ID: ${stream.formatId}")
        println("Audio Only: ${stream.isAudioOnly}")

        assertTrue("Stream URL must not be blank", stream.url.isNotBlank())
        assertTrue("Format must be audio-only", stream.isAudioOnly)
        assertTrue("Codec should be opus or aac", stream.codec == "opus" || stream.codec == "aac")
    }

    @Test
    fun testMultiTrackStreamResolutionAndAudioDataValidation() = runBlocking {
        val testTracks = listOf(
            "J7p4bzqLvCw" to "The Weeknd - Blinding Lights",
            "5NV6Rdv1a3I" to "Daft Punk - Get Lucky",
            "U3ASj1L6_sY" to "Adele - Easy On Me"
        )

        for ((trackId, trackName) in testTracks) {
            println("\n=======================================================")
            println("Testing track: $trackName ($trackId)")
            println("=======================================================")

            val startTime = System.currentTimeMillis()
            val result = extractor.resolveAudioStream(trackId)
            val elapsed = System.currentTimeMillis() - startTime
            assertTrue("Resolution for $trackName must succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)

            val stream = result.getOrNull()!!
            println("Resolution time: ${elapsed}ms")
            println("Stream codec: ${stream.codec}, itag: ${stream.formatId}, bitrate: ${stream.bitrate} bps")
            println("Is Audio-Only: ${stream.isAudioOnly}")
            println("Headers count: ${stream.headers.size}")
            assertTrue("Stream must be audio-only", stream.isAudioOnly)
            assertTrue("Codec must be opus or aac", stream.codec == "opus" || stream.codec == "aac")
            assertTrue("Bitrate must be > 0", stream.bitrate > 0)
            assertTrue("Headers must include User-Agent", stream.headers.containsKey("User-Agent"))
            assertTrue("Headers must include Referer", stream.headers.containsKey("Referer"))

            // Fetch initial audio chunk using stream's headers (replicating OkHttpDataSource pipeline)
            val reqBuilder = okhttp3.Request.Builder()
                .url(stream.url)
                .addHeader("Range", "bytes=0-16383")
            stream.headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                println("HTTP Response: ${response.code} ${response.message}")
                println("Content-Type: ${response.header("Content-Type")}")
                println("Content-Range: ${response.header("Content-Range")}")
                println("Content-Length: ${response.header("Content-Length")}")

                assertTrue("Response code must be 200 or 206, got ${response.code}", response.code == 200 || response.code == 206)

                val audioBytes = response.body?.bytes()
                assertNotNull("Audio body must not be null", audioBytes)
                assertTrue("Must receive valid bytes", (audioBytes?.size ?: 0) > 1000)
                println("Received valid audio payload: ${audioBytes!!.size} bytes")

                // Binary container header validation:
                if (stream.codec == "opus") {
                    // WebM / EBML magic header: 0x1A, 0x45, 0xDF, 0xA3
                    assertEquals("WebM byte 0 must be 0x1A", 0x1A.toByte(), audioBytes[0])
                    assertEquals("WebM byte 1 must be 0x45", 0x45.toByte(), audioBytes[1])
                    assertEquals("WebM byte 2 must be 0xDF", 0xDF.toByte(), audioBytes[2])
                    assertEquals("WebM byte 3 must be 0xA3", 0xA3.toByte(), audioBytes[3])
                    println("VERIFIED: Valid WebM/Opus container header detected (0x1A45DFA3)")
                } else if (stream.codec == "aac") {
                    // MP4 / M4A ftyp box: bytes 4-7 are 'f', 't', 'y', 'p'
                    assertEquals("MP4 'f'", 'f'.code.toByte(), audioBytes[4])
                    assertEquals("MP4 't'", 't'.code.toByte(), audioBytes[5])
                    assertEquals("MP4 'y'", 'y'.code.toByte(), audioBytes[6])
                    assertEquals("MP4 'p'", 'p'.code.toByte(), audioBytes[7])
                    println("VERIFIED: Valid MP4/AAC container header detected (ftyp)")
                }
            }
        }
    }
}
