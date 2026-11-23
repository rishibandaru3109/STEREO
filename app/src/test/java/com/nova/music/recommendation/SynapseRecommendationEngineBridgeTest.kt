package com.nova.music.recommendation

import com.nova.music.domain.model.ArtistAffinity
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import com.nova.music.domain.recommendation.SynapseRecommendationEngineBridge
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.synapse.recommendation.candidate.InMemoryCandidateProvider
import com.synapse.recommendation.engine.SynapseRecommendationEngine
import com.synapse.recommendation.taste.TasteIntelligence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SynapseRecommendationEngineBridgeTest {

    private lateinit var bridge: SynapseRecommendationEngineBridge
    private lateinit var fakePreferences: FakeMusicPreferencesRepository
    private lateinit var sampleCandidates: List<Track>
    private lateinit var sampleProfile: ListeningProfile

    @Before
    fun setUp() {
        fakePreferences = FakeMusicPreferencesRepository()

        val tracks = mutableListOf<Track>()
        val artists = listOf("A.R. Rahman", "Pink Floyd", "Daft Punk", "Billie Eilish", "Taylor Swift")
        for (i in artists.indices) {
            val artist = artists[i]
            for (j in 1..15) {
                tracks.add(
                    Track(
                        id = "track_${i}_$j",
                        title = "$artist Song $j",
                        artist = artist,
                        durationSeconds = 210L
                    )
                )
            }
        }
        sampleCandidates = tracks

        sampleProfile = ListeningProfile(
            topArtists = listOf(
                ArtistAffinity("A.R. Rahman", score = 80.0, playCount = 10, completionCount = 8),
                ArtistAffinity("Pink Floyd", score = 60.0, playCount = 7, completionCount = 6)
            ),
            recentTracks = sampleCandidates.take(5),
            likedTrackIds = setOf("track_0_1", "track_1_1"),
            totalListens = 15
        )

        val synapseEngine = SynapseRecommendationEngine(
            candidateProvider = InMemoryCandidateProvider(emptyList()),
            tasteIntelligence = TasteIntelligence()
        )

        bridge = SynapseRecommendationEngineBridge(
            synapseEngine = synapseEngine,
            preferencesRepository = fakePreferences,
            legacyEngine = RecommendationEngineImpl()
        )
    }

    @Test
    fun testBridgeGeneratesPersonalizedSectionsWhenEnabled() {
        fakePreferences.synapseEnabled = true
        fakePreferences.discoveryAlpha = 0.65f

        val sections = bridge.generateSections(
            profile = sampleProfile,
            candidates = sampleCandidates,
            excludedTrackIds = emptySet()
        )

        assertFalse("Sections must not be empty", sections.isEmpty())

        // Quick Picks should be present and contain tracks
        val quickPicks = sections.firstOrNull { it.type == SectionType.QUICK_PICKS }
        assertNotNull("Quick Picks rail must be generated", quickPicks)
        assertTrue(quickPicks!!.title.contains("Quick Picks"))
        assertTrue("Quick Picks must have tracks", quickPicks.tracks.isNotEmpty())

        // Daily Mix should be present
        val dailyMix = sections.firstOrNull { it.type == SectionType.MADE_FOR_YOU }
        assertNotNull("Daily Mix rail must be generated", dailyMix)
        assertTrue(dailyMix!!.mixes.isNotEmpty())

        // Top artist rail (A.R. Rahman)
        val becauseRail = sections.firstOrNull { it.type == SectionType.BECAUSE_YOU_LISTENED }
        assertNotNull("Because You Listened To rail must be generated", becauseRail)
        assertTrue(becauseRail!!.title.contains("A.R. Rahman"))
    }

    @Test
    fun testBridgeDelegatesToLegacyWhenDisabled() {
        fakePreferences.synapseEnabled = false

        val sections = bridge.generateSections(
            profile = sampleProfile,
            candidates = sampleCandidates,
            excludedTrackIds = emptySet()
        )

        assertFalse("Sections must not be empty", sections.isEmpty())
        val quickPicks = sections.firstOrNull { it.type == SectionType.QUICK_PICKS }
        assertNotNull(quickPicks)
        // Legacy subtitle does not contain "Powered by SYNAPSE"
        assertFalse(quickPicks?.subtitle?.contains("SYNAPSE") == true)
    }

    @Test
    fun testCircuitBreakerFallbackOnException() {
        // Preference store throws an error during pipeline execution
        val crashingPreferences = object : FakeMusicPreferencesRepository() {
            override val preferredLanguages: Flow<Set<String>>
                get() = kotlinx.coroutines.flow.flow { throw IllegalStateException("Simulated preference store failure") }
        }

        val bridgeWithFaulty = SynapseRecommendationEngineBridge(
            synapseEngine = SynapseRecommendationEngine(
                candidateProvider = InMemoryCandidateProvider(emptyList()),
                tasteIntelligence = TasteIntelligence()
            ),
            preferencesRepository = crashingPreferences,
            legacyEngine = RecommendationEngineImpl()
        )

        // Must NOT crash! Must gracefully fall back to legacy engine
        val sections = bridgeWithFaulty.generateSections(
            profile = sampleProfile,
            candidates = sampleCandidates,
            excludedTrackIds = emptySet()
        )

        assertFalse("Circuit breaker must deliver fallback sections", sections.isEmpty())
    }

    private open class FakeMusicPreferencesRepository : MusicPreferencesRepository {
        var synapseEnabled = true
        var discoveryAlpha = 0.65f

        override val isSynapseEnabled: Flow<Boolean>
            get() = flowOf(synapseEnabled)

        override val synapseDiscoveryAlpha: Flow<Float>
            get() = flowOf(discoveryAlpha)

        override val preferredLanguages: Flow<Set<String>> = flowOf(emptySet())
        override val preferredGenres: Flow<Set<String>> = flowOf(emptySet())
        override val discoveryMode: Flow<String> = flowOf("Familiar")
        override val isOnboardingCompleted: Flow<Boolean> = flowOf(true)
        override val isHistoryEnabled: Flow<Boolean> = flowOf(true)
        override val contentFilterMode: Flow<com.nova.music.domain.repository.ContentFilterMode> =
            flowOf(com.nova.music.domain.repository.ContentFilterMode.MUSIC_ONLY)

        override suspend fun setPreferredLanguages(languages: Set<String>) {}
        override suspend fun setPreferredGenres(genres: Set<String>) {}
        override suspend fun setDiscoveryMode(mode: String) {}
        override suspend fun setOnboardingCompleted(completed: Boolean) {}
        override suspend fun setHistoryEnabled(enabled: Boolean) {}
        override suspend fun setContentFilterMode(mode: com.nova.music.domain.repository.ContentFilterMode) {}
        override suspend fun resetRecommendationPreferences() {}
        override suspend fun setSynapseEnabled(enabled: Boolean) { synapseEnabled = enabled }
        override suspend fun setSynapseDiscoveryAlpha(alpha: Float) { discoveryAlpha = alpha }
    }
}
