package com.nova.music.feature.library.importing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.regex.Pattern

object PlaylistImportParser {

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun parseInput(input: String, okHttpClient: OkHttpClient): Result<ParsedPlaylist> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        when {
            trimmed.contains("spotify.com") || trimmed.startsWith("spotify:") -> parseSpotify(trimmed, okHttpClient)
            trimmed.contains("youtube.com/playlist") || trimmed.contains("music.youtube.com/playlist") -> parseYouTube(trimmed, okHttpClient)
            trimmed.startsWith("#EXTM3U") -> parseM3u(trimmed)
            trimmed.startsWith("{") || trimmed.startsWith("[") -> parseJson(trimmed)
            trimmed.contains(",") && (trimmed.contains("\n") || trimmed.contains("\r")) -> parseCsv(trimmed)
            else -> Result.failure(IllegalArgumentException("Unrecognized format. Paste a Spotify or YouTube playlist URL, or choose an M3U8, CSV, or JSON file."))
        }
    }

    suspend fun parseSpotify(urlOrUri: String, okHttpClient: OkHttpClient): Result<ParsedPlaylist> = withContext(Dispatchers.IO) {
        try {
            val (type, id) = extractSpotifyId(urlOrUri)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid Spotify URL or URI."))

            val embedUrl = "https://open.spotify.com/embed/$type/$id"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Failed to load Spotify playlist (HTTP ${response.code})."))
            }

            val html = response.body?.string() ?: ""
            if (html.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Received empty response from Spotify."))
            }

            val scriptMatch = Regex("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL).find(html)
                ?: return@withContext Result.failure(IllegalStateException("Could not extract playlist metadata from Spotify page."))

            val jsonText = scriptMatch.groupValues[1]
            val root = JSONObject(jsonText)
            val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
            val entity = pageProps?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
                ?: return@withContext Result.failure(IllegalStateException("Spotify playlist data structure not found."))

            val playlistTitle = entity.optString("name").ifEmpty { entity.optString("title", "Spotify Playlist") }
            val playlistDesc = entity.optString("subtitle", null)
            val coverSources = entity.optJSONObject("coverArt")?.optJSONArray("sources")
            val coverUrl = coverSources?.optJSONObject(0)?.optString("url")

            val trackList = entity.optJSONArray("trackList") ?: JSONArray()
            val rawTracks = mutableListOf<RawImportTrack>()

            for (i in 0 until trackList.length()) {
                val item = trackList.optJSONObject(i) ?: continue
                val title = item.optString("title").trim()
                if (title.isBlank()) continue

                val rawArtist = item.optString("subtitle", "").replace('\u00A0', ' ').trim()
                val durationMs = item.optLong("duration", 0L)
                val durationSec = durationMs / 1000L
                val uri = item.optString("uri")

                rawTracks.add(
                    RawImportTrack(
                        title = title,
                        artist = rawArtist,
                        durationSeconds = durationSec,
                        sourceTrackId = uri
                    )
                )
            }

            if (rawTracks.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No tracks found in Spotify playlist."))
            }

            Result.success(
                ParsedPlaylist(
                    title = playlistTitle,
                    description = playlistDesc,
                    coverUrl = coverUrl,
                    source = ImportSource.SPOTIFY,
                    tracks = rawTracks
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseYouTube(url: String, okHttpClient: OkHttpClient): Result<ParsedPlaylist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractYouTubePlaylistId(url)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid YouTube playlist URL."))

            val targetUrl = "https://www.youtube.com/playlist?list=$playlistId"
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Failed to load YouTube playlist (HTTP ${response.code})."))
            }

            val html = response.body?.string() ?: ""
            var playlistTitle = "YouTube Playlist"
            val titleMatch = Regex("""<title>(.*?)(?: - YouTube)?</title>""").find(html)
            if (titleMatch != null) {
                playlistTitle = titleMatch.groupValues[1].replace("- YouTube", "").trim()
            }

            // Extract video IDs and titles using regex matching from the page
            val videoPattern = Pattern.compile(""""videoId":"([a-zA-Z0-9_-]{11})"[^}]*?"title":\{"runs":\[\{"text":"([^"]+)"""")
            val matcher = videoPattern.matcher(html)
            val seenIds = mutableSetOf<String>()
            val rawTracks = mutableListOf<RawImportTrack>()

            while (matcher.find()) {
                val vid = matcher.group(1)
                val title = matcher.group(2)
                if (vid != null && title != null && !seenIds.contains(vid)) {
                    seenIds.add(vid)
                    // Split title if formatted as "Artist - Song"
                    val parts = title.split(" - ", limit = 2)
                    val (artist, songTitle) = if (parts.size == 2) {
                        parts[0].trim() to parts[1].trim()
                    } else {
                        "Unknown Artist" to title.trim()
                    }

                    rawTracks.add(
                        RawImportTrack(
                            title = songTitle,
                            artist = artist,
                            sourceTrackId = vid,
                            thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                        )
                    )
                }
            }

            if (rawTracks.isEmpty()) {
                // Fallback: check if we can at least parse from playlist items
                val altTitlePattern = Pattern.compile(""""title":\{"accessibility":\{"accessibilityData":\{"label":"([^"]+) by ([^"]+)""")
                val altMatcher = altTitlePattern.matcher(html)
                while (altMatcher.find()) {
                    val song = altMatcher.group(1)
                    val artist = altMatcher.group(2)
                    if (song != null && artist != null) {
                        rawTracks.add(RawImportTrack(title = song.trim(), artist = artist.trim()))
                    }
                }
            }

            if (rawTracks.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Could not extract tracks from YouTube playlist. It may be private or unavailable."))
            }

            Result.success(
                ParsedPlaylist(
                    title = playlistTitle,
                    source = ImportSource.YOUTUBE_MUSIC,
                    tracks = rawTracks
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseM3u(text: String): Result<ParsedPlaylist> {
        try {
            val reader = BufferedReader(StringReader(text))
            var playlistTitle = "Imported Playlist"
            val rawTracks = mutableListOf<RawImportTrack>()

            var line: String?
            var pendingDuration: Long = 0
            var pendingTitle: String? = null
            var pendingArtist: String? = null

            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.startsWith("#PLAYLIST:", ignoreCase = true)) {
                    playlistTitle = l.substringAfter("#PLAYLIST:").trim()
                } else if (l.startsWith("#EXTINF:", ignoreCase = true)) {
                    val content = l.substringAfter("#EXTINF:")
                    val parts = content.split(",", limit = 2)
                    pendingDuration = parts[0].trim().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    if (parts.size > 1) {
                        val trackInfo = parts[1].trim()
                        val trackParts = trackInfo.split(" - ", limit = 2)
                        if (trackParts.size == 2) {
                            pendingArtist = trackParts[0].trim()
                            pendingTitle = trackParts[1].trim()
                        } else {
                            pendingTitle = trackInfo
                            pendingArtist = "Unknown Artist"
                        }
                    }
                } else if (!l.startsWith("#") && l.isNotBlank()) {
                    // Track URI line
                    val title = pendingTitle ?: l.substringAfterLast("/").substringBeforeLast(".")
                    val artist = pendingArtist ?: "Unknown Artist"
                    rawTracks.add(
                        RawImportTrack(
                            title = title,
                            artist = artist,
                            durationSeconds = pendingDuration
                        )
                    )
                    pendingDuration = 0
                    pendingTitle = null
                    pendingArtist = null
                }
            }

            if (rawTracks.isEmpty()) {
                return Result.failure(IllegalStateException("No tracks found in M3U file."))
            }

            return Result.success(
                ParsedPlaylist(
                    title = playlistTitle,
                    source = ImportSource.M3U8,
                    tracks = rawTracks
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun parseCsv(text: String): Result<ParsedPlaylist> {
        try {
            val reader = BufferedReader(StringReader(text))
            var headerLine = reader.readLine() ?: return Result.failure(IllegalStateException("Empty CSV content."))
            val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

            val titleIndex = headers.indexOfFirst { it.contains("title") || it.contains("song") || it.contains("track") }
            val artistIndex = headers.indexOfFirst { it.contains("artist") || it.contains("creator") }
            val albumIndex = headers.indexOfFirst { it.contains("album") }
            val durationIndex = headers.indexOfFirst { it.contains("duration") || it.contains("length") || it.contains("time") }

            if (titleIndex == -1) {
                return Result.failure(IllegalStateException("Could not detect title column in CSV (must include 'Title', 'Song', or 'Track')."))
            }

            val rawTracks = mutableListOf<RawImportTrack>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isBlank()) continue
                val values = parseCsvLine(l)
                if (titleIndex in values.indices) {
                    val title = values[titleIndex].trim()
                    if (title.isBlank()) continue
                    val artist = if (artistIndex in values.indices) values[artistIndex].trim() else "Unknown Artist"
                    val album = if (albumIndex in values.indices) values[albumIndex].trim() else null
                    val durationSec = if (durationIndex in values.indices) {
                        values[durationIndex].trim().toLongOrNull() ?: 0L
                    } else 0L

                    rawTracks.add(
                        RawImportTrack(
                            title = title,
                            artist = if (artist.isNotBlank()) artist else "Unknown Artist",
                            album = album,
                            durationSeconds = durationSec
                        )
                    )
                }
            }

            if (rawTracks.isEmpty()) {
                return Result.failure(IllegalStateException("No valid track rows found in CSV."))
            }

            return Result.success(
                ParsedPlaylist(
                    title = "Imported Playlist",
                    source = ImportSource.CSV,
                    tracks = rawTracks
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun parseJson(text: String): Result<ParsedPlaylist> {
        try {
            val trimmed = text.trim()
            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                // Check if STEREO native backup
                if (root.optString("format") == "STEREO_PLAYLIST_BACKUP" || root.has("playlist")) {
                    val pObj = root.optJSONObject("playlist")
                    val title = pObj?.optString("name", "Imported Playlist") ?: "Imported Playlist"
                    val desc = pObj?.optString("description", null)
                    val cover = pObj?.optString("coverArtworkUrl", null)

                    val tracksArray = root.optJSONArray("tracks") ?: JSONArray()
                    val rawTracks = mutableListOf<RawImportTrack>()

                    for (i in 0 until tracksArray.length()) {
                        val t = tracksArray.optJSONObject(i) ?: continue
                        val songTitle = t.optString("title").trim()
                        if (songTitle.isBlank()) continue

                        rawTracks.add(
                            RawImportTrack(
                                title = songTitle,
                                artist = t.optString("artist", "Unknown Artist"),
                                album = t.optString("album", null),
                                durationSeconds = t.optLong("durationSeconds", 0L),
                                sourceTrackId = t.optString("id", null),
                                thumbnailUrl = t.optString("thumbnailUrl", null)
                            )
                        )
                    }

                    return Result.success(
                        ParsedPlaylist(
                            title = title,
                            description = desc,
                            coverUrl = cover,
                            source = ImportSource.STEREO_NATIVE,
                            tracks = rawTracks
                        )
                    )
                }
            }

            // Generic JSON array format
            val array = if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).optJSONArray("tracks")
                ?: return Result.failure(IllegalStateException("No tracks array found in JSON."))

            val rawTracks = mutableListOf<RawImportTrack>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val title = item.optString("title", item.optString("name")).trim()
                if (title.isBlank()) continue

                val artist = item.optString("artist", item.optString("subtitle", "Unknown Artist")).trim()
                val album = item.optString("album", null)
                val duration = item.optLong("durationSeconds", item.optLong("duration", 0L) / 1000L)

                rawTracks.add(
                    RawImportTrack(
                        title = title,
                        artist = artist,
                        album = album,
                        durationSeconds = duration
                    )
                )
            }

            if (rawTracks.isEmpty()) {
                return Result.failure(IllegalStateException("No valid tracks found in JSON."))
            }

            return Result.success(
                ParsedPlaylist(
                    title = "Imported Playlist",
                    source = ImportSource.JSON,
                    tracks = rawTracks
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun extractSpotifyId(input: String): Pair<String, String>? {
        val trimmed = input.trim()
        val playlistRegex = Regex("""spotify\.com/(?:[a-zA-Z0-9_-]+/)?playlist/([a-zA-Z0-9]+)""")
        playlistRegex.find(trimmed)?.let { return "playlist" to it.groupValues[1] }

        val albumRegex = Regex("""spotify\.com/(?:[a-zA-Z0-9_-]+/)?album/([a-zA-Z0-9]+)""")
        albumRegex.find(trimmed)?.let { return "album" to it.groupValues[1] }

        val uriRegex = Regex("""spotify:(playlist|album):([a-zA-Z0-9]+)""")
        uriRegex.find(trimmed)?.let { return it.groupValues[1] to it.groupValues[2] }

        if (trimmed.matches(Regex("""^[a-zA-Z0-9]{22}$"""))) {
            return "playlist" to trimmed
        }
        return null
    }

    private fun extractYouTubePlaylistId(input: String): String? {
        val regex = Regex("""[?&]list=([a-zA-Z0-9_-]+)""")
        return regex.find(input.trim())?.groupValues?.get(1)
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }
}
