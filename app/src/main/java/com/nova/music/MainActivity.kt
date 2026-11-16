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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.feature.player.FullPlayerSheet
import com.nova.music.feature.player.MiniPlayer
import com.nova.music.feature.player.PlayerViewModel
import com.nova.music.feature.search.SearchScreen
import com.nova.music.feature.search.SearchViewModel
import com.nova.music.feature.theme.ThemeGalleryScreen
import com.nova.music.feature.theme.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

enum class MainScreen {
    SEARCH,
    THEMES
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
            val themeViewModel: ThemeViewModel = koinViewModel()
            val currentColors by themeViewModel.currentTheme.collectAsStateWithLifecycle()

            StereoTheme(colors = currentColors) {
                val searchViewModel: SearchViewModel = koinViewModel()
                val playerViewModel: PlayerViewModel = koinViewModel()
                var currentScreen by remember { mutableStateOf(MainScreen.SEARCH) }
                var isFullPlayerExpanded by remember { mutableStateOf(false) }

                BackHandler(enabled = currentScreen != MainScreen.SEARCH) {
                    currentScreen = MainScreen.SEARCH
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(currentColors.background)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = currentColors.background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            MainScreen.SEARCH -> {
                                SearchScreen(
                                    viewModel = searchViewModel,
                                    onOpenThemeGallery = { currentScreen = MainScreen.THEMES },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            MainScreen.THEMES -> {
                                ThemeGalleryScreen(
                                    viewModel = themeViewModel,
                                    onBackClick = { currentScreen = MainScreen.SEARCH },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Floating Mini-Player docked at the bottom across all screens
                        MiniPlayer(
                            viewModel = playerViewModel,
                            onExpandClick = { isFullPlayerExpanded = true },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

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
}
