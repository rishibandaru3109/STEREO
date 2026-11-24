package com.nova.music.feature.downloads

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.DownloadItem
import com.nova.music.domain.model.DownloadStatus
import com.nova.music.domain.model.Track
import com.nova.music.feature.library.AddToPlaylistBottomSheet
import org.koin.androidx.compose.koinViewModel

@Composable
fun DownloadsScreen(
    onBackClick: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors
    val context = LocalContext.current

    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val failedDownloads by viewModel.failedDownloads.collectAsStateWithLifecycle()
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Downloaded, 1: Active, 2: Failed
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var trackForAddToPlaylist by remember { mutableStateOf<Track?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "${storageUsage.formattedString} used • ${storageUsage.trackCount} songs offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

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
                    if (failedDownloads.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Clear Failed Downloads", color = colors.textPrimary) },
                            onClick = {
                                menuExpanded = false
                                viewModel.clearFailedDownloads()
                                Toast.makeText(context, "Cleared failed downloads", Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = colors.textSecondary)
                            }
                        )
                    }
                    if (completedDownloads.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Delete All Downloads", color = colors.error) },
                            onClick = {
                                menuExpanded = false
                                showDeleteAllDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error)
                            }
                        )
                    }
                }
            }
        }

        // Tab Navigation (Downloaded, Active Queue, Failed)
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colors.background,
            contentColor = colors.primaryAccent,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = colors.primaryAccent
                    )
                }
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        text = "Downloaded (${completedDownloads.size})",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        text = "Queue (${activeDownloads.size})",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = {
                    Text(
                        text = "Failed (${failedDownloads.size})",
                        maxLines = 1,
                        softWrap = false,
                        color = if (failedDownloads.isNotEmpty()) colors.error else colors.textSecondary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTabIndex) {
            0 -> {
                // Completed Downloads View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play All & Shuffle Buttons
                    if (completedDownloads.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.playAll(shuffle = false) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (colors.isDark) colors.background else colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Play All",
                                        color = if (colors.isDark) colors.background else colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { viewModel.playAll(shuffle = true) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Shuffle", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Search and Sort Row
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Search downloads...", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(Icons.Default.Clear, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primaryAccent,
                                        unfocusedBorderColor = colors.surfaceHighlight,
                                        focusedTextColor = colors.textPrimary,
                                        unfocusedTextColor = colors.textPrimary
                                    ),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box {
                                    FilterChip(
                                        selected = sortOrder != DownloadSortOrder.RECENTLY_DOWNLOADED,
                                        onClick = { sortMenuExpanded = true },
                                        label = {
                                            val label = when (sortOrder) {
                                                DownloadSortOrder.RECENTLY_DOWNLOADED -> "Recent"
                                                DownloadSortOrder.TITLE_AZ -> "A-Z"
                                                DownloadSortOrder.ARTIST_AZ -> "Artist"
                                                DownloadSortOrder.FILE_SIZE -> "Size"
                                            }
                                            Text(label, style = MaterialTheme.typography.labelSmall)
                                        },
                                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = colors.primaryAccent
                                        ),
                                        modifier = Modifier.height(48.dp)
                                    )

                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false },
                                        modifier = Modifier.background(colors.surfaceElevated)
                                    ) {
                                        DropdownMenuItem(text = { Text("Recently Downloaded", color = colors.textPrimary) }, onClick = { viewModel.setSortOrder(DownloadSortOrder.RECENTLY_DOWNLOADED); sortMenuExpanded = false })
                                        DropdownMenuItem(text = { Text("Title (A-Z)", color = colors.textPrimary) }, onClick = { viewModel.setSortOrder(DownloadSortOrder.TITLE_AZ); sortMenuExpanded = false })
                                        DropdownMenuItem(text = { Text("Artist (A-Z)", color = colors.textPrimary) }, onClick = { viewModel.setSortOrder(DownloadSortOrder.ARTIST_AZ); sortMenuExpanded = false })
                                        DropdownMenuItem(text = { Text("File Size", color = colors.textPrimary) }, onClick = { viewModel.setSortOrder(DownloadSortOrder.FILE_SIZE); sortMenuExpanded = false })
                                    }
                                }
                            }
                        }
                    }

                    if (completedDownloads.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DownloadDone, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(56.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No downloaded music", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Download songs to listen offline without internet", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                                }
                            }
                        }
                    } else {
                        items(completedDownloads, key = { it.track.id }) { item ->
                            DownloadedTrackRow(
                                item = item,
                                onPlayClick = { viewModel.playTrack(item.track) },
                                onDeleteClick = { viewModel.deleteDownload(item.track.id) },
                                onAddToPlaylist = { trackForAddToPlaylist = item.track }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp)) // Mini-player clearance
                    }
                }
            }
            1 -> {
                // Active Queue View
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeDownloads.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Downloading, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(56.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Download queue is empty", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                                }
                            }
                        }
                    } else {
                        items(activeDownloads, key = { it.track.id }) { item ->
                            ActiveDownloadRow(
                                item = item,
                                onCancelClick = { viewModel.cancelDownload(item.track.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
            2 -> {
                // Failed Downloads View
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(failedDownloads, key = { it.track.id }) { item ->
                        FailedDownloadRow(
                            item = item,
                            onRetryClick = { viewModel.retryDownload(item.track.id) },
                            onDeleteClick = { viewModel.deleteDownload(item.track.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
    }

    // Add to playlist sheet
    if (trackForAddToPlaylist != null) {
        AddToPlaylistBottomSheet(
            track = trackForAddToPlaylist,
            onDismissRequest = { trackForAddToPlaylist = null }
        )
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = colors.surfaceElevated,
            title = { Text("Delete All Downloads?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all ${completedDownloads.size} offline downloaded songs from storage. Your playlists and listening history will not be affected.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllDownloads()
                        showDeleteAllDialog = false
                        Toast.makeText(context, "All downloads deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text("Delete All", color = colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
fun DownloadedTrackRow(
    item: DownloadItem,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onPlayClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (item.track.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = item.track.thumbnailUrl,
                    contentDescription = item.track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = null,
                    tint = colors.primaryAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.track.artist} • ${item.formattedDownloadedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(colors.surfaceElevated)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Playlist", color = colors.textPrimary) },
                    onClick = { menuExpanded = false; onAddToPlaylist() },
                    leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = colors.primaryAccent) }
                )
                DropdownMenuItem(
                    text = { Text("Delete from Storage", color = colors.error) },
                    onClick = { menuExpanded = false; onDeleteClick() },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error) }
                )
            }
        }
    }
}

@Composable
fun ActiveDownloadRow(
    item: DownloadItem,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = colors.primaryAccent,
                trackColor = colors.surfaceHighlight
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (item.status == DownloadStatus.QUEUED) "Queued..." else "Downloading (${item.progressPercent}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primaryAccent
                )
                Text(
                    text = "${item.formattedDownloadedSize} / ${item.formattedTotalSize}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        IconButton(onClick = onCancelClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FailedDownloadRow(
    item: DownloadItem,
    onRetryClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.error.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = colors.error, modifier = Modifier.size(24.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.errorMessage ?: "Download failed",
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onRetryClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = colors.primaryAccent, modifier = Modifier.size(18.dp))
        }

        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
