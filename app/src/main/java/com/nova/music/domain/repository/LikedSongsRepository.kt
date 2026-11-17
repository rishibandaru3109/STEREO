package com.nova.music.domain.repository

import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow

enum class LikedSortOrder {
    RECENTLY_ADDED,
    TITLE,
    ARTIST
}

interface LikedSongsRepository {
    fun getLikedSongs(sortOrder: LikedSortOrder = LikedSortOrder.RECENTLY_ADDED): Flow<List<Track>>
    fun searchLikedSongs(query: String): Flow<List<Track>>
    fun isLiked(trackId: String): Flow<Boolean>
    suspend fun isLikedSync(trackId: String): Boolean
    suspend fun likeTrack(track: Track)
    suspend fun unlikeTrack(trackId: String)
    suspend fun toggleLike(track: Track): Boolean
    fun getLikedCount(): Flow<Int>
    fun getAllLikedTrackIds(): Flow<List<String>>
}
