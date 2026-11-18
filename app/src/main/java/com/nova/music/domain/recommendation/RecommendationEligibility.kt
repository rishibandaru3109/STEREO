package com.nova.music.domain.recommendation

import com.nova.music.domain.model.StereoLanguages
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

    // Non-Latin Script Regexes
    private val DEVANAGARI_REGEX = Regex("[\\u0900-\\u097F]")
    private val GURMUKHI_REGEX = Regex("[\\u0A00-\\u0A7F]")
    private val GUJARATI_REGEX = Regex("[\\u0A80-\\u0AFF]")
    private val BENGALI_REGEX = Regex("[\\u0980-\\u09FF]")
    private val TAMIL_REGEX = Regex("[\\u0B80-\\u0BFF]")
    private val TELUGU_REGEX = Regex("[\\u0C00-\\u0C7F]")
    private val KANNADA_REGEX = Regex("[\\u0C80-\\u0CFF]")
    private val MALAYALAM_REGEX = Regex("[\\u0D00-\\u0D7F]")
    private val HANGUL_REGEX = Regex("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F]")
    private val KANA_REGEX = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF]")
    private val CJK_REGEX = Regex("[\\u4E00-\\u9FFF]")
    private val THAI_REGEX = Regex("[\\u0E00-\\u0E7F]")
    private val BURMESE_REGEX = Regex("[\\u1000-\\u109F]")
    private val HEBREW_REGEX = Regex("[\\u0590-\\u05FF]")
    private val ARABIC_REGEX = Regex("[\\u0600-\\u06FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")
    private val CYRILLIC_REGEX = Regex("[\\u0400-\\u04FF]")
    private val GREEK_REGEX = Regex("[\\u0370-\\u03FF]")
    private val ETHIOPIC_REGEX = Regex("[\\u1200-\\u137F]")

    // Regional keyword regexes (case-insensitive)
    private val HINDI_KEYWORDS = Regex("(?i)\\b(hindi|bollywood|ghazal|bhajan)\\b")
    private val PUNJABI_KEYWORDS = Regex("(?i)\\b(punjabi|bhangra)\\b")
    private val GUJARATI_KEYWORDS = Regex("(?i)\\b(gujarati|garba|dandiya)\\b")
    private val BENGALI_KEYWORDS = Regex("(?i)\\b(bengali|bangla|rabindra)\\b")
    private val MARATHI_KEYWORDS = Regex("(?i)\\b(marathi|lavani)\\b")
    private val URDU_KEYWORDS = Regex("(?i)\\b(urdu|qawwali)\\b")
    private val NEPALI_KEYWORDS = Regex("(?i)\\b(nepali|nepal)\\b")
    private val TELUGU_KEYWORDS = Regex("(?i)\\b(telugu|tollywood)\\b")
    private val TAMIL_KEYWORDS = Regex("(?i)\\b(tamil|kollywood)\\b")
    private val MALAYALAM_KEYWORDS = Regex("(?i)\\b(malayalam|mollywood)\\b")
    private val KANNADA_KEYWORDS = Regex("(?i)\\b(kannada|sandalwood)\\b")
    private val SPANISH_KEYWORDS = Regex("(?i)\\b(spanish|espanol|latino|reggaeton|bachata)\\b")
    private val PORTUGUESE_KEYWORDS = Regex("(?i)\\b(portuguese|portugues|samba|bossa nova|sertanejo|funk carioca)\\b")
    private val FRENCH_KEYWORDS = Regex("(?i)\\b(french|francais|chanson)\\b")
    private val GERMAN_KEYWORDS = Regex("(?i)\\b(german|deutsch|schlager)\\b")
    private val ITALIAN_KEYWORDS = Regex("(?i)\\b(italian|italiano|canzone)\\b")
    private val RUSSIAN_KEYWORDS = Regex("(?i)\\b(russian|русский)\\b")
    private val UKRAINIAN_KEYWORDS = Regex("(?i)\\b(ukrainian|українська)\\b")
    private val POLISH_KEYWORDS = Regex("(?i)\\b(polish|polski)\\b")
    private val DUTCH_KEYWORDS = Regex("(?i)\\b(dutch|nederlands)\\b")
    private val GREEK_KEYWORDS = Regex("(?i)\\b(greek|ελληνικά|bouzouki)\\b")
    private val KOREAN_KEYWORDS = Regex("(?i)\\b(korean|kpop|k-pop)\\b")
    private val JAPANESE_KEYWORDS = Regex("(?i)\\b(japanese|jpop|j-pop|anime)\\b")
    private val MANDARIN_KEYWORDS = Regex("(?i)\\b(mandarin|cpop|c-pop|mandopop|huayu)\\b")
    private val CANTONESE_KEYWORDS = Regex("(?i)\\b(cantonese|cantopop)\\b")
    private val INDONESIAN_KEYWORDS = Regex("(?i)\\b(indonesian|dangdut)\\b")
    private val MALAY_KEYWORDS = Regex("(?i)\\b(malay|melayu)\\b")
    private val THAI_KEYWORDS = Regex("(?i)\\b(thai|t-pop)\\b")
    private val VIETNAMESE_KEYWORDS = Regex("(?i)\\b(vietnamese|vpop|v-pop)\\b")
    private val FILIPINO_KEYWORDS = Regex("(?i)\\b(filipino|tagalog|opm)\\b")
    private val BURMESE_KEYWORDS = Regex("(?i)\\b(burmese|myanmar)\\b")
    private val ARABIC_KEYWORDS = Regex("(?i)\\b(arabic|tarab|dabke|khaliji)\\b")
    private val TURKISH_KEYWORDS = Regex("(?i)\\b(turkish|turkce|turku)\\b")
    private val PERSIAN_KEYWORDS = Regex("(?i)\\b(persian|farsi|iranian)\\b")
    private val HEBREW_KEYWORDS = Regex("(?i)\\b(hebrew|mizrahi|ivrit)\\b")
    private val SWAHILI_KEYWORDS = Regex("(?i)\\b(swahili|kiswahili|bongo flava)\\b")
    private val AMHARIC_KEYWORDS = Regex("(?i)\\b(amharic|ethiopian)\\b")
    private val ROMANIAN_KEYWORDS = Regex("(?i)\\b(romanian|romana|manele)\\b")
    private val HUNGARIAN_KEYWORDS = Regex("(?i)\\b(hungarian|magyar)\\b")

    private val ALL_NON_ENGLISH_KEYWORDS = Regex(
        "(?i)\\b(hindi|bollywood|ghazal|bhajan|punjabi|bhangra|telugu|tollywood|tamil|kollywood|malayalam|mollywood|kannada|sandalwood|bhojpuri|marathi|bengali|gujarati|urdu|nepali|kpop|k-pop|jpop|j-pop|cpop|c-pop|cantopop|dangdut|vpop|v-pop|opm|manele|reggaeton|bachata|bossa nova|sertanejo|chanson|schlager)\\b"
    )

    fun accepts(track: Track, preferences: RecommendationPreferences): Boolean =
        ContentFilter.isMusicTrack(track) && acceptsLanguage(track, preferences)

    fun acceptsLanguage(track: Track, preferences: RecommendationPreferences): Boolean {
        if (preferences.languages.isEmpty()) return true

        val userLangs = StereoLanguages.normalize(preferences.languages)
            .ifEmpty { preferences.languages }

        // 1. Explicit verified lyrics language
        if (track.lyricsLanguage != null) {
            val trackLangs = track.lyricsLanguage.split('+')
                .map { StereoLanguages.resolve(it)?.name ?: it }
            return trackLangs.all { lang -> userLangs.any { it.equals(lang, ignoreCase = true) } }
        }

        val text = "${track.title} ${track.artist} ${track.album.orEmpty()}"

        // Script checks
        val hasDevanagari = DEVANAGARI_REGEX.containsMatchIn(text)
        val hasGurmukhi = GURMUKHI_REGEX.containsMatchIn(text)
        val hasGujarati = GUJARATI_REGEX.containsMatchIn(text)
        val hasBengali = BENGALI_REGEX.containsMatchIn(text)
        val hasTamil = TAMIL_REGEX.containsMatchIn(text)
        val hasTelugu = TELUGU_REGEX.containsMatchIn(text)
        val hasKannada = KANNADA_REGEX.containsMatchIn(text)
        val hasMalayalam = MALAYALAM_REGEX.containsMatchIn(text)
        val hasHangul = HANGUL_REGEX.containsMatchIn(text)
        val hasKana = KANA_REGEX.containsMatchIn(text)
        val hasCJK = CJK_REGEX.containsMatchIn(text)
        val hasThai = THAI_REGEX.containsMatchIn(text)
        val hasBurmese = BURMESE_REGEX.containsMatchIn(text)
        val hasHebrew = HEBREW_REGEX.containsMatchIn(text)
        val hasArabicScript = ARABIC_REGEX.containsMatchIn(text)
        val hasCyrillic = CYRILLIC_REGEX.containsMatchIn(text)
        val hasGreek = GREEK_REGEX.containsMatchIn(text)
        val hasEthiopic = ETHIOPIC_REGEX.containsMatchIn(text)

        fun hasPref(vararg names: String): Boolean =
            names.any { name -> userLangs.any { it.equals(name, ignoreCase = true) } }

        // Confident script mismatch rejection
        if (hasDevanagari && !hasPref("Hindi", "Marathi", "Nepali")) return false
        if (hasGurmukhi && !hasPref("Punjabi")) return false
        if (hasGujarati && !hasPref("Gujarati")) return false
        if (hasBengali && !hasPref("Bengali")) return false
        if (hasTamil && !hasPref("Tamil")) return false
        if (hasTelugu && !hasPref("Telugu")) return false
        if (hasKannada && !hasPref("Kannada")) return false
        if (hasMalayalam && !hasPref("Malayalam")) return false
        if (hasHangul && !hasPref("Korean")) return false
        if (hasKana && !hasPref("Japanese")) return false
        if (hasCJK && !hasPref("Mandarin Chinese", "Cantonese", "Japanese")) return false
        if (hasThai && !hasPref("Thai")) return false
        if (hasBurmese && !hasPref("Burmese")) return false
        if (hasHebrew && !hasPref("Hebrew")) return false
        if (hasArabicScript && !hasPref("Arabic", "Urdu", "Persian")) return false
        if (hasCyrillic && !hasPref("Russian", "Ukrainian")) return false
        if (hasGreek && !hasPref("Greek")) return false
        if (hasEthiopic && !hasPref("Amharic")) return false

        // Confident keyword mismatch rejection
        if (HINDI_KEYWORDS.containsMatchIn(text) && !hasPref("Hindi")) return false
        if (PUNJABI_KEYWORDS.containsMatchIn(text) && !hasPref("Punjabi")) return false
        if (GUJARATI_KEYWORDS.containsMatchIn(text) && !hasPref("Gujarati")) return false
        if (BENGALI_KEYWORDS.containsMatchIn(text) && !hasPref("Bengali")) return false
        if (MARATHI_KEYWORDS.containsMatchIn(text) && !hasPref("Marathi")) return false
        if (URDU_KEYWORDS.containsMatchIn(text) && !hasPref("Urdu")) return false
        if (NEPALI_KEYWORDS.containsMatchIn(text) && !hasPref("Nepali")) return false
        if (TELUGU_KEYWORDS.containsMatchIn(text) && !hasPref("Telugu")) return false
        if (TAMIL_KEYWORDS.containsMatchIn(text) && !hasPref("Tamil")) return false
        if (MALAYALAM_KEYWORDS.containsMatchIn(text) && !hasPref("Malayalam")) return false
        if (KANNADA_KEYWORDS.containsMatchIn(text) && !hasPref("Kannada")) return false
        if (SPANISH_KEYWORDS.containsMatchIn(text) && !hasPref("Spanish")) return false
        if (PORTUGUESE_KEYWORDS.containsMatchIn(text) && !hasPref("Portuguese")) return false
        if (FRENCH_KEYWORDS.containsMatchIn(text) && !hasPref("French")) return false
        if (GERMAN_KEYWORDS.containsMatchIn(text) && !hasPref("German")) return false
        if (ITALIAN_KEYWORDS.containsMatchIn(text) && !hasPref("Italian")) return false
        if (RUSSIAN_KEYWORDS.containsMatchIn(text) && !hasPref("Russian")) return false
        if (UKRAINIAN_KEYWORDS.containsMatchIn(text) && !hasPref("Ukrainian")) return false
        if (POLISH_KEYWORDS.containsMatchIn(text) && !hasPref("Polish")) return false
        if (DUTCH_KEYWORDS.containsMatchIn(text) && !hasPref("Dutch")) return false
        if (GREEK_KEYWORDS.containsMatchIn(text) && !hasPref("Greek")) return false
        if (KOREAN_KEYWORDS.containsMatchIn(text) && !hasPref("Korean")) return false
        if (JAPANESE_KEYWORDS.containsMatchIn(text) && !hasPref("Japanese")) return false
        if (MANDARIN_KEYWORDS.containsMatchIn(text) && !hasPref("Mandarin Chinese")) return false
        if (CANTONESE_KEYWORDS.containsMatchIn(text) && !hasPref("Cantonese")) return false
        if (INDONESIAN_KEYWORDS.containsMatchIn(text) && !hasPref("Indonesian")) return false
        if (MALAY_KEYWORDS.containsMatchIn(text) && !hasPref("Malay")) return false
        if (THAI_KEYWORDS.containsMatchIn(text) && !hasPref("Thai")) return false
        if (VIETNAMESE_KEYWORDS.containsMatchIn(text) && !hasPref("Vietnamese")) return false
        if (FILIPINO_KEYWORDS.containsMatchIn(text) && !hasPref("Filipino")) return false
        if (BURMESE_KEYWORDS.containsMatchIn(text) && !hasPref("Burmese")) return false
        if (ARABIC_KEYWORDS.containsMatchIn(text) && !hasPref("Arabic")) return false
        if (TURKISH_KEYWORDS.containsMatchIn(text) && !hasPref("Turkish")) return false
        if (PERSIAN_KEYWORDS.containsMatchIn(text) && !hasPref("Persian")) return false
        if (HEBREW_KEYWORDS.containsMatchIn(text) && !hasPref("Hebrew")) return false
        if (SWAHILI_KEYWORDS.containsMatchIn(text) && !hasPref("Swahili")) return false
        if (AMHARIC_KEYWORDS.containsMatchIn(text) && !hasPref("Amharic")) return false
        if (ROMANIAN_KEYWORDS.containsMatchIn(text) && !hasPref("Romanian")) return false
        if (HUNGARIAN_KEYWORDS.containsMatchIn(text) && !hasPref("Hungarian")) return false

        val hasEnglish = hasPref("English")

        // If English is the ONLY preference, verify no non-English markers
        if (userLangs.size == 1 && hasEnglish) {
            val hasAnyNonEnglishScript = hasDevanagari || hasGurmukhi || hasGujarati || hasBengali ||
                hasTamil || hasTelugu || hasKannada || hasMalayalam || hasHangul || hasKana ||
                hasCJK || hasThai || hasBurmese || hasHebrew || hasArabicScript || hasCyrillic ||
                hasGreek || hasEthiopic
            val hasAnyNonEnglishKeyword = ALL_NON_ENGLISH_KEYWORDS.containsMatchIn(text)
            if (hasAnyNonEnglishScript || hasAnyNonEnglishKeyword) return false
            return true
        }

        // If user selected non-English languages without English:
        if (!hasEnglish) {
            val hasMatchingScript = (hasDevanagari && hasPref("Hindi", "Marathi", "Nepali")) ||
                (hasGurmukhi && hasPref("Punjabi")) ||
                (hasGujarati && hasPref("Gujarati")) ||
                (hasBengali && hasPref("Bengali")) ||
                (hasTamil && hasPref("Tamil")) ||
                (hasTelugu && hasPref("Telugu")) ||
                (hasKannada && hasPref("Kannada")) ||
                (hasMalayalam && hasPref("Malayalam")) ||
                (hasHangul && hasPref("Korean")) ||
                (hasKana && hasPref("Japanese")) ||
                (hasCJK && hasPref("Mandarin Chinese", "Cantonese", "Japanese")) ||
                (hasThai && hasPref("Thai")) ||
                (hasBurmese && hasPref("Burmese")) ||
                (hasHebrew && hasPref("Hebrew")) ||
                (hasArabicScript && hasPref("Arabic", "Urdu", "Persian")) ||
                (hasCyrillic && hasPref("Russian", "Ukrainian")) ||
                (hasGreek && hasPref("Greek")) ||
                (hasEthiopic && hasPref("Amharic"))

            val hasMatchingKeyword = (HINDI_KEYWORDS.containsMatchIn(text) && hasPref("Hindi")) ||
                (PUNJABI_KEYWORDS.containsMatchIn(text) && hasPref("Punjabi")) ||
                (GUJARATI_KEYWORDS.containsMatchIn(text) && hasPref("Gujarati")) ||
                (BENGALI_KEYWORDS.containsMatchIn(text) && hasPref("Bengali")) ||
                (MARATHI_KEYWORDS.containsMatchIn(text) && hasPref("Marathi")) ||
                (URDU_KEYWORDS.containsMatchIn(text) && hasPref("Urdu")) ||
                (NEPALI_KEYWORDS.containsMatchIn(text) && hasPref("Nepali")) ||
                (TELUGU_KEYWORDS.containsMatchIn(text) && hasPref("Telugu")) ||
                (TAMIL_KEYWORDS.containsMatchIn(text) && hasPref("Tamil")) ||
                (MALAYALAM_KEYWORDS.containsMatchIn(text) && hasPref("Malayalam")) ||
                (KANNADA_KEYWORDS.containsMatchIn(text) && hasPref("Kannada")) ||
                (SPANISH_KEYWORDS.containsMatchIn(text) && hasPref("Spanish")) ||
                (PORTUGUESE_KEYWORDS.containsMatchIn(text) && hasPref("Portuguese")) ||
                (FRENCH_KEYWORDS.containsMatchIn(text) && hasPref("French")) ||
                (GERMAN_KEYWORDS.containsMatchIn(text) && hasPref("German")) ||
                (ITALIAN_KEYWORDS.containsMatchIn(text) && hasPref("Italian")) ||
                (RUSSIAN_KEYWORDS.containsMatchIn(text) && hasPref("Russian")) ||
                (UKRAINIAN_KEYWORDS.containsMatchIn(text) && hasPref("Ukrainian")) ||
                (POLISH_KEYWORDS.containsMatchIn(text) && hasPref("Polish")) ||
                (DUTCH_KEYWORDS.containsMatchIn(text) && hasPref("Dutch")) ||
                (GREEK_KEYWORDS.containsMatchIn(text) && hasPref("Greek")) ||
                (KOREAN_KEYWORDS.containsMatchIn(text) && hasPref("Korean")) ||
                (JAPANESE_KEYWORDS.containsMatchIn(text) && hasPref("Japanese")) ||
                (MANDARIN_KEYWORDS.containsMatchIn(text) && hasPref("Mandarin Chinese")) ||
                (CANTONESE_KEYWORDS.containsMatchIn(text) && hasPref("Cantonese")) ||
                (INDONESIAN_KEYWORDS.containsMatchIn(text) && hasPref("Indonesian")) ||
                (MALAY_KEYWORDS.containsMatchIn(text) && hasPref("Malay")) ||
                (THAI_KEYWORDS.containsMatchIn(text) && hasPref("Thai")) ||
                (VIETNAMESE_KEYWORDS.containsMatchIn(text) && hasPref("Vietnamese")) ||
                (FILIPINO_KEYWORDS.containsMatchIn(text) && hasPref("Filipino")) ||
                (BURMESE_KEYWORDS.containsMatchIn(text) && hasPref("Burmese")) ||
                (ARABIC_KEYWORDS.containsMatchIn(text) && hasPref("Arabic")) ||
                (TURKISH_KEYWORDS.containsMatchIn(text) && hasPref("Turkish")) ||
                (PERSIAN_KEYWORDS.containsMatchIn(text) && hasPref("Persian")) ||
                (HEBREW_KEYWORDS.containsMatchIn(text) && hasPref("Hebrew")) ||
                (SWAHILI_KEYWORDS.containsMatchIn(text) && hasPref("Swahili")) ||
                (AMHARIC_KEYWORDS.containsMatchIn(text) && hasPref("Amharic")) ||
                (ROMANIAN_KEYWORDS.containsMatchIn(text) && hasPref("Romanian")) ||
                (HUNGARIAN_KEYWORDS.containsMatchIn(text) && hasPref("Hungarian"))

            if (hasMatchingScript || hasMatchingKeyword) return true

            // Safe fallback: If it has no foreign markers and came through discovery
            return false
        }

        // Multi-language user with English (e.g. English + Telugu, English + Spanish)
        return true
    }
}
