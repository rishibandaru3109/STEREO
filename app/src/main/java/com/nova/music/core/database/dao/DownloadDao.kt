package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nova.music.core.database.entity.DownloadEntity
import com.nova.music.core.database.entity.PlaylistDownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun observeCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING') ORDER BY createdAt ASC")
    fun observeActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING') ORDER BY createdAt ASC")
    suspend fun getActiveDownloads(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'FAILED' ORDER BY createdAt DESC")
    fun observeFailedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE trackId = :trackId LIMIT 1")
    suspend fun getDownload(trackId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE trackId = :trackId LIMIT 1")
    fun observeDownload(trackId: String): Flow<DownloadEntity?>

    @Query("SELECT trackId FROM downloads WHERE status = 'COMPLETED'")
    fun observeDownloadedTrackIds(): Flow<List<String>>

    @Query("SELECT trackId FROM downloads WHERE status = 'COMPLETED'")
    suspend fun getDownloadedTrackIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :downloadedBytes, totalBytes = :totalBytes WHERE trackId = :trackId")
    suspend fun updateProgress(trackId: String, status: String, downloadedBytes: Long, totalBytes: Long)

    @Query("UPDATE downloads SET status = :status, downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, filePath = :filePath, completedAt = :completedAt, errorMessage = null WHERE trackId = :trackId")
    suspend fun markCompleted(trackId: String, status: String, downloadedBytes: Long, totalBytes: Long, filePath: String, completedAt: Long)

    @Query("UPDATE downloads SET status = 'FAILED', errorMessage = :errorMessage WHERE trackId = :trackId")
    suspend fun markFailed(trackId: String, errorMessage: String)

    @Query("DELETE FROM downloads WHERE trackId = :trackId")
    suspend fun deleteDownload(trackId: String)

    @Query("DELETE FROM downloads WHERE status = 'FAILED'")
    suspend fun clearFailedDownloads()

    @Query("DELETE FROM downloads")
    suspend fun deleteAllDownloads()

    @Query("SELECT SUM(downloadedBytes) FROM downloads WHERE status = 'COMPLETED'")
    fun observeTotalStorageBytes(): Flow<Long?>

    // Playlist download associations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistDownload(entity: PlaylistDownloadEntity)

    @Query("SELECT d.* FROM downloads d INNER JOIN playlist_downloads pd ON d.trackId = pd.trackId WHERE pd.playlistId = :playlistId")
    fun observePlaylistDownloads(playlistId: String): Flow<List<DownloadEntity>>

    @Query("SELECT d.* FROM downloads d INNER JOIN playlist_downloads pd ON d.trackId = pd.trackId WHERE pd.playlistId = :playlistId")
    suspend fun getPlaylistDownloads(playlistId: String): List<DownloadEntity>

    @Query("SELECT DISTINCT playlistId FROM playlist_downloads")
    fun observeDownloadedPlaylistIds(): Flow<List<String>>

    @Query("DELETE FROM playlist_downloads WHERE playlistId = :playlistId")
    suspend fun deletePlaylistDownloads(playlistId: String)
}
