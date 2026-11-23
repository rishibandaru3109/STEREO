package com.synapse.recommendation.ranking

import com.synapse.recommendation.model.CandidateSource
import com.synapse.recommendation.model.ExplanationType
import com.synapse.recommendation.model.RecommendationCandidate
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.RecommendationExplanation
import com.synapse.recommendation.model.RecommendationResult
import com.synapse.recommendation.model.TasteProfile
import com.synapse.recommendation.similarity.ContentSimilarityEngine
import kotlin.math.exp
import kotlin.math.max

/**
 * Interface for candidate ranking engines.
 */
interface CandidateRanker {
    fun rank(
        candidates: List<RecommendationCandidate>,
        profile: TasteProfile,
        context: RecommendationContext
    ): List<RecommendationResult>
}

/**
 * Adaptive Multi-Factor Hybrid Ranker combining long-term Bayesian affinity,
 * short-term session intent, Lu & Wu category interest, graph relevance,
 * active skip discounting, and transparent explainability.
 */
class HybridCandidateRanker(
    private val similarityEngine: ContentSimilarityEngine = ContentSimilarityEngine(),
    private val radioGraphWeight: Double = 30.0,
    private val artistAffinityWeight: Double = 0.60,
    private val sessionSimilarityWeight: Double = 25.0,
    private val genreInterestWeight: Double = 20.0,
    private val repeatPenaltyBase: Double = 20.0
) : CandidateRanker {

    override fun rank(
        candidates: List<RecommendationCandidate>,
        profile: TasteProfile,
        context: RecommendationContext
    ): List<RecommendationResult> {
        val now = System.currentTimeMillis()
        val recentPlayedIds = profile.shortTerm.activeSessionTracks.map { it.id }.toSet()
        val topArtistNames = profile.longTerm.topArtists.associate {
            it.artistName.lowercase().trim() to it.score
        }

        return candidates.map { cand ->
            val track = cand.track
            val normArtist = track.artist.lowercase().trim()

            // 1. Source / Graph Component
            val sourceScore = when (cand.source) {
                CandidateSource.SONG_RADIO -> radioGraphWeight
                CandidateSource.FAVORITES_AFFINITY -> 25.0
                CandidateSource.ARTIST_RELATIONSHIP -> 15.0
                CandidateSource.RECENT_SESSION -> 5.0
                CandidateSource.DISCOVERY_SUPPLEMENT -> 8.0
            }

            // 2. Long-Term Artist Affinity Component
            val rawArtistScore = topArtistNames[normArtist]?.coerceIn(0.0, 60.0) ?: 0.0
            val artistScore = rawArtistScore * artistAffinityWeight

            // 3. Short-Term Session Similarity Component
            val sessionScore = if (profile.shortTerm.activeSessionTracks.isNotEmpty()) {
                val maxSim = profile.shortTerm.activeSessionTracks.maxOfOrNull { sessionTrack ->
                    similarityEngine.computeSimilarity(track, sessionTrack)
                } ?: 0.0
                maxSim * sessionSimilarityWeight
            } else {
                0.0
            }

            // 4. Normalized Category / Genre Interest (Lu & Wu Eq 11)
            val genreScore = track.genres.maxOfOrNull { genre ->
                profile.longTerm.getCategoryWeight(genre) * genreInterestWeight
            } ?: (track.language?.let { profile.longTerm.getCategoryWeight("$it music") * genreInterestWeight } ?: 0.0)

            // 5. Active Negative Penalty (Skips and Dislikes)
            val trackPenalty = profile.activePenalties[track.id] ?: 0.0
            val artistPenalty = profile.activePenalties[normArtist] ?: 0.0
            val totalPenalty = trackPenalty + (artistPenalty * 0.5)

            // 6. Repetition Penalty for Recently Played Tracks
            val repeatPenalty = if (recentPlayedIds.contains(track.id)) {
                repeatPenaltyBase
            } else {
                0.0
            }

            // Composite Score Fusion
            val totalScore = sourceScore + artistScore + sessionScore + genreScore - totalPenalty - repeatPenalty

            // Component Breakdown for Auditing & Explanations
            val components = mapOf(
                "source" to sourceScore,
                "artist" to artistScore,
                "session" to sessionScore,
                "genre" to genreScore,
                "penalty" to totalPenalty,
                "repeat" to repeatPenalty
            )

            // Generate authentic explanation
            val explanation = generateExplanation(
                cand = cand,
                artistScore = artistScore,
                sessionScore = sessionScore,
                genreScore = genreScore,
                sourceScore = sourceScore
            )

            RecommendationResult(
                track = track,
                rankScore = totalScore,
                explanation = explanation,
                source = cand.source,
                componentScores = components
            )
        }.sortedByDescending { it.rankScore }
    }

    private fun generateExplanation(
        cand: RecommendationCandidate,
        artistScore: Double,
        sessionScore: Double,
        genreScore: Double,
        sourceScore: Double
    ): RecommendationExplanation {
        val maxScore = max(max(artistScore, sessionScore), max(genreScore, sourceScore))

        return when {
            maxScore == artistScore && artistScore > 5.0 -> RecommendationExplanation(
                primaryReason = ExplanationType.TOP_ARTIST_AFFINITY,
                detailText = "Because you often listen to ${cand.track.artist}"
            )
            maxScore == sessionScore && sessionScore > 5.0 -> RecommendationExplanation(
                primaryReason = ExplanationType.SESSION_VIBE_MATCH,
                detailText = "Fits your current listening session"
            )
            cand.source == CandidateSource.SONG_RADIO -> RecommendationExplanation(
                primaryReason = ExplanationType.RADIO_GRAPH_RELATION,
                detailText = "Similar to tracks in your recent radio"
            )
            cand.track.genres.isNotEmpty() -> RecommendationExplanation(
                primaryReason = ExplanationType.EXPLORE_GENRE,
                detailText = "Selected from your favorite ${cand.track.genres.first()} style"
            )
            else -> RecommendationExplanation(
                primaryReason = ExplanationType.DISCOVER_NEW_ARTIST,
                detailText = "Fresh discovery recommended for you"
            )
        }
    }
}
