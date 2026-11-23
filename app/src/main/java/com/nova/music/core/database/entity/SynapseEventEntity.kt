package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "synapse_listening_events",
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["timestamp"])
    ]
)
data class SynapseEventEntity(
    @PrimaryKey
    val eventId: String,
    val trackId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val listeningDurationMs: Long,
    val totalTrackDurationMs: Long,
    val completionRatio: Float,
    val isManualSkip: Boolean,
    val playbackContext: String? = null,
    val seekCount: Int = 0
)
