package com.nova.music.domain.recommendation

import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.Track
import kotlin.random.Random

sealed interface SurpriseResult {
    data class Success(val selectedTrack: Track, val queueTracks: List<Track>) : SurpriseResult
    data class Fallback(val selectedTrack: Track, val queueTracks: List<Track>, val message: String) : SurpriseResult
    data class Unavailable(val message: String) : SurpriseResult
}

interface SurpriseMeEngine {
    fun pickSurpriseTrack(
        profile: ListeningProfile,
        candidates: List<Track>,
        fallbackTracks: List<Track>,
        excludedTrackIds: Set<String>,
        currentlyPlayingId: String? = null
    ): SurpriseResult
}

class SurpriseMeEngineImpl(
    private val recommendationEngine: RecommendationEngine
) : SurpriseMeEngine {

    override fun pickSurpriseTrack(
        profile: ListeningProfile,
        candidates: List<Track>,
        fallbackTracks: List<Track>,
        excludedTrackIds: Set<String>,
        currentlyPlayingId: String?
    ): SurpriseResult {
        // 1. Exclude recent listens, dislikes, and currently playing track
        val recentTrackIds = profile.recentTracks.take(8).map { it.id }.toSet()
        val totalExcluded = excludedTrackIds + recentTrackIds + listOfNotNull(currentlyPlayingId)

        val validCandidates = candidates.filter { it.id !in totalExcluded }.distinctBy(Track::id)

        // 2. If valid candidates are empty, attempt fallback pool (e.g. liked songs / library)
        if (validCandidates.isEmpty()) {
            val validFallback = fallbackTracks.filter { it.id !in excludedTrackIds && it.id != currentlyPlayingId }
                .distinctBy(Track::id)
            if (validFallback.isEmpty()) {
                return SurpriseResult.Unavailable("No music available to surprise you right now. Check your internet connection or library.")
            }

            val fallbackSelected = validFallback.random(Random(System.currentTimeMillis()))
            val remainingFallback = validFallback.filter { it.id != fallbackSelected.id }.shuffled().take(15)
            val queue = listOf(fallbackSelected) + remainingFallback
            return SurpriseResult.Fallback(
                selectedTrack = fallbackSelected,
                queueTracks = queue,
                message = "Surprise picked from your library favorites"
            )
        }

        // 3. Taste-Weighted Sampling
        val topArtistNames = profile.topArtists.map { it.artistName.lowercase().trim() }
        val top1Artist = topArtistNames.firstOrNull()
        val topTrackIds = profile.topTracks.filter { it.score > 0 }.map { it.trackId }.toSet()

        val weightedPool = validCandidates.map { track ->
            var weight = 1.0 // Base exploration weight for fresh musical discovery

            val trackArtist = track.artist.lowercase().trim()

            // Top #1 Artist Affinity (+6 weight)
            if (top1Artist != null && trackArtist == top1Artist) {
                weight += 6.0
            } else if (trackArtist in topArtistNames) {
                // Secondary Top Artist Affinity (+3 weight)
                weight += 3.0
            }

            // High Listen / Repeated Affinity (+2 weight)
            if (track.id in topTrackIds) {
                weight += 2.0
            }

            track to weight
        }

        // Roulette-wheel selection
        val totalWeight = weightedPool.sumOf { it.second }
        val randomRoll = Random(System.currentTimeMillis()).nextDouble(totalWeight)
        var cumulative = 0.0
        var selected = weightedPool.first().first

        for ((track, weight) in weightedPool) {
            cumulative += weight
            if (cumulative >= randomRoll) {
                selected = track
                break
            }
        }

        // 4. Construct Context Queue
        // Selected track is placed at index 0, followed by related diversified tracks
        val remainingCandidates = validCandidates.filter { it.id != selected.id }
        val relatedPool = if (recommendationEngine is RecommendationEngineImpl) {
            recommendationEngine.diversifyTracks(remainingCandidates, maxConsecutivePerArtist = 2).take(15)
        } else {
            remainingCandidates.take(15)
        }

        val fullQueue = listOf(selected) + relatedPool
        return SurpriseResult.Success(selectedTrack = selected, queueTracks = fullQueue)
    }
}
