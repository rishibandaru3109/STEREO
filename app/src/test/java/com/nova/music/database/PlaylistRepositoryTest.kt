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

    override suspend fun getTopArtworks(playlistId: String): List<String> {
        return entriesFlow.value.filter { it.playlistId == playlistId }
            .sortedBy { it.position }
            .mapNotNull { fakeTrackDao.tracks[it.trackId]?.thumbnailUrl?.takeIf { u -> u.isNotBlank() } }
            .distinct()
            .take(4)
    }

    override fun getTopArtworksFlow(playlistId: String): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.filter { it.playlistId == playlistId }
                .sortedBy { it.position }
                .mapNotNull { fakeTrackDao.tracks[it.trackId]?.thumbnailUrl?.takeIf { u -> u.isNotBlank() } }
                .distinct()
                .take(4)
        }
    }

    override fun getTrackIdsFlow(playlistId: String): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.filter { it.playlistId == playlistId }
                .sortedBy { it.position }
                .map { it.trackId }
        }
    }

    override suspend fun getTrackIds(playlistId: String): List<String> {
        return entriesFlow.value.filter { it.playlistId == playlistId }
            .sortedBy { it.position }
            .map { it.trackId }
    }

    override suspend fun containsTrack(playlistId: String, trackId: String): Boolean {
        return entriesFlow.value.any { it.playlistId == playlistId && it.trackId == trackId }
    }

    override fun getPlaylistIdsContainingTrackFlow(trackId: String): Flow<List<String>> {
        return entriesFlow.map { list ->
            list.filter { it.trackId == trackId }.map { it.playlistId }.distinct()
        }
    }

    override suspend fun getPlaylistIdsContainingTrack(trackId: String): List<String> {
        return entriesFlow.value.filter { it.trackId == trackId }.map { it.playlistId }.distinct()
    }

    override suspend fun removeTracksFromPlaylist(playlistId: String, trackIds: List<String>) {
        val removeSet = trackIds.toSet()
        entriesFlow.value = entriesFlow.value.filterNot { it.playlistId == playlistId && it.trackId in removeSet }
    }

    override suspend fun updatePlaylistDetails(playlistId: String, name: String, description: String?, coverArtworkUrl: String?, timestamp: Long) {
        playlistsFlow.value[playlistId]?.let {
            insertPlaylist(it.copy(name = name, description = description, coverArtworkUrl = coverArtworkUrl, updatedAt = timestamp))
        }
    }

    override suspend fun updatePlaylistCover(playlistId: String, coverArtworkUrl: String?, timestamp: Long) {
        playlistsFlow.value[playlistId]?.let {
            insertPlaylist(it.copy(coverArtworkUrl = coverArtworkUrl, updatedAt = timestamp))
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

    @Test
    fun testDuplicatePrevention() = runTest {
        val playlistId = repository.createPlaylist("No Duplicates")
        val addedFirst = repository.addTrackToPlaylist(playlistId, trackA, allowDuplicates = false)
        assertTrue(addedFirst)

        val addedDuplicate = repository.addTrackToPlaylist(playlistId, trackA, allowDuplicates = false)
        assertFalse(addedDuplicate)

        val tracks = repository.getPlaylistTracks(playlistId).first()
        assertEquals(1, tracks.size)

        // Adding with allowDuplicates = true should succeed
        val addedExplicitDup = repository.addTrackToPlaylist(playlistId, trackA, allowDuplicates = true)
        assertTrue(addedExplicitDup)
        val tracksAfter = repository.getPlaylistTracks(playlistId).first()
        assertEquals(2, tracksAfter.size)
    }

    @Test
    fun testMoveTracksBetweenPlaylists() = runTest {
        val p1 = repository.createPlaylist("Playlist 1")
        val p2 = repository.createPlaylist("Playlist 2")

        repository.addTracksToPlaylist(p1, listOf(trackA, trackB, trackC))
        repository.moveTracksBetweenPlaylists(p1, p2, listOf(trackA.id, trackB.id))

        val p1Tracks = repository.getPlaylistTracks(p1).first()
        val p2Tracks = repository.getPlaylistTracks(p2).first()

        assertEquals(1, p1Tracks.size)
        assertEquals("Song C", p1Tracks[0].title)

        assertEquals(2, p2Tracks.size)
        assertEquals("Song A", p2Tracks[0].title)
        assertEquals("Song B", p2Tracks[1].title)
    }

    @Test
    fun testCopyTracksToPlaylist() = runTest {
        val p1 = repository.createPlaylist("Source")
        val p2 = repository.createPlaylist("Destination")

        repository.addTracksToPlaylist(p1, listOf(trackA, trackB))
        repository.copyTracksToPlaylist(p2, listOf(trackA.id, trackB.id))

        val p1Tracks = repository.getPlaylistTracks(p1).first()
        val p2Tracks = repository.getPlaylistTracks(p2).first()

        assertEquals(2, p1Tracks.size)
        assertEquals(2, p2Tracks.size)
        assertEquals("Song A", p2Tracks[0].title)
        assertEquals("Song B", p2Tracks[1].title)
    }

    @Test
    fun testDuplicatePlaylist() = runTest {
        val originalId = repository.createPlaylist("Original", "A description")
        repository.addTracksToPlaylist(originalId, listOf(trackA, trackB))

        val clonedId = repository.duplicatePlaylist(originalId)
        assertTrue(clonedId.isNotBlank())

        val cloned = repository.getPlaylist(clonedId).first()
        assertNotNull(cloned)
        assertEquals("Original (Copy)", cloned?.name)
        assertEquals(2, cloned?.trackCount)

        val clonedTracks = repository.getPlaylistTracks(clonedId).first()
        assertEquals(2, clonedTracks.size)
        assertEquals("Song A", clonedTracks[0].title)
        assertEquals("Song B", clonedTracks[1].title)
    }

    @Test
    fun testReorderAllTracks() = runTest {
        val playlistId = repository.createPlaylist("Reorder Test")
        repository.addTracksToPlaylist(playlistId, listOf(trackA, trackB, trackC))

        // Reverse the order: C, B, A
        repository.reorderAllTracks(playlistId, listOf(trackC.id, trackB.id, trackA.id))

        val tracks = repository.getPlaylistTracks(playlistId).first()
        assertEquals(3, tracks.size)
        assertEquals("Song C", tracks[0].title)
        assertEquals("Song B", tracks[1].title)
        assertEquals("Song A", tracks[2].title)
    }

    @Test
    fun testTrackIdsAndContainingPlaylists() = runTest {
        val p1 = repository.createPlaylist("P1")
        val p2 = repository.createPlaylist("P2")

        repository.addTrackToPlaylist(p1, trackA)
        repository.addTrackToPlaylist(p2, trackA)
        repository.addTrackToPlaylist(p2, trackB)

        val p1TrackIds = repository.getPlaylistTrackIds(p1).first()
        assertEquals(setOf(trackA.id), p1TrackIds)

        val containingA = repository.getPlaylistIdsContainingTrack(trackA.id).first()
        assertEquals(setOf(p1, p2), containingA)

        val containingB = repository.getPlaylistIdsContainingTrack(trackB.id).first()
        assertEquals(setOf(p2), containingB)
    }
}
