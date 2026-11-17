package com.nova.music.data.repository

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import com.nova.music.domain.recommendation.ContentFilter
import com.nova.music.domain.repository.ContentFilterMode
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val extractor: MusicExtractor,
    private val preferencesRepository: MusicPreferencesRepository? = null
) : MusicRepository {

    override fun search(query: String): Flow<Result<List<Track>>> = flow {
        val result = extractor.search(query)
        if (result.isSuccess) {
            val tracks = result.getOrNull() ?: emptyList()
            val mode = preferencesRepository?.contentFilterMode?.first() ?: ContentFilterMode.MUSIC_ONLY
            val filtered = ContentFilter.filterTracks(tracks, mode)
            emit(Result.success(filtered))
        } else {
            emit(result)
        }
    }

    override suspend fun resolveStream(trackId: String): Result<AudioStream> {
        return extractor.resolveAudioStream(trackId)
    }

    override suspend fun getTrack(trackId: String): Result<Track> {
        return extractor.getTrackDetails(trackId)
    }
}
