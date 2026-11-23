package com.synapse.recommendation.event

import com.synapse.recommendation.model.SynapseTrack
import kotlin.math.max
import kotlin.math.min

/**
 * High-fidelity listening event capturing exact playback behavior.
 */
data class ListeningEvent(
    val eventId: String = java.util.UUID.randomUUID().toString(),
    val track: SynapseTrack,
    val timestamp: Long = System.currentTimeMillis(),
    val listeningDurationMs: Long,
    val totalTrackDurationMs: Long,
    val completionRatio: Float,
    val isManualSkip: Boolean = false,
    val playbackContext: String? = null,
    val seekCount: Int = 0
)

/**
 * Semantic classification of a playback skip.
 */
enum class SkipNature {
    NONE,                   // Track completed or naturally transitioned
    IMMEDIATE_DISLIKE,      // Skipped within <10 seconds: strong negative preference
    MOOD_MISMATCH,          // Skipped between 10s and 30s: contextual/vibe clash, not permanent dislike
    QUEUE_SEEK,             // Skipped within <3 seconds: browsing/skipping to next queue item
    OUTRO_TRANSITION        // Skipped after >=80% completion: treated as positive completion
}

/**
 * Event Intelligence processor implementing monotonic accounting,
 * contextual skip interpretation, and Lu & Wu (2025) duration clamping.
 */
class EventIntelligence(
    private val minDurationNoiseMs: Long = 5_000L,
    private val maxDurationSaturationMs: Long = 180_000L
) {
    private var lastRecordedEventTime: Long = 0L
    private var lastTrackId: String? = null

    /**
     * Evaluates the semantic nature of a playback skip.
     */
    fun classifySkip(
        listeningDurationMs: Long,
        totalTrackDurationMs: Long,
        isManualSkip: Boolean,
        recentSkipBurstCount: Int = 0
    ): SkipNature {
        if (!isManualSkip) return SkipNature.NONE

        val duration = if (totalTrackDurationMs > 0) totalTrackDurationMs else 180_000L
        val completionRatio = (listeningDurationMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

        return when {
            // Outro transition: listened to >= 80% or within last 25s of track
            completionRatio >= 0.80f || (duration - listeningDurationMs) <= 25_000L -> SkipNature.OUTRO_TRANSITION
            // Rapid browsing / seeking across queue
            recentSkipBurstCount >= 2 && listeningDurationMs < 3_000L -> SkipNature.QUEUE_SEEK
            // Immediate rejection
            listeningDurationMs < 10_000L -> SkipNature.IMMEDIATE_DISLIKE
            // Mid-song vibe/mood mismatch
            listeningDurationMs in 10_000L..35_000L -> SkipNature.MOOD_MISMATCH
            else -> SkipNature.MOOD_MISMATCH
        }
    }

    /**
     * Calculates the bounded listening-time appreciation score kappa_cz
     * according to Lu & Wu (2025) Equations (9) and (10).
     *
     * Prevents long symphonies (e.g. 15 minutes) from drowning out short songs (e.g. 2 minutes).
     *
     * @return Normalized appreciation factor in [0.0, 1.0].
     */
    fun computeAppreciationScore(
        listeningDurationMs: Long,
        totalTrackDurationMs: Long
    ): Double {
        if (listeningDurationMs <= 0) return 0.0

        val totalMs = if (totalTrackDurationMs > 0) totalTrackDurationMs else 180_000L
        val delta = min(minDurationNoiseMs, (totalMs * 0.10).toLong()).toDouble()
        val chi = min(maxDurationSaturationMs, (totalMs * 0.85).toLong()).toDouble()

        if (chi <= delta) return 0.5

        val tRaw = listeningDurationMs.toDouble()
        val tClamped = when {
            tRaw <= delta -> delta
            tRaw >= chi -> chi
            else -> tRaw
        }

        return ((tClamped - delta) / (chi - delta)).coerceIn(0.0, 1.0)
    }

    /**
     * Monotonic event validator and deduplicator.
     * Prevents media session retries, seek storms, and duplicate event spam.
     */
    fun shouldRecordEvent(
        trackId: String,
        listeningDurationMs: Long,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        // Discard trivial noise under 2 seconds unless explicit
        if (listeningDurationMs < 2000L) return false

        // Debounce exact duplicate event within 1500ms
        if (trackId == lastTrackId && (currentTime - lastRecordedEventTime) < 1500L) {
            return false
        }

        lastTrackId = trackId
        lastRecordedEventTime = currentTime
        return true
    }

    /**
     * Computes the incremental taste deltas for long-term and short-term profiles.
     */
    fun computeEventImpact(
        event: ListeningEvent,
        appreciationScore: Double,
        skipNature: SkipNature
    ): EventImpact {
        return when (skipNature) {
            SkipNature.IMMEDIATE_DISLIKE -> EventImpact(
                longTermDelta = -15.0, // Track-specific penalty; artist-level negative feedback requires repeated skips
                shortTermDelta = -20.0,
                skipPenalty = 25.0,
                isCountedAsListen = false,
                isCountedAsSkip = true
            )
            SkipNature.MOOD_MISMATCH -> EventImpact(
                longTermDelta = -5.0, // Mild long-term effect
                shortTermDelta = -15.0, // Noticeable session penalty
                skipPenalty = 15.0,
                isCountedAsListen = true,
                isCountedAsSkip = true
            )
            SkipNature.QUEUE_SEEK -> EventImpact(
                longTermDelta = 0.0,
                shortTermDelta = 0.0,
                skipPenalty = 0.0,
                isCountedAsListen = false,
                isCountedAsSkip = false
            )
            SkipNature.OUTRO_TRANSITION -> EventImpact(
                longTermDelta = 30.0 * max(0.8, appreciationScore),
                shortTermDelta = 25.0,
                skipPenalty = 0.0,
                isCountedAsListen = true,
                isCountedAsSkip = false
            )
            SkipNature.NONE -> {
                val completionFactor = if (event.completionRatio >= 0.75f) 1.0 else 0.4
                EventImpact(
                    longTermDelta = 30.0 * appreciationScore * completionFactor,
                    shortTermDelta = 20.0 * appreciationScore,
                    skipPenalty = 0.0,
                    isCountedAsListen = true,
                    isCountedAsSkip = false
                )
            }
        }
    }
}

data class EventImpact(
    val longTermDelta: Double,
    val shortTermDelta: Double,
    val skipPenalty: Double,
    val isCountedAsListen: Boolean,
    val isCountedAsSkip: Boolean
)
