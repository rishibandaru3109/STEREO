package com.nova.music.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.domain.model.DownloadItem
import com.nova.music.domain.model.DownloadStatus
import com.nova.music.domain.model.StorageUsage
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.DownloadRepository
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.playback.NovaPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadSortOrder {
    RECENTLY_DOWNLOADED,
    TITLE_AZ,
    ARTIST_AZ,
    FILE_SIZE
}

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository,
    private val playerController: NovaPlayerController,
    private val preferencesRepository: MusicPreferencesRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(DownloadSortOrder.RECENTLY_DOWNLOADED)
    val sortOrder: StateFlow<DownloadSortOrder> = _sortOrder.asStateFlow()

    val storageUsage: StateFlow<StorageUsage> = downloadRepository.storageUsage
        .stateIn(viewModelScope, SharingStarted.Eagerly, StorageUsage(0L, "0 MB", 0))

    val downloadedTrackIds: StateFlow<Set<String>> = downloadRepository.downloadedTrackIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val activeDownloads: StateFlow<List<DownloadItem>> = downloadRepository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val failedDownloads: StateFlow<List<DownloadItem>> = downloadRepository.failedDownloads
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val completedDownloads: StateFlow<List<DownloadItem>> = combine(
        downloadRepository.completedDownloads,
        _searchQuery,
        _sortOrder
    ) { items, query, sort ->
        val filtered = if (query.isBlank()) {
            items
        } else {
            val q = query.trim().lowercase()
            items.filter {
                it.track.title.lowercase().contains(q) || it.track.artist.lowercase().contains(q)
            }
        }
        when (sort) {
            DownloadSortOrder.RECENTLY_DOWNLOADED -> filtered.sortedByDescending { it.completedAt ?: it.createdAt }
            DownloadSortOrder.TITLE_AZ -> filtered.sortedBy { it.track.title.lowercase() }
            DownloadSortOrder.ARTIST_AZ -> filtered.sortedBy { it.track.artist.lowercase() }
            DownloadSortOrder.FILE_SIZE -> filtered.sortedByDescending { it.downloadedBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: DownloadSortOrder) {
        _sortOrder.value = order
    }

    fun playTrack(track: Track) {
        playerController.playTrack(track)
    }

    fun playAll(shuffle: Boolean = false) {
        val tracks = completedDownloads.value.map { it.track }
        if (tracks.isEmpty()) return
        playerController.setQueue(tracks, 0)
        if (shuffle && !playerController.queueSnapshot.value.isShuffle) {
            playerController.toggleShuffle()
        }
    }

    fun deleteDownload(trackId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(trackId)
        }
    }

    fun retryDownload(trackId: String) {
        viewModelScope.launch {
            downloadRepository.retryDownload(trackId)
        }
    }

    fun cancelDownload(trackId: String) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(trackId)
        }
    }

    fun clearFailedDownloads() {
        viewModelScope.launch {
            downloadRepository.clearFailedDownloads()
        }
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            downloadRepository.deleteAllDownloads()
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository.downloadTrack(track)
        }
    }

    fun downloadPlaylist(playlistId: String, tracks: List<Track>) {
        viewModelScope.launch {
            downloadRepository.downloadPlaylist(playlistId, tracks)
        }
    }
}
