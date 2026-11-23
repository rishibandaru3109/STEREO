package com.nova.music.data.repository

import com.nova.music.core.database.dao.RecommendationDao
import com.nova.music.core.network.TrafficMeter
import com.nova.music.core.database.entity.RecommendationCacheEntity
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.recommendation.CandidateProvider
import com.nova.music.domain.recommendation.ContentFilter
import com.nova.music.domain.recommendation.RecommendationEligibility
import com.nova.music.domain.recommendation.RecommendationEngine
import com.nova.music.domain.recommendation.distinctRecordings
import com.nova.music.domain.repository.HomeRepository
import com.nova.music.domain.repository.ListeningProfileRepository
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.domain.repository.RecommendationPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HomeRepositoryImpl(
    private val listeningProfileRepository: ListeningProfileRepository,
    private val candidateProvider: CandidateProvider,
    private val recommendationEngine: RecommendationEngine,
    private val recommendationDao: RecommendationDao,
    private val preferencesRepository: MusicPreferencesRepository? = null
) : HomeRepository {
    private val cacheLock = Mutex()

    override suspend fun getHomeFeed(forceRefresh: Boolean): Result<List<RecommendationSection>> =
        withContext(Dispatchers.IO) {
            cacheLock.withLock {
                try {
                    Result.success(loadFeed(forceRefresh))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Result.failure(error)
                }
            }
        }

    private suspend fun loadFeed(forceRefresh: Boolean): List<RecommendationSection> {
        val preferences = preferencesRepository?.recommendationPreferences?.first() ?: RecommendationPreferences()
        val profile = listeningProfileRepository.getListeningProfile()
        val excluded = listeningProfileRepository.getExcludedTrackIds() +
            profile.topTracks.filter { it.score < 0 }.map { it.trackId }
        val now = System.currentTimeMillis()
        val fingerprint = listOf(
            preferences.languages.sorted(), preferences.genres.sorted(), preferences.discoveryMode,
            preferences.contentFilterMode, profile.likedTrackIds.sorted(),
            // Regenerate sections from current history locally without refetching the catalog after every song.
            profile.totalListens / 10, excluded.sorted()
        ).hashCode().toString()

        val cached = recommendationDao.getCachedSection(CANDIDATE_CACHE)
        val cachedTracks = cached?.takeIf { now - it.cachedAt <= OFFLINE_TTL }
            ?.let { TrackJsonSerializer.deserializeTracks(it.tracksJson) }.orEmpty()
            .filter { it.id !in excluded && RecommendationEligibility.accepts(it, preferences) }
        val candidates = if (!forceRefresh && cached?.sectionTitle == fingerprint && cached.expiresAt > now) {
            TrafficMeter.counters.metadataCacheHit()
            cachedTracks
        } else {
            val fresh = try {
                candidateProvider.getCandidates(profile, excluded, preferences)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }.filter { it.id !in excluded && RecommendationEligibility.accepts(it, preferences) }
            val localIds = profile.recentTracks.map { it.id }.toSet() + profile.likedTrackIds
            val remoteCount = fresh.count { it.id !in localIds }
            val merged = distinctRecordings(fresh + cachedTracks.takeIf { remoteCount < 12 }.orEmpty())
            if (remoteCount > 0 || cached == null) {
                recommendationDao.cacheSection(
                    RecommendationCacheEntity(
                        sectionKey = CANDIDATE_CACHE,
                        sectionTitle = fingerprint,
                        tracksJson = TrackJsonSerializer.serializeTracks(merged.take(160)),
                        cachedAt = if (remoteCount > 0) now else cached?.cachedAt ?: now,
                        expiresAt = now + if (remoteCount >= 12) 60 * MINUTE else 5 * MINUTE
                    )
                )
            } else if (now - cached.cachedAt <= OFFLINE_TTL) {
                recommendationDao.cacheSection(cached.copy(sectionTitle = fingerprint, expiresAt = now + 5 * MINUTE))
            }
            merged
        }

        val languageByTrack = candidates.associateBy { it.id }
        return recommendationEngine.generateSections(profile, candidates, excluded).mapNotNull { section ->
            if (section.type != SectionType.RECENTLY_PLAYED) return@mapNotNull section
            val recent = section.tracks.map { languageByTrack[it.id] ?: it }
                .filter { it.id !in excluded && ContentFilter.isMusicTrack(it) }
            section.copy(tracks = recent).takeIf { recent.isNotEmpty() }
        }
    }

    override suspend fun recordExplicitFeedback(trackId: String, feedbackType: String) {
        listeningProfileRepository.recordExplicitFeedback(trackId, feedbackType)
    }

    override suspend fun clearCache() {
        cacheLock.withLock { recommendationDao.clearAllCache() }
    }

    private companion object {
        const val CANDIDATE_CACHE = "candidate_pool_v1"
        const val MINUTE = 60_000L
        const val OFFLINE_TTL = 7 * 24 * 60 * MINUTE
    }
}
