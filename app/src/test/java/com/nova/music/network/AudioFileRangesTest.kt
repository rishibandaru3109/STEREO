package com.nova.music.network

import com.nova.music.core.cache.AudioFileRanges
import com.nova.music.core.cache.AudioRangeFile
import com.nova.music.domain.model.AudioStream
import com.nova.music.domain.model.withRepresentationMetadata
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class AudioFileRangesTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun span(position: Long, data: ByteArray): AudioRangeFile {
        val file = temporary.newFile()
        file.writeBytes(data)
        return AudioRangeFile(position, data.size.toLong(), file)
    }

    @Test fun completedWritesCoverEveryByteAndReplayFromFiles() {
        val expected = ByteArray(100_000) { (it % 251).toByte() }
        val pieces = listOf(span(0, expected.copyOfRange(0, 40_000)), span(40_000, expected.copyOfRange(40_000, 100_000)))
        assertTrue(AudioFileRanges.complete(pieces, expected.size.toLong()))
        val output = ByteArray(expected.size)
        var position = 0
        while (position < output.size) position += AudioFileRanges.read(pieces, output.size.toLong(),
            position.toLong(), output, position, output.size - position)
        assertArrayEquals(expected, output)
        assertEquals(-1, AudioFileRanges.read(pieces, output.size.toLong(), output.size.toLong(), ByteArray(1), 0, 1))
    }

    @Test fun partialGappedOverlappingAndUnknownLengthAudioNeverPublish() {
        val a = span(0, ByteArray(10))
        assertFalse(AudioFileRanges.complete(listOf(a), 20))
        assertFalse(AudioFileRanges.complete(listOf(a, span(11, ByteArray(9))), 20))
        assertFalse(AudioFileRanges.complete(listOf(a, span(9, ByteArray(11))), 20))
        assertFalse(AudioFileRanges.complete(listOf(a), 0))
        assertFalse(AudioFileRanges.complete(emptyList(), 10))
    }

    @Test fun cachedSeekingReadsTheExactRequestedRange() {
        val pieces = listOf(span(0, byteArrayOf(1, 2, 3)), span(3, byteArrayOf(4, 5, 6)))
        val output = ByteArray(2)
        assertEquals(2, AudioFileRanges.read(pieces, 6, 4, output, 0, 2))
        assertArrayEquals(byteArrayOf(5, 6), output)
    }

    @Test fun truncationAndMissingFilesAreDetected() {
        val a = span(0, ByteArray(100))
        a.file.writeBytes(ByteArray(99))
        assertFalse(AudioFileRanges.complete(listOf(a), 100))
        a.file.delete()
        assertFalse(AudioFileRanges.complete(listOf(a), 100))
    }

    @Test fun sameLengthCorruptionChangesTheValidatedDigest() {
        val a = span(0, ByteArray(100) { 3 })
        val hash = AudioFileRanges.digest(listOf(a), 100)
        a.file.writeBytes(ByteArray(100) { 4 })
        assertNotEquals(hash, AudioFileRanges.digest(listOf(a), 100))
    }

    @Test fun reopeningExistingSpansPreservesAudioAndDigest() {
        val a = span(0, ByteArray(100) { it.toByte() })
        val hash = AudioFileRanges.digest(listOf(a), 100)
        val reopened = AudioRangeFile(a.position, a.length, java.io.File(a.file.absolutePath))
        assertTrue(AudioFileRanges.complete(listOf(reopened), 100))
        assertEquals(hash, AudioFileRanges.digest(listOf(reopened), 100))
    }

    @Test(expected = IOException::class) fun missingSeekRangeFailsRatherThanReadingUninitializedBytes() {
        AudioFileRanges.read(listOf(span(10, ByteArray(10))), 20, 0, ByteArray(4), 0, 4)
    }

    @Test fun extractorUrlHintsRetainLengthRevisionAndLanguageWithoutTokens() {
        val stream = AudioStream("https://r1.googlevideo.com/videoplayback?clen=1024&lmt=version1&xtags=lang%3Den&expire=123&sig=secret")
            .withRepresentationMetadata()
        assertEquals(1024L, stream.contentLength)
        assertEquals("version1", stream.representationRevision)
        assertEquals("lang=en", stream.audioTrackId)
    }
}
