package com.nova.music.data.extractor

import android.util.Log
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.StreamExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.models.YouTubeClient
import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI

/**
 * InnerTubeX implementation of [MusicExtractor].
 * Reuses Metrolist's multi-client catalog fallback and QuackJS AST cipher solver.
 */
class InnerTubeXExtractor(
    private val okHttpClient: OkHttpClient,
    private val fallbackExtractor: MusicExtractor = YouTubeMusicExtractor(okHttpClient)
) : MusicExtractor {

    private val ktorClient: HttpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        followRedirects = false
    }

    private val innerTube: InnerTube = InnerTube(httpClient = ktorClient)
    private val cipherService: YouTubeCipherService = YouTubeCipherService(httpClient = ktorClient)
    private val configParser: YtConfigParserImpl = YtConfigParserImpl(
        httpClient = ktorClient,
        innerTube = innerTube,
        cipherService = cipherService
    )
    private val streamExtractor: StreamExtractor = InnerTubeExtractor(
        configParser = configParser,
        cipherService = cipherService,
        innerTube = innerTube
    )

    companion object {
        private const val TAG = "STEREO_EXTRACTOR"

        private fun logI(msg: String) {
            try {
                Log.i(TAG, msg)
            } catch (_: Throwable) {
                println("[$TAG] $msg")
            }
        }

        private fun logE(msg: String, tr: Throwable? = null) {
            try {
                if (tr != null) Log.e(TAG, msg, tr) else Log.e(TAG, msg)
            } catch (_: Throwable) {
                System.err.println("[$TAG] $msg ${tr?.message ?: ""}")
            }
        }
    }

    override suspend fun search(query: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        val primaryResult = runCatching {
            logI("Searching music via InnerTubeX: query='$query'")
            val startTime = System.currentTimeMillis()
            val response = innerTube.search(
                client = YouTubeClient.WEB_REMIX,
                query = query
            )
            val jsonStr = response.bodyAsText()
            val tracks = parseMusicSearchResponse(jsonStr)
            val elapsed = System.currentTimeMillis() - startTime
            logI("Search completed in ${elapsed}ms: found ${tracks.size} tracks")
            tracks
        }

        if (primaryResult.isSuccess && !primaryResult.getOrNull().isNullOrEmpty()) {
            primaryResult
        } else {
            logI("InnerTubeX search yielded no results or failed (${primaryResult.exceptionOrNull()?.message}). Falling back to YouTubeMusicExtractor.")
            fallbackExtractor.search(query)
        }
    }

    override suspend fun resolveAudioStream(trackId: String): Result<AudioStream> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        logI("Resolving audio stream via InnerTubeX for track: $trackId")

        val extracted = try {
            streamExtractor.extract(
                videoId = trackId,
                hints = ContentHints(),
                audioQuality = AudioQuality.AUTO
            )
        } catch (e: Exception) {
            logE("InnerTubeX extraction error: ${e.message}", e)
            null
        }

        val rawUrl = extracted?.audioUrl ?: ""
        val isValidDirectUrl = (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) && rawUrl.contains("googlevideo.com")
        val isVerified = if (extracted != null && isValidDirectUrl) {
            verifyStreamPlayable(rawUrl, extracted.headers)
        } else false

        if (extracted != null && isValidDirectUrl && isVerified) {
            val elapsed = System.currentTimeMillis() - startTime
            val host = runCatching { URI(rawUrl).host }.getOrDefault("unknown")
            logI("InnerTubeX stream resolved and verified in ${elapsed}ms: host=$host, itag=${extracted.itag}, client=${extracted.clientName}, mime=${extracted.mimeType}")

            Result.success(
                AudioStream(
                    url = rawUrl,
                    mimeType = extracted.mimeType ?: "audio/webm",
                    codec = extracted.codecs ?: "opus",
                    bitrate = extracted.bitrate ?: 160000,
                    formatId = extracted.itag.toString(),
                    contentLength = extracted.contentLengthBytes ?: 0L,
                    isAudioOnly = true,
                    headers = extracted.headers
                )
            )
        } else {
            val elapsed = System.currentTimeMillis() - startTime
            logI("InnerTubeX returned unverified or non-playable URL in ${elapsed}ms. Seamlessly falling back to YouTubeMusicExtractor for track: $trackId")
            val fallbackResult = fallbackExtractor.resolveAudioStream(trackId)
            if (fallbackResult.isSuccess) {
                val totalElapsed = System.currentTimeMillis() - startTime
                logI("Fallback YouTubeMusicExtractor resolved stream in ${totalElapsed}ms for track: $trackId")
            } else {
                logE("Fallback resolution failed for track '$trackId': ${fallbackResult.exceptionOrNull()?.message}", fallbackResult.exceptionOrNull())
            }
            fallbackResult
        }
    }

    private fun verifyStreamPlayable(url: String, headers: Map<String, String>): Boolean {
        return try {
            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=0-1024")
            headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                response.isSuccessful || response.code == 206
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getTrackDetails(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
        val primary = runCatching {
            logI("Fetching track details via InnerTubeX: $trackId")
            val response = innerTube.player(
                client = YouTubeClient.WEB_REMIX,
                videoId = trackId
            )
            val jsonStr = response.bodyAsText()
            parsePlayerDetails(trackId, jsonStr)
        }
        if (primary.isSuccess) {
            primary
        } else {
            logI("InnerTubeX track details failed (${primary.exceptionOrNull()?.message}). Falling back to YouTubeMusicExtractor.")
            fallbackExtractor.getTrackDetails(trackId)
        }
    }

    internal fun parsePlayerDetails(trackId: String, jsonStr: String): Track {
        val root = JSONObject(jsonStr)
        val details = root.optJSONObject("videoDetails") ?: error("No videoDetails in player response")
        val title = details.optString("title", "Unknown Title")
        val author = details.optString("author", "Unknown Artist")
        val duration = details.optLong("lengthSeconds", 0L)
        val thumb = details.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbs ->
                if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null
            } ?: "https://i.ytimg.com/vi/$trackId/hqdefault.jpg"

        return Track(
            id = trackId,
            title = title,
            artist = author,
            durationSeconds = duration,
            thumbnailUrl = thumb
        )
    }

    override suspend fun getSongRadio(trackId: String): Result<List<Track>> = withContext(Dispatchers.IO) {
        runCatching {
            logI("Fetching song radio via InnerTubeX for seed track: $trackId")
            val startTime = System.currentTimeMillis()
            val response = innerTube.next(
                client = YouTubeClient.WEB_REMIX,
                videoId = trackId
            )
            val jsonStr = response.bodyAsText()
            val tracks = parseSongRadioResponse(trackId, jsonStr)
            val elapsed = System.currentTimeMillis() - startTime
            logI("Song radio completed in ${elapsed}ms: found ${tracks.size} tracks for seed $trackId")
            tracks
        }
    }

    override suspend fun getHomeShelves(): Result<List<Track>> = withContext(Dispatchers.IO) {
        runCatching {
            logI("Fetching home shelves via InnerTubeX (FEmusic_home)")
            val startTime = System.currentTimeMillis()
            val response = innerTube.browse(
                client = YouTubeClient.WEB_REMIX,
                browseId = "FEmusic_home"
            )
            val jsonStr = response.bodyAsText()
            val tracks = parseBrowseHomeResponse(jsonStr)
            val elapsed = System.currentTimeMillis() - startTime
            logI("Home shelves completed in ${elapsed}ms: found ${tracks.size} tracks")
            tracks
        }
    }

    internal fun parseSongRadioResponse(seedTrackId: String, jsonStr: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)
            val rootKeys = root.keys().asSequence().toList()
            logI("next response root keys: $rootKeys")
            val contents = root.optJSONObject("contents")
            val contentsKeys = contents?.keys()?.asSequence()?.toList() ?: emptyList()
            logI("next response contents keys: $contentsKeys")

            // 1. YouTube Music Web Remix queue
            val queueRenderer = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicQueueRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")
                ?.optJSONArray("contents")

            if (queueRenderer != null) {
                for (i in 0 until queueRenderer.length()) {
                    val item = queueRenderer.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer") ?: continue
                    val videoId = item.optString("videoId")
                    if (videoId.isEmpty() || videoId == seedTrackId) continue

                    val title = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                    val artist = item.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
                    val lengthText = item.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                    val duration = parseDurationText(lengthText)
                    val thumb = item.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        ?.let { thumbs -> if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null }
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    tracks.add(
                        Track(
                            id = videoId,
                            title = title,
                            artist = artist,
                            durationSeconds = duration,
                            thumbnailUrl = thumb
                        )
                    )
                }
            }

            // 2. Fallback to standard secondaryResults if queue was empty
            if (tracks.isEmpty()) {
                val secondaryResults = root.optJSONObject("contents")
                    ?.optJSONObject("twoColumnWatchNextResults")
                    ?.optJSONObject("secondaryResults")
                    ?.optJSONObject("secondaryResults")
                    ?.optJSONArray("results")

                if (secondaryResults != null) {
                    for (i in 0 until secondaryResults.length()) {
                        val item = secondaryResults.optJSONObject(i)?.optJSONObject("compactVideoRenderer") ?: continue
                        val videoId = item.optString("videoId")
                        if (videoId.isEmpty() || videoId == seedTrackId) continue

                        val title = item.optJSONObject("title")?.optString("simpleText")
                            ?: item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                        val artist = item.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
                        val lengthText = item.optJSONObject("lengthText")?.optString("simpleText") ?: ""
                        val duration = parseDurationText(lengthText)
                        val thumb = item.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            ?.let { thumbs -> if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null }
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                        tracks.add(
                            Track(
                                id = videoId,
                                title = title,
                                artist = artist,
                                durationSeconds = duration,
                                thumbnailUrl = thumb
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logE("Error parsing song radio response: ${e.message}", e)
        }
        return tracks
    }

    internal fun parseBrowseHomeResponse(jsonStr: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)
            val sectionList = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until sectionList.length()) {
                val sec = sectionList.optJSONObject(i) ?: continue
                val carousel = sec.optJSONObject("musicCarouselShelfRenderer")
                    ?: sec.optJSONObject("musicShelfRenderer") ?: continue
                val contents = carousel.optJSONArray("contents") ?: continue

                for (j in 0 until contents.length()) {
                    val item = contents.optJSONObject(j) ?: continue

                    item.optJSONObject("musicResponsiveListItemRenderer")?.let { responsiveItem ->
                        parseMusicResponsiveItem(responsiveItem)?.let { tracks.add(it) }
                    }

                    item.optJSONObject("musicTwoRowItemRenderer")?.let { twoRowItem ->
                        val title = twoRowItem.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                        val videoId = twoRowItem.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                            ?: twoRowItem.optJSONObject("onTap")?.optJSONObject("watchEndpoint")?.optString("videoId")
                        if (!title.isNullOrEmpty() && !videoId.isNullOrEmpty()) {
                            val artist = twoRowItem.optJSONObject("subtitle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: "Unknown Artist"
                            val thumb = twoRowItem.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                ?.let { thumbs -> if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null }
                                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                            tracks.add(
                                Track(
                                    id = videoId,
                                    title = title,
                                    artist = artist,
                                    thumbnailUrl = thumb
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logE("Error parsing browse home response: ${e.message}", e)
        }
        return tracks
    }

    internal fun parseMusicSearchResponse(jsonStr: String): List<Track> {
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

                // 2. itemSectionRenderer (categorized item sections)
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
        } catch (e: Exception) {
            logE("Error parsing search response: ${e.message}", e)
        }
        return tracks
    }

    private fun parseMusicResponsiveItem(item: JSONObject): Track? {
        try {
            val flexColumns = item.optJSONArray("flexColumns") ?: return null

            // Title
            val titleCol = flexColumns.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
            val title = titleCol?.optJSONObject(0)?.optString("text") ?: return null

            // Video ID
            val videoId = titleCol.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                ?: item.optJSONObject("playlistItemData")
                    ?.optString("videoId")
                    ?.ifEmpty { null }
                ?: item.optJSONObject("doubleTapCommand")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                ?: item.optJSONObject("playNavigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                ?: item.optJSONObject("overlay")
                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                ?: return null

            // Artist, Album, Duration
            var isSpokenWord = false
            var artist = "Unknown Artist"
            var album: String? = null
            var durationSeconds = 0L

            val subtitleCol = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            if (subtitleCol != null) {
                val textParts = mutableListOf<String>()
                for (k in 0 until subtitleCol.length()) {
                    val runText = subtitleCol.optJSONObject(k)?.optString("text") ?: ""
                    if (runText.isNotEmpty() && runText != " • ") {
                        textParts.add(runText)
                    }
                }

                val mediaLabel = textParts.firstOrNull()
                isSpokenWord = mediaLabel in setOf("Episode", "Podcast")
                if (mediaLabel in setOf("Song", "Video", "Episode", "Podcast")) {
                    textParts.removeAt(0)
                }
                if (textParts.isNotEmpty()) {
                    artist = textParts[0]
                }
                if (textParts.size > 2) {
                    album = textParts[1]
                    durationSeconds = parseDurationText(textParts.last())
                } else if (textParts.size == 2) {
                    val parsedDur = parseDurationText(textParts[1])
                    if (parsedDur > 0) {
                        durationSeconds = parsedDur
                    } else {
                        album = textParts[1]
                    }
                }
            }

            // Thumbnail
            val thumbUrl = item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.let { thumbs ->
                    if (thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null
                } ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            return Track(
                id = videoId,
                title = title,
                artist = artist,
                album = album,
                durationSeconds = durationSeconds,
                thumbnailUrl = thumbUrl,
                isSpokenWord = isSpokenWord
            )
        } catch (_: Exception) {
            return null
        }
    }

    internal fun parseDurationText(text: String): Long {
        val parts = text.trim().split(":")
        if (parts.size == 2) {
            val min = parts[0].toLongOrNull() ?: 0L
            val sec = parts[1].toLongOrNull() ?: 0L
            return min * 60 + sec
        } else if (parts.size == 3) {
            val hr = parts[0].toLongOrNull() ?: 0L
            val min = parts[1].toLongOrNull() ?: 0L
            val sec = parts[2].toLongOrNull() ?: 0L
            return hr * 3600 + min * 60 + sec
        }
        return 0L
    }
}
