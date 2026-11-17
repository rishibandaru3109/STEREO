package com.nova.music.playback

import androidx.media3.common.Player
import com.nova.music.domain.model.PlaybackState
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.playback.queue.QueueSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class FakePlayerController : NovaPlayerController {
    val state = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = state

    val qSnapshot = MutableStateFlow(QueueSnapshot())
    override val queueSnapshot: StateFlow<QueueSnapshot> = qSnapshot

    var playCalled = false
    var pauseCalled = false
    var nextCalled = false
    var prevCalled = false
    var lastQueueSet: List<Track> = emptyList()
    var lastStartIndex: Int = 0

    val queue: List<Track> get() = lastQueueSet
    val currentTrack: Track? get() = state.value.currentTrack
    val isPlaying: Boolean get() = playCalled || state.value.status == com.nova.music.domain.model.PlayerStatus.PLAYING

    override fun playTrack(track: Track) {
        setQueue(listOf(track), 0)
    }

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        lastQueueSet = tracks
        lastStartIndex = startIndex
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            state.value = state.value.copy(
                currentTrack = tracks[startIndex],
                status = com.nova.music.domain.model.PlayerStatus.PLAYING
            )
        }
    }
    override fun playNext(track: Track) {}
    override fun addToQueue(track: Track) {}
    override fun removeFromQueue(index: Int) {}
    override fun moveTrack(fromIndex: Int, toIndex: Int) {}
    override fun jumpToQueueIndex(index: Int) {}
    override fun clearUpcoming() {}
    override fun undoQueueChange(): Boolean = false
    override fun play() { playCalled = true }
    override fun pause() { pauseCalled = true }
    override fun togglePlayPause() {}
    override fun seekTo(positionMs: Long) {}
    override fun next() { nextCalled = true }
    override fun previous() { prevCalled = true }
    override fun setRepeatMode(repeatMode: RepeatMode) {}
    override fun toggleShuffle() {}
    override fun release() {}
}

class NovaPlaybackUnitTest {

    private fun createDummyPlayer(): Player {
        return Proxy.newProxyInstance(
            Player::class.java.classLoader,
            arrayOf(Player::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Float::class.javaPrimitiveType -> 0f
                else -> null
            }
        } as Player
    }

    @Test
    fun testForwardingPlayerDelegatesToPlayerController() {
        val dummyPlayer = createDummyPlayer()
        val fakeController = FakePlayerController()
        val forwardingPlayer = NovaForwardingPlayer(dummyPlayer, fakeController)

        forwardingPlayer.play()
        assertTrue("play should delegate to controller", fakeController.playCalled)

        forwardingPlayer.pause()
        assertTrue("pause should delegate to controller", fakeController.pauseCalled)

        forwardingPlayer.seekToNext()
        assertTrue("seekToNext should delegate to controller", fakeController.nextCalled)

        forwardingPlayer.seekToPrevious()
        assertTrue("seekToPrevious should delegate to controller", fakeController.prevCalled)
    }

    @Test
    fun testCommandAvailabilityWithQueue() {
        val dummyPlayer = createDummyPlayer()
        val fakeController = FakePlayerController()
        val forwardingPlayer = NovaForwardingPlayer(dummyPlayer, fakeController)

        // Empty queue
        assertFalse("Next command not available when queue empty", forwardingPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
        assertFalse("hasNextMediaItem false when queue empty", forwardingPlayer.hasNextMediaItem())

        // Queue with 1 track
        val track1 = Track(id = "1", title = "Track 1", artist = "Artist 1")
        fakeController.state.value = PlaybackState(queue = listOf(track1))
        assertTrue("Next command available when queue not empty", forwardingPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
        assertFalse("hasNextMediaItem false when only 1 track", forwardingPlayer.hasNextMediaItem())
        assertTrue("hasPreviousMediaItem true when track present", forwardingPlayer.hasPreviousMediaItem())

        // Queue with 2 tracks
        val track2 = Track(id = "2", title = "Track 2", artist = "Artist 2")
        fakeController.state.value = PlaybackState(queue = listOf(track1, track2))
        assertTrue("hasNextMediaItem true when multiple tracks", forwardingPlayer.hasNextMediaItem())
    }
}
