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
import org.json.JSONObject
import java.io.IOException

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "stereo_theme_preferences")

/**
 * Card Style Modes per Phase D Directive:
 * Exactly two modes per theme: DEFAULT or THEME_SPECIFIC.
 */
enum class CardStyleMode {
    DEFAULT,
    THEME_SPECIFIC
}

/**
 * Advanced Appearance tokens per Phase F Directive.
 */
data class AdvancedAppearanceConfig(
    val batchPlaybackControlColor: Long? = null,
    val navigationBarColor: Long? = null,
    val miniPlayerAccentColor: Long? = null,
    val surfaceOpacity: Float = 1.0f,
    val cornerRoundingScale: Float = 1.0f
) {
    fun toJson(): String {
        val json = JSONObject()
        batchPlaybackControlColor?.let { json.put("batchControlColor", it) }
        navigationBarColor?.let { json.put("navBarColor", it) }
        miniPlayerAccentColor?.let { json.put("miniPlayerAccent", it) }
        json.put("surfaceOpacity", surfaceOpacity.toDouble())
        json.put("cornerRoundingScale", cornerRoundingScale.toDouble())
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): AdvancedAppearanceConfig {
            return try {
                val json = JSONObject(jsonStr)
                AdvancedAppearanceConfig(
                    batchPlaybackControlColor = if (json.has("batchControlColor")) json.getLong("batchControlColor") else null,
                    navigationBarColor = if (json.has("navBarColor")) json.getLong("navBarColor") else null,
                    miniPlayerAccentColor = if (json.has("miniPlayerAccent")) json.getLong("miniPlayerAccent") else null,
                    surfaceOpacity = json.optDouble("surfaceOpacity", 1.0).toFloat(),
                    cornerRoundingScale = json.optDouble("cornerRoundingScale", 1.0).toFloat()
                )
            } catch (_: Exception) {
                AdvancedAppearanceConfig()
            }
        }
    }
}

interface ThemeRepository {
    val selectedThemeId: Flow<String>
    val selectedTheme: Flow<StereoColors>
    val customThemes: Flow<List<CustomTheme>>
    val cardStyleMode: Flow<CardStyleMode>
    val advancedAppearance: Flow<AdvancedAppearanceConfig>

    suspend fun setThemeId(themeId: String)
    suspend fun saveCustomTheme(theme: CustomTheme)
    suspend fun deleteCustomTheme(themeId: String)
    suspend fun setCardStyleMode(mode: CardStyleMode)
    suspend fun setAdvancedAppearance(config: AdvancedAppearanceConfig)
    suspend fun resetAdvancedAppearance()
    suspend fun restoreThemeDefaults()
    suspend fun resetToDefault()
}

class ThemePreferencesRepository(
    private val context: Context
) : ThemeRepository {

    companion object {
        val KEY_THEME_ID = stringPreferencesKey("selected_theme_id")
        val KEY_CUSTOM_THEMES = stringPreferencesKey("custom_themes_json")
        val KEY_CARD_STYLE_MODE = stringPreferencesKey("card_style_mode")
        val KEY_ADVANCED_APPEARANCE = stringPreferencesKey("advanced_appearance_json")
        const val DEFAULT_THEME_ID = "monochrome_minimal"
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

    override val cardStyleMode: Flow<CardStyleMode> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeStr = preferences[KEY_CARD_STYLE_MODE] ?: CardStyleMode.DEFAULT.name
            try {
                CardStyleMode.valueOf(modeStr)
            } catch (_: Exception) {
                CardStyleMode.DEFAULT
            }
        }

    override val advancedAppearance: Flow<AdvancedAppearanceConfig> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonStr = preferences[KEY_ADVANCED_APPEARANCE] ?: return@map AdvancedAppearanceConfig()
            AdvancedAppearanceConfig.fromJson(jsonStr)
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

    override suspend fun setCardStyleMode(mode: CardStyleMode) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_CARD_STYLE_MODE] = mode.name
        }
    }

    override suspend fun setAdvancedAppearance(config: AdvancedAppearanceConfig) {
        context.themeDataStore.edit { preferences ->
            preferences[KEY_ADVANCED_APPEARANCE] = config.toJson()
        }
    }

    override suspend fun resetAdvancedAppearance() {
        context.themeDataStore.edit { preferences ->
            preferences.remove(KEY_ADVANCED_APPEARANCE)
        }
    }

    override suspend fun restoreThemeDefaults() {
        context.themeDataStore.edit { preferences ->
            preferences.remove(KEY_ADVANCED_APPEARANCE)
            preferences[KEY_CARD_STYLE_MODE] = CardStyleMode.DEFAULT.name
            preferences[KEY_THEME_ID] = DEFAULT_THEME_ID
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
        restoreThemeDefaults()
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
