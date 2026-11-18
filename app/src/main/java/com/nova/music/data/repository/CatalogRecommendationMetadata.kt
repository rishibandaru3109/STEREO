package com.nova.music.data.repository

import android.content.Context
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.LyricsLanguage
import com.nova.music.domain.recommendation.RecommendationMetadata
import com.nova.music.domain.recommendation.recordingName
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs

class CatalogRecommendationMetadata(
    context: Context,
    client: OkHttpClient
) : RecommendationMetadata {
    private val http = client.newBuilder().apply {
        interceptors().removeAll { it is HttpLoggingInterceptor }
    }.callTimeout(2_500L, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build()
    private val cache = context.getSharedPreferences("stereo_lyrics_language_v1", Context.MODE_PRIVATE)
    private val lyricsSlots = Semaphore(4)
    private val lyricsRateLock = Mutex()
    private val artistLock = Mutex()
    private var lastLyricsRequestAt = 0L
    private var lastMusicBrainzRequestAt = 0L
    private val retryAfter = ConcurrentHashMap<String, Long>()
    private val artistCache = object : LinkedHashMap<String, Pair<Long, List<String>>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, List<String>>>): Boolean =
            size > 32
    }

    override suspend fun identifyLanguages(tracks: List<Track>): List<Track> = coroutineScope {
        val now = System.currentTimeMillis()
        val cached = tracks.map { track ->
            val value = cache.getString(track.id, null)?.split('|', limit = 2)
            val label = value?.getOrNull(1)?.takeIf {
                (value.first().toLongOrNull() ?: 0L) > now
            }
            track to label
        }
        val pending = cached.filter { (track, label) ->
            label == null && track.lyricsLanguage == null && track.durationSeconds in 1L..3600L && track.artist.isNotBlank()
        }.take(18)
        val labels = pending.map { (track, _) ->
            async { track.id to lyricsSlots.withPermit { fetchLanguage(track) } }
        }.awaitAll().mapNotNull { (id, label) -> label?.let { id to it } }.toMap()

        if (labels.isNotEmpty()) {
            val editor = cache.edit()
            val oldEntries = cache.all.entries.sortedBy { it.value.toString().substringBefore('|') }
            oldEntries.take((oldEntries.size + labels.size - 800).coerceAtLeast(0))
                .forEach { editor.remove(it.key) }
            labels.forEach { (id, label) ->
                val ttl = if (label == UNKNOWN) 24 * 3_600_000L else 30 * 24 * 3_600_000L
                editor.putString(id, "${now + ttl}|$label")
            }
            editor.apply()
        }
        cached.map { (track, label) ->
            val language = (label ?: labels[track.id])?.takeUnless { it == UNKNOWN }
            track.copy(lyricsLanguage = track.lyricsLanguage ?: language)
        }
    }

    private suspend fun fetchLanguage(track: Track): String? {
        val url = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", track.title)
            .addQueryParameter("artist_name", track.artist)
            .addQueryParameter("duration", track.durationSeconds.toString())
        track.album?.takeIf { it.isNotBlank() }?.let { url.addQueryParameter("album_name", it) }
        lyricsRateLock.withLock {
            delay((lastLyricsRequestAt + 200L - System.currentTimeMillis()).coerceAtLeast(0))
            lastLyricsRequestAt = System.currentTimeMillis()
        }
        val result = get(url.build()) ?: return null
        if (result.first == 404) return UNKNOWN
        val body = parse(result)
        if (body == null) return null
        val duration = body.optDouble("duration")
        if (recordingName(body.optString("trackName")) != recordingName(track.title) ||
            recordingName(body.optString("artistName")) != recordingName(track.artist) ||
            !duration.isFinite() || abs(duration - track.durationSeconds) > 2
        ) return UNKNOWN
        return LyricsLanguage.identify(body.optString("plainLyrics", "")) ?: UNKNOWN
    }

    override suspend fun similarArtists(artist: String): List<String> = artistLock.withLock {
        val key = recordingName(artist)
        val now = System.currentTimeMillis()
        artistCache[key]?.takeIf { it.first > now }?.let { return@withLock it.second }

        delay((lastMusicBrainzRequestAt + 1_100L - System.currentTimeMillis()).coerceAtLeast(0))
        lastMusicBrainzRequestAt = System.currentTimeMillis()
        // Escape Lucene query syntax within an exact phrase.
        val phrase = artist.replace("\\", "\\\\").replace("\"", "\\\"")
        val lookup = "https://musicbrainz.org/ws/2/artist".toHttpUrl().newBuilder()
            .addQueryParameter("query", "artist:\"$phrase\"")
            .addQueryParameter("fmt", "json")
            .addQueryParameter("limit", "5").build()
        val matches = parse(get(lookup))?.optJSONArray("artists") ?: return@withLock emptyList()
        val exact = (0 until matches.length()).mapNotNull { matches.optJSONObject(it) }
            .filter { recordingName(it.optString("name")) == key }
        val mbid = exact.singleOrNull()?.optString("id").orEmpty()
        if (!MBID.matches(mbid)) {
            artistCache[key] = (now + 3_600_000L) to emptyList()
            return@withLock emptyList()
        }

        val radio = "https://api.listenbrainz.org/1/lb-radio/artist/$mbid".toHttpUrl().newBuilder()
            .addQueryParameter("mode", "medium")
            .addQueryParameter("max_similar_artists", "5")
            .addQueryParameter("max_recordings_per_artist", "1").build()
        val payload = parse(get(radio)) ?: return@withLock emptyList()
        val names = payload.keys().asSequence().filter { it != mbid }.mapNotNull { id ->
            val entry = payload.optJSONArray(id)?.optJSONObject(0) ?: return@mapNotNull null
            entry.optString("similar_artist_name").takeIf { it.isNotBlank() }
                ?.let { it to entry.optLong("total_listen_count") }
        }.sortedByDescending { it.second }.map { it.first }.distinct().toList()
        artistCache[key] = (now + 24 * 3_600_000L) to names
        names
    }

    private fun parse(result: Pair<Int, String>?): JSONObject? =
        result?.takeIf { it.first == 200 }?.let { runCatching { JSONObject(it.second) }.getOrNull() }

    private suspend fun get(url: HttpUrl): Pair<Int, String>? {
        if ((retryAfter[url.host] ?: 0L) > System.currentTimeMillis()) return null
        val request = Request.Builder().url(url)
            .header("User-Agent", "STEREO/1.0.0-alpha.1 (https://github.com/rishibandaru3109)")
            .header("Accept", "application/json").build()
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(null)
                }
                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        if (it.code == 429 || it.code == 503 ||
                            it.header("X-RateLimit-Remaining") == "0"
                        ) {
                            val headers = listOfNotNull(it.header("Retry-After")?.toLongOrNull(),
                                it.header("X-RateLimit-Reset-In")?.toLongOrNull())
                            retryAfter[url.host] = System.currentTimeMillis() +
                                (headers.maxOrNull() ?: 60L).coerceIn(1L, 3600L) * 1000L
                        }
                        try {
                            it.code to it.body?.string().orEmpty()
                        } catch (_: IOException) {
                            null
                        }
                    }
                    continuation.resume(result)
                }
            })
        }
    }

    private companion object {
        const val UNKNOWN = "?"
        val MBID = Regex("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    }
}
