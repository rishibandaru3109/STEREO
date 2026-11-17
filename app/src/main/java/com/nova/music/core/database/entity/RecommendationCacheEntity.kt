package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendation_cache")
data class RecommendationCacheEntity(
    @PrimaryKey
    val sectionKey: String,
    val sectionTitle: String,
    val tracksJson: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
)
