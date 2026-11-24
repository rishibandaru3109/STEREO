package com.nova.music.library

import com.nova.music.feature.library.importing.ImportSource
import com.nova.music.feature.library.importing.PlaylistImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportParserTest {

    @Test
    fun testParseM3uStandard() {
        val m3uContent = """
            #EXTM3U
            #PLAYLIST:Workout Hits
            #EXTINF:240,Coldplay - Viva La Vida
            https://example.com/audio1.mp3
            #EXTINF:180,The Weeknd - Blinding Lights
            https://example.com/audio2.mp3
        """.trimIndent()

        val result = PlaylistImportParser.parseM3u(m3uContent)
        assertTrue(result.isSuccess)

        val parsed = result.getOrThrow()
        assertEquals("Workout Hits", parsed.title)
        assertEquals(ImportSource.M3U8, parsed.source)
        assertEquals(2, parsed.tracks.size)

        assertEquals("Viva La Vida", parsed.tracks[0].title)
        assertEquals("Coldplay", parsed.tracks[0].artist)
        assertEquals(240L, parsed.tracks[0].durationSeconds)

        assertEquals("Blinding Lights", parsed.tracks[1].title)
        assertEquals("The Weeknd", parsed.tracks[1].artist)
        assertEquals(180L, parsed.tracks[1].durationSeconds)
    }

    @Test
    fun testParseCsvStandard() {
        val csvContent = """
            Title,Artist,Album,Duration
            "Viva La Vida","Coldplay","Viva la Vida or Death and All His Friends",242
            "Starboy","The Weeknd","Starboy",230
        """.trimIndent()

        val result = PlaylistImportParser.parseCsv(csvContent)
        assertTrue(result.isSuccess)

        val parsed = result.getOrThrow()
        assertEquals(ImportSource.CSV, parsed.source)
        assertEquals(2, parsed.tracks.size)

        assertEquals("Viva La Vida", parsed.tracks[0].title)
        assertEquals("Coldplay", parsed.tracks[0].artist)
        assertEquals("Viva la Vida or Death and All His Friends", parsed.tracks[0].album)
        assertEquals(242L, parsed.tracks[0].durationSeconds)

        assertEquals("Starboy", parsed.tracks[1].title)
        assertEquals("The Weeknd", parsed.tracks[1].artist)
        assertEquals("Starboy", parsed.tracks[1].album)
        assertEquals(230L, parsed.tracks[1].durationSeconds)
    }

    @Test
    fun testParseStereoNativeBackupJson() {
        val jsonContent = """
            {
              "format": "STEREO_PLAYLIST_BACKUP",
              "version": 1,
              "playlist": {
                "name": "My Road Trip",
                "description": "Songs for the highway",
                "coverArtworkUrl": "https://example.com/cover.jpg"
              },
              "tracks": [
                {
                  "id": "t1",
                  "title": "Highway to Hell",
                  "artist": "AC/DC",
                  "album": "Highway to Hell",
                  "durationSeconds": 208,
                  "thumbnailUrl": "https://example.com/t1.jpg"
                },
                {
                  "id": "t2",
                  "title": "Born to Run",
                  "artist": "Bruce Springsteen",
                  "album": "Born to Run",
                  "durationSeconds": 270,
                  "thumbnailUrl": "https://example.com/t2.jpg"
                }
              ]
            }
        """.trimIndent()

        val result = PlaylistImportParser.parseJson(jsonContent)
        assertTrue(result.isSuccess)

        val parsed = result.getOrThrow()
        assertEquals("My Road Trip", parsed.title)
        assertEquals("Songs for the highway", parsed.description)
        assertEquals("https://example.com/cover.jpg", parsed.coverUrl)
        assertEquals(ImportSource.STEREO_NATIVE, parsed.source)
        assertEquals(2, parsed.tracks.size)

        assertEquals("Highway to Hell", parsed.tracks[0].title)
        assertEquals("AC/DC", parsed.tracks[0].artist)
        assertEquals("t1", parsed.tracks[0].sourceTrackId)

        assertEquals("Born to Run", parsed.tracks[1].title)
        assertEquals("Bruce Springsteen", parsed.tracks[1].artist)
        assertEquals("t2", parsed.tracks[1].sourceTrackId)
    }

    @Test
    fun testParseGenericJsonArray() {
        val jsonContent = """
            [
              {"title": "Song One", "artist": "Artist One", "durationSeconds": 150},
              {"name": "Song Two", "subtitle": "Artist Two", "duration": 200000}
            ]
        """.trimIndent()

        val result = PlaylistImportParser.parseJson(jsonContent)
        assertTrue(result.isSuccess)

        val parsed = result.getOrThrow()
        assertEquals(2, parsed.tracks.size)
        assertEquals("Song One", parsed.tracks[0].title)
        assertEquals("Artist One", parsed.tracks[0].artist)
        assertEquals(150L, parsed.tracks[0].durationSeconds)

        assertEquals("Song Two", parsed.tracks[1].title)
        assertEquals("Artist Two", parsed.tracks[1].artist)
        assertEquals(200L, parsed.tracks[1].durationSeconds)
    }
}
