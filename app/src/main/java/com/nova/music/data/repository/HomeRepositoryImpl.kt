package com.nova.music.data.repository

import com.nova.music.core.database.dao.RecommendationDao
import com.nova.music.core.database.entity.RecommendationCacheEntity
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.recommendation.CandidateProvider
import com.nova.music.domain.recommendation.RecommendationEngine
import com.nova.music.domain.repository.HomeRepository
import com.nova.music.domain.repository.ListeningProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepositoryImpl(
    private val listeningProfileRepository: ListeningProfileRepository,
    private val candidateProvider: CandidateProvider,
    private val recommendationEngine: RecommendationEngine,
    private val recommendationDao: RecommendationDao
) : HomeRepository {

    override suspend fun getHomeFeed(forceRefresh: Boolean): Result<List<RecommendationSection>> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()

            // 1. Check cache first if not forced refresh
            if (!forceRefresh) {
                recommendationDao.clearExpiredCache(now)
                val cachedEntities = loadCachedSections(now)
                if (cachedEntities.isNotEmpty()) {
                    return@runCatching cachedEntities
                }
            }

            // 2. Fetch fresh candidates and generate feed
            val profile = listeningProfileRepository.getListeningProfile()
            val excludedTrackIds = listeningProfileRepository.getExcludedTrackIds()
            val candidates = candidateProvider.getCandidates(profile, excludedTrackIds)

            val sections = recommendationEngine.generateSections(profile, candidates, excludedTrackIds)

            // 3. Cache sections
            for (section in sections) {
                val ttlMillis = when (section.type) {
                    SectionType.QUICK_PICKS -> 2 * 60 * 60 * 1000L // 2 hours
                    SectionType.RECENTLY_PLAYED -> 15 * 60 * 1000L // 15 mins
                    SectionType.MADE_FOR_YOU -> 6 * 60 * 60 * 1000L // 6 hours
                    SectionType.DISCOVER_WEEKLY -> 24 * 60 * 60 * 1000L // 24 hours
                    SectionType.ON_REPEAT -> 4 * 60 * 60 * 1000L // 4 hours
                    SectionType.BECAUSE_YOU_LISTENED -> 6 * 60 * 60 * 1000L // 6 hours
                }

                val payloadJson = if (section.type == SectionType.MADE_FOR_YOU) {
                    RecommendationSerializer.serializeMixes(section.mixes)
                } else {
                    TrackJsonSerializer.serializeTracks(section.tracks)
                }

                recommendationDao.cacheSection(
                    RecommendationCacheEntity(
                        sectionKey = section.type.name,
                        sectionTitle = section.title,
                        tracksJson = payloadJson,
                        cachedAt = now,
                        expiresAt = now + ttlMillis
                    )
                )
            }

            sections
        }
    }

    override suspend fun recordExplicitFeedback(trackId: String, feedbackType: String) = withContext(Dispatchers.IO) {
        listeningProfileRepository.recordExplicitFeedback(trackId, feedbackType)
        // Clear cached sections so filtered track is removed on next feed generation
        recommendationDao.clearAllCache()
    }

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        recommendationDao.clearAllCache()
    }

    private suspend fun loadCachedSections(now: Long): List<RecommendationSection> {
        val sections = mutableListOf<RecommendationSection>()

        for (type in SectionType.values()) {
            val cached = recommendationDao.getCachedSection(type.name) ?: continue
            if (cached.expiresAt <= now) continue

            if (type == SectionType.MADE_FOR_YOU) {
                val mixes = RecommendationSerializer.deserializeMixes(cached.tracksJson)
                if (mixes.isNotEmpty()) {
                    sections.add(
                        RecommendationSection(
                            type = type,
                            title = cached.sectionTitle,
                            mixes = mixes
                        )
                    )
                }
            } else {
                val tracks = TrackJsonSerializer.deserializeTracks(cached.tracksJson)
                if (tracks.isNotEmpty()) {
                    sections.add(
                        RecommendationSection(
                            type = type,
                            title = cached.sectionTitle,
                            tracks = tracks
                        )
                    )
                }
            }
        }

        return sections
    }
}
