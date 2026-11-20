package com.example.floating

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import androidx.annotation.VisibleForTesting
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
        "MIUI 14 freeform APIs or standard Android Freeform LaunchBounds will be probed on launch."
    ),
    MIUI_SIDEBAR(
        "Path 1: MIUI Freeform Window",
        "Launched using MiuiMultiWindowUtils + freeform launch bounds."
    ),
    FREEFORM_BOUNDS(
        "Path 2: Android Freeform LaunchBounds",
        "Launched with WINDOWING_MODE_FREEFORM (mode 5) + ActivityOptions.setLaunchBounds."
    ),
    STANDARD_FALLBACK(
        "Fallback: Standard App Launch",
        "Device blocked external freeform windowing; launched normally."
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
    companion object {
        const val WINDOWING_MODE_FREEFORM = 5
        const val KEY_LAUNCH_WINDOWING_MODE = "android.activity.windowingMode"
    }

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
        val path = launchIntentFloatingOrFallback(launchIntent, packageName)
        val result = FloatingLaunchResult(packageName, path, true)
        _lastResult.value = result
        scope.launch {
            settingsRepository.update { it.copy(verifiedFloatingPath = path.name) }
        }
        return result
    }

    fun launchIntentFloatingOrFallback(
        intent: Intent,
        targetPackage: String? = intent.`package` ?: intent.component?.packageName
    ): FloatingLaunchPath {
        val bounds = defaultFreeformBounds()

        // Path 1: MIUI 14 MiuiMultiWindowUtils reflection + freeform ActivityOptions bundle
        if (isMiuiDevice() && !targetPackage.isNullOrBlank()) {
            val miuiOptions = buildMiuiFreeformBundle(targetPackage, bounds)
            if (miuiOptions != null) {
                val miuiIntent = Intent(intent).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val miuiAttempt = runCatching {
                    context.startActivity(miuiIntent, miuiOptions)
                }
                if (miuiAttempt.isSuccess) {
                    return FloatingLaunchPath.MIUI_SIDEBAR
                }
            }
        }

        // Path 2: Standard Android Freeform LaunchBounds + WINDOWING_MODE_FREEFORM (5)
        val ffIntent = Intent(intent).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val ffBundle = buildFreeformOptionsBundle(bounds)
        val freeformAttempt = runCatching {
            context.startActivity(ffIntent, ffBundle)
        }
        if (freeformAttempt.isSuccess) {
            return FloatingLaunchPath.FREEFORM_BOUNDS
        }

        // Honest fallback
        val fallback = Intent(intent).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        runCatching { context.startActivity(fallback) }
        return FloatingLaunchPath.STANDARD_FALLBACK
    }

    fun defaultFreeformBounds(): Rect {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels.coerceAtLeast(360)
        val h = dm.heightPixels.coerceAtLeast(640)
        return Rect(
            (w * 0.08f).toInt(),
            (h * 0.14f).toInt(),
            (w * 0.92f).toInt(),
            (h * 0.68f).toInt()
        )
    }

    @VisibleForTesting
    fun buildFreeformOptionsBundle(bounds: Rect): Bundle {
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            options.setLaunchBounds(bounds)
        }
        runCatching {
            val m = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
            m.invoke(options, WINDOWING_MODE_FREEFORM)
        }
        val bundle = options.toBundle() ?: Bundle()
        bundle.putInt(KEY_LAUNCH_WINDOWING_MODE, WINDOWING_MODE_FREEFORM)
        return bundle
    }

    private fun buildMiuiFreeformBundle(packageName: String, bounds: Rect): Bundle? {
        return runCatching {
            val clazz = Class.forName("android.util.MiuiMultiWindowUtils")
            val activityOptions = runCatching {
                val m = clazz.getMethod(
                    "getActivityOptions",
                    Context::class.java,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType
                )
                m.invoke(null, context, packageName, true, false) as? ActivityOptions
            }.getOrNull() ?: runCatching {
                val m = clazz.getMethod("getActivityOptions", Context::class.java, String::class.java)
                m.invoke(null, context, packageName) as? ActivityOptions
            }.getOrNull() ?: return null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activityOptions.setLaunchBounds(bounds)
            }
            val bundle = activityOptions.toBundle() ?: Bundle()
            bundle.putInt(KEY_LAUNCH_WINDOWING_MODE, WINDOWING_MODE_FREEFORM)
            bundle
        }.getOrNull()
    }

    fun isMiuiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
    }
}
