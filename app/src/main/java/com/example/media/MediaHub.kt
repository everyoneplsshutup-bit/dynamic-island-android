package com.example.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import androidx.core.graphics.ColorUtils
import com.example.island.IslandActivity
import com.example.island.IslandController
import com.example.notifications.IslandNotificationListener
import com.example.system.EventKind
import com.example.system.IslandSettings
import com.example.system.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

enum class LyricsLookupStatus {
    IDLE,
    LOADING,
    LOADED_SYNCED,
    LOADED_PLAIN,
    NEEDS_INTERNET_STREAMED,
    NO_LYRICS_FOUND,
    RATE_LIMITED
}

data class MediaUiState(
    val hasActiveSession: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val packageName: String = "",
    val appLabel: String = "",
    val title: String = "Nothing Playing",
    val artist: String = "",
    val album: String = "",
    val albumArt: Bitmap? = null,
    val artworkAccentArgb: Int? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPixelMusic: Boolean = false,
    // PixelMusic custom Media3 SessionCommands (only shown when offered & NL is on)
    val supportsShuffle: Boolean = false,
    val supportsRepeat: Boolean = false,
    val supportsLike: Boolean = false,
    val isShuffled: Boolean = false,
    val repeatModeLabel: String = "Off",
    val isLiked: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val lyricsStatus: LyricsLookupStatus = LyricsLookupStatus.IDLE,
    val lyricsSource: LyricsSource = LyricsSource.NONE
)

