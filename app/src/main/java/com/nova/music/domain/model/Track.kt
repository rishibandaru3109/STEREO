package com.nova.music.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Long = 0,
    val thumbnailUrl: String = "",
    val streamUrl: String? = null,
    val lyricsLanguage: String? = null,
    val isSpokenWord: Boolean = false
) {
    val durationFormatted: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val artworkUrl: String
        get() = thumbnailUrl
}
