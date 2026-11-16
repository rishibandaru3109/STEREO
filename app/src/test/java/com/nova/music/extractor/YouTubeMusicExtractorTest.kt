package com.nova.music.extractor

import com.nova.music.data.extractor.YouTubeMusicExtractor
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
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
}
