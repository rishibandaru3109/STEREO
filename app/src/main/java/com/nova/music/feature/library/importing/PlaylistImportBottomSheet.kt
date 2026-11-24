package com.nova.music.feature.library.importing

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.theme.StereoTheme
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.feature.library.LibraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.koin.compose.koinInject

private enum class ImportStep {
    INPUT,
    FETCHING,
    MATCHING,
    PREVIEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistImportBottomSheet(
    viewModel: LibraryViewModel,
    onPlaylistImported: (String) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val colors = StereoTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val okHttpClient: OkHttpClient = koinInject()
    val musicRepository: MusicRepository = koinInject()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentStep by remember { mutableStateOf(ImportStep.INPUT) }
    var inputUrlOrText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Parsed Data
    var parsedPlaylist by remember { mutableStateOf<ParsedPlaylist?>(null) }
    val matchedTracks = remember { mutableStateListOf<MatchedImportTrack>() }
    var matchProgress by remember { mutableFloatStateOf(0f) }
    var matchedCount by remember { mutableIntStateOf(0) }

    // Preview configurations
    var playlistTitle by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }
    var skipDuplicates by remember { mutableStateOf(true) }
    var importMatchedOnly by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var matchingJob by remember { mutableStateOf<Job?>(null) }

    // File picker launcher for M3U, CSV, JSON
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    currentStep = ImportStep.FETCHING
                    statusMessage = "Reading playlist file..."
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Could not read file.")

