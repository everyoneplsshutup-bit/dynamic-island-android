package com.example.media

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlin.math.abs

@Serializable
data class LrclibTrackRecord(
    val id: Long = 0L,
    val trackName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val duration: Double = 0.0,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

sealed interface LrclibResult {
    data class Found(val lyrics: ResolvedLyrics) : LrclibResult
    data object NotFound : LrclibResult
    data object OfflineDisabled : LrclibResult
    data class RateLimited(val retryAfterSeconds: Long) : LrclibResult
    data object NetworkError : LrclibResult
}

/**
 * Dedicated LRCLIB (https://lrclib.net) client.
 * - Strict host & protocol guard: ONLY connects to https://lrclib.net, zero off-host redirects.
 * - 1 request per unique track change + persistent disk cache under filesDir/lyrics_cache/.
 * - Duration matched within +-2 s when duration is known.
 * - Immediately aborted when [lyricsFromInternet] is false.
 */
class LrclibClient(private val context: Context) {

    companion object {
        const val BASE_URL = "https://lrclib.net"
        private const val ALLOWED_HOST = "lrclib.net"
        private const val USER_AGENT = "DynamicIslandAndroid/1.0 ($BASE_URL)"
        private const val DURATION_TOLERANCE_SEC = 2.0
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val cacheDir: File by lazy {
        File(context.filesDir, "lyrics_cache").apply { if (!exists()) mkdirs() }
    }

    @Volatile
    private var rateLimitedUntilMs: Long = 0L

    private val inFlightKeys = HashSet<String>()

    suspend fun fetchLyrics(
        trackName: String,
        artistName: String,
        albumName: String,
        durationMs: Long,
        lyricsFromInternet: Boolean
    ): LrclibResult = withContext(Dispatchers.IO) {
        val cleanTrack = trackName.trim()
        val cleanArtist = artistName.trim()
        if (cleanTrack.isEmpty()) return@withContext LrclibResult.NotFound

        val cacheKey = buildCacheKey(cleanTrack, cleanArtist, albumName.trim())
        readFromDiskCache(cacheKey)?.let { cached ->
            return@withContext if (cached.lines.isNotEmpty()) LrclibResult.Found(cached) else LrclibResult.NotFound
        }

        if (!lyricsFromInternet) {
            return@withContext LrclibResult.OfflineDisabled
        }

        val now = System.currentTimeMillis()
        if (now < rateLimitedUntilMs) {
            val remSec = ((rateLimitedUntilMs - now) / 1000L).coerceAtLeast(1L)
            return@withContext LrclibResult.RateLimited(remSec)
        }

        synchronized(inFlightKeys) {
            if (!inFlightKeys.add(cacheKey)) {
                return@withContext LrclibResult.NotFound
            }
        }

        try {
            val durationSec = if (durationMs > 0L) (durationMs / 1000.0) else 0.0
            val getRecord = queryGetEndpoint(cleanTrack, cleanArtist, albumName.trim(), durationSec)
            when (getRecord) {
                is HttpOutcome.RateLimited -> return@withContext LrclibResult.RateLimited(getRecord.retryAfterSec)
                is HttpOutcome.Success -> {
                    val parsed = runCatching { json.decodeFromString<LrclibTrackRecord>(getRecord.body) }.getOrNull()
                    if (parsed != null && isDurationMatch(parsed.duration, durationSec)) {
                        val resolved = toResolvedLyrics(parsed)
                        if (resolved != null) {
                            writeDiskCache(cacheKey, parsed)
                            return@withContext LrclibResult.Found(resolved)
                        }
                    }
                }
                else -> Unit
            }

            // Fallback to /api/search if exact /api/get missed
            val searchOutcome = querySearchEndpoint(cleanTrack, cleanArtist)
            when (searchOutcome) {
                is HttpOutcome.RateLimited -> return@withContext LrclibResult.RateLimited(searchOutcome.retryAfterSec)
                is HttpOutcome.Success -> {
                    val items = runCatching { json.decodeFromString<List<LrclibTrackRecord>>(searchOutcome.body) }
                        .getOrNull()
                        .orEmpty()
                    val best = items
                        .filter { isDurationMatch(it.duration, durationSec) }
                        .sortedBy { candidate ->
                            val hasSyncedScore = if (!candidate.syncedLyrics.isNullOrBlank()) 0.0 else 100.0
                            val durDiff = if (durationSec > 0.0) abs(candidate.duration - durationSec) else 0.0
                            hasSyncedScore + durDiff
                        }
                        .firstOrNull { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() }

                    if (best != null) {
                        val resolved = toResolvedLyrics(best)
                        if (resolved != null) {
                            writeDiskCache(cacheKey, best)
                            return@withContext LrclibResult.Found(resolved)
                        }
                    }
                    // Cache negative miss so same track isn't re-fetched repeatedly
                    writeDiskCache(cacheKey, LrclibTrackRecord(trackName = cleanTrack, artistName = cleanArtist))
                    return@withContext LrclibResult.NotFound
                }
                is HttpOutcome.NotFound -> {
                    writeDiskCache(cacheKey, LrclibTrackRecord(trackName = cleanTrack, artistName = cleanArtist))
                    return@withContext LrclibResult.NotFound
                }
                is HttpOutcome.Error -> return@withContext LrclibResult.NetworkError
            }
        } finally {
            synchronized(inFlightKeys) {
                inFlightKeys.remove(cacheKey)
            }
        }
    }

    internal fun buildVerifiedUrl(pathAndQuery: String): URL {
        val url = URL("$BASE_URL$pathAndQuery")
        check(url.protocol.equals("https", ignoreCase = true) && url.host.equals(ALLOWED_HOST, ignoreCase = true)) {
            "Blocked non-LRCLIB endpoint: $url"
        }
        return url
    }

    private fun isDurationMatch(candidateSec: Double, targetSec: Double): Boolean {
        if (targetSec <= 1.0 || candidateSec <= 1.0) return true
        return abs(candidateSec - targetSec) <= DURATION_TOLERANCE_SEC
    }

    private fun queryGetEndpoint(
        track: String,
        artist: String,
        album: String,
        durationSec: Double
    ): HttpOutcome {
        val params = buildList {
            add("track_name=${encode(track)}")
            if (artist.isNotEmpty()) add("artist_name=${encode(artist)}")
            if (album.isNotEmpty()) add("album_name=${encode(album)}")
            if (durationSec > 1.0) add("duration=${durationSec.toInt()}")
        }.joinToString("&")
        return executeGet("/api/get?$params")
    }

    private fun querySearchEndpoint(track: String, artist: String): HttpOutcome {
        val params = buildList {
            add("track_name=${encode(track)}")
            if (artist.isNotEmpty()) add("artist_name=${encode(artist)}")
        }.joinToString("&")
        return executeGet("/api/search?$params")
    }

    private sealed interface HttpOutcome {
        data class Success(val body: String) : HttpOutcome
        data object NotFound : HttpOutcome
        data class RateLimited(val retryAfterSec: Long) : HttpOutcome
        data object Error : HttpOutcome
    }

    private fun executeGet(pathAndQuery: String): HttpOutcome {
        return runCatching {
            val url = buildVerifiedUrl(pathAndQuery)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                when (code) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        HttpOutcome.Success(body)
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> HttpOutcome.NotFound
                    429 -> {
                        val retryHeader = conn.getHeaderField("Retry-After")?.toLongOrNull() ?: 30L
                        rateLimitedUntilMs = System.currentTimeMillis() + retryHeader.coerceIn(5L, 300L) * 1000L
                        HttpOutcome.RateLimited(retryHeader)
                    }
                    in 300..399 -> HttpOutcome.Error // Strictly refuse redirects
                    else -> HttpOutcome.Error
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(HttpOutcome.Error)
    }

    private fun toResolvedLyrics(record: LrclibTrackRecord): ResolvedLyrics? {
        val syncedRaw = record.syncedLyrics?.takeIf { it.isNotBlank() }
        if (syncedRaw != null) {
            val lines = LocalLyricsReader.parseLyricsText(syncedRaw)
            if (lines.isNotEmpty()) {
                return ResolvedLyrics(lines, isSynced = lines.any { it.timestampMs >= 0L }, source = LyricsSource.LRCLIB)
            }
        }
        val plainRaw = record.plainLyrics?.takeIf { it.isNotBlank() }
        if (plainRaw != null) {
            val lines = LocalLyricsReader.parseLyricsText(plainRaw)
            if (lines.isNotEmpty()) {
                return ResolvedLyrics(lines, isSynced = false, source = LyricsSource.LRCLIB)
            }
        }
        return null
    }

    private fun readFromDiskCache(key: String): ResolvedLyrics? {
        val file = File(cacheDir, "$key.json")
        if (!file.exists()) return null
        val rec = runCatching { json.decodeFromString<LrclibTrackRecord>(file.readText(Charsets.UTF_8)) }.getOrNull()
            ?: return null
        return toResolvedLyrics(rec) ?: ResolvedLyrics(emptyList(), isSynced = false, source = LyricsSource.LRCLIB)
    }

    private fun writeDiskCache(key: String, record: LrclibTrackRecord) {
        runCatching {
            val files = cacheDir.listFiles()
            if (files != null && files.size > 180) {
                files.sortedBy { it.lastModified() }.take(files.size - 160).forEach { it.delete() }
            }
            File(cacheDir, "$key.json").writeText(json.encodeToString(record), Charsets.UTF_8)
        }
    }

    private fun buildCacheKey(track: String, artist: String, album: String): String {
        val input = "${track.lowercase()}|${artist.lowercase()}|${album.lowercase()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
