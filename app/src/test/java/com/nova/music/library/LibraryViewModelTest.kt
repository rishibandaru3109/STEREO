package com.nova.music.library

import com.nova.music.database.FakeHistoryDao
import com.nova.music.database.FakeLikedSongDao
import com.nova.music.database.FakePlaylistDao
import com.nova.music.database.FakeTrackDao
import com.nova.music.data.repository.HistoryRepositoryImpl
import com.nova.music.data.repository.LikedSongsRepositoryImpl
import com.nova.music.data.repository.PlaylistRepositoryImpl
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.LikedSortOrder
import com.nova.music.feature.library.LibraryViewModel
import com.nova.music.playback.FakePlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var fakeLikedSongDao: FakeLikedSongDao
    private lateinit var fakePlaylistDao: FakePlaylistDao
    private lateinit var fakeHistoryDao: FakeHistoryDao
    private lateinit var fakePlayerController: FakePlayerController

    private lateinit var likedSongsRepo: LikedSongsRepositoryImpl
    private lateinit var playlistRepo: PlaylistRepositoryImpl
    private lateinit var historyRepo: HistoryRepositoryImpl

    private lateinit var viewModel: LibraryViewModel

    private val track1 = Track(id = "1", title = "After Hours", artist = "The Weeknd")
    private val track2 = Track(id = "2", title = "In Your Eyes", artist = "The Weeknd")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeTrackDao = FakeTrackDao()
        fakeLikedSongDao = FakeLikedSongDao(fakeTrackDao)
        fakePlaylistDao = FakePlaylistDao(fakeTrackDao)
        fakeHistoryDao = FakeHistoryDao(fakeTrackDao)
        fakePlayerController = FakePlayerController()

        likedSongsRepo = LikedSongsRepositoryImpl(fakeTrackDao, fakeLikedSongDao)
        playlistRepo = PlaylistRepositoryImpl(fakePlaylistDao, fakeTrackDao)
        historyRepo = HistoryRepositoryImpl(fakeHistoryDao, fakeTrackDao)

        viewModel = LibraryViewModel(
            likedSongsRepository = likedSongsRepo,
            playlistRepository = playlistRepo,
            historyRepository = historyRepo,
            playerController = fakePlayerController
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest {
        testScheduler.advanceUntilIdle()
        assertEquals(0, viewModel.likedCount.value)
        assertTrue(viewModel.playlists.value.isEmpty())
        assertTrue(viewModel.recentTracks.value.isEmpty())
    }

    @Test
    fun testToggleLikeUpdatesCountAndList() = runTest {
        viewModel.toggleLike(track1)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.likedCount.value)
        val songs = viewModel.likedSongs.value
        assertEquals(1, songs.size)
        assertEquals("After Hours", songs[0].title)

        // Untoggle
        viewModel.toggleLike(track1)
        testScheduler.advanceUntilIdle()

        assertEquals(0, viewModel.likedCount.value)
        assertTrue(viewModel.likedSongs.value.isEmpty())
    }

    @Test
    fun testSortingAndSearchQueries() = runTest {
        viewModel.toggleLike(track1)
        viewModel.toggleLike(track2)
        testScheduler.advanceUntilIdle()

        viewModel.setLikedSortOrder(LikedSortOrder.TITLE)
        assertEquals(LikedSortOrder.TITLE, viewModel.likedSortOrder.value)

        viewModel.setLikedSearchQuery("After")
        assertEquals("After", viewModel.likedSearchQuery.value)
    }

    @Test
    fun testPlaylistLifecycle() = runTest {
        var createdId: String? = null
        viewModel.createPlaylist("Midnight Drive", "Late synthwave") { id ->
            createdId = id
        }
        testScheduler.advanceUntilIdle()

        assertNotNull(createdId)
        val playlists = viewModel.playlists.value
        assertEquals(1, playlists.size)
        assertEquals("Midnight Drive", playlists[0].name)

        // Select playlist
        viewModel.selectPlaylist(createdId)
        testScheduler.advanceUntilIdle()
        assertEquals(createdId, viewModel.selectedPlaylistId.value)

        // Add track to playlist
        viewModel.addTrackToPlaylist(createdId!!, track1)
        testScheduler.advanceUntilIdle()

        val selectedWithTracks = viewModel.selectedPlaylistWithTracks.value
        assertNotNull(selectedWithTracks)
        assertEquals(1, selectedWithTracks?.tracks?.size)
        assertEquals("After Hours", selectedWithTracks?.tracks?.get(0)?.title)

        // Rename
        viewModel.renamePlaylist(createdId!!, "Night Drive")
        testScheduler.advanceUntilIdle()
        assertEquals("Night Drive", viewModel.playlists.value[0].name)

        // Delete
        viewModel.deletePlaylist(createdId!!)
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.playlists.value.isEmpty())
        assertNull(viewModel.selectedPlaylistId.value)
    }
}
