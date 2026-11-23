package com.synapse.recommendation.filter

import com.synapse.recommendation.model.RecommendationCandidate
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.TasteProfile

/**
 * Stage 1 Hard Eligibility & Constraint Filter.
 * Executes before feature calculation and ranking to eliminate unqualified candidates.
 */
class EligibilityFilter {

    fun filter(
        candidates: List<RecommendationCandidate>,
        profile: TasteProfile,
        context: RecommendationContext
    ): FilterResult {
        val excludedIds = context.excludedTrackIds + profile.longTerm.dislikedTrackIds
        val suppressedArtists = profile.longTerm.suppressedArtistNames.map { it.lowercase().trim() }.toSet()
        val preferredLangs = context.preferredLanguages.map { it.lowercase().trim() }.toSet()

        val qualified = mutableListOf<RecommendationCandidate>()
        val rejectedSpokenWord = mutableListOf<RecommendationCandidate>()
        val rejectedLanguage = mutableListOf<RecommendationCandidate>()
        val rejectedBlacklist = mutableListOf<RecommendationCandidate>()

        for (cand in candidates) {
            val track = cand.track
            val normArtist = track.artist.lowercase().trim()

            // 1. Blacklist / Excluded IDs
            if (excludedIds.contains(track.id) || suppressedArtists.contains(normArtist)) {
                rejectedBlacklist.add(cand)
                continue
            }

            // 2. Spoken word / non-music check
            if (track.isSpokenWord) {
                rejectedSpokenWord.add(cand)
                continue
            }

            // 3. Language eligibility
            if (preferredLangs.isNotEmpty()) {
                val trackLang = track.language?.lowercase()?.trim()
                if (trackLang == null || !preferredLangs.contains(trackLang)) {
                    rejectedLanguage.add(cand)
                    continue
                }
            }

            qualified.add(cand)
        }

        // Safe Fallback Rule: If strict language filtering removed ALL candidates,
        // fallback to non-language-rejected music candidates so user never suffers a blank screen.
        val finalCandidates = if (qualified.isEmpty() && rejectedLanguage.isNotEmpty()) {
            rejectedLanguage
        } else {
            qualified
        }

        return FilterResult(
            eligible = finalCandidates,
            isFallbackActivated = qualified.isEmpty() && rejectedLanguage.isNotEmpty(),
            rejectedBlacklistCount = rejectedBlacklist.size,
            rejectedSpokenWordCount = rejectedSpokenWord.size,
            rejectedLanguageCount = rejectedLanguage.size
        )
    }
}

data class FilterResult(
    val eligible: List<RecommendationCandidate>,
    val isFallbackActivated: Boolean,
    val rejectedBlacklistCount: Int,
    val rejectedSpokenWordCount: Int,
    val rejectedLanguageCount: Int
)
