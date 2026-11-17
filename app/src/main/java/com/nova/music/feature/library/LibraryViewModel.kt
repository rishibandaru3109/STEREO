package com.nova.music.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.domain.model.Playlist
import com.nova.music.domain.model.PlaylistWithTracks
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.HistoryRepository
import com.nova.music.domain.repository.LikedSongsRepository
import com.nova.music.domain.repository.LikedSortOrder
import com.nova.music.domain.repository.PlaylistRepository
import com.nova.music.playback.NovaPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val likedSongsRepository: LikedSongsRepository,
    private val playlistRepository: PlaylistRepository,
    private val historyRepository: HistoryRepository,
    private val playerController: NovaPlayerController
) : ViewModel() {

    private val _likedSortOrder = MutableStateFlow(LikedSortOrder.RECENTLY_ADDED)
    val likedSortOrder: StateFlow<LikedSortOrder> = _likedSortOrder.asStateFlow()

    private val _likedSearchQuery = MutableStateFlow("")
    val likedSearchQuery: StateFlow<String> = _likedSearchQuery.asStateFlow()

    val likedSongs: StateFlow<List<Track>> = combine(
        _likedSortOrder,
        _likedSearchQuery
    ) { sortOrder, query ->
        Pair(sortOrder, query)
    }.flatMapLatest { (sortOrder, query) ->
        if (query.isBlank()) {
            likedSongsRepository.getLikedSongs(sortOrder)
        } else {
            likedSongsRepository.searchLikedSongs(query)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val likedCount: StateFlow<Int> = likedSongsRepository.getLikedCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val likedTrackIds: StateFlow<List<String>> = likedSongsRepository.getAllLikedTrackIds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentTracks: StateFlow<List<Track>> = historyRepository.getRecentTracks(limit = 20)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    val selectedPlaylistWithTracks: StateFlow<PlaylistWithTracks?> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id != null) {
                playlistRepository.getPlaylistWithTracks(id)
            } else {
                kotlinx.coroutines.flow.flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setLikedSortOrder(order: LikedSortOrder) {
        _likedSortOrder.value = order
    }

    fun setLikedSearchQuery(query: String) {
        _likedSearchQuery.value = query
    }

    fun selectPlaylist(id: String?) {
        _selectedPlaylistId.value = id
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            likedSongsRepository.toggleLike(track)
        }
    }

    fun createPlaylist(name: String, description: String? = null, onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(name, description)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(id, newName)
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(id)
            if (_selectedPlaylistId.value == id) {
                _selectedPlaylistId.value = null
            }
        }
    }

    fun setPlaylistPinned(id: String, isPinned: Boolean) {
        viewModelScope.launch {
            playlistRepository.setPlaylistPinned(id, isPinned)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun reorderPlaylistTracks(playlistId: String, fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            playlistRepository.reorderTracks(playlistId, fromPosition, toPosition)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun playTrack(track: Track) {
        playerController.playTrack(track)
        recordPlay(track, "library_direct")
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        playerController.setQueue(tracks, startIndex)
        if (shuffle) {
            playerController.toggleShuffle()
        }
        val target = tracks.getOrNull(startIndex) ?: tracks[0]
        recordPlay(target, "library_playlist")
    }

    fun playNext(track: Track) {
        playerController.playNext(track)
    }

    fun addToQueue(track: Track) {
        playerController.addToQueue(track)
    }

    private fun recordPlay(track: Track, context: String) {
        viewModelScope.launch {
            historyRepository.recordPlayback(
                track = track,
                durationMs = 5000L,
                totalDurationMs = track.durationSeconds * 1000L,
                isSkipped = false,
                playbackContext = context
            )
        }
    }
}
