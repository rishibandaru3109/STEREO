package com.nova.music.data.extractor

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import meow.nyx.quackjs.PlayerHints
import meow.nyx.quackjs.PlayerProgram
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder

class YouTubeMusicExtractor(
    private val httpClient: OkHttpClient
) : MusicExtractor {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedProgram: PlayerProgram? = null
    private val programLock = Any()

    companion object {
        private const val DEFAULT_STS = 20772
        private const val DEFAULT_SIG_EXPR = "Pl(16,2515,INPUT)"
        private const val DEFAULT_N_CLASS = "Ol"
        private const val BASE_JS_URL = "https://www.youtube.com/s/player/907ce1cd/player_ias.vflset/en_GB/base.js"
    }

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
            try {
                val androidResults = searchYouTubeAndroid(query)
                Result.success(androidResults)
            } catch (fallbackEx: Exception) {
                Result.failure(IOException("Failed to search music: ${e.message}", e))
            }
        }
    }

    private fun searchYouTubeMusic(query: String, filterSongs: Boolean = true): List<Track> {
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
            if (filterSongs) {
                // Song filter param (Base64 decoded value, do not percent-encode in JSON)
                put("params", "EgWKAQIIAWoKEAMQBBAJEAoQBQ==")
            }
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", "1.20240722.01.00")
            .addHeader("Referer", "https://music.youtube.com/")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val tracks = parseMusicSearchResponse(body)
            if (tracks.isEmpty() && filterSongs) {
                // Try unfiltered search if song-filtered returned empty
                return searchYouTubeMusic(query, filterSongs = false)
            }
            return tracks
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
                val secObj = contents.optJSONObject(i) ?: continue

                // 1. musicShelfRenderer (standard search results shelf)
                val musicShelf = secObj.optJSONObject("musicShelfRenderer")
                if (musicShelf != null) {
                    val items = musicShelf.optJSONArray("contents")
                    if (items != null) {
                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            parseMusicResponsiveItem(item)?.let { tracks.add(it) }
                        }
                    }
                }

                // 2. itemSectionRenderer (categorized/tabbed item sections)
                val itemSection = secObj.optJSONObject("itemSectionRenderer")
                if (itemSection != null) {
                    val items = itemSection.optJSONArray("contents")
                    if (items != null) {
                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            parseMusicResponsiveItem(item)?.let { tracks.add(it) }
                        }
                    }
                }

                // 3. musicCardShelfRenderer (featured hero / top hit card)
                val cardShelf = secObj.optJSONObject("musicCardShelfRenderer")
                if (cardShelf != null) {
                    val title = cardShelf.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    val videoId = cardShelf.optJSONObject("onTap")?.optJSONObject("watchEndpoint")?.optString("videoId")
                        ?: cardShelf.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)
                            ?.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")

                    if (!title.isNullOrEmpty() && !videoId.isNullOrEmpty()) {
                        val artist = cardShelf.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
                        val thumbUrl = cardShelf.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            ?.let { if (it.length() > 0) it.optJSONObject(it.length() - 1)?.optString("url") else null }
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                        tracks.add(
                            Track(
                                id = videoId,
                                title = title,
                                artist = artist,
                                thumbnailUrl = thumbUrl
                            )
                        )
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

        // 1. Title & VideoId from Column 0
        val col0Runs = flexColumns.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs") ?: return null

        if (col0Runs.length() == 0) return null
        val titleRun = col0Runs.optJSONObject(0) ?: return null
        val title = titleRun.optString("text").ifEmpty { return null }

        val videoId = titleRun.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?.ifEmpty { null }
            ?: item.optJSONObject("playlistItemData")?.optString("videoId")?.ifEmpty { null }
            ?: item.optJSONObject("doubleTapCommand")?.optJSONObject("watchEndpoint")?.optString("videoId")?.ifEmpty { null }
            ?: item.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")
                ?.optString("videoId")?.ifEmpty { null }
            ?: return null // Skip artist/album navigation items without playable tracks

        // 2. Artist & Album from Column 1
        var artist = "Unknown Artist"
        var album: String? = null
        var durationSeconds = 0L

        if (flexColumns.length() > 1) {
            val col1Runs = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            if (col1Runs != null && col1Runs.length() > 0) {
                val runTexts = mutableListOf<String>()
                for (k in 0 until col1Runs.length()) {
                    val t = col1Runs.optJSONObject(k)?.optString("text")?.trim() ?: ""
                    if (t.isNotEmpty() && t != "•" && t != "·") {
                        runTexts.add(t)
                    }
                }

                if (runTexts.isNotEmpty()) {
                    if (runTexts[0].equals("Song", ignoreCase = true) && runTexts.size > 1) {
                        artist = runTexts[1]
                        if (runTexts.size > 2 && !runTexts[2].contains(":")) {
                            album = runTexts[2]
                        }
                    } else {
                        artist = runTexts[0]
                        if (runTexts.size > 1 && !runTexts[1].contains(":")) {
                            album = runTexts[1]
                        }
                    }
                }
            }
        }

        // 3. Duration from fixed column if present
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

        // 4. Thumbnail
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
        try {
            val stream = resolveWithWebRemix(trackId)
            if (stream != null) {
                return@withContext Result.success(stream)
            }
        } catch (e: Exception) {
            // Log or fallback
        }

        Result.failure(IOException("Could not resolve playable audio stream for track: $trackId"))
    }

    private fun resolveWithWebRemix(trackId: String): AudioStream? {
        val url = "https://music.youtube.com/youtubei/v1/player"
        val requestBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240722.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("videoId", trackId)
            put("playbackContext", JSONObject().apply {
                put("contentPlaybackContext", JSONObject().apply {
                    put("signatureTimestamp", DEFAULT_STS)
                })
            })
        }.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", "1.20240722.01.00")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return selectAndDecipherAudioStream(body)
        }
    }

    private fun selectAndDecipherAudioStream(jsonStr: String): AudioStream? {
        val root = JSONObject(jsonStr)
        val streamingData = root.optJSONObject("streamingData") ?: return null
        val formats = streamingData.optJSONArray("adaptiveFormats") ?: return null

        data class CandidateFormat(
            val directUrl: String?,
            val cipher: String?,
            val mimeType: String,
            val codec: String,
            val bitrate: Int,
            val itag: String,
            val contentLength: Long
        )

        val candidates = mutableListOf<CandidateFormat>()
        for (i in 0 until formats.length()) {
            val format = formats.optJSONObject(i) ?: continue
            val mimeType = format.optString("mimeType")
            if (!mimeType.startsWith("audio/")) continue

            val directUrl = format.optString("url").ifEmpty { null }
            val cipher = format.optString("signatureCipher").ifEmpty {
                format.optString("cipher").ifEmpty { null }
            }

            if (directUrl == null && cipher == null) continue

            val bitrate = format.optInt("bitrate", 128000)
            val itag = format.optInt("itag", 0).toString()
            val contentLength = format.optLong("contentLength", 0L)
            val isOpus = mimeType.contains("opus")

            candidates.add(
                CandidateFormat(
                    directUrl = directUrl,
                    cipher = cipher,
                    mimeType = mimeType,
                    codec = if (isOpus) "opus" else "aac",
                    bitrate = bitrate,
                    itag = itag,
                    contentLength = contentLength
                )
            )
        }

        // Selection priority: Opus format with highest bitrate (itag 251), then AAC (itag 140)
        val bestCandidate = candidates
            .sortedWith(
                compareByDescending<CandidateFormat> { it.codec == "opus" }
                    .thenByDescending { it.bitrate }
            )
            .firstOrNull() ?: return null

        // If direct URL is available without cipher, return directly
        if (!bestCandidate.directUrl.isNullOrEmpty()) {
            return AudioStream(
                url = bestCandidate.directUrl,
                mimeType = bestCandidate.mimeType,
                codec = bestCandidate.codec,
                bitrate = bestCandidate.bitrate,
                formatId = bestCandidate.itag,
                contentLength = bestCandidate.contentLength,
                isAudioOnly = true
            )
        }

        // Decipher signature and transform N parameter
        val playableUrl = decipherMediaUrl(bestCandidate.cipher ?: return null) ?: return null

        return AudioStream(
            url = playableUrl,
            mimeType = bestCandidate.mimeType,
            codec = bestCandidate.codec,
            bitrate = bestCandidate.bitrate,
            formatId = bestCandidate.itag,
            contentLength = bestCandidate.contentLength,
            isAudioOnly = true
        )
    }

    private fun decipherMediaUrl(cipherString: String): String? {
        try {
            val params = parseQueryString(cipherString)
            val encryptedSig = params["s"] ?: return null
            val rawUrl = params["url"] ?: return null
            val sigParam = params["sp"] ?: "sig"

            val httpUrl = rawUrl.toHttpUrlOrNull() ?: return null
            val rawN = httpUrl.queryParameter("n")

            val program = getOrCompileProgram() ?: return null
            val decipheredSig = program.decipherSignature(encryptedSig)
            val transformedN = if (rawN != null) program.transformN(rawN) else null

            val urlBuilder = httpUrl.newBuilder()
            if (rawN != null && transformedN != null) {
                urlBuilder.removeAllQueryParameters("n")
                urlBuilder.addQueryParameter("n", transformedN)
            }
            urlBuilder.removeAllQueryParameters(sigParam)
            urlBuilder.addQueryParameter(sigParam, decipheredSig)

            return urlBuilder.build().toString()
        } catch (_: Exception) {
            return null
        }
    }

    private fun getOrCompileProgram(): PlayerProgram? {
        cachedProgram?.let { return it }

        synchronized(programLock) {
            cachedProgram?.let { return it }

            try {
                val request = Request.Builder()
                    .url(BASE_JS_URL)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return null
                    val js = response.body?.string() ?: return null

                    val hints = PlayerHints(DEFAULT_SIG_EXPR, DEFAULT_N_CLASS, DEFAULT_STS)
                    val program = PlayerProgram.Companion.compile(js, hints)
                    cachedProgram = program
                    return program
                }
            } catch (_: Exception) {
                return null
            }
        }
    }

    private fun parseQueryString(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    override suspend fun getTrackDetails(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
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
