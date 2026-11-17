package com.nova.music.feature.home

import com.nova.music.database.FakeLikedSongDao
import com.nova.music.database.FakeTrackDao
import com.nova.music.data.repository.LikedSongsRepositoryImpl
import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.HomeRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeHomeRepository : HomeRepository {
    var feedResult: Result<List<RecommendationSection>> = Result.success(emptyList())
    var lastFeedbackTrackId: String? = null
    var lastFeedbackType: String? = null

    override suspend fun getHomeFeed(forceRefresh: Boolean): Result<List<RecommendationSection>> {
        return feedResult
    }

    override suspend fun recordExplicitFeedback(trackId: String, feedbackType: String) {
        lastFeedbackTrackId = trackId
        lastFeedbackType = feedbackType
    }

    override suspend fun clearCache() {
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeHomeRepo: FakeHomeRepository
    private lateinit var fakePlayerController: FakePlayerController
    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var fakeLikedSongDao: FakeLikedSongDao
    private lateinit var likedSongsRepo: LikedSongsRepositoryImpl
    private lateinit var viewModel: HomeViewModel

    private val track1 = Track(id = "1", title = "Blinding Lights", artist = "The Weeknd")
    private val track2 = Track(id = "2", title = "Save Your Tears", artist = "The Weeknd")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeHomeRepo = FakeHomeRepository()
        fakePlayerController = FakePlayerController()
        fakeTrackDao = FakeTrackDao()
        fakeLikedSongDao = FakeLikedSongDao(fakeTrackDao)
        likedSongsRepo = LikedSongsRepositoryImpl(fakeTrackDao, fakeLikedSongDao)

        val quickPicks = RecommendationSection(
            type = SectionType.QUICK_PICKS,
            title = "Quick Picks",
            tracks = listOf(track1, track2)
        )
        fakeHomeRepo.feedResult = Result.success(listOf(quickPicks))

        viewModel = HomeViewModel(
            homeRepository = fakeHomeRepo,
            playerController = fakePlayerController,
            likedSongsRepository = likedSongsRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialFeedLoading() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.sections.size)
        assertEquals(SectionType.QUICK_PICKS, state.sections.first().type)
    }

    @Test
    fun testPlayTrackSetsQueueAndStartsPlayback() = runTest {
        advanceUntilIdle()
        viewModel.playTrack(track1, listOf(track1, track2))

        assertEquals(2, fakePlayerController.queue.size)
        assertEquals("1", fakePlayerController.currentTrack?.id)
        assertTrue(fakePlayerController.isPlaying)
    }

    @Test
    fun testPlayMixSetsQueueAndPlays() = runTest {
        advanceUntilIdle()
        val mix = RecommendationMix(
            id = "mix_1",
            title = "Weekend Mix",
            subtitle = "The Weeknd",
            tracks = listOf(track1, track2)
        )

        viewModel.playMix(mix)

        assertEquals(2, fakePlayerController.queue.size)
        assertEquals("1", fakePlayerController.currentTrack?.id)
        assertTrue(fakePlayerController.isPlaying)
    }

    @Test
    fun testNotInterestedRecordsFeedback() = runTest {
        advanceUntilIdle()
        viewModel.notInterested(track1)
        advanceUntilIdle()

        assertEquals("1", fakeHomeRepo.lastFeedbackTrackId)
        assertEquals("NOT_INTERESTED", fakeHomeRepo.lastFeedbackType)
    }
}
