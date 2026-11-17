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

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistFlow(id: String): Flow<PlaylistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PlaylistEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)

    @Query("DELETE FROM playlist_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

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

    @Transaction
    suspend fun reorderEntries(playlistId: String, orderedEntryIds: List<Long>) {
        orderedEntryIds.forEachIndexed { index, entryId ->
            updateEntryPosition(entryId, index)
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
