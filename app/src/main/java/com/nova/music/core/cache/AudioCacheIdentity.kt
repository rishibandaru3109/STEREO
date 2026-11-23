package com.nova.music.core.cache

import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.AudioCachePermission
import com.nova.music.domain.model.AudioTransport
import java.net.URI
import java.security.MessageDigest

object AudioCacheIdentity {
    fun key(trackId: String, stream: AudioStream, personalBuild: Boolean = false): String? {
        val uri = runCatching { URI(stream.url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase(java.util.Locale.ROOT).orEmpty()
        val youtube = stream.providerId.equals("youtube", true) ||
            host == "googlevideo.com" || host.endsWith(".googlevideo.com") ||
            host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
        if ((!personalBuild && (youtube || stream.cachePermission != AudioCachePermission.PERMITTED)) ||
            uri.scheme !in listOf("https", "http") || host.isBlank() ||
            stream.providerId.isBlank() || stream.transport != AudioTransport.DIRECT ||
            !stream.isAudioOnly || !stream.mimeType.startsWith("audio/") || trackId.isBlank() || stream.formatId.isBlank() ||
            stream.codec.isBlank() || stream.contentLength < 0 || (!personalBuild && stream.contentLength == 0L)
        ) return null
        val identity = listOf(stream.providerId, trackId, stream.formatId, stream.codec,
            stream.mimeType, stream.bitrate.toString(),
            stream.audioTrackId, stream.representationRevision)
            .joinToString("") { it.length.toString() + ":" + it }
        return "stereo-audio-v2-" + MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

object AudioCacheLimits {
    val optionsMb = listOf(0, 100, 250, 500, 1000, -1)
    const val DEFAULT_MB = 500
    fun validated(value: Int): Int = value.takeIf { it in optionsMb } ?: DEFAULT_MB
    fun bytes(value: Int): Long = if (validated(value) == -1) Long.MAX_VALUE else validated(value) * 1_000_000L
}
