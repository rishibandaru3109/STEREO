package com.nova.music.data.repository

import com.nova.music.core.database.dao.HistoryDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.database.entity.ListeningHistoryEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val trackDao: TrackDao
) : HistoryRepository {

    override suspend fun recordPlayback(
        track: Track,
        durationMs: Long,
        totalDurationMs: Long,
        isSkipped: Boolean,
        playbackContext: String?
    ) {
        // Debounce: ignore events where track played for less than 3 seconds
        if (durationMs < 3000L && totalDurationMs > 10000L) return

        trackDao.upsert(TrackEntity.fromTrack(track))

        val completionRatio = if (totalDurationMs > 0) {
            (durationMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        val entity = ListeningHistoryEntity(
            trackId = track.id,
            playedAt = System.currentTimeMillis(),
            listeningDurationMs = durationMs,
            completionRatio = completionRatio,
            isSkipped = isSkipped,
            playbackContext = playbackContext
        )
        historyDao.insertHistory(entity)
    }

    override fun getRecentTracks(limit: Int): Flow<List<Track>> {
        return historyDao.getRecentUniqueTracks(limit).map { list ->
            list.map { it.toTrack() }
        }
    }

    override suspend fun removeTrackFromHistory(trackId: String) {
        historyDao.removeHistoryForTrack(trackId)
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