                    val result = PlaylistImportParser.parseInput(content, okHttpClient)
                    result.onSuccess { parsed ->
                        parsedPlaylist = parsed
                        playlistTitle = parsed.title
                        playlistDescription = parsed.description ?: ""
                        startMatching(parsed, musicRepository, matchedTracks, { p, m ->
                            matchProgress = p
                            matchedCount = m
                        }) {
                            currentStep = ImportStep.PREVIEW
                        }
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Failed to parse playlist file."
                        currentStep = ImportStep.INPUT
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to read file."
                    currentStep = ImportStep.INPUT
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            matchingJob?.cancel()
            onDismissRequest()
        },
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
                        text = "Import Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Spotify • YouTube Music • M3U8 • CSV • JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                IconButton(onClick = {
                    matchingJob?.cancel()
                    onDismissRequest()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            // Error display
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.error.copy(alpha = 0.15f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = colors.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorMessage!!, style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            when (currentStep) {
                ImportStep.INPUT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Paste a public playlist URL or choose a file:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )

                        OutlinedTextField(
                            value = inputUrlOrText,
                            onValueChange = {
                                inputUrlOrText = it
                                errorMessage = null
                            },
                            placeholder = { Text("https://open.spotify.com/playlist/...", color = colors.textTertiary) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = colors.primaryAccent)
                            },
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceHighlight),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose File", color = colors.textPrimary, style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    if (inputUrlOrText.isNotBlank()) {
                                        errorMessage = null
                                        currentStep = ImportStep.FETCHING
                                        statusMessage = "Retrieving playlist metadata..."
                                        coroutineScope.launch {
                                            val result = PlaylistImportParser.parseInput(inputUrlOrText, okHttpClient)
                                            result.onSuccess { parsed ->
                                                parsedPlaylist = parsed
                                                playlistTitle = parsed.title
                                                playlistDescription = parsed.description ?: ""
                                                matchingJob = startMatching(parsed, musicRepository, matchedTracks, { p, m ->
                                                    matchProgress = p
                                                    matchedCount = m
                                                }) {
                                                    currentStep = ImportStep.PREVIEW
                                                }
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Failed to retrieve playlist."
                                                currentStep = ImportStep.INPUT
                                            }
                                        }
                                    }
                                },
                                enabled = inputUrlOrText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch URL", color = if (colors.isDark) colors.background else colors.textPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                ImportStep.FETCHING, ImportStep.MATCHING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = colors.primaryAccent, strokeWidth = 3.dp)

                        Text(
                            text = if (currentStep == ImportStep.MATCHING) "Matching tracks: $matchedCount / ${parsedPlaylist?.tracks?.size ?: 0}" else statusMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )

                        if (currentStep == ImportStep.MATCHING) {
                            LinearProgressIndicator(
                                progress = { matchProgress },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = colors.primaryAccent,
                                trackColor = colors.surfaceHighlight
                            )
                        }

                        TextButton(onClick = {
                            matchingJob?.cancel()
                            currentStep = ImportStep.INPUT
                        }) {
                            Text("Cancel", color = colors.error)
                        }
                    }
                }

                ImportStep.PREVIEW -> {
                    val total = matchedTracks.size
                    val exactOrHigh = matchedTracks.count { it.status == MatchStatus.EXACT_MATCH || it.status == MatchStatus.HIGH_CONFIDENCE || it.status == MatchStatus.MEDIUM_CONFIDENCE }
                    val unmatched = total - exactOrHigh

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Editable Title & Description
                        OutlinedTextField(
                            value = playlistTitle,
                            onValueChange = { playlistTitle = it },
                            label = { Text("Playlist Name") },
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Match stats chip row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$exactOrHigh matched • $unmatched missing • ${parsedPlaylist?.source?.displayName ?: ""}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.primaryAccent
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = skipDuplicates,
                                    onCheckedChange = { skipDuplicates = it },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.primaryAccent)
                                )
                                Text("Skip Dups", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Track preview list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(matchedTracks, key = { index, _ -> index }) { index, item ->
                                val isMatched = item.matchedTrack != null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceHighlight.copy(alpha = 0.5f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isMatched) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = if (isMatched) colors.primaryAccent else colors.textTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.matchedTrack?.title ?: item.rawTrack.title,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.matchedTrack?.artist ?: item.rawTrack.artist,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (item.confidence > 0f) {
                                        Text(
                                            text = "${(item.confidence * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textTertiary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Confirm Import Button
                        Button(
                            onClick = {
                                if (playlistTitle.isNotBlank() && !isSaving) {
                                    isSaving = true
                                    coroutineScope.launch {
                                        val validTracks = matchedTracks
                                            .mapNotNull { it.matchedTrack }
                                            .let { if (skipDuplicates) it.distinctBy { t -> t.id } else it }

                                        val newId = viewModel.createPlaylist(
                                            name = playlistTitle.trim(),
                                            description = playlistDescription.trim().takeIf { it.isNotBlank() },
                                            coverArtworkUrl = parsedPlaylist?.coverUrl
                                        ) { createdId ->
                                            if (validTracks.isNotEmpty()) {
                                                viewModel.addTracksToPlaylist(createdId, validTracks, allowDuplicates = !skipDuplicates) { count ->
                                                    Toast.makeText(
                                                        context,
                                                        "Imported \"${playlistTitle.trim()}\" ($count tracks)",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onPlaylistImported(createdId)
                                                    coroutineScope.launch {
                                                        try { sheetState.hide() } catch (_: Throwable) {}
                                                        onDismissRequest()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "Created empty playlist \"${playlistTitle.trim()}\"", Toast.LENGTH_SHORT).show()
                                                onPlaylistImported(createdId)
                                                coroutineScope.launch {
                                                    try { sheetState.hide() } catch (_: Throwable) {}
                                                    onDismissRequest()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = playlistTitle.isNotBlank() && !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primaryAccent)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.background)
                            } else {
                                Text(
                                    text = "Import $exactOrHigh Songs to Playlist",
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

private fun startMatching(
    parsed: ParsedPlaylist,
    musicRepository: MusicRepository,
    matchedList: MutableList<MatchedImportTrack>,
    onProgress: (Float, Int) -> Unit,
    onComplete: () -> Unit
): Job {
    return CoroutineScope(Dispatchers.IO).launch {
        matchedList.clear()
        val total = parsed.tracks.size
        var current = 0

        for (raw in parsed.tracks) {
            current++
            val query = "${raw.title} ${raw.artist}"
            var candidates = emptyList<Track>()
            try {
                val searchResult = musicRepository.search(query).first()
                candidates = searchResult.getOrDefault(emptyList())
            } catch (_: Exception) {}

            val matched = TrackMatchingEngine.matchTrack(raw, candidates)
            matchedList.add(matched)
            onProgress(current.toFloat() / total.toFloat(), current)
        }
        onComplete()
    }
}
