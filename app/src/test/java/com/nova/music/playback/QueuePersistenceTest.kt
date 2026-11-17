package com.nova.music.playback

import com.nova.music.core.database.dao.QueueDao
import com.nova.music.core.database.entity.QueueStateEntity
import com.nova.music.data.repository.QueuePersistenceRepository
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.playback.queue.QueueManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeQueueDao : QueueDao {
    private var state: QueueStateEntity? = null

    override suspend fun saveQueueState(queueState: QueueStateEntity) {
        state = queueState
    }

    override suspend fun getQueueState(queueId: String): QueueStateEntity? = state

    override suspend fun clearQueueState(queueId: String) {
        state = null
    }
}

class QueuePersistenceTest {

    private lateinit var fakeQueueDao: FakeQueueDao
    private lateinit var persistenceRepo: QueuePersistenceRepository

    private val track1 = Track(id = "q1", title = "Blinding Lights", artist = "The Weeknd")
    private val track2 = Track(id = "q2", title = "Save Your Tears", artist = "The Weeknd")

    @Before
    fun setup() {
        fakeQueueDao = FakeQueueDao()
        persistenceRepo = QueuePersistenceRepository(fakeQueueDao)
    }

    @Test
    fun testSerializationAndDeserialization() {
        val tracks = listOf(track1, track2)
        val json = persistenceRepo.serializeTracks(tracks)
        assertTrue(json.contains("Blinding Lights"))

        val deserialized = persistenceRepo.deserializeTracks(json)
        assertEquals(2, deserialized.size)
        assertEquals("Blinding Lights", deserialized[0].title)
        assertEquals("Save Your Tears", deserialized[1].title)
    }

    @Test
    fun testSaveAndRestoreQueue() = runTest {
        val queueManager = QueueManager()
        queueManager.setQueue(listOf(track1, track2), startIndex = 1)

        persistenceRepo.saveQueue(
            queueManager = queueManager,
            playbackPositionMs = 45000L,
            repeatMode = RepeatMode.ALL
        )

        val restored = persistenceRepo.restoreQueue()
        assertNotNull(restored)
        assertEquals(2, restored?.activeQueue?.size)
        assertEquals(1, restored?.currentIndex)
        assertEquals("q2", restored?.currentTrackId)
        assertEquals(45000L, restored?.playbackPositionMs)
        assertEquals(RepeatMode.ALL, restored?.repeatMode)
    }

    @Test
    fun testClearQueue() = runTest {
        val queueManager = QueueManager()
        queueManager.setQueue(listOf(track1))
        persistenceRepo.saveQueue(queueManager, 0L, RepeatMode.OFF)

        assertNotNull(persistenceRepo.restoreQueue())

        persistenceRepo.clearQueue()
        assertNull(persistenceRepo.restoreQueue())
    }
}
