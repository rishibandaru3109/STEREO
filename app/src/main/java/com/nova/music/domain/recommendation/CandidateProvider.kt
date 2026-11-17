package com.nova.music.domain.recommendation

import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

interface CandidateProvider {
    suspend fun getCandidates(profile: ListeningProfile, excludedTrackIds: Set<String>): List<Track>
}

class CatalogCandidateProvider(
    private val extractor: MusicExtractor
) : CandidateProvider {

    private val coldStartQueries = listOf(
        "Top hits music",
        "Popular songs 2024",
        "Chill acoustic vibes",
        "Electronic dance essentials"
    )

    override suspend fun getCandidates(
        profile: ListeningProfile,
        excludedTrackIds: Set<String>
    ): List<Track> = coroutineScope {
        val searchQueries = mutableListOf<String>()

        if (profile.topArtists.isNotEmpty()) {
            // Pick top 3 artists to query candidates
            val topArtistNames = profile.topArtists.take(3).map { it.artistName }
            for (artist in topArtistNames) {
                searchQueries.add("$artist songs")
                searchQueries.add("$artist hits")
            }
        }

        // If cold start or few artists, add general discovery queries
        if (searchQueries.size < 4) {
            val needed = 4 - searchQueries.size
            searchQueries.addAll(coldStartQueries.take(needed))
        }

        // Fan out parallel search requests
        val searchDeferreds = searchQueries.map { query ->
            async {
                try {
                    extractor.search(query).getOrDefault(emptyList())
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        val allResults = searchDeferreds.awaitAll().flatten()

        // Filter excluded tracks (dislikes/not interested) and deduplicate
        allResults
            .filter { track -> !excludedTrackIds.contains(track.id) }
            .distinctBy { it.id }
    }
}
