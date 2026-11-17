package com.nova.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.core.designsystem.wallpaper.WallpaperBackdrop
import com.nova.music.core.designsystem.wallpaper.WallpaperConfig
import com.nova.music.feature.home.HomeScreen
import com.nova.music.feature.home.HomeViewModel
import com.nova.music.feature.library.LibraryScreen
import com.nova.music.feature.library.LibraryViewModel
import com.nova.music.feature.library.LikedSongsScreen
import com.nova.music.feature.library.PlaylistDetailScreen
import com.nova.music.feature.navigation.NavigationTab
import com.nova.music.feature.navigation.StereoBottomBar
import com.nova.music.feature.player.FullPlayerSheet
import com.nova.music.feature.player.MiniPlayer
import com.nova.music.feature.player.PlayerViewModel
import com.nova.music.feature.search.SearchScreen
import com.nova.music.feature.search.SearchViewModel
import com.nova.music.feature.settings.SettingsScreen
import com.nova.music.feature.settings.SettingsViewModel
import com.nova.music.feature.theme.ThemeGalleryScreen
import com.nova.music.feature.theme.ThemeStudioScreen
import com.nova.music.feature.theme.ThemeViewModel
import com.nova.music.feature.wallpaper.WallpaperStudioScreen
import com.nova.music.feature.wallpaper.WallpaperViewModel
import org.koin.androidx.compose.koinViewModel

enum class SubScreen {
    LIKED_SONGS,
    PLAYLIST_DETAILS,
    THEMES,
    THEME_STUDIO,
    WALLPAPER_STUDIO,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+ (Tiramisu) for playback controls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            StereoApp()
        }
    }
}

