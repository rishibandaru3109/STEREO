package com.nova.music.feature.settings

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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.music.core.designsystem.theme.CardStyleMode
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.core.designsystem.wallpaper.WallpaperMode
import com.nova.music.feature.home.RecommendationOnboardingDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors
    val selectedThemeId by viewModel.selectedThemeId.collectAsStateWithLifecycle()
    val cardStyleMode by viewModel.cardStyleMode.collectAsStateWithLifecycle()
    val advancedAppearance by viewModel.advancedAppearance.collectAsStateWithLifecycle()
    val wallpaperConfig by viewModel.wallpaperConfig.collectAsStateWithLifecycle()
    val preferredLanguages by viewModel.preferredLanguages.collectAsStateWithLifecycle()
    val preferredGenres by viewModel.preferredGenres.collectAsStateWithLifecycle()
    val isHistoryEnabled by viewModel.isHistoryEnabled.collectAsStateWithLifecycle()

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
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Settings Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = colors.textPrimary
                )
            )
        }

        // Settings Body Scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ================================================================
            // SECTION 1: APPEARANCE (PHASE C, D, E, F)
            // ================================================================
            SettingsSectionHeader(title = "APPEARANCE", icon = Icons.Default.Palette)

            // 1. Official Themes
            Text(
                text = "Official Themes (7 Styles)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            )
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

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Card Style Mode (Phase D)
            Text(
                text = "Card Style Mode (Phase D)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            )
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

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Custom Player Background (Phase E)
            Text(
                text = "Custom Player Background (Phase E)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            )
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
                Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Batch Playback Control Color (Phase F)
            Text(
                text = "Batch Playback Control Colors (Phase F)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                            .padding(horizontal = 10.dp, vertical = 6.dp)
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

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.resetAdvancedAppearance() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Reset Advanced Appearance")
                }
                OutlinedButton(
                    onClick = { viewModel.restoreThemeDefaults() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Restore Theme Defaults")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================================================================
            // SECTION 2: AUDIO & PLAYBACK (PHASE G2)
            // ================================================================
            SettingsSectionHeader(title = "AUDIO & PLAYBACK", icon = Icons.Default.GraphicEq)

            SettingsInfoRow(title = "Streaming Engine", value = "InnerTubeX Lossless + YouTube Music Fallback")
            SettingsInfoRow(title = "Output Sample Rate", value = "48000 Hz, 32-bit PCM")
            SettingsInfoRow(title = "Audio Focus & Headset", value = "Enabled (Auto-pause on call/disconnect)")
            SettingsInfoRow(title = "Shuffle Algorithm", value = "Balanced Fisher-Yates (Artist Diversity)")

            Spacer(modifier = Modifier.height(24.dp))

            // ================================================================
            // SECTION 3: RECOMMENDATIONS & TASTE (PHASE H)
            // ================================================================
            SettingsSectionHeader(title = "RECOMMENDATIONS", icon = Icons.Default.Tune)

            Text(
                text = "Preferred Languages: ${preferredLanguages.joinToString()}",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Preferred Genres: ${preferredGenres.joinToString()}",
                style = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showOnboardingDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent, contentColor = Color.Black)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tune Preferences", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.resetRecommendationPreferences() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Reset Taste Profile")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================================================================
            // SECTION 4: LIBRARY & PRIVACY (PHASE G4, G7)
            // ================================================================
            SettingsSectionHeader(title = "LIBRARY & PRIVACY", icon = Icons.Default.Security)

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "Used to power your Daily Mix and listening profile",
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

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.clearListeningHistory() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Clear Listening History")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================================================================
            // SECTION 5: STORAGE (PHASE G5)
            // ================================================================
            SettingsSectionHeader(title = "STORAGE", icon = Icons.Default.Storage)

            OutlinedButton(
                onClick = { viewModel.clearCache() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Clear Thumbnail & Audio Cache")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================================================================
            // SECTION 6: ABOUT (PHASE G8)
            // ================================================================
            SettingsSectionHeader(title = "ABOUT", icon = Icons.Default.Info)

            SettingsInfoRow(title = "Application", value = "STEREO")
            SettingsInfoRow(title = "Version", value = "v2.0 (Phase 2 Master Build)")
            SettingsInfoRow(title = "Platform", value = "Native Android (Kotlin + Jetpack Compose)")
            SettingsInfoRow(title = "Architecture", value = "Clean MVVM + Media3 ExoPlayer + Koin DI")
            SettingsInfoRow(title = "License", value = "Open Source (Zero Paid APIs)")

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = StereoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.primaryAccent
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(title: String, value: String) {
    val colors = StereoTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        )
    }
}
