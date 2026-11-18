package com.nova.music.extractor

import com.nova.music.data.extractor.YouTubeMusicExtractor
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic offline unit tests for YouTubeMusicExtractor.
 * Uses realistic response fixtures and mocked HTTP interceptors to test
 * JSON parsing, adaptive format extraction, and codec detection without live network calls.
 */
class YouTubeMusicExtractorTest {

    private val testHttpClient = OkHttpClient.Builder().build()
    private val extractor = YouTubeMusicExtractor(testHttpClient)

    @Test
    fun testParseDurationToSeconds() {
        assertEquals(200L, extractor.parseDurationToSeconds("3:20"))
        assertEquals(3900L, extractor.parseDurationToSeconds("1:05:00"))
        assertEquals(45L, extractor.parseDurationToSeconds("0:45"))
        assertEquals(0L, extractor.parseDurationToSeconds(""))
        assertEquals(0L, extractor.parseDurationToSeconds("LIVE"))
    }

    @Test
    fun testParseMusicSearchResponseWithSongFilter() {
        val searchJson = """
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
                                      "videoId": "5NV6Rdv1a3I"
                                    },
                                    "flexColumns": [
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              { "text": "Get Lucky" }
                                            ]
                                          }
                                        }
                                      },
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              { "text": "Daft Punk" },
                                              { "text": " • " },
                                              { "text": "Random Access Memories" },
                                              { "text": " • " },
                                              { "text": "4:08" }
                                            ]
                                          }
                                        }
                                      }
                                    ],
                                    "fixedColumns": [
                                      {
                                        "musicResponsiveListItemFixedColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              { "text": "4:08" }
                                            ]
                                          }
                                        }
                                      }
                                    ],
                                    "thumbnail": {
                                      "musicThumbnailRenderer": {
                                        "thumbnail": {
                                          "thumbnails": [
                                            { "url": "https://lh3.googleusercontent.com/daft_punk=w120-h120" }
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

        val tracks = extractor.parseMusicSearchResponse(searchJson)
        assertEquals(1, tracks.size)
        val track = tracks.first()
        assertEquals("5NV6Rdv1a3I", track.id)
        assertEquals("Get Lucky", track.title)
        assertEquals("Daft Punk", track.artist)
        assertEquals(248L, track.durationSeconds)
        assertEquals("https://lh3.googleusercontent.com/daft_punk=w120-h120", track.thumbnailUrl)
    }

    @Test
    fun testParseAndroidSearchResponseFallback() {
        val androidJson = """
        {
          "contents": {
            "twoColumnSearchResultsRenderer": {
              "primaryContents": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "itemSectionRenderer": {
                        "contents": [
                          {
                            "videoRenderer": {
                              "videoId": "android_vid_99",
                              "title": { "runs": [{ "text": "Android Search Track" }] },
                              "ownerText": { "runs": [{ "text": "Fallback Artist" }] },
                              "lengthText": { "simpleText": "2:30" },
                              "thumbnail": { "thumbnails": [{ "url": "https://i.ytimg.com/vi/android_vid_99/hqdefault.jpg" }] }
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
        }
        """.trimIndent()

        val tracks = extractor.parseAndroidSearchResponse(androidJson)
        assertEquals(1, tracks.size)
        val track = tracks.first()
        assertEquals("android_vid_99", track.id)
        assertEquals("Android Search Track", track.title)
        assertEquals("Fallback Artist", track.artist)
        assertEquals(150L, track.durationSeconds)
    }

    @Test
    fun testResolveAudioStreamFromMockedResponse() = runBlocking {
        val playerJson = """
        {
          "streamingData": {
            "adaptiveFormats": [
              {
                "itag": 251,
                "url": "https://rr2---sn-test.googlevideo.com/videoplayback?expire=99999&itag=251",
                "mimeType": "audio/webm; codecs=\"opus\"",
                "bitrate": 160000,
                "audioQuality": "AUDIO_QUALITY_MEDIUM"
              },
              {
                "itag": 140,
                "url": "https://rr2---sn-test.googlevideo.com/videoplayback?expire=99999&itag=140",
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
                .body(playerJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val testExtractor = YouTubeMusicExtractor(mockClient)
        val result = testExtractor.resolveAudioStream("test_track_77")

        assertTrue("Audio stream resolution must succeed with mock player response", result.isSuccess)
        val stream = result.getOrNull()!!
        assertTrue("Stream must point to googlevideo", stream.url.contains("googlevideo.com"))
        assertEquals("opus", stream.codec)
        assertEquals(160000, stream.bitrate)
        assertEquals("251", stream.formatId)
        assertTrue(stream.isAudioOnly)
        assertTrue(stream.headers.containsKey("User-Agent"))
        assertTrue(stream.headers.containsKey("Referer"))
    }

    @Test
    fun testResolveAudioStreamHandlesEmptyFormats() = runBlocking {
        val emptyPlayerJson = """
        {
          "streamingData": {
            "adaptiveFormats": []
          }
        }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(emptyPlayerJson.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()

        val testExtractor = YouTubeMusicExtractor(mockClient)
        val result = testExtractor.resolveAudioStream("empty_track_00")

        assertTrue("Result should fail when no formats are available", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Could not resolve playable audio stream") == true)
    }
}
