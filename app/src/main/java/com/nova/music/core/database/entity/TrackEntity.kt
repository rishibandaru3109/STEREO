package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nova.music.domain.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Long = 0,
    val thumbnailUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl
    )

    companion object {
        fun fromTrack(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = track.durationSeconds,
            thumbnailUrl = track.thumbnailUrl
        )
    }
}
