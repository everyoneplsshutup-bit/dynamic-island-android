package com.example.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QuickTogglesState(
    val isTorchOn: Boolean = false,
    val torchStrength: Int = 1,
    val maxTorchStrength: Int = 1,
    val isDndOn: Boolean = false,
    val hasDndPermission: Boolean = false,
    val brightness: Int = 128,
    val hasWriteSettingsPermission: Boolean = false
)

class QuickTogglesManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val _state = MutableStateFlow(QuickTogglesState())
    val state: StateFlow<QuickTogglesState> = _state.asStateFlow()

    private var torchCameraId: String? = null

    init {
        discoverTorch()
        refreshSystemStates()
    }

    private fun discoverTorch() {
        val cm = cameraManager ?: return
        runCatching {
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    val maxLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    } else {
                        1
                    }
                    _state.update { it.copy(maxTorchStrength = maxLevel.coerceAtLeast(1)) }
                    break
                }
            }
        }
    }

    fun refreshSystemStates() {
        val dndGranted = notificationManager?.isNotificationPolicyAccessGranted == true
        val dndActive = if (dndGranted) {
            val filter = notificationManager?.currentInterruptionFilter
            filter != null && filter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }
        val canWrite = Settings.System.canWrite(context)
        val currentBrightness = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        }.getOrDefault(128)

        _state.update {
            it.copy(
                hasDndPermission = dndGranted,
                isDndOn = dndActive,
                hasWriteSettingsPermission = canWrite,
                brightness = currentBrightness.coerceIn(10, 255)
            )
        }
    }

    fun toggleFlashlight(): Boolean {
        val cm = cameraManager ?: return false
        val id = torchCameraId ?: return false
        val targetOn = !_state.value.isTorchOn
        return runCatching {
            if (targetOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && _state.value.maxTorchStrength > 1) {
                cm.turnOnTorchWithStrengthLevel(id, _state.value.torchStrength.coerceIn(1, _state.value.maxTorchStrength))
            } else {
                cm.setTorchMode(id, targetOn)
            }
            _state.update { it.copy(isTorchOn = targetOn) }
            true
        }.getOrDefault(false)
    }

    fun setTorchStrength(level: Int) {
        val max = _state.value.maxTorchStrength
        val clamped = level.coerceIn(1, max)
        _state.update { it.copy(torchStrength = clamped) }
        val cm = cameraManager ?: return
        val id = torchCameraId ?: return
        if (_state.value.isTorchOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && max > 1) {
            runCatching { cm.turnOnTorchWithStrengthLevel(id, clamped) }
        }
    }

    fun toggleDoNotDisturb(): Boolean {
        val nm = notificationManager ?: return false
        if (!nm.isNotificationPolicyAccessGranted) {
            openDndAccessSettings()
            return false
        }
        val nextOn = !_state.value.isDndOn
        val targetFilter = if (nextOn) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        return runCatching {
            nm.setInterruptionFilter(targetFilter)
            _state.update { it.copy(isDndOn = nextOn, hasDndPermission = true) }
            true
        }.getOrDefault(false)
    }

    fun openDndAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun setScreenBrightness(value: Int): Boolean {
        if (!Settings.System.canWrite(context)) {
            openWriteSettings()
            return false
        }
        val clamped = value.coerceIn(10, 255)
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clamped
            )
            _state.update { it.copy(brightness = clamped, hasWriteSettingsPermission = true) }
            true
        }.getOrDefault(false)
    }

    fun openWriteSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Android 13 forbids silent Wi-Fi toggling. Open the system Wi-Fi Settings Panel honestly.
     */
    fun openWifiPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Android 13 forbids silent Bluetooth toggling. Open Bluetooth Settings honestly.
     */
    fun openBluetoothPanel() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
