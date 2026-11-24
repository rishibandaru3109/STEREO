package com.nova.music.feature.settings

import android.content.Context
import com.nova.music.core.cache.AudioCacheEngine
import com.nova.music.core.cache.AudioCacheStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nova.music.domain.model.AudioQualityMode
import com.nova.music.core.network.TrafficMeter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import com.nova.music.domain.repository.ContentFilterMode
import com.nova.music.domain.repository.HistoryRepository
import com.nova.music.domain.repository.HomeRepository
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
    private val context: Context,
    private val homeRepository: HomeRepository? = null,
    private val audioCache: AudioCacheEngine? = null
) : ViewModel() {
    val audioCacheStats = audioCache?.stats ?: MutableStateFlow(AudioCacheStats())
    val audioCacheEnabled = audioCache?.enabled ?: MutableStateFlow(false)
    val audioPrefetchEnabled = audioCache?.prefetchEnabled ?: MutableStateFlow(false)
    val audioCacheLimitMb = audioCache?.limitMb ?: MutableStateFlow(500)

    fun setAudioCaching(value: Boolean) {
        viewModelScope.launch {
            try {
                if (value && audioCache?.limitMb?.value == 0) audioCache.setLimitMb(500)
                else audioCache?.setEnabled(value)
            } catch (error: Exception) { mutableStorageMessage.value = error.message }
        }
    }

    fun setAudioPrefetch(value: Boolean) {
        viewModelScope.launch {
            try { audioCache?.setPrefetch(value) }
            catch (error: Exception) { mutableStorageMessage.value = error.message }
        }
    }
    private val mutableArtworkBytes = MutableStateFlow<Long?>(null)
    val artworkBytes = mutableArtworkBytes.asStateFlow()
    private val mutableStorageMessage = MutableStateFlow<String?>(null)
    val storageMessage = mutableStorageMessage.asStateFlow()

    fun refreshStorage() {
        viewModelScope.launch {
            audioCache?.refresh()
            mutableArtworkBytes.value = withContext(Dispatchers.IO) {
                coil.Coil.imageLoader(context).diskCache?.size ?: 0L
            }
        }
    }

    fun setAudioCacheLimit(mb: Int) {
        viewModelScope.launch {
            try {
                audioCache?.setLimitMb(mb)
                mutableStorageMessage.value = "Audio cache limit saved"
            } catch (error: Exception) {
                mutableStorageMessage.value = error.message ?: "Could not change cache limit"
            }
            refreshStorage()
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            try {
                audioCache?.clear()
                mutableStorageMessage.value = "Audio cache cleared. Active playback may cache new segments."
            } catch (error: Exception) {
                mutableStorageMessage.value = error.message ?: "Could not clear audio cache"
            }
            refreshStorage()
        }
    }

    fun clearArtworkCache() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    coil.Coil.imageLoader(context).diskCache?.clear()
                    coil.Coil.imageLoader(context).memoryCache?.clear()
                }
                mutableStorageMessage.value = "Artwork cache cleared"
            } catch (error: Exception) {
                mutableStorageMessage.value = error.message ?: "Could not clear artwork cache"
            }
            refreshStorage()
        }
    }

    val audioQualityMode = preferencesRepository.audioQualityMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudioQualityMode.AUTO)

    private val mutableTraffic = MutableStateFlow(TrafficMeter.counters.snapshot())
    val traffic = mutableTraffic.asStateFlow()

    fun setAudioQualityMode(mode: AudioQualityMode) {
        viewModelScope.launch { preferencesRepository.setAudioQualityMode(mode) }
    }

    fun sampleTraffic() {
        mutableTraffic.value = TrafficMeter.counters.snapshot()
        if (com.nova.music.BuildConfig.DEBUG) {
            android.util.Log.i("STEREO_TRAFFIC", mutableTraffic.value.logLine())
        }
    }


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

    val isSynapseEnabled: StateFlow<Boolean> = preferencesRepository.isSynapseEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val synapseDiscoveryAlpha: StateFlow<Float> = preferencesRepository.synapseDiscoveryAlpha
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.65f)

    val contentFilterMode: StateFlow<ContentFilterMode> = preferencesRepository.contentFilterMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ContentFilterMode.MUSIC_ONLY)

    val isDownloadWifiOnly: StateFlow<Boolean> = preferencesRepository.isDownloadWifiOnly
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDownloadWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDownloadWifiOnly(wifiOnly)
        }
    }

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

    fun setSynapseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSynapseEnabled(enabled)
            homeRepository?.clearCache()
        }
    }

    fun setSynapseDiscoveryAlpha(alpha: Float) {
        viewModelScope.launch {
            preferencesRepository.setSynapseDiscoveryAlpha(alpha)
            homeRepository?.clearCache()
        }
    }

    fun setContentFilterMode(mode: ContentFilterMode) {
        viewModelScope.launch { preferencesRepository.setContentFilterMode(mode) }
    }

    fun clearListeningHistory() {
        viewModelScope.launch { historyRepository.clearHistory() }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                coil.Coil.imageLoader(context).diskCache?.clear()
                homeRepository?.clearCache()
            } catch (_: Exception) {}
        }
    }

    fun saveRecommendationPreferences(languages: Set<String>, genres: Set<String>, discoveryMode: String) {
        viewModelScope.launch {
            preferencesRepository.saveRecommendationPreferences(languages, genres, discoveryMode)
        }
    }

    fun resetRecommendationPreferences() {
        viewModelScope.launch {
            preferencesRepository.resetRecommendationPreferences()
            homeRepository?.clearCache()
        }
    }
}
