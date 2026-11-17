package com.nova.music.playback.queue

import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.playback.shuffle.BalancedShuffle
import com.nova.music.playback.shuffle.ShuffleEngine
import com.nova.music.playback.shuffle.ShuffleMode
import com.nova.music.playback.shuffle.StandardShuffle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.ArrayDeque

class QueueManager {

    private data class QueueBackup(
        val originalQueue: List<Track>,
        val activeQueue: List<Track>,
        val currentIndex: Int,
        val isShuffle: Boolean,
        val shuffleMode: ShuffleMode
    )

    private var originalQueue: MutableList<Track> = mutableListOf()
    private var activeQueue: MutableList<Track> = mutableListOf()
    private var userQueue: MutableList<Track> = mutableListOf()
    private var currentIndex: Int = -1
    private var isShuffle: Boolean = false
    private var shuffleMode: ShuffleMode = ShuffleMode.OFF

    private val undoStack = ArrayDeque<QueueBackup>(10)

    private val _snapshot = MutableStateFlow(QueueSnapshot())
    val snapshot: StateFlow<QueueSnapshot> = _snapshot.asStateFlow()

    private fun pushUndo() {
        if (undoStack.size >= 10) {
            undoStack.removeFirst()
        }
        undoStack.addLast(
            QueueBackup(
                originalQueue = ArrayList(originalQueue),
                activeQueue = ArrayList(activeQueue),
                currentIndex = currentIndex,
                isShuffle = isShuffle,
                shuffleMode = shuffleMode
            )
        )
    }

    private fun emitState() {
        val currTrack = if (currentIndex in activeQueue.indices) activeQueue[currentIndex] else null
        _snapshot.update {
            QueueSnapshot(
                currentTrack = currTrack,
                activeQueue = ArrayList(activeQueue),
                currentIndex = currentIndex,
                isShuffle = isShuffle,
                shuffleMode = shuffleMode,
                userQueueCount = userQueue.size
            )
        }
    }

    @Synchronized
    fun setQueue(
        tracks: List<Track>,
        startIndex: Int = 0,
        isShuffle: Boolean = false,
        shuffleMode: ShuffleMode = ShuffleMode.STANDARD
    ) {
        pushUndo()
        originalQueue = tracks.toMutableList()
        this.isShuffle = isShuffle
        this.shuffleMode = if (isShuffle) shuffleMode else ShuffleMode.OFF

        if (isShuffle && tracks.isNotEmpty()) {
            val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
            val selectedTrack = tracks[safeIndex]
            val engine: ShuffleEngine = when (shuffleMode) {
                ShuffleMode.BALANCED -> BalancedShuffle()
                else -> StandardShuffle()
            }
            activeQueue = engine.shuffle(tracks, selectedTrack).toMutableList()
            currentIndex = 0
        } else {
            activeQueue = tracks.toMutableList()
            currentIndex = if (tracks.isNotEmpty()) startIndex.coerceIn(0, tracks.size - 1) else -1
        }
        userQueue.clear()
        emitState()
    }

