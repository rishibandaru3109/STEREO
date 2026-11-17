package com.nova.music.playback.shuffle

import com.nova.music.domain.model.Track
import java.util.Random

enum class ShuffleMode {
    OFF,
    STANDARD,
    BALANCED
}

interface ShuffleEngine {
    fun shuffle(tracks: List<Track>, currentTrack: Track?, seed: Long? = null): List<Track>
}

class StandardShuffle : ShuffleEngine {
    override fun shuffle(tracks: List<Track>, currentTrack: Track?, seed: Long?): List<Track> {
        if (tracks.isEmpty()) return emptyList()
        val rng = if (seed != null) kotlin.random.Random(seed) else kotlin.random.Random.Default
        val mutable = tracks.toMutableList()

        if (currentTrack != null) {
            val currIndex = mutable.indexOfFirst { it.id == currentTrack.id }
            if (currIndex >= 0) {
                mutable.removeAt(currIndex)
            }
            val shuffled = mutable.shuffled(rng)
            return listOf(currentTrack) + shuffled
        }

        return mutable.shuffled(rng)
    }
}

class BalancedShuffle : ShuffleEngine {
    override fun shuffle(tracks: List<Track>, currentTrack: Track?, seed: Long?): List<Track> {
        if (tracks.size <= 2) return StandardShuffle().shuffle(tracks, currentTrack, seed)
        val rng = if (seed != null) kotlin.random.Random(seed) else kotlin.random.Random.Default

        val pool = tracks.toMutableList()
        if (currentTrack != null) {
            val idx = pool.indexOfFirst { it.id == currentTrack.id }
            if (idx >= 0) pool.removeAt(idx)
        }

        // Group by artist (normalized)
        val artistGroups: MutableMap<String, MutableList<Track>> = mutableMapOf()
        pool.shuffled(rng).forEach { track ->
            val key = track.artist.trim().lowercase()
            artistGroups.getOrPut(key) { mutableListOf() }.add(track)
        }

        val result = mutableListOf<Track>()
        var lastArtist = currentTrack?.artist?.trim()?.lowercase() ?: ""

        val totalRemaining = pool.size
        for (i in 0 until totalRemaining) {
            // Find valid artist candidate that is NOT lastArtist (if possible)
            val candidateKeys = artistGroups.keys.filter { it != lastArtist && artistGroups[it]?.isNotEmpty() == true }

            val chosenKey = if (candidateKeys.isNotEmpty()) {
                // Pick the candidate with the highest remaining songs to avoid starvation
                candidateKeys.maxByOrNull { artistGroups[it]?.size ?: 0 }!!
            } else {
                // If only the same artist remains, pick from any available group
                artistGroups.keys.firstOrNull { artistGroups[it]?.isNotEmpty() == true }
            }

            if (chosenKey != null) {
                val group = artistGroups[chosenKey]!!
                val pickedTrack = group.removeAt(0)
                if (group.isEmpty()) {
                    artistGroups.remove(chosenKey)
                }
                result.add(pickedTrack)
                lastArtist = chosenKey
            }
        }

        return if (currentTrack != null) listOf(currentTrack) + result else result
    }
}
