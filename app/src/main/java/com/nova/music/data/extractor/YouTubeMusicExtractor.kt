package com.nova.music.data.extractor

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class YouTubeMusicExtractor(
    private val httpClient: OkHttpClient
) : MusicExtractor {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun search(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            // First attempt: YouTube Music search (music.youtube.com)
            val ytMusicResults = searchYouTubeMusic(query)
            if (ytMusicResults.isNotEmpty()) {
                return@withContext Result.success(ytMusicResults)
            }

            // Fallback: YouTube Android client search
            val androidResults = searchYouTubeAndroid(query)
            if (androidResults.isNotEmpty()) {
                return@withContext Result.success(androidResults)
            }

            Result.success(emptyList())
        } catch (e: Exception) {
            // If primary network calls fail, attempt fallback
            try {
                val androidResults = searchYouTubeAndroid(query)
                Result.success(androidResults)
            } catch (fallbackEx: Exception) {
                Result.failure(IOException("Failed to search music: ${e.message}", e))
            }
        }
    }

    private fun searchYouTubeMusic(query: String): List<Track> {
        val url = "https://music.youtube.com/youtubei/v1/search"
        val requestBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240722.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", query)
            // Song filter param
            put("params", "Eg-KAQwIABAAGAAgACgAMABqChAEEAMQCRAFEAo%3D")
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", "1.20240722.01.00")
            .addHeader("Referer", "https://music.youtube.com/")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            return parseMusicSearchResponse(body)
        }
    }

    private fun parseMusicSearchResponse(jsonStr: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i)?.optJSONObject("musicShelfRenderer") ?: continue
                val items = section.optJSONArray("contents") ?: continue

                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                    val track = parseMusicResponsiveItem(item)
                    if (track != null) {
                        tracks.add(track)
                    }
                }
            }
        } catch (_: Exception) {
            // Resilient fallback on parsing
        }
        return tracks
    }

    private fun parseMusicResponsiveItem(item: JSONObject): Track? {
        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() == 0) return null

        // 1. Title & VideoId
        val col0 = flexColumns.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs") ?: return null

        val titleRun = col0.optJSONObject(0) ?: return null
        val title = titleRun.optString("text")
        val videoId = titleRun.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.ifEmpty { null }
            ?: item.optJSONObject("playlistItemData")?.optString("videoId")?.ifEmpty { null }
            ?: item.optJSONObject("doubleTapCommand")?.optJSONObject("watchEndpoint")?.optString("videoId")?.ifEmpty { null }
            ?: return null

        // 2. Artist, Album, Duration from column 1
        var artist = "Unknown Artist"
        var album: String? = null
        var durationSeconds = 0L

        if (flexColumns.length() > 1) {
            val col1Runs = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            if (col1Runs != null && col1Runs.length() > 0) {
                artist = col1Runs.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
                if (col1Runs.length() > 2) {
                    val possibleAlbum = col1Runs.optJSONObject(2)?.optString("text")
                    if (possibleAlbum != null && !possibleAlbum.contains(":")) {
                        album = possibleAlbum
                    }
                }
            }
        }

        // Duration from fixed column if present
        val fixedColumns = item.optJSONArray("fixedColumns")
        if (fixedColumns != null && fixedColumns.length() > 0) {
            val durText = fixedColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""
            durationSeconds = parseDurationToSeconds(durText)
        }

        // 3. Thumbnail
        val thumbnails = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val thumbUrl = if (thumbnails != null && thumbnails.length() > 0) {
            thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url") ?: ""
        } else {
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }

        return Track(
            id = videoId,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbUrl
        )
    }

    private fun searchYouTubeAndroid(query: String): List<Track> {
        val url = "https://www.youtube.com/youtubei/v1/search"
        val requestBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID")
                    put("clientVersion", "19.29.37")
                    put("androidSdkVersion", 34)
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", query)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "com.google.android.youtube/19.29.37 (Linux; U; Android 14)")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            return parseAndroidSearchResponse(body)
        }
    }

    private fun parseAndroidSearchResponse(jsonStr: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)
            val contents = root.optJSONObject("contents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val itemSection = contents.optJSONObject(i)?.optJSONObject("itemSectionRenderer") ?: continue
                val items = itemSection.optJSONArray("contents") ?: continue

                for (j in 0 until items.length()) {
                    val videoRenderer = items.optJSONObject(j)?.optJSONObject("videoRenderer") ?: continue
                    val videoId = videoRenderer.optString("videoId").ifEmpty { null } ?: continue
                    val title = videoRenderer.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                    val artist = videoRenderer.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "YouTube"
                    val durationText = videoRenderer.optJSONObject("lengthText")?.optString("simpleText") ?: ""
                    val durationSeconds = parseDurationToSeconds(durationText)

                    val thumbs = videoRenderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                        thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                    } else {
                        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    }

                    tracks.add(
                        Track(
                            id = videoId,
                            title = title,
                            artist = artist,
                            durationSeconds = durationSeconds,
                            thumbnailUrl = thumbUrl
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return tracks
    }

    override suspend fun resolveAudioStream(trackId: String): Result<AudioStream> = withContext(Dispatchers.IO) {
        // 1. Try iOS Client (returns clean, direct audio stream URLs without signature cipher)
        try {
            val iosStream = resolveWithClient(trackId, clientName = "IOS", clientVersion = "19.29.1", deviceModel = "iPhone16,2")
            if (iosStream != null) {
                return@withContext Result.success(iosStream)
            }
        } catch (_: Exception) {}

        // 2. Try Android Client
        try {
            val androidStream = resolveWithClient(trackId, clientName = "ANDROID", clientVersion = "19.29.37", sdkVersion = 34)
            if (androidStream != null) {
                return@withContext Result.success(androidStream)
            }
        } catch (_: Exception) {}

        // 3. Try Piped API Public Resolvers as reliable secondary fallback
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.private.coffee",
            "https://pipedapi.tokhmi.xyz"
        )
        for (instance in pipedInstances) {
            try {
                val stream = resolveWithPiped(instance, trackId)
                if (stream != null) {
                    return@withContext Result.success(stream)
                }
            } catch (_: Exception) {}
        }

        Result.failure(IOException("Could not resolve playable audio stream for track: $trackId"))
    }

    private fun resolveWithClient(
        trackId: String,
        clientName: String,
        clientVersion: String,
        deviceModel: String? = null,
        sdkVersion: Int? = null
    ): AudioStream? {
        val url = "https://www.youtube.com/youtubei/v1/player"
        val clientObj = JSONObject().apply {
            put("clientName", clientName)
            put("clientVersion", clientVersion)
            put("hl", "en")
            put("gl", "US")
            if (deviceModel != null) put("deviceModel", deviceModel)
            if (sdkVersion != null) put("androidSdkVersion", sdkVersion)
        }

        val requestBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", clientObj)
            })
            put("videoId", trackId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X)")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return selectBestAudioStream(body)
        }
    }

    private fun selectBestAudioStream(jsonStr: String): AudioStream? {
        val root = JSONObject(jsonStr)
        val streamingData = root.optJSONObject("streamingData") ?: return null
        val formats = streamingData.optJSONArray("adaptiveFormats") ?: return null

        val audioFormats = mutableListOf<AudioStream>()
        for (i in 0 until formats.length()) {
            val format = formats.optJSONObject(i) ?: continue
            val mimeType = format.optString("mimeType")
            if (!mimeType.startsWith("audio/")) continue

            val directUrl = format.optString("url")
            if (directUrl.isEmpty()) continue // Skip cipher-encrypted formats without transform

            val bitrate = format.optInt("bitrate", 128000)
            val itag = format.optInt("itag", 0).toString()
            val contentLength = format.optLong("contentLength", 0L)
            val isOpus = mimeType.contains("opus")

            audioFormats.add(
                AudioStream(
                    url = directUrl,
                    mimeType = mimeType,
                    codec = if (isOpus) "opus" else "aac",
                    bitrate = bitrate,
                    formatId = itag,
                    contentLength = contentLength,
                    isAudioOnly = true
                )
            )
        }

        // Selection priority:
        // 1. Opus format with highest bitrate (typically itag 251 @ ~160kbps)
        // 2. Highest bitrate audio format available
        return audioFormats
            .sortedWith(
                compareByDescending<AudioStream> { it.codec == "opus" }
                    .thenByDescending { it.bitrate }
            )
            .firstOrNull()
    }

    private fun resolveWithPiped(instance: String, trackId: String): AudioStream? {
        val request = Request.Builder()
            .url("$instance/streams/$trackId")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val audioStreams = json.optJSONArray("audioStreams") ?: return null

            val streams = mutableListOf<AudioStream>()
            for (i in 0 until audioStreams.length()) {
                val s = audioStreams.optJSONObject(i) ?: continue
                val streamUrl = s.optString("url")
                if (streamUrl.isEmpty()) continue

                val mimeType = s.optString("mimeType", "audio/webm")
                val bitrate = s.optInt("bitrate", 128000)
                val codec = s.optString("codec", "opus")
                val formatId = s.optString("format", "251")

                streams.add(
                    AudioStream(
                        url = streamUrl,
                        mimeType = mimeType,
                        codec = codec,
                        bitrate = bitrate,
                        formatId = formatId,
                        isAudioOnly = true
                    )
                )
            }

            return streams
                .sortedWith(
                    compareByDescending<AudioStream> { it.codec.contains("opus") }
                        .thenByDescending { it.bitrate }
                )
                .firstOrNull()
        }
    }

    override suspend fun getTrackDetails(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
        // Fast track detail lookup
        Result.success(
            Track(
                id = trackId,
                title = "Unknown Song",
                artist = "Unknown Artist",
                thumbnailUrl = "https://i.ytimg.com/vi/$trackId/hqdefault.jpg"
            )
        )
    }

    private fun parseDurationToSeconds(text: String): Long {
        if (text.isEmpty()) return 0L
        val parts = text.split(":")
        return when (parts.size) {
            2 -> (parts[0].toLongOrNull() ?: 0) * 60 + (parts[1].toLongOrNull() ?: 0)
            3 -> (parts[0].toLongOrNull() ?: 0) * 3600 + (parts[1].toLongOrNull() ?: 0) * 60 + (parts[2].toLongOrNull() ?: 0)
            else -> 0L
        }
    }
}
