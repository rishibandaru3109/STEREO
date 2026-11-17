package com.nova.music.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.LikedSortOrder

@Composable
fun LikedSongsScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val likedCount by viewModel.likedCount.collectAsStateWithLifecycle()
    val currentSort by viewModel.likedSortOrder.collectAsStateWithLifecycle()
    val searchQuery by viewModel.likedSearchQuery.collectAsStateWithLifecycle()

    var trackForPlaylistDialog by remember { mutableStateOf<Track?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Liked Songs",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "$likedCount ${if (likedCount == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
            }

            IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                Icon(
                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search Liked Songs",
                    tint = if (isSearchVisible) colors.primaryAccent else colors.textPrimary
                )
            }
        }

        // Optional Live Search Field
        if (isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLikedSearchQuery(it) },
                placeholder = { Text("Filter liked songs...", color = colors.textTertiary) },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = colors.textTertiary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setLikedSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = colors.textTertiary)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primaryAccent,
                    unfocusedBorderColor = colors.surfaceHighlight,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.primaryAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // Sort Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LikedSortOrder.entries.forEach { sort ->
                val label = when (sort) {
                    LikedSortOrder.RECENTLY_ADDED -> "Recent"
                    LikedSortOrder.TITLE -> "Title"
                    LikedSortOrder.ARTIST -> "Artist"
                }
                val selected = currentSort == sort
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setLikedSortOrder(sort) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryAccent,
                        selectedLabelColor = if (colors.isDark) colors.background else colors.textPrimary,
                        containerColor = colors.surfaceElevated,
                        labelColor = colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (selected) colors.primaryAccent else colors.surfaceHighlight,
                        enabled = true,
                        selected = selected
                    )
                )
            }
        }

        // Action Buttons: Play All & Shuffle All
        if (likedSongs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.playTracks(likedSongs, startIndex = 0, shuffle = false) },
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
                    onClick = { viewModel.playTracks(likedSongs, startIndex = 0, shuffle = true) },
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

        Spacer(modifier = Modifier.height(8.dp))

        // Liked Songs List
        if (likedSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching songs" else "No liked songs yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try a different search term" else "Tap the heart icon on any song to save it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(likedSongs, key = { it.id }) { track ->
                    LikedSongRow(
                        track = track,
                        onPlayClick = { viewModel.playTrack(track) },
                        onUnlikeClick = { viewModel.toggleLike(track) },
                        onPlayNext = { viewModel.playNext(track) },
                        onAddToQueue = { viewModel.addToQueue(track) },
                        onAddToPlaylist = { trackForPlaylistDialog = track }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Mini-player clearance
                }
            }
        }
    }

    trackForPlaylistDialog?.let { track ->
        AddToPlaylistDialog(
            track = track,
            viewModel = viewModel,
            onDismissRequest = { trackForPlaylistDialog = null }
        )
    }
}

@Composable
private fun LikedSongRow(
    track: Track,
    onPlayClick: () -> Unit,
    onUnlikeClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val colors = StereoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.surfaceHighlight, RoundedCornerShape(12.dp))
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork
        Box(
            modifier = Modifier
                .size(48.dp)
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
                    modifier = Modifier.size(24.dp)
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
                text = track.artist + (track.album?.let { " • $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Unlike Heart Button
        IconButton(onClick = onUnlikeClick) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Unlike",
                tint = colors.primaryAccent,
                modifier = Modifier.size(22.dp)
            )
        }

        // Overflow Menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
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
                    text = { Text("Add to Playlist", color = colors.textPrimary) },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylist()
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, tint = colors.textSecondary)
                    }
                )
            }
        }
    }
}
