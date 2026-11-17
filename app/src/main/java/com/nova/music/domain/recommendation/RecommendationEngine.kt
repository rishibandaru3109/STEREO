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
        val validCandidates = candidates.filter { !excludedTrackIds.contains(it.id) }
        val knownTrackIds = (profile.recentTracks.map { it.id } + profile.likedTrackIds).toSet()

        val sections = mutableListOf<RecommendationSection>()

        // 1. QUICK PICKS (blends top known tracks + top scoring new candidates)
        val quickPicks = generateQuickPicks(profile, validCandidates)
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

        // 2. RECENTLY PLAYED (pure listening history, if available)
        if (profile.recentTracks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.RECENTLY_PLAYED,
                    title = "Recently Played",
                    subtitle = "Jump back in",
                    tracks = profile.recentTracks.filter { !excludedTrackIds.contains(it.id) }.take(15)
                )
            )
        }

        // 3. MADE FOR YOU (Algorithmic mixes with multi-artist clustering and artwork mosaics)
        val mixes = generateMadeForYouMixes(profile, validCandidates)
        if (mixes.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.MADE_FOR_YOU,
                    title = "Made For You",
                    subtitle = "Personalized mixes tailored to your vibe",
                    mixes = mixes
                )
            )
        }

        // 4. ON REPEAT (Songs played repeatedly with high completion)
        val onRepeatTracks = generateOnRepeat(profile, validCandidates)
        if (onRepeatTracks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.ON_REPEAT,
                    title = "On Repeat",
                    subtitle = "Your current heavy rotation",
                    tracks = onRepeatTracks
                )
            )
        }

        // 5. DISCOVER WEEKLY (Fresh tracks user has never played before)
        val discoverTracks = generateDiscoverWeekly(profile, validCandidates, knownTrackIds)
        if (discoverTracks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.DISCOVER_WEEKLY,
                    title = "Discover Weekly",
                    subtitle = "Fresh music you haven't heard yet",
                    tracks = discoverTracks
                )
            )
        }

        // 6. BECAUSE YOU LISTENED TO [ARTIST]
        if (profile.topArtists.isNotEmpty()) {
            val topArtist = profile.topArtists.first().artistName
            val becauseTracks = validCandidates
                .filter { it.artist.contains(topArtist, ignoreCase = true) }
                .take(12)
            if (becauseTracks.isNotEmpty()) {
                sections.add(
                    RecommendationSection(
                        type = SectionType.BECAUSE_YOU_LISTENED,
                        title = "Because You Listened To $topArtist",
                        subtitle = "More tracks from and inspired by your top artist",
                        tracks = becauseTracks
                    )
                )
            }
        }

        return sections
    }

    private fun generateQuickPicks(profile: ListeningProfile, candidates: List<Track>): List<Track> {
        val scoredList = mutableListOf<Pair<Track, Double>>()

        val artistScoreMap = profile.topArtists.associate { it.artistName.lowercase() to it.score }

        // Score candidates
        for (track in candidates) {
            var score = 1.0
            val artistScore = artistScoreMap[track.artist.lowercase()] ?: 0.0
            score += artistScore * 1.5

            if (profile.likedTrackIds.contains(track.id)) {
                score += 3.0
            }
            scoredList.add(track to score)
        }

        // Add recent top tracks
        for (track in profile.recentTracks.take(10)) {
            val artistScore = artistScoreMap[track.artist.lowercase()] ?: 0.0
            val score = 2.5 + artistScore
            if (scoredList.none { it.first.id == track.id }) {
                scoredList.add(track to score)
            }
        }

        val sorted = scoredList.sortedByDescending { it.second }.map { it.first }
        return diversifyTracks(sorted, maxConsecutivePerArtist = 2).take(16)
    }

    private fun generateMadeForYouMixes(profile: ListeningProfile, candidates: List<Track>): List<RecommendationMix> {
        val mixes = mutableListOf<RecommendationMix>()

        if (!profile.isColdStart && profile.topArtists.isNotEmpty()) {
            // Mix 1: Top Artist #1 Focus
            val artist1 = profile.topArtists.first().artistName
            val artist1Tracks = candidates.filter { it.artist.contains(artist1, ignoreCase = true) }
            val otherCandidates = candidates.filter { !it.artist.contains(artist1, ignoreCase = true) }
            val mix1Pool = (artist1Tracks.take(6) + otherCandidates.shuffled(Random(101)).take(9))
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
            }

            // Mix 2: Top Artist #2 (or runner up) Focus
            val artist2 = profile.topArtists.getOrNull(1)?.artistName
            if (artist2 != null) {
                val artist2Tracks = candidates.filter { it.artist.contains(artist2, ignoreCase = true) }
                val remaining = candidates.filter { !it.artist.contains(artist2, ignoreCase = true) }
                val mix2Pool = (artist2Tracks.take(6) + remaining.shuffled(Random(202)).take(9))
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
                }
            }

            // Mix 3: Discovery Mix
            val knownIds = (profile.recentTracks.map { it.id } + profile.likedTrackIds).toSet()
            val freshCandidates = candidates.filter { !knownIds.contains(it.id) }.shuffled(Random(303)).take(15)
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
            }
        } else {
            // Cold start curated starter mixes
            val chunks = candidates.chunked(12)
            val starterNames = listOf(
                "Trending Hits" to "Top trending music right now",
                "Chill Vibes" to "Laid back and acoustic rhythms",
                "Fresh Finds" to "New discoveries to start your journey"
            )

            for ((index, pair) in starterNames.withIndex()) {
                val pool = chunks.getOrNull(index) ?: candidates.shuffled(Random(index * 99 + 1)).take(12)
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
                }
            }
        }

        return mixes
    }

    private fun generateOnRepeat(profile: ListeningProfile, candidates: List<Track>): List<Track> {
        val candidateMap = candidates.associateBy { it.id }
        val repeatAffinityTracks = profile.topTracks
            .filter { it.playCount >= 2 }
            .take(12)

        if (repeatAffinityTracks.isEmpty()) return emptyList()

        return repeatAffinityTracks.mapNotNull { affinity ->
            profile.recentTracks.find { it.id == affinity.trackId }
                ?: candidateMap[affinity.trackId]
                ?: Track(
                    id = affinity.trackId,
                    title = affinity.title,
                    artist = affinity.artist
                )
        }
    }

    private fun generateDiscoverWeekly(
        profile: ListeningProfile,
        candidates: List<Track>,
        knownTrackIds: Set<String>
    ): List<Track> {
        val fresh = candidates.filter { !knownTrackIds.contains(it.id) }
        val artistScoreMap = profile.topArtists.associate { it.artistName.lowercase() to it.score }

        val scored = fresh.map { track ->
            val artistScore = artistScoreMap[track.artist.lowercase()] ?: 0.0
            track to artistScore
        }.sortedByDescending { it.second }.map { it.first }

        return diversifyTracks(scored, maxConsecutivePerArtist = 2).take(15)
    }

    /**
     * Enforces artist diversity constraint:
     * - No more than [maxConsecutivePerArtist] consecutive songs by the same artist.
     * - Reorders tracks smoothly to maintain variety without dropping songs unnecessarily.
     */
    fun diversifyTracks(tracks: List<Track>, maxConsecutivePerArtist: Int = 2): List<Track> {
        if (tracks.size <= 2) return tracks

        val result = mutableListOf<Track>()
        val remaining = tracks.toMutableList()

        while (remaining.isNotEmpty()) {
            val lastArtist = result.lastOrNull()?.artist
            val secondLastArtist = if (result.size >= 2) result[result.size - 2].artist else null

            val mustAvoidSame = lastArtist != null &&
                    secondLastArtist != null &&
                    lastArtist.equals(secondLastArtist, ignoreCase = true)

            val candidateIndex = if (mustAvoidSame) {
                // Find first track by a different artist
                remaining.indexOfFirst { !it.artist.equals(lastArtist, ignoreCase = true) }.let {
                    if (it == -1) 0 else it
                }
            } else {
                0
            }

            val chosen = remaining.removeAt(candidateIndex)
            result.add(chosen)
        }

        return result
    }
}
