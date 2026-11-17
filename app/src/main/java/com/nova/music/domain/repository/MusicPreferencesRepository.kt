package com.nova.music.domain.repository

import kotlinx.coroutines.flow.Flow

interface MusicPreferencesRepository {
    val preferredLanguages: Flow<Set<String>>
    val preferredGenres: Flow<Set<String>>
    val discoveryMode: Flow<String> // "Familiar" or "Adventurous"
    val isOnboardingCompleted: Flow<Boolean>
    val isHistoryEnabled: Flow<Boolean>

    suspend fun setPreferredLanguages(languages: Set<String>)
    suspend fun setPreferredGenres(genres: Set<String>)
    suspend fun setDiscoveryMode(mode: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setHistoryEnabled(enabled: Boolean)
    suspend fun resetRecommendationPreferences()
}
