package com.nova.music.database

import com.nova.music.core.database.dao.PlaylistDao
import com.nova.music.core.database.dao.PlaylistWithMetadata
import com.nova.music.core.database.entity.PlaylistEntity
import com.nova.music.core.database.entity.PlaylistEntryEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.data.repository.PlaylistRepositoryImpl
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakePlaylistDao(private val fakeTrackDao: FakeTrackDao) : PlaylistDao {
    private val playlistsFlow = MutableStateFlow<Map<String, PlaylistEntity>>(emptyMap())
    private val entriesFlow = MutableStateFlow<List<PlaylistEntryEntity>>(emptyList())
    private var entryIdCounter = 1L

    override suspend fun insertPlaylist(playlist: PlaylistEntity) {
        val current = playlistsFlow.value.toMutableMap()
        current[playlist.id] = playlist
        playlistsFlow.value = current
    }

    override suspend fun updatePlaylist(playlist: PlaylistEntity) {
        insertPlaylist(playlist)
    }

    override suspend fun deletePlaylist(playlistId: String) {
        val current = playlistsFlow.value.toMutableMap()
        current.remove(playlistId)
        playlistsFlow.value = current
        entriesFlow.value = entriesFlow.value.filter { it.playlistId != playlistId }
    }

    override fun getPlaylists(): Flow<List<PlaylistEntity>> {
        return playlistsFlow.map { it.values.toList() }
    }

    override fun getPlaylistsWithMetadata(): Flow<List<PlaylistWithMetadata>> {
        return playlistsFlow.map { map ->
            val allEntries = entriesFlow.value
            map.values.sortedWith(compareByDescending<PlaylistEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                .map { p ->
                    val pEntries = allEntries.filter { it.playlistId == p.id }.sortedBy { it.position }
                    val autoArt = pEntries.firstOrNull()?.let { fakeTrackDao.tracks[it.trackId]?.thumbnailUrl }
                    PlaylistWithMetadata(
                        id = p.id,
                        name = p.name,
                        description = p.description,
                        coverArtworkUrl = p.coverArtworkUrl,
                        createdAt = p.createdAt,
                        updatedAt = p.updatedAt,
                        isPinned = p.isPinned,
                        trackCount = pEntries.size,
                        autoArtworkUrl = autoArt
                    )
                }
        }
    }

    override suspend fun getPlaylistById(id: String): PlaylistEntity? = playlistsFlow.value[id]

    override fun getPlaylistFlow(id: String): Flow<PlaylistEntity?> {
        return playlistsFlow.map { it[id] }
    }

    override suspend fun insertEntry(entry: PlaylistEntryEntity): Long {
        val id = entryIdCounter++
        val entryWithId = entry.copy(id = id)
        entriesFlow.value = entriesFlow.value + entryWithId
        return id
    }

    override suspend fun insertEntries(entries: List<PlaylistEntryEntity>) {
        val newEntries = entries.map { it.copy(id = entryIdCounter++) }
        entriesFlow.value = entriesFlow.value + newEntries
    }

    override suspend fun deleteEntry(entryId: Long) {
        entriesFlow.value = entriesFlow.value.filter { it.id != entryId }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        entriesFlow.value = entriesFlow.value.filterNot { it.playlistId == playlistId && it.trackId == trackId }
    }

    override suspend fun clearEntriesForPlaylist(playlistId: String) {
        entriesFlow.value = entriesFlow.value.filter { it.playlistId != playlistId }
    }

    override fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>> {
        return entriesFlow.map { list ->
            list.filter { it.playlistId == playlistId }
                .sortedBy { it.position }
                .mapNotNull { fakeTrackDao.tracks[it.trackId] }
        }
    }

    override suspend fun getEntriesForPlaylist(playlistId: String): List<PlaylistEntryEntity> {
        return entriesFlow.value.filter { it.playlistId == playlistId }.sortedBy { it.position }
    }

    override suspend fun getMaxPosition(playlistId: String): Int {
        return entriesFlow.value.filter { it.playlistId == playlistId }.maxOfOrNull { it.position } ?: -1
    }

    override fun getPlaylistTrackCount(playlistId: String): Flow<Int> {
        return entriesFlow.map { list -> list.count { it.playlistId == playlistId } }
    }

    override suspend fun renamePlaylist(playlistId: String, name: String, timestamp: Long) {
        playlistsFlow.value[playlistId]?.let {
            insertPlaylist(it.copy(name = name, updatedAt = timestamp))
        }
    }

    override suspend fun updateEntryPosition(entryId: Long, newPosition: Int) {
        entriesFlow.value = entriesFlow.value.map {
            if (it.id == entryId) it.copy(position = newPosition) else it
        }
    }

    override suspend fun updatePlaylistTimestamp(playlistId: String, timestamp: Long) {
        playlistsFlow.value[playlistId]?.let {
            insertPlaylist(it.copy(updatedAt = timestamp))
        }
    }

    override suspend fun setPlaylistPinned(playlistId: String, isPinned: Boolean, timestamp: Long) {
        playlistsFlow.value[playlistId]?.let {
            insertPlaylist(it.copy(isPinned = isPinned, updatedAt = timestamp))
        }
    }
}

class PlaylistRepositoryTest {

    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var fakePlaylistDao: FakePlaylistDao
    private lateinit var repository: PlaylistRepositoryImpl

    private val trackA = Track(id = "track_a", title = "Song A", artist = "Artist A")
    private val trackB = Track(id = "track_b", title = "Song B", artist = "Artist B")
    private val trackC = Track(id = "track_c", title = "Song C", artist = "Artist C")

    @Before
    fun setup() {
        fakeTrackDao = FakeTrackDao()
        fakePlaylistDao = FakePlaylistDao(fakeTrackDao)
        repository = PlaylistRepositoryImpl(fakePlaylistDao, fakeTrackDao)
    }

    @Test
    fun testCreateAndGetPlaylist() = runTest {
        val playlistId = repository.createPlaylist("Chill Vibes", "Late night listening")
        val playlists = repository.getPlaylists().first()

        assertEquals(1, playlists.size)
        assertEquals("Chill Vibes", playlists[0].name)
        assertEquals("Late night listening", playlists[0].description)
        assertEquals(0, playlists[0].trackCount)
    }

    @Test
    fun testAddTracksAndTrackCount() = runTest {
        val playlistId = repository.createPlaylist("Favorites")
        repository.addTrackToPlaylist(playlistId, trackA)
        repository.addTrackToPlaylist(playlistId, trackB)

        val tracks = repository.getPlaylistTracks(playlistId).first()
        assertEquals(2, tracks.size)
        assertEquals("Song A", tracks[0].title)
        assertEquals("Song B", tracks[1].title)

        val playlist = repository.getPlaylist(playlistId).first()
        assertNotNull(playlist)
        assertEquals(2, playlist?.trackCount)
    }

    @Test
    fun testRemoveTrackFromPlaylist() = runTest {
        val playlistId = repository.createPlaylist("Rock")
        repository.addTrackToPlaylist(playlistId, trackA)
        repository.addTrackToPlaylist(playlistId, trackB)

        repository.removeTrackFromPlaylist(playlistId, trackA.id)

        val tracks = repository.getPlaylistTracks(playlistId).first()
        assertEquals(1, tracks.size)
        assertEquals("Song B", tracks[0].title)
    }

    @Test
    fun testReorderTracks() = runTest {
        val playlistId = repository.createPlaylist("Ordered List")
        repository.addTracksToPlaylist(playlistId, listOf(trackA, trackB, trackC))

        // Initial order: A, B, C
        var tracks = repository.getPlaylistTracks(playlistId).first()
        assertEquals("Song A", tracks[0].title)
        assertEquals("Song B", tracks[1].title)
        assertEquals("Song C", tracks[2].title)

        // Move index 0 (Song A) to index 2 -> Order becomes: B, C, A
        repository.reorderTracks(playlistId, fromPosition = 0, toPosition = 2)

        tracks = repository.getPlaylistTracks(playlistId).first()
        assertEquals(3, tracks.size)
        assertEquals("Song B", tracks[0].title)
        assertEquals("Song C", tracks[1].title)
        assertEquals("Song A", tracks[2].title)
    }

    @Test
    fun testRenameAndDeletePlaylist() = runTest {
        val playlistId = repository.createPlaylist("Old Name")
        repository.renamePlaylist(playlistId, "New Name")

        var playlist = repository.getPlaylist(playlistId).first()
        assertEquals("New Name", playlist?.name)

        repository.deletePlaylist(playlistId)
        playlist = repository.getPlaylist(playlistId).first()
        assertNull(playlist)

        val allPlaylists = repository.getPlaylists().first()
        assertTrue(allPlaylists.isEmpty())
    }

    @Test
    fun testPinPlaylist() = runTest {
        val playlistId = repository.createPlaylist("Pinned List")
        assertFalse(repository.getPlaylist(playlistId).first()?.isPinned ?: true)

        repository.setPlaylistPinned(playlistId, true)
        assertTrue(repository.getPlaylist(playlistId).first()?.isPinned ?: false)
    }
}
