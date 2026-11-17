package com.nova.music.domain.recommendation

import android.util.Log
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.ContentFilterMode
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

    companion object {
        private const val TAG = "STEREO_REC_PROVENANCE"

        private fun logProvenance(msg: String) {
            try {
                Log.i(TAG, msg)
            } catch (_: Throwable) {
                println("[$TAG] $msg")
            }
        }
    }

    private val defaultColdStartQueries = listOf(
        "Top hits music",
        "Trending music essentials",
        "Chill acoustic vibes"
    )

    override suspend fun getCandidates(
        profile: ListeningProfile,
        excludedTrackIds: Set<String>
    ): List<Track> = coroutineScope {
        val allHarvestedCandidates = mutableListOf<Track>()

        // =====================================================================
        // 1. PRIORITIZE SONG-RADIO SEEDED FROM HIGH-AFFINITY USER TRACKS
        // Priority order: 1) Liked tracks, 2) Repeatedly played/completed tracks, 3) Recent tracks
        // =====================================================================
        val seedCandidates = mutableListOf<Pair<String, String>>() // Pair(trackId, seedReason)

        // A. Liked Tracks
        for (likedId in profile.likedTrackIds.take(2)) {
            seedCandidates.add(likedId to "Liked Track")
        }

        // B. Repeatedly Played / High-Affinity Tracks
        val highAffinityTracks = profile.topTracks
            .filter { it.playCount >= 2 || it.score >= 5.0 }
            .sortedByDescending { it.score }
            .map { it.trackId }
            .filter { id -> !seedCandidates.any { it.first == id } }

        for (topId in highAffinityTracks.take(2)) {
            seedCandidates.add(topId to "Repeatedly Played Track")
        }

        // C. Explicitly Selected / Recent Tracks
        val recentIds = profile.recentTracks
            .map { it.id }
            .filter { id -> !seedCandidates.any { it.first == id } }

        for (recentId in recentIds.take(1)) {
            seedCandidates.add(recentId to "Recent Track")
        }

        val seedDeferreds = seedCandidates.take(3).map { (seedId, seedReason) ->
            async {
                val seedName = profile.recentTracks.find { it.id == seedId }?.title
                    ?: profile.topTracks.find { it.trackId == seedId }?.title
                    ?: seedId
                val startTime = System.currentTimeMillis()
                val result = try {
                    extractor.getSongRadio(seedId).getOrDefault(emptyList())
                } catch (e: Exception) {
                    logProvenance("Song radio failed for seed '$seedName' ($seedId): ${e.message}")
                    emptyList()
                }
                val elapsed = System.currentTimeMillis() - startTime
                logProvenance("Seed [$seedReason]: '$seedName' ($seedId) -> Endpoint: innerTube.next -> Fetched ${result.size} candidates in ${elapsed}ms")
                result
            }
        }

        val radioCandidates = seedDeferreds.awaitAll().flatten()
        allHarvestedCandidates.addAll(radioCandidates)

        // =====================================================================
        // 2. FALLBACK / COLD START / SUPPLEMENTARY DISCOVERY
        // Note: Unauthenticated FEmusic_home is treated as potentially generic
        // and used only when seeds are scarce or for cold-start onboarding.
        // =====================================================================
        val isColdStart = profile.isColdStart || allHarvestedCandidates.size < 15

        if (isColdStart) {
            logProvenance("Seed pool scarce (${allHarvestedCandidates.size} tracks) or cold start; engaging supplementary discovery")

            // A. Fetch YouTube Music Explore/Home Shelves (Marked as generic discovery)
            val homeDeferred = async {
                val startTime = System.currentTimeMillis()
                val homeTracks = try {
                    extractor.getHomeShelves().getOrDefault(emptyList())
                } catch (e: Exception) {
                    logProvenance("FEmusic_home fetch failed: ${e.message}")
                    emptyList()
                }
                val elapsed = System.currentTimeMillis() - startTime
                logProvenance("Discovery: unauthenticated FEmusic_home -> Endpoint: innerTube.browse -> Fetched ${homeTracks.size} generic discovery candidates in ${elapsed}ms")
                homeTracks
            }

            // B. User Preferences (Selected genres and languages from onboarding)
            val userGenres = preferencesRepository?.preferredGenres?.first() ?: emptySet()
            val userLanguages = preferencesRepository?.preferredLanguages?.first() ?: emptySet()
            val queryList = mutableListOf<String>()

            for (genre in userGenres.take(2)) {
                queryList.add("$genre music")
            }
            for (lang in userLanguages.take(1)) {
                queryList.add("$lang top songs")
            }
            if (queryList.isEmpty()) {
                queryList.addAll(defaultColdStartQueries.take(2))
            }

            val queryDeferreds = queryList.distinct().take(3).map { query ->
                async {
                    val startTime = System.currentTimeMillis()
                    val searchTracks = try {
                        extractor.search(query).getOrDefault(emptyList())
                    } catch (e: Exception) {
                        logProvenance("Search query '$query' failed: ${e.message}")
                        emptyList()
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    logProvenance("Preference Query: '$query' -> Endpoint: innerTube.search -> Fetched ${searchTracks.size} candidates in ${elapsed}ms")
                    searchTracks
                }
            }

            val discoveryCandidates = (listOf(homeDeferred) + queryDeferreds).awaitAll().flatten()
            allHarvestedCandidates.addAll(discoveryCandidates)
        }

        // =====================================================================
        // 3. METADATA-FIRST CONTENT FILTERING (Music Only vs All Content)
        // =====================================================================
        val filterMode = preferencesRepository?.contentFilterMode?.first() ?: ContentFilterMode.MUSIC_ONLY
        val filteredCandidates = ContentFilter.filterTracks(allHarvestedCandidates, filterMode)
        val suppressedCount = allHarvestedCandidates.size - filteredCandidates.size
        logProvenance("ContentFilter: mode=$filterMode, Total harvested=${allHarvestedCandidates.size}, Passed=${filteredCandidates.size}, Suppressed=$suppressedCount non-music tracks")

        // =====================================================================
        // 4. NEGATIVE FEEDBACK EXCLUSION & DEDUPLICATION
        // =====================================================================
        val finalPool = filteredCandidates
            .filter { track -> !excludedTrackIds.contains(track.id) }
            .distinctBy { it.id }

        logProvenance("Final Candidate Pool: ${finalPool.size} unique tracks available for recommendation sections")
        finalPool
    }
}
