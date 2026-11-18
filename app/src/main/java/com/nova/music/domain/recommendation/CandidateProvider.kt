package com.nova.music.domain.recommendation

import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.domain.repository.RecommendationPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

interface CandidateProvider {
    suspend fun getCandidates(profile: ListeningProfile, excludedTrackIds: Set<String>): List<Track>

    suspend fun getCandidates(
        profile: ListeningProfile,
        excludedTrackIds: Set<String>,
        preferences: RecommendationPreferences
    ): List<Track> = getCandidates(profile, excludedTrackIds)
}

interface RecommendationMetadata {
    suspend fun identifyLanguages(tracks: List<Track>): List<Track>
    suspend fun similarArtists(artist: String): List<String>
}

class CatalogCandidateProvider(
    private val extractor: MusicExtractor,
    private val preferencesRepository: MusicPreferencesRepository? = null,
    private val metadata: RecommendationMetadata? = null
) : CandidateProvider {

    override suspend fun getCandidates(profile: ListeningProfile, excludedTrackIds: Set<String>): List<Track> =
        getCandidates(
            profile, excludedTrackIds,
            preferencesRepository?.recommendationPreferences?.first() ?: RecommendationPreferences()
        )

    override suspend fun getCandidates(
        profile: ListeningProfile,
        excludedTrackIds: Set<String>,
        preferences: RecommendationPreferences
    ): List<Track> = coroutineScope {
        val negativelyRated = profile.topTracks.filter { it.score < 0 }.map { it.trackId }.toSet()
        val blocked = excludedTrackIds + negativelyRated
        val affinity = profile.topTracks.filter { it.score > 0 }
        val topArtists = profile.topArtists.filter { it.score > 0 }
        val seedIds = (affinity.filter { it.isLiked }.take(1).map { it.trackId } +
            affinity.map { it.trackId } + profile.likedTrackIds.sorted() + profile.recentTracks.map { it.id })
            .distinct().filterNot { it in blocked }.take(3)

        val related = seedIds.map { id -> async { source { extractor.getSongRadio(id).getOrThrow() } } }
            .awaitAll()
        val interleaved = (0 until (related.maxOfOrNull { it.size } ?: 0)).flatMap { index ->
            related.mapNotNull { it.getOrNull(index) }
        }
        val radio = distinctRecordings(interleaved)
            .filter { it.id !in blocked && it.id !in seedIds && ContentFilter.isMusicTrack(it) }
        val adventurous = preferences.discoveryMode.equals("Adventurous", ignoreCase = true)
        val needDiscovery = radio.size < 22 || adventurous || preferences.languages.isNotEmpty()
        val supplement = if (needDiscovery) {
            val genre = preferences.genres.sorted().firstOrNull()
            val queries = preferences.languages.sorted().take(2).map { language ->
                listOfNotNull(language, genre, "songs").joinToString(" ")
            }.ifEmpty {
                when {
                    topArtists.isNotEmpty() -> listOf(topArtists.first().artistName + " songs")
                    genre != null -> listOf("$genre songs")
                    else -> emptyList()
                }
            }
            val searches = queries.map { query -> async { source { extractor.search(query).getOrThrow() } } }
            val similar = async {
                val artist = topArtists.firstOrNull()?.artistName
                if (artist == null || metadata == null || (radio.size >= 22 && !adventurous)) {
                    emptyList()
                } else {
                    source {
                        val name = metadata.similarArtists(artist).firstOrNull()
                        if (name == null) emptyList() else extractor.search("$name songs").getOrThrow()
                    }
                }
            }
            val home = async {
                if (preferences.languages.isEmpty() && radio.size < 12) {
                    source { extractor.getHomeShelves().getOrThrow() }
                } else emptyList()
            }
            searches.awaitAll().flatten() + similar.await() + home.await()
        } else emptyList()

        val preferred = distinctRecordings(supplement)
            .filter { it.id !in blocked && it.id !in seedIds && ContentFilter.isMusicTrack(it) }
        val local = profile.recentTracks + profile.likedTracks
        val shortlisted = if (preferences.languages.isEmpty()) {
            distinctRecordings(radio + preferred + local)
        } else {
            val pool = distinctRecordings(radio.take(16) + preferred.take(16) + local.take(8))
                .filter { it.id !in blocked && ContentFilter.isMusicTrack(it) }
            metadata?.identifyLanguages(pool) ?: pool
        }
        val ranked = shortlisted.filter { it.id !in blocked && RecommendationEligibility.accepts(it, preferences) }
        rank(ranked, radio.map { it.id }.toSet(), profile, adventurous)
    }

    private fun rank(
        tracks: List<Track>,
        radioIds: Set<String>,
        profile: ListeningProfile,
        adventurous: Boolean
    ): List<Track> {
        val artistScores = profile.topArtists.associate { recordingName(it.artistName) to it.score }
        val known = profile.recentTracks.map { it.id }.toSet() + profile.likedTrackIds
        val preferred = tracks.withIndex().sortedByDescending { (index, track) ->
            val affinity = artistScores[recordingName(track.artist)]?.coerceIn(0.0, 80.0) ?: 0.0
            (if (track.id in radioIds) 32.0 else 0.0) +
                affinity / (if (adventurous) 8.0 else 3.0) -
                (if (track.id in known) { if (adventurous) 20.0 else 7.0 } else 0.0) -
                index * 0.24
        }.map { it.value }
        // Keep the head of the list varied while preserving lower-ranked options for artist mixes.
        val primary = preferred.groupBy { recordingName(it.artist) }.values.flatMap { it.take(4) }.toSet()
        return preferred.filter { it in primary } + preferred.filterNot { it in primary }
    }

    private suspend fun <T> source(load: suspend () -> List<T>): List<T> = try {
        withTimeoutOrNull(5_000L) { load() }.orEmpty()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }
}
