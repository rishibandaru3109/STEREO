package com.synapse.recommendation.model

/**
 * Universal, stable track identifier across streaming providers, local files, and datasets.
 */
data class TrackIdentity(
    val uri: String,
    val rawId: String,
    val namespace: String = "yt"
) {
    companion object {
        fun fromYouTube(videoId: String): TrackIdentity =
            TrackIdentity("yt:$videoId", videoId, "yt")

        fun fromMusicBrainz(mbid: String): TrackIdentity =
            TrackIdentity("mbid:$mbid", mbid, "mbid")

        fun fromLastFm(artist: String, track: String): TrackIdentity {
            val normArtist = artist.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            val normTrack = track.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            return TrackIdentity("lfm:$normArtist::$normTrack", "$artist - $track", "lfm")
        }

        fun fromLocal(hash: String): TrackIdentity =
            TrackIdentity("local:$hash", hash, "local")
    }
}

/**
 * Rich domain track entity for recommendation computations.
 */
data class SynapseTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Long = 0L,
    val thumbnailUrl: String = "",
    val language: String? = null,
    val genres: List<String> = emptyList(),
    val isSpokenWord: Boolean = false,
    val releaseYear: Int? = null,
    val tempoBpm: Float? = null,
    val energy: Float? = null
) {
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    fun canonicalKey(): String {
        val normTitle = title.lowercase().trim()
            .replace(TITLE_CLEANUP_REGEX, "")
            .replace(NON_ALPHANUMERIC_REGEX, "")
        val normArtist = artist.lowercase().trim().replace(NON_ALPHANUMERIC_REGEX, "")
        return "$normArtist::$normTitle"
    }

    companion object {
        private val TITLE_CLEANUP_REGEX = Regex("\\b(official|music|video|audio|lyric|lyrics|remix|hd|4k|feat|ft)\\b.*")
        private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]")
    }
}

/**
 * Source metadata for retrieved candidates.
 */
enum class CandidateSource {
    SONG_RADIO,
    ARTIST_RELATIONSHIP,
    FAVORITES_AFFINITY,
    DISCOVERY_SUPPLEMENT,
    RECENT_SESSION
}

/**
 * A track candidate passing through candidate generation and ranking stages.
 */
data class RecommendationCandidate(
    val track: SynapseTrack,
    val source: CandidateSource,
    val seedTrackId: String? = null,
    val initialGraphWeight: Double = 0.0,
    val retrievedAt: Long = System.currentTimeMillis()
)

/**
 * Contextual request payload for a recommendation turn.
 */
data class RecommendationContext(
    val currentTrackId: String? = null,
    val sessionHistory: List<SynapseTrack> = emptyList(),
    val excludedTrackIds: Set<String> = emptySet(),
    val targetSection: String = "HOME_FEED",
    val discoveryAlpha: Double = 0.65, // 0.0 = Pure Familiarity, 1.0 = Pure Exploration
    val preferredLanguages: Set<String> = emptySet(),
    val preferredGenres: Set<String> = emptySet(),
    val isPrivateListening: Boolean = false,
    val maxResults: Int = 20
)

/**
 * Types of human-interpretable explanations.
 */
enum class ExplanationType {
    TOP_ARTIST_AFFINITY,
    SESSION_VIBE_MATCH,
    RADIO_GRAPH_RELATION,
    EXPLORE_GENRE,
    DISCOVER_NEW_ARTIST,
    COMMUNITY_TRENDING
}

data class RecommendationExplanation(
    val primaryReason: ExplanationType,
    val detailText: String
)

/**
 * Final scored and ranked recommendation item.
 */
data class RecommendationResult(
    val track: SynapseTrack,
    val rankScore: Double,
    val explanation: RecommendationExplanation,
    val source: CandidateSource,
    val componentScores: Map<String, Double> = emptyMap()
)

/**
 * Feedback types recorded for adaptive learning.
 */
enum class FeedbackType {
    LIKE,
    DISLIKE,
    DISMISS_RECOMMENDATION,
    NOT_INTERESTED_ARTIST
}

data class UserFeedback(
    val targetId: String,
    val feedbackType: FeedbackType,
    val isArtistLevel: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
