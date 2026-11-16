package com.nova.music.di

import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.nova.music.core.designsystem.theme.ThemePreferencesRepository
import com.nova.music.core.designsystem.theme.ThemeRepository
import com.nova.music.data.extractor.InnerTubeXExtractor
import com.nova.music.data.extractor.YouTubeMusicExtractor
import com.nova.music.data.repository.MusicRepositoryImpl
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.feature.player.PlayerViewModel
import com.nova.music.feature.search.SearchViewModel
import com.nova.music.feature.theme.ThemeViewModel
import com.nova.music.playback.ExoPlayerController
import com.nova.music.playback.NovaPlayerController
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    // Network: OkHttpClient with safe diagnostic logging for streaming traffic
    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Safe diagnostic interceptor for Google Video CDN requests (no sensitive URLs/tokens logged)
        val gvsLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val host = request.url.host
            val isGvs = host.contains("googlevideo.com")
            val startTime = System.currentTimeMillis()
            val response = chain.proceed(request)
            val elapsed = System.currentTimeMillis() - startTime
            if (isGvs) {
                val contentLength = response.header("Content-Length")
                val contentRange = response.header("Content-Range")
                val contentType = response.header("Content-Type")
                try {
                    Log.i("NOVA_DIAGNOSTICS", "OkHttp GVS response: HTTP ${response.code} ${response.message} from $host in ${elapsed}ms [type=$contentType, len=$contentLength, range=$contentRange]")
                    if (!response.isSuccessful) {
                        val relevantHeaders = response.headers.names()
                            .filter { it.contains("x-", ignoreCase = true) || it.contains("content", ignoreCase = true) || it.equals("server", ignoreCase = true) }
                            .associateWith { response.header(it) }
                        Log.e("NOVA_DIAGNOSTICS", "GVS non-2xx status: ${response.code}, headers=$relevantHeaders")
                    }
                } catch (_: Throwable) {
                    println("[NOVA_DIAGNOSTICS] OkHttp GVS response: HTTP ${response.code} from $host in ${elapsed}ms")
                }
            }
            response
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(gvsLoggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Extractor: InnerTubeX with multi-client fallback & QuackJS
    single<MusicExtractor> {
        InnerTubeXExtractor(okHttpClient = get())
    }

    // Repository
    single<MusicRepository> {
        MusicRepositoryImpl(extractor = get())
    }

    // Media3 Data Source Factory with Web Remix headers
    single<DataSource.Factory> {
        val okHttpClient: OkHttpClient = get()
        val defaultHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Referer" to "https://music.youtube.com/",
            "Origin" to "https://music.youtube.com",
            "Accept" to "*/*"
        )
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(defaultHeaders)
        DefaultDataSource.Factory(androidContext(), httpDataSourceFactory)
    }

    // Media3 ExoPlayer Engine (shared singleton configured with OkHttp and Web Remix headers)
    single<ExoPlayer> {
        val dataSourceFactory: DataSource.Factory = get()
        val mediaSourceFactory = DefaultMediaSourceFactory(androidContext())
            .setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(androidContext())
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handleAudioFocus = true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    // Playback Controller
    single<NovaPlayerController> {
        ExoPlayerController(
            context = androidContext(),
            musicRepository = get(),
            exoPlayer = get()
        )
    }

    // ViewModels
    viewModel {
        SearchViewModel(
            musicRepository = get(),
            playerController = get()
        )
    }

    viewModel {
        PlayerViewModel(
            playerController = get()
        )
    }

    // Theme Preferences & Management
    single<ThemeRepository> {
        ThemePreferencesRepository(context = androidContext())
    }

    viewModel {
        ThemeViewModel(
            themeRepository = get()
        )
    }
}
