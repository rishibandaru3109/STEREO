package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nova.music.core.database.entity.ListeningHistoryEntity
import com.nova.music.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ListeningHistoryEntity): Long

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN (
            SELECT trackId, MAX(playedAt) as maxPlayedAt
            FROM listening_history
            GROUP BY trackId
            ORDER BY maxPlayedAt DESC
            LIMIT :limit
        ) h ON t.id = h.trackId
        ORDER BY h.maxPlayedAt DESC
    """)
    fun getRecentUniqueTracks(limit: Int = 50): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN listening_history h ON t.id = h.trackId
        ORDER BY h.playedAt DESC
        LIMIT :limit
    """)
    fun getFullHistory(limit: Int = 100): Flow<List<TrackEntity>>

    @Query("DELETE FROM listening_history WHERE trackId = :trackId")
    suspend fun removeHistoryForTrack(trackId: String)

    @Query("DELETE FROM listening_history")
    suspend fun clearHistory()
}
