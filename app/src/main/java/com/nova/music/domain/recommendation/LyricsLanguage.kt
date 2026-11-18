package com.nova.music.domain.recommendation

import java.util.Locale

internal object LyricsLanguage {
    private val words = Regex("\\p{L}[\\p{L}\\p{M}'’]*")
    private val english = "the and you your this that with from have will when what where there they my me love don't can't into never just wanna of is it are for to be was were not in know how all we i'm see feel come like".split(' ').toSet()
    private val spanish = "que para con pero eres como donde cuando quiero estoy tiene tengo amor vida poco todo toda nunca solo siempre porque tus mis los las del una uno más corazón".split(' ').toSet()
    private val romanized = "hai hain nahi nahin mera meri tera teri tere mere tum tumse mujhe tujhe kya dil zindagi chahe hum dil se ishq pyaar saath jaana kaise jaane kuch hoon unakku enna unnai kadhal nenu nuvvu prema nee".split(' ').toSet()

    fun identify(lyrics: String): String? {
        val tokens = words.findAll(lyrics.lowercase(Locale.ROOT).replace('’', '\''))
            .map { it.value }.take(3000).toList()
        if (tokens.size < 8) return null
        val distinct = tokens.toSet()
        val latin = tokens.filter { it.first() in 'a'..'z' || it.first() in 'à'..'ÿ' }
        val result = mutableListOf<String>()
        for ((name, range) in listOf(
            "Hindi" to ('\u0900'..'\u097f'),
            "Punjabi" to ('\u0a00'..'\u0a7f'),
            "Tamil" to ('\u0b80'..'\u0bff'),
            "Telugu" to ('\u0c00'..'\u0c7f'),
            "Malayalam" to ('\u0d00'..'\u0d7f'),
            "Korean" to ('\uac00'..'\ud7af')
        )) {
            if (tokens.count { it.first() in range } >= 5) result.add(name)
        }
        // Any unclassified script can indicate lyrics outside the supported selection.
        val supported = listOf('\u0900'..'\u097f', '\u0a00'..'\u0a7f', '\u0b80'..'\u0bff',
            '\u0c00'..'\u0c7f', '\u0d00'..'\u0d7f', '\uac00'..'\ud7af')
        if (tokens.count { token -> token.first() > 'ÿ' && supported.none { token.first() in it } } >= 4) return null
        if (latin.size >= 8) {
            val en = distinct.count { it in english }
            val es = distinct.count { it in spanish }
            if (distinct.count { it in romanized } >= 3) return null
            when {
                en >= 5 && es >= 5 -> result.addAll(listOf("English", "Spanish"))
                en >= 5 && es <= 2 && latin.count { it in english } >= latin.size / 4 -> result.add("English")
                es >= 5 && en <= 2 && latin.count { it in spanish } >= latin.size / 5 -> result.add("Spanish")
                result.isEmpty() || latin.size >= tokens.size / 3 -> return null
            }
        }
        return result.takeIf { it.isNotEmpty() }?.joinToString("+")
    }
}
