package com.nova.music.domain.repository

import com.nova.music.domain.model.Playlist
import com.nova.music.domain.model.PlaylistWithTracks
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylist(playlistId: String): Flow<Playlist?>
    fun getPlaylistTracks(playlistId: String): Flow<List<Track>>
    fun getPlaylistWithTracks(playlistId: String): Flow<PlaylistWithTracks?>
    suspend fun createPlaylist(name: String, description: String? = null): String
    suspend fun renamePlaylist(playlistId: String, newName: String)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    suspend fun reorderTracks(playlistId: String, fromPosition: Int, toPosition: Int)
    suspend fun setPlaylistPinned(playlistId: String, isPinned: Boolean)
}
