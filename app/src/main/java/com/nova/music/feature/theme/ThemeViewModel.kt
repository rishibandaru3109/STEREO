package com.nova.music.feature.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.core.designsystem.theme.AdvancedAppearanceConfig
import com.nova.music.core.designsystem.theme.CardStyleMode
import com.nova.music.core.designsystem.theme.CustomTheme
import com.nova.music.core.designsystem.theme.StereoColors
import com.nova.music.core.designsystem.theme.ThemePresets
import com.nova.music.core.designsystem.theme.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val currentThemeId: StateFlow<String> = themeRepository.selectedThemeId
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePresets.MonochromeMinimal.id
        )

    val currentTheme: StateFlow<StereoColors> = themeRepository.selectedTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePresets.MonochromeMinimal
        )

    val cardStyleMode: StateFlow<CardStyleMode> = themeRepository.cardStyleMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = CardStyleMode.DEFAULT
        )

    val advancedAppearance: StateFlow<AdvancedAppearanceConfig> = themeRepository.advancedAppearance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AdvancedAppearanceConfig()
        )

    val customThemes: StateFlow<List<CustomTheme>> = themeRepository.customThemes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val officialThemes: List<StereoColors> = ThemePresets.officialThemes
    val allPresets: List<StereoColors> = ThemePresets.allPresets

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            themeRepository.setThemeId(themeId)
        }
    }

    fun setCardStyleMode(mode: CardStyleMode) {
        viewModelScope.launch {
            themeRepository.setCardStyleMode(mode)
        }
    }

    fun setAdvancedAppearance(config: AdvancedAppearanceConfig) {
        viewModelScope.launch {
            themeRepository.setAdvancedAppearance(config)
        }
    }

    fun resetAdvancedAppearance() {
        viewModelScope.launch {
            themeRepository.resetAdvancedAppearance()
        }
    }

    fun restoreThemeDefaults() {
        viewModelScope.launch {
            themeRepository.restoreThemeDefaults()
        }
    }

    fun saveCustomTheme(theme: CustomTheme) {
        viewModelScope.launch {
            themeRepository.saveCustomTheme(theme)
        }
    }

    fun deleteCustomTheme(themeId: String) {
        viewModelScope.launch {
            themeRepository.deleteCustomTheme(themeId)
        }
    }

    fun importThemeJson(jsonStr: String): Boolean {
        val parsed = CustomTheme.fromJson(jsonStr) ?: return false
        viewModelScope.launch {
            themeRepository.saveCustomTheme(parsed)
        }
        return true
    }

    fun resetToDefault() {
        viewModelScope.launch {
            themeRepository.resetToDefault()
        }
    }
}
