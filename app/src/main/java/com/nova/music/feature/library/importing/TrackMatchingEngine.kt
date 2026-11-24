package com.nova.music.feature.library.importing

import com.nova.music.domain.model.Track
import kotlin.math.abs
import kotlin.math.max

object TrackMatchingEngine {

    private val NOISE_REGEX = Regex(
        """(?i)\s*[\(\[][^)\]]*(?:official|video|audio|visualizer|remaster|4k|hd|explicit|clean|radio edit|single version)[^)\]]*[\)\]]"""
    )
    private val FEAT_REGEX = Regex(
        """(?i)\s*[\(\[](?:feat\.|ft\.|with)\s+([^\]\)]+)[\)\]]"""
    )
    private val PUNCTUATION_REGEX = Regex("""[^\p{L}\p{N}\s]""")

    fun normalizeTitle(rawTitle: String): String {
        var cleaned = NOISE_REGEX.replace(rawTitle, "")
        cleaned = FEAT_REGEX.replace(cleaned, "")
        cleaned = PUNCTUATION_REGEX.replace(cleaned, " ")
        return cleaned.trim().replace(Regex("""\s+"""), " ").lowercase()
    }

    fun normalizeArtist(rawArtist: String): String {
        val cleaned = PUNCTUATION_REGEX.replace(rawArtist, " ")
        return cleaned.trim().replace(Regex("""\s+"""), " ").lowercase()
    }

    fun scoreCandidate(raw: RawImportTrack, candidate: Track): Float {
        val rawNormTitle = normalizeTitle(raw.title)
        val candNormTitle = normalizeTitle(candidate.title)

        val rawNormArtist = normalizeArtist(raw.artist)
        val candNormArtist = normalizeArtist(candidate.artist)

        // Title score (Token overlap + Levenshtein similarity)
        val titleTokenScore = tokenOverlapScore(rawNormTitle, candNormTitle)
        val titleLevScore = stringSimilarity(rawNormTitle, candNormTitle)
        val titleScore = (titleTokenScore * 0.4f) + (titleLevScore * 0.6f)

        // Artist score
        val artistTokenScore = tokenOverlapScore(rawNormArtist, candNormArtist)
        val artistLevScore = stringSimilarity(rawNormArtist, candNormArtist)
        val artistScore = (artistTokenScore * 0.4f) + (artistLevScore * 0.6f)

        // Duration tolerance
        var durationDeltaPenalty = 0.0f
        if (raw.durationSeconds > 0 && candidate.durationSeconds > 0) {
            val delta = abs(raw.durationSeconds - candidate.durationSeconds)
            if (delta <= 5) {
                durationDeltaPenalty = 0.05f // Bonus for precise duration match
            } else if (delta in 6..15) {
                durationDeltaPenalty = 0.0f
            } else if (delta in 16..30) {
                durationDeltaPenalty = -0.08f
            } else {
                durationDeltaPenalty = -0.25f // Major penalty for huge duration difference
            }
        }

        // Version / cover check: prevent covers or karaoke from hijacking original
        val candLowerTitle = candidate.title.lowercase()
        val rawLowerTitle = raw.title.lowercase()
        var versionPenalty = 0.0f

        val unwantedTags = listOf("cover", "karaoke", "instrumental", "tribute", "parody", "remix", "live", "acoustic")
        for (tag in unwantedTags) {
            if (candLowerTitle.contains(tag) && !rawLowerTitle.contains(tag)) {
                versionPenalty -= 0.35f
            }
        }

        val baseScore = (titleScore * 0.60f) + (artistScore * 0.35f) + durationDeltaPenalty + versionPenalty
        return baseScore.coerceIn(0.0f, 1.0f)
    }

    fun matchTrack(raw: RawImportTrack, candidates: List<Track>): MatchedImportTrack {
        if (candidates.isEmpty()) {
            return MatchedImportTrack(
                rawTrack = raw,
                matchedTrack = null,
                confidence = 0f,
                status = MatchStatus.UNMATCHED
            )
        }

        var bestScore = -1f
        var bestTrack: Track? = null

        for (candidate in candidates) {
            val score = scoreCandidate(raw, candidate)
            if (score > bestScore) {
                bestScore = score
                bestTrack = candidate
            }
        }

        return when {
            bestScore >= 0.95f -> MatchedImportTrack(
                rawTrack = raw,
                matchedTrack = bestTrack,
                confidence = bestScore,
                status = MatchStatus.EXACT_MATCH
            )
            bestScore >= 0.78f -> MatchedImportTrack(
                rawTrack = raw,
                matchedTrack = bestTrack,
                confidence = bestScore,
                status = MatchStatus.HIGH_CONFIDENCE
            )
            bestScore >= 0.50f -> MatchedImportTrack(
                rawTrack = raw,
                matchedTrack = bestTrack,
                confidence = bestScore,
                status = MatchStatus.MEDIUM_CONFIDENCE
            )
            else -> MatchedImportTrack(
                rawTrack = raw,
                matchedTrack = null,
                confidence = bestScore.coerceAtLeast(0f),
                status = MatchStatus.UNMATCHED
            )
        }
    }

    private fun tokenOverlapScore(s1: String, s2: String): Float {
        val t1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
        val t2 = s2.split(" ").filter { it.isNotBlank() }.toSet()
        if (t1.isEmpty() || t2.isEmpty()) return 0f
        val intersection = t1.intersect(t2).size
        val union = t1.union(t2).size
        return intersection.toFloat() / union.toFloat()
    }

    private fun stringSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1f
        if (s1.isEmpty() || s2.isEmpty()) return 0f
        val distance = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)
        return (maxLen - distance).toFloat() / maxLen.toFloat()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
