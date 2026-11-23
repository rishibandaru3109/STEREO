package com.nova.music.domain.model

data class AudioStream(
    val url: String,
    val mimeType: String = "audio/webm",
    val codec: String = "opus",
    val bitrate: Int = 160000,
    val formatId: String = "251",
    val contentLength: Long = 0,
    val isAudioOnly: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
    val providerId: String = "youtube",
    val cachePermission: AudioCachePermission = AudioCachePermission.DENIED,
    val representationRevision: String = "",
    val audioTrackId: String = "",
    val transport: AudioTransport = AudioTransport.DIRECT,
    val isLocalCache: Boolean = false

)

enum class AudioCachePermission { DENIED, PERMITTED }
enum class AudioTransport { DIRECT, SABR, SEGMENTED }

/** Stable representation hints; expiring signatures never enter the disk identity. */
fun AudioStream.withRepresentationMetadata(): AudioStream {
    if (isLocalCache) return this
    val query = runCatching { java.net.URI(url).rawQuery }.getOrNull().orEmpty().split('&')
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator < 0) null else part.substring(0, separator) to
                java.net.URLDecoder.decode(part.substring(separator + 1), "UTF-8")
        }.toMap()
    return copy(
        contentLength = contentLength.takeIf { it > 0 } ?: query["clen"]?.toLongOrNull() ?: 0L,
        representationRevision = representationRevision.ifBlank { query["lmt"].orEmpty() },
        audioTrackId = audioTrackId.ifBlank { query["xtags"].orEmpty() }
    )
}
