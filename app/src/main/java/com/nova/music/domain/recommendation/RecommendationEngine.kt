package com.nova.music.domain.recommendation

import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import kotlin.random.Random

interface RecommendationEngine {
    fun generateSections(
        profile: ListeningProfile,
        candidates: List<Track>,
        excludedTrackIds: Set<String>
    ): List<RecommendationSection>
}

class RecommendationEngineImpl : RecommendationEngine {

    override fun generateSections(
        profile: ListeningProfile,
        candidates: List<Track>,
        excludedTrackIds: Set<String>
    ): List<RecommendationSection> {
        val validCandidates = distinctRecordings(candidates.filter { it.id !in excludedTrackIds })
        val knownTrackIds = (profile.recentTracks.map { it.id } + profile.likedTrackIds).toSet()

        // Strict cross-section deduplication registry
        val consumedTrackIds = mutableSetOf<String>()
        val sections = mutableListOf<RecommendationSection>()

        val recentIds = profile.recentTracks.map { it.id }.toSet()
        val recommendationPool = validCandidates.filterNot { it.id in recentIds }
        val topArtist = profile.topArtists.firstOrNull()?.artistName
        val artistReserve = recommendationPool.filter { it.artist.equals(topArtist, ignoreCase = true) }
            .takeLast((recommendationPool.size / 10).coerceAtMost(3)).map { it.id }.toSet()
        val mixPool = recommendationPool.filterNot { it.id in artistReserve }

        val quickPicks = generateQuickPicks(mixPool, consumedTrackIds)
        if (quickPicks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.QUICK_PICKS,
                    title = "Quick Picks",
                    subtitle = "Start listening instantly",
                    tracks = quickPicks
                )
            )
        }

        // 2. DAILY MIX (Phase B1: Horizontally scrollable mix cards based on user preferences)
        val discoverReserve = mixPool.asReversed().filter {
            it.id !in knownTrackIds && it.id !in consumedTrackIds
        }.take((validCandidates.size / 4).coerceAtMost(15)).map { it.id }.toSet()
        val mixes = generateDailyMixes(profile, mixPool.filter { it.id !in discoverReserve }, consumedTrackIds)
        if (mixes.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.MADE_FOR_YOU,
                    title = "Daily Mix",
                    subtitle = "Personalized mixes tailored to your vibe",
                    mixes = mixes
                )
            )
        }

        // 3. RECENT (Phase B1: Actual listening history, quick access)
        if (profile.recentTracks.isNotEmpty()) {
            val recentList = profile.recentTracks
                .filter { !excludedTrackIds.contains(it.id) }
                .take(15)
            if (recentList.isNotEmpty()) {
                sections.add(
                    RecommendationSection(
                        type = SectionType.RECENTLY_PLAYED,
                        title = "Recent",
                        subtitle = "Jump back in",
                        tracks = recentList
                    )
                )
            }
        }

        // 4. WEEKLY DISCOVER (Phase B1: Fresh discovery music, strictly excludes Daily Mix and History)
        val discoverTracks = generateWeeklyDiscover(profile, recommendationPool, knownTrackIds, consumedTrackIds)
        if (discoverTracks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.DISCOVER_WEEKLY,
                    title = "Weekly Discover",
                    subtitle = "Fresh music you haven't heard yet",
                    tracks = discoverTracks
                )
            )
        }

        // 5. FURTHER RECOMMENDATIONS (Phase B1: Distinct related music, strictly deduplicated)
        val furtherSections = generateFurtherRecommendations(profile, recommendationPool, consumedTrackIds)
        sections.addAll(furtherSections)

        return sections
    }

    private fun generateQuickPicks(
        candidates: List<Track>,
        consumedTrackIds: MutableSet<String>
    ): List<Track> {
        val picks = diversifyTracks(candidates, maxConsecutivePerArtist = 2).take(6)
        consumedTrackIds.addAll(picks.map { it.id })
        return picks
    }

    private fun generateDailyMixes(
        profile: ListeningProfile,
        candidates: List<Track>,
        consumedTrackIds: MutableSet<String>
    ): List<RecommendationMix> {
        val mixes = mutableListOf<RecommendationMix>()

        if (!profile.isColdStart && profile.topArtists.isNotEmpty()) {
            // Mix 1: Top Artist #1 Focus + Related Candidates
            val artist1 = profile.topArtists.first().artistName
            val availableForMix1 = candidates.filter { it.id !in consumedTrackIds }
            val artist1Tracks = availableForMix1.filter { it.artist.equals(artist1, ignoreCase = true) }
            val otherCandidates = availableForMix1.filter { !it.artist.equals(artist1, ignoreCase = true) }
            val mix1Pool = (artist1Tracks.take(6) + otherCandidates.shuffled(Random(101)).take(8))
            val mix1Tracks = diversifyTracks(mix1Pool.distinctBy { it.id }, maxConsecutivePerArtist = 2)

            if (mix1Tracks.isNotEmpty()) {
                mixes.add(
                    RecommendationMix(
                        id = "mix_1",
                        title = "Daily Mix 1",
                        subtitle = "$artist1 & similar artists",
                        tracks = mix1Tracks,
                        coverArtUrls = mix1Tracks.mapNotNull { it.thumbnailUrl.takeIf { u -> u.isNotBlank() } }.take(4)
                    )
                )
                consumedTrackIds.addAll(mix1Tracks.map { it.id })
            }

            // Mix 2: Top Artist #2 (or runner up) Focus
            val artist2 = profile.topArtists.getOrNull(1)?.artistName
            if (artist2 != null) {
                val availableForMix2 = candidates.filter { it.id !in consumedTrackIds }
                val artist2Tracks = availableForMix2.filter { it.artist.equals(artist2, ignoreCase = true) }
                val remaining = availableForMix2.filter { !it.artist.equals(artist2, ignoreCase = true) }
                val mix2Pool = (artist2Tracks.take(6) + remaining.shuffled(Random(202)).take(8))
                val mix2Tracks = diversifyTracks(mix2Pool.distinctBy { it.id }, maxConsecutivePerArtist = 2)

                if (mix2Tracks.isNotEmpty()) {
                    mixes.add(
                        RecommendationMix(
                            id = "mix_2",
                            title = "Daily Mix 2",
                            subtitle = "$artist2 & similar artists",
                            tracks = mix2Tracks,
                            coverArtUrls = mix2Tracks.mapNotNull { it.thumbnailUrl.takeIf { u -> u.isNotBlank() } }.take(4)
                        )
                    )
                    consumedTrackIds.addAll(mix2Tracks.map { it.id })
                }
            }

            // Mix 3: Discovery Mix
            val knownIds = (profile.recentTracks.map { it.id } + profile.likedTrackIds).toSet()
            val freshCandidates = candidates
                .filter { it.id !in consumedTrackIds && it.id !in knownIds }
                .shuffled(Random(303))
                .take(12)
            val mix3Tracks = diversifyTracks(freshCandidates, maxConsecutivePerArtist = 2)
            if (mix3Tracks.isNotEmpty()) {
                mixes.add(
                    RecommendationMix(
                        id = "mix_discovery",
                        title = "Discovery Mix",
                        subtitle = "Fresh sounds picked for you",
                        tracks = mix3Tracks,
                        coverArtUrls = mix3Tracks.mapNotNull { it.thumbnailUrl.takeIf { u -> u.isNotBlank() } }.take(4)
                    )
                )
                consumedTrackIds.addAll(mix3Tracks.map { it.id })
            }
        } else {
            // Cold start curated starter mixes
            val chunks = candidates.chunked(10)
            val starterNames = listOf(
                "Trending Hits" to "Top trending music right now",
                "Chill Vibes" to "Laid back and acoustic rhythms",
                "Fresh Finds" to "New discoveries to start your journey"
            )

            for ((index, pair) in starterNames.withIndex()) {
                val pool = (chunks.getOrNull(index) ?: candidates.shuffled(Random(index * 99 + 1)).take(10))
                    .filter { it.id !in consumedTrackIds }
                val tracks = diversifyTracks(pool, maxConsecutivePerArtist = 2)
                if (tracks.isNotEmpty()) {
                    mixes.add(
                        RecommendationMix(
                            id = "starter_mix_${index + 1}",
                            title = pair.first,
                            subtitle = pair.second,
                            tracks = tracks,
                            coverArtUrls = tracks.mapNotNull { it.thumbnailUrl.takeIf { u -> u.isNotBlank() } }.take(4)
                        )
                    )
                    consumedTrackIds.addAll(tracks.map { it.id })
                }
            }
        }

        return mixes
    }

    private fun generateWeeklyDiscover(
        profile: ListeningProfile,
        candidates: List<Track>,
        knownTrackIds: Set<String>,
        consumedTrackIds: MutableSet<String>
    ): List<Track> {
        // Strictly exclude tracks already selected in Daily Mix AND tracks in listening history.
        // Also prioritize fresh artists outside the top established artist so Weekly Discover is genuinely novel.
        val topArtist = profile.topArtists.firstOrNull()?.artistName
        val freshDiscoveryPool = candidates.filter { candidate ->
            !consumedTrackIds.contains(candidate.id) &&
            !knownTrackIds.contains(candidate.id) &&
            (topArtist == null || !candidate.artist.equals(topArtist, ignoreCase = true))
        }

        val diversified = diversifyTracks(freshDiscoveryPool, maxConsecutivePerArtist = 2).take(15)
        consumedTrackIds.addAll(diversified.map { it.id })
        return diversified
    }

    private fun generateFurtherRecommendations(
        profile: ListeningProfile,
        candidates: List<Track>,
        consumedTrackIds: MutableSet<String>
    ): List<RecommendationSection> {
        val sections = mutableListOf<RecommendationSection>()

        // 1. Because You Listened To [Artist]
        if (profile.topArtists.isNotEmpty()) {
            val topArtist = profile.topArtists.first().artistName
            val artistTracks = candidates
                .filter { it.id !in consumedTrackIds && it.artist.equals(topArtist, ignoreCase = true) }
                .take(12)

            if (artistTracks.isNotEmpty()) {
                sections.add(
                    RecommendationSection(
                        type = SectionType.BECAUSE_YOU_LISTENED,
                        title = "Because You Listened To $topArtist",
                        subtitle = "More tracks from and inspired by your top artist",
                        tracks = artistTracks
                    )
                )
                consumedTrackIds.addAll(artistTracks.map { it.id })
            }
        }

        // 2. Extra Discovery Rail if distinct candidates remain
        val remainingUnconsumed = candidates.filter { !consumedTrackIds.contains(it.id) }
        if (remainingUnconsumed.size >= 4) {
            val extraTracks = diversifyTracks(remainingUnconsumed, maxConsecutivePerArtist = 2).take(12)
            if (extraTracks.size >= 4) {
                sections.add(
                    RecommendationSection(
                        type = SectionType.EXPLORE_MORE,
                        title = "Explore More",
                        subtitle = "Selected from your preferred genres",
                        tracks = extraTracks
                    )
                )
                consumedTrackIds.addAll(extraTracks.map { it.id })
            }
        }

        return sections
    }

    /**
     * Enforces artist diversification: at most [maxConsecutivePerArtist] consecutive tracks
     * from the same artist in any recommended sequence.
     */
    fun diversifyTracks(tracks: List<Track>, maxConsecutivePerArtist: Int = 2): List<Track> {
        if (tracks.size <= 1) return tracks

        val result = mutableListOf<Track>()
        val pending = tracks.toMutableList()

        while (pending.isNotEmpty()) {
            val lastArtist = result.lastOrNull()?.artist?.lowercase()?.trim()
            val limit = maxConsecutivePerArtist.coerceAtLeast(1)
            val isBlocked = result.size >= limit && result.takeLast(limit)
                .all { it.artist.lowercase().trim() == lastArtist }

            val candidateIndex = if (isBlocked) {
                pending.indexOfFirst { it.artist.lowercase().trim() != lastArtist }
            } else {
                0
            }

            if (candidateIndex >= 0) {
                result.add(pending.removeAt(candidateIndex))
            } else {
                result.add(pending.removeAt(0))
            }
        }

        return result
    }
}
