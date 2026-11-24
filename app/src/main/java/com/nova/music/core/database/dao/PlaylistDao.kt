package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nova.music.core.database.entity.PlaylistEntity
import com.nova.music.core.database.entity.PlaylistEntryEntity
import com.nova.music.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithMetadata(
    val id: String,
    val name: String,
    val description: String?,
    val coverArtworkUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val trackCount: Int,
    val autoArtworkUrl: String?
)

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlists ORDER BY isPinned DESC, updatedAt DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT p.id, p.name, p.description, p.coverArtworkUrl, p.createdAt, p.updatedAt, p.isPinned,
               (SELECT COUNT(*) FROM playlist_entries pe WHERE pe.playlistId = p.id) as trackCount,
               (SELECT t.thumbnailUrl FROM playlist_entries pe INNER JOIN tracks t ON pe.trackId = t.id WHERE pe.playlistId = p.id ORDER BY pe.position ASC LIMIT 1) as autoArtworkUrl
        FROM playlists p
        ORDER BY p.isPinned DESC, p.updatedAt DESC
    """)
    fun getPlaylistsWithMetadata(): Flow<List<PlaylistWithMetadata>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistFlow(id: String): Flow<PlaylistEntity?>

    @Query("""
        SELECT DISTINCT t.thumbnailUrl FROM playlist_entries pe
        INNER JOIN tracks t ON pe.trackId = t.id
        WHERE pe.playlistId = :playlistId AND t.thumbnailUrl IS NOT NULL AND t.thumbnailUrl != ''
        ORDER BY pe.position ASC
        LIMIT 4
    """)
    suspend fun getTopArtworks(playlistId: String): List<String>

    @Query("""
        SELECT DISTINCT t.thumbnailUrl FROM playlist_entries pe
        INNER JOIN tracks t ON pe.trackId = t.id
        WHERE pe.playlistId = :playlistId AND t.thumbnailUrl IS NOT NULL AND t.thumbnailUrl != ''
        ORDER BY pe.position ASC
        LIMIT 4
    """)
    fun getTopArtworksFlow(playlistId: String): Flow<List<String>>

    @Query("SELECT trackId FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getTrackIdsFlow(playlistId: String): Flow<List<String>>

    @Query("SELECT trackId FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getTrackIds(playlistId: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_entries WHERE playlistId = :playlistId AND trackId = :trackId)")
    suspend fun containsTrack(playlistId: String, trackId: String): Boolean

    @Query("SELECT playlistId FROM playlist_entries WHERE trackId = :trackId")
    fun getPlaylistIdsContainingTrackFlow(trackId: String): Flow<List<String>>

    @Query("SELECT playlistId FROM playlist_entries WHERE trackId = :trackId")
    suspend fun getPlaylistIdsContainingTrack(trackId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PlaylistEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)

    @Query("DELETE FROM playlist_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND trackId IN (:trackIds)")
    suspend fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun clearEntriesForPlaylist(playlistId: String)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_entries pe ON t.id = pe.trackId
        WHERE pe.playlistId = :playlistId
        ORDER BY pe.position ASC
    """)
    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getEntriesForPlaylist(playlistId: String): List<PlaylistEntryEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int

    @Query("SELECT COUNT(*) FROM playlist_entries WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: String): Flow<Int>

    @Query("UPDATE playlists SET name = :name, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: String, name: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET name = :name, description = :description, coverArtworkUrl = :coverArtworkUrl, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun updatePlaylistDetails(playlistId: String, name: String, description: String?, coverArtworkUrl: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET coverArtworkUrl = :coverArtworkUrl, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun updatePlaylistCover(playlistId: String, coverArtworkUrl: String?, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun reorderEntries(playlistId: String, orderedEntryIds: List<Long>) {
        orderedEntryIds.forEachIndexed { index, entryId ->
            updateEntryPosition(entryId, index)
        }
        updatePlaylistTimestamp(playlistId, System.currentTimeMillis())
    }

    @Transaction
    suspend fun reorderEntriesByTrackIds(playlistId: String, orderedTrackIds: List<String>) {
        val entries = getEntriesForPlaylist(playlistId)
        val entryMap = entries.groupBy { it.trackId }.mapValues { it.value.toMutableList() }
        var position = 0
        for (trackId in orderedTrackIds) {
            val match = entryMap[trackId]?.removeFirstOrNull()
            if (match != null) {
                updateEntryPosition(match.id, position++)
            }
        }
        updatePlaylistTimestamp(playlistId, System.currentTimeMillis())
    }

    @Transaction
    suspend fun duplicatePlaylist(
        sourcePlaylistId: String,
        newPlaylistId: String,
        newName: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val source = getPlaylistById(sourcePlaylistId) ?: return
        val newPlaylist = source.copy(
            id = newPlaylistId,
            name = newName,
            createdAt = timestamp,
            updatedAt = timestamp,
            isPinned = false
        )
        insertPlaylist(newPlaylist)
        val sourceEntries = getEntriesForPlaylist(sourcePlaylistId)
        val newEntries = sourceEntries.map {
            it.copy(id = 0, playlistId = newPlaylistId, addedAt = timestamp)
        }
        insertEntries(newEntries)
    }

    @Transaction
    suspend fun recompactPositions(playlistId: String) {
        val entries = getEntriesForPlaylist(playlistId)
        entries.forEachIndexed { index, entry ->
            if (entry.position != index) {
                updateEntryPosition(entry.id, index)
            }
        }
        updatePlaylistTimestamp(playlistId, System.currentTimeMillis())
    }

    @Query("UPDATE playlist_entries SET position = :newPosition WHERE id = :entryId")
    suspend fun updateEntryPosition(entryId: Long, newPosition: Int)

    @Query("UPDATE playlists SET updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun updatePlaylistTimestamp(playlistId: String, timestamp: Long)

    @Query("UPDATE playlists SET isPinned = :isPinned, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun setPlaylistPinned(playlistId: String, isPinned: Boolean, timestamp: Long = System.currentTimeMillis())
}
