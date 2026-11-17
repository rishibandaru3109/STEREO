package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nova.music.core.database.entity.QueueStateEntity

@Dao
interface QueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQueueState(queueState: QueueStateEntity)

    @Query("SELECT * FROM queue_state WHERE queueId = :queueId")
    suspend fun getQueueState(queueId: String = QueueStateEntity.DEFAULT_QUEUE_ID): QueueStateEntity?

    @Query("DELETE FROM queue_state WHERE queueId = :queueId")
    suspend fun clearQueueState(queueId: String = QueueStateEntity.DEFAULT_QUEUE_ID)
}