@Composable
fun StereoApp() {
    val themeViewModel: ThemeViewModel = koinViewModel()
    val wallpaperViewModel: WallpaperViewModel = koinViewModel()
    val currentColors by themeViewModel.currentTheme.collectAsStateWithLifecycle()
    val cardStyleMode by themeViewModel.cardStyleMode.collectAsStateWithLifecycle()
    val advancedAppearance by themeViewModel.advancedAppearance.collectAsStateWithLifecycle()
    val wallpaperConfig by wallpaperViewModel.wallpaperConfig.collectAsStateWithLifecycle()

    StereoTheme(
        colors = currentColors,
        cardMode = cardStyleMode,
        advancedConfig = advancedAppearance
    ) {
        val homeViewModel: HomeViewModel = koinViewModel()
        val searchViewModel: SearchViewModel = koinViewModel()
        val playerViewModel: PlayerViewModel = koinViewModel()
        val libraryViewModel: LibraryViewModel = koinViewModel()
        val playbackState by playerViewModel.playbackState.collectAsStateWithLifecycle()

        var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.HOME) }
        var activeSubScreen by rememberSaveable { mutableStateOf<SubScreen?>(null) }
        var isFullPlayerExpanded by remember { mutableStateOf(false) }

        // BackHandler manages nested screens and tab switching
        BackHandler(enabled = activeSubScreen != null || selectedTab != NavigationTab.HOME) {
            when {
                activeSubScreen == SubScreen.THEME_STUDIO || activeSubScreen == SubScreen.WALLPAPER_STUDIO -> {
                    activeSubScreen = SubScreen.THEMES
                }
                activeSubScreen == SubScreen.THEMES || activeSubScreen == SubScreen.SETTINGS -> {
                    activeSubScreen = null
                }
                activeSubScreen == SubScreen.LIKED_SONGS || activeSubScreen == SubScreen.PLAYLIST_DETAILS -> {
                    activeSubScreen = null
                }
                selectedTab != NavigationTab.HOME -> {
                    selectedTab = NavigationTab.HOME
                }
            }
        }

        val effectiveRootWallpaper = if (!wallpaperConfig.applyToPlayerOnly) {
            wallpaperConfig
        } else {
            WallpaperConfig() // Default solid theme background for browse screens
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(currentColors.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = currentColors.background
        ) { innerPadding ->
            WallpaperBackdrop(
                config = effectiveRootWallpaper,
                artworkUrl = playbackState.currentTrack?.artworkUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main Content Layer
                    if (activeSubScreen != null) {
                        when (activeSubScreen) {
                            SubScreen.LIKED_SONGS -> {
                                LikedSongsScreen(
                                    viewModel = libraryViewModel,
                                    onBackClick = { activeSubScreen = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            SubScreen.PLAYLIST_DETAILS -> {
                                PlaylistDetailScreen(
                                    viewModel = libraryViewModel,
                                    onBackClick = { activeSubScreen = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            SubScreen.THEMES -> {
                                ThemeGalleryScreen(
                                    viewModel = themeViewModel,
                                    onBackClick = { activeSubScreen = null },
                                    onCreateCustomTheme = { activeSubScreen = SubScreen.THEME_STUDIO },
                                    onOpenWallpaperStudio = { activeSubScreen = SubScreen.WALLPAPER_STUDIO },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            SubScreen.THEME_STUDIO -> {
                                ThemeStudioScreen(
                                    viewModel = themeViewModel,
                                    initialTheme = currentColors,
                                    onBackClick = { activeSubScreen = SubScreen.THEMES },
                                    onThemeSaved = { activeSubScreen = SubScreen.THEMES },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            SubScreen.WALLPAPER_STUDIO -> {
                                WallpaperStudioScreen(
                                    viewModel = wallpaperViewModel,
                                    onBackClick = { activeSubScreen = SubScreen.THEMES },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            SubScreen.SETTINGS -> {
                                val settingsViewModel: SettingsViewModel = koinViewModel()
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBackClick = { activeSubScreen = null },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            null -> {}
                        }
                    } else {
                        // Primary Tabs (HOME, SEARCH, LIBRARY)
                        Crossfade(
                            targetState = selectedTab,
                            animationSpec = tween(150),
                            label = "tabCrossfade"
                        ) { tab ->
                            when (tab) {
                                NavigationTab.HOME -> {
                                    HomeScreen(
                                        viewModel = homeViewModel,
                                        onOpenThemeGallery = { activeSubScreen = SubScreen.THEMES },
                                        onOpenSettings = { activeSubScreen = SubScreen.SETTINGS },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                NavigationTab.SEARCH -> {
                                    SearchScreen(
                                        viewModel = searchViewModel,
                                        onOpenLibrary = { selectedTab = NavigationTab.LIBRARY },
                                        onOpenThemeGallery = { activeSubScreen = SubScreen.THEMES },
                                        onOpenSettings = { activeSubScreen = SubScreen.SETTINGS },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                NavigationTab.LIBRARY -> {
                                    LibraryScreen(
                                        viewModel = libraryViewModel,
                                        onOpenLikedSongs = { activeSubScreen = SubScreen.LIKED_SONGS },
                                        onOpenPlaylist = { playlistId ->
                                            libraryViewModel.selectPlaylist(playlistId)
                                            activeSubScreen = SubScreen.PLAYLIST_DETAILS
                                        },
                                        onOpenSearch = { selectedTab = NavigationTab.SEARCH },
                                        onOpenThemeGallery = { activeSubScreen = SubScreen.THEMES },
                                        onOpenSettings = { activeSubScreen = SubScreen.SETTINGS },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Dock Layer: Mini-Player directly stacked above Bottom Navigation Bar
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        MiniPlayer(
                            viewModel = playerViewModel,
                            onExpandClick = { isFullPlayerExpanded = true }
                        )

                        if (activeSubScreen == null) {
                            StereoBottomBar(
                                currentTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                    }

                    // Full Screen Player Modal Sheet
                    if (isFullPlayerExpanded) {
                        FullPlayerSheet(
                            viewModel = playerViewModel,
                            onDismissRequest = { isFullPlayerExpanded = false }
                        )
                    }
                }
            }
        }
    }
}
