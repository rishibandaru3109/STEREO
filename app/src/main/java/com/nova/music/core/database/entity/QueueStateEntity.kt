package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey
    val queueId: String = DEFAULT_QUEUE_ID,
    val currentTrackId: String? = null,
    val playbackPositionMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: String = "OFF",
    val originalOrderJson: String = "[]",
    val activeOrderJson: String = "[]",
    val currentIndex: Int = 0,
    val shuffleSeed: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_QUEUE_ID = "default_queue"
    }
}
