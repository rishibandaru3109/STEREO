package com.nova.music.domain.model

enum class SectionType {
    QUICK_PICKS,
    RECENTLY_PLAYED,
    MADE_FOR_YOU,
    DISCOVER_WEEKLY,
    ON_REPEAT,
    BECAUSE_YOU_LISTENED
}

data class RecommendationMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
    val coverArtUrls: List<String> = emptyList()
)

data class RecommendationSection(
    val type: SectionType,
    val title: String,
    val subtitle: String? = null,
    val tracks: List<Track> = emptyList(),
    val mixes: List<RecommendationMix> = emptyList()
)
