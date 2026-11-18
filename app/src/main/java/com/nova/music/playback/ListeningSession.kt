package com.nova.music.playback

import com.nova.music.domain.model.Track

internal class ListeningSession(val track: Track) {
    private var startedAt: Long? = null
    private var accumulatedMs = 0L

    fun updatePlaying(playing: Boolean, elapsedMs: Long) {
        val started = startedAt
        if (playing && started == null) {
            startedAt = elapsedMs
        } else if (!playing && started != null) {
            accumulatedMs += (elapsedMs - started).coerceAtLeast(0L)
            startedAt = null
        }
    }

    fun finish(elapsedMs: Long, durationMs: Long, manuallySkipped: Boolean): ListeningSample? {
        val listened = accumulatedMs + (startedAt?.let { (elapsedMs - it).coerceAtLeast(0L) } ?: 0L)
        val duration = durationMs.takeIf { it > 0L } ?: track.durationSeconds * 1000
        val earlySkip = manuallySkipped && duration > 0 && listened < minOf(30_000L, duration / 5)
        return if (listened >= 3000L) ListeningSample(track, listened, duration, earlySkip) else null
    }
}

internal data class ListeningSample(
    val track: Track,
    val listenedMs: Long,
    val durationMs: Long,
    val isSkipped: Boolean
)
