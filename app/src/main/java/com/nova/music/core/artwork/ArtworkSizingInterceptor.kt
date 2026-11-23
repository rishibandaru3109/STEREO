package com.nova.music.core.artwork

import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.decode.DataSource
import com.nova.music.core.network.TrafficMeter
import coil.size.pxOrElse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun sizedArtworkUrl(url: String, pixels: Int): String {
    val host = url.toHttpUrlOrNull()?.host ?: return url
    if (host != "lh3.googleusercontent.com" && host != "yt3.ggpht.com") return url
    val bucket = when {
        pixels <= 128 -> 128
        pixels <= 256 -> 256
        pixels <= 512 -> 512
        else -> 768
    }
    return url.replace(Regex("=w\\d+-h\\d+"), "=w$bucket-h$bucket")
        .replace(Regex("=s\\d+"), "=s$bucket")
}

class ArtworkSizingInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data as? String ?: return chain.proceed(chain.request)
        val size = maxOf(chain.size.width.pxOrElse { 768 }, chain.size.height.pxOrElse { 768 })
        val result = chain.proceed(chain.request.newBuilder().data(sizedArtworkUrl(data, size)).build())
        if (result is SuccessResult &&
            (result.dataSource == DataSource.MEMORY_CACHE || result.dataSource == DataSource.DISK)
        ) TrafficMeter.counters.artworkCacheHit()
        return result
    }
}
