package com.example.ocr

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.DynamicIslandApp
import com.example.R
import com.example.island.AlertKind
import com.example.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Handles both:
 * 1. Single-frame MediaProjection capture (Shelf screenshot / <API 30 Tier B OCR fallback)
 * 2. Interactive Screen Recording initiated from Dynamic Island Tools with 3-2-1 countdown & Movies/DynamicIsland MP4 output.
 */
class MediaProjectionCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val ACTION_STOP_RECORDING = "com.example.action.STOP_SCREEN_RECORDING"
        private const val NOTIF_ID = 3042
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private var outputUri: Uri? = null
    private var outputPfd: ParcelFileDescriptor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_RECORDING) {
            finalizeAndStopRecording(notifySaved = true)
            return START_NOT_STICKY
        }

        val mode = intent?.getStringExtra(MediaProjectionPromptActivity.EXTRA_MODE)
            ?: MediaProjectionPromptActivity.MODE_VAULT_SCREENSHOT

        val notif = buildNotification(isRecording = mode == MediaProjectionPromptActivity.MODE_SCREEN_RECORD)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        @Suppress("DEPRECATION")
        val bounds = intent?.getParcelableExtra<Rect>(MediaProjectionPromptActivity.EXTRA_BOUNDS)
        val tapX = intent?.getIntExtra(MediaProjectionPromptActivity.EXTRA_TAP_X, -1) ?: -1
        val tapY = intent?.getIntExtra(MediaProjectionPromptActivity.EXTRA_TAP_Y, -1) ?: -1

        if (resultCode != Activity.RESULT_OK || data == null) {
            cleanupAndStop()
            return START_NOT_STICKY
        }

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val mp = mgr?.getMediaProjection(resultCode, data)
        if (mp == null) {
            cleanupAndStop()
            return START_NOT_STICKY
        }
        projection = mp

        if (mode == MediaProjectionPromptActivity.MODE_SCREEN_RECORD) {
            startScreenRecording(mp)
        } else {
            captureSingleFrame(mp, mode, bounds, tapX, tapY)
        }
        return START_NOT_STICKY
    }

    private fun startScreenRecording(mp: MediaProjection) {
        val app = applicationContext as? DynamicIslandApp
        val mainHandler = Handler(Looper.getMainLooper())
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                finalizeAndStopRecording(notifySaved = true)
            }
        }
        projectionCallback = callback
        runCatching { mp.registerCallback(callback, mainHandler) }

        recordingJob?.cancel()
        recordingJob = scope.launch {
            app?.islandController?.collapse()
            for (sec in 3 downTo 1) {
                app?.soundManager?.playTap()
                app?.islandController?.upsertActivity(
                    IslandActivity.ScreenRecording(
                        elapsedMs = 0L,
                        isCountdown = true,
                        countdownSec = sec
                    ),
                    autoShowCompact = true
                )
                delay(850L)
            }

            val started = beginMediaRecorder(mp)
            if (!started) {
                app?.soundManager?.playError()
                app?.islandController?.showTransientBanner("Screen recording failed to start")
                finalizeAndStopRecording(notifySaved = false)
                return@launch
            }

            app?.soundManager?.playSuccess()
            app?.islandController?.setScreenRecording(true)
            val startedAtMs = System.currentTimeMillis()
            while (isActive && projection === mp && mediaRecorder != null) {
                val elapsed = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
                app?.islandController?.upsertActivity(
                    IslandActivity.ScreenRecording(
                        elapsedMs = elapsed,
                        isCountdown = false,
                        countdownSec = 0
                    ),
                    autoShowCompact = true
                )
                delay(1000L)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun beginMediaRecorder(mp: MediaProjection): Boolean {
        return runCatching {
            val dm = resources.displayMetrics
            val rawW = (dm.widthPixels.coerceAtLeast(480) / 2) * 2
            val rawH = (dm.heightPixels.coerceAtLeast(640) / 2) * 2
            val density = dm.densityDpi

            val fileName = "DynamicIsland_${System.currentTimeMillis()}.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DynamicIsland")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            val pfd = contentResolver.openFileDescriptor(uri, "rw") ?: return false
            outputUri = uri
            outputPfd = pfd

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoSize(rawW, rawH)
            recorder.setVideoFrameRate(30)
            recorder.setVideoEncodingBitRate(4_500_000)
            recorder.setOutputFile(pfd.fileDescriptor)
            recorder.prepare()

            virtualDisplay = mp.createVirtualDisplay(
                "DynamicIslandScreenRecord",
                rawW,
                rawH,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                Handler(Looper.getMainLooper())
            )
            recorder.start()
            mediaRecorder = recorder
            true
        }.getOrDefault(false)
    }

    private fun finalizeAndStopRecording(notifySaved: Boolean) {
        recordingJob?.cancel()
        recordingJob = null
        val hadRecorder = mediaRecorder != null
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        runCatching { outputPfd?.close() }
        outputPfd = null

        val uri = outputUri
        outputUri = null
        if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            }
        }

        val app = applicationContext as? DynamicIslandApp
        app?.islandController?.removeActivity("screen_rec")
        app?.islandController?.setScreenRecording(false)
        if (notifySaved && hadRecorder) {
            app?.soundManager?.playSuccess()
            app?.islandController?.upsertActivity(
                IslandActivity.Alert(
                    id = "screen_rec_saved_${System.currentTimeMillis()}",
                    kind = AlertKind.GENERAL,
                    title = "Recording saved",
                    subtitle = "Saved to Movies/DynamicIsland"
                ),
                autoShowCompact = true,
                autoDismissMs = 2200L
            )
        }
        cleanupAndStop()
    }

    private fun captureSingleFrame(mp: MediaProjection, mode: String, bounds: Rect?, tapX: Int, tapY: Int) {
        val dm = resources.displayMetrics
        val width = dm.widthPixels.coerceAtLeast(1)
        val height = dm.heightPixels.coerceAtLeast(1)
        val density = dm.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        val mainHandler = Handler(Looper.getMainLooper())
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                cleanupAndStop()
            }
        }
        projectionCallback = callback
        runCatching { mp.registerCallback(callback, mainHandler) }

        mainHandler.postDelayed({
            if (projection !== mp) return@postDelayed
            virtualDisplay = runCatching {
                mp.createVirtualDisplay(
                    "DynamicIslandSingleCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface,
                    null,
                    mainHandler
                )
            }.getOrNull()
            if (virtualDisplay == null) {
                cleanupAndStop()
                return@postDelayed
            }

            reader.setOnImageAvailableListener({ r ->
                r.setOnImageAvailableListener(null, null)
                val image = r.acquireLatestImage()
                if (image != null) {
                    val plane = image.planes[0]
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val bmp = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bmp.copyPixelsFromBuffer(plane.buffer)
                    image.close()

                    val croppedScreen = Bitmap.createBitmap(bmp, 0, 0, width, height)
                    if (croppedScreen !== bmp) bmp.recycle()

                    processCapturedBitmap(croppedScreen, mode, bounds, tapX, tapY)
                } else {
                    cleanupAndStop()
                }
            }, Handler(Looper.getMainLooper()))
        }, 180L)
    }

    private fun processCapturedBitmap(frame: Bitmap, mode: String, bounds: Rect?, tapX: Int, tapY: Int) {
        val app = applicationContext as? DynamicIslandApp
        scope.launch {
            try {
                if (app != null) {
                    if (mode == MediaProjectionPromptActivity.MODE_OCR_TIER_B) {
                        val recognized = TierBMlKitOcr.recognizeSelectionAtPoint(
                            fullFrame = frame,
                            elementBounds = bounds,
                            tapX = tapX,
                            tapY = tapY,
                            preserveFrozenFrameForOverlay = true
                        )
                        if (recognized != null) {
                            app.soundManager.playTap()
                            app.islandController.setOcrSelection(recognized.selectionState)
                        } else {
                            OcrBitmapHolder.clear()
                            if (!frame.isRecycled) frame.recycle()
                            app.islandController.showTransientBanner("No text detected at selected point")
                        }
                    } else {
                        app.vaultManager.saveBitmap(frame, sourceType = "CAPTURED", namePrefix = "island_shot")
                        if (!frame.isRecycled) frame.recycle()
                    }
                } else {
                    if (!frame.isRecycled) frame.recycle()
                }
            } finally {
                cleanupAndStop()
            }
        }
    }

    private fun cleanupAndStop() {
        val mp = projection
        val cb = projectionCallback
        projection = null
        projectionCallback = null
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        if (mp != null && cb != null) {
            runCatching { mp.unregisterCallback(cb) }
        }
        runCatching { mp?.stop() }
        virtualDisplay = null
        imageReader = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(isRecording: Boolean): Notification {
        return NotificationCompat.Builder(this, DynamicIslandApp.CHANNEL_ACTIVE_RECORDING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isRecording) "Recording Screen" else "Single-Frame Capture")
            .setContentText(if (isRecording) "Tap the island pill to stop" else "Capturing selected frame on-device")
            .setSilent(true)
            .build()
    }
}
