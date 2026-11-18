package com.nova.music.domain.model

enum class LanguageRegion(val displayName: String) {
    SOUTH_ASIA("South Asia"),
    EUROPE_AMERICAS("Europe & Americas"),
    EAST_SOUTHEAST_ASIA("East & Southeast Asia"),
    MIDDLE_EAST_AFRICA("Middle East & Africa"),
    ADDITIONAL("Additional")
}

data class StereoLanguage(
    val id: String,
    val name: String,
    val nativeName: String,
    val region: LanguageRegion,
    val searchKeyword: String
)

object StereoLanguages {

    val ALL: List<StereoLanguage> = listOf(
        // South Asia (11)
        StereoLanguage("hi", "Hindi", "हिन्दी", LanguageRegion.SOUTH_ASIA, "hindi songs"),
        StereoLanguage("te", "Telugu", "తెలుగు", LanguageRegion.SOUTH_ASIA, "telugu songs"),
        StereoLanguage("ta", "Tamil", "தமிழ்", LanguageRegion.SOUTH_ASIA, "tamil songs"),
        StereoLanguage("ml", "Malayalam", "മലയാളം", LanguageRegion.SOUTH_ASIA, "malayalam songs"),
        StereoLanguage("kn", "Kannada", "ಕನ್ನಡ", LanguageRegion.SOUTH_ASIA, "kannada songs"),
        StereoLanguage("bn", "Bengali", "বাংলা", LanguageRegion.SOUTH_ASIA, "bengali songs"),
        StereoLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", LanguageRegion.SOUTH_ASIA, "punjabi songs"),
        StereoLanguage("mr", "Marathi", "मराठी", LanguageRegion.SOUTH_ASIA, "marathi songs"),
        StereoLanguage("gu", "Gujarati", "ગુજરાતી", LanguageRegion.SOUTH_ASIA, "gujarati songs"),
        StereoLanguage("ur", "Urdu", "اردو", LanguageRegion.SOUTH_ASIA, "urdu songs"),
        StereoLanguage("ne", "Nepali", "नेपाली", LanguageRegion.SOUTH_ASIA, "nepali songs"),

        // Europe & Americas (11)
        StereoLanguage("en", "English", "English", LanguageRegion.EUROPE_AMERICAS, "top english hits"),
        StereoLanguage("es", "Spanish", "Español", LanguageRegion.EUROPE_AMERICAS, "spanish latin songs"),
        StereoLanguage("pt", "Portuguese", "Português", LanguageRegion.EUROPE_AMERICAS, "portuguese songs"),
        StereoLanguage("fr", "French", "Français", LanguageRegion.EUROPE_AMERICAS, "french chanson music"),
        StereoLanguage("de", "German", "Deutsch", LanguageRegion.EUROPE_AMERICAS, "german pop songs"),
        StereoLanguage("it", "Italian", "Italiano", LanguageRegion.EUROPE_AMERICAS, "italian pop songs"),
        StereoLanguage("ru", "Russian", "Русский", LanguageRegion.EUROPE_AMERICAS, "russian pop songs"),
        StereoLanguage("uk", "Ukrainian", "Українська", LanguageRegion.EUROPE_AMERICAS, "ukrainian songs"),
        StereoLanguage("pl", "Polish", "Polski", LanguageRegion.EUROPE_AMERICAS, "polish music"),
        StereoLanguage("nl", "Dutch", "Nederlands", LanguageRegion.EUROPE_AMERICAS, "dutch pop songs"),
        StereoLanguage("el", "Greek", "Ελληνικά", LanguageRegion.EUROPE_AMERICAS, "greek songs"),

        // East & Southeast Asia (10)
        StereoLanguage("ko", "Korean", "한국어", LanguageRegion.EAST_SOUTHEAST_ASIA, "k-pop songs"),
        StereoLanguage("ja", "Japanese", "日本語", LanguageRegion.EAST_SOUTHEAST_ASIA, "j-pop songs"),
        StereoLanguage("zh", "Mandarin Chinese", "中文", LanguageRegion.EAST_SOUTHEAST_ASIA, "mandarin chinese songs"),
        StereoLanguage("yue", "Cantonese", "粵語", LanguageRegion.EAST_SOUTHEAST_ASIA, "cantonese songs"),
        StereoLanguage("id", "Indonesian", "Bahasa Indonesia", LanguageRegion.EAST_SOUTHEAST_ASIA, "indonesian pop songs"),
        StereoLanguage("ms", "Malay", "Bahasa Melayu", LanguageRegion.EAST_SOUTHEAST_ASIA, "malay songs"),
        StereoLanguage("th", "Thai", "ไทย", LanguageRegion.EAST_SOUTHEAST_ASIA, "thai pop songs"),
        StereoLanguage("vi", "Vietnamese", "Tiếng Việt", LanguageRegion.EAST_SOUTHEAST_ASIA, "v-pop songs"),
        StereoLanguage("fil", "Filipino", "Filipino", LanguageRegion.EAST_SOUTHEAST_ASIA, "opm filipino songs"),
        StereoLanguage("my", "Burmese", "မြန်မာစာ", LanguageRegion.EAST_SOUTHEAST_ASIA, "burmese songs"),

        // Middle East & Africa (6)
        StereoLanguage("ar", "Arabic", "العربية", LanguageRegion.MIDDLE_EAST_AFRICA, "arabic songs"),
        StereoLanguage("tr", "Turkish", "Türkçe", LanguageRegion.MIDDLE_EAST_AFRICA, "turkish pop songs"),
        StereoLanguage("fa", "Persian", "فارسی", LanguageRegion.MIDDLE_EAST_AFRICA, "persian music"),
        StereoLanguage("he", "Hebrew", "עברית", LanguageRegion.MIDDLE_EAST_AFRICA, "hebrew songs"),
        StereoLanguage("sw", "Swahili", "Kiswahili", LanguageRegion.MIDDLE_EAST_AFRICA, "swahili bongo flava"),
        StereoLanguage("am", "Amharic", "አማርኛ", LanguageRegion.MIDDLE_EAST_AFRICA, "amharic ethiopian songs"),

        // Additional (2)
        StereoLanguage("ro", "Romanian", "Română", LanguageRegion.ADDITIONAL, "romanian songs"),
        StereoLanguage("hu", "Hungarian", "Magyar", LanguageRegion.ADDITIONAL, "hungarian songs")
    )

    private val byIdMap: Map<String, StereoLanguage> = ALL.associateBy { it.id.lowercase() }
    private val byNameMap: Map<String, StereoLanguage> = ALL.associateBy { it.name.lowercase() }

    fun findById(id: String): StereoLanguage? = byIdMap[id.lowercase()]

    fun findByName(name: String): StereoLanguage? = byNameMap[name.lowercase()]

    fun resolve(idOrName: String): StereoLanguage? =
        byIdMap[idOrName.lowercase()] ?: byNameMap[idOrName.lowercase()]

    fun normalize(inputs: Collection<String>): Set<String> {
        return inputs.mapNotNull { resolve(it)?.name }.toSet()
    }

    fun byRegion(): Map<LanguageRegion, List<StereoLanguage>> =
        ALL.groupBy { it.region }
}
