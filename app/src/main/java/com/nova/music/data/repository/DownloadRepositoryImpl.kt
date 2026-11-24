package com.nova.music.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nova.music.core.database.dao.DownloadDao
import com.nova.music.core.database.dao.SynapseDao
import com.nova.music.core.database.entity.DownloadEntity
import com.nova.music.core.database.entity.PlaylistDownloadEntity
import com.nova.music.core.database.entity.SynapseEventEntity
import com.nova.music.domain.model.AudioQualityMode
import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.DownloadItem
import com.nova.music.domain.model.DownloadStatus
import com.nova.music.domain.model.StorageUsage
import com.nova.music.domain.model.Track
import com.nova.music.domain.repository.DownloadRepository
import com.nova.music.domain.repository.MusicExtractor
import com.nova.music.domain.repository.MusicPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val extractor: MusicExtractor,
    private val okHttpClient: OkHttpClient,
    private val synapseDao: SynapseDao? = null,
    private val preferencesRepository: MusicPreferencesRepository? = null
) : DownloadRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadSemaphore = Semaphore(2)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val downloadDir: File = context.filesDir.resolve("offline_downloads").apply { mkdirs() }

    init {
        scope.launch {
            try {
                val active = downloadDao.getActiveDownloads()
                active.forEach { entity ->
                    val track = Track(
                        id = entity.trackId,
                        title = entity.title,
                        artist = entity.artist,
                        album = entity.album,
                        durationSeconds = entity.durationSeconds,
                        thumbnailUrl = entity.thumbnailUrl
                    )
                    val quality = runCatching { AudioQualityMode.valueOf(entity.quality) }.getOrDefault(AudioQualityMode.HIGH)
                    enqueueDownloadJob(track, quality)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recovering active downloads: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "STEREO_DOWNLOADS"
    }

    override val completedDownloads: Flow<List<DownloadItem>> =
        downloadDao.observeCompletedDownloads().map { list -> list.map { it.toDomain() } }

    override val activeDownloads: Flow<List<DownloadItem>> =
        downloadDao.observeActiveDownloads().map { list -> list.map { it.toDomain() } }

    override val failedDownloads: Flow<List<DownloadItem>> =
        downloadDao.observeFailedDownloads().map { list -> list.map { it.toDomain() } }

    override val allDownloads: Flow<List<DownloadItem>> =
        downloadDao.observeAllDownloads().map { list -> list.map { it.toDomain() } }

    override val downloadedTrackIds: Flow<Set<String>> =
        downloadDao.observeDownloadedTrackIds().map { it.toSet() }.distinctUntilChanged()

    override val storageUsage: Flow<StorageUsage> =
        combine(downloadDao.observeTotalStorageBytes(), downloadDao.observeCompletedDownloads()) { bytes, list ->
            val total = bytes ?: 0L
            StorageUsage(
                totalBytes = total,
                formattedString = DownloadItem.formatBytes(total),
                trackCount = list.size
            )
        }

    override fun getDownloadItem(trackId: String): Flow<DownloadItem?> =
        downloadDao.observeDownload(trackId).map { it?.toDomain() }

    override suspend fun isTrackDownloaded(trackId: String): Boolean {
        val entity = downloadDao.getDownload(trackId) ?: return false
        if (entity.status != "COMPLETED") return false
        val file = File(entity.filePath)
        return file.exists() && file.length() > 0
    }

    override suspend fun getCompletedDownloadStream(trackId: String): AudioStream? {
        val entity = downloadDao.getDownload(trackId) ?: return null
        if (entity.status != "COMPLETED" || entity.filePath.isBlank()) return null
        val file = File(entity.filePath)
        if (!file.exists() || file.length() <= 0) return null

        return AudioStream(
            url = Uri.fromFile(file).toString(),
            mimeType = entity.mimeType,
            codec = if (entity.mimeType.contains("mp4") || entity.mimeType.contains("aac")) "aac" else "opus",
            bitrate = if (entity.quality == "HIGH") 160000 else 128000,
            formatId = "downloaded_local",
            contentLength = file.length(),
            isAudioOnly = true,
            isLocalCache = true
        )
    }

    override suspend fun downloadTrack(track: Track, quality: AudioQualityMode) {
        val existing = downloadDao.getDownload(track.id)
        if (existing?.status == "COMPLETED" && File(existing.filePath).let { it.exists() && it.length() > 0 }) {
            Log.i(TAG, "Track ${track.id} already downloaded; skipping.")
            return
        }

        val entity = DownloadEntity(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = track.durationSeconds,
            thumbnailUrl = track.thumbnailUrl,
            quality = quality.name,
            status = DownloadStatus.QUEUED.name,
            downloadedBytes = 0L,
            totalBytes = 0L,
            filePath = "",
            createdAt = System.currentTimeMillis()
        )
        downloadDao.insertOrUpdate(entity)
        enqueueDownloadJob(track, quality)
    }

    override suspend fun downloadTracks(tracks: List<Track>, quality: AudioQualityMode) {
        tracks.forEach { downloadTrack(it, quality) }
    }

    override suspend fun downloadPlaylist(playlistId: String, tracks: List<Track>, quality: AudioQualityMode) {
        tracks.forEach { track ->
            downloadDao.insertPlaylistDownload(
                PlaylistDownloadEntity(playlistId = playlistId, trackId = track.id)
            )
            downloadTrack(track, quality)
        }
    }

    private fun enqueueDownloadJob(track: Track, quality: AudioQualityMode) {
        activeJobs[track.id]?.cancel()

        val job = scope.launch {
            try {
                downloadSemaphore.withPermit {
                    executeDownload(track, quality)
                }
            } catch (cancelled: CancellationException) {
                Log.i(TAG, "Download job cancelled for ${track.id}")
            } catch (t: Throwable) {
                Log.e(TAG, "Download error for ${track.id}: ${t.message}", t)
                downloadDao.markFailed(track.id, t.message ?: "Unknown error")
            } finally {
                activeJobs.remove(track.id)
            }
        }
        activeJobs[track.id] = job
    }

    private fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
               caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private suspend fun executeDownload(track: Track, quality: AudioQualityMode) {
        val wifiOnly = try {
            preferencesRepository?.isDownloadWifiOnly?.first() ?: false
        } catch (_: Exception) {
            false
        }
        if (wifiOnly && !isWifiConnected()) {
            Log.i(TAG, "Download paused for ${track.id}: Wi-Fi only enabled but not on Wi-Fi")
            downloadDao.markFailed(track.id, "Waiting for Wi-Fi connection")
            return
        }

        downloadDao.updateProgress(track.id, DownloadStatus.DOWNLOADING.name, 0L, 0L)
        Log.i(TAG, "Resolving stream for download: ${track.id} (${track.title})")

        val streamResult = extractor.resolveAudioStream(track.id, quality)
        if (streamResult.isFailure) {
            val err = streamResult.exceptionOrNull()?.message ?: "Failed to resolve stream"
            downloadDao.markFailed(track.id, err)
            return
        }
        val stream = streamResult.getOrThrow()
        val extension = when {
            stream.mimeType.contains("mp4") || stream.mimeType.contains("aac") || stream.codec == "aac" -> "m4a"
            else -> "opus"
        }

        val tempFile = File(downloadDir, "temp_${track.id}.tmp")
        val finalFile = File(downloadDir, "${track.id}.$extension")

        val reqBuilder = Request.Builder().url(stream.url)
        stream.headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        val call = okHttpClient.newCall(reqBuilder.build())
        val response = call.execute()

        if (!response.isSuccessful) {
            downloadDao.markFailed(track.id, "HTTP ${response.code} download failed")
            response.close()
            return
        }

        val body = response.body ?: run {
            downloadDao.markFailed(track.id, "Empty response body")
            response.close()
            return
        }

        val totalBytes = if (body.contentLength() > 0) body.contentLength() else stream.contentLength

        try {
            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L
                    var lastUpdate = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 300) {
                            downloadDao.updateProgress(
                                track.id,
                                DownloadStatus.DOWNLOADING.name,
                                totalDownloaded,
                                totalBytes
                            )
                            lastUpdate = now
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.length() <= 0) {
                tempFile.delete()
                downloadDao.markFailed(track.id, "Downloaded file was 0 bytes")
                return
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val completedLength = finalFile.length()
            downloadDao.markCompleted(
                trackId = track.id,
                status = DownloadStatus.COMPLETED.name,
                downloadedBytes = completedLength,
                totalBytes = completedLength,
                filePath = finalFile.absolutePath,
                completedAt = System.currentTimeMillis()
            )
            Log.i(TAG, "Download COMPLETED for ${track.title}: $completedLength bytes saved at ${finalFile.absolutePath}")

            // Log explicit download intent to SYNAPSE Recommendation Engine
            try {
                synapseDao?.insertEvent(
                    SynapseEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        trackId = track.id,
                        timestamp = System.currentTimeMillis(),
                        listeningDurationMs = (track.durationSeconds * 1000L).coerceAtLeast(180_000L),
                        totalTrackDurationMs = (track.durationSeconds * 1000L).coerceAtLeast(180_000L),
                        completionRatio = 1.0f,
                        isManualSkip = false,
                        playbackContext = "explicit_download"
                    )
                )
            } catch (_: Throwable) {}

        } catch (e: Exception) {
            tempFile.delete()
            throw e
        } finally {
            response.close()
        }
    }

    override suspend fun cancelDownload(trackId: String) {
        activeJobs[trackId]?.cancel()
        activeJobs.remove(trackId)
        File(downloadDir, "temp_${trackId}.tmp").delete()
        downloadDao.deleteDownload(trackId)
    }

    override suspend fun retryDownload(trackId: String) {
        val existing = downloadDao.getDownload(trackId) ?: return
        val track = Track(
            id = existing.trackId,
            title = existing.title,
            artist = existing.artist,
            album = existing.album,
            durationSeconds = existing.durationSeconds,
            thumbnailUrl = existing.thumbnailUrl
        )
        val quality = runCatching { AudioQualityMode.valueOf(existing.quality) }.getOrDefault(AudioQualityMode.HIGH)
        downloadTrack(track, quality)
    }

    override suspend fun deleteDownload(trackId: String) {
        cancelDownload(trackId)
        downloadDao.deleteDownload(trackId)
        listOf("opus", "m4a", "webm", "mp3").forEach { ext ->
            File(downloadDir, "$trackId.$ext").delete()
        }
    }

    override suspend fun clearFailedDownloads() {
        downloadDao.clearFailedDownloads()
    }

    override suspend fun deleteAllDownloads() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        downloadDao.deleteAllDownloads()
        downloadDir.listFiles()?.forEach { it.delete() }
    }

    private fun DownloadEntity.toDomain(): DownloadItem {
        val statusEnum = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.FAILED)
        val qualityEnum = runCatching { AudioQualityMode.valueOf(quality) }.getOrDefault(AudioQualityMode.HIGH)
        return DownloadItem(
            track = Track(
                id = trackId,
                title = title,
                artist = artist,
                album = album,
                durationSeconds = durationSeconds,
                thumbnailUrl = thumbnailUrl
            ),
            status = statusEnum,
            quality = qualityEnum,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            filePath = filePath,
            errorMessage = errorMessage,
            completedAt = completedAt,
            createdAt = createdAt
        )
    }
}
