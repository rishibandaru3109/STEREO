package com.nova.music.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.playback.NovaPlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val tracks: List<Track>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class SearchScreenState(
    val query: String = "",
    val searchUiState: SearchUiState = SearchUiState.Idle,
    val currentlyPlayingTrackId: String? = null,
    val isRefreshing: Boolean = false
)

class SearchViewModel(
    private val musicRepository: MusicRepository,
    private val playerController: NovaPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchScreenState())
    val uiState: StateFlow<SearchScreenState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Observe playback state to highlight the currently playing song in the list
        viewModelScope.launch {
            playerController.playbackState.collect { playbackState ->
                _uiState.update {
                    it.copy(currentlyPlayingTrackId = playbackState.currentTrack?.id)
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isRefreshing = false) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update { it.copy(searchUiState = SearchUiState.Idle) }
            return
        }

        // Debounce search
        searchJob = viewModelScope.launch {
            delay(500)
            executeSearch(newQuery)
        }
    }

    fun onSearchSubmitted() {
        searchJob?.cancel()
        val q = _uiState.value.query.trim()
        if (q.isNotEmpty()) {
            searchJob = viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = false) }
                executeSearch(q)
            }
        }
    }

    private suspend fun executeSearch(query: String) {
        _uiState.update { it.copy(searchUiState = SearchUiState.Loading) }
        musicRepository.search(query).collect { result ->
            result.onSuccess { tracks ->
                _uiState.update {
                    it.copy(
                        searchUiState = if (tracks.isEmpty()) SearchUiState.Empty else SearchUiState.Success(tracks)
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        searchUiState = SearchUiState.Error(error.localizedMessage ?: "Failed to search music")
                    )
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        val q = _uiState.value.query.trim()
        searchJob?.cancel()
        if (q.isNotEmpty()) {
            searchJob = viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                musicRepository.search(q).collect { result ->
                    result.onSuccess { tracks ->
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                searchUiState = if (tracks.isEmpty()) SearchUiState.Empty else SearchUiState.Success(tracks)
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                searchUiState = SearchUiState.Error(error.localizedMessage ?: "Failed to refresh search results")
                            )
                        }
                    }
                }
            }
        } else {
            searchJob = viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                delay(600)
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onTrackSelected(track: Track) {
        val currentState = _uiState.value.searchUiState
        if (currentState is SearchUiState.Success) {
            val tracks = currentState.tracks
            val index = tracks.indexOf(track)
            if (index >= 0) playerController.setQueue(tracks, index)
            else playerController.playTrack(track)
        } else {
            playerController.playTrack(track)
        }
    }
}
