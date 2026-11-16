package com.nova.music.feature.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            initialValue = ThemePresets.MidnightStereo.id
        )

    val currentTheme: StateFlow<StereoColors> = themeRepository.selectedTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePresets.MidnightStereo
        )

    val allPresets: List<StereoColors> = ThemePresets.allPresets

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            themeRepository.setThemeId(themeId)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            themeRepository.resetToDefault()
        }
    }
}
