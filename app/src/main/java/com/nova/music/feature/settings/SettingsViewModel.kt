package com.nova.music.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.core.designsystem.theme.AdvancedAppearanceConfig
import com.nova.music.core.designsystem.theme.CardStyleMode
import com.nova.music.core.designsystem.theme.StereoColors
import com.nova.music.core.designsystem.theme.ThemePresets
import com.nova.music.core.designsystem.theme.ThemeRepository
import com.nova.music.core.designsystem.wallpaper.WallpaperConfig
import com.nova.music.core.designsystem.wallpaper.WallpaperMode
import com.nova.music.core.designsystem.wallpaper.WallpaperRepository
import com.nova.music.domain.repository.HistoryRepository
import com.nova.music.domain.repository.MusicPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val preferencesRepository: MusicPreferencesRepository,
    private val historyRepository: HistoryRepository,
    private val context: Context
) : ViewModel() {

    val selectedThemeId: StateFlow<String> = themeRepository.selectedThemeId
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePresets.MonochromeMinimal.id)

    val currentTheme: StateFlow<StereoColors> = themeRepository.selectedTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePresets.MonochromeMinimal)

    val cardStyleMode: StateFlow<CardStyleMode> = themeRepository.cardStyleMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, CardStyleMode.DEFAULT)

    val advancedAppearance: StateFlow<AdvancedAppearanceConfig> = themeRepository.advancedAppearance
        .stateIn(viewModelScope, SharingStarted.Eagerly, AdvancedAppearanceConfig())

    val wallpaperConfig: StateFlow<WallpaperConfig> = wallpaperRepository.wallpaperConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, WallpaperConfig())

    val preferredLanguages: StateFlow<Set<String>> = preferencesRepository.preferredLanguages
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf("English", "Hindi"))

    val preferredGenres: StateFlow<Set<String>> = preferencesRepository.preferredGenres
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf("Pop", "Rock", "Chill"))

    val isHistoryEnabled: StateFlow<Boolean> = preferencesRepository.isHistoryEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val officialThemes = ThemePresets.officialThemes

    fun selectTheme(themeId: String) {
        viewModelScope.launch { themeRepository.setThemeId(themeId) }
    }

    fun setCardStyleMode(mode: CardStyleMode) {
        viewModelScope.launch { themeRepository.setCardStyleMode(mode) }
    }

    fun setBatchControlColor(colorLong: Long?) {
        viewModelScope.launch {
            val current = advancedAppearance.value
            themeRepository.setAdvancedAppearance(current.copy(batchPlaybackControlColor = colorLong))
        }
    }

    fun resetAdvancedAppearance() {
        viewModelScope.launch { themeRepository.resetAdvancedAppearance() }
    }

    fun restoreThemeDefaults() {
        viewModelScope.launch {
            themeRepository.restoreThemeDefaults()
            wallpaperRepository.resetToDefault()
        }
    }

    fun setCustomPlayerBackground(uri: String?) {
        viewModelScope.launch {
            val current = wallpaperConfig.value
            if (uri != null) {
                wallpaperRepository.setWallpaperConfig(
                    current.copy(
                        mode = WallpaperMode.CUSTOM_IMAGE,
                        customImageUri = uri,
                        applyToPlayerOnly = true
                    )
                )
            } else {
                wallpaperRepository.setWallpaperConfig(
                    current.copy(
                        mode = WallpaperMode.THEME_SOLID,
                        customImageUri = null
                    )
                )
            }
        }
    }

    fun updateCustomBackgroundParams(blur: Float, dimming: Float) {
        viewModelScope.launch {
            val current = wallpaperConfig.value
            wallpaperRepository.setWallpaperConfig(
                current.copy(
                    blurRadius = blur,
                    dimmingAlpha = dimming
                )
            )
        }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHistoryEnabled(enabled) }
    }

    fun clearListeningHistory() {
        viewModelScope.launch { historyRepository.clearHistory() }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                context.cacheDir.deleteRecursively()
                context.cacheDir.mkdirs()
            } catch (_: Exception) {}
        }
    }

    fun saveRecommendationPreferences(languages: Set<String>, genres: Set<String>, discoveryMode: String) {
        viewModelScope.launch {
            preferencesRepository.setPreferredLanguages(languages)
            preferencesRepository.setPreferredGenres(genres)
            preferencesRepository.setDiscoveryMode(discoveryMode)
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun resetRecommendationPreferences() {
        viewModelScope.launch { preferencesRepository.resetRecommendationPreferences() }
    }
}
