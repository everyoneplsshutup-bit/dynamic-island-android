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

data class MediaUiState(
    val hasActiveSession: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val packageName: String = "",
    val appLabel: String = "",
    val title: String = "Nothing Playing",
    val artist: String = "",
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
    val lyrics: List<LyricLine> = emptyList()
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
    private var sessionsListenerRegistered = false

    init {
        settingsRepository?.let { repo ->
            scope.launch {
                repo.settingsFlow.collectLatest { latestSettings = it }
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
            // Survive PixelMusic being force-stopped: clear current controller and re-poll sessions
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
        // PixelMusic first! Always control PixelMusic session when it exists.
        val chosen = controllers.firstOrNull { it.packageName == PIXEL_MUSIC_PKG }
            ?: controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

        if (chosen?.sessionToken == activeController?.sessionToken) {
            syncFromController()
            return
        }

        detachController()
        if (chosen != null) {
            activeController = chosen
            chosen.registerCallback(controllerCallback)
            syncFromController()
        } else {
            _state.value = MediaUiState(hasNotificationAccess = isNotificationListenerEnabled())
            islandController.removeActivity("music_session")
        }
    }

    private fun detachController() {
        progressJob?.cancel()
        accentJob?.cancel()
        activeController?.let { runCatching { it.unregisterCallback(controllerCallback) } }
        activeController = null
    }

    private fun syncFromController() {
        val controller = activeController
        if (controller == null) {
            _state.update { MediaUiState(hasNotificationAccess = isNotificationListenerEnabled()) }
            islandController.removeActivity("music_session")
            return
        }

        val hasAccess = isNotificationListenerEnabled()
        val metadata = controller.metadata
        val playback = controller.playbackState
        val pkg = controller.packageName
        val isPixelMusic = pkg == PIXEL_MUSIC_PKG

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Track"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val pos = playback?.position?.coerceAtLeast(0L) ?: 0L
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING

        val customActions = playback?.customActions ?: emptyList()
        val extras = controller.extras
        val supportsShuffle = hasAccess && (
            customActions.any { it.action.contains(CMD_TOGGLE_SHUFFLE, ignoreCase = true) || it.action.contains("shuffle", ignoreCase = true) } ||
                extras?.getBoolean("supports_toggle_shuffle", false) == true
            )
        val supportsRepeat = hasAccess && (
            customActions.any { it.action.contains(CMD_CYCLE_REPEAT, ignoreCase = true) || it.action.contains("repeat", ignoreCase = true) } ||
                extras?.getBoolean("supports_cycle_repeat", false) == true
            )
        val supportsLike = hasAccess && (
            customActions.any { it.action.contains(CMD_LIKE, ignoreCase = true) || it.action.contains("favorite", ignoreCase = true) } ||
                extras?.getBoolean("supports_like", false) == true
            )

        val mediaUriStr = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
        val lyricsRaw = extras?.getString("lyrics")
            ?: extras?.getString("lrc")
            ?: metadata?.getString("android.media.metadata.LYRICS")
            ?: readSidecarLrcIfLocal(mediaUriStr)
        val parsedLyrics = parseLyrics(lyricsRaw)

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
            albumArt = art,
            artworkAccentArgb = _state.value.artworkAccentArgb,
            isPlaying = isPlaying,
            positionMs = pos,
            durationMs = duration,
            isPixelMusic = isPixelMusic,
            supportsShuffle = supportsShuffle,
            supportsRepeat = supportsRepeat,
            supportsLike = supportsLike,
            isShuffled = extras?.getBoolean("shuffle_enabled", false) ?: false,
            repeatModeLabel = extras?.getString("repeat_mode") ?: "Off",
            isLiked = extras?.getBoolean("is_liked", false) ?: false,
            lyrics = parsedLyrics
        )

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
                    isPlaying = isPlaying,
                    positionMs = pos,
                    durationMs = duration
                ),
                autoShowCompact = isPlaying,
                autoExpandToFull = isPlaying && musicCfg.autoExpand
            )
        }

        if (isPlaying) startProgressTicker() else progressJob?.cancel()
    }

    private fun extractArtworkAccentAsync(bitmap: Bitmap) {
        accentJob?.cancel()
        accentJob = scope.launch {
            val argb = withContext(Dispatchers.Default) {
                runCatching {
                    val stepX = (bitmap.width / 10).coerceAtLeast(1)
                    val stepY = (bitmap.height / 10).coerceAtLeast(1)
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
            while (isActive) {
                val controller = activeController ?: break
                val pb = controller.playbackState
                if (pb?.state == PlaybackState.STATE_PLAYING) {
                    val elapsedSinceUpdate = android.os.SystemClock.elapsedRealtime() - pb.lastPositionUpdateTime
                    val estimated = (pb.position + (elapsedSinceUpdate * pb.playbackSpeed).toLong())
                        .coerceAtLeast(0L)
                        .let { pos ->
                            val dur = _state.value.durationMs
                            if (dur > 0L) pos.coerceAtMost(dur) else pos
                        }
                    _state.update { it.copy(positionMs = estimated) }
                }
                delay(250L)
            }
        }
    }

    fun togglePlayPause() {
        val c = activeController ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
        _state.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun sendShuffleCommand() = sendPixelMusicCustomCommand(CMD_TOGGLE_SHUFFLE)
    fun sendRepeatCommand() = sendPixelMusicCustomCommand(CMD_CYCLE_REPEAT)
    fun sendLikeCommand() = sendPixelMusicCustomCommand(CMD_LIKE)

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
        val pkg = _state.value.packageName.ifEmpty { PIXEL_MUSIC_PKG }
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: context.packageManager.getLaunchIntentForPackage(PIXEL_MUSIC_PKG)
        launch?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(it) }
        }
    }

    /**
     * Strictly on-device local .lrc sidecar reader (if player exposes file:// METADATA_KEY_MEDIA_URI).
     * NEVER performs any network call (app has zero INTERNET permission).
     */
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
