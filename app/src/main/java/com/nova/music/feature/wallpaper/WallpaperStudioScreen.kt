package com.nova.music.feature.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.core.designsystem.wallpaper.GradientPreset
import com.nova.music.core.designsystem.wallpaper.WallpaperBackdrop
import com.nova.music.core.designsystem.wallpaper.WallpaperConfig
import com.nova.music.core.designsystem.wallpaper.WallpaperMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperStudioScreen(
    viewModel: WallpaperViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedConfig by viewModel.wallpaperConfig.collectAsStateWithLifecycle()
    val activeColors = StereoTheme.colors

    var mode by remember(savedConfig) { mutableStateOf(savedConfig.mode) }
    var gradientPreset by remember(savedConfig) { mutableStateOf(savedConfig.gradientPreset) }
    var blurRadius by remember(savedConfig) { mutableFloatStateOf(savedConfig.blurRadius) }
    var dimmingAlpha by remember(savedConfig) { mutableFloatStateOf(savedConfig.dimmingAlpha) }
    var isDesaturated by remember(savedConfig) { mutableStateOf(savedConfig.isDesaturated) }
    var applyToPlayerOnly by remember(savedConfig) { mutableStateOf(savedConfig.applyToPlayerOnly) }

    val currentDraft = remember(mode, gradientPreset, blurRadius, dimmingAlpha, isDesaturated, applyToPlayerOnly) {
        WallpaperConfig(
            mode = mode,
            gradientPreset = gradientPreset,
            blurRadius = blurRadius,
            dimmingAlpha = dimmingAlpha,
            isDesaturated = isDesaturated,
            applyToPlayerOnly = applyToPlayerOnly
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(activeColors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(activeColors.surfaceHighlight)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = activeColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WALLPAPER STUDIO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = activeColors.textPrimary
                )
                Text(
                    text = "Backdrops, Gradients & Dynamic Blur",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeColors.textTertiary
                )
            }

            IconButton(
                onClick = {
                    viewModel.resetToDefault()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(activeColors.surfaceHighlight)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Wallpaper",
                    tint = activeColors.textSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            item {
                Text(
                    text = "LIVE BACKDROP PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = activeColors.textTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, activeColors.surfaceHighlight, RoundedCornerShape(16.dp))
                ) {
                    WallpaperBackdrop(
                        config = currentDraft,
                        artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Floating Mockup Controls Inside Preview
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "STEREO HI-FI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    ),
                                    color = activeColors.primaryAccent
                                )
                                Text(
                                    text = if (applyToPlayerOnly) "PLAYER ONLY" else "ALL SCREENS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = activeColors.textSecondary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(activeColors.surfaceElevated)
                                        .border(1.dp, activeColors.primaryAccent, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = activeColors.primaryAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "High-Fidelity Soundstage",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = activeColors.textPrimary
                                    )
                                    Text(
                                        text = "Tactile Studio Backing",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = activeColors.textSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(activeColors.primaryAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (activeColors.isDark) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Backdrop Mode Selector
            item {
                Text(
                    text = "BACKDROP MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = activeColors.textTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WallpaperModeChip(
                        title = "Solid Theme",
                        isSelected = mode == WallpaperMode.THEME_SOLID,
                        onClick = { mode = WallpaperMode.THEME_SOLID },
                        modifier = Modifier.weight(1f)
                    )
                    WallpaperModeChip(
                        title = "Gradients",
                        isSelected = mode == WallpaperMode.GRADIENT,
                        onClick = { mode = WallpaperMode.GRADIENT },
                        modifier = Modifier.weight(1f)
                    )
                    WallpaperModeChip(
                        title = "Album Art",
                        isSelected = mode == WallpaperMode.ALBUM_ART,
                        onClick = { mode = WallpaperMode.ALBUM_ART },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mode Details: Gradients
            if (mode == WallpaperMode.GRADIENT) {
                item {
                    Text(
                        text = "GRADIENT PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = activeColors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (preset in GradientPreset.entries) {
                            val isSelected = gradientPreset == preset
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(preset.getBrush(activeColors))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) activeColors.primaryAccent else activeColors.surfaceHighlight,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { gradientPreset = preset }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = preset.name.replace("_", " "),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = activeColors.primaryAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mode Details: Album Art
            if (mode == WallpaperMode.ALBUM_ART) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeColors.surface)
                            .border(1.dp, activeColors.surfaceHighlight, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Blur Radius Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Blur Radius",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = activeColors.textPrimary
                                )
                                Text(
                                    text = "${blurRadius.toInt()} dp",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = activeColors.primaryAccent
                                )
                            }
                            Slider(
                                value = blurRadius,
                                onValueChange = { blurRadius = it },
                                valueRange = 10f..80f,
                                colors = SliderDefaults.colors(
                                    thumbColor = activeColors.primaryAccent,
                                    activeTrackColor = activeColors.primaryAccent,
                                    inactiveTrackColor = activeColors.surfaceHighlight
                                )
                            )
                        }

                        // Dimming Alpha Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Dimming Overlay",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = activeColors.textPrimary
                                )
                                Text(
                                    text = "${(dimmingAlpha * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = activeColors.primaryAccent
                                )
                            }
                            Slider(
                                value = dimmingAlpha,
                                onValueChange = { dimmingAlpha = it },
                                valueRange = 0f..0.9f,
                                colors = SliderDefaults.colors(
                                    thumbColor = activeColors.primaryAccent,
                                    activeTrackColor = activeColors.primaryAccent,
                                    inactiveTrackColor = activeColors.surfaceHighlight
                                )
                            )
                        }

                        // Desaturation Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Monochrome Artwork",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = activeColors.textPrimary
                                )
                                Text(
                                    text = "Desaturate background art to black & white",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = activeColors.textSecondary
                                )
                            }
                            Switch(
                                checked = isDesaturated,
                                onCheckedChange = { isDesaturated = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = activeColors.primaryAccent,
                                    checkedTrackColor = activeColors.surfaceElevated
                                )
                            )
                        }
                    }
                }
            }

            // Scope Toggle: Player Only vs All Screens
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(activeColors.surface)
                        .border(1.dp, activeColors.surfaceHighlight, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply to Full Player Only",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = activeColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (applyToPlayerOnly) "Browse screens retain pure theme background" else "Wallpaper active across all app screens",
                            style = MaterialTheme.typography.bodySmall,
                            color = activeColors.textSecondary
                        )
                    }
                    Switch(
                        checked = applyToPlayerOnly,
                        onCheckedChange = { applyToPlayerOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = activeColors.primaryAccent,
                            checkedTrackColor = activeColors.surfaceElevated
                        )
                    )
                }
            }

            // Save and Apply Buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = activeColors.textSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, activeColors.surfaceHighlight)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            viewModel.updateConfig(currentDraft)
                            onBackClick()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColors.primaryAccent,
                            contentColor = if (activeColors.isDark) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = "Apply Wallpaper",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun WallpaperModeChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColors = StereoTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activeColors.primaryAccent.copy(alpha = 0.15f) else activeColors.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) activeColors.primaryAccent else activeColors.surfaceHighlight,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) activeColors.primaryAccent else activeColors.textSecondary
        )
    }
}
