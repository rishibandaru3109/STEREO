package com.nova.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.nova.music.core.designsystem.theme.NovaTheme
import com.nova.music.core.designsystem.theme.PureBlack
import com.nova.music.feature.player.FullPlayerSheet
import com.nova.music.feature.player.MiniPlayer
import com.nova.music.feature.player.PlayerViewModel
import com.nova.music.feature.search.SearchScreen
import com.nova.music.feature.search.SearchViewModel
import org.koin.androidx.compose.koinViewModel

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
            NovaTheme {
                val searchViewModel: SearchViewModel = koinViewModel()
                val playerViewModel: PlayerViewModel = koinViewModel()
                var isFullPlayerExpanded by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    containerColor = PureBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Search & Discovery Screen
                        SearchScreen(
                            viewModel = searchViewModel,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating AMOLED Mini-Player docked at the bottom
                        MiniPlayer(
                            viewModel = playerViewModel,
                            onExpandClick = { isFullPlayerExpanded = true },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        // Full Screen AMOLED Player Modal Sheet
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
