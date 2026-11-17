package com.nova.music.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nova.music.domain.repository.MusicPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.musicPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "stereo_music_preferences")

class MusicPreferencesRepositoryImpl(
    private val context: Context
) : MusicPreferencesRepository {

    companion object {
        val KEY_LANGUAGES = stringSetPreferencesKey("pref_languages")
        val KEY_GENRES = stringSetPreferencesKey("pref_genres")
        val KEY_DISCOVERY_MODE = stringPreferencesKey("pref_discovery_mode")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("pref_onboarding_completed")
        val KEY_HISTORY_ENABLED = booleanPreferencesKey("pref_history_enabled")

        val DEFAULT_LANGUAGES = setOf("English", "Hindi")
        val DEFAULT_GENRES = setOf("Pop", "Rock", "Hip-Hop", "Chill")
        const val DEFAULT_DISCOVERY_MODE = "Familiar"
    }

    override val preferredLanguages: Flow<Set<String>> = context.musicPreferencesDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LANGUAGES] ?: DEFAULT_LANGUAGES }

    override val preferredGenres: Flow<Set<String>> = context.musicPreferencesDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_GENRES] ?: DEFAULT_GENRES }

    override val discoveryMode: Flow<String> = context.musicPreferencesDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DISCOVERY_MODE] ?: DEFAULT_DISCOVERY_MODE }

    override val isOnboardingCompleted: Flow<Boolean> = context.musicPreferencesDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    override val isHistoryEnabled: Flow<Boolean> = context.musicPreferencesDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_HISTORY_ENABLED] ?: true }

    override suspend fun setPreferredLanguages(languages: Set<String>) {
        context.musicPreferencesDataStore.edit { it[KEY_LANGUAGES] = languages }
    }

    override suspend fun setPreferredGenres(genres: Set<String>) {
        context.musicPreferencesDataStore.edit { it[KEY_GENRES] = genres }
    }

    override suspend fun setDiscoveryMode(mode: String) {
        context.musicPreferencesDataStore.edit { it[KEY_DISCOVERY_MODE] = mode }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.musicPreferencesDataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        context.musicPreferencesDataStore.edit { it[KEY_HISTORY_ENABLED] = enabled }
    }

    override suspend fun resetRecommendationPreferences() {
        context.musicPreferencesDataStore.edit {
            it[KEY_LANGUAGES] = DEFAULT_LANGUAGES
            it[KEY_GENRES] = DEFAULT_GENRES
            it[KEY_DISCOVERY_MODE] = DEFAULT_DISCOVERY_MODE
            it[KEY_ONBOARDING_COMPLETED] = false
        }
    }
}
