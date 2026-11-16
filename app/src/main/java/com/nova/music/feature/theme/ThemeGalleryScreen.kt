package com.nova.music.feature.theme

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.nova.music.core.designsystem.theme.CustomTheme
import com.nova.music.core.designsystem.theme.StereoColors
import com.nova.music.core.designsystem.theme.StereoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeGalleryScreen(
    viewModel: ThemeViewModel,
    onBackClick: (() -> Unit)? = null,
    onCreateCustomTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentThemeId by viewModel.currentThemeId.collectAsStateWithLifecycle()
    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
    val activeColors = StereoTheme.colors

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(activeColors.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(CircleShape)
                        .background(activeColors.surfaceHighlight)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = activeColors.textPrimary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "THEME GALLERY",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = activeColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(activeColors.primaryAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "12 PRESETS + STUDIO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = activeColors.primaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Instant 0ms audio switch",
                        style = MaterialTheme.typography.bodySmall,
                        color = activeColors.textTertiary
                    )
                }
            }

            IconButton(
                onClick = { viewModel.resetToDefault() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(activeColors.surfaceHighlight)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Theme",
                    tint = activeColors.textSecondary
                )
            }
        }

        // Action Row: Theme Studio & Import
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onCreateCustomTheme,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColors.primaryAccent,
                    contentColor = if (activeColors.isDark) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Create Theme",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = activeColors.textPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, activeColors.surfaceHighlight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Import JSON",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Themes List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Custom Themes Section
            if (customThemes.isNotEmpty()) {
                item {
                    Text(
                        text = "MY CUSTOM THEMES (${customThemes.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = activeColors.primaryAccent
                    )
                }

                items(
                    items = customThemes,
                    key = { it.id }
                ) { customTheme ->
                    val stereoColors = customTheme.toStereoColors()
                    val isSelected = stereoColors.id.equals(currentThemeId, ignoreCase = true)
                    Box {
                        ThemePresetCard(
                            preset = stereoColors,
                            isSelected = isSelected,
                            onClick = { viewModel.selectTheme(stereoColors.id) }
                        )

                        // Delete button for custom theme
                        IconButton(
                            onClick = { viewModel.deleteCustomTheme(customTheme.id) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(activeColors.surface.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Theme",
                                tint = activeColors.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Built-in Presets Section Header
            item {
                Text(
                    text = "BUILT-IN HI-FI PRESETS (12)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = activeColors.textTertiary
                )
            }

            // Presets Cards
            items(
                items = viewModel.allPresets,
                key = { it.id }
            ) { preset ->
                val isSelected = preset.id.equals(currentThemeId, ignoreCase = true)
                ThemePresetCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { viewModel.selectTheme(preset.id) }
                )
            }

            // Bottom spacing to avoid mini player overlap
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importError = null
            },
            title = {
                Text(
                    text = "Import Theme JSON",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = activeColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste a custom theme JSON string below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = activeColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6,
                        placeholder = { Text("Paste JSON here...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = activeColors.textPrimary,
                            unfocusedTextColor = activeColors.textPrimary,
                            focusedBorderColor = activeColors.primaryAccent,
                            unfocusedBorderColor = activeColors.surfaceHighlight
                        )
                    )
                    importError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = activeColors.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.importThemeJson(importJsonText.trim())
                        if (success) {
                            showImportDialog = false
                            importJsonText = ""
                            importError = null
                        } else {
                            importError = "Invalid theme JSON format. Please verify the JSON string."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColors.primaryAccent,
                        contentColor = if (activeColors.isDark) Color.Black else Color.White
                    )
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    importError = null
                }) {
                    Text("Cancel", color = activeColors.textSecondary)
                }
            },
            containerColor = activeColors.surface
        )
    }
}

@Composable
fun ThemePresetCard(
    preset: StereoColors,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColors = StereoTheme.colors
    val cardBorderColor = if (isSelected) {
        preset.primaryAccent
    } else {
        activeColors.surfaceHighlight
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(preset.background)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            // Preset Header: Name, Mode Pill & Selected Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = preset.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (preset.isDark) "DARK STUDIO PALETTE" else "LIGHT PAPER PALETTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = preset.textTertiary
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(preset.primaryAccent)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (preset.isDark) Color.Black else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = if (preset.isDark) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Mini-Mockup Preview Card inside the theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(preset.surface)
                    .border(1.dp, preset.surfaceHighlight, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mockup Album Art
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(preset.surfaceElevated)
                            .border(1.dp, preset.surfaceHighlight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = preset.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Mockup Titles & Progress Line
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hi-Fi Fidelity Stream",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = preset.textPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Opus 48kHz • Lossless Tone",
                            style = MaterialTheme.typography.bodySmall,
                            color = preset.textSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Progress bar preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(preset.sliderTrack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(3.dp)
                                    .background(preset.sliderThumb)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Mockup Play Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(preset.primaryAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (preset.isDark) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Swatch Row: Primary, Secondary, Surface, Elevated, Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorSwatchChip("Accent", preset.primaryAccent, preset.textTertiary)
                ColorSwatchChip("Gold", preset.secondaryAccent, preset.textTertiary)
                ColorSwatchChip("Surface", preset.surface, preset.textTertiary)
                ColorSwatchChip("Card", preset.surfaceElevated, preset.textTertiary)
                ColorSwatchChip("Text", preset.textPrimary, preset.textTertiary)
            }
        }
    }
}

@Composable
fun ColorSwatchChip(
    label: String,
    color: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = labelColor
        )
    }
}
