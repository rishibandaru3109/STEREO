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
    fun getPlaylistTrackIds(playlistId: String): Flow<Set<String>>
    fun getPlaylistIdsContainingTrack(trackId: String): Flow<Set<String>>
    suspend fun createPlaylist(name: String, description: String? = null, coverArtworkUrl: String? = null): String
    suspend fun renamePlaylist(playlistId: String, newName: String)
    suspend fun updatePlaylistDetails(playlistId: String, name: String, description: String?, coverArtworkUrl: String?)
    suspend fun updatePlaylistCover(playlistId: String, coverArtworkUrl: String?)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun duplicatePlaylist(playlistId: String, newName: String? = null): String
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    suspend fun addTrackToPlaylist(playlistId: String, track: Track, allowDuplicates: Boolean): Boolean
    suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>)
    suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>, allowDuplicates: Boolean): Int
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    suspend fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>)
    suspend fun moveTracksBetweenPlaylists(sourcePlaylistId: String, targetPlaylistId: String, trackIds: List<String>)
    suspend fun copyTracksToPlaylist(targetPlaylistId: String, trackIds: List<String>)
    suspend fun clearPlaylist(playlistId: String)
    suspend fun reorderTracks(playlistId: String, fromPosition: Int, toPosition: Int)
    suspend fun reorderAllTracks(playlistId: String, orderedTrackIds: List<String>)
    suspend fun setPlaylistPinned(playlistId: String, isPinned: Boolean)
}
