package com.synapse.recommendation.similarity

import com.synapse.recommendation.model.SynapseTrack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Lightweight, on-device Content Similarity Engine inspired by TF-IDF and
 * Spotify feature-distance references, engineered to run strictly in O(K * D) memory.
 *
 * Avoids the catastrophic O(N^2) memory footprint of full-catalog cosine matrices.
 */
class ContentSimilarityEngine {

    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "in", "on", "at", "by", "for", "with",
        "official", "video", "audio", "music", "lyric", "lyrics", "remix",
        "hd", "4k", "feat", "ft", "prod", "version", "original", "edit"
    )

    /**
     * Computes multi-field similarity between two tracks in [0.0, 1.0].
     */
    fun computeSimilarity(t1: SynapseTrack, t2: SynapseTrack): Double {
        if (t1.id == t2.id) return 1.0

        val artistSim = computeArtistSimilarity(t1.artist, t2.artist)
        val genreSim = computeGenreSimilarity(t1.genres, t2.genres)
        val titleSim = computeTitleSimilarity(t1.title, t2.title)
        val languageSim = computeLanguageSimilarity(t1.language, t2.language)
        val durationSim = computeDurationSimilarity(t1.durationSeconds, t2.durationSeconds)

        return (0.40 * artistSim) +
               (0.25 * genreSim) +
               (0.15 * titleSim) +
               (0.10 * languageSim) +
               (0.10 * durationSim)
    }

    /**
     * Exact or substring/token artist matching.
     */
    fun computeArtistSimilarity(artist1: String, artist2: String): Double {
        val a1 = normalize(artist1)
        val a2 = normalize(artist2)
        if (a1.isEmpty() || a2.isEmpty()) return 0.0
        if (a1 == a2) return 1.0

        val tokens1 = tokenize(a1).filterNot { it in stopWords }
        val tokens2 = tokenize(a2).filterNot { it in stopWords }
        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size
        return if (union > 0) intersection.toDouble() / union.toDouble() else 0.0
    }

    /**
     * Jaccard similarity across genre tags.
     */
    fun computeGenreSimilarity(genres1: List<String>, genres2: List<String>): Double {
        if (genres1.isEmpty() || genres2.isEmpty()) return 0.0
        val s1 = genres1.map { it.lowercase().trim() }.toSet()
        val s2 = genres2.map { it.lowercase().trim() }.toSet()
        val intersection = s1.intersect(s2).size
        val union = s1.union(s2).size
        return if (union > 0) intersection.toDouble() / union.toDouble() else 0.0
    }

    /**
     * TF-IDF-inspired cosine similarity over title tokens without global matrix allocation.
     */
    fun computeTitleSimilarity(title1: String, title2: String): Double {
        val tokens1 = tokenize(title1).filterNot { it in stopWords }
        val tokens2 = tokenize(title2).filterNot { it in stopWords }
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val freq1 = tokens1.groupingBy { it }.eachCount()
        val freq2 = tokens2.groupingBy { it }.eachCount()

        val allTokens = freq1.keys.union(freq2.keys)
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (token in allTokens) {
            val v1 = freq1[token]?.toDouble() ?: 0.0
            val v2 = freq2[token]?.toDouble() ?: 0.0
            dotProduct += v1 * v2
            norm1 += v1 * v1
            norm2 += v2 * v2
        }

        if (norm1 == 0.0 || norm2 == 0.0) return 0.0
        return (dotProduct / (sqrt(norm1) * sqrt(norm2))).coerceIn(0.0, 1.0)
    }

    fun computeLanguageSimilarity(lang1: String?, lang2: String?): Double {
        if (lang1.isNullOrBlank() || lang2.isNullOrBlank()) return 0.5 // Neutral uncertainty
        return if (lang1.equals(lang2, ignoreCase = true)) 1.0 else 0.0
    }

    fun computeDurationSimilarity(dur1: Long, dur2: Long): Double {
        if (dur1 <= 0 || dur2 <= 0) return 0.5
        val diff = abs(dur1 - dur2)
        return max(0.0, 1.0 - (diff.toDouble() / 60.0)).coerceIn(0.0, 1.0)
    }

    private val nonAlphanumericSpaceRegex = Regex("[^a-z0-9\\s]")
    private val whitespaceRegex = Regex("\\s+")

    private fun normalize(text: String): String =
        text.lowercase().trim().replace(nonAlphanumericSpaceRegex, "")

    private fun tokenize(text: String): Set<String> =
        normalize(text).split(whitespaceRegex).filter { it.isNotBlank() }.toSet()
}
