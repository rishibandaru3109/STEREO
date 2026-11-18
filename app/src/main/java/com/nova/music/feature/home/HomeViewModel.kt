package com.nova.music.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.RecommendationEligibility
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import com.nova.music.domain.recommendation.SurpriseMeEngine
import com.nova.music.domain.recommendation.SurpriseMeEngineImpl
import com.nova.music.domain.recommendation.SurpriseResult
import com.nova.music.domain.repository.RecommendationPreferences
import com.nova.music.domain.repository.HomeRepository
import com.nova.music.domain.repository.LikedSongsRepository
import com.nova.music.domain.repository.ListeningProfileRepository
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.playback.NovaPlayerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeFeedState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val sections: List<RecommendationSection> = emptyList(),
    val currentlyPlayingTrackId: String? = null,
    val errorMessage: String? = null,
    val isSurpriseMeLoading: Boolean = false,
    val userMessage: String? = null
)

class HomeViewModel @JvmOverloads constructor(
    private val homeRepository: HomeRepository,
    private val playerController: NovaPlayerController,
    private val likedSongsRepository: LikedSongsRepository,
    private val preferencesRepository: MusicPreferencesRepository? = null,
    private val surpriseMeEngine: SurpriseMeEngine? = null,
    private val listeningProfileRepository: ListeningProfileRepository? = null
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

    private var feedJob: Job? = null
    private var surpriseJob: Job? = null

    init {
        if (preferencesRepository == null) {
            loadHomeFeed()
        } else {
            viewModelScope.launch {
                preferencesRepository.recommendationPreferences.distinctUntilChanged().collect {
                    surpriseJob?.cancel()
                    _uiState.update { state -> state.copy(sections = emptyList(), userMessage = null) }
                    loadHomeFeed()
                }
            }
        }
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
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            _uiState.update { current ->
                if (forceRefresh) current.copy(isRefreshing = true)
                else current.copy(isLoading = current.sections.isEmpty(), isRefreshing = false)
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

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun triggerSurpriseMe() {
        if (surpriseJob?.isActive == true) return
        surpriseJob = viewModelScope.launch {
            _uiState.update { it.copy(isSurpriseMeLoading = true) }
            try {
                val profile = listeningProfileRepository?.getListeningProfile() ?: ListeningProfile()
                val excluded = listeningProfileRepository?.getExcludedTrackIds().orEmpty() +
                    profile.topTracks.filter { it.score < 0 }.map { it.trackId }
                val preferences = preferencesRepository?.recommendationPreferences?.first()
                    ?: RecommendationPreferences()
                val feedCandidates = _uiState.value.sections.flatMap { section ->
                    section.tracks + section.mixes.flatMap { it.tracks }
                }.filter { RecommendationEligibility.accepts(it, preferences) }

                val candidates = feedCandidates.ifEmpty {
                    val sections = homeRepository.getHomeFeed(forceRefresh = false).getOrDefault(emptyList())
                    sections.flatMap { it.tracks + it.mixes.flatMap { mix -> mix.tracks } }
                }.filter { RecommendationEligibility.accepts(it, preferences) }

                val languageById = candidates.associateBy { it.id }
                val fallbackLiked = likedSongsRepository.getLikedSongs().first()
                    .map { languageById[it.id] ?: it }
                    .filter { RecommendationEligibility.accepts(it, preferences) }

                val engine = surpriseMeEngine ?: SurpriseMeEngineImpl(RecommendationEngineImpl())
                val result = engine.pickSurpriseTrack(
                    profile = profile,
                    candidates = candidates,
                    fallbackTracks = fallbackLiked,
                    excludedTrackIds = excluded,
                    currentlyPlayingId = playerController.playbackState.value.currentTrack?.id
                )

                when (result) {
                    is SurpriseResult.Success -> {
                        playerController.setQueue(result.queueTracks, startIndex = 0)
                        playerController.play()
                    }
                    is SurpriseResult.Fallback -> {
                        playerController.setQueue(result.queueTracks, startIndex = 0)
                        playerController.play()
                        _uiState.update { it.copy(userMessage = result.message) }
                    }
                    is SurpriseResult.Unavailable -> {
                        _uiState.update { it.copy(userMessage = result.message) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(userMessage = "Could not select surprise music right now") }
            } finally {
                _uiState.update { it.copy(isSurpriseMeLoading = false) }
            }
        }
    }

    fun playTrack(track: Track, contextTracks: List<Track> = listOf(track)) {
        val startIndex = contextTracks.indexOfFirst { it.id == track.id }
        if (startIndex < 0) playerController.playTrack(track)
        else playerController.setQueue(contextTracks, startIndex = startIndex)
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
            surpriseJob?.cancel()
            _uiState.update { state ->
                state.copy(sections = state.sections.map { section ->
                    section.copy(
                        tracks = section.tracks.filterNot { it.id == track.id },
                        mixes = section.mixes.map {
                            it.copy(tracks = it.tracks.filterNot { candidate -> candidate.id == track.id })
                        }.filter { it.tracks.isNotEmpty() }
                    )
                }.filter { it.tracks.isNotEmpty() || it.mixes.isNotEmpty() })
            }
            // Refresh feed to remove candidate
            loadHomeFeed(forceRefresh = true)
        }
    }

    fun completeOnboarding(languages: Set<String>, genres: Set<String>, discoveryMode: String) {
        viewModelScope.launch {
            preferencesRepository?.saveRecommendationPreferences(languages, genres, discoveryMode)
        }
    }
}
