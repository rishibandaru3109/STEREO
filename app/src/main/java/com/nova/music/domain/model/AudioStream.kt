package com.nova.music.domain.model

data class AudioStream(
    val url: String,
    val mimeType: String = "audio/webm",
    val codec: String = "opus",
    val bitrate: Int = 160000,
    val formatId: String = "251",
    val contentLength: Long = 0,
    val isAudioOnly: Boolean = true,
    val headers: Map<String, String> = emptyMap()
)
