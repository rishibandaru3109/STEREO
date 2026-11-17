package com.nova.music.data.repository

import com.nova.music.core.database.dao.QueueDao
import com.nova.music.core.database.entity.QueueStateEntity
import com.nova.music.domain.model.RepeatMode
import com.nova.music.domain.model.Track
import com.nova.music.playback.queue.QueueManager
import com.nova.music.playback.shuffle.ShuffleMode
import org.json.JSONArray
import org.json.JSONObject

data class RestoredQueueState(
    val originalQueue: List<Track>,
    val activeQueue: List<Track>,
    val currentIndex: Int,
    val currentTrackId: String?,
    val playbackPositionMs: Long,
    val isShuffle: Boolean,
    val repeatMode: RepeatMode
)

class QueuePersistenceRepository(
    private val queueDao: QueueDao
) {

    fun serializeTracks(tracks: List<Track>): String {
        val array = JSONArray()
        for (track in tracks) {
            val obj = JSONObject()
            obj.put("id", track.id)
            obj.put("title", track.title)
            obj.put("artist", track.artist)
            if (track.album != null) obj.put("album", track.album)
            obj.put("durationSeconds", track.durationSeconds)
            obj.put("thumbnailUrl", track.thumbnailUrl)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeTracks(json: String): List<Track> {
        if (json.isBlank()) return emptyList()
        val result = mutableListOf<Track>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    Track(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = if (obj.has("album")) obj.getString("album") else null,
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        thumbnailUrl = obj.optString("thumbnailUrl", "")
                    )
                )
            }
        } catch (_: Exception) {
            // Return whatever was successfully parsed or empty
        }
        return result
    }

    suspend fun saveQueue(
        queueManager: QueueManager,
        playbackPositionMs: Long,
        repeatMode: RepeatMode
    ) {
        val origJson = serializeTracks(queueManager.getOriginalQueue())
        val activeJson = serializeTracks(queueManager.getActiveQueue())
        val currentTrack = queueManager.getCurrentTrack()

        val entity = QueueStateEntity(
            queueId = QueueStateEntity.DEFAULT_QUEUE_ID,
            currentTrackId = currentTrack?.id,
            playbackPositionMs = playbackPositionMs,
            isShuffle = queueManager.isShuffleEnabled(),
            repeatMode = repeatMode.name,
            originalOrderJson = origJson,
            activeOrderJson = activeJson,
            currentIndex = queueManager.getCurrentIndex(),
            updatedAt = System.currentTimeMillis()
        )
        queueDao.saveQueueState(entity)
    }

    suspend fun restoreQueue(): RestoredQueueState? {
        val entity = queueDao.getQueueState() ?: return null
        val original = deserializeTracks(entity.originalOrderJson)
        val active = deserializeTracks(entity.activeOrderJson)
        if (active.isEmpty()) return null

        val repeat = runCatching { RepeatMode.valueOf(entity.repeatMode) }.getOrDefault(RepeatMode.OFF)

        return RestoredQueueState(
            originalQueue = original,
            activeQueue = active,
            currentIndex = entity.currentIndex,
            currentTrackId = entity.currentTrackId,
            playbackPositionMs = entity.playbackPositionMs,
            isShuffle = entity.isShuffle,
            repeatMode = repeat
        )
    }

    suspend fun clearQueue() {
        queueDao.clearQueueState()
    }
}
