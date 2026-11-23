package com.nova.music.domain.recommendation

import android.util.Log
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.synapse.recommendation.candidate.InMemoryCandidateProvider
import com.synapse.recommendation.engine.SynapseRecommendationEngine
import com.synapse.recommendation.model.RecommendationCandidate
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.SynapseTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/**
 * Production bridge and coordinator connecting STEREO's domain recommendation interfaces
 * to the autonomous SYNAPSE recommendation engine.
 *
 * Implements:
 * 1. Runtime Feature Flag control (KEY_SYNAPSE_RECOMMENDER_ENABLED)
 * 2. Zero-Crash Circuit Breaker with seamless fallback to legacy RecommendationEngineImpl
 * 3. MMR-Diversified rail partitioning (Quick Picks, Daily Mixes, Discover Weekly, Because You Listened)
 */
class SynapseRecommendationEngineBridge(
    private val synapseEngine: SynapseRecommendationEngine,
    private val preferencesRepository: MusicPreferencesRepository,
    private val legacyEngine: RecommendationEngineImpl = RecommendationEngineImpl()
) : RecommendationEngine {

    companion object {
        private const val TAG = "SYNAPSE_BRIDGE"
    }

    override fun generateSections(
        profile: ListeningProfile,
        candidates: List<Track>,
        excludedTrackIds: Set<String>
    ): List<RecommendationSection> {
        val isEnabled = try {
            runBlocking { preferencesRepository.isSynapseEnabled.first() }
        } catch (t: Throwable) {
            true
        }

        if (!isEnabled) {
            Log.d(TAG, "SYNAPSE is disabled via feature flag; delegating to legacy engine.")
            return legacyEngine.generateSections(profile, candidates, excludedTrackIds)
        }

        return try {
            executeSynapsePipeline(profile, candidates, excludedTrackIds)
        } catch (t: Throwable) {
            Log.e(TAG, "SYNAPSE pipeline encountered an exception; executing circuit-breaker fallback to legacy engine.", t)
            legacyEngine.generateSections(profile, candidates, excludedTrackIds)
        }
    }

    private fun executeSynapsePipeline(
        profile: ListeningProfile,
        candidates: List<Track>,
        excludedTrackIds: Set<String>
    ): List<RecommendationSection> = runBlocking {
        if (candidates.isEmpty()) {
            return@runBlocking legacyEngine.generateSections(profile, candidates, excludedTrackIds)
        }

        val trackMap = candidates.associateBy { it.id }
        val synapseTracks = candidates.map { toSynapseTrack(it) }

        val discoveryAlpha = try {
            preferencesRepository.synapseDiscoveryAlpha.first().toDouble()
        } catch (_: Throwable) {
            0.65
        }

        val preferredLangs = try {
            preferencesRepository.preferredLanguages.first()
        } catch (_: Throwable) {
            emptySet()
        }

        val preferredGenres = try {
            preferencesRepository.preferredGenres.first()
        } catch (_: Throwable) {
            emptySet()
        }

        // Configure candidate provider for current pool
        val tempEngine = SynapseRecommendationEngine(
            candidateProvider = InMemoryCandidateProvider(synapseTracks),
            tasteIntelligence = synapseEngine.getTasteIntelligence()
        )

        val context = RecommendationContext(
            currentTrackId = profile.recentTracks.firstOrNull()?.id,
            sessionHistory = profile.recentTracks.take(10).map { toSynapseTrack(it) },
            excludedTrackIds = excludedTrackIds,
            discoveryAlpha = discoveryAlpha,
            preferredLanguages = preferredLangs,
            preferredGenres = preferredGenres,
            maxResults = candidates.size
        )

        val response = tempEngine.recommend(context)
        val rankedTracks = response.recommendations.mapNotNull { trackMap[it.track.id] }

        if (rankedTracks.isEmpty()) {
            Log.w(TAG, "SYNAPSE produced 0 recommendations; falling back to legacy.")
            return@runBlocking legacyEngine.generateSections(profile, candidates, excludedTrackIds)
        }

        Log.d(TAG, "SYNAPSE ranked ${rankedTracks.size} candidates in ${response.diagnostics.latencyMs}ms (fallback: ${response.diagnostics.isFallbackActivated})")

        // Build distinct STEREO recommendation rails with strict cross-rail deduplication
        val consumedTrackIds = mutableSetOf<String>()
        val sections = mutableListOf<RecommendationSection>()
        val knownTrackIds = (profile.recentTracks.map { it.id } + profile.likedTrackIds).toSet()

        // 1. QUICK PICKS (Top MMR-ranked head, up to 6 tracks with artist diversity)
        val quickPicks = mutableListOf<Track>()
        val quickPickArtistCounts = mutableMapOf<String, Int>()
        for (track in rankedTracks) {
            if (track.id in consumedTrackIds) continue
            val count = quickPickArtistCounts.getOrDefault(track.artist.lowercase(), 0)
            if (count < 2) {
                quickPicks.add(track)
                quickPickArtistCounts[track.artist.lowercase()] = count + 1
                if (quickPicks.size == 6) break
            }
        }
        // Fallback if diversity was too restrictive
        if (quickPicks.size < 4) {
            for (track in rankedTracks) {
                if (track.id !in consumedTrackIds && track !in quickPicks) {
                    quickPicks.add(track)
                    if (quickPicks.size == 6) break
                }
            }
        }

        if (quickPicks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.QUICK_PICKS,
                    title = "Quick Picks",
                    subtitle = "Start listening instantly • Powered by SYNAPSE",
                    tracks = quickPicks
                )
            )
            consumedTrackIds.addAll(quickPicks.map { it.id })
        }

        // 2. DAILY MIX (Curated mixes built from SYNAPSE affinity clusters)
        val availableForMixes = rankedTracks.filter { it.id !in consumedTrackIds }
        val mixes = generateDailyMixes(profile, availableForMixes, consumedTrackIds)
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

        // 3. RECENT (Listening history access)
        if (profile.recentTracks.isNotEmpty()) {
            val recentList = profile.recentTracks
                .filter { it.id !in excludedTrackIds }
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

        // 4. WEEKLY DISCOVER (Fresh discovery tracks strictly outside history and top artist)
        val topArtist = profile.topArtists.firstOrNull()?.artistName
        val discoverTracks = rankedTracks.filter { track ->
            track.id !in consumedTrackIds &&
            track.id !in knownTrackIds &&
            (topArtist == null || !track.artist.equals(topArtist, ignoreCase = true))
        }.take(15)

        if (discoverTracks.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = SectionType.DISCOVER_WEEKLY,
                    title = "Weekly Discover",
                    subtitle = "Fresh music you haven't heard yet",
                    tracks = discoverTracks
                )
            )
            consumedTrackIds.addAll(discoverTracks.map { it.id })
        }

        // 5. BECAUSE YOU LISTENED TO [Artist]
        if (topArtist != null) {
            var artistTracks = rankedTracks.filter {
                it.id !in consumedTrackIds && it.artist.equals(topArtist, ignoreCase = true)
            }.take(12)

            // If other rails (e.g. daily mix) consumed all tracks, ensure the dedicated artist rail is preserved
            if (artistTracks.isEmpty()) {
                val quickPickIds = quickPicks.map { it.id }.toSet()
                artistTracks = rankedTracks.filter {
                    it.id !in quickPickIds && it.artist.equals(topArtist, ignoreCase = true)
                }.take(12)
            }

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

        // 6. EXPLORE MORE (Remaining diverse candidates)
        val remaining = rankedTracks.filter { it.id !in consumedTrackIds }.take(12)
        if (remaining.size >= 4) {
            sections.add(
                RecommendationSection(
                    type = SectionType.EXPLORE_MORE,
                    title = "Explore More",
                    subtitle = "Selected by SYNAPSE adaptive intelligence",
                    tracks = remaining
                )
            )
            consumedTrackIds.addAll(remaining.map { it.id })
        }

        sections
    }

    private fun generateDailyMixes(
        profile: ListeningProfile,
        candidates: List<Track>,
        consumedTrackIds: MutableSet<String>
    ): List<RecommendationMix> {
        val mixes = mutableListOf<RecommendationMix>()
        if (candidates.isEmpty()) return mixes

        if (!profile.isColdStart && profile.topArtists.isNotEmpty()) {
            val artist1 = profile.topArtists.first().artistName
            val artist1Tracks = candidates.filter { it.artist.equals(artist1, ignoreCase = true) && it.id !in consumedTrackIds }
            val otherTracks = candidates.filter { !it.artist.equals(artist1, ignoreCase = true) && it.id !in consumedTrackIds }
            val mix1Tracks = (artist1Tracks.take(6) + otherTracks.take(8)).distinctBy { it.id }

            if (mix1Tracks.isNotEmpty()) {
                mixes.add(
                    RecommendationMix(
                        id = "synapse_mix_1",
                        title = "Daily Mix 1",
                        subtitle = "$artist1 & similar artists",
                        tracks = mix1Tracks,
                        coverArtUrls = mix1Tracks.mapNotNull { it.thumbnailUrl.takeIf { u -> u.isNotBlank() } }.take(4)
                    )
                )
                consumedTrackIds.addAll(mix1Tracks.map { it.id })
            }

            val artist2 = profile.topArtists.getOrNull(1)?.artistName
            if (artist2 != null) {
                val artist2Tracks = candidates.filter { it.artist.equals(artist2, ignoreCase = true) && it.id !in consumedTrackIds }
                val remaining = candidates.filter { !it.artist.equals(artist2, ignoreCase = true) && it.id !in consumedTrackIds }
                val mix2Tracks = (artist2Tracks.take(6) + remaining.take(8)).distinctBy { it.id }

                if (mix2Tracks.isNotEmpty()) {
                    mixes.add(
                        RecommendationMix(
                            id = "synapse_mix_2",
                            title = "Daily Mix 2",
                            subtitle = "$artist2 & similar artists",
                            tracks = mix2Tracks,
                            coverArtUrls = mix2Tracks.mapNotNull { it.thumbnailUrl.takeIf { u -> u.isNotBlank() } }.take(4)
                        )
                    )
                    consumedTrackIds.addAll(mix2Tracks.map { it.id })
                }
            }
        }

        return mixes
    }

    private fun toSynapseTrack(track: Track): SynapseTrack {
        return SynapseTrack(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = track.durationSeconds,
            thumbnailUrl = track.thumbnailUrl,
            language = track.lyricsLanguage,
            isSpokenWord = track.isSpokenWord
        )
    }
}
