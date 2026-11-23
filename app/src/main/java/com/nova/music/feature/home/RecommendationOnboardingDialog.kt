package com.nova.music.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.LanguageRegion
import com.nova.music.domain.model.StereoLanguage
import com.nova.music.domain.model.StereoLanguages

val AVAILABLE_LANGUAGES = StereoLanguages.ALL.map { it.name }
val AVAILABLE_GENRES = listOf("Pop", "Rock", "Hip-Hop", "Indie", "Electronic", "Acoustic", "R&B", "Classical", "Jazz", "Lo-Fi")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecommendationOnboardingDialog(
    initialLanguages: Set<String> = setOf("English", "Hindi"),
    initialGenres: Set<String> = setOf("Pop", "Rock", "Chill"),
    initialDiscoveryMode: String = "Familiar",
    onDismiss: () -> Unit,
    onSavePreferences: (languages: Set<String>, genres: Set<String>, discoveryMode: String) -> Unit
) {
    val colors = StereoTheme.colors

    val normalizedInitialLanguages: Set<String> = remember(initialLanguages) {
        val normalized = StereoLanguages.normalize(initialLanguages)
        if (normalized.isNotEmpty()) {
            normalized
        } else if (initialLanguages.isNotEmpty()) {
            initialLanguages
        } else {
            setOf("English", "Hindi")
        }
    }
    var selectedLanguages by remember { mutableStateOf(normalizedInitialLanguages) }
    var selectedGenres by remember { mutableStateOf(initialGenres) }
    var discoveryMode by remember { mutableStateOf(initialDiscoveryMode) }
    var languageSearchQuery by remember { mutableStateOf("") }

    val languagesByRegion = remember { StereoLanguages.byRegion() }

    val filteredLanguages = remember(languageSearchQuery) {
        val query = languageSearchQuery.trim().lowercase()
        if (query.isEmpty()) {
            emptyList()
        } else {
            StereoLanguages.ALL.filter { lang ->
                lang.name.lowercase().contains(query) ||
                    lang.nativeName.lowercase().contains(query) ||
                    lang.id.lowercase().contains(query) ||
                    lang.region.displayName.lowercase().contains(query)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = colors.surfaceElevated,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primaryAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tune Your Sound",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            text = "Select your music languages (40 mainstream supported)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colors.textSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Languages Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCOVERY LANGUAGES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryAccent,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "${selectedLanguages.size} of 40 selected",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar for 40 Languages
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceHighlight)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = languageSearchQuery,
                            onValueChange = { languageSearchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.primaryAccent),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (languageSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search 40 languages (e.g. Spanish, Telugu)...",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (languageSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { languageSearchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Languages Content: Search Results vs Regional Categorization
                if (languageSearchQuery.isNotBlank()) {
                    if (filteredLanguages.isEmpty()) {
                        Text(
                            text = "No languages match \"$languageSearchQuery\"",
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredLanguages.forEach { lang ->
                                LanguageChip(
                                    lang = lang,
                                    isSelected = selectedLanguages.contains(lang.name),
                                    onToggle = { isSelected ->
                                        selectedLanguages = if (isSelected) {
                                            if (selectedLanguages.size > 1) selectedLanguages - lang.name else selectedLanguages
                                        } else {
                                            selectedLanguages + lang.name
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Organized by Region
                    LanguageRegion.values().forEach { region ->
                        val regionLanguages = languagesByRegion[region].orEmpty()
                        if (regionLanguages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${region.displayName.uppercase()} (${regionLanguages.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                regionLanguages.forEach { lang ->
                                    LanguageChip(
                                        lang = lang,
                                        isSelected = selectedLanguages.contains(lang.name),
                                        onToggle = { isSelected ->
                                            selectedLanguages = if (isSelected) {
                                                if (selectedLanguages.size > 1) selectedLanguages - lang.name else selectedLanguages
                                            } else {
                                                selectedLanguages + lang.name
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Genres Section
                Text(
                    text = "GENRES & MOODS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AVAILABLE_GENRES.forEach { genre ->
                        val isSelected = selectedGenres.contains(genre)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) colors.secondaryAccent else colors.surfaceHighlight
                                )
                                .clickable {
                                    selectedGenres = if (isSelected) {
                                        if (selectedGenres.size > 1) selectedGenres - genre else selectedGenres
                                    } else {
                                        selectedGenres + genre
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else colors.textPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Discovery Style
                Text(
                    text = "DISCOVERY STYLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryAccent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Familiar" to "Familiar Favorites", "Adventurous" to "Adventurous Explorer").forEach { (mode, label) ->
                        val isSelected = discoveryMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.surfaceHighlight else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) colors.primaryAccent else colors.surfaceHighlight,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { discoveryMode = mode }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primaryAccent else colors.textSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Skip", color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSavePreferences(selectedLanguages, selectedGenres, discoveryMode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = if (colors.primaryAccent == Color.White) Color.Black else colors.background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Save & Discover",
                            fontWeight = FontWeight.Bold,
                            color = if (colors.primaryAccent == Color.White) Color.Black else colors.background
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(
    lang: StereoLanguage,
    isSelected: Boolean,
    onToggle: (isSelected: Boolean) -> Unit
) {
    val colors = StereoTheme.colors
    val chipTextColor = if (isSelected) {
        if (colors.primaryAccent == Color.White) Color.Black else colors.background
    } else {
        colors.textPrimary
    }

    val label = if (lang.name == lang.nativeName) {
        lang.name
    } else {
        "${lang.name} (${lang.nativeName})"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) colors.primaryAccent else colors.surfaceHighlight)
            .clickable { onToggle(isSelected) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = chipTextColor
            )
        )
    }
}
