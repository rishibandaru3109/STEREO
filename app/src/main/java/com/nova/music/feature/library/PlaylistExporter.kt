package com.nova.music.feature.library

import android.content.Context
import android.content.Intent
import com.nova.music.domain.model.Playlist
import com.nova.music.domain.model.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PlaylistExporter {

    fun exportToStereoJson(playlist: Playlist, tracks: List<Track>): String {
        val root = JSONObject()
        root.put("format", "STEREO_PLAYLIST_BACKUP")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        val pObj = JSONObject()
        pObj.put("id", playlist.id)
        pObj.put("name", playlist.name)
        pObj.put("description", playlist.description ?: "")
        pObj.put("coverArtworkUrl", playlist.coverArtworkUrl ?: "")
        pObj.put("createdAt", playlist.createdAt)
        pObj.put("updatedAt", playlist.updatedAt)
        pObj.put("trackCount", tracks.size)
        root.put("playlist", pObj)

        val tracksArray = JSONArray()
        tracks.forEachIndexed { index, track ->
            val tObj = JSONObject()
            tObj.put("position", index)
            tObj.put("id", track.id)
            tObj.put("title", track.title)
            tObj.put("artist", track.artist)
            tObj.put("album", track.album ?: "")
            tObj.put("durationSeconds", track.durationSeconds)
            tObj.put("thumbnailUrl", track.thumbnailUrl)
            tracksArray.put(tObj)
        }
        root.put("tracks", tracksArray)

        return root.toString(2)
    }

    fun exportToCsv(playlist: Playlist, tracks: List<Track>): String {
        val sb = StringBuilder()
        sb.append("Title,Artist,Album,DurationSeconds,TrackId\n")
        tracks.forEach { track ->
            val title = escapeCsv(track.title)
            val artist = escapeCsv(track.artist)
            val album = escapeCsv(track.album ?: "")
            sb.append("$title,$artist,$album,${track.durationSeconds},${escapeCsv(track.id)}\n")
        }
        return sb.toString()
    }

    fun exportToM3U8(playlist: Playlist, tracks: List<Track>): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        sb.append("#PLAYLIST:${playlist.name}\n")
        tracks.forEach { track ->
            sb.append("#EXTINF:${track.durationSeconds},${track.artist} - ${track.title}\n")
            sb.append("https://music.youtube.com/watch?v=${track.id}\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun shareExport(context: Context, playlistName: String, content: String, extension: String, mimeType: String) {
        val safeName = playlistName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${safeName}_$timestamp.$extension"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "STEREO Playlist Export - $playlistName")
            putExtra(Intent.EXTRA_TEXT, content)
            type = mimeType
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Playlist \"$playlistName\"")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
