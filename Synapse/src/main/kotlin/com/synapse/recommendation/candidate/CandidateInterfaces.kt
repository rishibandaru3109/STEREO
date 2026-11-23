package com.synapse.recommendation.candidate

import com.synapse.recommendation.model.CandidateSource
import com.synapse.recommendation.model.RecommendationCandidate
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.SynapseTrack
import com.synapse.recommendation.model.TasteProfile

/**
 * Universal candidate generation SPI.
 */
interface CandidateProvider {
    suspend fun generateCandidates(
        profile: TasteProfile,
        context: RecommendationContext
    ): List<RecommendationCandidate>
}

/**
 * High-performance candidate deduplicator.
 * Reconciles identical recordings across different uploaders, titles with "feat.",
 * or alternate video IDs.
 */
class CandidateDeduplicator {

    fun deduplicate(candidates: List<RecommendationCandidate>): List<RecommendationCandidate> {
        val seenIds = mutableSetOf<String>()
        val seenCanonicalKeys = mutableSetOf<String>()
        val result = mutableListOf<RecommendationCandidate>()

        for (candidate in candidates) {
            val track = candidate.track
            if (track.id.isBlank()) continue

            // 1. Exact ID check
            if (seenIds.contains(track.id)) continue

            // 2. Canonical artist + title check (collapses "(Official Video)", "(Lyrics)", etc.)
            val canonicalKey = track.canonicalKey()
            if (seenCanonicalKeys.contains(canonicalKey)) continue

            seenIds.add(track.id)
            seenCanonicalKeys.add(canonicalKey)
            result.add(candidate)
        }

        return result
    }
}

/**
 * Fast in-memory candidate provider for unit tests, offline benchmarking, and local cache retrieval.
 */
class InMemoryCandidateProvider(
    private val catalog: List<SynapseTrack>
) : CandidateProvider {

    override suspend fun generateCandidates(
        profile: TasteProfile,
        context: RecommendationContext
    ): List<RecommendationCandidate> {
        val topArtistNames = profile.longTerm.topArtists.map { it.artistName.lowercase().trim() }.toSet()
        val likedIds = profile.longTerm.likedTrackIds
        val currentSeedArtist = context.currentTrackId?.let { id ->
            catalog.firstOrNull { it.id == id }?.artist?.lowercase()?.trim()
        }

        return catalog.mapNotNull { track ->
            val normArtist = track.artist.lowercase().trim()
            val isLiked = likedIds.contains(track.id)
            val isTopArtist = topArtistNames.contains(normArtist)
            val isSameArtist = currentSeedArtist != null && normArtist == currentSeedArtist

            val source = when {
                isSameArtist -> CandidateSource.SONG_RADIO
                isTopArtist -> CandidateSource.ARTIST_RELATIONSHIP
                isLiked -> CandidateSource.FAVORITES_AFFINITY
                else -> CandidateSource.DISCOVERY_SUPPLEMENT
            }

            RecommendationCandidate(
                track = track,
                source = source,
                seedTrackId = context.currentTrackId,
                initialGraphWeight = if (isSameArtist) 1.0 else if (isTopArtist) 0.8 else 0.5
            )
        }
    }
}
