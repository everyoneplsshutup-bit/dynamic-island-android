package com.example.notes

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.DynamicIslandApp
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class VoiceMemoService : Service() {

    companion object {
        const val ACTION_START = "com.example.notes.ACTION_START_MEMO"
        const val ACTION_STOP = "com.example.notes.ACTION_STOP_MEMO"
        private const val NOTIF_ID = 3021
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecordingAndFinish()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (recorder != null) return
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }

        val memoDir = File(filesDir, "memos").apply { if (!exists()) mkdirs() }
        val file = File(memoDir, "memo_${UUID.randomUUID()}.m4a")
        outputFile = file

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        val started = runCatching {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(96_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
        }.isSuccess

        if (!started) {
            runCatching { mr.release() }
            outputFile = null
            stopSelf()
            return
        }

        recorder = mr
        startTimeMs = System.currentTimeMillis()
        val app = applicationContext as? DynamicIslandApp
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                app?.voiceMemoManager?.onRecordingTick(elapsed)
                delay(200L)
            }
        }
    }

    private fun stopRecordingAndFinish() {
        tickJob?.cancel()
        val duration = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
        val app = applicationContext as? DynamicIslandApp
        app?.voiceMemoManager?.onRecordingFinished(outputFile, duration)
        outputFile = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        runCatching { recorder?.release() }
        recorder = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, DynamicIslandApp.CHANNEL_ACTIVE_RECORDING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Recording Voice Memo")
            .setContentText("Microphone active inside Dynamic Island")
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
