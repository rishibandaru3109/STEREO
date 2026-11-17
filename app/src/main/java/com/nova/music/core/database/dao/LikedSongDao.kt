package com.nova.music.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nova.music.core.database.entity.LikedSongEntity
import com.nova.music.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(likedSong: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE trackId = :trackId")
    suspend fun unlike(trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE trackId = :trackId)")
    fun isLikedFlow(trackId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE trackId = :trackId)")
    suspend fun isLiked(trackId: String): Boolean

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN liked_songs l ON t.id = l.trackId
        ORDER BY l.likedAt DESC
    """)
    fun getLikedSongsRecent(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN liked_songs l ON t.id = l.trackId
        ORDER BY t.title COLLATE NOCASE ASC
    """)
    fun getLikedSongsAlphabetical(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN liked_songs l ON t.id = l.trackId
        ORDER BY t.artist COLLATE NOCASE ASC
    """)
    fun getLikedSongsByArtist(): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN liked_songs l ON t.id = l.trackId
        WHERE t.title LIKE '%' || :query || '%' OR t.artist LIKE '%' || :query || '%'
        ORDER BY l.likedAt DESC
    """)
    fun searchLikedSongs(query: String): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM liked_songs")
    fun getLikedSongsCount(): Flow<Int>

    @Query("SELECT trackId FROM liked_songs")
    fun getAllLikedTrackIds(): Flow<List<String>>

    @Query("SELECT trackId FROM liked_songs")
    suspend fun getAllLikedTrackIdsList(): List<String>
}
