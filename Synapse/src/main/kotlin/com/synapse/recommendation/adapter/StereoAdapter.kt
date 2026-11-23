package com.synapse.recommendation.adapter

import com.synapse.recommendation.model.CandidateSource
import com.synapse.recommendation.model.RecommendationCandidate
import com.synapse.recommendation.model.RecommendationContext
import com.synapse.recommendation.model.RecommendationResult
import com.synapse.recommendation.model.SynapseTrack

/**
 * Clean bridge between STEREO application domain and SYNAPSE engine.
 * Ensures zero architectural leakage and preserves strict backward compatibility.
 */
object StereoAdapter {

    /**
     * Converts a generic or STEREO track record into a canonical SynapseTrack.
     */
    fun toSynapseTrack(
        id: String,
        title: String,
        artist: String,
        album: String? = null,
        durationSeconds: Long = 0L,
        thumbnailUrl: String = "",
        lyricsLanguage: String? = null,
        isSpokenWord: Boolean = false,
        genres: List<String> = emptyList()
    ): SynapseTrack {
        return SynapseTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationSeconds,
            thumbnailUrl = thumbnailUrl,
            language = lyricsLanguage,
            genres = genres,
            isSpokenWord = isSpokenWord
        )
    }

    /**
     * Converts a list of candidate tracks into SYNAPSE RecommendationCandidates.
     */
    fun wrapAsCandidates(
        tracks: List<SynapseTrack>,
        source: CandidateSource = CandidateSource.SONG_RADIO,
        seedTrackId: String? = null
    ): List<RecommendationCandidate> {
        return tracks.map { track ->
            RecommendationCandidate(
                track = track,
                source = source,
                seedTrackId = seedTrackId,
                initialGraphWeight = if (source == CandidateSource.SONG_RADIO) 1.0 else 0.5
            )
        }
    }

    /**
     * Maps recommendation results into section groupings matching STEREO's Home view hierarchy.
     */
    fun partitionIntoSections(
        results: List<RecommendationResult>,
        recentTracks: List<SynapseTrack> = emptyList(),
        topArtistName: String? = null
    ): Map<String, List<RecommendationResult>> {
        val sections = mutableMapOf<String, List<RecommendationResult>>()

        // Quick Picks: Highest-scoring head of recommendations (up to 8 tracks)
        sections["QUICK_PICKS"] = results.take(8)

        // Discover Weekly / Fresh Discoveries: High-novelty tracks outside top artist
        val discoverList = results.filter { r ->
            topArtistName == null || !r.track.artist.equals(topArtistName, ignoreCase = true)
        }.drop(8).take(12)
        if (discoverList.isNotEmpty()) {
            sections["DISCOVER_WEEKLY"] = discoverList
        }

        // Because You Listened To [Artist]
        if (topArtistName != null) {
            val artistMatches = results.filter { r ->
                r.track.artist.equals(topArtistName, ignoreCase = true)
            }.take(10)
            if (artistMatches.isNotEmpty()) {
                sections["BECAUSE_YOU_LISTENED"] = artistMatches
            }
        }

        return sections
    }
}
