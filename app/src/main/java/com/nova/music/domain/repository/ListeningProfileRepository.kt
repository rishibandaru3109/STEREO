package com.nova.music.domain.repository

import com.nova.music.domain.model.ListeningProfile
import kotlinx.coroutines.flow.Flow

interface ListeningProfileRepository {
    suspend fun getListeningProfile(): ListeningProfile
    fun observeListeningProfile(): Flow<ListeningProfile>
    suspend fun recordExplicitFeedback(trackId: String, feedbackType: String)
    suspend fun getExcludedTrackIds(): Set<String>
}
