package com.nova.music.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.HomeRepository
import com.nova.music.domain.repository.LikedSongsRepository
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.playback.NovaPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeFeedState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val sections: List<RecommendationSection> = emptyList(),
    val currentlyPlayingTrackId: String? = null,
    val errorMessage: String? = null
)

class HomeViewModel @JvmOverloads constructor(
    private val homeRepository: HomeRepository,
    private val playerController: NovaPlayerController,
    private val likedSongsRepository: LikedSongsRepository,
    private val preferencesRepository: MusicPreferencesRepository? = null
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean> = preferencesRepository?.isOnboardingCompleted
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, true)
        ?: MutableStateFlow(true)

    private val _uiState = MutableStateFlow(HomeFeedState(isLoading = true))
    val uiState: StateFlow<HomeFeedState> = _uiState.asStateFlow()

    val likedTrackIds: StateFlow<Set<String>> = likedSongsRepository.getAllLikedTrackIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    init {
        loadHomeFeed(forceRefresh = false)
        observePlaybackState()
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            playerController.playbackState.collect { playbackState ->
                _uiState.update { current ->
                    current.copy(currentlyPlayingTrackId = playbackState.currentTrack?.id)
                }
            }
        }
    }

    fun loadHomeFeed(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { current ->
                if (forceRefresh) current.copy(isRefreshing = true)
                else current.copy(isLoading = current.sections.isEmpty())
            }

            homeRepository.getHomeFeed(forceRefresh)
                .onSuccess { feedSections ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            sections = feedSections,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "Failed to load music feed"
                        )
                    }
                }
        }
    }

    fun playTrack(track: Track, contextTracks: List<Track> = listOf(track)) {
        val startIndex = contextTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerController.setQueue(contextTracks, startIndex = startIndex)
        playerController.play()
    }

    fun playMix(mix: RecommendationMix) {
        if (mix.tracks.isNotEmpty()) {
            playerController.setQueue(mix.tracks, startIndex = 0)
            playerController.play()
        }
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            likedSongsRepository.toggleLike(track)
        }
    }

    fun notInterested(track: Track) {
        viewModelScope.launch {
            homeRepository.recordExplicitFeedback(track.id, "NOT_INTERESTED")
            // Refresh feed to remove candidate
            loadHomeFeed(forceRefresh = true)
        }
    }

    fun completeOnboarding(languages: Set<String>, genres: Set<String>, discoveryMode: String) {
        viewModelScope.launch {
            preferencesRepository?.apply {
                setPreferredLanguages(languages)
                setPreferredGenres(genres)
                setDiscoveryMode(discoveryMode)
                setOnboardingCompleted(true)
            }
            loadHomeFeed(forceRefresh = true)
        }
    }
}
