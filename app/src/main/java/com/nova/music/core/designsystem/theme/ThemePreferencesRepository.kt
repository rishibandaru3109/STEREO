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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.io.IOException

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "stereo_theme_preferences")

interface ThemeRepository {
    val selectedThemeId: Flow<String>
    val selectedTheme: Flow<StereoColors>
    val customThemes: Flow<List<CustomTheme>>

    suspend fun setThemeId(themeId: String)
    suspend fun saveCustomTheme(theme: CustomTheme)
    suspend fun deleteCustomTheme(themeId: String)
    suspend fun resetToDefault()
}

class ThemePreferencesRepository(
    private val context: Context
) : ThemeRepository {

    companion object {
        val KEY_THEME_ID = stringPreferencesKey("selected_theme_id")
        val KEY_CUSTOM_THEMES = stringPreferencesKey("custom_themes_json")
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

    override val customThemes: Flow<List<CustomTheme>> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonStr = preferences[KEY_CUSTOM_THEMES] ?: return@map emptyList<CustomTheme>()
            parseCustomThemes(jsonStr)
        }

    override val selectedTheme: Flow<StereoColors> = combine(
        selectedThemeId,
        customThemes
    ) { id, customList ->
        val customMatch = customList.firstOrNull { it.id.equals(id, ignoreCase = true) }
        customMatch?.toStereoColors() ?: ThemePresets.getById(id)
    }

    override suspend fun setThemeId(themeId: String) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_ID] = themeId
        }
    }

    override suspend fun saveCustomTheme(theme: CustomTheme) {
        context.themeDataStore.edit { preferences ->
            val currentList = parseCustomThemes(preferences[KEY_CUSTOM_THEMES] ?: "[]").toMutableList()
            val existingIndex = currentList.indexOfFirst { it.id == theme.id }
            if (existingIndex >= 0) {
                currentList[existingIndex] = theme
            } else {
                currentList.add(theme)
            }
            preferences[KEY_CUSTOM_THEMES] = serializeCustomThemes(currentList)
            // Automatically switch to the newly saved custom theme
            preferences[KEY_THEME_ID] = theme.id
        }
    }

    override suspend fun deleteCustomTheme(themeId: String) {
        context.themeDataStore.edit { preferences ->
            val currentList = parseCustomThemes(preferences[KEY_CUSTOM_THEMES] ?: "[]").toMutableList()
            currentList.removeAll { it.id == themeId }
            preferences[KEY_CUSTOM_THEMES] = serializeCustomThemes(currentList)
            if (preferences[KEY_THEME_ID] == themeId) {
                preferences[KEY_THEME_ID] = DEFAULT_THEME_ID
            }
        }
    }

    override suspend fun resetToDefault() {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_THEME_ID] = DEFAULT_THEME_ID
        }
    }

    private fun parseCustomThemes(jsonStr: String): List<CustomTheme> {
        val result = mutableListOf<CustomTheme>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val itemStr = jsonArray.getString(i)
                CustomTheme.fromJson(itemStr)?.let { result.add(it) }
            }
        } catch (_: Throwable) {
            // Ignore parse errors, return parsed items
        }
        return result
    }

    private fun serializeCustomThemes(themes: List<CustomTheme>): String {
        val jsonArray = JSONArray()
        for (theme in themes) {
            jsonArray.put(theme.toJson())
        }
        return jsonArray.toString()
    }
}