    @Synchronized
    fun playNow(track: Track): Track {
        pushUndo()
        val existingIndex = activeQueue.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            currentIndex = existingIndex
        } else {
            val insertIndex = if (currentIndex >= 0 && currentIndex < activeQueue.size) currentIndex + 1 else activeQueue.size
            activeQueue.add(insertIndex, track)
            originalQueue.add(track)
            currentIndex = insertIndex
        }
        emitState()
        return track
    }

    @Synchronized
    fun playNext(track: Track) {
        pushUndo()
        if (activeQueue.isEmpty()) {
            setQueue(listOf(track), 0, isShuffle, shuffleMode)
            return
        }
        val insertIndex = (currentIndex + 1).coerceIn(0, activeQueue.size)
        activeQueue.add(insertIndex, track)
        userQueue.add(track)
        if (!originalQueue.any { it.id == track.id }) {
            originalQueue.add(track)
        }
        emitState()
    }

    @Synchronized
    fun playNext(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        pushUndo()
        if (activeQueue.isEmpty()) {
            setQueue(tracks, 0, isShuffle, shuffleMode)
            return
        }
        val insertIndex = (currentIndex + 1).coerceIn(0, activeQueue.size)
        activeQueue.addAll(insertIndex, tracks)
        userQueue.addAll(tracks)
        tracks.forEach { track ->
            if (!originalQueue.any { it.id == track.id }) {
                originalQueue.add(track)
            }
        }
        emitState()
    }

    @Synchronized
    fun addToQueue(track: Track) {
        pushUndo()
        if (activeQueue.isEmpty()) {
            setQueue(listOf(track), 0, isShuffle, shuffleMode)
            return
        }
        activeQueue.add(track)
        if (!originalQueue.any { it.id == track.id }) {
            originalQueue.add(track)
        }
        emitState()
    }

    @Synchronized
    fun addToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        pushUndo()
        if (activeQueue.isEmpty()) {
            setQueue(tracks, 0, isShuffle, shuffleMode)
            return
        }
        activeQueue.addAll(tracks)
        tracks.forEach { track ->
            if (!originalQueue.any { it.id == track.id }) {
                originalQueue.add(track)
            }
        }
        emitState()
    }

    @Synchronized
    fun removeFromQueue(index: Int): Track? {
        if (index !in activeQueue.indices) return null
        pushUndo()
        val removed = activeQueue.removeAt(index)
        userQueue.removeAll { it.id == removed.id }

        if (index < currentIndex) {
            currentIndex--
        } else if (index == currentIndex) {
            if (currentIndex >= activeQueue.size) {
                currentIndex = activeQueue.size - 1
            }
        }
        emitState()
        return removed
    }

    @Synchronized
    fun moveTrack(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in activeQueue.indices || toIndex !in activeQueue.indices || fromIndex == toIndex) return
        pushUndo()
        val currentTrack = if (currentIndex in activeQueue.indices) activeQueue[currentIndex] else null
        val item = activeQueue.removeAt(fromIndex)
        activeQueue.add(toIndex, item)

        if (currentTrack != null) {
            currentIndex = activeQueue.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
        }
        emitState()
    }

    @Synchronized
    fun clearUpcoming() {
        if (currentIndex !in activeQueue.indices) return
        pushUndo()
        val kept = activeQueue.take(currentIndex + 1).toMutableList()
        activeQueue = kept
        userQueue.clear()
        emitState()
    }

    @Synchronized
    fun clearQueue() {
        pushUndo()
        val current = if (currentIndex in activeQueue.indices) activeQueue[currentIndex] else null
        if (current != null) {
            activeQueue = mutableListOf(current)
            originalQueue = mutableListOf(current)
            currentIndex = 0
        } else {
            activeQueue.clear()
            originalQueue.clear()
            currentIndex = -1
        }
        userQueue.clear()
        emitState()
    }

    @Synchronized
    fun next(repeatMode: RepeatMode = RepeatMode.OFF): Track? {
        if (activeQueue.isEmpty()) return null

        if (repeatMode == RepeatMode.ONE && currentIndex in activeQueue.indices) {
            return activeQueue[currentIndex]
        }

        if (currentIndex < activeQueue.size - 1) {
            currentIndex++
            emitState()
            return activeQueue[currentIndex]
        }

        if (repeatMode == RepeatMode.ALL) {
            currentIndex = 0
            emitState()
            return activeQueue[0]
        }

        return null
    }

    @Synchronized
    fun previous(repeatMode: RepeatMode = RepeatMode.OFF): Track? {
        if (activeQueue.isEmpty()) return null

        if (currentIndex > 0) {
            currentIndex--
            emitState()
            return activeQueue[currentIndex]
        }

        if (repeatMode == RepeatMode.ALL) {
            currentIndex = activeQueue.size - 1
            emitState()
            return activeQueue[currentIndex]
        }

        return if (currentIndex in activeQueue.indices) activeQueue[currentIndex] else null
    }

    @Synchronized
    fun jumpToIndex(index: Int): Track? {
        if (index in activeQueue.indices) {
            currentIndex = index
            emitState()
            return activeQueue[currentIndex]
        }
        return null
    }

    @Synchronized
    fun toggleShuffle(
        enable: Boolean,
        mode: ShuffleMode = ShuffleMode.STANDARD,
        seed: Long? = null
    ) {
        if (isShuffle == enable && (enable && shuffleMode == mode)) return
        pushUndo()
        isShuffle = enable
        shuffleMode = if (enable) mode else ShuffleMode.OFF

        val currentTrack = if (currentIndex in activeQueue.indices) activeQueue[currentIndex] else null

        if (enable) {
            if (activeQueue.isNotEmpty()) {
                val engine: ShuffleEngine = when (mode) {
                    ShuffleMode.BALANCED -> BalancedShuffle()
                    else -> StandardShuffle()
                }
                activeQueue = engine.shuffle(activeQueue, currentTrack, seed).toMutableList()
                currentIndex = 0
            }
        } else {
            // Restore original order and preserve current track pointer
            activeQueue = ArrayList(originalQueue)
            if (currentTrack != null) {
                val origIndex = activeQueue.indexOfFirst { it.id == currentTrack.id }
                currentIndex = if (origIndex >= 0) origIndex else 0
            }
        }
        emitState()
    }

    @Synchronized
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val backup = undoStack.removeLast()
        originalQueue = ArrayList(backup.originalQueue)
        activeQueue = ArrayList(backup.activeQueue)
        currentIndex = backup.currentIndex
        isShuffle = backup.isShuffle
        shuffleMode = backup.shuffleMode
        emitState()
        return true
    }

    @Synchronized
    fun restoreState(
        savedOriginal: List<Track>,
        savedActive: List<Track>,
        savedIndex: Int,
        savedShuffle: Boolean,
        savedShuffleMode: ShuffleMode = ShuffleMode.OFF
    ) {
        originalQueue = savedOriginal.toMutableList()
        activeQueue = savedActive.toMutableList()
        currentIndex = if (savedActive.isNotEmpty()) savedIndex.coerceIn(0, savedActive.size - 1) else -1
        isShuffle = savedShuffle
        shuffleMode = savedShuffleMode
        userQueue.clear()
        emitState()
    }

    fun getCurrentTrack(): Track? =
        if (currentIndex in activeQueue.indices) activeQueue[currentIndex] else null

    fun getOriginalQueue(): List<Track> = ArrayList(originalQueue)

    fun getActiveQueue(): List<Track> = ArrayList(activeQueue)

    fun getCurrentIndex(): Int = currentIndex

    fun isShuffleEnabled(): Boolean = isShuffle

    fun getShuffleMode(): ShuffleMode = shuffleMode
}
