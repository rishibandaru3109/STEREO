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
                val topArtworks = tracks.mapNotNull { it.thumbnailUrl.takeIf { u -> u.isNotBlank() } }
                    .distinct()
                    .take(4)
                PlaylistWithTracks(
                    playlist = playlist.copy(topArtworkUrls = topArtworks),
                    tracks = tracks
                )
            } else {
                null
            }
        }
    }

    override fun getPlaylistTrackIds(playlistId: String): Flow<Set<String>> {
        return playlistDao.getTrackIdsFlow(playlistId).map { it.toSet() }
    }

    override fun getPlaylistIdsContainingTrack(trackId: String): Flow<Set<String>> {
        return playlistDao.getPlaylistIdsContainingTrackFlow(trackId).map { it.toSet() }
    }

    override suspend fun createPlaylist(name: String, description: String?, coverArtworkUrl: String?): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(
            id = id,
            name = name.trim(),
            description = description?.trim(),
            coverArtworkUrl = coverArtworkUrl?.trim(),
            createdAt = now,
            updatedAt = now
        )
        playlistDao.insertPlaylist(entity)
        return id
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    override suspend fun updatePlaylistDetails(
        playlistId: String,
        name: String,
        description: String?,
        coverArtworkUrl: String?
    ) {
        playlistDao.updatePlaylistDetails(
            playlistId = playlistId,
            name = name.trim(),
            description = description?.trim(),
            coverArtworkUrl = coverArtworkUrl?.trim(),
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun updatePlaylistCover(playlistId: String, coverArtworkUrl: String?) {
        playlistDao.updatePlaylistCover(playlistId, coverArtworkUrl?.trim(), System.currentTimeMillis())
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun duplicatePlaylist(playlistId: String, newName: String?): String {
        val source = playlistDao.getPlaylistById(playlistId) ?: return ""
        val targetName = newName?.trim() ?: "${source.name} (Copy)"
        val newId = UUID.randomUUID().toString()
        playlistDao.duplicatePlaylist(
            sourcePlaylistId = playlistId,
            newPlaylistId = newId,
            newName = targetName
        )
        return newId
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        addTrackToPlaylist(playlistId, track, allowDuplicates = false)
    }

    override suspend fun addTrackToPlaylist(
        playlistId: String,
        track: Track,
        allowDuplicates: Boolean
    ): Boolean {
        if (!allowDuplicates && playlistDao.containsTrack(playlistId, track.id)) {
            return false
        }
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
        return true
    }

    override suspend fun addTracksToPlaylist(playlistId: String, tracks: List<Track>) {
        addTracksToPlaylist(playlistId, tracks, allowDuplicates = false)
    }

    override suspend fun addTracksToPlaylist(
        playlistId: String,
        tracks: List<Track>,
        allowDuplicates: Boolean
    ): Int {
        if (tracks.isEmpty()) return 0
        val existingTrackIds = if (!allowDuplicates) {
            playlistDao.getTrackIds(playlistId).toSet()
        } else {
            emptySet()
        }
        val tracksToAdd = if (!allowDuplicates) {
            tracks.filterNot { it.id in existingTrackIds }
        } else {
            tracks
        }
        if (tracksToAdd.isEmpty()) return 0

        trackDao.upsertAll(tracksToAdd.map { TrackEntity.fromTrack(it) })
        val currentMax = playlistDao.getMaxPosition(playlistId)
        var startPos = if (currentMax >= 0) currentMax + 1 else 0

        val entries = tracksToAdd.map { track ->
            PlaylistEntryEntity(
                playlistId = playlistId,
                trackId = track.id,
                position = startPos++
            )
        }
        playlistDao.insertEntries(entries)
        playlistDao.updatePlaylistTimestamp(playlistId, System.currentTimeMillis())
        return tracksToAdd.size
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        playlistDao.recompactPositions(playlistId)
    }

    override suspend fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>) {
        if (trackIds.isEmpty()) return
        playlistDao.removeTracksFromPlaylist(playlistId, trackIds)
        playlistDao.recompactPositions(playlistId)
    }

    override suspend fun moveTracksBetweenPlaylists(
        sourcePlaylistId: String,
        targetPlaylistId: String,
        trackIds: List<String>
    ) {
        if (trackIds.isEmpty() || sourcePlaylistId == targetPlaylistId) return
        val tracks = trackDao.getTracksByIds(trackIds).map { it.toTrack() }
        addTracksToPlaylist(targetPlaylistId, tracks, allowDuplicates = false)
        removeTracksFromPlaylist(sourcePlaylistId, trackIds)
    }

    override suspend fun copyTracksToPlaylist(targetPlaylistId: String, trackIds: List<String>) {
        if (trackIds.isEmpty()) return
        val tracks = trackDao.getTracksByIds(trackIds).map { it.toTrack() }
        addTracksToPlaylist(targetPlaylistId, tracks, allowDuplicates = false)
    }

    override suspend fun clearPlaylist(playlistId: String) {
        playlistDao.clearEntriesForPlaylist(playlistId)
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

    override suspend fun reorderAllTracks(playlistId: String, orderedTrackIds: List<String>) {
        playlistDao.reorderEntriesByTrackIds(playlistId, orderedTrackIds)
    }

    override suspend fun setPlaylistPinned(playlistId: String, isPinned: Boolean) {
        playlistDao.setPlaylistPinned(playlistId, isPinned)
    }
}
