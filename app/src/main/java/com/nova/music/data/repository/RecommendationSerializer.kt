package com.nova.music.data.repository

import com.nova.music.domain.model.RecommendationMix
import com.nova.music.domain.model.RecommendationSection
import com.nova.music.domain.model.SectionType
import org.json.JSONArray
import org.json.JSONObject

object RecommendationSerializer {

    fun serializeMixes(mixes: List<RecommendationMix>): String {
        val array = JSONArray()
        for (mix in mixes) {
            val obj = JSONObject()
            obj.put("id", mix.id)
            obj.put("title", mix.title)
            obj.put("subtitle", mix.subtitle)
            obj.put("tracksJson", TrackJsonSerializer.serializeTracks(mix.tracks))

            val artArray = JSONArray()
            mix.coverArtUrls.forEach { artArray.put(it) }
            obj.put("coverArtUrls", artArray)

            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeMixes(json: String): List<RecommendationMix> {
        val list = mutableListOf<RecommendationMix>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val subtitle = obj.getString("subtitle")
                val tracks = TrackJsonSerializer.deserializeTracks(obj.getString("tracksJson"))

                val artArray = obj.optJSONArray("coverArtUrls")
                val artList = mutableListOf<String>()
                if (artArray != null) {
                    for (j in 0 until artArray.length()) {
                        artList.add(artArray.getString(j))
                    }
                }

                list.add(
                    RecommendationMix(
                        id = id,
                        title = title,
                        subtitle = subtitle,
                        tracks = tracks,
                        coverArtUrls = artList
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
