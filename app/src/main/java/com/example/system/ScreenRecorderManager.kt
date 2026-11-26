package com.example.system

import android.content.Context
import android.content.Intent
import com.example.ocr.MediaProjectionCaptureService
import com.example.ocr.MediaProjectionPromptActivity

/**
 * Coordinates interactive Screen Recording from the Dynamic Island Tools panel.
 * Launches fresh MediaProjection user consent every start, shows a 3-2-1 countdown pill,
 * records MP4 cleanly to Movies/DynamicIsland, and stops immediately on tap.
 */
object ScreenRecorderManager {

    fun startRecording(context: Context) {
        MediaProjectionPromptActivity.launchForScreenRecord(context)
    }

    fun stopRecording(context: Context) {
        val stopIntent = Intent(context, MediaProjectionCaptureService::class.java).apply {
            action = MediaProjectionCaptureService.ACTION_STOP_RECORDING
        }
        runCatching { context.startService(stopIntent) }
    }
}
