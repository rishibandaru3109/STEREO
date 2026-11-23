package com.synapse.recommendation.model

/**
 * Learned long-term and short-term affinity for an artist.
 */
data class ArtistAffinity(
    val artistName: String,
    val score: Double,
    val playCount: Int,
    val completionCount: Int,
    val skipCount: Int,
    val lastPlayedAt: Long = 0L
)

/**
 * Learned affinity for a specific track.
 */
data class TrackAffinity(
    val trackId: String,
    val title: String,
    val artist: String,
    val score: Double,
    val playCount: Int,
    val completionCount: Int,
    val isLiked: Boolean,
    val lastPlayedAt: Long = 0L
)

/**
 * Normalized category / genre interest based on Lu & Wu (2025) Eq. (11).
 * Int_cz = sum(t_cz) / sum_all(t_cz)
 */
data class CategoryInterest(
    val categoryName: String,
    val totalListeningTimeMs: Long,
    val normalizedWeight: Double
)

/**
 * Represents the immediate session intent and recent active state.
 */
data class ShortTermProfile(
    val activeSessionTracks: List<SynapseTrack> = emptyList(),
    val recentSkips: List<String> = emptySet<String>().toList(),
    val sessionGenreWeights: Map<String, Double> = emptyMap(),
    val sessionStartedAt: Long = System.currentTimeMillis(),
    val lastEventAt: Long = System.currentTimeMillis()
) {
    val currentArtistCentroid: String?
        get() = activeSessionTracks.lastOrNull()?.artist
}

/**
 * Represents the persistent, slowly decaying user preference profile.
 */
data class LongTermProfile(
    val topArtists: List<ArtistAffinity> = emptyList(),
    val topTracks: List<TrackAffinity> = emptyList(),
    val categoryInterests: List<CategoryInterest> = emptyList(),
    val likedTrackIds: Set<String> = emptySet(),
    val dislikedTrackIds: Set<String> = emptySet(),
    val suppressedArtistNames: Set<String> = emptySet(),
    val totalListens: Int = 0,
    val totalSkips: Int = 0,
    val lastListenAt: Long = 0L
) {
    val isColdStart: Boolean
        get() = totalListens < 3 && likedTrackIds.isEmpty()

    fun getArtistAffinity(artist: String): Double {
        val norm = artist.lowercase().trim()
        return topArtists.firstOrNull { it.artistName.lowercase().trim() == norm }?.score ?: 0.0
    }

    fun getCategoryWeight(category: String): Double {
        val norm = category.lowercase().trim()
        return categoryInterests.firstOrNull { it.categoryName.lowercase().trim() == norm }?.normalizedWeight ?: 0.0
    }
}

/**
 * Consolidated TasteProfile combining dual horizons and active penalties.
 */
data class TasteProfile(
    val longTerm: LongTermProfile = LongTermProfile(),
    val shortTerm: ShortTermProfile = ShortTermProfile(),
    val activePenalties: Map<String, Double> = emptyMap() // trackId/artist -> decayed negative penalty
)
