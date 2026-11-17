package com.nova.music.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenLibrary: () -> Unit = {},
    onOpenThemeGallery: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val colors = StereoTheme.colors

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Compact Brand Header (Phase 2)
        com.nova.music.feature.navigation.StereoTopHeader(
            title = "Search",
            subtitle = "STEREO CATALOG",
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

        Spacer(modifier = Modifier.height(16.dp))

        // Tactile Search Input Bar
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search songs, artists, albums...",
                    color = colors.textTertiary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.primaryAccent
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = state.query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = colors.textSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.primaryAccent,
                unfocusedBorderColor = colors.surfaceHighlight,
                cursorColor = colors.primaryAccent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.onSearchSubmitted()
                    keyboardController?.hide()
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Results Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            when (val searchState = state.searchUiState) {
                is SearchUiState.Idle -> {
                    SearchIdlePlaceholder(
                        onSuggestionClick = { suggestion ->
                            viewModel.onQueryChange(suggestion)
                            viewModel.onSearchSubmitted()
                            keyboardController?.hide()
                        }
                    )
                }
                is SearchUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.primaryAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                is SearchUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs found for \"${state.query}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary
                        )
                    }
                }
                is SearchUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = searchState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.error
                            )
                        }
                    }
                }
                is SearchUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchState.tracks, key = { it.id }) { track ->
                            val isPlaying = track.id == state.currentlyPlayingTrackId
                            TrackItem(
                                track = track,
                                isCurrentlyPlaying = isPlaying,
                                onClick = {
                                    viewModel.onTrackSelected(track)
                                    keyboardController?.hide()
                                }
                            )
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = colors.surfaceElevated,
                contentColor = colors.primaryAccent
            )
        }
    }
}
}

@Composable
private fun TrackItem(
    track: Track,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentlyPlaying) colors.surfaceElevated else colors.surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceHighlight),
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
                    tint = colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrentlyPlaying) colors.primaryAccent else colors.textPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist + (track.album?.let { " • $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Playing status icon or duration
        if (isCurrentlyPlaying) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Playing",
                tint = colors.primaryAccent,
                modifier = Modifier.size(20.dp)
            )
        } else if (track.durationSeconds > 0) {
            Text(
                text = track.durationFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun SearchIdlePlaceholder(
    onSuggestionClick: (String) -> Unit
) {
    val colors = StereoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = colors.surfaceElevated,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Discover Any Song",
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search YouTube Music's global catalog with lossless audio extraction",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textTertiary,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Popular Searches",
            style = MaterialTheme.typography.labelSmall,
            color = colors.primaryAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf("Daft Punk - Get Lucky", "The Weeknd - Blinding Lights", "Hans Zimmer - Time", "Coldplay - Viva La Vida")
        suggestions.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .clickable { onSuggestionClick(suggestion) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}
