package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "playlist_downloads",
    primaryKeys = ["playlistId", "trackId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["trackId"])
    ]
)
data class PlaylistDownloadEntity(
    val playlistId: String,
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)
