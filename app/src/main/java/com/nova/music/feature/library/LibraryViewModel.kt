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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlaylistSortOrder {
    CUSTOM,
    RECENTLY_ADDED,
    OLDEST_ADDED,
    TITLE_AZ,
    TITLE_ZA,
    ARTIST_AZ,
    ARTIST_ZA,
    DURATION
}

class LibraryViewModel(
    private val likedSongsRepository: LikedSongsRepository,
    private val playlistRepository: PlaylistRepository,
    private val historyRepository: HistoryRepository,
    private val playerController: NovaPlayerController,
    private val musicRepository: com.nova.music.domain.repository.MusicRepository? = null,
    private val synapseDao: com.nova.music.core.database.dao.SynapseDao? = null,
    private val downloadRepository: com.nova.music.domain.repository.DownloadRepository? = null
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

    fun getPlaylistsContainingTrack(trackId: String): Flow<Set<String>> {
        return playlistRepository.getPlaylistIdsContainingTrack(trackId)
    }

    val recentTracks: StateFlow<List<Track>> = historyRepository.getRecentTracks(limit = 20)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(400)
            _isRefreshing.value = false
        }
    }

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

    private val _playlistSortOrder = MutableStateFlow(PlaylistSortOrder.CUSTOM)
    val playlistSortOrder: StateFlow<PlaylistSortOrder> = _playlistSortOrder.asStateFlow()

    private val _playlistSearchQuery = MutableStateFlow("")
    val playlistSearchQuery: StateFlow<String> = _playlistSearchQuery.asStateFlow()

    val displayedPlaylistTracks: StateFlow<List<Track>> = combine(
        selectedPlaylistWithTracks,
        _playlistSortOrder,
        _playlistSearchQuery
    ) { playlistWithTracks, sortOrder, query ->
        val rawTracks = playlistWithTracks?.tracks ?: emptyList()
        val filtered = if (query.isBlank()) {
            rawTracks
        } else {
            val q = query.trim().lowercase()
            rawTracks.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
        }
        when (sortOrder) {
            PlaylistSortOrder.CUSTOM -> filtered
            PlaylistSortOrder.RECENTLY_ADDED -> filtered.reversed()
            PlaylistSortOrder.OLDEST_ADDED -> filtered
            PlaylistSortOrder.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
            PlaylistSortOrder.TITLE_ZA -> filtered.sortedByDescending { it.title.lowercase() }
            PlaylistSortOrder.ARTIST_AZ -> filtered.sortedBy { it.artist.lowercase() }
            PlaylistSortOrder.ARTIST_ZA -> filtered.sortedByDescending { it.artist.lowercase() }
            PlaylistSortOrder.DURATION -> filtered.sortedByDescending { it.durationSeconds }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _refreshSuggestionsTrigger = MutableStateFlow(0)

    val suggestedTracksForPlaylist: StateFlow<List<Track>> = combine(
        selectedPlaylistWithTracks,
        _refreshSuggestionsTrigger
    ) { playlistWithTracks, _ ->
        playlistWithTracks
    }.flatMapLatest { playlistWithTracks ->
        flow {
            if (playlistWithTracks == null || musicRepository == null) {
                emit(emptyList<Track>())
                return@flow
            }
            val existingTrackIds = playlistWithTracks.tracks.map { it.id }.toSet()
            val artists = playlistWithTracks.tracks.map { it.artist }.filter { it.isNotBlank() }.distinct()
            val searchQueries = if (artists.isNotEmpty()) {
                artists.shuffled().take(3)
            } else {
                recentTracks.value.map { it.artist }.filter { it.isNotBlank() }.distinct().take(2)
            }
            if (searchQueries.isEmpty()) {
                emit(emptyList<Track>())
                return@flow
            }
            val collectedTracks = mutableListOf<Track>()
            for (query in searchQueries) {
                try {
                    val result = musicRepository.search(query).firstOrNull()?.getOrNull() ?: emptyList()
                    for (track in result) {
                        if (!existingTrackIds.contains(track.id) && collectedTracks.none { it.id == track.id }) {
                            collectedTracks.add(track)
                        }
                    }
                } catch (_: Throwable) {
                    // Ignore network failure for background suggestions
                }
            }
            emit(collectedTracks.take(8))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshSuggestedTracks() {
        _refreshSuggestionsTrigger.value++
    }

    fun setLikedSortOrder(order: LikedSortOrder) {
        _likedSortOrder.value = order
    }

    fun setLikedSearchQuery(query: String) {
        _likedSearchQuery.value = query
    }

    fun setPlaylistSortOrder(order: PlaylistSortOrder) {
        _playlistSortOrder.value = order
    }

    fun setPlaylistSearchQuery(query: String) {
        _playlistSearchQuery.value = query
    }

    fun selectPlaylist(id: String?) {
        _selectedPlaylistId.value = id
        _playlistSearchQuery.value = ""
        _playlistSortOrder.value = PlaylistSortOrder.CUSTOM
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            likedSongsRepository.toggleLike(track)
        }
    }

    fun createPlaylist(
        name: String,
        description: String? = null,
        coverArtworkUrl: String? = null,
        onCreated: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(name, description, coverArtworkUrl)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(id, newName)
        }
    }

    fun updatePlaylistDetails(id: String, name: String, description: String?, coverArtworkUrl: String?) {
        viewModelScope.launch {
            playlistRepository.updatePlaylistDetails(id, name, description, coverArtworkUrl)
        }
    }

    fun duplicatePlaylist(id: String, onDuplicated: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val newId = playlistRepository.duplicatePlaylist(id)
            if (newId.isNotBlank()) {
                onDuplicated?.invoke(newId)
            }
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

    fun addTrackToPlaylist(
        playlistId: String,
        track: Track,
        allowDuplicates: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val added = playlistRepository.addTrackToPlaylist(playlistId, track, allowDuplicates)
            if (added) {
                try {
                    synapseDao?.insertEvent(
                        com.nova.music.core.database.entity.SynapseEventEntity(
                            eventId = java.util.UUID.randomUUID().toString(),
                            trackId = track.id,
                            timestamp = System.currentTimeMillis(),
                            listeningDurationMs = 180_000L,
                            totalTrackDurationMs = 180_000L,
                            completionRatio = 1.0f,
                            isManualSkip = false,
                            playbackContext = "playlist_add"
                        )
                    )
                } catch (_: Throwable) {}
            }
            onComplete?.invoke(added)
        }
    }

    fun addTracksToPlaylist(
        playlistId: String,
        tracks: List<Track>,
        allowDuplicates: Boolean = false,
        onComplete: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val count = playlistRepository.addTracksToPlaylist(playlistId, tracks, allowDuplicates)
            if (count > 0) {
                try {
                    for (track in tracks) {
                        synapseDao?.insertEvent(
                            com.nova.music.core.database.entity.SynapseEventEntity(
                                eventId = java.util.UUID.randomUUID().toString(),
                                trackId = track.id,
                                timestamp = System.currentTimeMillis(),
                                listeningDurationMs = 180_000L,
                                totalTrackDurationMs = 180_000L,
                                completionRatio = 1.0f,
                                isManualSkip = false,
                                playbackContext = "playlist_add"
                            )
                        )
                    }
                } catch (_: Throwable) {}
            }
            onComplete?.invoke(count)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>) {
        viewModelScope.launch {
            playlistRepository.removeTracksFromPlaylist(playlistId, trackIds)
        }
    }

    fun moveTracksBetweenPlaylists(sourcePlaylistId: String, targetPlaylistId: String, trackIds: List<String>) {
        viewModelScope.launch {
            playlistRepository.moveTracksBetweenPlaylists(sourcePlaylistId, targetPlaylistId, trackIds)
        }
    }

    fun copyTracksToPlaylist(targetPlaylistId: String, trackIds: List<String>) {
        viewModelScope.launch {
            playlistRepository.copyTracksToPlaylist(targetPlaylistId, trackIds)
        }
    }

    fun clearPlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.clearPlaylist(playlistId)
        }
    }

    fun reorderPlaylistTracks(playlistId: String, fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            playlistRepository.reorderTracks(playlistId, fromPosition, toPosition)
        }
    }

    fun reorderAllTracks(playlistId: String, orderedTrackIds: List<String>) {
        viewModelScope.launch {
            playlistRepository.reorderAllTracks(playlistId, orderedTrackIds)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun playTrack(track: Track) {
        playerController.playTrack(track)
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        playerController.setQueue(tracks, startIndex)
        if (shuffle && !playerController.queueSnapshot.value.isShuffle) {
            playerController.toggleShuffle()
        }
    }

    fun playNext(track: Track) {
        playerController.playNext(track)
    }

    fun addToQueue(track: Track) {
        playerController.addToQueue(track)
    }

    fun appendTracksToQueue(tracks: List<Track>) {
        tracks.forEach { playerController.addToQueue(it) }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository?.downloadTrack(track)
        }
    }

    fun downloadPlaylist(playlistId: String, tracks: List<Track>) {
        viewModelScope.launch {
            downloadRepository?.downloadPlaylist(playlistId, tracks)
        }
    }
}
