package com.nova.music.domain.repository

import com.nova.music.domain.model.RecommendationSection

interface HomeRepository {
    suspend fun getHomeFeed(forceRefresh: Boolean = false): Result<List<RecommendationSection>>
    suspend fun recordExplicitFeedback(trackId: String, feedbackType: String)
    suspend fun clearCache()
}
