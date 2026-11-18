package com.nova.music.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

enum class ContentFilterMode {
    MUSIC_ONLY,
    ALL_CONTENT
}

data class RecommendationPreferences(
    val languages: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val discoveryMode: String = "Familiar",
    val contentFilterMode: ContentFilterMode = ContentFilterMode.MUSIC_ONLY
)

interface MusicPreferencesRepository {
    val recommendationPreferences: Flow<RecommendationPreferences>
        get() = combine(preferredLanguages, preferredGenres, discoveryMode, contentFilterMode, isOnboardingCompleted) {
                languages, genres, mode, filter, configured ->
            RecommendationPreferences(if (configured) languages else emptySet(), genres, mode, filter)
        }

    suspend fun saveRecommendationPreferences(languages: Set<String>, genres: Set<String>, mode: String) {
        setPreferredLanguages(languages)
        setPreferredGenres(genres)
        setDiscoveryMode(mode)
        setOnboardingCompleted(true)
    }

    val preferredLanguages: Flow<Set<String>>
    val preferredGenres: Flow<Set<String>>
    val discoveryMode: Flow<String> // "Familiar" or "Adventurous"
    val isOnboardingCompleted: Flow<Boolean>
    val isHistoryEnabled: Flow<Boolean>
    val contentFilterMode: Flow<ContentFilterMode>

    suspend fun setPreferredLanguages(languages: Set<String>)
    suspend fun setPreferredGenres(genres: Set<String>)
    suspend fun setDiscoveryMode(mode: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setHistoryEnabled(enabled: Boolean)
    suspend fun setContentFilterMode(mode: ContentFilterMode)
    suspend fun resetRecommendationPreferences()
}
