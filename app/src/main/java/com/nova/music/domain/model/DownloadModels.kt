package com.nova.music.domain.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

data class DownloadItem(
    val track: Track,
    val status: DownloadStatus,
    val quality: AudioQualityMode = AudioQualityMode.HIGH,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val filePath: String = "",
    val errorMessage: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        else if (status == DownloadStatus.COMPLETED) 1f else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val formattedDownloadedSize: String
        get() = formatBytes(downloadedBytes)

    val formattedTotalSize: String
        get() = formatBytes(totalBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 MB"
            val mb = bytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                val gb = mb / 1024.0
                String.format("%.1f GB", gb)
            } else {
                String.format("%.1f MB", mb)
            }
        }
    }
}

data class StorageUsage(
    val totalBytes: Long,
    val formattedString: String,
    val trackCount: Int
)
