package com.synapse.recommendation.discovery

import com.synapse.recommendation.model.RecommendationResult
import com.synapse.recommendation.model.TasteProfile
import com.synapse.recommendation.similarity.ContentSimilarityEngine
import kotlin.math.max

/**
 * Discovery Intelligence implementing Maximal Marginal Relevance (MMR),
 * artist diversification constraints, and novelty calibration.
 */
class DiscoveryIntelligence(
    private val similarityEngine: ContentSimilarityEngine = ContentSimilarityEngine()
) {

    /**
     * Applies MMR re-ranking to balance relevance against intra-list redundancy.
     *
     * @param rankedResults Items pre-sorted by hybrid score.
     * @param discoveryAlpha 0.0 to 1.0 (0.85 = Familiar, 0.65 = Balanced, 0.35 = Adventurous).
     * @param maxResults Desired output size.
     * @param maxConsecutivePerArtist Strict ceiling on consecutive plays by same artist (default 2).
     */
    fun rerankWithDiversity(
        rankedResults: List<RecommendationResult>,
        profile: TasteProfile,
        discoveryAlpha: Double = 0.65,
        maxResults: Int = 20,
        maxConsecutivePerArtist: Int = 2
    ): List<RecommendationResult> {
        if (rankedResults.size <= 1) return rankedResults

        val alpha = discoveryAlpha.coerceIn(0.15, 0.95)
        val minScore = rankedResults.minOf { it.rankScore }
        val maxScore = rankedResults.maxOf { it.rankScore }
        val scoreRange = max(1.0, maxScore - minScore)

        val unselected = rankedResults.toMutableList()
        val selected = mutableListOf<RecommendationResult>()
        val artistCounts = mutableMapOf<String, Int>()

        while (unselected.isNotEmpty() && selected.size < maxResults) {
            val lastArtist = selected.lastOrNull()?.track?.artist?.lowercase()?.trim()

            // Check if last artist has hit consecutive limit
            val isConsecutiveBlocked = selected.size >= maxConsecutivePerArtist &&
                selected.takeLast(maxConsecutivePerArtist).all { it.track.artist.lowercase().trim() == lastArtist }

            var bestIndex = -1
            var bestMmrScore = Double.NEGATIVE_INFINITY

            for (i in unselected.indices) {
                val candidate = unselected[i]
                val candArtist = candidate.track.artist.lowercase().trim()

                // Enforce consecutive artist block
                if (isConsecutiveBlocked && candArtist == lastArtist) {
                    continue
                }

                // Enforce per-artist total saturation cap (max 3 per section)
                val currentCount = artistCounts[candArtist] ?: 0
                if (currentCount >= 3 && unselected.size > (maxResults - selected.size)) {
                    continue
                }

                // Normalized relevance in [0.0, 1.0]
                val normRelevance = (candidate.rankScore - minScore) / scoreRange

                // Maximum similarity to already selected tracks
                val maxRedundancy = if (selected.isEmpty()) {
                    0.0
                } else {
                    selected.maxOf { s -> similarityEngine.computeSimilarity(candidate.track, s.track) }
                }

                // MMR Equation: alpha * Relevance - (1 - alpha) * Redundancy
                val mmrScore = (alpha * normRelevance) - ((1.0 - alpha) * maxRedundancy)

                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore
                    bestIndex = i
                }
            }

            // Fallback if strict filters blocked all remaining items
            if (bestIndex == -1) {
                bestIndex = 0
            }

            val chosen = unselected.removeAt(bestIndex)
            selected.add(chosen)
            val chosenArtist = chosen.track.artist.lowercase().trim()
            artistCounts[chosenArtist] = (artistCounts[chosenArtist] ?: 0) + 1
        }

        return selected
    }
}
