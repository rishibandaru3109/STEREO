package com.nova.music.domain.repository

import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun recordPlayback(
        track: Track,
        durationMs: Long,
        totalDurationMs: Long,
        isSkipped: Boolean,
        playbackContext: String? = null
    )

    fun getRecentTracks(limit: Int = 50): Flow<List<Track>>

    suspend fun removeTrackFromHistory(trackId: String)

    suspend fun clearHistory()
}
