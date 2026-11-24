package com.nova.music.feature.library

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.Playlist
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    track: Track? = null,
    tracks: List<Track> = track?.let { listOf(it) } ?: emptyList(),
    viewModel: LibraryViewModel = koinViewModel(),
    onDismissRequest: () -> Unit
) {
    val colors = StereoTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val allPlaylists by viewModel.playlists.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Track which playlists already contain the single track
    var containingPlaylistIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val singleTrack = if (tracks.size == 1) tracks.first() else null

    LaunchedEffect(singleTrack?.id) {
        if (singleTrack != null) {
            viewModel.getPlaylistsContainingTrack(singleTrack.id).collect { ids ->
                containingPlaylistIds = ids
            }
        }
    }

    // Selected target playlists for multi-playlist add
    val selectedPlaylistIds = remember { mutableStateListOf<String>() }

    val filteredPlaylists = remember(allPlaylists, searchQuery) {
        if (searchQuery.isBlank()) {
            allPlaylists
        } else {
            val q = searchQuery.trim().lowercase()
            allPlaylists.filter { it.name.lowercase().contains(q) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.surfaceElevated,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCreatingNew) "New Playlist" else "Add to Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    if (!isCreatingNew && tracks.isNotEmpty()) {
                        val subtitle = if (tracks.size == 1) {
                            "\"${tracks[0].title}\" by ${tracks[0].artist}"
                        } else {
                            "${tracks.size} tracks selected"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            if (isCreatingNew) {
                // New Playlist Creation Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name *") },
                        placeholder = { Text("e.g. Synthwave Night", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryAccent,
                            unfocusedBorderColor = colors.surfaceHighlight,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.primaryAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("Add some notes...", color = colors.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryAccent,
                            unfocusedBorderColor = colors.surfaceHighlight,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.primaryAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isCreatingNew = false }) {
                            Text("Back", color = colors.textSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank() && !isSaving) {
                                    isSaving = true
                                    viewModel.createPlaylist(
                                        name = newPlaylistName.trim(),
                                        description = newPlaylistDesc.trim().takeIf { it.isNotBlank() }
                                    ) { newId ->
                                        if (tracks.isNotEmpty()) {
                                            viewModel.addTracksToPlaylist(newId, tracks) { addedCount ->
                                                Toast.makeText(
                                                    context,
                                                    "Created \"${newPlaylistName.trim()}\" and added ${tracks.size} track(s)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                coroutineScope.launch {
                                                    sheetState.hide()
                                                    onDismissRequest()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Created \"${newPlaylistName.trim()}\"", Toast.LENGTH_SHORT).show()
                                            coroutineScope.launch {
                                                sheetState.hide()
                                                onDismissRequest()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = newPlaylistName.isNotBlank() && !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.background
                                )
                            } else {
                                Text(
                                    text = if (tracks.isNotEmpty()) "Create & Add" else "Create",
                                    color = if (colors.isDark) colors.background else colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search playlists...", color = colors.textTertiary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryAccent,
                        unfocusedBorderColor = colors.surfaceHighlight,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primaryAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Quick Action: "Create new playlist"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isCreatingNew = true }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Create new playlist",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryAccent
                    )
                }

                // Playlists List
                if (filteredPlaylists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No playlists matching \"$searchQuery\"" else "No playlists yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredPlaylists, key = { it.id }) { playlist ->
                            val isAlreadyIn = containingPlaylistIds.contains(playlist.id)
                            val isSelected = selectedPlaylistIds.contains(playlist.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (singleTrack != null && selectedPlaylistIds.isEmpty() && !isAlreadyIn) {
                                            // Single-tap quick add
                                            viewModel.addTrackToPlaylist(playlist.id, singleTrack) { added ->
                                                if (added) {
                                                    Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Already in ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                }
                                                coroutineScope.launch {
                                                    sheetState.hide()
                                                    onDismissRequest()
                                                }
                                            }
                                        } else {
                                            // Multi-select toggle
                                            if (isSelected) {
                                                selectedPlaylistIds.remove(playlist.id)
                                            } else {
                                                selectedPlaylistIds.add(playlist.id)
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceHighlight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!playlist.coverArtworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = playlist.coverArtworkUrl,
                                            contentDescription = playlist.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistPlay,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.trackCount} tracks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textTertiary
                                    )
                                }

                                // Status Indicator
                                if (isAlreadyIn) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.primaryAccent.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.primaryAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Added",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.primaryAccent
                                        )
                                    }
                                } else {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedPlaylistIds.add(playlist.id)
                                            } else {
                                                selectedPlaylistIds.remove(playlist.id)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colors.primaryAccent,
                                            uncheckedColor = colors.controlInactive,
                                            checkmarkColor = colors.background
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Batch Add Confirm Button
                AnimatedVisibility(
                    visible = selectedPlaylistIds.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                        Button(
                            onClick = {
                                if (!isSaving && selectedPlaylistIds.isNotEmpty() && tracks.isNotEmpty()) {
                                    isSaving = true
                                    coroutineScope.launch {
                                        var totalAdded = 0
                                        selectedPlaylistIds.forEach { pid ->
                                            val count = viewModel.addTracksToPlaylist(pid, tracks)
                                        }
                                        val targetNames = allPlaylists
                                            .filter { it.id in selectedPlaylistIds }
                                            .joinToString(", ") { it.name }
                                        Toast.makeText(
                                            context,
                                            "Added to $targetNames",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        sheetState.hide()
                                        onDismissRequest()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.background
                                )
                            } else {
                                val label = if (tracks.size == 1) {
                                    "Add to ${selectedPlaylistIds.size} Playlist${if (selectedPlaylistIds.size > 1) "s" else ""}"
                                } else {
                                    "Add ${tracks.size} tracks to ${selectedPlaylistIds.size} Playlist${if (selectedPlaylistIds.size > 1) "s" else ""}"
                                }
                                Text(
                                    text = label,
                                    color = if (colors.isDark) colors.background else colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
