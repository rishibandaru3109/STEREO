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

    // Regex for non-English scripts
    private val DEVANAGARI_REGEX = Regex("[\\u0900-\\u097F]")
    private val GURMUKHI_REGEX = Regex("[\\u0A00-\\u0A7F]")
    private val TAMIL_REGEX = Regex("[\\u0B80-\\u0BFF]")
    private val TELUGU_REGEX = Regex("[\\u0C00-\\u0C7F]")
    private val MALAYALAM_REGEX = Regex("[\\u0D00-\\u0D7F]")
    private val KANNADA_REGEX = Regex("[\\u0C80-\\u0CFF]")
    private val BENGALI_REGEX = Regex("[\\u0980-\\u09FF]")
    private val ARABIC_REGEX = Regex("[\\u0600-\\u06FF]")
    private val CJK_REGEX = Regex("[\\u4E00-\\u9FFF\\u3040-\\u30FF]")
    private val HANGUL_REGEX = Regex("[\\uAC00-\\uD7AF]")
    private val CYRILLIC_REGEX = Regex("[\\u0400-\\u04FF]")

    // Regional / language keyword regexes (case-insensitive words)
    private val HINDI_KEYWORDS = Regex("(?i)\\b(hindi|bollywood|ghazal|bhajan)\\b")
    private val PUNJABI_KEYWORDS = Regex("(?i)\\b(punjabi|bhangra)\\b")
    private val TELUGU_KEYWORDS = Regex("(?i)\\b(telugu|tollywood)\\b")
    private val TAMIL_KEYWORDS = Regex("(?i)\\b(tamil|kollywood)\\b")
    private val MALAYALAM_KEYWORDS = Regex("(?i)\\b(malayalam|mollywood)\\b")
    private val SPANISH_KEYWORDS = Regex("(?i)\\b(spanish|espanol|latino|reggaeton)\\b")
    private val KOREAN_KEYWORDS = Regex("(?i)\\b(korean|kpop|k-pop)\\b")

    private val ALL_NON_ENGLISH_KEYWORDS = Regex(
        "(?i)\\b(hindi|bollywood|ghazal|bhajan|punjabi|bhangra|telugu|tollywood|tamil|kollywood|malayalam|mollywood|kannada|sandallwood|bhojpuri|marathi|bengali|gujarati|urdu|kpop|k-pop)\\b"
    )

    fun accepts(track: Track, preferences: RecommendationPreferences): Boolean =
        ContentFilter.isMusicTrack(track) && acceptsLanguage(track, preferences)

    fun acceptsLanguage(track: Track, preferences: RecommendationPreferences): Boolean {
        if (preferences.languages.isEmpty()) return true

        // 1. If explicit lyrics language is known, it must match preferences
        if (track.lyricsLanguage != null) {
            val languages = track.lyricsLanguage.split('+')
            return languages.all { lang -> preferences.languages.any { it.equals(lang, ignoreCase = true) } }
        }

        val text = "${track.title} ${track.artist}"

        // Check non-English scripts
        val hasDevanagari = DEVANAGARI_REGEX.containsMatchIn(text)
        val hasGurmukhi = GURMUKHI_REGEX.containsMatchIn(text)
        val hasTamil = TAMIL_REGEX.containsMatchIn(text)
        val hasTelugu = TELUGU_REGEX.containsMatchIn(text)
        val hasMalayalam = MALAYALAM_REGEX.containsMatchIn(text)
        val hasHangul = HANGUL_REGEX.containsMatchIn(text)
        val hasOtherForeignScript = KANNADA_REGEX.containsMatchIn(text) ||
            BENGALI_REGEX.containsMatchIn(text) || ARABIC_REGEX.containsMatchIn(text) ||
            CJK_REGEX.containsMatchIn(text) || CYRILLIC_REGEX.containsMatchIn(text)

        val hasHindiKw = HINDI_KEYWORDS.containsMatchIn(text)
        val hasPunjabiKw = PUNJABI_KEYWORDS.containsMatchIn(text)
        val hasTeluguKw = TELUGU_KEYWORDS.containsMatchIn(text)
        val hasTamilKw = TAMIL_KEYWORDS.containsMatchIn(text)
        val hasMalayalamKw = MALAYALAM_KEYWORDS.containsMatchIn(text)
        val hasSpanishKw = SPANISH_KEYWORDS.containsMatchIn(text)
        val hasKoreanKw = KOREAN_KEYWORDS.containsMatchIn(text)

        val prefHasHindi = preferences.languages.any { it.equals("Hindi", ignoreCase = true) }
        val prefHasPunjabi = preferences.languages.any { it.equals("Punjabi", ignoreCase = true) }
        val prefHasTelugu = preferences.languages.any { it.equals("Telugu", ignoreCase = true) }
        val prefHasTamil = preferences.languages.any { it.equals("Tamil", ignoreCase = true) }
        val prefHasMalayalam = preferences.languages.any { it.equals("Malayalam", ignoreCase = true) }
        val prefHasSpanish = preferences.languages.any { it.equals("Spanish", ignoreCase = true) }
        val prefHasKorean = preferences.languages.any { it.equals("Korean", ignoreCase = true) }
        val prefHasEnglish = preferences.languages.any { it.equals("English", ignoreCase = true) }

        // Confidently reject if it contains non-matching regional script or keywords
        if (hasDevanagari && !prefHasHindi) return false
        if (hasGurmukhi && !prefHasPunjabi) return false
        if (hasTelugu && !prefHasTelugu) return false
        if (hasTamil && !prefHasTamil) return false
        if (hasMalayalam && !prefHasMalayalam) return false
        if (hasHangul && !prefHasKorean) return false
        if (hasOtherForeignScript) return false

        if (hasHindiKw && !prefHasHindi) return false
        if (hasPunjabiKw && !prefHasPunjabi) return false
        if (hasTeluguKw && !prefHasTelugu) return false
        if (hasTamilKw && !prefHasTamil) return false
        if (hasMalayalamKw && !prefHasMalayalam) return false
        if (hasSpanishKw && !prefHasSpanish) return false
        if (hasKoreanKw && !prefHasKorean) return false

        // If English is the ONLY preference, verify no non-English markers
        if (preferences.languages.size == 1 && prefHasEnglish) {
            val hasAnyNonEnglishMarker = ALL_NON_ENGLISH_KEYWORDS.containsMatchIn(text) ||
                hasDevanagari || hasGurmukhi || hasTelugu || hasTamil || hasMalayalam || hasHangul || hasOtherForeignScript
            if (hasAnyNonEnglishMarker) return false
            return true
        }

        // If user selected specific regional language(s) without English:
        if (!prefHasEnglish) {
            val matchesSelected = (prefHasHindi && (hasDevanagari || hasHindiKw)) ||
                (prefHasPunjabi && (hasGurmukhi || hasPunjabiKw)) ||
                (prefHasTelugu && (hasTelugu || hasTeluguKw)) ||
                (prefHasTamil && (hasTamil || hasTamilKw)) ||
                (prefHasMalayalam && (hasMalayalam || hasMalayalamKw)) ||
                (prefHasSpanish && hasSpanishKw) ||
                (prefHasKorean && (hasHangul || hasKoreanKw))
            return matchesSelected
        }

        // If user selected English + other languages (e.g. English + Hindi):
        // Allow if it matches any preferred regional language, or is clean Latin without non-matching markers
        return true
    }
}
