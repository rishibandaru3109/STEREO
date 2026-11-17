package com.nova.music.database

import com.nova.music.core.database.dao.RecommendationDao
import com.nova.music.core.database.entity.ExplicitFeedbackEntity
import com.nova.music.core.database.entity.LikedSongEntity
import com.nova.music.core.database.entity.ListeningHistoryEntity
import com.nova.music.core.database.entity.RecommendationCacheEntity
import com.nova.music.core.database.entity.TrackEntity
import com.nova.music.data.repository.ListeningProfileRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRecommendationDao : RecommendationDao {
    val cache = mutableMapOf<String, RecommendationCacheEntity>()
    val feedbackList = MutableStateFlow<List<ExplicitFeedbackEntity>>(emptyList())

    override suspend fun cacheSection(cache: RecommendationCacheEntity) {
        this.cache[cache.sectionKey] = cache
    }

    override suspend fun getCachedSection(sectionKey: String): RecommendationCacheEntity? {
        return cache[sectionKey]
    }

    override fun getAllCachedSections(): Flow<List<RecommendationCacheEntity>> {
        return MutableStateFlow(cache.values.toList())
    }

    override suspend fun clearSectionCache(sectionKey: String) {
        cache.remove(sectionKey)
    }

    override suspend fun clearExpiredCache(currentTime: Long) {
        cache.entries.removeIf { it.value.expiresAt < currentTime }
    }

    override suspend fun clearAllCache() {
        cache.clear()
    }

    override suspend fun insertFeedback(feedback: ExplicitFeedbackEntity) {
        feedbackList.value = feedbackList.value.filter { it.trackId != feedback.trackId } + feedback
    }

    override fun getFeedbackFlow(): Flow<List<ExplicitFeedbackEntity>> = feedbackList

    override suspend fun getExcludedTrackIds(): List<String> {
        return feedbackList.value.map { it.trackId }
    }

    override suspend fun removeFeedback(trackId: String) {
        feedbackList.value = feedbackList.value.filter { it.trackId != trackId }
    }
}

class ListeningProfileRepositoryTest {

    private lateinit var fakeTrackDao: FakeTrackDao
    private lateinit var fakeHistoryDao: FakeHistoryDao
    private lateinit var fakeLikedSongDao: FakeLikedSongDao
    private lateinit var fakeRecommendationDao: FakeRecommendationDao
    private lateinit var repository: ListeningProfileRepositoryImpl

    @Before
    fun setUp() {
        fakeTrackDao = FakeTrackDao()
        fakeHistoryDao = FakeHistoryDao(fakeTrackDao)
        fakeLikedSongDao = FakeLikedSongDao(fakeTrackDao)
        fakeRecommendationDao = FakeRecommendationDao()

        repository = ListeningProfileRepositoryImpl(
            historyDao = fakeHistoryDao,
            likedSongDao = fakeLikedSongDao,
            trackDao = fakeTrackDao,
            recommendationDao = fakeRecommendationDao
        )
    }

    @Test
    fun testColdStartDetection() = runTest {
        val initialProfile = repository.getListeningProfile()
        assertTrue("Empty profile should be cold start", initialProfile.isColdStart)
        assertEquals(0, initialProfile.totalListens)

        // Adding 1 liked track breaks cold start
        fakeTrackDao.upsert(TrackEntity("t1", "Track 1", "Artist 1"))
        fakeLikedSongDao.insertLike(LikedSongEntity(trackId = "t1"))

        val updatedProfile = repository.getListeningProfile()
        assertFalse("Profile with liked song should not be cold start", updatedProfile.isColdStart)
    }

    @Test
    fun testAffinityScoringWithCompletionsAndSkips() = runTest {
        val t1 = TrackEntity("t1", "Good Track", "Artist Alpha")
        val t2 = TrackEntity("t2", "Skipped Track", "Artist Beta")
        fakeTrackDao.upsert(t1)
        fakeTrackDao.upsert(t2)

        val now = System.currentTimeMillis()

        // 3 completed listens on t1
        repeat(3) {
            fakeHistoryDao.insertHistory(
                ListeningHistoryEntity(
                    trackId = "t1",
                    playedAt = now - 1000 * 60,
                    listeningDurationMs = 120_000L,
                    completionRatio = 0.95f,
                    isSkipped = false
                )
            )
        }

        // 2 skipped listens on t2
        repeat(2) {
            fakeHistoryDao.insertHistory(
                ListeningHistoryEntity(
                    trackId = "t2",
                    playedAt = now - 1000 * 60,
                    listeningDurationMs = 5_000L,
                    completionRatio = 0.05f,
                    isSkipped = true
                )
            )
        }

        val profile = repository.getListeningProfile()

        assertEquals(5, profile.totalListens)
        val alphaAffinity = profile.topArtists.find { it.artistName == "Artist Alpha" }
        val betaAffinity = profile.topArtists.find { it.artistName == "Artist Beta" }

        assertTrue(alphaAffinity != null && alphaAffinity.score > 0)
        // Beta score is penalized
        assertTrue(betaAffinity == null || betaAffinity.score <= 0.0)

        // Alpha should be top artist
        assertEquals("Artist Alpha", profile.topArtists.first().artistName)
    }

    @Test
    fun testExplicitFeedbackExclusion() = runTest {
        repository.recordExplicitFeedback("annoying_track", "NOT_INTERESTED")
        val excluded = repository.getExcludedTrackIds()
        assertTrue(excluded.contains("annoying_track"))
    }
}
