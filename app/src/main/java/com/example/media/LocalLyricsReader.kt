package com.example.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

enum class LyricsSource {
    EMBEDDED_ID3,
    SIDECAR_LRC,
    LRCLIB,
    NONE
}

data class ResolvedLyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val source: LyricsSource
)

/**
 * Local-first lyrics extractor implementing EMBEDDED_FIRST:
 * 1. Embedded ID3v2 USLT (unsynchronized) & SYLT (synchronized) frames from local MP3 files
 * 2. Sibling .lrc files beside local audio tracks
 */
object LocalLyricsReader {

    private val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")

    fun readLocalLyrics(
        context: Context,
        mediaUriStr: String?,
        title: String,
        artist: String
    ): ResolvedLyrics? {
        val localAudioFile = resolveLocalFile(context, mediaUriStr, title, artist)
        if (localAudioFile != null && localAudioFile.exists() && localAudioFile.canRead()) {
            // 1. Embedded ID3v2 SYLT / USLT first
            val id3Lyrics = readId3v2Lyrics(localAudioFile)
            if (id3Lyrics != null && id3Lyrics.lines.isNotEmpty()) {
                return id3Lyrics
            }
            // 2. Sibling .lrc file
            val parent = localAudioFile.parentFile
            if (parent != null) {
                val lrcFile = File(parent, localAudioFile.nameWithoutExtension + ".lrc")
                if (lrcFile.exists() && lrcFile.canRead() && lrcFile.length() in 1L..256_000L) {
                    val raw = runCatching { lrcFile.readText(Charsets.UTF_8) }.getOrNull()
                    val parsed = parseLyricsText(raw)
                    if (parsed.isNotEmpty()) {
                        val isSynced = parsed.any { it.timestampMs >= 0L }
                        return ResolvedLyrics(parsed, isSynced, LyricsSource.SIDECAR_LRC)
                    }
                }
            }
        }
        return null
    }

