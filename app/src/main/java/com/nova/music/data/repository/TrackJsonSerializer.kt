package com.nova.music.data.repository

import com.nova.music.domain.model.Track
import org.json.JSONArray
import org.json.JSONObject

object TrackJsonSerializer {

    fun serializeTracks(tracks: List<Track>): String {
        val array = JSONArray()
        for (track in tracks) {
            val obj = JSONObject()
            obj.put("id", track.id)
            obj.put("title", track.title)
            obj.put("artist", track.artist)
            obj.put("album", track.album ?: "")
            obj.put("durationSeconds", track.durationSeconds)
            obj.put("thumbnailUrl", track.thumbnailUrl)
            obj.put("streamUrl", track.streamUrl ?: "")
            obj.put("lyricsLanguage", track.lyricsLanguage ?: "")
            obj.put("isSpokenWord", track.isSpokenWord)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeTracks(json: String): List<Track> {
        val list = mutableListOf<Track>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Track(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.optString("album").takeIf { it.isNotBlank() },
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        streamUrl = obj.optString("streamUrl").takeIf { it.isNotBlank() },
                        lyricsLanguage = obj.optString("lyricsLanguage").takeIf { it.isNotBlank() },
                        isSpokenWord = obj.optBoolean("isSpokenWord")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
