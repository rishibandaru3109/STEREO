package com.nova.music.domain.recommendation

import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicPreferencesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

interface CandidateProvider {
    suspend fun getCandidates(profile: ListeningProfile, excludedTrackIds: Set<String>): List<Track>
}

class CatalogCandidateProvider(
    private val extractor: MusicExtractor,
    private val preferencesRepository: MusicPreferencesRepository? = null
) : CandidateProvider {

    private val defaultDiscoveryQueries = listOf(
        "Top hits music",
        "New trending music 2024",
        "Chill acoustic vibes",
        "Electronic dance essentials",
        "Indie alternative favorites"
    )

    override suspend fun getCandidates(
        profile: ListeningProfile,
        excludedTrackIds: Set<String>
    ): List<Track> = coroutineScope {
        val searchQueries = mutableListOf<String>()

        // 1. Incorporate User's Preferred Genres and Languages from Onboarding/Settings
        val userGenres = preferencesRepository?.preferredGenres?.first() ?: emptySet()
        val userLanguages = preferencesRepository?.preferredLanguages?.first() ?: emptySet()

        for (genre in userGenres.take(3)) {
            searchQueries.add("$genre music hits")
        }
        for (lang in userLanguages.take(2)) {
            searchQueries.add("$lang top songs")
        }

        // 2. Incorporate Top Artists from Listening Profile
        if (profile.topArtists.isNotEmpty()) {
            val topArtistNames = profile.topArtists.take(3).map { it.artistName }
            for (artist in topArtistNames) {
                searchQueries.add("$artist songs")
                searchQueries.add("tracks similar to $artist")
            }
        }

        // 3. Fallback or Supplementary Discovery Seeds
        if (searchQueries.size < 4) {
            val needed = 4 - searchQueries.size
            searchQueries.addAll(defaultDiscoveryQueries.take(needed))
        }

        // Parallelize candidate searches (capped at 6 to prevent network saturation)
        val queriesToRun = searchQueries.distinct().take(6)
        val searchDeferreds = queriesToRun.map { query ->
            async {
                try {
                    extractor.search(query).getOrDefault(emptyList())
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        val allResults = searchDeferreds.awaitAll().flatten()

        // Filter excluded tracks and deduplicate candidate pool
        allResults
            .filter { track -> !excludedTrackIds.contains(track.id) }
            .distinctBy { it.id }
    }
}
