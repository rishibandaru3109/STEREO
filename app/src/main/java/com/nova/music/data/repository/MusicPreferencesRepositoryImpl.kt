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
import com.nova.music.domain.repository.ContentFilterMode
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.domain.repository.RecommendationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.musicPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "stereo_music_preferences")

class MusicPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : MusicPreferencesRepository {
    constructor(context: Context) : this(context.musicPreferencesDataStore)


    companion object {
        val KEY_LANGUAGES = stringSetPreferencesKey("pref_languages")
        val KEY_GENRES = stringSetPreferencesKey("pref_genres")
        val KEY_DISCOVERY_MODE = stringPreferencesKey("pref_discovery_mode")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("pref_onboarding_completed")
        val KEY_HISTORY_ENABLED = booleanPreferencesKey("pref_history_enabled")
        val KEY_CONTENT_FILTER_MODE = stringPreferencesKey("pref_content_filter_mode")

        val DEFAULT_LANGUAGES = setOf("English", "Hindi")
        val DEFAULT_GENRES = setOf("Pop", "Rock", "Hip-Hop", "Chill")
        const val DEFAULT_DISCOVERY_MODE = "Familiar"
    }

    override val recommendationPreferences: Flow<RecommendationPreferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            RecommendationPreferences(
                languages = it[KEY_LANGUAGES].orEmpty(),
                genres = it[KEY_GENRES] ?: DEFAULT_GENRES,
                discoveryMode = it[KEY_DISCOVERY_MODE] ?: DEFAULT_DISCOVERY_MODE,
                contentFilterMode = if (it[KEY_CONTENT_FILTER_MODE] == ContentFilterMode.ALL_CONTENT.name) {
                    ContentFilterMode.ALL_CONTENT
                } else {
                    ContentFilterMode.MUSIC_ONLY
                }
            )
        }

    override suspend fun saveRecommendationPreferences(languages: Set<String>, genres: Set<String>, mode: String) {
        dataStore.edit {
            it[KEY_LANGUAGES] = languages
            it[KEY_GENRES] = genres
            it[KEY_DISCOVERY_MODE] = mode
            it[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    override val preferredLanguages: Flow<Set<String>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LANGUAGES] ?: DEFAULT_LANGUAGES }

    override val preferredGenres: Flow<Set<String>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_GENRES] ?: DEFAULT_GENRES }

    override val discoveryMode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DISCOVERY_MODE] ?: DEFAULT_DISCOVERY_MODE }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    override val isHistoryEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_HISTORY_ENABLED] ?: true }

    override val contentFilterMode: Flow<ContentFilterMode> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            when (it[KEY_CONTENT_FILTER_MODE]) {
                ContentFilterMode.ALL_CONTENT.name -> ContentFilterMode.ALL_CONTENT
                else -> ContentFilterMode.MUSIC_ONLY
            }
        }

    override suspend fun setPreferredLanguages(languages: Set<String>) {
        dataStore.edit { it[KEY_LANGUAGES] = languages }
    }

    override suspend fun setPreferredGenres(genres: Set<String>) {
        dataStore.edit { it[KEY_GENRES] = genres }
    }

    override suspend fun setDiscoveryMode(mode: String) {
        dataStore.edit { it[KEY_DISCOVERY_MODE] = mode }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setHistoryEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HISTORY_ENABLED] = enabled }
    }

    override suspend fun setContentFilterMode(mode: ContentFilterMode) {
        dataStore.edit { it[KEY_CONTENT_FILTER_MODE] = mode.name }
    }

    override suspend fun resetRecommendationPreferences() {
        dataStore.edit {
            it.remove(KEY_LANGUAGES)
            it[KEY_GENRES] = DEFAULT_GENRES
            it[KEY_DISCOVERY_MODE] = DEFAULT_DISCOVERY_MODE
            it[KEY_ONBOARDING_COMPLETED] = false
        }
    }
}
