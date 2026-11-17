package com.nova.music.data.repository

import com.nova.music.core.database.dao.PlaylistDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.database.entity.PlaylistEntity
import com.nova.music.core.database.entity.PlaylistEntryEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.domain.model.Playlist
import com.nova.music.domain.model.PlaylistWithTracks
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class PlaylistRepositoryImpl(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao
) : PlaylistRepository {

    override fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsWithMetadata().map { list ->
            list.map { item ->
                Playlist(
                    id = item.id,
                    name = item.name,
                    description = item.description,
                    coverArtworkUrl = item.coverArtworkUrl ?: item.autoArtworkUrl,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                    isPinned = item.isPinned,
                    trackCount = item.trackCount
                )
            }
        }
    }

    override fun getPlaylist(playlistId: String): Flow<Playlist?> {
        return combine(
            playlistDao.getPlaylistFlow(playlistId),
            playlistDao.getPlaylistTrackCount(playlistId)
        ) { entity, count ->
            entity?.let {
                Playlist(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    coverArtworkUrl = it.coverArtworkUrl,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    isPinned = it.isPinned,
                    trackCount = count
                )
            }
        }
    }

    override fun getPlaylistTracks(playlistId: String): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { list ->
            list.map { it.toTrack() }
        }
    }

    override fun getPlaylistWithTracks(playlistId: String): Flow<PlaylistWithTracks?> {
        return combine(
            getPlaylist(playlistId),
            getPlaylistTracks(playlistId)
        ) { playlist, tracks ->
            if (playlist != null) {
                PlaylistWithTracks(playlist = playlist, tracks = tracks)
            } else {
                null
            }
        }
    }

    override suspend fun createPlaylist(name: String, description: String?): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(
            id = id,
            name = name.trim(),
            description = description?.trim(),
            createdAt = now,
            updatedAt = now
        )
        playlistDao.insertPlaylist(entity)
        return id
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        trackDao.upsert(TrackEntity.fromTrack(track))
        val currentMax = playlistDao.getMaxPosition(playlistId)
        val nextPos = if (currentMax >= 0) currentMax + 1 else 0
        val entry = PlaylistEntryEntity(
            playlistId = playlistId,
            trackId = track.id,
            position = nextPos
        )
        playlistDao.insertEntry(entry)
        playlistDao.updatePlaylistTimestamp(playlistId, System.currentTimeMillis())
    }

    override suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        trackDao.upsertAll(tracks.map { TrackEntity.fromTrack(it) })
        val currentMax = playlistDao.getMaxPosition(playlistId)
        var startPos = if (currentMax >= 0) currentMax + 1 else 0

        val entries = tracks.map { track ->
            PlaylistEntryEntity(
                playlistId = playlistId,
                trackId = track.id,
                position = startPos++
            )
        }
        playlistDao.insertEntries(entries)
        playlistDao.updatePlaylistTimestamp(playlistId, System.currentTimeMillis())
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        playlistDao.updatePlaylistTimestamp(playlistId, System.currentTimeMillis())
    }

    override suspend fun reorderTracks(playlistId: String, fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        val entries = playlistDao.getEntriesForPlaylist(playlistId).toMutableList()
        if (fromPosition !in entries.indices || toPosition !in entries.indices) return

        val moved = entries.removeAt(fromPosition)
        entries.add(toPosition, moved)

        val orderedIds = entries.map { it.id }
        playlistDao.reorderEntries(playlistId, orderedIds)
    }

    override suspend fun setPlaylistPinned(playlistId: String, isPinned: Boolean) {
        playlistDao.setPlaylistPinned(playlistId, isPinned)
    }
}
