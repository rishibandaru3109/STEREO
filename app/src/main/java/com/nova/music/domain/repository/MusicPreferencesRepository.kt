package com.nova.music.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ContentFilterMode {
    MUSIC_ONLY,
    ALL_CONTENT
}

interface MusicPreferencesRepository {
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
