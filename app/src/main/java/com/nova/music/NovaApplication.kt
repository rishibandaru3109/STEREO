package com.nova.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nova.music.core.artwork.ArtworkSizingInterceptor
import okhttp3.OkHttpClient
import org.koin.core.context.GlobalContext
import com.nova.music.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class NovaApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(GlobalContext.get().get<OkHttpClient>())
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.12).build() }
        .diskCache {
            DiskCache.Builder().directory(cacheDir.resolve("artwork")).maxSizeBytes(64L * 1024 * 1024).build()
        }
        .components { add(ArtworkSizingInterceptor()) }
        .build()


    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@NovaApplication)
            modules(appModule)
        }
    }
}
