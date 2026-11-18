package com.nova.music.feature.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.nova.music.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.component.StereoCard
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import com.nova.music.feature.navigation.StereoTopHeader
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenThemeGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val likedTrackIds by viewModel.likedTrackIds.collectAsStateWithLifecycle()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val colors = StereoTheme.colors
    val greeting = rememberGreeting()
    val context = LocalContext.current

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadHomeFeed(forceRefresh = true)
        }
    }
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(state.userMessage) {
        val msg = state.userMessage
        if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    if (!isOnboardingCompleted) {
        RecommendationOnboardingDialog(
            onDismiss = {
                viewModel.completeOnboarding(emptySet(), setOf("Pop", "Rock", "Chill"), "Familiar")
            },
            onSavePreferences = { langs, genres, mode ->
                viewModel.completeOnboarding(langs, genres, mode)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Compact Brand Header (Phase 2)
        StereoTopHeader(
            title = greeting,
            subtitle = "STEREO DISCOVER",
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Feed Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            when {
                state.isLoading && state.sections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colors.primaryAccent)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Preparing your personal mix...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                state.sections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = state.errorMessage ?: "No matching recommendations yet. Check your music preferences or try again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadHomeFeed(forceRefresh = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent)
                            ) {
                                Text("Retry", color = colors.background)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        items(state.sections, key = { "${it.type.name}_${it.title}" }) { section ->
                            when (section.type) {
                                SectionType.QUICK_PICKS, SectionType.EXPLORE_MORE -> {
                                    QuickPicksSection(
                                        section = section,
                                        currentlyPlayingId = state.currentlyPlayingTrackId,
                                        likedTrackIds = likedTrackIds,
                                        onTrackClick = { track -> viewModel.playTrack(track, section.tracks) },
                                        onToggleLike = { viewModel.toggleLike(it) },
                                        onNotInterested = { viewModel.notInterested(it) }
                                    )
                                }

                                SectionType.MADE_FOR_YOU -> {
                                    MadeForYouSection(
                                        section = section,
                                        onMixClick = { mix -> viewModel.playMix(mix) }
                                    )
                                }

                                else -> {
                                    StandardTrackRowSection(
                                        section = section,
                                        currentlyPlayingId = state.currentlyPlayingTrackId,
                                        likedTrackIds = likedTrackIds,
                                        onTrackClick = { track -> viewModel.playTrack(track, section.tracks) },
                                        onToggleLike = { viewModel.toggleLike(it) },
                                        onNotInterested = { viewModel.notInterested(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Surprise Me Floating Action Button (Theme-aware, positioned above mini-player)
            SurpriseMeFab(
                isLoading = state.isSurpriseMeLoading,
                onClick = { viewModel.triggerSurpriseMe() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (state.currentlyPlayingTrackId != null) 156.dp else 84.dp
                    )
            )

            // Native Theme-aware Material 3 Pull to Refresh Indicator
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = colors.surfaceElevated,
                contentColor = colors.primaryAccent
            )
        }
    }
}

@Composable
private fun SurpriseMeFab(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors
    ExtendedFloatingActionButton(
        onClick = { if (!isLoading) onClick() },
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.primaryAccent,
        contentColor = colors.background,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.background,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_dice_surprise),
                contentDescription = "Surprise me",
                modifier = Modifier.size(20.dp),
                tint = colors.background
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Surprise me",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.background
        )
    }
}

@Composable
private fun QuickPicksSection(
    section: RecommendationSection,
    currentlyPlayingId: String?,
    likedTrackIds: Set<String>,
    onTrackClick: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onNotInterested: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = section.title, subtitle = section.subtitle)

        Spacer(modifier = Modifier.height(12.dp))

        val pairs = section.tracks.chunked(2)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pairs) { columnPair ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.width(280.dp)
                ) {
                    columnPair.forEach { track ->
                        QuickPickItem(
                            track = track,
                            isPlaying = track.id == currentlyPlayingId,
                            isLiked = likedTrackIds.contains(track.id),
                            onClick = { onTrackClick(track) },
                            onToggleLike = { onToggleLike(track) },
                            onNotInterested = { onNotInterested(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPickItem(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onNotInterested: () -> Unit
) {
    val colors = StereoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    StereoCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Playing",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPlaying) colors.primaryAccent else colors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isLiked) "Unlike" else "Like", color = colors.textPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isLiked) colors.controlActive else colors.textSecondary
                            )
                        },
                        onClick = {
                            onToggleLike()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Not interested", color = colors.textPrimary) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = colors.textSecondary)
                        },
                        onClick = {
                            onNotInterested()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MadeForYouSection(
    section: RecommendationSection,
    onMixClick: (RecommendationMix) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = section.title, subtitle = section.subtitle)

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(section.mixes, key = { it.id }) { mix ->
                MixCard(mix = mix, onClick = { onMixClick(mix) })
            }
        }
    }
}

@Composable
private fun MixCard(
    mix: RecommendationMix,
    onClick: () -> Unit
) {
    val colors = StereoTheme.colors

    StereoCard(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Mosaic cover art or solid card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                val artUrls = mix.coverArtUrls.filter { it.isNotBlank() }
                if (artUrls.size >= 4) {
                    // 2x2 Artwork Grid
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = artUrls[0],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            AsyncImage(
                                model = artUrls[1],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = artUrls[2],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            AsyncImage(
                                model = artUrls[3],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else if (artUrls.isNotEmpty()) {
                    AsyncImage(
                        model = artUrls.first(),
                        contentDescription = mix.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.primaryAccent.copy(alpha = 0.6f), colors.secondaryAccent.copy(alpha = 0.6f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Play Overlay Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Mix",
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = mix.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = mix.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StandardTrackRowSection(
    section: RecommendationSection,
    currentlyPlayingId: String?,
    likedTrackIds: Set<String>,
    onTrackClick: (Track) -> Unit,
    onToggleLike: (Track) -> Unit,
    onNotInterested: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = section.title, subtitle = section.subtitle)

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.tracks, key = { it.id }) { track ->
                TrackCard(
                    track = track,
                    isPlaying = track.id == currentlyPlayingId,
                    isLiked = likedTrackIds.contains(track.id),
                    onClick = { onTrackClick(track) },
                    onToggleLike = { onToggleLike(track) },
                    onNotInterested = { onNotInterested(track) }
                )
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onNotInterested: () -> Unit
) {
    val colors = StereoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    StereoCard(
        modifier = Modifier.width(136.dp),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
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
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Playing",
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPlaying) colors.primaryAccent else colors.textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isLiked) "Unlike" else "Like", color = colors.textPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isLiked) colors.controlActive else colors.textSecondary
                                )
                            },
                            onClick = {
                                onToggleLike()
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Not interested", color = colors.textPrimary) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = colors.textSecondary)
                            },
                            onClick = {
                                onNotInterested()
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String?) {
    val colors = StereoTheme.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
        }
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
