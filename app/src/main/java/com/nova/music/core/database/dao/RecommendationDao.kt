package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nova.music.core.database.entity.ExplicitFeedbackEntity
import com.nova.music.core.database.entity.RecommendationCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheSection(cache: RecommendationCacheEntity)

    @Query("SELECT * FROM recommendation_cache WHERE sectionKey = :sectionKey LIMIT 1")
    suspend fun getCachedSection(sectionKey: String): RecommendationCacheEntity?

    @Query("SELECT * FROM recommendation_cache")
    fun getAllCachedSections(): Flow<List<RecommendationCacheEntity>>

    @Query("DELETE FROM recommendation_cache WHERE sectionKey = :sectionKey")
    suspend fun clearSectionCache(sectionKey: String)

    @Query("DELETE FROM recommendation_cache WHERE expiresAt < :currentTime")
    suspend fun clearExpiredCache(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM recommendation_cache")
    suspend fun clearAllCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: ExplicitFeedbackEntity)

    @Query("SELECT * FROM explicit_feedback")
    fun getFeedbackFlow(): Flow<List<ExplicitFeedbackEntity>>

    @Query("SELECT trackId FROM explicit_feedback")
    suspend fun getExcludedTrackIds(): List<String>

    @Query("DELETE FROM explicit_feedback WHERE trackId = :trackId")
    suspend fun removeFeedback(trackId: String)
}
