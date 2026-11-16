package com.nova.music.data.repository

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MusicRepositoryImpl(
    private val extractor: MusicExtractor
) : MusicRepository {

    override fun search(query: String): Flow<Result<List<Track>>> = flow {
        emit(extractor.search(query))
    }

    override suspend fun resolveStream(trackId: String): Result<AudioStream> {
        return extractor.resolveAudioStream(trackId)
    }

    override suspend fun getTrack(trackId: String): Result<Track> {
        return extractor.getTrackDetails(trackId)
    }
}
