package com.nova.music.data.repository

import com.nova.music.core.database.dao.LikedSongDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.database.entity.LikedSongEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.LikedSongsRepository
import com.nova.music.domain.repository.LikedSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LikedSongsRepositoryImpl(
    private val trackDao: TrackDao,
    private val likedSongDao: LikedSongDao
) : LikedSongsRepository {

    override fun getLikedSongs(sortOrder: LikedSortOrder): Flow<List<Track>> {
        val entityFlow = when (sortOrder) {
            LikedSortOrder.RECENTLY_ADDED -> likedSongDao.getLikedSongsRecent()
            LikedSortOrder.TITLE -> likedSongDao.getLikedSongsAlphabetical()
            LikedSortOrder.ARTIST -> likedSongDao.getLikedSongsByArtist()
        }
        return entityFlow.map { list -> list.map { it.toTrack() } }
    }

    override fun searchLikedSongs(query: String): Flow<List<Track>> {
        return likedSongDao.searchLikedSongs(query.trim()).map { list -> list.map { it.toTrack() } }
    }

    override fun isLiked(trackId: String): Flow<Boolean> {
        return likedSongDao.isLikedFlow(trackId)
    }

    override suspend fun isLikedSync(trackId: String): Boolean {
        return likedSongDao.isLiked(trackId)
    }

    override suspend fun likeTrack(track: Track) {
        trackDao.upsert(TrackEntity.fromTrack(track))
        likedSongDao.insertLike(LikedSongEntity(trackId = track.id))
    }

    override suspend fun unlikeTrack(trackId: String) {
        likedSongDao.unlike(trackId)
    }

    override suspend fun toggleLike(track: Track): Boolean {
        val currentlyLiked = likedSongDao.isLiked(track.id)
        return if (currentlyLiked) {
            unlikeTrack(track.id)
            false
        } else {
            likeTrack(track)
            true
        }
    }

    override fun getLikedCount(): Flow<Int> {
        return likedSongDao.getLikedSongsCount()
    }

    override fun getAllLikedTrackIds(): Flow<List<String>> {
        return likedSongDao.getAllLikedTrackIds()
    }
}
