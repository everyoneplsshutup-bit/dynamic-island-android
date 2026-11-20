package com.example.notes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.island.IslandActivity
import com.example.island.IslandController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class VoiceMemoUiState(
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L,
    val playingMemoId: Long? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L
)

class VoiceMemoManager(
    private val context: Context,
    private val voiceMemoDao: VoiceMemoDao,
    private val islandController: IslandController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val memosFlow: Flow<List<VoiceMemoEntity>> = voiceMemoDao.observeAll()

    private val _uiState = MutableStateFlow(VoiceMemoUiState())
    val uiState: StateFlow<VoiceMemoUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var playbackJob: Job? = null

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun startRecording(): Boolean {
        if (!hasMicPermission()) return false
        val intent = Intent(context, VoiceMemoService::class.java).apply {
            action = VoiceMemoService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
        return true
    }

    fun stopRecording() {
        val intent = Intent(context, VoiceMemoService::class.java).apply {
            action = VoiceMemoService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
    }

    internal fun onRecordingTick(elapsedMs: Long) {
        _uiState.update { it.copy(isRecording = true, recordingElapsedMs = elapsedMs) }
        islandController.upsertActivity(
            IslandActivity.VoiceRecording(elapsedMs = elapsedMs),
            autoShowCompact = true
        )
    }

    internal fun onRecordingFinished(file: File?, durationMs: Long) {
        _uiState.update { it.copy(isRecording = false, recordingElapsedMs = 0L) }
        islandController.removeActivity("voice_memo_rec")
        if (file != null && file.exists() && durationMs >= 300L) {
            scope.launch(Dispatchers.IO) {
                val count = System.currentTimeMillis() % 10000
                voiceMemoDao.insert(
                    VoiceMemoEntity(
                        title = "Voice Memo #$count",
                        filePath = file.absolutePath,
                        durationMs = durationMs
                    )
                )
            }
        }
    }

    fun togglePlayMemo(memo: VoiceMemoEntity) {
        val current = _uiState.value
        if (current.playingMemoId == memo.id && current.isPlaying) {
            player?.pause()
            _uiState.update { it.copy(isPlaying = false) }
            playbackJob?.cancel()
            return
        }
        if (current.playingMemoId == memo.id && !current.isPlaying) {
            player?.play()
            _uiState.update { it.copy(isPlaying = true) }
            startPlaybackProgress()
            return
        }

        stopPlayback()
        val file = File(memo.filePath)
        if (!file.exists()) return

        val exo = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            prepare()
            play()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        stopPlayback()
                    }
                }
            })
        }
        player = exo
        _uiState.update {
            it.copy(
                playingMemoId = memo.id,
                isPlaying = true,
                playbackPositionMs = 0L,
                playbackDurationMs = memo.durationMs
            )
        }
        startPlaybackProgress()
    }

    private fun startPlaybackProgress() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive) {
                val p = player ?: break
                _uiState.update {
                    it.copy(
                        playbackPositionMs = p.currentPosition.coerceAtLeast(0L),
                        playbackDurationMs = p.duration.takeIf { d -> d > 0L } ?: it.playbackDurationMs
                    )
                }
                delay(150L)
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        player?.release()
        player = null
        _uiState.update {
            it.copy(
                playingMemoId = null,
                isPlaying = false,
                playbackPositionMs = 0L
            )
        }
    }

    fun deleteMemo(memo: VoiceMemoEntity) {
        if (_uiState.value.playingMemoId == memo.id) stopPlayback()
        scope.launch(Dispatchers.IO) {
            runCatching { File(memo.filePath).delete() }
            voiceMemoDao.deleteById(memo.id)
        }
    }
}
