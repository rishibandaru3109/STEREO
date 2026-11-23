package com.synapse.recommendation.taste

import com.synapse.recommendation.event.EventIntelligence
import com.synapse.recommendation.event.ListeningEvent
import com.synapse.recommendation.event.SkipNature
import com.synapse.recommendation.model.ArtistAffinity

import com.synapse.recommendation.model.CategoryInterest
import com.synapse.recommendation.model.FeedbackType
import com.synapse.recommendation.model.LongTermProfile
import com.synapse.recommendation.model.ShortTermProfile
import com.synapse.recommendation.model.SynapseTrack
import com.synapse.recommendation.model.TasteProfile
import com.synapse.recommendation.model.TrackAffinity
import com.synapse.recommendation.model.UserFeedback
import kotlin.math.exp
import kotlin.math.max

/**
 * Taste Intelligence engine maintaining dual-horizon preference representations
 * (long-term slow decay + short-term fast session decay), category distributions,
 * and active negative feedback penalties.
 */
class TasteIntelligence(
    private val eventIntelligence: EventIntelligence = EventIntelligence(),
    private val longTermDecayRatePerDay: Double = 0.07,  // Half-life ~ 10 days
    private val shortTermDecayRatePerHour: Double = 0.50, // Half-life ~ 1.4 hours
    private val penaltyDecayRatePerHour: Double = 0.12    // Half-life ~ 6 hours
) {
    private var tasteProfile = TasteProfile()
    private val rawListeningEvents = mutableListOf<ListeningEvent>()
    private val explicitFeedbacks = mutableListOf<UserFeedback>()
    private val artistSkipCounts = mutableMapOf<String, Int>()

    fun getTasteProfile(): TasteProfile = tasteProfile

    /**
     * Ingests a new listening event and updates both profiles.
     */
    @Synchronized
    fun processEvent(
        event: ListeningEvent,
        recentSkipBurstCount: Int = 0
    ): TasteProfile {
        if (!eventIntelligence.shouldRecordEvent(event.track.id, event.listeningDurationMs, event.timestamp)) {
            return tasteProfile
        }

        rawListeningEvents.add(event)
        val skipNature = eventIntelligence.classifySkip(
            listeningDurationMs = event.listeningDurationMs,
            totalTrackDurationMs = event.totalTrackDurationMs,
            isManualSkip = event.isManualSkip,
            recentSkipBurstCount = recentSkipBurstCount
        )

        val appreciationScore = eventIntelligence.computeAppreciationScore(
            listeningDurationMs = event.listeningDurationMs,
            totalTrackDurationMs = event.totalTrackDurationMs
        )

        val impact = eventIntelligence.computeEventImpact(event, appreciationScore, skipNature)
        val normArtist = event.track.artist.lowercase().trim()

        // 1. Update Active Penalties with confidence weighting
        val updatedPenalties = tasteProfile.activePenalties.toMutableMap()
        if (impact.skipPenalty > 0.0) {
            val currentPen = updatedPenalties[event.track.id] ?: 0.0
            updatedPenalties[event.track.id] = currentPen + impact.skipPenalty

            // Confidence-weighted artist penalty: a single accidental skip does NOT penalize the entire artist.
            // Repeated skips (>= 2) accumulate artist-level negative feedback.
            val currentSkips = (artistSkipCounts[normArtist] ?: 0) + 1
            artistSkipCounts[normArtist] = currentSkips
            if (currentSkips >= 2) {
                val repeatedArtistPen = (currentSkips - 1) * 8.0
                val artistPen = updatedPenalties[normArtist] ?: 0.0
                updatedPenalties[normArtist] = artistPen + repeatedArtistPen
            }
        } else if (impact.isCountedAsListen) {
            // Positive feedback (listen / completion) recovers artist from skip penalties
            artistSkipCounts[normArtist] = maxOf(0, (artistSkipCounts[normArtist] ?: 0) - 1)
            if ((artistSkipCounts[normArtist] ?: 0) == 0) {
                updatedPenalties.remove(normArtist)
            }
        }

        // 2. Update Short-Term Profile
        val updatedSessionTracks = (tasteProfile.shortTerm.activeSessionTracks + event.track).takeLast(20)
        val updatedSkips = if (impact.isCountedAsSkip) {
            (tasteProfile.shortTerm.recentSkips + event.track.id).takeLast(10)
        } else {
            tasteProfile.shortTerm.recentSkips
        }

        val shortTerm = ShortTermProfile(
            activeSessionTracks = updatedSessionTracks,
            recentSkips = updatedSkips,
            sessionStartedAt = tasteProfile.shortTerm.sessionStartedAt,
            lastEventAt = event.timestamp
        )

        // 3. Recompute Long-Term Profile with exponential decay
        val longTerm = computeLongTermProfile(event.timestamp)

        tasteProfile = TasteProfile(
            longTerm = longTerm,
            shortTerm = shortTerm,
            activePenalties = updatedPenalties
        )

        return tasteProfile
    }

    /**
     * Processes explicit user feedback (Likes, Dislikes, Discard).
     */
    @Synchronized
    fun processFeedback(feedback: UserFeedback): TasteProfile {
        explicitFeedbacks.add(feedback)
        val updatedPenalties = tasteProfile.activePenalties.toMutableMap()
        val currentLong = tasteProfile.longTerm

        when (feedback.feedbackType) {
            FeedbackType.LIKE -> {
                updatedPenalties.remove(feedback.targetId)
                if (feedback.isArtistLevel) {
                    val norm = feedback.targetId.lowercase().trim()
                    updatedPenalties.remove(norm)
                    artistSkipCounts[norm] = 0
                }
                val likedTrack = rawListeningEvents.firstOrNull { it.track.id == feedback.targetId }?.track
                if (likedTrack != null) {
                    val norm = likedTrack.artist.lowercase().trim()
                    updatedPenalties.remove(norm)
                    artistSkipCounts[norm] = 0
                }
                tasteProfile = tasteProfile.copy(
                    longTerm = currentLong.copy(
                        likedTrackIds = currentLong.likedTrackIds + feedback.targetId,
                        dislikedTrackIds = currentLong.dislikedTrackIds - feedback.targetId
                    ),
                    activePenalties = updatedPenalties
                )
            }
            FeedbackType.DISLIKE -> {
                updatedPenalties[feedback.targetId] = 100.0
                tasteProfile = tasteProfile.copy(
                    longTerm = currentLong.copy(
                        likedTrackIds = currentLong.likedTrackIds - feedback.targetId,
                        dislikedTrackIds = currentLong.dislikedTrackIds + feedback.targetId
                    ),
                    activePenalties = updatedPenalties
                )
            }
            FeedbackType.NOT_INTERESTED_ARTIST -> {
                val normArtist = feedback.targetId.lowercase().trim()
                updatedPenalties[normArtist] = 100.0
                tasteProfile = tasteProfile.copy(
                    longTerm = currentLong.copy(
                        suppressedArtistNames = currentLong.suppressedArtistNames + normArtist
                    ),
                    activePenalties = updatedPenalties
                )
            }
            FeedbackType.DISMISS_RECOMMENDATION -> {
                updatedPenalties[feedback.targetId] = (updatedPenalties[feedback.targetId] ?: 0.0) + 30.0
                tasteProfile = tasteProfile.copy(activePenalties = updatedPenalties)
            }
        }

        // Recompute profile with newly assigned explicit feedback
        tasteProfile = tasteProfile.copy(longTerm = computeLongTermProfile(System.currentTimeMillis()))
        return tasteProfile
    }

    /**
     * Recomputes the entire long-term preference profile over all events
     * applying Lu & Wu (2025) clamping and Bayesian decay.
     */
    private fun computeLongTermProfile(now: Long): LongTermProfile {
        class Accumulator(
            val track: SynapseTrack,
            var score: Double = 0.0,
            var playCount: Int = 0,
            var completionCount: Int = 0,
            var skipCount: Int = 0,
            var totalListeningMs: Long = 0L,
            var lastPlayedAt: Long = 0L
        )

        val trackMap = mutableMapOf<String, Accumulator>()
        val categoryDwellMap = mutableMapOf<String, Long>()

        for (event in rawListeningEvents) {
            val acc = trackMap.getOrPut(event.track.id) { Accumulator(event.track) }
            val ageDays = max(0L, now - event.timestamp) / (1000.0 * 60 * 60 * 24)
            val decay = exp(-longTermDecayRatePerDay * ageDays)

            acc.playCount++
            acc.totalListeningMs += event.listeningDurationMs
            if (event.timestamp > acc.lastPlayedAt) {
                acc.lastPlayedAt = event.timestamp
            }

            val appreciation = eventIntelligence.computeAppreciationScore(
                event.listeningDurationMs,
                event.totalTrackDurationMs
            )

            val skipNature = eventIntelligence.classifySkip(
                event.listeningDurationMs,
                event.totalTrackDurationMs,
                event.isManualSkip
            )

            val impact = eventIntelligence.computeEventImpact(event, appreciation, skipNature)
            acc.score += impact.longTermDelta * decay

            if (skipNature == SkipNature.OUTRO_TRANSITION || (!event.isManualSkip && event.completionRatio >= 0.75f)) {
                acc.completionCount++
            } else if (impact.isCountedAsSkip) {
                acc.skipCount++
            }

            // Accumulate genre / category dwell time
            val categories = event.track.genres.ifEmpty {
                listOfNotNull(event.track.language?.let { "$it music" })
            }
            for (cat in categories) {
                val normCat = cat.lowercase().trim()
                categoryDwellMap[normCat] = (categoryDwellMap[normCat] ?: 0L) + event.listeningDurationMs
            }
        }

        // Apply explicit like/dislike bonuses
        val likedIds = tasteProfile.longTerm.likedTrackIds
        val dislikedIds = tasteProfile.longTerm.dislikedTrackIds

        for (likedId in likedIds) {
            trackMap[likedId]?.let { it.score += 40.0 }
        }
        for (dislikedId in dislikedIds) {
            trackMap[dislikedId]?.let { it.score -= 100.0 }
        }

        // 1. Build Track Affinities
        val topTracks = trackMap.values.map { acc ->
            TrackAffinity(
                trackId = acc.track.id,
                title = acc.track.title,
                artist = acc.track.artist,
                score = acc.score,
                playCount = acc.playCount,
                completionCount = acc.completionCount,
                isLiked = likedIds.contains(acc.track.id),
                lastPlayedAt = acc.lastPlayedAt
            )
        }.sortedByDescending { it.score }

        // 2. Build Artist Affinities
        class ArtistAcc(
            val artistName: String,
            var score: Double = 0.0,
            var playCount: Int = 0,
            var completionCount: Int = 0,
            var skipCount: Int = 0,
            var lastPlayedAt: Long = 0L
        )

        val artistMap = mutableMapOf<String, ArtistAcc>()
        for (acc in trackMap.values) {
            val norm = acc.track.artist.lowercase().trim()
            if (norm.isBlank()) continue
            val aAcc = artistMap.getOrPut(norm) { ArtistAcc(acc.track.artist) }
            // Positive track dwell time contributes directly to artist affinity
            if (acc.score > 0.0) {
                aAcc.score += acc.score
            }
            aAcc.playCount += acc.playCount
            aAcc.completionCount += acc.completionCount
            aAcc.skipCount += acc.skipCount
            if (acc.lastPlayedAt > aAcc.lastPlayedAt) {
                aAcc.lastPlayedAt = acc.lastPlayedAt
            }
        }

        // Apply confidence-weighted repeated skip penalty to artist affinity:
        // A single accidental skip (skipCount == 1) does NOT destroy the artist.
        // Repeated skips (>= 2) penalize artist affinity proportionally.
        for (a in artistMap.values) {
            if (a.skipCount >= 2) {
                val repeatedSkipPenalty = (a.skipCount - 1) * 5.0
                a.score = maxOf(0.0, a.score - repeatedSkipPenalty)
            }
        }

        val topArtists = artistMap.values.map { a ->
            ArtistAffinity(
                artistName = a.artistName,
                score = a.score,
                playCount = a.playCount,
                completionCount = a.completionCount,
                skipCount = a.skipCount,
                lastPlayedAt = a.lastPlayedAt
            )
        }.filter { it.score > 0.0 }.sortedByDescending { it.score }

        // 3. Compute Normalized Category Interests (Lu & Wu 2025 Eq 11)
        val totalCategoryDwellMs = categoryDwellMap.values.sum().toDouble()
        val categoryInterests = if (totalCategoryDwellMs > 0.0) {
            categoryDwellMap.map { (cat, dwellMs) ->
                CategoryInterest(
                    categoryName = cat,
                    totalListeningTimeMs = dwellMs,
                    normalizedWeight = (dwellMs.toDouble() / totalCategoryDwellMs).coerceIn(0.0, 1.0)
                )
            }.sortedByDescending { it.normalizedWeight }
        } else {
            emptyList()
        }

        return LongTermProfile(
            topArtists = topArtists,
            topTracks = topTracks,
            categoryInterests = categoryInterests,
            likedTrackIds = likedIds,
            dislikedTrackIds = dislikedIds,
            suppressedArtistNames = tasteProfile.longTerm.suppressedArtistNames,
            totalListens = rawListeningEvents.size,
            totalSkips = trackMap.values.sumOf { it.skipCount },
            lastListenAt = rawListeningEvents.lastOrNull()?.timestamp ?: 0L
        )
    }

    /**
     * Decays active temporary penalties based on elapsed time.
     */
    fun decayPenalties(elapsedHours: Double) {
        if (tasteProfile.activePenalties.isEmpty()) return
        val factor = exp(-penaltyDecayRatePerHour * elapsedHours)
        val decayed = tasteProfile.activePenalties.mapNotNull { (key, penalty) ->
            val pNew = penalty * factor
            if (pNew > 2.0) key to pNew else null
        }.toMap()
        tasteProfile = tasteProfile.copy(activePenalties = decayed)
    }

    fun resetLongTermProfile() {
        tasteProfile = tasteProfile.copy(longTerm = LongTermProfile())
        rawListeningEvents.clear()
    }

    fun resetShortTermProfile() {
        tasteProfile = tasteProfile.copy(
            shortTerm = ShortTermProfile(),
            activePenalties = emptyMap()
        )
    }

    fun clearAll() {
        resetLongTermProfile()
        resetShortTermProfile()
        explicitFeedbacks.clear()
    }
}
