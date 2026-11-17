package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "explicit_feedback")
data class ExplicitFeedbackEntity(
    @PrimaryKey
    val trackId: String,
    val feedbackType: String, // "DISLIKE", "NOT_INTERESTED"
    val createdAt: Long = System.currentTimeMillis()
)
