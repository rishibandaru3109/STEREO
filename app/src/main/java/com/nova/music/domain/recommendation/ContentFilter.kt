package com.nova.music.domain.recommendation

import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.ContentFilterMode

/**
 * Metadata-first music classifier.
 * Filters out podcasts, interviews, reaction videos, and spoken-word media
 * while preserving legitimate long tracks, live performances, DJ mixes, and full albums.
 */
object ContentFilter {

    private val NON_MUSIC_TITLE_REGEX = Regex(
        "(?i)\\b(interview|podcast|reactions?|reacting|review|audiobook|audio\\s+book|commentary|unboxing|tutorial|gameplay|vlogs?|q&a|q\\s+and\\s+a|trailers?|teasers?)\\b"
    )

    private val MUSIC_METADATA_WHITELIST_REGEX = Regex(
        "(?i)\\b(music\\s+video|official\\s+video|official\\s+audio|lyric\\s+video|remix|dj\\s+mix|continuous\\s+mix|live\\s+set|live\\s+at|live\\s+from|live\\s+session|live\\s+in|concert|acoustic|soundtrack|ost|full\\s+album|symphony|orchestra|performance)\\b"
    )

    private val NON_MUSIC_CHANNEL_REGEX = Regex(
        "(?i)\\b(podcasts?|audiobooks?|tutorials?|gameplays?)\\b"
    )

    fun isMusicTrack(track: Track): Boolean {
        if (track.isSpokenWord) return false
        val title = track.title.trim()
        val artist = track.artist.trim()

        // 1. Check title against music whitelist first (music video, live set, continuous mix, remix, etc.)
        val isExplicitMusic = MUSIC_METADATA_WHITELIST_REGEX.containsMatchIn(title)

        // 2. Check title for non-music keywords
        val hasNonMusicTitle = NON_MUSIC_TITLE_REGEX.containsMatchIn(title)
        if (hasNonMusicTitle && (track.album.isNullOrBlank() || !isExplicitMusic)) {
            return false
        }

        // 3. Check channel/artist descriptor
        val hasNonMusicChannel = NON_MUSIC_CHANNEL_REGEX.containsMatchIn(artist)
        if (hasNonMusicChannel && !isExplicitMusic) {
            return false
        }

        // 4. Stings and UI sound effects under 15 seconds (unless explicitly titled music)
        if (track.durationSeconds in 1..14 && !isExplicitMusic) {
            return false
        }

        // Note: Legitimate long tracks (DJ mixes, full albums, live sets, symphonies)
        // are explicitly preserved and never rejected based on high duration.
        return true
    }

    fun filterTracks(tracks: List<Track>, mode: ContentFilterMode): List<Track> {
        if (mode == ContentFilterMode.ALL_CONTENT) return tracks
        return tracks.filter { isMusicTrack(it) }
    }
}
