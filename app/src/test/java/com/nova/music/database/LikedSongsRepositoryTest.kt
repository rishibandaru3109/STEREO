package com.nova.music.database

import com.nova.music.core.database.dao.LikedSongDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.database.entity.LikedSongEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.data.repository.LikedSongsRepositoryImpl
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.LikedSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTrackDao : TrackDao {
    val tracks = mutableMapOf<String, TrackEntity>()

    override suspend fun upsert(track: TrackEntity) {
        tracks[track.id] = track
    }

    override suspend fun upsertAll(tracks: List<TrackEntity>) {
        tracks.forEach { this.tracks[it.id] = it }
    }

    override suspend fun getTrackById(id: String): TrackEntity? = tracks[id]

    override suspend fun getTracksByIds(ids: List<String>): List<TrackEntity> =
        ids.mapNotNull { tracks[it] }

    override suspend fun deleteTrackById(id: String) {
        tracks.remove(id)
    }
}

class FakeLikedSongDao(private val fakeTrackDao: FakeTrackDao) : LikedSongDao {
    private val likesFlow = MutableStateFlow<Map<String, LikedSongEntity>>(emptyMap())

    override suspend fun insertLike(likedSong: LikedSongEntity) {
        val current = likesFlow.value.toMutableMap()
        current[likedSong.trackId] = likedSong
        likesFlow.value = current
    }

    override suspend fun unlike(trackId: String) {
        val current = likesFlow.value.toMutableMap()
        current.remove(trackId)
        likesFlow.value = current
    }

    override fun isLikedFlow(trackId: String): Flow<Boolean> {
        return likesFlow.map { it.containsKey(trackId) }
    }

    override suspend fun isLiked(trackId: String): Boolean {
        return likesFlow.value.containsKey(trackId)
    }

    override fun getLikedSongsRecent(): Flow<List<TrackEntity>> {
        return likesFlow.map { map ->
            map.values.sortedByDescending { it.likedAt }
                .mapNotNull { fakeTrackDao.tracks[it.trackId] }
        }
    }

    override fun getLikedSongsAlphabetical(): Flow<List<TrackEntity>> {
        return likesFlow.map { map ->
            map.keys.mapNotNull { fakeTrackDao.tracks[it] }
                .sortedBy { it.title.lowercase() }
        }
    }

    override fun getLikedSongsByArtist(): Flow<List<TrackEntity>> {
        return likesFlow.map { map ->
            map.keys.mapNotNull { fakeTrackDao.tracks[it] }
                .sortedBy { it.artist.lowercase() }
        }
    }

    override fun searchLikedSongs(query: String): Flow<List<TrackEntity>> {
        val q = query.lowercase()
        return likesFlow.map { map ->
            map.keys.mapNotNull { fakeTrackDao.tracks[it] }
                .filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
        }
    }

    override fun getLikedSongsCount(): Flow<Int> {
        return likesFlow.map { it.size }
    }

    override fun getAllLikedTrackIds(): Flow<List<String>> {
        return likesFlow.map { it.keys.toList() }
    }
}

class LikedSongsRepositoryTest {

    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var fakeLikedSongDao: FakeLikedSongDao
    private lateinit var repository: LikedSongsRepositoryImpl

    private val track1 = Track(
        id = "track_1",
        title = "Starboy",
        artist = "The Weeknd",
        album = "Starboy",
        durationSeconds = 230,
        thumbnailUrl = "https://example.com/starboy.jpg"
    )

    private val track2 = Track(
        id = "track_2",
        title = "Blinding Lights",
        artist = "The Weeknd",
        album = "After Hours",
        durationSeconds = 200,
        thumbnailUrl = "https://example.com/blinding.jpg"
    )

    private val track3 = Track(
        id = "track_3",
        title = "Anti-Hero",
        artist = "Taylor Swift",
        album = "Midnights",
        durationSeconds = 201,
        thumbnailUrl = "https://example.com/antihero.jpg"
    )

    @Before
    fun setup() {
        fakeTrackDao = FakeTrackDao()
        fakeLikedSongDao = FakeLikedSongDao(fakeTrackDao)
        repository = LikedSongsRepositoryImpl(fakeTrackDao, fakeLikedSongDao)
    }

    @Test
    fun testLikeTrackPersistsTrackAndLike() = runTest {
        assertFalse(repository.isLiked("track_1").first())

        repository.likeTrack(track1)

        assertTrue(repository.isLiked("track_1").first())
        assertEquals(track1.title, fakeTrackDao.getTrackById("track_1")?.title)
        assertEquals(1, repository.getLikedCount().first())
    }

    @Test
    fun testUnlikeTrackRemovesLike() = runTest {
        repository.likeTrack(track1)
        assertTrue(repository.isLiked("track_1").first())

        repository.unlikeTrack(track1.id)
        assertFalse(repository.isLiked("track_1").first())
        assertEquals(0, repository.getLikedCount().first())
    }

    @Test
    fun testToggleLike() = runTest {
        val result1 = repository.toggleLike(track1)
        assertTrue("First toggle should like track", result1)
        assertTrue(repository.isLiked(track1.id).first())

        val result2 = repository.toggleLike(track1)
        assertFalse("Second toggle should unlike track", result2)
        assertFalse(repository.isLiked(track1.id).first())
    }

    @Test
    fun testSortingAlphabeticalAndByArtist() = runTest {
        repository.likeTrack(track1) // Starboy - The Weeknd
        repository.likeTrack(track2) // Blinding Lights - The Weeknd
        repository.likeTrack(track3) // Anti-Hero - Taylor Swift

        val alphabetical = repository.getLikedSongs(LikedSortOrder.TITLE).first()
        assertEquals("Anti-Hero", alphabetical[0].title)
        assertEquals("Blinding Lights", alphabetical[1].title)
        assertEquals("Starboy", alphabetical[2].title)

        val byArtist = repository.getLikedSongs(LikedSortOrder.ARTIST).first()
        assertEquals("Taylor Swift", byArtist[0].artist)
        assertEquals("The Weeknd", byArtist[1].artist)
    }

    @Test
    fun testSearchLikedSongs() = runTest {
        repository.likeTrack(track1)
        repository.likeTrack(track2)
        repository.likeTrack(track3)

        val weekndResults = repository.searchLikedSongs("weeknd").first()
        assertEquals(2, weekndResults.size)

        val antiHeroResults = repository.searchLikedSongs("anti").first()
        assertEquals(1, antiHeroResults.size)
        assertEquals("Anti-Hero", antiHeroResults[0].title)

        val emptyResults = repository.searchLikedSongs("nonexistent").first()
        assertEquals(0, emptyResults.size)
    }
}
