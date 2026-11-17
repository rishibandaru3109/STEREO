package com.nova.music.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nova.music.core.database.dao.HistoryDao
import com.nova.music.core.database.dao.LikedSongDao
import com.nova.music.core.database.dao.PlaylistDao
import com.nova.music.core.database.dao.QueueDao
import com.nova.music.core.database.dao.RecommendationDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.database.entity.ExplicitFeedbackEntity
import com.nova.music.core.database.entity.LikedSongEntity
import com.nova.music.core.database.entity.ListeningHistoryEntity
import com.nova.music.core.database.entity.PlaylistEntity
import com.nova.music.core.database.entity.PlaylistEntryEntity
import com.nova.music.core.database.entity.QueueStateEntity
import com.nova.music.core.database.entity.RecommendationCacheEntity
import com.nova.music.core.database.entity.TrackEntity

@Database(
    entities = [
        TrackEntity::class,
        LikedSongEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        ListeningHistoryEntity::class,
        QueueStateEntity::class,
        RecommendationCacheEntity::class,
        ExplicitFeedbackEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StereoDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun likedSongDao(): LikedSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun recommendationDao(): RecommendationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recommendation_cache` (
                        `sectionKey` TEXT NOT NULL,
                        `sectionTitle` TEXT NOT NULL,
                        `tracksJson` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        PRIMARY KEY(`sectionKey`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `explicit_feedback` (
                        `trackId` TEXT NOT NULL,
                        `feedbackType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`trackId`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