    fun parseLyricsText(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val timed = mutableListOf<LyricLine>()
        val plain = mutableListOf<LyricLine>()
        raw.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val match = lrcRegex.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    timed.add(LyricLine((min * 60_000L) + (sec * 1000L) + msPart, text))
                }
            } else if (!trimmed.startsWith("[ar:", ignoreCase = true) &&
                !trimmed.startsWith("[ti:", ignoreCase = true) &&
                !trimmed.startsWith("[al:", ignoreCase = true) &&
                !trimmed.startsWith("[by:", ignoreCase = true)
            ) {
                plain.add(LyricLine(-1L, trimmed))
            }
        }
        return if (timed.isNotEmpty()) timed.sortedBy { it.timestampMs } else plain
    }

    @Suppress("DEPRECATION")
    private fun resolveLocalFile(
        context: Context,
        mediaUriStr: String?,
        title: String,
        artist: String
    ): File? {
        if (!mediaUriStr.isNullOrBlank()) {
            val uri = runCatching { Uri.parse(mediaUriStr) }.getOrNull()
            if (uri != null) {
                if (uri.scheme.equals("file", ignoreCase = true)) {
                    uri.path?.let { File(it) }?.takeIf { it.exists() }?.let { return it }
                }
                if (uri.scheme.equals("content", ignoreCase = true)) {
                    runCatching {
                        context.contentResolver.query(
                            uri,
                            arrayOf(MediaStore.Audio.Media.DATA),
                            null,
                            null,
                            null
                        )?.use { c ->
                            if (c.moveToFirst()) {
                                val idx = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                                if (idx >= 0) {
                                    c.getString(idx)?.let { File(it) }?.takeIf { it.exists() }?.let { return it }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (title.isBlank()) return null
        return runCatching {
            val sel = "${MediaStore.Audio.Media.TITLE} = ?"
            val args = arrayOf(title)
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ARTIST),
                sel,
                args,
                null
            )?.use { c ->
                val dataIdx = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val artistIdx = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                while (c.moveToNext()) {
                    val path = if (dataIdx >= 0) c.getString(dataIdx) else null
                    val rowArtist = if (artistIdx >= 0) c.getString(artistIdx).orEmpty() else ""
                    if (path != null && (artist.isBlank() || rowArtist.contains(artist, ignoreCase = true))) {
                        val f = File(path)
                        if (f.exists()) return@runCatching f
                    }
                }
                null
            }
        }.getOrNull()
    }

    internal fun readId3v2Lyrics(file: File): ResolvedLyrics? {
        return runCatching {
            if (file.length() < 16L) return@runCatching null
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(10)
                raf.readFully(header)
                if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                    return@runCatching null
                }
                val majorVersion = header[3].toInt() and 0xFF
                if (majorVersion < 3 || majorVersion > 4) return@runCatching null
                val flags = header[5].toInt() and 0xFF
                val tagSize = decodeSyncSafeInt(header, 6).coerceAtMost(2_000_000)
                if (tagSize <= 10) return@runCatching null

                val tagBytes = ByteArray(tagSize)
                raf.readFully(tagBytes)
                var pos = 0
                if ((flags and 0x40) != 0 && tagBytes.size >= 4) {
                    val extSize = if (majorVersion == 4) decodeSyncSafeInt(tagBytes, 0) else decodeIntBE(tagBytes, 0)
                    pos += extSize.coerceAtLeast(4)
                }

                var bestSynced: ResolvedLyrics? = null
                var bestPlain: ResolvedLyrics? = null

                while (pos + 10 <= tagBytes.size) {
                    val frameId = String(tagBytes, pos, 4, Charsets.ISO_8859_1)
                    if (frameId.all { it == '\u0000' }) break
                    val frameSize = if (majorVersion == 4) {
                        decodeSyncSafeInt(tagBytes, pos + 4)
                    } else {
                        decodeIntBE(tagBytes, pos + 4)
                    }
                    pos += 10
                    if (frameSize <= 0 || pos + frameSize > tagBytes.size) break

                    when (frameId) {
                        "SYLT" -> {
                            val parsed = decodeSyltFrame(tagBytes, pos, frameSize)
                            if (parsed.isNotEmpty()) {
                                bestSynced = ResolvedLyrics(parsed, isSynced = true, source = LyricsSource.EMBEDDED_ID3)
                            }
                        }
                        "USLT" -> {
                            val raw = decodeUsltFrame(tagBytes, pos, frameSize)
                            val parsed = parseLyricsText(raw)
                            if (parsed.isNotEmpty()) {
                                val synced = parsed.any { it.timestampMs >= 0L }
                                val res = ResolvedLyrics(parsed, synced, LyricsSource.EMBEDDED_ID3)
                                if (synced && bestSynced == null) bestSynced = res
                                else if (bestPlain == null) bestPlain = res
                            }
                        }
                    }
                    pos += frameSize
                }
                bestSynced ?: bestPlain
            }
        }.getOrNull()
    }

    private fun decodeUsltFrame(data: ByteArray, offset: Int, length: Int): String? {
        if (length < 5) return null
        val encodingByte = data[offset].toInt() and 0xFF
        val charset = id3Charset(encodingByte)
        // Skip encoding(1) + language(3) = 4 bytes, then find null-terminated descriptor
        var cur = offset + 4
        val end = offset + length
        cur = skipNullTerminated(data, cur, end, isDoubleByte = encodingByte == 1 || encodingByte == 2)
        if (cur >= end) return null
        return String(data, cur, end - cur, charset).trim('\u0000', ' ', '\r', '\n').takeIf { it.isNotEmpty() }
    }

    private fun decodeSyltFrame(data: ByteArray, offset: Int, length: Int): List<LyricLine> {
        if (length < 8) return emptyList()
        val encodingByte = data[offset].toInt() and 0xFF
        val charset = id3Charset(encodingByte)
        val timeFormat = data[offset + 4].toInt() and 0xFF
        if (timeFormat != 2) return emptyList() // 2 = milliseconds
        val end = offset + length
        val isDouble = encodingByte == 1 || encodingByte == 2
        var cur = skipNullTerminated(data, offset + 6, end, isDouble)
        val result = mutableListOf<LyricLine>()
        while (cur < end) {
            val textStart = cur
            val termPos = findTerminator(data, textStart, end, isDouble)
            val text = String(data, textStart, (termPos - textStart).coerceAtLeast(0), charset)
                .trim('\u0000', '\r', '\n')
            cur = termPos + if (isDouble) 2 else 1
            if (cur + 4 > end) break
            val ts = decodeIntBE(data, cur).toLong()
            cur += 4
            if (text.isNotEmpty()) {
                result.add(LyricLine(ts.coerceAtLeast(0L), text))
            }
        }
        return result.sortedBy { it.timestampMs }
    }

    private fun id3Charset(encodingByte: Int): Charset = when (encodingByte) {
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        3 -> Charsets.UTF_8
        else -> Charsets.ISO_8859_1
    }

    private fun findTerminator(data: ByteArray, start: Int, end: Int, isDoubleByte: Boolean): Int {
        var i = start
        if (isDoubleByte) {
            while (i + 1 < end) {
                if (data[i] == 0.toByte() && data[i + 1] == 0.toByte()) return i
                i += 2
            }
        } else {
            while (i < end) {
                if (data[i] == 0.toByte()) return i
                i++
            }
        }
        return end
    }

    private fun skipNullTerminated(data: ByteArray, start: Int, end: Int, isDoubleByte: Boolean): Int {
        val term = findTerminator(data, start, end, isDoubleByte)
        return (term + if (isDoubleByte) 2 else 1).coerceAtMost(end)
    }

    private fun decodeSyncSafeInt(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0x7F) shl 21) or
            ((buf[offset + 1].toInt() and 0x7F) shl 14) or
            ((buf[offset + 2].toInt() and 0x7F) shl 7) or
            (buf[offset + 3].toInt() and 0x7F)
    }

    private fun decodeIntBE(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0xFF) shl 24) or
            ((buf[offset + 1].toInt() and 0xFF) shl 16) or
            ((buf[offset + 2].toInt() and 0xFF) shl 8) or
            (buf[offset + 3].toInt() and 0xFF)
    }
}
