package com.nova.music.extractor

import com.nova.music.data.extractor.InnerTubeXExtractor
import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic offline unit tests for InnerTubeXExtractor.
 * Uses realistic response fixtures and mocked HTTP interceptors to test
 * JSON parsing, track mapping, fallback logic, and stream resolution without network access.
 */
class InnerTubeXExtractorTest {

    private val testHttpClient = OkHttpClient.Builder().build()
    private val extractor = InnerTubeXExtractor(testHttpClient)

    @Test
    fun testParseDurationText() {
        assertEquals(225L, extractor.parseDurationText("3:45"))
        assertEquals(3735L, extractor.parseDurationText("1:02:15"))
        assertEquals(45L, extractor.parseDurationText("0:45"))
        assertEquals(0L, extractor.parseDurationText(""))
        assertEquals(0L, extractor.parseDurationText("LIVE"))
    }

    @Test
    fun testParseMusicSearchResponseReturnsValidTracks() {
        val jsonFixture = """
        {
          "contents": {
            "tabbedSearchResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "musicShelfRenderer": {
                              "contents": [
                                {
                                  "musicResponsiveListItemRenderer": {
                                    "playlistItemData": {
                                      "videoId": "4NRXx6U8ABQ"
                                    },
                                    "flexColumns": [
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              {
                                                "text": "Blinding Lights",
                                                "navigationEndpoint": {
                                                  "watchEndpoint": {
                                                    "videoId": "4NRXx6U8ABQ"
                                                  }
                                                }
                                              }
                                            ]
                                          }
                                        }
                                      },
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              { "text": "The Weeknd" },
                                              { "text": " • " },
                                              { "text": "After Hours" },
                                              { "text": " • " },
                                              { "text": "3:20" }
                                            ]
                                          }
                                        }
                                      }
                                    ],
                                    "thumbnail": {
                                      "musicThumbnailRenderer": {
                                        "thumbnail": {
                                          "thumbnails": [
                                            { "url": "https://lh3.googleusercontent.com/test_thumb=w120-h120" }
                                          ]
                                        }
                                      }
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val tracks = extractor.parseMusicSearchResponse(jsonFixture)
        assertNotNull(tracks)
        assertEquals(1, tracks.size)

        val track = tracks.first()
        assertEquals("4NRXx6U8ABQ", track.id)
        assertEquals("Blinding Lights", track.title)
        assertEquals("The Weeknd", track.artist)
        assertEquals("https://lh3.googleusercontent.com/test_thumb=w120-h120", track.thumbnailUrl)
        assertEquals(200L, track.durationSeconds)
    }

    @Test
    fun testParseSongRadioResponseExtractsTracks() {
        val radioJson = """
        {
          "contents": {
            "singleColumnMusicWatchNextResultsRenderer": {
              "tabbedRenderer": {
                "watchNextTabbedResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "musicQueueRenderer": {
                            "content": {
                              "playlistPanelRenderer": {
                                "contents": [
                                  {
                                    "playlistPanelVideoRenderer": {
                                      "videoId": "seed_123",
                                      "title": { "runs": [{ "text": "Seed Track" }] },
                                      "shortBylineText": { "runs": [{ "text": "Original Artist" }] }
                                    }
                                  },
                                  {
                                    "playlistPanelVideoRenderer": {
                                      "videoId": "candidate_456",
                                      "title": { "runs": [{ "text": "Radio Candidate" }] },
                                      "shortBylineText": { "runs": [{ "text": "Related Artist" }] },
                                      "lengthText": { "runs": [{ "text": "3:15" }] },
                                      "thumbnail": { "thumbnails": [{ "url": "https://lh3.googleusercontent.com/radio_thumb" }] }
                                    }
                                  }
                                ]
                              }
                            }
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()

        val candidates = extractor.parseSongRadioResponse("seed_123", radioJson)
        assertEquals(1, candidates.size)
        assertEquals("candidate_456", candidates.first().id)
        assertEquals("Radio Candidate", candidates.first().title)
        assertEquals("Related Artist", candidates.first().artist)
    }

    @Test
    fun testSearchFallbackEngagesWhenPrimaryFails() = runBlocking {
        val mockFallback = object : MusicExtractor {
            override suspend fun search(query: String): Result<List<Track>> {
                return Result.success(listOf(Track(id = "fallback_id", title = "Fallback Title", artist = "Fallback Artist")))
            }
            override suspend fun resolveAudioStream(trackId: String): Result<AudioStream> {
                return Result.failure(UnsupportedOperationException())
            }
            override suspend fun getTrackDetails(trackId: String): Result<Track> {
                return Result.failure(UnsupportedOperationException())
            }
        }

        // Mock client that returns empty search response
        val emptyClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"contents":{}}""".toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val testExtractor = InnerTubeXExtractor(emptyClient, fallbackExtractor = mockFallback)
        val result = testExtractor.search("Any query")

        assertTrue("Fallback search should succeed", result.isSuccess)
        val tracks = result.getOrNull()!!
        assertEquals(1, tracks.size)
        assertEquals("fallback_id", tracks.first().id)
        assertEquals("Fallback Title", tracks.first().title)
    }

    @Test
    fun testResolveAudioStreamWithMockedOkHttpInterceptor() = runBlocking {
        val fakePlayerJson = """
        {
          "streamingData": {
            "adaptiveFormats": [
              {
                "itag": 251,
                "url": "https://rr1---sn-test.googlevideo.com/videoplayback?expire=12345&itag=251",
                "mimeType": "audio/webm; codecs=\"opus\"",
                "bitrate": 160000,
                "audioQuality": "AUDIO_QUALITY_MEDIUM"
              },
              {
                "itag": 140,
                "url": "https://rr1---sn-test.googlevideo.com/videoplayback?expire=12345&itag=140",
                "mimeType": "audio/mp4; codecs=\"mp4a.40.2\"",
                "bitrate": 128000,
                "audioQuality": "AUDIO_QUALITY_MEDIUM"
              }
            ]
          }
        }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(fakePlayerJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val mockFallback = object : MusicExtractor {
            override suspend fun search(query: String): Result<List<Track>> = Result.success(emptyList())
            override suspend fun resolveAudioStream(trackId: String): Result<AudioStream> {
                // Fallback successfully returns the stream parsed from mockClient
                val fallbackYt = com.nova.music.data.extractor.YouTubeMusicExtractor(mockClient)
                return fallbackYt.resolveAudioStream(trackId)
            }
            override suspend fun getTrackDetails(trackId: String): Result<Track> {
                return Result.failure(UnsupportedOperationException())
            }
        }

        val testExtractor = InnerTubeXExtractor(mockClient, fallbackExtractor = mockFallback)
        val result = testExtractor.resolveAudioStream("test_vid_123")

        assertTrue("Stream resolution must succeed with mock response: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val stream = result.getOrNull()!!
        assertTrue("Stream must point to googlevideo", stream.url.contains("googlevideo.com"))
        assertEquals("opus", stream.codec)
        assertEquals(160000, stream.bitrate)
        assertTrue(stream.isAudioOnly)
        assertTrue(stream.headers.containsKey("User-Agent"))
    }
}
