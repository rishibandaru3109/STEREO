package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nova.music.core.database.entity.SynapseEventEntity
import com.nova.music.core.database.entity.SynapseTasteProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SynapseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SynapseEventEntity)

    @Query("SELECT * FROM synapse_listening_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<SynapseEventEntity>

    @Query("SELECT * FROM synapse_listening_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int): Flow<List<SynapseEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: SynapseTasteProfileEntity)

    @Query("SELECT * FROM synapse_taste_profile WHERE profileKey = :key LIMIT 1")
    suspend fun getProfile(key: String = "default_user_profile"): SynapseTasteProfileEntity?

    @Query("DELETE FROM synapse_listening_events")
    suspend fun clearEvents()

    @Query("DELETE FROM synapse_taste_profile")
    suspend fun clearProfile()
}
