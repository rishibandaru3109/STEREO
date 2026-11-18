package com.nova.music.domain.model

data class ArtistAffinity(
    val artistName: String,
    val score: Double,
    val playCount: Int,
    val completionCount: Int,
    val lastPlayedAt: Long = 0L
)

data class TrackAffinity(
    val trackId: String,
    val title: String,
    val artist: String,
    val score: Double,
    val playCount: Int,
    val isLiked: Boolean,
    val lastPlayedAt: Long = 0L
)

data class ListeningProfile(
    val topArtists: List<ArtistAffinity> = emptyList(),
    val topTracks: List<TrackAffinity> = emptyList(),
    val likedTrackIds: Set<String> = emptySet(),
    val recentTracks: List<Track> = emptyList(),
    val totalListens: Int = 0,
    val totalSkips: Int = 0,
    val lastListenAt: Long = 0L,
    val likedTracks: List<Track> = emptyList()
) {
    val isColdStart: Boolean
        get() = totalListens < 3 && likedTrackIds.isEmpty()
}
