package com.nova.music.core.cache

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest

data class AudioRangeFile(val position: Long, val length: Long, val file: File)

/** Reads the existing committed Media3 spans in place; promotion needs no second audio copy. */
object AudioFileRanges {
    fun complete(pieces: List<AudioRangeFile>, expectedLength: Long): Boolean {
        if (expectedLength <= 0 || pieces.isEmpty()) return false
        var end = 0L
        for (piece in pieces.sortedBy { it.position }) {
            if (piece.position != end || piece.length <= 0 || piece.file.length() != piece.length) return false
            end += piece.length
            if (end > expectedLength) return false
        }
        return end == expectedLength
    }

    fun read(pieces: List<AudioRangeFile>, size: Long, position: Long, buffer: ByteArray, offset: Int, count: Int): Int {
        require(position >= 0 && offset >= 0 && count >= 0 && offset <= buffer.size - count)
        if (count == 0) return 0
        if (position >= size) return -1
        val piece = pieces.firstOrNull { position >= it.position && position - it.position < it.length }
            ?: throw IOException("Missing cached audio range at $position")
        val amount = minOf(count.toLong(), piece.length - (position - piece.position), size - position).toInt()
        RandomAccessFile(piece.file, "r").use {
            it.seek(position - piece.position)
            it.readFully(buffer, offset, amount)
        }
        return amount
    }

    fun digest(pieces: List<AudioRangeFile>, size: Long): String {
        check(complete(pieces, size)) { "Incomplete audio ranges" }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var position = 0L
        while (position < size) {
            val count = read(pieces, size, position, buffer, 0, buffer.size)
            check(count > 0)
            digest.update(buffer, 0, count)
            position += count
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
