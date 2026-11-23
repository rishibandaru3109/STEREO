package com.nova.music.testing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.TextView
import androidx.media3.common.Player
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import com.nova.music.core.cache.AudioCacheEngine
import com.nova.music.core.network.TrafficMeter
import com.nova.music.domain.model.*
import com.nova.music.domain.repository.MusicPreferencesRepository
import com.nova.music.domain.repository.MusicRepository
import com.nova.music.playback.NovaPlayerController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.koin.core.context.GlobalContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong

/** Debug-only, explicit ADB entry point. No release component or automatic test playback. */
class CacheVerificationActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "stereo:cache_verification")?.apply {
            setReferenceCounted(false)
        }
        val text = TextView(this).apply { text = "Running cache verification..."; textSize = 18f }
        setContentView(text)
        runPhase(intent, text)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val text = TextView(this).apply { text = "Running cache verification..."; textSize = 18f }
        setContentView(text)
        intent?.let { runPhase(it, text) }
    }

    private fun runPhase(intent: Intent, text: TextView) {
        val phase = intent.getStringExtra("phase") ?: "fixture"
        val results = File(filesDir, "cache-verification").apply { mkdirs() }
        File(results, "$phase.json").delete()
        scope.launch {
            wakeLock?.acquire(10 * 60 * 1000L) // Safe 10m maximum
            val result = try {
                if (phase == "fixture") fixture() else playback(phase)
            } catch (error: Exception) {
                android.util.Log.e("STEREO_CACHE_TEST", "Phase $phase failed with exception", error)
                JSONObject().put("passed", false).put("error", error.toString())
            }.put("phase", phase).put("timeMs", System.currentTimeMillis())
                .put("runId", intent.getStringExtra("runId").orEmpty())
            withContext(Dispatchers.IO) {
                val temp = File(results, "$phase.tmp")
                temp.writeText(result.toString(2))
                check(temp.renameTo(File(results, "$phase.json")))
            }
            text.text = result.toString(2)
            android.util.Log.i("STEREO_CACHE_TEST", result.toString())
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
    }

    override fun onDestroy() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun playback(phase: String): JSONObject {
        require(phase in setOf("first", "replay", "restart", "offline", "seek", "background"))
        val koin = GlobalContext.get()
        val cache = koin.get<AudioCacheEngine>()
        val repository = koin.get<MusicRepository>()
        val player = koin.get<NovaPlayerController>()
        val exoPlayer = koin.get<ExoPlayer>()
        val preferences = koin.get<MusicPreferencesRepository>()
        val saved = getSharedPreferences("cache_verification", MODE_PRIVATE)
        val id = intent.getStringExtra("trackId") ?: saved.getString("track", null)
            ?: player.queueSnapshot.value.currentTrack?.id ?: error("Supply --es trackId VIDEO_ID on first run")
        saved.edit().putString("track", id).commit()
        val track = Track(id, "Cache verification: $id", "STEREO private test")
        val previousQuality = preferences.audioQualityMode.first()
        preferences.setAudioQualityMode(AudioQualityMode.HIGH)
        cache.setEnabled(true)
        cache.setPrefetch(false)
        player.pause()
        player.setRepeatMode(RepeatMode.OFF)
        if (phase == "first") cache.clear()
        // Let any older canceled media loader finish before taking the network baseline.
        delay(1_000)
        val localBefore = cache.fullyCached(id, AudioQualityMode.HIGH) != null
        if (phase != "first") check(localBefore) { "Track is not validated and fully cached; complete first playback first" }
        val before = TrafficMeter.counters.snapshot().audioBytes
        if (phase == "offline") check(!repository.networkConnected.first()) { "Phone still has a network connection" }
        var position = 0L
        try {
            android.util.Log.i("STEREO_CACHE_TEST", "[$phase] Starting playback for track $id...")
            player.setQueue(listOf(track))

            // Wait up to 30 seconds for initial playback start
            withTimeout(30_000) {
                player.playbackState.first { it.status == PlayerStatus.ERROR || it.currentTrack?.id == id && it.isPlaying }
            }
            check(player.playbackState.value.status != PlayerStatus.ERROR) { player.playbackState.value.errorMessage.orEmpty() }

            if (phase == "first") {
                android.util.Log.i("STEREO_CACHE_TEST", "[first] Initial playback active. Monitoring full track caching...")
                val startTime = System.currentTimeMillis()
                var lastProgressTime = startTime
                var lastPos = -1L
                val maxPhaseMs = 300_000L // 5 minutes max (more than enough for a ~3m song)

                while (System.currentTimeMillis() - startTime < maxPhaseMs) {
                    val pState = player.playbackState.value
                    val curPos = pState.currentPositionMs
                    val dur = pState.durationMs
                    val buf = pState.bufferedPositionMs
                    val netBytes = TrafficMeter.counters.snapshot().audioBytes - before
                    val exoState = exoPlayer.playbackState

                    android.util.Log.i("STEREO_CACHE_TEST", "[first] exoState=$exoState status=${pState.status} isPlaying=${pState.isPlaying} pos=$curPos/$dur buf=$buf netBytes=$netBytes")

                    if (pState.status == PlayerStatus.ENDED || exoState == Player.STATE_ENDED) {
                        android.util.Log.i("STEREO_CACHE_TEST", "[first] Reached ENDED state successfully!")
                        break
                    }
                    if (pState.status == PlayerStatus.ERROR) {
                        error("Player reported ERROR during first playback: ${pState.errorMessage}")
                    }
                    if (exoPlayer.playerError != null) {
                        error("ExoPlayer reported error: ${exoPlayer.playerError?.message}")
                    }

                    if (curPos != lastPos) {
                        lastPos = curPos
                        lastProgressTime = System.currentTimeMillis()
                    } else if (dur > 0 && curPos >= dur - 1500L) {
                        android.util.Log.i("STEREO_CACHE_TEST", "[first] Reached near end of track ($curPos/$dur)!")
                        break
                    } else if (System.currentTimeMillis() - lastProgressTime > 25_000L) {
                        error("Playback stalled for 25s at pos=$curPos/$dur, buf=$buf, netBytes=$netBytes, status=${pState.status}, isPlaying=${pState.isPlaying}, err=${pState.errorMessage}")
                    }
                    delay(1_000)
                }

                val finalState = player.playbackState.value
                val finalExoState = exoPlayer.playbackState
                if (finalState.status != PlayerStatus.ENDED && finalExoState != Player.STATE_ENDED &&
                    (finalState.durationMs <= 0 || finalState.currentPositionMs < finalState.durationMs - 2000L)) {
                    error("First playback did not complete within timeout: pos=${finalState.currentPositionMs}/${finalState.durationMs}, status=${finalState.status}")
                }
            } else {
                delay(5_000)
                if (phase == "seek") {
                    val target = (player.playbackState.value.durationMs / 2).coerceAtLeast(10_000L)
                    android.util.Log.i("STEREO_CACHE_TEST", "[$phase] Seeking to $target ms...")
                    player.seekTo(target)
                }
                delay(5_000)
            }
            position = player.playbackState.value.currentPositionMs
            check(player.playbackState.value.status != PlayerStatus.ERROR) { player.playbackState.value.errorMessage.orEmpty() }
        } finally {
            player.pause()
            preferences.setAudioQualityMode(previousQuality)
        }

        // Allow DataSource close and validateAndPublish to finalize in background
        delay(2_000)
        cache.refresh()
        var localAfter = cache.fullyCached(id, AudioQualityMode.HIGH) != null
        if (!localAfter) {
            delay(2_000)
            cache.refresh()
            localAfter = cache.fullyCached(id, AudioQualityMode.HIGH) != null
        }
        val bytes = TrafficMeter.counters.snapshot().audioBytes - before
        android.util.Log.i("STEREO_CACHE_TEST", "[$phase] Result: localBefore=$localBefore, localAfter=$localAfter, audioNetworkBytes=$bytes, playedPositionMs=$position, stats=${cache.stats.value}")

        return JSONObject().put("trackId", id).put("audioNetworkBytes", bytes)
            .put("localBefore", localBefore).put("localAfter", localAfter)
            .put("playedPositionMs", position).put("completeSongs", cache.stats.value.completeSongs)
            .put("passed", localAfter && (if (phase == "first") bytes > 0 else bytes == 0L && position > 0))
    }

    private suspend fun fixture(): JSONObject = withContext(Dispatchers.IO) {
        // A real Android Media3 disk-cache fixture, kept separate from the user's song cache.
        val pcmBytes = 32_000
        val bytes = ByteBuffer.allocate(pcmBytes + 44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(pcmBytes + 36); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(8_000); putInt(16_000)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(pcmBytes)
            repeat(pcmBytes / 2) { putShort(((it % 32) * 200).toShort()) }
        }.array()
        val directory = File(cacheDir, "audio-verification-fixture")
        var engine = AudioCacheEngine(this@CacheVerificationActivity, directory, personalBuild = true,
            preferencesName = "stereo_cache_fixture")
        val networkBytes = AtomicLong()
        val upstream = DataSource.Factory {
            val source = ByteArrayDataSource(bytes)
            object : DataSource by source {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int = source.read(buffer, offset, length).also {
                    if (it > 0) networkBytes.addAndGet(it.toLong())
                }
            }
        }
        val stream = AudioStream("https://fixture.invalid/audio", "audio/wav", "pcm", 128_000,
            "wav", bytes.size.toLong(), providerId = "fixture")
        try {
            engine.clear()
            val key = engine.prepare("fixture", AudioQualityMode.HIGH, stream) ?: error("No cache key")
            fun read(position: Long, length: Long, local: Boolean = false): ByteArray {
                val source = engine.factory(upstream).createDataSource()
                val output = java.io.ByteArrayOutputStream()
                try {
                    source.open(DataSpec.Builder().setUri(if (local) "https://stereo-cache.invalid/$key" else stream.url)
                        .setKey(key).setPosition(position).setLength(length).build())
                    val buffer = ByteArray(4096)
                    while (true) { val count = source.read(buffer, 0, buffer.size); if (count < 0) break; output.write(buffer, 0, count) }
                } finally { source.close() }
                return output.toByteArray()
            }
            val half = bytes.size / 2
            check(read(0, half.toLong()).contentEquals(bytes.copyOfRange(0, half)))
            check(engine.fullyCached("fixture", AudioQualityMode.HIGH) == null) { "Partial entry published" }
            val beforePartial = networkBytes.get()
            read(0, half.toLong())
            check(networkBytes.get() == beforePartial) { "Partial range was redownloaded" }
            read(half.toLong(), (bytes.size - half).toLong())
            check(engine.fullyCached("fixture", AudioQualityMode.HIGH) != null) { "Complete WAV was not validated" }
            val firstBytes = networkBytes.get()
            check(read(0, bytes.size.toLong(), true).contentEquals(bytes))
            check(networkBytes.get() == firstBytes) { "Replay used upstream" }
            engine.close()
            engine = AudioCacheEngine(this@CacheVerificationActivity, directory, personalBuild = true,
                preferencesName = "stereo_cache_fixture")
            check(engine.fullyCached("fixture", AudioQualityMode.HIGH) != null) { "Restart lost audio" }
            check(read(half.toLong(), 100, true).contentEquals(bytes.copyOfRange(half, half + 100)))
            check(networkBytes.get() == firstBytes) { "Restart/seek used upstream" }
            val replayAdditionalBytes = networkBytes.get() - firstBytes
            engine.clear()
            check(engine.prepare("fixture", AudioQualityMode.HIGH, stream) == key)
            val beforeConcurrent = networkBytes.get()
            coroutineScope {
                listOf(async(Dispatchers.IO) { read(0, bytes.size.toLong()) },
                    async(Dispatchers.IO) { read(0, bytes.size.toLong()) }).awaitAll().forEach {
                    check(it.contentEquals(bytes))
                }
            }
            val concurrentBytes = networkBytes.get() - beforeConcurrent
            check(concurrentBytes == bytes.size.toLong()) { "Simultaneous readers downloaded duplicate bytes" }
            check(engine.fullyCached("fixture", AudioQualityMode.HIGH) != null)
            val span = directory.walkTopDown().first { it.isFile && it.name.endsWith(".exo") }
            java.io.RandomAccessFile(span, "rw").use { it.seek(0); it.write(0) }
            check(engine.fullyCached("fixture", AudioQualityMode.HIGH) == null) { "Same-size corruption was accepted" }
            check(engine.prepare("fixture", AudioQualityMode.HIGH, stream) == key)
            read(0, bytes.size.toLong())
            check(engine.fullyCached("fixture", AudioQualityMode.HIGH) != null) { "Corrupt entry could not recover" }
            engine.setPlaybackKey(key)
            val attached = engine.factory(upstream).createDataSource()
            attached.open(DataSpec.Builder().setUri("https://stereo-cache.invalid/$key").setKey(key).setLength(100).build())
            try {
                engine.clear()
                check(engine.fullyCached("fixture", AudioQualityMode.HIGH) == null)
                val output = ByteArray(100)
                check(attached.read(output, 0, output.size) == 100)
                check(output.contentEquals(bytes.copyOfRange(0, 100)))
            } finally { attached.close() }
            engine.setPlaybackKey(null)
            return@withContext JSONObject().put("passed", true).put("fixture", "WAV over real Media3 cache")
                .put("firstUpstreamBytes", firstBytes).put("repeatAdditionalBytes", replayAdditionalBytes)
                .put("concurrentUpstreamBytes", concurrentBytes)
                .put("checks", "partial reuse, complete promotion, replay, restart, seek, offline source, concurrent readers, corruption recovery, clear while leased")
        } finally { engine.close() }
    }
}
