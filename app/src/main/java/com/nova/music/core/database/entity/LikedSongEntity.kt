package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "liked_songs",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["likedAt"])
    ]
)
data class LikedSongEntity(
    @PrimaryKey
    val trackId: String,
    val likedAt: Long = System.currentTimeMillis()
)
