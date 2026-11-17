package com.nova.music.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val coverArtworkUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val trackCount: Int = 0
)

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<Track>
)
