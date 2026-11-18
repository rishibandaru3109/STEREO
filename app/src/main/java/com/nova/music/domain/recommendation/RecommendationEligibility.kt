package com.nova.music.domain.recommendation

import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.RecommendationPreferences
import java.text.Normalizer
import java.util.Locale

fun recordingName(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFKC)
        .trim().replace(Regex("[\\p{P}\\s]+"), " ").trim()

fun distinctRecordings(tracks: List<Track>): List<Track> = tracks.distinctBy(Track::id)
    .distinctBy { recordingName(it.artist) to recordingName(it.title) }

object RecommendationEligibility {
    fun accepts(track: Track, preferences: RecommendationPreferences): Boolean =
        ContentFilter.isMusicTrack(track) && acceptsLanguage(track, preferences)

    fun acceptsLanguage(track: Track, preferences: RecommendationPreferences): Boolean {
        if (preferences.languages.isEmpty()) return true
        val languages = track.lyricsLanguage?.split('+') ?: return false
        return languages.all { language -> preferences.languages.any { it.equals(language, ignoreCase = true) } }
    }
}
