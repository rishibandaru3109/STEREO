package com.nova.music.extractor

import com.nova.music.data.extractor.InnerTubeXExtractor
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class InnerTubeXExtractorTest {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val extractor = InnerTubeXExtractor(httpClient)

    @Test
    fun testSearchReturnsRealTracks() = runBlocking {
        val query = "Blinding Lights The Weeknd"
        val result = extractor.search(query)

        assertTrue("Search should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val tracks = result.getOrNull()
        assertNotNull("Track list should not be null", tracks)
        assertFalse("Track list should not be empty", tracks!!.isEmpty())

        val firstTrack = tracks.first()
        println("=== INNERTUBEX FIRST TRACK FOUND ===")
        println("ID: ${firstTrack.id}")
        println("Title: ${firstTrack.title}")
        println("Artist: ${firstTrack.artist}")
        println("Thumbnail: ${firstTrack.thumbnailUrl}")

        assertTrue("Video ID must not be blank", firstTrack.id.isNotBlank())
        assertTrue("Title must not be blank", firstTrack.title.isNotBlank())
    }

    @Test
    fun testResolveAudioStreamReturnsValidStream() = runBlocking {
        val videoId = "4NRXx6U8ABQ" // Blinding Lights
        val result = extractor.resolveAudioStream(videoId)

        assertTrue("Stream resolution should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val stream = result.getOrNull()
        assertNotNull("Audio stream should not be null", stream)
        println("=== INNERTUBEX STREAM RESOLVED ===")
        println("URL: ${stream!!.url}")
        println("URL Host: ${java.net.URI(stream.url).host}")
        println("Codec: ${stream.codec}")
        println("Mime: ${stream.mimeType}")
        println("Headers: ${stream.headers.keys}")

        assertTrue("Stream URL must be valid", stream.url.startsWith("https://"))
        assertTrue("Stream URL must target Google Video CDN", stream.url.contains("googlevideo.com"))
    }

    @Test
    fun testResolveAudioStreamVivaLaVida() {
        runBlocking {
            val videoId = "ALsvdSA9tOU" // Viva La Vida
            val result = extractor.resolveAudioStream(videoId)
            assertTrue("Stream resolution should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
            val stream = result.getOrNull()
            assertNotNull("Audio stream should not be null", stream)
            println("=== VIVA LA VIDA RESULT: isSuccess=${result.isSuccess} ===")
            println("URL: ${stream!!.url}")
            println("Host: ${java.net.URI(stream.url).host}")
            assertTrue("Stream URL must be valid HTTPS", stream.url.startsWith("https://"))
            assertTrue("Stream URL must target Google Video CDN", stream.url.contains("googlevideo.com"))
        }
    }

    @Test
    fun testLiveSongRadioRelevance() = runBlocking {
        val seedVideoId = "4NRXx6U8ABQ" // Blinding Lights - The Weeknd
        val result = extractor.getSongRadio(seedVideoId)
        println("=== LIVE SONG RADIO SEED: Blinding Lights ($seedVideoId) ===")
        println("isSuccess: ${result.isSuccess}")
        if (result.isFailure) {
            println("Exception: ${result.exceptionOrNull()?.message}")
        } else {
            val radioTracks = result.getOrNull() ?: emptyList()
            println("Fetched ${radioTracks.size} candidate tracks from YouTube Music song radio graph:")
            radioTracks.take(10).forEachIndexed { index, track ->
                println("${index + 1}. [${track.id}] ${track.title} - ${track.artist} (${track.durationSeconds}s)")
            }
        }
    }
}
