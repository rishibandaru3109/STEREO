package com.nova.music.feature.home

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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenThemeGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val likedTrackIds by viewModel.likedTrackIds.collectAsStateWithLifecycle()
    val colors = StereoTheme.colors
    val greeting = rememberGreeting()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top App Bar / Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STEREO",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = colors.primaryAccent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DISCOVER",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = colors.secondaryAccent
                            )
                        )
                    }
                }
            }

            IconButton(
                onClick = { viewModel.loadHomeFeed(forceRefresh = true) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .size(40.dp)
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.primaryAccent
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Feed",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenThemeGallery,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Theme Gallery",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Feed Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = colors.primaryAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Preparing your personalized sound...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                state.errorMessage != null && state.sections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = state.errorMessage ?: "Could not load music feed",
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
                        items(state.sections, key = { it.type.name }) { section ->
                            when (section.type) {
                                SectionType.QUICK_PICKS -> {
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
        }
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
    val colors = StereoTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = section.title, subtitle = section.subtitle)

        Spacer(modifier = Modifier.height(10.dp))

        // 2-column or horizontal grid of Quick Pick items
        val chunkedTracks = section.tracks.chunked(2)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chunkedTracks) { pair ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { track ->
                        QuickPickTrackCard(
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
private fun QuickPickTrackCard(
    track: Track,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onNotInterested: () -> Unit
) {
    val colors = StereoTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .width(260.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) colors.primaryAccent.copy(alpha = 0.12f) else colors.surfaceElevated)
            .border(
                0.5.dp,
                if (isPlaying) colors.primaryAccent else colors.surfaceHighlight,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
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
                        .background(Color.Black.copy(alpha = 0.45f)),
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

    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(0.5.dp, colors.surfaceHighlight, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
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
                                colors = listOf(colors.primaryAccent.copy(alpha = 0.3f), colors.surfaceHighlight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Play Floating Button over bottom-right corner of cover
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.primaryAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Mix",
                    tint = colors.background,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = mix.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = mix.subtitle,
            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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

    Column(
        modifier = Modifier
            .width(136.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) colors.primaryAccent.copy(alpha = 0.1f) else colors.surfaceElevated)
            .border(
                0.5.dp,
                if (isPlaying) colors.primaryAccent else colors.surfaceHighlight,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
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
                        modifier = Modifier.size(26.dp)
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

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String?
) {
    val colors = StereoTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 4..11 -> "GOOD MORNING"
        in 12..16 -> "GOOD AFTERNOON"
        in 17..21 -> "GOOD EVENING"
        else -> "LATE NIGHT LISTENING"
    }
}
