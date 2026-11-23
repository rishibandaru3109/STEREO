package com.synapse.recommendation.engine

import com.synapse.recommendation.candidate.CandidateDeduplicator
import com.synapse.recommendation.candidate.CandidateProvider
import com.synapse.recommendation.discovery.DiscoveryIntelligence
import com.synapse.recommendation.filter.EligibilityFilter
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.RecommendationResult
import com.synapse.recommendation.model.TasteProfile
import com.synapse.recommendation.ranking.CandidateRanker
import com.synapse.recommendation.ranking.HybridCandidateRanker
import com.synapse.recommendation.taste.TasteIntelligence

/**
 * Diagnostics and audit telemetry for recommendation pipeline execution.
 */
data class SynapseDiagnostics(
    val totalRawCandidates: Int,
    val deduplicatedCandidates: Int,
    val eligibleCandidates: Int,
    val finalRankedCount: Int,
    val isFallbackActivated: Boolean,
    val latencyMs: Long,
    val pipelineStageTimings: Map<String, Long> = emptyMap()
)

/**
 * Recommendation Response payload.
 */
data class SynapseRecommendationResponse(
    val recommendations: List<RecommendationResult>,
    val diagnostics: SynapseDiagnostics
)

/**
 * Main SYNAPSE Recommendation Engine.
 * Thread-safe, provider-agnostic, and fully decoupled from UI or playback buffers.
 */
class SynapseRecommendationEngine(
    private val candidateProvider: CandidateProvider,
    private val tasteIntelligence: TasteIntelligence = TasteIntelligence(),
    private val deduplicator: CandidateDeduplicator = CandidateDeduplicator(),
    private val eligibilityFilter: EligibilityFilter = EligibilityFilter(),
    private val ranker: CandidateRanker = HybridCandidateRanker(),
    private val discoveryIntelligence: DiscoveryIntelligence = DiscoveryIntelligence()
) {

    /**
     * Executes the end-to-end recommendation pipeline.
     */
    suspend fun recommend(context: RecommendationContext): SynapseRecommendationResponse {
        val startNs = System.nanoTime()
        val timings = mutableMapOf<String, Long>()

        // 1. Snapshot taste profile
        val profile = tasteIntelligence.getTasteProfile()

        // 2. Candidate Generation
        val t0 = System.nanoTime()
        val rawCandidates = candidateProvider.generateCandidates(profile, context)
        timings["candidate_generation_ms"] = (System.nanoTime() - t0) / 1_000_000L

        // 3. Candidate Deduplication
        val t1 = System.nanoTime()
        val deduplicated = deduplicator.deduplicate(rawCandidates)
        timings["deduplication_ms"] = (System.nanoTime() - t1) / 1_000_000L

        // 4. Hard Eligibility & Language Filtering
        val t2 = System.nanoTime()
        val filterResult = eligibilityFilter.filter(deduplicated, profile, context)
        timings["eligibility_filtering_ms"] = (System.nanoTime() - t2) / 1_000_000L

        // 5. Multi-Factor Candidate Ranking
        val t3 = System.nanoTime()
        val ranked = ranker.rank(filterResult.eligible, profile, context)
        timings["ranking_ms"] = (System.nanoTime() - t3) / 1_000_000L

        // 6. Discovery Intelligence & MMR Diversity Re-ranking
        val t4 = System.nanoTime()
        val finalSelection = discoveryIntelligence.rerankWithDiversity(
            rankedResults = ranked,
            profile = profile,
            discoveryAlpha = context.discoveryAlpha,
            maxResults = context.maxResults
        )
        timings["diversity_rerank_ms"] = (System.nanoTime() - t4) / 1_000_000L

        val totalLatency = (System.nanoTime() - startNs) / 1_000_000L
        val diagnostics = SynapseDiagnostics(
            totalRawCandidates = rawCandidates.size,
            deduplicatedCandidates = deduplicated.size,
            eligibleCandidates = filterResult.eligible.size,
            finalRankedCount = finalSelection.size,
            isFallbackActivated = filterResult.isFallbackActivated,
            latencyMs = totalLatency,
            pipelineStageTimings = timings
        )

        return SynapseRecommendationResponse(
            recommendations = finalSelection,
            diagnostics = diagnostics
        )
    }

    fun getTasteIntelligence(): TasteIntelligence = tasteIntelligence
}
