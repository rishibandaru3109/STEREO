package com.nova.music.core.designsystem.wallpaper

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

val Context.wallpaperDataStore: DataStore<Preferences> by preferencesDataStore(name = "stereo_wallpaper_preferences")

interface WallpaperRepository {
    val wallpaperConfig: Flow<WallpaperConfig>
    suspend fun setWallpaperConfig(config: WallpaperConfig)
    suspend fun resetToDefault()
}

class WallpaperPreferencesRepository(
    private val context: Context
) : WallpaperRepository {

    companion object {
        val KEY_WALLPAPER_CONFIG = stringPreferencesKey("wallpaper_config_json")
    }

    override val wallpaperConfig: Flow<WallpaperConfig> = context.wallpaperDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonStr = preferences[KEY_WALLPAPER_CONFIG]
            if (jsonStr != null) {
                WallpaperConfig.fromJson(jsonStr)
            } else {
                WallpaperConfig()
            }
        }

    override suspend fun setWallpaperConfig(config: WallpaperConfig) {
        context.wallpaperDataStore.edit { preferences ->
            preferences[KEY_WALLPAPER_CONFIG] = config.toJson()
        }
    }

    override suspend fun resetToDefault() {
        context.wallpaperDataStore.edit { preferences ->
            preferences[KEY_WALLPAPER_CONFIG] = WallpaperConfig().toJson()
        }
    }
}
