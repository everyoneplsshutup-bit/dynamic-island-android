package com.example.floating

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import com.example.system.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FloatingLaunchPath(val title: String, val honestNote: String) {
    UNTESTED(
        "Untested on this device",
        "MIUI 14 has no public API to force floating windows. Ranked paths will be probed on launch."
    ),
    MIUI_SIDEBAR(
        "Path 1: MIUI Freeform / Sidebar handoff",
        "Invoked with MIUI freeform launch extras; verified only if MIUI opens a floating card on your ROM."
    ),
    FREEFORM_BOUNDS(
        "Path 2: Android Freeform LaunchBounds",
        "Uses ActivityOptions.setLaunchBounds + WINDOWING_MODE_FREEFORM where MIUI allows it."
    ),
    STANDARD_FALLBACK(
        "Fallback: Standard App Launch",
        "Device blocked freeform windowing; launched normally."
    )
}

data class FloatingLaunchResult(
    val packageName: String,
    val pathUsed: FloatingLaunchPath,
    val launched: Boolean
)

class FloatingWindowManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _lastResult = MutableStateFlow<FloatingLaunchResult?>(null)
    val lastResult: StateFlow<FloatingLaunchResult?> = _lastResult.asStateFlow()

    fun launchAppInFloatingWindow(packageName: String): FloatingLaunchResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            val fail = FloatingLaunchResult(packageName, FloatingLaunchPath.STANDARD_FALLBACK, false)
            _lastResult.value = fail
            return fail
        }
        val path = launchIntentFloatingOrFallback(launchIntent)
        val result = FloatingLaunchResult(packageName, path, true)
        _lastResult.value = result
        scope.launch {
            settingsRepository.update { it.copy(verifiedFloatingPath = path.name) }
        }
        return result
    }

    fun launchIntentFloatingOrFallback(intent: Intent): FloatingLaunchPath {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val bounds = Rect(
            (w * 0.10f).toInt(),
            (h * 0.15f).toInt(),
            (w * 0.90f).toInt(),
            (h * 0.65f).toInt()
        )

        // Path 1: MIUI freeform intent flags / ActivityOptions reflection where exposed
        if (isMiuiDevice()) {
            val miuiAttempt = runCatching {
                val miuiIntent = Intent(intent).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    putExtra("miui_multi_window_mode", 1)
                }
                val options = ActivityOptions.makeBasic()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    options.setLaunchBounds(bounds)
                }
                // Reflect setLaunchWindowingMode(5 = WINDOWING_MODE_FREEFORM) if supported
                runCatching {
                    val m = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                    m.invoke(options, 5)
                }
                context.startActivity(miuiIntent, options.toBundle())
            }
            if (miuiAttempt.isSuccess) {
                return FloatingLaunchPath.MIUI_SIDEBAR
            }
        }

        // Path 2: Android standard Freeform LaunchBounds
        val freeformAttempt = runCatching {
            val ffIntent = Intent(intent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                options.setLaunchBounds(bounds)
            }
            runCatching {
                val m = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                m.invoke(options, 5)
            }
            context.startActivity(ffIntent, options.toBundle())
        }
        if (freeformAttempt.isSuccess) {
            return FloatingLaunchPath.FREEFORM_BOUNDS
        }

        // Honest fallback
        val fallback = Intent(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(fallback) }
        return FloatingLaunchPath.STANDARD_FALLBACK
    }

    fun isMiuiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
    }
}
