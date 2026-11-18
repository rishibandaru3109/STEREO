package com.nova.music.domain.recommendation

import java.util.Locale

internal object LyricsLanguage {
    private val words = Regex("[\\p{L}\\p{M}']+")

    private val english = "the and you your this that with from have will when what where there they my me love don't can't into never just wanna of is it are for to be was were not in know how all we i'm see feel come like".split(' ').toSet()
    private val spanish = "que para con pero eres como donde cuando quiero estoy tiene tengo amor vida poco todo toda nunca solo siempre porque tus mis los las del una uno mas corazon".split(' ').toSet()
    private val portuguese = "voce não uma com para meu minha amor vida tudo todos quando quero mais sobre quem".split(' ').toSet()
    private val french = "vous avec pour dans cette mais tout tous faire comme sont nous mon sa ses chanson".split(' ').toSet()
    private val german = "nicht eine sich auch aber wenn hier schon wieder mein deine liebe noch immer oder dann doch".split(' ').toSet()
    private val italian = "questo quella anche sempre amore perche per me noi sono tutto solo vita cuore notte senza".split(' ').toSet()
    private val romanized = "hai hain nahi nahin mera meri tera teri tere mere tum tumse mujhe tujhe kya dil zindagi chahe hum dil se ishq pyaar saath jaana kaise jaane kuch hoon unakku enna unnai kadhal nenu nuvvu prema nee".split(' ').toSet()

    fun identify(lyrics: String): String? {
        val tokens = words.findAll(lyrics.lowercase(Locale.ROOT))
            .map { it.value }.take(3000).toList()
        if (tokens.size < 8) return null
        val distinct = tokens.toSet()
        val latin = tokens.filter { token ->
            val first = token.first()
            (first in 'a'..'z') || (first in '\u00C0'..'\u024F')
        }
        val result = mutableListOf<String>()

        // Script-based classification
        for ((name, range) in listOf(
            "Hindi" to ('\u0900'..'\u097F'),
            "Punjabi" to ('\u0A00'..'\u0A7F'),
            "Bengali" to ('\u0980'..'\u09FF'),
            "Gujarati" to ('\u0A80'..'\u0AFF'),
            "Tamil" to ('\u0B80'..'\u0BFF'),
            "Telugu" to ('\u0C00'..'\u0C7F'),
            "Kannada" to ('\u0C80'..'\u0CFF'),
            "Malayalam" to ('\u0D00'..'\u0D7F'),
            "Korean" to ('\uAC00'..'\uD7AF'),
            "Japanese" to ('\u3040'..'\u30FF'),
            "Mandarin Chinese" to ('\u4E00'..'\u9FFF'),
            "Thai" to ('\u0E00'..'\u0E7F'),
            "Burmese" to ('\u1000'..'\u109F'),
            "Hebrew" to ('\u0590'..'\u05FF'),
            "Arabic" to ('\u0600'..'\u06FF'),
            "Russian" to ('\u0400'..'\u04FF'),
            "Greek" to ('\u0370'..'\u03FF'),
            "Amharic" to ('\u1200'..'\u137F')
        )) {
            if (tokens.count { it.first() in range } >= 5) result.add(name)
        }

        val supported = listOf(
            '\u0900'..'\u097F', '\u0A00'..'\u0A7F', '\u0980'..'\u09FF', '\u0A80'..'\u0AFF',
            '\u0B80'..'\u0BFF', '\u0C00'..'\u0C7F', '\u0C80'..'\u0CFF', '\u0D00'..'\u0D7F',
            '\uAC00'..'\uD7AF', '\u3040'..'\u30FF', '\u4E00'..'\u9FFF', '\u0E00'..'\u0E7F',
            '\u1000'..'\u109F', '\u0590'..'\u05FF', '\u0600'..'\u06FF', '\u0400'..'\u04FF',
            '\u0370'..'\u03FF', '\u1200'..'\u137F'
        )
        if (tokens.count { token -> token.first() > '\u024F' && supported.none { token.first() in it } } >= 4) return null

        if (latin.size >= 8) {
            val en = distinct.count { it in english }
            val es = distinct.count { it in spanish }
            val pt = distinct.count { it in portuguese }
            val fr = distinct.count { it in french }
            val de = distinct.count { it in german }
            val itCount = distinct.count { it in italian }

            if (distinct.count { it in romanized } >= 3) return null

            when {
                en >= 5 && es >= 5 -> result.addAll(listOf("English", "Spanish"))
                en >= 5 && fr >= 5 -> result.addAll(listOf("English", "French"))
                en >= 5 && pt >= 5 -> result.addAll(listOf("English", "Portuguese"))
                en >= 5 && es <= 2 && fr <= 2 && pt <= 2 && de <= 2 && itCount <= 2 &&
                    latin.count { it in english } >= (latin.size / 4).coerceAtLeast(1) -> result.add("English")
                es >= 5 && en <= 2 && latin.count { it in spanish } >= (latin.size / 5).coerceAtLeast(1) -> result.add("Spanish")
                pt >= 5 && en <= 2 && latin.count { it in portuguese } >= (latin.size / 5).coerceAtLeast(1) -> result.add("Portuguese")
                fr >= 5 && en <= 2 && latin.count { it in french } >= (latin.size / 5).coerceAtLeast(1) -> result.add("French")
                de >= 5 && en <= 2 && latin.count { it in german } >= (latin.size / 5).coerceAtLeast(1) -> result.add("German")
                itCount >= 5 && en <= 2 && latin.count { it in italian } >= (latin.size / 5).coerceAtLeast(1) -> result.add("Italian")
                result.isEmpty() || latin.size >= tokens.size / 3 -> return null
            }
        }
        return result.distinct().takeIf { it.isNotEmpty() }?.joinToString("+")
    }
}
