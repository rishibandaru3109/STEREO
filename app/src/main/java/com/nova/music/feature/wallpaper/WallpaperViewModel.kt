package com.nova.music.feature.wallpaper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.music.core.designsystem.wallpaper.WallpaperConfig
import com.nova.music.core.designsystem.wallpaper.WallpaperRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WallpaperViewModel(
    private val wallpaperRepository: WallpaperRepository
) : ViewModel() {

    val wallpaperConfig: StateFlow<WallpaperConfig> = wallpaperRepository.wallpaperConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = WallpaperConfig()
        )

    fun updateConfig(config: WallpaperConfig) {
        viewModelScope.launch {
            wallpaperRepository.setWallpaperConfig(config)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            wallpaperRepository.resetToDefault()
        }
    }
}
