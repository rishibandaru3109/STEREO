package com.nova.music.feature.library.importing

import com.nova.music.domain.model.Track

enum class ImportSource(val displayName: String) {
    SPOTIFY("Spotify"),
    YOUTUBE_MUSIC("YouTube Music"),
    M3U8("M3U / M3U8 File"),
    CSV("CSV Spreadsheet"),
    JSON("JSON Export"),
    STEREO_NATIVE("STEREO Backup")
}

data class RawImportTrack(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Long = 0,
    val sourceTrackId: String? = null,
    val thumbnailUrl: String? = null
)

enum class MatchStatus {
    EXACT_MATCH,
    HIGH_CONFIDENCE,
    MEDIUM_CONFIDENCE,
    UNMATCHED,
    DUPLICATE
}

data class MatchedImportTrack(
    val rawTrack: RawImportTrack,
    val matchedTrack: Track?,
    val confidence: Float,
    val status: MatchStatus
)

data class ParsedPlaylist(
    val title: String,
    val description: String? = null,
    val coverUrl: String? = null,
    val source: ImportSource,
    val tracks: List<RawImportTrack>
)

data class ImportSummary(
    val playlistId: String,
    val playlistName: String,
    val totalTracks: Int,
    val importedTracks: Int,
    val skippedDuplicates: Int,
    val unmatchedTracks: Int
)
