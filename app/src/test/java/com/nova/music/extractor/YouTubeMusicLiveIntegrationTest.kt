package com.nova.music.extractor

import com.nova.music.data.extractor.YouTubeMusicExtractor
import com.nova.music.testing.LiveNetworkTest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category
import java.util.concurrent.TimeUnit

/**
 * Live integration tests for YouTubeMusicExtractor against live YouTube endpoints.
 * These require external internet access and are executed in the `testLiveIntegration` suite.
 */
@Category(LiveNetworkTest::class)
class YouTubeMusicLiveIntegrationTest {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val extractor = YouTubeMusicExtractor(httpClient)

    @Test
    fun testSearchReturnsRealTracks() = runBlocking {
        val query = "Get Lucky Daft Punk"
        val result = extractor.search(query)

        assertTrue("Search should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val tracks = result.getOrNull()
        assertNotNull("Track list should not be null", tracks)
        assertFalse("Track list should not be empty", tracks!!.isEmpty())

        val firstTrack = tracks.first()
        assertTrue("Video ID must not be blank", firstTrack.id.isNotBlank())
        assertTrue("Title must not be blank", firstTrack.title.isNotBlank())
    }

    @Test
    fun testResolveAudioStream() = runBlocking {
        val trackId = "5NV6Rdv1a3I"
        val result = extractor.resolveAudioStream(trackId)

        assertTrue("Audio stream resolution should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val stream = result.getOrNull()
        assertNotNull("Stream should not be null", stream)
        assertTrue("Stream URL must not be blank", stream!!.url.isNotBlank())
        assertTrue("Format must be audio-only", stream.isAudioOnly)
        assertTrue("Codec should be opus or aac", stream.codec == "opus" || stream.codec == "aac")
    }

    @Test
    fun testMultiTrackStreamResolutionAndAudioDataValidation() = runBlocking {
        val testTracks = listOf(
            "J7p4bzqLvCw" to "The Weeknd - Blinding Lights",
            "5NV6Rdv1a3I" to "Daft Punk - Get Lucky"
        )

        for ((trackId, trackName) in testTracks) {
            val result = extractor.resolveAudioStream(trackId)
            assertTrue("Resolution for $trackName must succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
            val stream = result.getOrNull()!!
            assertTrue("Stream must be audio-only", stream.isAudioOnly)
            assertTrue("Codec must be opus or aac", stream.codec == "opus" || stream.codec == "aac")
            assertTrue("Bitrate must be > 0", stream.bitrate > 0)
        }
    }
}
