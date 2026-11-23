package com.nova.music.feature.settings

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import com.nova.music.core.cache.AudioCacheLimits
import com.nova.music.domain.model.AudioQualityMode
import com.nova.music.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.music.core.designsystem.theme.CardStyleMode
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.core.designsystem.wallpaper.WallpaperMode
import com.nova.music.domain.repository.ContentFilterMode
import com.nova.music.feature.home.RecommendationOnboardingDialog

private enum class SettingsDestination(val title: String) {
    MAIN("SETTINGS"),
    APPEARANCE("APPEARANCE"),
    ADVANCED_CUSTOMIZATION("ADVANCED CUSTOMIZATION"),
    AUDIO_PLAYBACK("AUDIO & PLAYBACK"),
    RECOMMENDATIONS("RECOMMENDATIONS"),
    CONTENT_PREFERENCES("CONTENT PREFERENCES"),
    LIBRARY_DATA("LIBRARY & DATA"),
    STORAGE_CACHE("STORAGE & CACHE"),
    PRIVACY("PRIVACY"),
    ABOUT("ABOUT")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qualityMode by viewModel.audioQualityMode.collectAsStateWithLifecycle()
    val traffic by viewModel.traffic.collectAsStateWithLifecycle()
    val cacheStats by viewModel.audioCacheStats.collectAsStateWithLifecycle()
    val cacheEnabled by viewModel.audioCacheEnabled.collectAsStateWithLifecycle()
    val prefetchEnabled by viewModel.audioPrefetchEnabled.collectAsStateWithLifecycle()
    val cacheLimitMb by viewModel.audioCacheLimitMb.collectAsStateWithLifecycle()
    val artworkBytes by viewModel.artworkBytes.collectAsStateWithLifecycle()
    val storageMessage by viewModel.storageMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = StereoTheme.colors
    var currentDestination by remember { mutableStateOf(SettingsDestination.MAIN) }

    val selectedThemeId by viewModel.selectedThemeId.collectAsStateWithLifecycle()
    val cardStyleMode by viewModel.cardStyleMode.collectAsStateWithLifecycle()
    val advancedAppearance by viewModel.advancedAppearance.collectAsStateWithLifecycle()
    val wallpaperConfig by viewModel.wallpaperConfig.collectAsStateWithLifecycle()
    val preferredLanguages by viewModel.preferredLanguages.collectAsStateWithLifecycle()
    val preferredGenres by viewModel.preferredGenres.collectAsStateWithLifecycle()
    val isSynapseEnabled by viewModel.isSynapseEnabled.collectAsStateWithLifecycle()
    val synapseDiscoveryAlpha by viewModel.synapseDiscoveryAlpha.collectAsStateWithLifecycle()
    val isHistoryEnabled by viewModel.isHistoryEnabled.collectAsStateWithLifecycle()
    val contentFilterMode by viewModel.contentFilterMode.collectAsStateWithLifecycle()

