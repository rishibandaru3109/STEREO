package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nova.music.core.database.entity.TrackEntity

@Dao
interface TrackDao {

    @Upsert
    suspend fun upsert(track: TrackEntity)

    @Upsert
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<String>): List<TrackEntity>

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteTrackById(id: String)
}
