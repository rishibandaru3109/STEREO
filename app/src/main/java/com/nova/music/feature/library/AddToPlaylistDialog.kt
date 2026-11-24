package com.nova.music.feature.library

import androidx.compose.runtime.Composable
import com.nova.music.domain.model.Track

@Composable
fun AddToPlaylistDialog(
    track: Track,
    viewModel: LibraryViewModel,
    onDismissRequest: () -> Unit
) {
    AddToPlaylistBottomSheet(
        track = track,
        viewModel = viewModel,
        onDismissRequest = onDismissRequest
    )
}
