package com.nova.music.database

import com.nova.music.core.database.dao.HistoryDao
import com.nova.music.core.database.entity.ListeningHistoryEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.data.repository.HistoryRepositoryImpl
import com.nova.music.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeHistoryDao(private val fakeTrackDao: FakeTrackDao) : HistoryDao {
    private val historyEvents = MutableStateFlow<List<ListeningHistoryEntity>>(emptyList())
    private var idCounter = 1L

    override suspend fun insertHistory(history: ListeningHistoryEntity): Long {
        val id = idCounter++
        val eventWithId = history.copy(id = id)
        historyEvents.value = historyEvents.value + eventWithId
        return id
    }

    override fun getRecentUniqueTracks(limit: Int): Flow<List<TrackEntity>> {
        return historyEvents.map { events ->
            events.sortedByDescending { it.playedAt }
                .map { it.trackId }
                .distinct()
                .take(limit)
                .mapNotNull { fakeTrackDao.tracks[it] }
        }
    }

    override fun getFullHistory(limit: Int): Flow<List<TrackEntity>> {
        return historyEvents.map { events ->
            events.sortedByDescending { it.playedAt }
                .take(limit)
                .mapNotNull { fakeTrackDao.tracks[it.trackId] }
        }
    }

    override fun getRawHistory(limit: Int): Flow<List<ListeningHistoryEntity>> {
        return historyEvents.map { it.sortedByDescending { e -> e.playedAt }.take(limit) }
    }

    override suspend fun getRawHistoryList(limit: Int): List<ListeningHistoryEntity> {
        return historyEvents.value.sortedByDescending { it.playedAt }.take(limit)
    }

    override suspend fun removeHistoryForTrack(trackId: String) {
        historyEvents.value = historyEvents.value.filter { it.trackId != trackId }
    }

    override suspend fun clearHistory() {
        historyEvents.value = emptyList()
    }

    fun getAllEvents(): List<ListeningHistoryEntity> = historyEvents.value
}

class HistoryRepositoryTest {

    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var fakeHistoryDao: FakeHistoryDao
    private lateinit var repository: HistoryRepositoryImpl

    private val track1 = Track(id = "h1", title = "Midnight City", artist = "M83", durationSeconds = 240)
    private val track2 = Track(id = "h2", title = "Wait", artist = "M83", durationSeconds = 230)

    @Before
    fun setup() {
        fakeTrackDao = FakeTrackDao()
        fakeHistoryDao = FakeHistoryDao(fakeTrackDao)
        repository = HistoryRepositoryImpl(fakeHistoryDao, fakeTrackDao)
    }

    @Test
    fun testRecordPlaybackDebouncesShortListen() = runTest {
        // Less than 3 seconds listen on a 240s song should be ignored
        repository.recordPlayback(
            track = track1,
            durationMs = 1500L,
            totalDurationMs = 240000L,
            isSkipped = true
        )

        val recent = repository.getRecentTracks().first()
        assertTrue("Short skipped listen under 3s should not be recorded", recent.isEmpty())
    }

    @Test
    fun testRecordPlaybackSavesMeaningfulListen() = runTest {
        repository.recordPlayback(
            track = track1,
            durationMs = 45000L,
            totalDurationMs = 240000L,
            isSkipped = false,
            playbackContext = "search"
        )

        val recent = repository.getRecentTracks().first()
        assertEquals(1, recent.size)
        assertEquals("Midnight City", recent[0].title)

        val events = fakeHistoryDao.getAllEvents()
        assertEquals(1, events.size)
        assertEquals("search", events[0].playbackContext)
    }

    @Test
    fun testClearHistory() = runTest {
        repository.recordPlayback(track1, 10000L, 240000L, false)
        repository.recordPlayback(track2, 15000L, 230000L, false)
        assertEquals(2, repository.getRecentTracks().first().size)

        repository.clearHistory()
        assertEquals(0, repository.getRecentTracks().first().size)
    }
}
