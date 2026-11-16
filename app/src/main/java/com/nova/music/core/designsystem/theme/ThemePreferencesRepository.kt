package com.nova.music.core.designsystem.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "stereo_theme_preferences")

interface ThemeRepository {
    val selectedThemeId: Flow<String>
    val selectedTheme: Flow<StereoColors>
    suspend fun setThemeId(themeId: String)
    suspend fun resetToDefault()
}

class ThemePreferencesRepository(
    private val context: Context
) : ThemeRepository {

    companion object {
        val KEY_THEME_ID = stringPreferencesKey("selected_theme_id")
        const val DEFAULT_THEME_ID = "midnight_stereo"
    }

    override val selectedThemeId: Flow<String> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_THEME_ID] ?: DEFAULT_THEME_ID
        }

    override val selectedTheme: Flow<StereoColors> = selectedThemeId
        .map { id ->
            ThemePresets.getById(id)
        }

    override suspend fun setThemeId(themeId: String) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_ID] = themeId
        }
    }

    override suspend fun resetToDefault() {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_ID] = DEFAULT_THEME_ID
        }
    }
}
