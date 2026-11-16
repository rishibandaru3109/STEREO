package com.nova.music.feature.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
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
import com.nova.music.core.designsystem.theme.ContrastValidator
import com.nova.music.core.designsystem.theme.CustomTheme
import com.nova.music.core.designsystem.theme.StereoColors
import com.nova.music.core.designsystem.theme.StereoTheme

// Curated high-fidelity color swatches for quick, tactile selection
private val curatedColors = listOf(
    Color(0xFFFF6B35), // Stereo Amber
    Color(0xFFFFA726), // Bright Orange
    Color(0xFFD97706), // Copper
    Color(0xFFFF9F1C), // Gold
    Color(0xFFC85A32), // Terracotta
    Color(0xFFE05A78), // Burgundy Rose
    Color(0xFF38BDF8), // Electric Cyan
    Color(0xFF0284C7), // Ocean Blue
    Color(0xFF8F9E7B), // Olive Green
    Color(0xFFC084FC), // Lavender
    Color(0xFFFFFFFF), // Pure White
    Color(0xFFF8F5EE), // Cream
    Color(0xFFDCD7C9), // Paper Surface
    Color(0xFF453B5D), // Charcoal Purple
    Color(0xFF24242D), // Studio Surface
    Color(0xFF1B1B22), // Deep Charcoal
    Color(0xFF0D0D11), // Midnight Black
    Color(0xFF000000)  // Pure AMOLED
)

