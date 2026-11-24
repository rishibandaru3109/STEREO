package com.nova.music.domain.repository

import com.nova.music.domain.model.AudioQualityMode
import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.DownloadItem
import com.nova.music.domain.model.StorageUsage
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    val completedDownloads: Flow<List<DownloadItem>>
    val activeDownloads: Flow<List<DownloadItem>>
    val failedDownloads: Flow<List<DownloadItem>>
    val allDownloads: Flow<List<DownloadItem>>
    val storageUsage: Flow<StorageUsage>
    val downloadedTrackIds: Flow<Set<String>>

    fun getDownloadItem(trackId: String): Flow<DownloadItem?>
    suspend fun downloadTrack(track: Track, quality: AudioQualityMode = AudioQualityMode.HIGH)
    suspend fun downloadTracks(tracks: List<Track>, quality: AudioQualityMode = AudioQualityMode.HIGH)
    suspend fun downloadPlaylist(playlistId: String, tracks: List<Track>, quality: AudioQualityMode = AudioQualityMode.HIGH)
    suspend fun cancelDownload(trackId: String)
    suspend fun retryDownload(trackId: String)
    suspend fun deleteDownload(trackId: String)
    suspend fun clearFailedDownloads()
    suspend fun deleteAllDownloads()
    suspend fun getCompletedDownloadStream(trackId: String): AudioStream?
    suspend fun isTrackDownloaded(trackId: String): Boolean
}
