package com.example.system

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.island.IslandController
import com.example.island.PrivacyIndicatorState

/**
 * Monitors active camera / microphone sensors so the island displays
 * Apple-style green (Camera) / orange (Mic) privacy indicator dots without
 * requesting extra sensitive permissions.
 */
class PrivacyOpsMonitor(
    private val context: Context,
    private val islandController: IslandController
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var started = false
    private val activeCameraIds = HashSet<String>()
    private var micActive = false
    private var micApps = emptyList<String>()

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            activeCameraIds.add(cameraId)
            publish()
        }

        override fun onCameraAvailable(cameraId: String) {
            activeCameraIds.remove(cameraId)
            publish()
        }
    }

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<android.media.AudioRecordingConfiguration>?) {
            val active = configs.orEmpty()
            micActive = active.isNotEmpty()
            micApps = active.mapNotNull { cfg ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        val uid = cfg.clientAudioSessionId
                        "Mic stream #$uid"
                    }.getOrNull()
                } else {
                    null
                }
            }.distinct()
            publish()
        }
    }

    fun start() {
        if (started) return
        started = true
        runCatching {
            cameraManager?.registerAvailabilityCallback(cameraCallback, mainHandler)
        }
        runCatching {
            audioManager?.registerAudioRecordingCallback(recordingCallback, mainHandler)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val active = audioManager?.activeRecordingConfigurations.orEmpty()
                micActive = active.isNotEmpty()
            }
        }
        publish()
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { cameraManager?.unregisterAvailabilityCallback(cameraCallback) }
        runCatching { audioManager?.unregisterAudioRecordingCallback(recordingCallback) }
        activeCameraIds.clear()
        micActive = false
        micApps = emptyList()
        islandController.updatePrivacyIndicator(PrivacyIndicatorState())
    }

    private fun publish() {
        val camOn = activeCameraIds.isNotEmpty()
        val fgPkg = islandController.state.value.currentForegroundPackage
        val fgLabel = fgPkg?.let { pkg ->
            runCatching {
                val ai = context.packageManager.getApplicationInfo(pkg, 0)
                context.packageManager.getApplicationLabel(ai).toString().trim()
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }
        val apps = buildList {
            if (camOn) {
                add(if (fgLabel != null) "Camera · $fgLabel" else "Camera in use")
            }
            if (micActive) {
                if (fgLabel != null) {
                    add("Microphone · $fgLabel")
                } else if (micApps.isNotEmpty()) {
                    addAll(micApps)
                } else {
                    add("Microphone active")
                }
            }
        }
        islandController.updatePrivacyIndicator(
            PrivacyIndicatorState(
                cameraActive = camOn,
                micActive = micActive,
                locationActive = false,
                activeApps = apps
            )
        )
    }
}