enum class ColorSlot {
    PRIMARY_ACCENT,
    SECONDARY_ACCENT,
    BACKGROUND,
    SURFACE,
    TEXT_PRIMARY,
    CONTROL_ACTIVE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeStudioScreen(
    viewModel: ThemeViewModel,
    initialTheme: StereoColors = StereoTheme.colors,
    onBackClick: () -> Unit,
    onThemeSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColors = StereoTheme.colors

    var themeName by remember { mutableStateOf("${initialTheme.name} (Custom)") }
    var isDark by remember { mutableStateOf(initialTheme.isDark) }
    var primaryAccent by remember { mutableStateOf(initialTheme.primaryAccent) }
    var secondaryAccent by remember { mutableStateOf(initialTheme.secondaryAccent) }
    var backgroundColor by remember { mutableStateOf(initialTheme.background) }
    var surfaceColor by remember { mutableStateOf(initialTheme.surface) }
    var surfaceElevatedColor by remember { mutableStateOf(initialTheme.surfaceElevated) }
    var textPrimaryColor by remember { mutableStateOf(initialTheme.textPrimary) }
    var textSecondaryColor by remember { mutableStateOf(initialTheme.textSecondary) }
    var controlActiveColor by remember { mutableStateOf(initialTheme.controlActive) }

    var selectedSlot by remember { mutableStateOf(ColorSlot.PRIMARY_ACCENT) }
    var showExportDialog by remember { mutableStateOf(false) }

    val contrastRatio = remember(textPrimaryColor, backgroundColor) {
        ContrastValidator.calculateContrast(textPrimaryColor, backgroundColor)
    }
    val hasGoodContrast = contrastRatio >= 3.0f

    val draftTheme = remember(
        themeName, isDark, primaryAccent, secondaryAccent,
        backgroundColor, surfaceColor, surfaceElevatedColor,
        textPrimaryColor, textSecondaryColor, controlActiveColor
    ) {
        CustomTheme(
            name = themeName,
            isDark = isDark,
            background = backgroundColor,
            surface = surfaceColor,
            surfaceElevated = surfaceElevatedColor,
            primaryAccent = primaryAccent,
            secondaryAccent = secondaryAccent,
            textPrimary = textPrimaryColor,
            textSecondary = textSecondaryColor,
            controlActive = controlActiveColor
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(activeColors.background)
    ) {
        // Top App Bar
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
                    text = "THEME STUDIO",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = activeColors.textPrimary
                )
                Text(
                    text = "Custom Palette & Real-Time Engine",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeColors.textTertiary
                )
            }

            IconButton(
                onClick = { showExportDialog = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(activeColors.surfaceHighlight)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Export JSON",
                    tint = activeColors.primaryAccent
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
                    text = "LIVE THEME PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = activeColors.textTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThemePresetCard(
                    preset = draftTheme.toStereoColors(),
                    isSelected = true,
                    onClick = { /* Preview only */ }
                )
            }

            // Contrast Validation Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hasGoodContrast) activeColors.surface else activeColors.error.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (hasGoodContrast) activeColors.surfaceHighlight else activeColors.error,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasGoodContrast) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasGoodContrast) activeColors.primaryAccent else activeColors.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasGoodContrast) {
                                    "Accessible Contrast (${"%.1f".format(contrastRatio)}:1)"
                                } else {
                                    "Low Contrast Warning (${"%.1f".format(contrastRatio)}:1)"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (hasGoodContrast) activeColors.textPrimary else activeColors.error
                            )
                            Text(
                                text = if (hasGoodContrast) {
                                    "Text and background colors meet WCAG readability standards."
                                } else {
                                    "Text may be difficult to read. Recommended ratio is at least 3.0:1."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = activeColors.textSecondary
                            )
                        }
                    }
                }
            }

            // Theme Name Input
            item {
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("Theme Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = activeColors.textPrimary,
                        unfocusedTextColor = activeColors.textPrimary,
                        focusedBorderColor = activeColors.primaryAccent,
                        unfocusedBorderColor = activeColors.surfaceHighlight,
                        focusedLabelColor = activeColors.primaryAccent,
                        unfocusedLabelColor = activeColors.textTertiary
                    ),
                    singleLine = true
                )
            }

            // Dark / Light Palette Mode Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { isDark = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) activeColors.primaryAccent.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) activeColors.primaryAccent else activeColors.surfaceHighlight
                        )
                    ) {
                        Text(
                            text = "Dark Studio",
                            color = if (isDark) activeColors.primaryAccent else activeColors.textSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { isDark = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!isDark) activeColors.primaryAccent.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isDark) activeColors.primaryAccent else activeColors.surfaceHighlight
                        )
                    ) {
                        Text(
                            text = "Light Paper",
                            color = if (!isDark) activeColors.primaryAccent else activeColors.textSecondary
                        )
                    }
                }
            }

            // Color Slot Selection Chips
            item {
                Text(
                    text = "SELECT COLOR SLOT TO CUSTOMIZE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = activeColors.textTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorSlotChip(
                        name = "Primary Accent",
                        color = primaryAccent,
                        isSelected = selectedSlot == ColorSlot.PRIMARY_ACCENT,
                        onClick = { selectedSlot = ColorSlot.PRIMARY_ACCENT }
                    )
                    ColorSlotChip(
                        name = "Secondary Accent",
                        color = secondaryAccent,
                        isSelected = selectedSlot == ColorSlot.SECONDARY_ACCENT,
                        onClick = { selectedSlot = ColorSlot.SECONDARY_ACCENT }
                    )
                    ColorSlotChip(
                        name = "Background",
                        color = backgroundColor,
                        isSelected = selectedSlot == ColorSlot.BACKGROUND,
                        onClick = { selectedSlot = ColorSlot.BACKGROUND }
                    )
                    ColorSlotChip(
                        name = "Surface Card",
                        color = surfaceColor,
                        isSelected = selectedSlot == ColorSlot.SURFACE,
                        onClick = { selectedSlot = ColorSlot.SURFACE }
                    )
                    ColorSlotChip(
                        name = "Primary Text",
                        color = textPrimaryColor,
                        isSelected = selectedSlot == ColorSlot.TEXT_PRIMARY,
                        onClick = { selectedSlot = ColorSlot.TEXT_PRIMARY }
                    )
                    ColorSlotChip(
                        name = "Controls",
                        color = controlActiveColor,
                        isSelected = selectedSlot == ColorSlot.CONTROL_ACTIVE,
                        onClick = { selectedSlot = ColorSlot.CONTROL_ACTIVE }
                    )
                }
            }

            // Palette Swatches for currently selected slot
            item {
                Text(
                    text = "SWATCH PALETTE FOR ${selectedSlot.name.replace("_", " ")}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = activeColors.textTertiary
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (swatch in curatedColors) {
                        val isCurrentColor = when (selectedSlot) {
                            ColorSlot.PRIMARY_ACCENT -> primaryAccent == swatch
                            ColorSlot.SECONDARY_ACCENT -> secondaryAccent == swatch
                            ColorSlot.BACKGROUND -> backgroundColor == swatch
                            ColorSlot.SURFACE -> surfaceColor == swatch
                            ColorSlot.TEXT_PRIMARY -> textPrimaryColor == swatch
                            ColorSlot.CONTROL_ACTIVE -> controlActiveColor == swatch
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (isCurrentColor) 3.dp else 1.dp,
                                    color = if (isCurrentColor) activeColors.primaryAccent else activeColors.surfaceHighlight,
                                    shape = CircleShape
                                )
                                .clickable {
                                    when (selectedSlot) {
                                        ColorSlot.PRIMARY_ACCENT -> primaryAccent = swatch
                                        ColorSlot.SECONDARY_ACCENT -> secondaryAccent = swatch
                                        ColorSlot.BACKGROUND -> backgroundColor = swatch
                                        ColorSlot.SURFACE -> {
                                            surfaceColor = swatch
                                            surfaceElevatedColor = swatch.copy(alpha = 0.9f)
                                        }
                                        ColorSlot.TEXT_PRIMARY -> textPrimaryColor = swatch
                                        ColorSlot.CONTROL_ACTIVE -> controlActiveColor = swatch
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentColor) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (swatch.red * 0.3 + swatch.green * 0.59 + swatch.blue * 0.11 > 0.5) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save and Cancel Actions
            item {
                Spacer(modifier = Modifier.height(12.dp))
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
                            viewModel.saveCustomTheme(draftTheme)
                            onThemeSaved()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColors.primaryAccent,
                            contentColor = if (activeColors.isDark) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = "Save Theme",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Space for bottom sheet/dock
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text(
                    text = "Export Theme JSON",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = activeColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Share or backup your custom theme:",
                        style = MaterialTheme.typography.bodySmall,
                        color = activeColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeColors.surfaceElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = draftTheme.toJson(),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = activeColors.textPrimary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = activeColors.primaryAccent)
                }
            },
            containerColor = activeColors.surface
        )
    }
}

@Composable
private fun ColorSlotChip(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColors = StereoTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) activeColors.surfaceHighlight else activeColors.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) activeColors.primaryAccent else activeColors.surfaceHighlight,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) activeColors.primaryAccent else activeColors.textPrimary
        )
    }
}
