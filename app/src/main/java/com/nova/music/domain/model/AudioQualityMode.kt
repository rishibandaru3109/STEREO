package com.nova.music.domain.model

enum class AudioQualityMode(val label: String, val description: String) {
    AUTO("Auto", "Data Saver on metered connections, High on unmetered connections"),
    DATA_SAVER("Data Saver", "Lowest available audio bitrate"),
    BALANCED("Balanced", "Closest available audio bitrate to 96 kbps"),
    HIGH("High", "Highest available audio bitrate");

    fun effective(metered: Boolean): AudioQualityMode =
        if (this == AUTO) { if (metered) DATA_SAVER else HIGH } else this

    companion object {
        fun fromStored(value: String?): AudioQualityMode = entries.firstOrNull { it.name == value } ?: AUTO
    }
}

fun <T> selectAudioFormat(formats: List<T>, mode: AudioQualityMode, bitrate: (T) -> Int): T? {
    val known = formats.filter { bitrate(it) > 0 }
    val available = known.ifEmpty { formats }
    return when (mode) {
        AudioQualityMode.DATA_SAVER -> available.minByOrNull(bitrate)
        AudioQualityMode.BALANCED -> available.minWithOrNull(
            compareBy<T> { kotlin.math.abs(bitrate(it).toLong() - 96_000L) }.thenBy(bitrate)
        )
        AudioQualityMode.HIGH, AudioQualityMode.AUTO -> available.maxByOrNull(bitrate)
    }
}
