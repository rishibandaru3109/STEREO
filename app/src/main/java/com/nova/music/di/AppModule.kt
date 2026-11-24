package com.nova.music.di

import android.util.Log
import com.nova.music.BuildConfig
import com.nova.music.core.cache.AudioPrefetcher
import org.koin.core.qualifier.named
import com.nova.music.core.cache.AudioCacheEngine
import com.nova.music.core.network.NetworkMonitor
import com.nova.music.core.network.TrafficMeter
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.room.Room
import com.nova.music.core.database.StereoDatabase
import com.nova.music.core.database.dao.DownloadDao
import com.nova.music.core.database.dao.HistoryDao
import com.nova.music.core.database.dao.LikedSongDao
import com.nova.music.core.database.dao.PlaylistDao
import com.nova.music.core.database.dao.QueueDao
import com.nova.music.core.database.dao.RecommendationDao
import com.nova.music.core.database.dao.SynapseDao
import com.nova.music.core.database.dao.TrackDao
import com.nova.music.core.designsystem.theme.ThemePreferencesRepository
import com.nova.music.core.designsystem.theme.ThemeRepository
import com.nova.music.core.designsystem.wallpaper.WallpaperPreferencesRepository
import com.nova.music.core.designsystem.wallpaper.WallpaperRepository
import com.nova.music.data.extractor.InnerTubeXExtractor
import com.nova.music.data.repository.DownloadRepositoryImpl
import com.nova.music.data.repository.HistoryRepositoryImpl
import com.nova.music.data.repository.HomeRepositoryImpl
import com.nova.music.data.repository.CatalogRecommendationMetadata
import com.nova.music.data.repository.LikedSongsRepositoryImpl
import com.nova.music.data.repository.ListeningProfileRepositoryImpl
import com.nova.music.data.repository.MusicPreferencesRepositoryImpl
import com.nova.music.data.repository.MusicRepositoryImpl
import com.nova.music.data.repository.PlaylistRepositoryImpl
import com.nova.music.data.repository.QueuePersistenceRepository
import com.nova.music.domain.recommendation.CandidateProvider
import com.nova.music.domain.recommendation.CatalogCandidateProvider
import com.nova.music.domain.recommendation.RecommendationEngine
import com.nova.music.domain.recommendation.RecommendationEngineImpl
import com.nova.music.domain.recommendation.SynapseRecommendationEngineBridge
import com.synapse.recommendation.engine.SynapseRecommendationEngine
import com.synapse.recommendation.taste.TasteIntelligence
import com.nova.music.domain.recommendation.SurpriseMeEngine
import com.nova.music.domain.recommendation.SurpriseMeEngineImpl
import com.nova.music.domain.repository.DownloadRepository
import com.nova.music.domain.repository.HistoryRepository
import com.nova.music.feature.downloads.DownloadsViewModel
import com.nova.music.domain.repository.HomeRepository
import com.nova.music.domain.repository.LikedSongsRepository
import com.nova.music.domain.repository.ListeningProfileRepository
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.domain.repository.PlaylistRepository
import com.nova.music.feature.home.HomeViewModel
import com.nova.music.feature.library.LibraryViewModel
import com.nova.music.feature.player.PlayerViewModel
import com.nova.music.feature.search.SearchViewModel
import com.nova.music.feature.settings.SettingsViewModel
import com.nova.music.feature.theme.ThemeViewModel
import com.nova.music.feature.wallpaper.WallpaperViewModel
import com.nova.music.playback.ExoPlayerController
import com.nova.music.playback.NovaPlayerController
import com.nova.music.playback.queue.QueueManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    single { AudioCacheEngine(androidContext(), personalBuild = true) }
    single(createdAtStart = true) { AudioPrefetcher(get(), get(), get(), get<NetworkMonitor>().state, get(named("audioUpstream"))) }
    single { NetworkMonitor(androidContext()) }

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
            .addNetworkInterceptor(TrafficMeter.interceptor)
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

    // Room Database & DAOs with Migration(1, 2, 3, 4)
    single<StereoDatabase> {
        Room.databaseBuilder(
            androidContext(),
            StereoDatabase::class.java,
            "stereo_database.db"
        )
            .addMigrations(
                StereoDatabase.MIGRATION_1_2,
                StereoDatabase.MIGRATION_2_3,
                StereoDatabase.MIGRATION_3_4
            )
            .build()
    }
    single<TrackDao> { get<StereoDatabase>().trackDao() }
    single<LikedSongDao> { get<StereoDatabase>().likedSongDao() }
    single<PlaylistDao> { get<StereoDatabase>().playlistDao() }
    single<HistoryDao> { get<StereoDatabase>().historyDao() }
    single<QueueDao> { get<StereoDatabase>().queueDao() }
    single<RecommendationDao> { get<StereoDatabase>().recommendationDao() }
    single<SynapseDao> { get<StereoDatabase>().synapseDao() }
    single<DownloadDao> { get<StereoDatabase>().downloadDao() }

    // Offline Downloads Repository
    single<DownloadRepository> {
        DownloadRepositoryImpl(
            context = androidContext(),
            downloadDao = get(),
            extractor = get(),
            okHttpClient = get(),
            synapseDao = getOrNull(),
            preferencesRepository = getOrNull()
        )
    }

    // Repository
    single<MusicRepository> {
        MusicRepositoryImpl(
            extractor = get(),
            preferencesRepository = get(),
            networkState = get<NetworkMonitor>().state,
            audioCache = get(),
            downloadRepository = getOrNull()
        )
    }

    // Liked Songs Repository
    single<LikedSongsRepository> {
        LikedSongsRepositoryImpl(
            trackDao = get(),
            likedSongDao = get()
        )
    }

    // Playlist Repository
    single<PlaylistRepository> {
        PlaylistRepositoryImpl(
            playlistDao = get(),
            trackDao = get()
        )
    }

    // History Repository
    single<HistoryRepository> {
        HistoryRepositoryImpl(
            historyDao = get(),
            trackDao = get()
        )
    }

    // Listening Profile Repository
    single<ListeningProfileRepository> {
        ListeningProfileRepositoryImpl(
            historyDao = get(),
            likedSongDao = get(),
            trackDao = get(),
            recommendationDao = get()
        )
    }

    // Recommendation Engine & Providers
    single<MusicPreferencesRepository> {
        MusicPreferencesRepositoryImpl(androidContext())
    }

    single<CandidateProvider> {
        CatalogCandidateProvider(
            extractor = get(),
            preferencesRepository = get(),
            metadata = CatalogRecommendationMetadata(androidContext(), get())
        )
    }

    // SYNAPSE Autonomous Recommendation Engine
    single<TasteIntelligence> { TasteIntelligence() }
    single<SynapseRecommendationEngine> {
        SynapseRecommendationEngine(
            candidateProvider = com.synapse.recommendation.candidate.InMemoryCandidateProvider(emptyList()),
            tasteIntelligence = get()
        )
    }

    single<RecommendationEngine> {
        SynapseRecommendationEngineBridge(
            synapseEngine = get(),
            preferencesRepository = get(),
            legacyEngine = RecommendationEngineImpl()
        )
    }

    single<SurpriseMeEngine> {
        SurpriseMeEngineImpl(recommendationEngine = get())
    }

    single<HomeRepository> {
        HomeRepositoryImpl(
            listeningProfileRepository = get(),
            candidateProvider = get(),
            recommendationEngine = get(),
            recommendationDao = get(),
            preferencesRepository = get()
        )
    }

    // Queue Persistence Repository
    single {
        QueuePersistenceRepository(queueDao = get())
    }

    // Media3 Data Source Factory with Web Remix headers
    single<DataSource.Factory>(named("audioUpstream")) {
        val okHttpClient: OkHttpClient = get()
        val defaultHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Accept" to "*/*"
        )
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(defaultHeaders)
        DefaultDataSource.Factory(androidContext(), httpDataSourceFactory)
    }
    single<DataSource.Factory> { get<AudioCacheEngine>().factory(get(named("audioUpstream"))) }

    // Media3 ExoPlayer Engine (shared singleton configured with OkHttp and Web Remix headers)
    single<ExoPlayer> {
        val dataSourceFactory: DataSource.Factory = get()
        val mediaSourceFactory = DefaultMediaSourceFactory(androidContext())
            .setDataSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(androidContext())
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(5_000, 15_000, 1_000, 2_500)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .setBackBuffer(0, false)
                    .build()
            )
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

    // Queue Manager Engine
    single { QueueManager() }

    // Playback Controller
    single<NovaPlayerController> {
        ExoPlayerController(
            context = androidContext(),
            musicRepository = get(),
            exoPlayer = get(),
            queueManager = get(),
            historyRepository = get(),
            preferencesRepository = get(),
            audioCache = get(),
            synapseEngine = get(),
            synapseDao = get()
        )
    }

    // ViewModels
    viewModel {
        HomeViewModel(
            homeRepository = get(),
            playerController = get(),
            likedSongsRepository = get(),
            preferencesRepository = get(),
            surpriseMeEngine = get(),
            listeningProfileRepository = get(),
            downloadRepository = getOrNull()
        )
    }

    viewModel {
        SearchViewModel(
            musicRepository = get(),
            playerController = get(),
            downloadRepository = getOrNull()
        )
    }

    viewModel {
        PlayerViewModel(
            playerController = get(),
            likedSongsRepository = get(),
            downloadRepository = getOrNull()
        )
    }

    viewModel {
        LibraryViewModel(
            likedSongsRepository = get(),
            playlistRepository = get(),
            historyRepository = get(),
            playerController = get(),
            musicRepository = getOrNull(),
            synapseDao = getOrNull(),
            downloadRepository = getOrNull()
        )
    }

    viewModel {
        DownloadsViewModel(
            downloadRepository = get(),
            playerController = get(),
            preferencesRepository = getOrNull()
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

    // Wallpaper Preferences & Management
    single<WallpaperRepository> {
        WallpaperPreferencesRepository(context = androidContext())
    }

    viewModel {
        WallpaperViewModel(
            wallpaperRepository = get()
        )
    }

    // Settings ViewModel
    viewModel {
        SettingsViewModel(
            themeRepository = get(),
            wallpaperRepository = get(),
            preferencesRepository = get(),
            historyRepository = get(),
            context = androidContext(),
            homeRepository = get(),
            audioCache = get()
        )
    }
}
