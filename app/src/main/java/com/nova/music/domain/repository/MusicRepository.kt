package com.nova.music.domain.repository

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    val networkConnected: Flow<Boolean> get() = kotlinx.coroutines.flow.flowOf(true)
    val effectiveAudioQuality: Flow<com.nova.music.domain.model.AudioQualityMode>
        get() = kotlinx.coroutines.flow.flowOf(com.nova.music.domain.model.AudioQualityMode.HIGH)

    fun invalidateStream(trackId: String) {}
    fun search(query: String): Flow<Result<List<Track>>>
    suspend fun resolveStream(trackId: String): Result<AudioStream>
    suspend fun getTrack(trackId: String): Result<Track>
}
