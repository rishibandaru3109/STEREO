package com.nova.music.data.repository

import com.nova.music.core.database.dao.HistoryDao
import com.nova.music.core.database.dao.LikedSongDao
import com.nova.music.core.database.dao.RecommendationDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.database.entity.ExplicitFeedbackEntity
import com.nova.music.core.database.entity.ListeningHistoryEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.domain.model.ArtistAffinity
import com.nova.music.domain.model.ListeningProfile
import com.nova.music.domain.model.Track
import com.nova.music.domain.model.TrackAffinity
import com.nova.music.domain.repository.ListeningProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.exp

class ListeningProfileRepositoryImpl(
    private val historyDao: HistoryDao,
    private val likedSongDao: LikedSongDao,
    private val trackDao: TrackDao,
    private val recommendationDao: RecommendationDao
) : ListeningProfileRepository {

    override suspend fun getListeningProfile(): ListeningProfile {
        val rawHistory = historyDao.getRawHistoryList(200)
        val likedIds = likedSongDao.getAllLikedTrackIdsList().toSet()
        val allTrackIds = (rawHistory.map { it.trackId } + likedIds).distinct()
        val trackMap = if (allTrackIds.isNotEmpty()) {
            allTrackIds.chunked(500).flatMap { trackDao.getTracksByIds(it) }.associateBy { it.id }
        } else {
            emptyMap()
        }

        val excludedIds = getExcludedTrackIds()
        return computeProfile(rawHistory.filterNot { it.trackId in excludedIds }, likedIds - excludedIds, trackMap)
    }

    override fun observeListeningProfile(): Flow<ListeningProfile> {
        return combine(
            historyDao.getRawHistory(200),
            likedSongDao.getAllLikedTrackIds(),
            recommendationDao.getFeedbackFlow()
        ) { historyList, likedIdList, feedback ->
            val excludedIds = feedback.map { it.trackId }.toSet()
            val likedIds = likedIdList.toSet() - excludedIds
            val allTrackIds = (historyList.map { it.trackId } + likedIds).distinct()
            val trackMap = if (allTrackIds.isNotEmpty()) {
                allTrackIds.chunked(500).flatMap { trackDao.getTracksByIds(it) }.associateBy { it.id }
            } else {
                emptyMap()
            }
            computeProfile(historyList.filterNot { it.trackId in excludedIds }, likedIds, trackMap)
        }
    }

    override suspend fun recordExplicitFeedback(trackId: String, feedbackType: String) {
        recommendationDao.insertFeedback(
            ExplicitFeedbackEntity(
                trackId = trackId,
                feedbackType = feedbackType,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getExcludedTrackIds(): Set<String> {
        return recommendationDao.getExcludedTrackIds().toSet()
    }

    private fun computeProfile(
        historyList: List<ListeningHistoryEntity>,
        likedIds: Set<String>,
        trackMap: Map<String, TrackEntity>
    ): ListeningProfile {
        val now = System.currentTimeMillis()

        // Track stats accumulator
        class TrackStats(
            val trackId: String,
            var score: Double = 0.0,
            var playCount: Int = 0,
            var completionCount: Int = 0,
            var skipCount: Int = 0,
            var lastPlayedAt: Long = 0L
        )

        val trackStatsMap = mutableMapOf<String, TrackStats>()

        // 1. Process history events with exponential time decay
        for (event in historyList) {
            val stats = trackStatsMap.getOrPut(event.trackId) { TrackStats(event.trackId) }
            stats.playCount++
            if (event.playedAt > stats.lastPlayedAt) {
                stats.lastPlayedAt = event.playedAt
            }

            val ageDays = (now - event.playedAt).coerceAtLeast(0L) / (1000.0 * 60 * 60 * 24)
            // Decay lambda ~0.07 means half-life of ~10 days
            val decay = exp(-0.07 * ageDays)

            val isCompleted = event.completionRatio >= 0.8f || event.listeningDurationMs >= 30_000L
            val isSkipped = event.isSkipped || (event.completionRatio < 0.2f && event.listeningDurationMs < 30_000L)

            if (isSkipped) {
                stats.skipCount++
                stats.score -= 20.0 * decay
            } else if (isCompleted) {
                stats.completionCount++
                stats.score += 30.0 * decay
            } else {
                // Partial listen
                stats.score += 5.0 * decay
            }
        }

        // 2. Add Liked Song bonus (+30.0 per Phase H3)
        for (likedId in likedIds) {
            val stats = trackStatsMap.getOrPut(likedId) { TrackStats(likedId) }
            stats.score += 30.0
        }

        // 3. Assemble Track Affinities
        val trackAffinities = trackStatsMap.values.mapNotNull { stats ->
            val entity = trackMap[stats.trackId] ?: return@mapNotNull null
            TrackAffinity(
                trackId = stats.trackId,
                title = entity.title,
                artist = entity.artist,
                score = stats.score,
                playCount = stats.playCount,
                isLiked = likedIds.contains(stats.trackId),
                lastPlayedAt = stats.lastPlayedAt
            )
        }.sortedByDescending { it.score }

        // 4. Assemble Artist Affinities
        class ArtistStats(
            val artistName: String,
            var score: Double = 0.0,
            var playCount: Int = 0,
            var completionCount: Int = 0,
            var lastPlayedAt: Long = 0L
        )

        val artistStatsMap = mutableMapOf<String, ArtistStats>()
        for (trackAffinity in trackAffinities) {
            if (trackAffinity.artist.isBlank()) continue
            val artist = trackAffinity.artist.trim()
            val stats = artistStatsMap.getOrPut(artist.lowercase()) { ArtistStats(artist) }
            stats.score += trackAffinity.score
            stats.completionCount += trackStatsMap.getValue(trackAffinity.trackId).completionCount
            stats.playCount += trackAffinity.playCount
            if (trackAffinity.lastPlayedAt > stats.lastPlayedAt) {
                stats.lastPlayedAt = trackAffinity.lastPlayedAt
            }
        }

        val artistAffinities = artistStatsMap.values.map { stats ->
            ArtistAffinity(
                artistName = stats.artistName,
                score = stats.score,
                playCount = stats.playCount,
                completionCount = stats.completionCount,
                lastPlayedAt = stats.lastPlayedAt
            )
        }.filter { it.score > 0 }.sortedByDescending { it.score }

        // 5. Recent Tracks list (ordered by last played)
        val recentTracks = historyList
            .distinctBy { it.trackId }
            .mapNotNull { event ->
                trackMap[event.trackId]?.toTrack()
            }
            .take(30)

        val totalListens = historyList.size
        val totalSkips = trackStatsMap.values.sumOf { it.skipCount }

        return ListeningProfile(
            topArtists = artistAffinities,
            topTracks = trackAffinities,
            likedTrackIds = likedIds,
            recentTracks = recentTracks,
            totalListens = totalListens,
            totalSkips = totalSkips,
            lastListenAt = historyList.firstOrNull()?.playedAt ?: 0L,
            likedTracks = trackAffinities.asSequence().filter { it.isLiked }.take(40)
                .mapNotNull { trackMap[it.trackId]?.toTrack() }.toList()
        )
    }
}
