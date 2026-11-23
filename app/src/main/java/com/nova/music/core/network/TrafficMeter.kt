package com.nova.music.core.network

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import java.util.concurrent.atomic.AtomicLong

enum class TrafficKind { AUDIO, ARTWORK, API }

data class TrafficSnapshot(
    val audioBytes: Long,
    val artworkBytes: Long,
    val apiBytes: Long,
    val requests: Long,
    val declaredUploadBytes: Long,
    val metadataCacheHits: Long,
    val artworkCacheHits: Long
) {
    fun logLine(): String =
        "audio=$audioBytes artwork=$artworkBytes api=$apiBytes requests=$requests declaredUpload=$declaredUploadBytes metadataHits=$metadataCacheHits artworkHits=$artworkCacheHits"
}

class TrafficCounters {
    private val bytes = TrafficKind.entries.associateWith { AtomicLong() }
    private val requests = AtomicLong()
    private val uploads = AtomicLong()
    private val metadataHits = AtomicLong()
    private val artworkHits = AtomicLong()

    fun metadataCacheHit() { metadataHits.incrementAndGet() }
    fun artworkCacheHit() { artworkHits.incrementAndGet() }

    fun request(declaredUploadBytes: Long) {
        requests.incrementAndGet()
        uploads.addAndGet(declaredUploadBytes.coerceAtLeast(0))
    }

    fun add(kind: TrafficKind, count: Long) {
        if (count > 0) bytes.getValue(kind).addAndGet(count)
    }

    fun snapshot(): TrafficSnapshot = TrafficSnapshot(
        bytes.getValue(TrafficKind.AUDIO).get(), bytes.getValue(TrafficKind.ARTWORK).get(),
        bytes.getValue(TrafficKind.API).get(), requests.get(), uploads.get(), metadataHits.get(), artworkHits.get()
    )
}

internal class MeteredResponseBody(
    private val body: ResponseBody,
    private val counters: TrafficCounters,
    private val kind: TrafficKind
) : ResponseBody() {
    private val countedSource: BufferedSource by lazy {
        object : ForwardingSource(body.source()) {
            override fun read(sink: Buffer, byteCount: Long): Long =
                super.read(sink, byteCount).also { counters.add(kind, it) }
        }.buffer()
    }

    override fun contentType(): MediaType? = body.contentType()
    override fun contentLength(): Long = body.contentLength()
    override fun source(): BufferedSource = countedSource
}

object TrafficMeter {
    val counters = TrafficCounters()

    fun classify(host: String, contentType: String?): TrafficKind = when {
        host == "googlevideo.com" || host.endsWith(".googlevideo.com") ||
            contentType?.startsWith("audio/") == true -> TrafficKind.AUDIO
        contentType?.startsWith("image/") == true -> TrafficKind.ARTWORK
        else -> TrafficKind.API
    }

    val interceptor = Interceptor { chain ->
        val request = chain.request()
        counters.request(runCatching { request.body?.contentLength() ?: 0 }.getOrDefault(0))
        val response = chain.proceed(request)
        val body = response.body
        if (body == null) response else response.newBuilder().body(
            MeteredResponseBody(body, counters, classify(request.url.host, body.contentType()?.toString()))
        ).build()
    }
}
