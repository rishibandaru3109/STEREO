package com.nova.music.di

import com.nova.music.data.extractor.YouTubeMusicExtractor
import com.nova.music.data.repository.MusicRepositoryImpl
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.feature.player.PlayerViewModel
import com.nova.music.feature.search.SearchViewModel
import com.nova.music.playback.ExoPlayerController
import com.nova.music.playback.NovaPlayerController
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    // Network: OkHttpClient
    single {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    // Extractor
    single<MusicExtractor> {
        YouTubeMusicExtractor(httpClient = get())
    }

    // Repository
    single<MusicRepository> {
        MusicRepositoryImpl(extractor = get())
    }

    // Playback Engine
    single<NovaPlayerController> {
        ExoPlayerController(
            context = androidContext(),
            musicRepository = get()
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
}