    var showOnboardingDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCustomPlayerBackground(uri.toString())
        }
    }

    if (showOnboardingDialog) {
        RecommendationOnboardingDialog(
            initialLanguages = preferredLanguages,
            initialGenres = preferredGenres,
            onDismiss = { showOnboardingDialog = false },
            onSavePreferences = { langs, genres, mode ->
                viewModel.saveRecommendationPreferences(langs, genres, mode)
                showOnboardingDialog = false
            }
        )
    }

    BackHandler(enabled = currentDestination != SettingsDestination.MAIN) {
        currentDestination = if (currentDestination == SettingsDestination.ADVANCED_CUSTOMIZATION) {
            SettingsDestination.APPEARANCE
        } else {
            SettingsDestination.MAIN
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                when (currentDestination) {
                    SettingsDestination.MAIN -> onBackClick()
                    SettingsDestination.ADVANCED_CUSTOMIZATION -> currentDestination = SettingsDestination.APPEARANCE
                    else -> currentDestination = SettingsDestination.MAIN
                }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentDestination.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = colors.textPrimary
                )
            )
        }

        // Body Content based on destination
        when (currentDestination) {
            SettingsDestination.MAIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsCategoryCard(
                        title = "Appearance",
                        subtitle = "Themes, card styles, and player backgrounds",
                        icon = Icons.Default.Palette,
                        onClick = { currentDestination = SettingsDestination.APPEARANCE }
                    )

                    SettingsCategoryCard(
                        title = "Audio & Playback",
                        subtitle = "Audio quality, Data Saver, and network usage",
                        icon = Icons.Default.GraphicEq,
                        onClick = { currentDestination = SettingsDestination.AUDIO_PLAYBACK }
                    )

                    SettingsCategoryCard(
                        title = "Recommendations & Taste",
                        subtitle = "Tuned genres, languages, and discovery mode",
                        icon = Icons.Default.Tune,
                        onClick = { currentDestination = SettingsDestination.RECOMMENDATIONS }
                    )

                    SettingsCategoryCard(
                        title = "Content Preferences",
                        subtitle = if (contentFilterMode == ContentFilterMode.MUSIC_ONLY) "Music Only (Podcasts & Interviews filtered)" else "All Content (Unfiltered)",
                        icon = Icons.Default.FilterList,
                        onClick = { currentDestination = SettingsDestination.CONTENT_PREFERENCES }
                    )

                    SettingsCategoryCard(
                        title = "Library & Data",
                        subtitle = "Playlists, saved tracks, and local Room persistence",
                        icon = Icons.Default.LibraryMusic,
                        onClick = { currentDestination = SettingsDestination.LIBRARY_DATA }
                    )

                    SettingsCategoryCard(
                        title = "Storage & Cache",
                        subtitle = "Thumbnail cache and temporary media streams",
                        icon = Icons.Default.Storage,
                        onClick = { currentDestination = SettingsDestination.STORAGE_CACHE }
                    )

                    SettingsCategoryCard(
                        title = "Privacy",
                        subtitle = "Listening history and 100% offline telemetry guarantee",
                        icon = Icons.Default.Security,
                        onClick = { currentDestination = SettingsDestination.PRIVACY }
                    )

                    SettingsCategoryCard(
                        title = "About",
                        subtitle = "STEREO v2.1.0 Master ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¢ Zero paid APIs ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¢ Open Source",
                        icon = Icons.Default.Info,
                        onClick = { currentDestination = SettingsDestination.ABOUT }
                    )

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.APPEARANCE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 1. Official Themes
                    SettingsSectionTitle("Official Themes (7 Styles)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        viewModel.officialThemes.forEach { theme ->
                            val isSelected = theme.id == selectedThemeId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(theme.background)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) colors.primaryAccent else theme.surfaceHighlight,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectTheme(theme.id) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(theme.primaryAccent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = theme.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = theme.textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Card Style Mode
                    SettingsSectionTitle("Card Style Mode")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            CardStyleMode.DEFAULT to "Default Cards",
                            CardStyleMode.THEME_SPECIFIC to "Theme Cards"
                        ).forEach { (mode, label) ->
                            val isSelected = cardStyleMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.surfaceHighlight else colors.surfaceElevated)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) colors.primaryAccent else colors.surfaceHighlight,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setCardStyleMode(mode) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.primaryAccent else colors.textPrimary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Custom Player Background
                    SettingsSectionTitle("Player Background")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Choose Photo")
                        }

                        if (wallpaperConfig.customImageUri != null) {
                            OutlinedButton(
                                onClick = { viewModel.setCustomPlayerBackground(null) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Remove")
                            }
                        }
                    }

                    if (wallpaperConfig.mode == WallpaperMode.CUSTOM_IMAGE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Blur: ${wallpaperConfig.blurRadius.toInt()}dp",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                        )
                        Slider(
                            value = wallpaperConfig.blurRadius,
                            onValueChange = { viewModel.updateCustomBackgroundParams(it, wallpaperConfig.dimmingAlpha) },
                            valueRange = 10f..80f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primaryAccent,
                                activeTrackColor = colors.primaryAccent
                            )
                        )

                        Text(
                            text = "Darkening Overlay: ${(wallpaperConfig.dimmingAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                        )
                        Slider(
                            value = wallpaperConfig.dimmingAlpha,
                            onValueChange = { viewModel.updateCustomBackgroundParams(wallpaperConfig.blurRadius, it) },
                            valueRange = 0f..0.9f,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primaryAccent,
                                activeTrackColor = colors.primaryAccent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Advanced Customization Link
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.surfaceHighlight, RoundedCornerShape(12.dp))
                            .clickable { currentDestination = SettingsDestination.ADVANCED_CUSTOMIZATION }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Advanced Customization",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Batch playback control colors, overrides, and theme reset",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.ADVANCED_CUSTOMIZATION -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("Batch Playback Control Colors")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Instantly tint play, pause, shuffle, and seek controls in the player.",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val batchColorOptions = listOf(
                        null to "Theme Default",
                        0xFFE58B24L to "Amber",
                        0xFFFFD600L to "Yellow",
                        0xFF00B4D8L to "Cyan",
                        0xFF22C55EL to "Green",
                        0xFFEC4899L to "Pink",
                        0xFF9D4EDDL to "Purple",
                        0xFFFFFFFFL to "White"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        batchColorOptions.forEach { (colorVal, label) ->
                            val isSelected = advancedAppearance.batchPlaybackControlColor == colorVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.surfaceHighlight else colors.surfaceElevated)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) colors.primaryAccent else colors.surfaceHighlight,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setBatchControlColor(colorVal) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (colorVal != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorVal))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colors.primaryAccent else colors.textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.resetAdvancedAppearance()
                                Toast.makeText(context, "Advanced appearance reset", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Reset Advanced Appearance")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.restoreThemeDefaults()
                                Toast.makeText(context, "Theme defaults restored", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Restore Theme Defaults")
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.AUDIO_PLAYBACK -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("Audio quality")
                    AudioQualityMode.entries.forEach { mode ->
                        SettingsCategoryCard(
                            title = (if (qualityMode == mode) "ÃƒÂ¢Ã…â€œÃ¢â‚¬Å“ " else "") + mode.label,
                            subtitle = mode.description,
                            icon = if (qualityMode == mode) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            onClick = { viewModel.setAudioQualityMode(mode) }
                        )
                    }
                    Text(
                        text = "Auto adapts to connection costs. Quality changes resume the current song at its playback position. Available formats vary by song.",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (BuildConfig.DEBUG) {
                        SettingsSectionTitle("Network usage this process")
                        SettingsDetailCard {
                            SettingsInfoRow("Audio received", traffic.audioBytes.toString() + " bytes")
                            SettingsInfoRow("Artwork received", traffic.artworkBytes.toString() + " bytes")
                            SettingsInfoRow("API received", traffic.apiBytes.toString() + " bytes")
                            SettingsInfoRow("Network requests", traffic.requests.toString())
                            SettingsInfoRow("Metadata cache hits", traffic.metadataCacheHits.toString())
                            SettingsInfoRow("Artwork cache hits", traffic.artworkCacheHits.toString())
                        }
                        OutlinedButton(onClick = { viewModel.sampleTraffic() }) { Text("Sample traffic") }
                        Text(
                            text = "Counts HTTP response payload bytes read, including retries. Headers, TLS, DNS, and socket overhead are excluded.",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SettingsSectionTitle("Streaming & Playback Engine")
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsDetailCard {
                        SettingsInfoRow(title = "Streaming Engine", value = "InnerTubeX + YouTube Music fallback")
                        SettingsInfoRow(title = "Stream Quality", value = "Selected from available source formats")
                        SettingsInfoRow(title = "Audio Pipeline", value = "Media3 ExoPlayer")
                        SettingsInfoRow(title = "Audio Focus Handling", value = "Automatic ducking / pause on phone calls")
                        SettingsInfoRow(title = "Headphone Disconnect", value = "Auto-pause on unplug / Bluetooth disconnect")
                        SettingsInfoRow(title = "Shuffle Algorithm", value = "Balanced Fisher-Yates (Artist Diversity)")
                        SettingsInfoRow(title = "Forward Buffer", value = "5ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Å“15 seconds")
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.RECOMMENDATIONS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("SYNAPSE Autonomous Engine")
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsDetailCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SYNAPSE Recommendation Engine",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Adaptive AI engine with contextual skip learning, duration clamping, and MMR diversity re-ranking",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isSynapseEnabled,
                                onCheckedChange = { viewModel.setSynapseEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.primaryAccent,
                                    checkedTrackColor = colors.surfaceHighlight
                                )
                            )
                        }

                        if (isSynapseEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(colors.divider)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Discovery Calibration (MMR)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.primaryAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (synapseDiscoveryAlpha > 0.70f) "Familiar (70%)" else if (synapseDiscoveryAlpha < 0.45f) "Adventurous (35%)" else "Balanced (65%)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = colors.primaryAccent,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Balances familiar favorites against exploratory long-tail music",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = synapseDiscoveryAlpha,
                                    onValueChange = { viewModel.setSynapseDiscoveryAlpha(it) },
                                    valueRange = 0.20f..0.90f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.primaryAccent,
                                        activeTrackColor = colors.primaryAccent,
                                        inactiveTrackColor = colors.surfaceHighlight
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Adventurous (0.2)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            color = colors.textSecondary
                                        ),
                                        modifier = Modifier.weight(1.25f),
                                        textAlign = TextAlign.Start,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Balanced (0.65)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            color = colors.textSecondary
                                        ),
                                        modifier = Modifier.weight(1.1f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Familiar (0.9)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            color = colors.textSecondary
                                        ),
                                        modifier = Modifier.weight(1.0f),
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSectionTitle("Taste Profile & Signals")
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsDetailCard {
                        val displayLanguages = when {
                            preferredLanguages.isEmpty() -> "None (Global Discovery)"
                            preferredLanguages.size > 3 -> "${preferredLanguages.take(3).joinToString()}, +${preferredLanguages.size - 3} more"
                            else -> preferredLanguages.joinToString()
                        }
                        SettingsInfoRow(title = "Preferred Languages", value = displayLanguages)
                        SettingsInfoRow(title = "Preferred Genres", value = preferredGenres.joinToString())
                        SettingsInfoRow(title = "Recommendation Sources", value = "YouTube Music Home Shelves + Song Radio")
                        SettingsInfoRow(title = "Taste Signal Weights", value = "Skip (-20), Listen 30s+ (+30), Like (+30)")
                        SettingsInfoRow(title = "Cross-Section Deduplication", value = "Strict Unique Filtering Active")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showOnboardingDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAccent,
                                contentColor = if (colors.primaryAccent == Color.White) Color.Black else colors.background
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (colors.primaryAccent == Color.White) Color.Black else colors.background
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tune Taste Preferences",
                                fontWeight = FontWeight.Bold,
                                color = if (colors.primaryAccent == Color.White) Color.Black else colors.background
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetRecommendationPreferences()
                                Toast.makeText(context, "Taste preferences reset to defaults", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Reset Taste Profile")
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.CONTENT_PREFERENCES -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("Content Filtering")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Control what catalog items appear in Search, Home discovery shelves, and recommendations.",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Music Only
                    val isMusicOnly = contentFilterMode == ContentFilterMode.MUSIC_ONLY
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMusicOnly) colors.surfaceHighlight else colors.surfaceElevated)
                            .border(
                                width = if (isMusicOnly) 1.5.dp else 0.5.dp,
                                color = if (isMusicOnly) colors.primaryAccent else colors.surfaceHighlight,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setContentFilterMode(ContentFilterMode.MUSIC_ONLY) }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (isMusicOnly) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isMusicOnly) colors.primaryAccent else colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Music Only (Recommended)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMusicOnly) colors.primaryAccent else colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Strictly music tracks, albums, singles, remixes, and official music videos. Suppresses podcasts, talk shows, interviews, reaction videos, commentary, and audiobooks.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 2: All Content
                    val isAllContent = contentFilterMode == ContentFilterMode.ALL_CONTENT
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAllContent) colors.surfaceHighlight else colors.surfaceElevated)
                            .border(
                                width = if (isAllContent) 1.5.dp else 0.5.dp,
                                color = if (isAllContent) colors.primaryAccent else colors.surfaceHighlight,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setContentFilterMode(ContentFilterMode.ALL_CONTENT) }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (isAllContent) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isAllContent) colors.primaryAccent else colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "All Content",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAllContent) colors.primaryAccent else colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Disables nonmusic filtering. Displays all catalog results including podcasts, interviews, commentary, and live broadcasts.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.LIBRARY_DATA -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("Library & Storage Architecture")
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsDetailCard {
                        SettingsInfoRow(title = "Database Engine", value = "Room 2.6.1 (SQLite)")
                        SettingsInfoRow(title = "Storage Location", value = "Local Private App Sandbox")
                        SettingsInfoRow(title = "Liked Songs Index", value = "Indexed with timestamp & listen metrics")
                        SettingsInfoRow(title = "Playlists", value = "Custom playlists with drag-and-drop order")
                        SettingsInfoRow(title = "Cloud Synchronization", value = "None (Offline-first local privacy)")
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.STORAGE_CACHE -> {
                LaunchedEffect(Unit) { viewModel.refreshStorage() }
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("Audio cache")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Automatic audio caching", color = colors.textPrimary)
                        Switch(checked = cacheEnabled, onCheckedChange = viewModel::setAudioCaching)
                    }
                    SettingsDetailCard {
                        SettingsInfoRow("Used", cacheStats.usedBytes?.let { formatCacheBytes(it) } ?: "Unavailable")
                        SettingsInfoRow("Maximum", if (cacheLimitMb == -1) "Unlimited (storage reserve applies)"
                            else if (cacheLimitMb == 0) "Disabled" else formatCacheBytes(cacheStats.maxBytes))
                        SettingsInfoRow("Completely cached songs", cacheStats.completeSongs.toString())
                        SettingsInfoRow("Partially cached songs", cacheStats.partialSongs.toString())
                        if (BuildConfig.DEBUG) SettingsInfoRow("Full replay hits / misses", "${cacheStats.hits} / ${cacheStats.misses}")
                    }
                    Text(
                        if (BuildConfig.DEBUG) "Audio received during playback is kept for replay. Only validated complete songs play offline."
                        else "Stores audio from providers that permit caching.",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioCacheLimits.optionsMb.forEach { mb ->
                            OutlinedButton(onClick = { viewModel.setAudioCacheLimit(mb) }) {
                                Text((if (cacheStats.maxBytes == AudioCacheLimits.bytes(mb)) "✓ " else "") +
                                    when (mb) { 0 -> "Disabled"; -1 -> "Unlimited"; 1000 -> "1 GB"; else -> "$mb MB" })
                            }
                        }
                    }
                    OutlinedButton(onClick = { viewModel.clearAudioCache() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear audio cache")
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Wi-Fi-only next-song prefetch", modifier = Modifier.weight(1f), color = colors.textPrimary)
                        Switch(checked = prefetchEnabled, onCheckedChange = viewModel::setAudioPrefetch)
                    }
                    Text("Off by default. Up to 1 MiB on unmetered Wi-Fi.",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary))
                    cacheStats.error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(color = colors.error))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsSectionTitle("Artwork cache")
                    SettingsDetailCard {
                        SettingsInfoRow("Used on disk", artworkBytes?.let { formatCacheBytes(it) } ?: "Loading…")
                        SettingsInfoRow("Maximum", "64 MiB")
                    }
                    OutlinedButton(onClick = { viewModel.clearArtworkCache() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear artwork cache")
                    }
                    OutlinedButton(onClick = { viewModel.refreshStorage() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Refresh usage")
                    }
                    storageMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary))
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.PRIVACY -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("Privacy Controls")
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.surfaceHighlight, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Record Listening History",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Used strictly on-device to power your Daily Mix and taste profile",
                                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                            )
                        }
                        Switch(
                            checked = isHistoryEnabled,
                            onCheckedChange = { viewModel.setHistoryEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = colors.primaryAccent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.clearListeningHistory()
                            Toast.makeText(context, "Listening history cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Clear Listening History")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsDetailCard {
                        SettingsInfoRow(title = "Telemetry / Tracking", value = "0% (Zero analytics SDKs)")
                        SettingsInfoRow(title = "Account Requirement", value = "None (No login needed)")
                        SettingsInfoRow(title = "Data Storage", value = "100% On-Device Local SQLite")
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            SettingsDestination.ABOUT -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SettingsSectionTitle("About STEREO")
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsDetailCard {
                        SettingsInfoRow(title = "Application", value = "STEREO")
                        SettingsInfoRow(title = "Release", value = "v2.1.0 Master Release")
                        SettingsInfoRow(title = "Package", value = "com.nova.music")
                        SettingsInfoRow(title = "Platform", value = "Native Android (Kotlin + Jetpack Compose)")
                        SettingsInfoRow(title = "Architecture", value = "Clean MVVM + Media3 ExoPlayer + Koin DI")
                        SettingsInfoRow(title = "License", value = "Open Source (₹0 Budget, Zero Paid APIs)")
                        SettingsInfoRow(title = "Audio Engine", value = "InnerTubeX audio extractor")
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = StereoTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.surfaceHighlight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary,
                        lineHeight = 16.sp
                    )
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    val colors = StereoTheme.colors
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = colors.primaryAccent
        )
    )
}

@Composable
private fun SettingsDetailCard(content: @Composable () -> Unit) {
    val colors = StereoTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.surfaceHighlight, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    val colors = StereoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

private fun formatCacheBytes(bytes: Long): String =
    String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1_000_000.0)
