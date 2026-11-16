package com.nova.music.domain.repository

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun search(query: String): Flow<Result<List<Track>>>
    suspend fun resolveStream(trackId: String): Result<AudioStream>
    suspend fun getTrack(trackId: String): Result<Track>
}