class MediaHub(
    private val context: Context,
    private val islandController: IslandController,
    private val settingsRepository: SettingsRepository? = null
) {
    companion object {
        const val PIXEL_MUSIC_PKG = "com.unshoo.pixelmusic"
        const val CMD_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE"
        const val CMD_CYCLE_REPEAT = "CYCLE_REPEAT"
        const val CMD_LIKE = "LIKE"
        private const val ARTWORK_MAX_DIM_PX = 96
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val listenerComponent = ComponentName(context, IslandNotificationListener::class.java)

    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    @Volatile
    private var latestSettings: IslandSettings = IslandSettings()

    private var activeController: MediaController? = null
    private var progressJob: Job? = null
    private var accentJob: Job? = null
    private var lyricsJob: Job? = null
    private var lastLyricsFetchedTrackKey: String? = null
    private var sessionsListenerRegistered = false
    private val lrclibClient = LrclibClient(context)

    private data class OptimisticOverrides(
        val trackKey: String,
        var isPlaying: Boolean? = null,
        var isLiked: Boolean? = null,
        var isShuffled: Boolean? = null,
        var repeatModeLabel: String? = null,
        var updatedAtMs: Long = android.os.SystemClock.elapsedRealtime()
    )

    private var optimisticOverrides: OptimisticOverrides? = null

    private fun trackKeyFor(pkg: String, title: String, artist: String): String = "$pkg|$title|$artist"

    init {
        settingsRepository?.let { repo ->
            scope.launch {
                repo.settingsFlow.collectLatest { settings ->
                    val prevAllowed = latestSettings.allowedMusicPackages
                    val prevInternet = latestSettings.lyricsFromInternet
                    latestSettings = settings
                    if (prevAllowed != settings.allowedMusicPackages) {
                        refreshSessions()
                    } else if (!prevInternet && settings.lyricsFromInternet && _state.value.hasActiveSession) {
                        lastLyricsFetchedTrackKey = null
                        resolveLyricsAsync(
                            trackKey = trackKeyFor(_state.value.packageName, _state.value.title, _state.value.artist),
                            title = _state.value.title,
                            artist = _state.value.artist,
                            album = _state.value.album,
                            durationMs = _state.value.durationMs,
                            mediaUriStr = null
                        )
                    }
                }
            }
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            syncFromController()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            syncFromController()
        }

        override fun onExtrasChanged(extras: Bundle?) {
            syncFromController()
        }

        override fun onSessionDestroyed() {
            detachController()
            refreshSessions()
        }
    }

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindBestController(controllers ?: emptyList())
    }

    fun isNotificationListenerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(listenerComponent.flattenToString()) || enabled.contains(context.packageName)
    }

    fun start() {
        val hasAccess = isNotificationListenerEnabled()
        _state.update { it.copy(hasNotificationAccess = hasAccess) }
        if (!hasAccess) return

        runCatching {
            if (!sessionsListenerRegistered) {
                sessionManager?.addOnActiveSessionsChangedListener(activeSessionsListener, listenerComponent)
                sessionsListenerRegistered = true
            }
            refreshSessions()
        }
    }

    fun stop() {
        if (sessionsListenerRegistered) {
            runCatching { sessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener) }
            sessionsListenerRegistered = false
        }
        detachController()
    }

    fun refreshSessions() {
        val hasAccess = isNotificationListenerEnabled()
        _state.update { it.copy(hasNotificationAccess = hasAccess) }
        if (!hasAccess) {
            detachController()
            return
        }
        val controllers = runCatching {
            sessionManager?.getActiveSessions(listenerComponent)
        }.getOrNull() ?: emptyList()

        if (!sessionsListenerRegistered) {
            runCatching {
                sessionManager?.addOnActiveSessionsChangedListener(activeSessionsListener, listenerComponent)
                sessionsListenerRegistered = true
            }
        }
        bindBestController(controllers)
    }

    private fun bindBestController(controllers: List<MediaController>) {
        // Enforce user-chosen Music Source Picker (PixelMusic by default; browsers never sneak in).
        val allowedPkgs = latestSettings.allowedMusicPackages
        val eligible = if (allowedPkgs.isEmpty()) {
            emptyList()
        } else {
            controllers.filter { it.packageName in allowedPkgs }
        }

        val chosen = eligible.firstOrNull { it.packageName == PIXEL_MUSIC_PKG }
            ?: eligible.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: eligible.firstOrNull()

        if (chosen != null && chosen.sessionToken == activeController?.sessionToken) {
            syncFromController()
            return
        }

        detachController()
        if (chosen != null) {
            activeController = chosen
            chosen.registerCallback(controllerCallback)
            syncFromController()
        } else {
            optimisticOverrides = null
            _state.value = MediaUiState(hasNotificationAccess = isNotificationListenerEnabled())
            islandController.removeActivity("music_session")
        }
    }

    private fun detachController() {
        progressJob?.cancel()
        accentJob?.cancel()
        lyricsJob?.cancel()
        lastLyricsFetchedTrackKey = null
        activeController?.let { runCatching { it.unregisterCallback(controllerCallback) } }
        activeController = null
    }

    private fun syncFromController() {
        val controller = activeController
        if (controller == null) {
            optimisticOverrides = null
            _state.update { MediaUiState(hasNotificationAccess = isNotificationListenerEnabled()) }
            islandController.removeActivity("music_session")
            return
        }

        val pkg = controller.packageName
        if (latestSettings.allowedMusicPackages.isNotEmpty() && pkg !in latestSettings.allowedMusicPackages) {
            detachController()
            optimisticOverrides = null
            _state.value = MediaUiState(hasNotificationAccess = isNotificationListenerEnabled())
            islandController.removeActivity("music_session")
            return
        }

        val hasAccess = isNotificationListenerEnabled()
        val metadata = controller.metadata
        val playback = controller.playbackState
        val rawIsPlaying = playback?.state == PlaybackState.STATE_PLAYING
        if (metadata == null && !rawIsPlaying && playback?.state != PlaybackState.STATE_PAUSED) {
            optimisticOverrides = null
            _state.value = MediaUiState(hasNotificationAccess = hasAccess)
            islandController.removeActivity("music_session")
            progressJob?.cancel()
            return
        }
        val isPixelMusic = pkg == PIXEL_MUSIC_PKG

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val rawArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val art = rawArt?.let { downsampleArtwork(it) }
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val reportedPos = playback?.position?.coerceAtLeast(0L) ?: 0L

        val customActions = playback?.customActions ?: emptyList()
        val extras = controller.extras
        val supportsShuffle = hasAccess && (
            isPixelMusic ||
                customActions.any { it.action.contains(CMD_TOGGLE_SHUFFLE, ignoreCase = true) || it.action.contains("shuffle", ignoreCase = true) } ||
                extras?.getBoolean("supports_toggle_shuffle", false) == true
            )
        val supportsRepeat = hasAccess && (
            isPixelMusic ||
                customActions.any { it.action.contains(CMD_CYCLE_REPEAT, ignoreCase = true) || it.action.contains("repeat", ignoreCase = true) } ||
                extras?.getBoolean("supports_cycle_repeat", false) == true
            )
        val supportsLike = hasAccess && (
            isPixelMusic ||
                customActions.any { it.action.contains(CMD_LIKE, ignoreCase = true) || it.action.contains("favorite", ignoreCase = true) } ||
                extras?.getBoolean("supports_like", false) == true
            )

        val currentTrackKey = trackKeyFor(pkg, title, artist)
        val prevState = _state.value
        val activeOverride = optimisticOverrides?.takeIf { ov ->
            ov.trackKey == currentTrackKey &&
                (android.os.SystemClock.elapsedRealtime() - ov.updatedAtMs) < 8_000L
        }
        if (optimisticOverrides != null && optimisticOverrides?.trackKey != currentTrackKey) {
            optimisticOverrides = null
        }

        val controllerHasShuffle = extras?.containsKey("shuffle_enabled") == true
        val controllerHasRepeat = extras?.containsKey("repeat_mode") == true
        val controllerHasLike = extras?.containsKey("is_liked") == true

        val resolvedIsPlaying = if (activeOverride?.isPlaying != null && (android.os.SystemClock.elapsedRealtime() - activeOverride.updatedAtMs) < 1_600L) {
            if (activeOverride.isPlaying == rawIsPlaying) {
                activeOverride.isPlaying = null
                rawIsPlaying
            } else {
                activeOverride.isPlaying!!
            }
        } else {
            rawIsPlaying
        }

        val rawShuffle = if (controllerHasShuffle) extras?.getBoolean("shuffle_enabled", false) ?: false else if (prevState.packageName == pkg && trackKeyFor(prevState.packageName, prevState.title, prevState.artist) == currentTrackKey) prevState.isShuffled else false
        val resolvedShuffled = activeOverride?.isShuffled?.let { ov ->
            if (controllerHasShuffle && rawShuffle == ov) {
                activeOverride.isShuffled = null
                rawShuffle
            } else ov
        } ?: rawShuffle

        val rawRepeat = if (controllerHasRepeat) extras?.getString("repeat_mode") ?: "Off" else if (prevState.packageName == pkg && trackKeyFor(prevState.packageName, prevState.title, prevState.artist) == currentTrackKey) prevState.repeatModeLabel else "Off"
        val resolvedRepeat = activeOverride?.repeatModeLabel?.let { ov ->
            if (controllerHasRepeat && rawRepeat == ov) {
                activeOverride.repeatModeLabel = null
                rawRepeat
            } else ov
        } ?: rawRepeat

        val rawLiked = if (controllerHasLike) extras?.getBoolean("is_liked", false) ?: false else if (prevState.packageName == pkg && trackKeyFor(prevState.packageName, prevState.title, prevState.artist) == currentTrackKey) prevState.isLiked else false
        val resolvedLiked = activeOverride?.isLiked?.let { ov ->
            if (controllerHasLike && rawLiked == ov) {
                activeOverride.isLiked = null
                rawLiked
            } else ov
        } ?: rawLiked

        val pos = computeSmoothPosition(playback, prevState, currentTrackKey, reportedPos, duration)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()

        // 1. Metadata/extras embedded first, else keep already-resolved lyrics if same track
        val mediaUriStr = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
        val extraLyricsRaw = extras?.getString("lyrics")
            ?: extras?.getString("lrc")
            ?: metadata?.getString("android.media.metadata.LYRICS")
            ?: readSidecarLrcIfLocal(mediaUriStr)
        val syncParsedLyrics = LocalLyricsReader.parseLyricsText(extraLyricsRaw)
        val sameTrackLoaded = lastLyricsFetchedTrackKey == currentTrackKey
        val immediateLyrics = when {
            syncParsedLyrics.isNotEmpty() -> syncParsedLyrics
            sameTrackLoaded -> prevState.lyrics
            else -> emptyList()
        }
        val immediateStatus = when {
            syncParsedLyrics.isNotEmpty() ->
                if (syncParsedLyrics.any { it.timestampMs >= 0L }) LyricsLookupStatus.LOADED_SYNCED else LyricsLookupStatus.LOADED_PLAIN
            sameTrackLoaded -> prevState.lyricsStatus
            else -> LyricsLookupStatus.LOADING
        }
        val immediateSource = when {
            syncParsedLyrics.isNotEmpty() -> LyricsSource.EMBEDDED_ID3
            sameTrackLoaded -> prevState.lyricsSource
            else -> LyricsSource.NONE
        }

        val appLabel = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)

        _state.value = MediaUiState(
            hasActiveSession = true,
            hasNotificationAccess = hasAccess,
            packageName = pkg,
            appLabel = appLabel,
            title = title,
            artist = artist,
            album = album,
            albumArt = art,
            artworkAccentArgb = _state.value.artworkAccentArgb,
            isPlaying = resolvedIsPlaying,
            positionMs = pos,
            durationMs = duration,
            isPixelMusic = isPixelMusic,
            supportsShuffle = supportsShuffle,
            supportsRepeat = supportsRepeat,
            supportsLike = supportsLike,
            isShuffled = resolvedShuffled,
            repeatModeLabel = resolvedRepeat,
            isLiked = resolvedLiked,
            lyrics = immediateLyrics,
            lyricsStatus = immediateStatus,
            lyricsSource = immediateSource
        )

        if (syncParsedLyrics.isEmpty() && !sameTrackLoaded) {
            resolveLyricsAsync(
                trackKey = currentTrackKey,
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                mediaUriStr = mediaUriStr
            )
        }

        if (art != null) {
            extractArtworkAccentAsync(art)
        } else {
            _state.update { it.copy(artworkAccentArgb = null) }
        }

        val musicCfg = latestSettings.eventConfig(EventKind.MUSIC)
        if (latestSettings.musicEnabled && musicCfg.enabled) {
            islandController.upsertActivity(
                IslandActivity.Music(
                    packageName = pkg,
                    title = title,
                    artist = artist,
                    isPlaying = resolvedIsPlaying,
                    positionMs = pos,
                    durationMs = duration
                ),
                autoShowCompact = resolvedIsPlaying,
                autoExpandToFull = resolvedIsPlaying && musicCfg.autoExpand
            )
        }

        if (resolvedIsPlaying) startProgressTicker() else progressJob?.cancel()
    }

    private fun resolveLyricsAsync(
        trackKey: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        mediaUriStr: String?
    ) {
        lyricsJob?.cancel()
        lastLyricsFetchedTrackKey = trackKey
        lyricsJob = scope.launch {
            val local = withContext(Dispatchers.IO) {
                LocalLyricsReader.readLocalLyrics(context, mediaUriStr, title, artist)
            }
            if (_state.value.let { trackKeyFor(it.packageName, it.title, it.artist) } != trackKey) return@launch
            if (local != null && local.lines.isNotEmpty()) {
                _state.update {
                    it.copy(
                        lyrics = local.lines,
                        lyricsStatus = if (local.isSynced) LyricsLookupStatus.LOADED_SYNCED else LyricsLookupStatus.LOADED_PLAIN,
                        lyricsSource = local.source
                    )
                }
                return@launch
            }

            val remote = lrclibClient.fetchLyrics(
                trackName = title,
                artistName = artist,
                albumName = album,
                durationMs = durationMs,
                lyricsFromInternet = latestSettings.lyricsFromInternet
            )
            if (_state.value.let { trackKeyFor(it.packageName, it.title, it.artist) } != trackKey) return@launch
            when (remote) {
                is LrclibResult.Found -> {
                    _state.update {
                        it.copy(
                            lyrics = remote.lyrics.lines,
                            lyricsStatus = if (remote.lyrics.isSynced) LyricsLookupStatus.LOADED_SYNCED else LyricsLookupStatus.LOADED_PLAIN,
                            lyricsSource = LyricsSource.LRCLIB
                        )
                    }
                }
                LrclibResult.OfflineDisabled -> {
                    val isLikelyStreamed = mediaUriStr.isNullOrBlank() || mediaUriStr.startsWith("http", ignoreCase = true)
                    _state.update {
                        it.copy(
                            lyrics = emptyList(),
                            lyricsStatus = if (isLikelyStreamed) {
                                LyricsLookupStatus.NEEDS_INTERNET_STREAMED
                            } else {
                                LyricsLookupStatus.NO_LYRICS_FOUND
                            },
                            lyricsSource = LyricsSource.NONE
                        )
                    }
                }
                is LrclibResult.RateLimited -> {
                    _state.update {
                        it.copy(
                            lyrics = emptyList(),
                            lyricsStatus = LyricsLookupStatus.RATE_LIMITED,
                            lyricsSource = LyricsSource.NONE
                        )
                    }
                }
                LrclibResult.NotFound, LrclibResult.NetworkError -> {
                    _state.update {
                        it.copy(
                            lyrics = emptyList(),
                            lyricsStatus = LyricsLookupStatus.NO_LYRICS_FOUND,
                            lyricsSource = LyricsSource.NONE
                        )
                    }
                }
            }
        }
    }

    private fun computeSmoothPosition(
        playback: PlaybackState?,
        prevState: MediaUiState,
        currentTrackKey: String,
        reportedPos: Long,
        durationMs: Long
    ): Long {
        val base = if (playback != null && playback.state == PlaybackState.STATE_PLAYING &&
            playback.lastPositionUpdateTime > 0L && playback.playbackSpeed > 0f
        ) {
            val elapsed = (android.os.SystemClock.elapsedRealtime() - playback.lastPositionUpdateTime).coerceAtLeast(0L)
            reportedPos + (elapsed * playback.playbackSpeed).toLong()
        } else {
            val sameTrack = trackKeyFor(prevState.packageName, prevState.title, prevState.artist) == currentTrackKey
            if (sameTrack && prevState.isPlaying && reportedPos < prevState.positionMs && prevState.positionMs - reportedPos < 2_500L) {
                prevState.positionMs
            } else {
                reportedPos
            }
        }
        return base.coerceAtLeast(0L).let { p -> if (durationMs > 0L) p.coerceAtMost(durationMs) else p }
    }

    private fun downsampleArtwork(src: Bitmap): Bitmap? = runCatching {
        val w = src.width.coerceAtLeast(1)
        val h = src.height.coerceAtLeast(1)
        if (w <= ARTWORK_MAX_DIM_PX && h <= ARTWORK_MAX_DIM_PX) {
            return@runCatching if (!src.hasAlpha() && src.config != Bitmap.Config.RGB_565) {
                src.copy(Bitmap.Config.RGB_565, false)
            } else {
                src
            }
        }
        val scale = ARTWORK_MAX_DIM_PX.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * scale).toInt().coerceIn(24, ARTWORK_MAX_DIM_PX)
        val targetH = (h * scale).toInt().coerceIn(24, ARTWORK_MAX_DIM_PX)
        val scaled = Bitmap.createScaledBitmap(src, targetW, targetH, true)
        if (!src.hasAlpha()) {
            scaled.copy(Bitmap.Config.RGB_565, false).also {
                if (scaled !== it) scaled.recycle()
            }
        } else {
            scaled
        }
    }.getOrNull()

    private fun extractArtworkAccentAsync(bitmap: Bitmap) {
        accentJob?.cancel()
        accentJob = scope.launch {
            val argb = withContext(Dispatchers.Default) {
                runCatching {
                    val stepX = (bitmap.width / 8).coerceAtLeast(1)
                    val stepY = (bitmap.height / 8).coerceAtLeast(1)
                    var bestScore = -1f
                    var bestColor = 0xFFBF5AF2.toInt()
                    val hsl = FloatArray(3)
                    var y = 0
                    while (y < bitmap.height) {
                        var x = 0
                        while (x < bitmap.width) {
                            val pixel = bitmap.getPixel(x, y)
                            ColorUtils.colorToHSL(pixel, hsl)
                            val sat = hsl[1]
                            val light = hsl[2]
                            val score = sat * 2.0f - kotlin.math.abs(light - 0.55f)
                            if (sat > 0.18f && light in 0.18f..0.85f && score > bestScore) {
                                bestScore = score
                                hsl[1] = sat.coerceAtLeast(0.65f)
                                hsl[2] = light.coerceIn(0.50f, 0.70f)
                                bestColor = ColorUtils.HSLToColor(hsl)
                            }
                            x += stepX
                        }
                        y += stepY
                    }
                    bestColor
                }.getOrNull()
            }
            _state.update { it.copy(artworkAccentArgb = argb) }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastTickMs = android.os.SystemClock.elapsedRealtime()
            while (isActive) {
                val now = android.os.SystemClock.elapsedRealtime()
                val delta = (now - lastTickMs).coerceIn(50L, 1_000L)
                lastTickMs = now

                val controller = activeController ?: break
                val pb = controller.playbackState
                val curState = _state.value
                if (!curState.hasActiveSession || !curState.isPlaying) break

                val dur = curState.durationMs
                val estimated = if (pb != null && pb.state == PlaybackState.STATE_PLAYING &&
                    pb.lastPositionUpdateTime > 0L && pb.playbackSpeed > 0f
                ) {
                    val elapsedSinceUpdate = (now - pb.lastPositionUpdateTime).coerceAtLeast(0L)
                    (pb.position + (elapsedSinceUpdate * pb.playbackSpeed).toLong())
                } else {
                    maxOf(curState.positionMs, pb?.position ?: 0L) + delta
                }.coerceAtLeast(0L).let { pos -> if (dur > 0L) pos.coerceAtMost(dur) else pos }

                if (estimated != curState.positionMs) {
                    _state.update { it.copy(positionMs = estimated) }
                }
                delay(250L)
            }
        }
    }

    private fun ensureOverrides(): OptimisticOverrides {
        val cur = _state.value
        val key = trackKeyFor(cur.packageName, cur.title, cur.artist)
        val existing = optimisticOverrides
        return if (existing != null && existing.trackKey == key) {
            existing.apply { updatedAtMs = android.os.SystemClock.elapsedRealtime() }
        } else {
            OptimisticOverrides(trackKey = key).also { optimisticOverrides = it }
        }
    }

    fun togglePlayPause() {
        val c = activeController ?: return
        val currentlyPlaying = _state.value.isPlaying
        val nextPlaying = !currentlyPlaying
        ensureOverrides().isPlaying = nextPlaying
        _state.update { it.copy(isPlaying = nextPlaying) }
        islandController.upsertActivity(
            IslandActivity.Music(
                packageName = _state.value.packageName,
                title = _state.value.title,
                artist = _state.value.artist,
                isPlaying = nextPlaying,
                positionMs = _state.value.positionMs,
                durationMs = _state.value.durationMs
            ),
            autoShowCompact = false
        )
        if (nextPlaying) startProgressTicker() else progressJob?.cancel()
        if (currentlyPlaying) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun skipNext() {
        optimisticOverrides = null
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        optimisticOverrides = null
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L).let { p ->
            val dur = _state.value.durationMs
            if (dur > 0L) p.coerceAtMost(dur) else p
        }
        activeController?.transportControls?.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
    }

    fun sendShuffleCommand() {
        val next = !_state.value.isShuffled
        ensureOverrides().isShuffled = next
        _state.update { it.copy(isShuffled = next) }
        sendPixelMusicCustomCommand(CMD_TOGGLE_SHUFFLE)
    }

    fun sendRepeatCommand() {
        val next = when (_state.value.repeatModeLabel.lowercase()) {
            "all" -> "One"
            "one" -> "Off"
            else -> "All"
        }
        ensureOverrides().repeatModeLabel = next
        _state.update { it.copy(repeatModeLabel = next) }
        sendPixelMusicCustomCommand(CMD_CYCLE_REPEAT)
    }

    fun sendLikeCommand() {
        val next = !_state.value.isLiked
        ensureOverrides().isLiked = next
        _state.update { it.copy(isLiked = next) }
        sendPixelMusicCustomCommand(CMD_LIKE)
    }

    private fun sendPixelMusicCustomCommand(commandName: String) {
        if (!isNotificationListenerEnabled()) return
        val controller = activeController ?: return
        val customAction = controller.playbackState?.customActions?.firstOrNull {
            it.action.equals(commandName, ignoreCase = true) ||
                it.action.contains(commandName, ignoreCase = true)
        }
        if (customAction != null) {
            controller.transportControls.sendCustomAction(customAction, customAction.extras)
        } else {
            controller.sendCommand(commandName, Bundle.EMPTY, null)
        }
    }

    fun openPlayerApp() {
        val pkg = _state.value.packageName.ifEmpty {
            latestSettings.allowedMusicPackages.firstOrNull() ?: PIXEL_MUSIC_PKG
        }
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: context.packageManager.getLaunchIntentForPackage(PIXEL_MUSIC_PKG)
        launch?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(it) }
        }
    }

    private fun readSidecarLrcIfLocal(mediaUriStr: String?): String? {
        if (mediaUriStr.isNullOrBlank()) return null
        return runCatching {
            val uri = android.net.Uri.parse(mediaUriStr)
            if (!uri.scheme.equals("file", ignoreCase = true)) return null
            val path = uri.path ?: return null
            val audioFile = java.io.File(path)
            val parent = audioFile.parentFile ?: return null
            val lrcFile = java.io.File(parent, audioFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists() && lrcFile.canRead() && lrcFile.length() in 1L..256_000L) {
                lrcFile.readText(Charsets.UTF_8)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun parseLyrics(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")
        val lines = raw.lines().mapNotNull { line ->
            val match = lrcRegex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msPart = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) LyricLine((min * 60_000L) + (sec * 1000L) + msPart, text) else null
            } else {
                val clean = line.trim()
                if (clean.isNotEmpty()) LyricLine(-1L, clean) else null
            }
        }
        return lines
    }
}
