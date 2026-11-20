package com.example.ocr

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.DynamicIslandApp
import com.example.R
import com.example.island.AlertKind
import com.example.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Single-frame MediaProjection capture service.
 * Immediately stops projection, discards the frame, and stops itself after processing.
 */
class MediaProjectionCaptureService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val NOTIF_ID = 3042
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = buildNotification()
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
        val mode = intent?.getStringExtra(MediaProjectionPromptActivity.EXTRA_MODE)
            ?: MediaProjectionPromptActivity.MODE_VAULT_SCREENSHOT
        @Suppress("DEPRECATION")
        val bounds = intent?.getParcelableExtra<Rect>(MediaProjectionPromptActivity.EXTRA_BOUNDS)

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

        captureSingleFrame(mp, mode, bounds)
        return START_NOT_STICKY
    }

    private fun captureSingleFrame(mp: MediaProjection, mode: String, bounds: Rect?) {
        val dm = resources.displayMetrics
        val width = dm.widthPixels.coerceAtLeast(1)
        val height = dm.heightPixels.coerceAtLeast(1)
        val density = dm.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        // Delay ~180ms so the transparent prompt activity finishes animating away before capture
        Handler(Looper.getMainLooper()).postDelayed({
            virtualDisplay = mp.createVirtualDisplay(
                "DynamicIslandSingleCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )

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

                    processCapturedBitmap(croppedScreen, mode, bounds)
                } else {
                    cleanupAndStop()
                }
            }, Handler(Looper.getMainLooper()))
        }, 180L)
    }

    private fun processCapturedBitmap(frame: Bitmap, mode: String, bounds: Rect?) {
        val app = applicationContext as? DynamicIslandApp
        scope.launch {
            try {
                if (app != null) {
                    if (mode == MediaProjectionPromptActivity.MODE_OCR_TIER_B) {
                        val recognized = TierBMlKitOcr.recognizeCroppedElement(frame, bounds)
                        if (!recognized.isNullOrBlank()) {
                            app.clipboardMonitor.copyToClipboard(recognized)
                            app.islandController.upsertActivity(
                                IslandActivity.Alert(
                                    id = "ocr_copied_${System.currentTimeMillis()}",
                                    kind = AlertKind.OCR_COPIED,
                                    title = "Text Copied (Tier B OCR)",
                                    subtitle = recognized.take(56)
                                ),
                                autoShowCompact = true,
                                autoDismissMs = 3500L
                            )
                        } else {
                            app.islandController.showTransientBanner("No text detected in selected element")
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
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { projection?.stop() }
        virtualDisplay = null
        imageReader = null
        projection = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, DynamicIslandApp.CHANNEL_ACTIVE_RECORDING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Single-Frame Capture")
            .setContentText("Capturing selected frame on-device")
            .setSilent(true)
            .build()
    }
}
