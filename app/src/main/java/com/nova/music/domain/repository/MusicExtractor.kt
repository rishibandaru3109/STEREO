package com.nova.music.domain.repository

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track

/**
 * Independent extractor abstraction.
 * Shielding player engine and UI completely from upstream sources (InnerTubeX, YouTube, etc.)
 */
interface MusicExtractor {
    suspend fun search(query: String): Result<List<Track>>
    suspend fun resolveAudioStream(trackId: String): Result<AudioStream>
    suspend fun resolveAudioStream(trackId: String, quality: com.nova.music.domain.model.AudioQualityMode): Result<AudioStream> =
        resolveAudioStream(trackId)
    suspend fun getTrackDetails(trackId: String): Result<Track>
    suspend fun getSongRadio(trackId: String): Result<List<Track>> = Result.success(emptyList())
    suspend fun getHomeShelves(): Result<List<Track>> = Result.success(emptyList())
}
