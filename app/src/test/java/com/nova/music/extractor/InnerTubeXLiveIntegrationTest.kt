package com.nova.music.extractor

import com.nova.music.data.extractor.InnerTubeXExtractor
import com.nova.music.testing.LiveNetworkTest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category
import java.util.concurrent.TimeUnit

/**
 * Live integration tests for InnerTubeX against live YouTube endpoints.
 * These require external internet access and are executed in the `testLiveIntegration` suite.
 */
@Category(LiveNetworkTest::class)
class InnerTubeXLiveIntegrationTest {

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
        println("Codec: ${stream.codec}")
        println("Mime: ${stream.mimeType}")

        assertTrue("Stream URL must be valid", stream.url.startsWith("https://"))
        assertTrue("Stream URL must target Google Video CDN", stream.url.contains("googlevideo.com"))
    }

    @Test
    fun testResolveAudioStreamVivaLaVida() = runBlocking {
        val videoId = "ALsvdSA9tOU" // Viva La Vida
        val result = extractor.resolveAudioStream(videoId)
        assertTrue("Stream resolution should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val stream = result.getOrNull()
        assertNotNull("Audio stream should not be null", stream)
        assertTrue("Stream URL must be valid HTTPS", stream!!.url.startsWith("https://"))
        assertTrue("Stream URL must target Google Video CDN", stream.url.contains("googlevideo.com"))
    }

    @Test
    fun testLiveSongRadioRelevance() = runBlocking {
        val seedVideoId = "4NRXx6U8ABQ" // Blinding Lights - The Weeknd
        val result = extractor.getSongRadio(seedVideoId)
        println("=== LIVE SONG RADIO SEED: Blinding Lights ($seedVideoId) ===")
        println("isSuccess: ${result.isSuccess}")
        if (result.isSuccess) {
            val radioTracks = result.getOrNull() ?: emptyList()
            println("Fetched ${radioTracks.size} candidate tracks from YouTube Music song radio graph")
            assertTrue("Radio tracks should not be empty", radioTracks.isNotEmpty())
        }
    }
}
