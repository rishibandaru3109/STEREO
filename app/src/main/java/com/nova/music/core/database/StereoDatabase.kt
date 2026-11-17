package com.nova.music.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nova.music.core.database.dao.HistoryDao
import com.nova.music.core.database.dao.LikedSongDao
import com.nova.music.core.database.dao.PlaylistDao
import com.nova.music.core.database.dao.QueueDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.database.entity.LikedSongEntity
import com.nova.music.core.database.entity.ListeningHistoryEntity
import com.nova.music.core.database.entity.PlaylistEntity
import com.nova.music.core.database.entity.PlaylistEntryEntity
import com.nova.music.core.database.entity.QueueStateEntity
import com.nova.music.core.database.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        LikedSongEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        ListeningHistoryEntity::class,
        QueueStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StereoDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun likedSongDao(): LikedSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
}
