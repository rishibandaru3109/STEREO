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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
fun PlaylistDetailScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors
    val context = LocalContext.current
    val playlistWithTracks by viewModel.selectedPlaylistWithTracks.collectAsStateWithLifecycle()
    val displayedTracks by viewModel.displayedPlaylistTracks.collectAsStateWithLifecycle()
    val suggestedTracks by viewModel.suggestedTracksForPlaylist.collectAsStateWithLifecycle()
    val currentSort by viewModel.playlistSortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.playlistSearchQuery.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.playlists.collectAsStateWithLifecycle()

    var showEditDetailsDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }
    var editDescText by remember { mutableStateOf("") }
    var editCoverText by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearSongsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedTrackIds = remember { mutableStateListOf<String>() }
    var showMoveDialog by remember { mutableStateOf(false) }

    // Reorder mode state (only active in CUSTOM sort order)
    var isReorderMode by remember { mutableStateOf(false) }

    // Add to playlist bottom sheet
    var tracksForAddSheet by remember { mutableStateOf<List<Track>?>(null) }

    if (playlistWithTracks == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Playlist not found", color = colors.textSecondary)
        }
        return
    }

    val playlist = playlistWithTracks!!.playlist
    val allTracksInPlaylist = playlistWithTracks!!.tracks

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Reorder toggle (only enabled in CUSTOM sort order)
            if (currentSort == PlaylistSortOrder.CUSTOM && allTracksInPlaylist.size > 1 && !isMultiSelectMode) {
                IconButton(onClick = { isReorderMode = !isReorderMode }) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Reorder tracks",
                        tint = if (isReorderMode) colors.primaryAccent else colors.textSecondary
                    )
                }
            }

            // Multi-select toggle
            if (allTracksInPlaylist.isNotEmpty() && !isReorderMode) {
                IconButton(onClick = {
                    isMultiSelectMode = !isMultiSelectMode
                    if (!isMultiSelectMode) selectedTrackIds.clear()
                }) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = "Select multiple",
                        tint = if (isMultiSelectMode) colors.primaryAccent else colors.textSecondary
                    )
                }
            }

            // Pin / Unpin Shortcut
            IconButton(onClick = { viewModel.setPlaylistPinned(playlist.id, !playlist.isPinned) }) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = if (playlist.isPinned) "Unpin" else "Pin",
                    tint = if (playlist.isPinned) colors.primaryAccent else colors.controlInactive
                )
            }

            // Playlist Options Overflow
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Details", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            editNameText = playlist.name
                            editDescText = playlist.description ?: ""
                            editCoverText = playlist.coverArtworkUrl ?: ""
                            showEditDetailsDialog = true
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = colors.textSecondary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate Playlist", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            viewModel.duplicatePlaylist(playlist.id) {
                                Toast.makeText(context, "Playlist duplicated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = colors.textSecondary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Playlist", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            showExportDialog = true
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = colors.textSecondary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Download Playlist", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            if (allTracksInPlaylist.isNotEmpty()) {
                                viewModel.downloadPlaylist(playlist.id, allTracksInPlaylist)
                                Toast.makeText(context, "Downloading ${allTracksInPlaylist.size} tracks...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = colors.primaryAccent)
                        }
                    )
                    if (allTracksInPlaylist.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Clear All Songs", color = colors.error) },
                            onClick = {
                                menuExpanded = false
                                showClearSongsDialog = true
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = null, tint = colors.error)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = colors.error) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = colors.error)
                        }
                    )
                }
            }
        }

        // Multi-select Top Header Bar
        AnimatedVisibility(visible = isMultiSelectMode, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val allSelected = displayedTracks.isNotEmpty() && selectedTrackIds.size == displayedTracks.size
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedTrackIds.clear()
                                selectedTrackIds.addAll(displayedTracks.map { it.id })
                            } else {
                                selectedTrackIds.clear()
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.primaryAccent,
                            uncheckedColor = colors.controlInactive,
                            checkmarkColor = colors.background
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedTrackIds.size} of ${displayedTracks.size} selected",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary
                    )
                }

                TextButton(onClick = {
                    isMultiSelectMode = false
                    selectedTrackIds.clear()
                }) {
                    Text("Done", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hero Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaylistCollageCover(
                        coverArtworkUrl = playlist.coverArtworkUrl,
                        topArtworkUrls = playlist.topArtworkUrls,
                        modifier = Modifier.size(160.dp),
                        cornerRadius = 16.dp,
                        iconSize = 64.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )

                    if (!playlist.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    val totalDurationSecs = allTracksInPlaylist.sumOf { it.durationSeconds }
                    val totalMins = totalDurationSecs / 60
                    Text(
                        text = "${allTracksInPlaylist.size} tracks • $totalMins min",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play & Shuffle Buttons
                    if (allTracksInPlaylist.isNotEmpty() && !isMultiSelectMode && !isReorderMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.playTracks(displayedTracks, startIndex = 0, shuffle = false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (colors.isDark) colors.background else colors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Play All",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (colors.isDark) colors.background else colors.textPrimary
                                )
                            }

                            Button(
                                onClick = { viewModel.playTracks(displayedTracks, startIndex = 0, shuffle = true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                                border = ButtonDefaults.outlinedButtonBorder,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Shuffle",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // In-Playlist Search & Sort Controls
            if (allTracksInPlaylist.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setPlaylistSearchQuery(it) },
                            placeholder = { Text("Filter songs...", color = colors.textTertiary, style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setPlaylistSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
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
                                .weight(1f)
                                .height(50.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Sort chip
                        Box {
                            FilterChip(
                                selected = currentSort != PlaylistSortOrder.CUSTOM,
                                onClick = { sortMenuExpanded = true },
                                label = {
                                    val sortLabel = when (currentSort) {
                                        PlaylistSortOrder.CUSTOM -> "Custom"
                                        PlaylistSortOrder.RECENTLY_ADDED -> "Recent"
                                        PlaylistSortOrder.OLDEST_ADDED -> "Oldest"
                                        PlaylistSortOrder.TITLE_AZ -> "A-Z"
                                        PlaylistSortOrder.TITLE_ZA -> "Z-A"
                                        PlaylistSortOrder.ARTIST_AZ -> "Artist"
                                        PlaylistSortOrder.ARTIST_ZA -> "Artist Z-A"
                                        PlaylistSortOrder.DURATION -> "Duration"
                                    }
                                    Text(sortLabel, style = MaterialTheme.typography.labelSmall)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Sort",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = colors.primaryAccent,
                                    selectedLeadingIconColor = colors.primaryAccent
                                ),
                                modifier = Modifier.height(48.dp)
                            )

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                modifier = Modifier.background(colors.surfaceElevated)
                            ) {
                                val sortOptions = listOf(
                                    PlaylistSortOrder.CUSTOM to "Custom Order",
                                    PlaylistSortOrder.RECENTLY_ADDED to "Recently Added",
                                    PlaylistSortOrder.OLDEST_ADDED to "Oldest Added",
                                    PlaylistSortOrder.TITLE_AZ to "Title (A to Z)",
                                    PlaylistSortOrder.TITLE_ZA to "Title (Z to A)",
                                    PlaylistSortOrder.ARTIST_AZ to "Artist (A to Z)",
                                    PlaylistSortOrder.ARTIST_ZA to "Artist (Z to A)",
                                    PlaylistSortOrder.DURATION to "Duration"
                                )
                                sortOptions.forEach { (order, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                color = if (currentSort == order) colors.primaryAccent else colors.textPrimary,
                                                fontWeight = if (currentSort == order) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setPlaylistSortOrder(order)
                                            if (order != PlaylistSortOrder.CUSTOM) {
                                                isReorderMode = false
                                            }
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Empty State
            if (allTracksInPlaylist.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Playlist is empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add tracks from Search, Liked Songs, or Home",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textTertiary
                            )
                        }
                    }
                }
            } else if (displayedTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tracks match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary
                        )
                    }
                }
            } else {
                itemsIndexed(displayedTracks, key = { index, track -> "${track.id}_$index" }) { index, track ->
                    val isSelected = selectedTrackIds.contains(track.id)

                    PlaylistTrackRow(
                        index = index + 1,
                        track = track,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = isSelected,
                        onToggleSelect = {
                            if (isSelected) selectedTrackIds.remove(track.id) else selectedTrackIds.add(track.id)
                        },
                        isReorderMode = isReorderMode,
                        canMoveUp = index > 0,
                        canMoveDown = index < displayedTracks.size - 1,
                        onMoveUp = {
                            if (index > 0) {
                                viewModel.reorderPlaylistTracks(playlist.id, index, index - 1)
                            }
                        },
                        onMoveDown = {
                            if (index < displayedTracks.size - 1) {
                                viewModel.reorderPlaylistTracks(playlist.id, index, index + 1)
                            }
                        },
                        onPlayClick = {
                            if (isMultiSelectMode) {
                                if (isSelected) selectedTrackIds.remove(track.id) else selectedTrackIds.add(track.id)
                            } else {
                                viewModel.playTracks(displayedTracks, startIndex = index, shuffle = false)
                            }
                        },
                        onRemoveClick = { viewModel.removeTrackFromPlaylist(playlist.id, track.id) },
                        onPlayNext = { viewModel.playNext(track) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onAddToAnotherPlaylist = { tracksForAddSheet = listOf(track) },
                        onDownloadTrack = {
                            viewModel.downloadTrack(track)
                            Toast.makeText(context, "Downloading ${track.title}...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Recommended / Suggested Songs (SYNAPSE integration)
            if (!isMultiSelectMode && !isReorderMode && searchQuery.isBlank() && suggestedTracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Recommended Songs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Based on songs in this playlist",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textTertiary
                            )
                        }
                        IconButton(onClick = { viewModel.refreshSuggestedTracks() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Recommendations",
                                tint = colors.primaryAccent
                            )
                        }
                    }
                }

                items(suggestedTracks, key = { "suggested_${it.id}" }) { track ->
                    SuggestedTrackRow(
                        track = track,
                        onAddClick = {
                            viewModel.addTrackToPlaylist(playlist.id, track) {
                                Toast.makeText(context, "Added \"${track.title}\" to playlist", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPlayClick = {
                            viewModel.playTrack(track)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(110.dp)) // Mini-player clearance
            }
        }

        // Multi-select Bottom Actions Bar
        AnimatedVisibility(visible = isMultiSelectMode && selectedTrackIds.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Remove Selected Button
                Button(
                    onClick = {
                        val toRemove = selectedTrackIds.toList()
                        viewModel.removeTracksFromPlaylist(playlist.id, toRemove)
                        selectedTrackIds.clear()
                        Toast.makeText(context, "Removed ${toRemove.size} tracks", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove (${selectedTrackIds.size})", style = MaterialTheme.typography.labelSmall)
                }

                // Copy to Playlist Button
                Button(
                    onClick = {
                        val tracks = allTracksInPlaylist.filter { it.id in selectedTrackIds }
                        tracksForAddSheet = tracks
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to...", style = MaterialTheme.typography.labelSmall)
                }

                // Move to Playlist Button
                Button(
                    onClick = { showMoveDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHighlight),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.DriveFileMove, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Move to...", style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
                }
            }
        }
    }

    // Add to another playlist bottom sheet
    if (tracksForAddSheet != null) {
        AddToPlaylistBottomSheet(
            tracks = tracksForAddSheet!!,
            onDismissRequest = {
                tracksForAddSheet = null
                if (isMultiSelectMode) {
                    isMultiSelectMode = false
                    selectedTrackIds.clear()
                }
            }
        )
    }

    // Move to Playlist Dialog
    if (showMoveDialog) {
        val targetPlaylists = allPlaylists.filter { it.id != playlist.id }
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Move to Playlist", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (targetPlaylists.isEmpty()) {
                    Text("No other playlists available. Create one first.", color = colors.textSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        items(targetPlaylists, key = { it.id }) { target ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val toMove = selectedTrackIds.toList()
                                        viewModel.moveTracksBetweenPlaylists(playlist.id, target.id, toMove)
                                        showMoveDialog = false
                                        isMultiSelectMode = false
                                        selectedTrackIds.clear()
                                        Toast.makeText(context, "Moved ${toMove.size} tracks to ${target.name}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = null, tint = colors.primaryAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(target.name, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Edit Details Dialog
    if (showEditDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showEditDetailsDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Edit Playlist Details", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text("Playlist Name *") },
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
                        value = editDescText,
                        onValueChange = { editDescText = it },
                        label = { Text("Description") },
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
                        value = editCoverText,
                        onValueChange = { editCoverText = it },
                        label = { Text("Cover Image URL (Optional)") },
                        placeholder = { Text("Leave blank for auto-collage", color = colors.textTertiary) },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameText.isNotBlank()) {
                            viewModel.updatePlaylistDetails(
                                id = playlist.id,
                                name = editNameText.trim(),
                                description = editDescText.trim().takeIf { it.isNotBlank() },
                                coverArtworkUrl = editCoverText.trim().takeIf { it.isNotBlank() }
                            )
                            showEditDetailsDialog = false
                            Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                    enabled = editNameText.isNotBlank()
                ) {
                    Text("Save", color = if (colors.isDark) colors.background else colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDetailsDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Export Playlist", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose an export format:", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showExportDialog = false
                                val json = PlaylistExporter.exportToStereoJson(playlist, allTracksInPlaylist)
                                PlaylistExporter.shareExport(context, playlist.name, json, "json", "application/json")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STEREO Native JSON (Full Backup)", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showExportDialog = false
                                val csv = PlaylistExporter.exportToCsv(playlist, allTracksInPlaylist)
                                PlaylistExporter.shareExport(context, playlist.name, csv, "csv", "text/csv")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CSV (Spreadsheet)", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showExportDialog = false
                                val m3u8 = PlaylistExporter.exportToM3U8(playlist, allTracksInPlaylist)
                                PlaylistExporter.shareExport(context, playlist.name, m3u8, "m3u8", "audio/x-mpegurl")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("M3U8 (Standard Media Playlist)", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Clear All Songs Dialog
    if (showClearSongsDialog) {
        AlertDialog(
            onDismissRequest = { showClearSongsDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Clear All Songs?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove all ${allTracksInPlaylist.size} songs from \"${playlist.name}\"?",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPlaylist(playlist.id)
                        showClearSongsDialog = false
                        Toast.makeText(context, "Playlist cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Clear All", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSongsDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Delete Playlist Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Delete Playlist?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${playlist.name}\"? Tracks in your library and listening history will not be deleted.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Delete", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun PlaylistTrackRow(
    index: Int,
    track: Track,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    isReorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPlayClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToAnotherPlaylist: () -> Unit,
    onDownloadTrack: () -> Unit = {}
) {
    val colors = StereoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primaryAccent.copy(alpha = 0.12f) else colors.surfaceElevated)
            .border(
                1.dp,
                if (isSelected) colors.primaryAccent else colors.surfaceHighlight,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primaryAccent,
                    uncheckedColor = colors.controlInactive,
                    checkmarkColor = colors.background
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (isReorderMode) {
            Column(verticalArrangement = Arrangement.Center) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move up",
                        tint = if (canMoveUp) colors.primaryAccent else colors.controlInactive.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move down",
                        tint = if (canMoveDown) colors.primaryAccent else colors.controlInactive.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            // Track Index Number
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textTertiary,
                modifier = Modifier.width(22.dp)
            )
        }

        // Artwork
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = track.durationFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        // Overflow Menu (hidden during multi-select or reorder)
        if (!isMultiSelectMode && !isReorderMode) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Next", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = colors.textSecondary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = colors.textSecondary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to another Playlist", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            onAddToAnotherPlaylist()
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, tint = colors.primaryAccent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Download", color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            onDownloadTrack()
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = colors.primaryAccent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Playlist", color = colors.error) },
                        onClick = {
                            menuExpanded = false
                            onRemoveClick()
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = null, tint = colors.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedTrackRow(
    track: Track,
    onAddClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onPlayClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = track.durationFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add to playlist",
                tint = colors.primaryAccent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
