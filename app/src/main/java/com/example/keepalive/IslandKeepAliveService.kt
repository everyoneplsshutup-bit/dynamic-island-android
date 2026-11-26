package com.example.keepalive

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.compose.runtime.mutableFloatStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.DynamicIslandApp
import com.example.MainActivity
import com.example.R
import com.example.island.DeadZoneGeometry
import com.example.island.ExclusionZone
import com.example.island.IslandMode
import com.example.island.IslandService
import com.example.island.IslandState
import com.example.island.OverlayWindowHost
import com.example.system.IslandSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors AccessibilityService liveness and hosts a reliable TYPE_APPLICATION_OVERLAY
 * fallback whenever Accessibility is not currently hosting the overlay (e.g. MIUI delayed attach
 * or Overlay-granted setup) and `settings.islandEnabled` is true.
 */
class IslandKeepAliveService : Service() {

    companion object {
        private const val NOTIF_ID = 3001
        private const val ALERT_ACCESSIBILITY_OFF_ID = 3002

        private val _isFallbackHosting = MutableStateFlow(false)
        val isFallbackHosting: StateFlow<Boolean> = _isFallbackHosting.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, IslandKeepAliveService::class.java)
            runCatching { contextCompatStart(context, intent) }
        }

        private fun contextCompatStart(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null
    private var overlaySyncJob: Job? = null

    private val pullDownFractionState = mutableFloatStateOf(0f)
    private val localExclusionZone = MutableStateFlow<ExclusionZone?>(null)
    private val localBlurSupported = MutableStateFlow(false)
    private var fallbackHost: OverlayWindowHost? = null

    private var latestSettings: IslandSettings = IslandSettings()
    private var latestState: IslandState = IslandState()
    private var latestKeepAwake: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }

        ensureFallbackHostInitialized()
        startMonitor()
        startFallbackOverlayLoop()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val zone = DeadZoneGeometry.measureLive(this, latestSettings)
        localExclusionZone.value = zone
        fallbackHost?.updateLayout(latestState, latestSettings, latestKeepAwake, zone, forceTouch = true)
    }

    private fun ensureFallbackHostInitialized() {
        if (fallbackHost != null) return
        val app = applicationContext as? DynamicIslandApp ?: return
        fallbackHost = OverlayWindowHost(
            serviceContext = this,
            scope = scope,
            preferAccessibilityOverlay = false,
            pullDownFractionState = pullDownFractionState,
            liveExclusionZone = localExclusionZone,
            crossWindowBlurSupported = localBlurSupported,
            onBlurSupportedChanged = { localBlurSupported.value = it },
            onSelectModePoint = { _, _ ->
                app.islandController.setSelectMode(false)
                app.islandController.showTransientBanner("Enable Accessibility Service for Select OCR")
            }
        )
    }

    private fun startFallbackOverlayLoop() {
        if (overlaySyncJob != null) return
        val app = applicationContext as? DynamicIslandApp ?: return
        app.mediaHub.start()

        overlaySyncJob = scope.launch {
            launch {
                combine(
                    IslandService.isHostingOverlay,
                    app.settingsRepository.settingsFlow
                ) { accHosting, settings ->
                    accHosting to settings
                }.collectLatest { (accHosting, settings) ->
                    latestSettings = settings
                    val zone = DeadZoneGeometry.measureLive(this@IslandKeepAliveService, settings)
                    localExclusionZone.value = zone

                    val host = fallbackHost ?: return@collectLatest
                    val shouldHostFallback = !accHosting &&
                        settings.islandEnabled &&
                        Settings.canDrawOverlays(this@IslandKeepAliveService)

                    if (shouldHostFallback) {
                        if (!host.isAttached.value) {
                            val ok = host.attach(app, settings, latestState, latestKeepAwake)
                            _isFallbackHosting.value = ok
                            app.islandController.setHiddenReason(
                                if (ok) "Visible (Keep-Alive overlay)" else "Overlay attach blocked by system",
                                serviceAlive = true
                            )
                        } else {
                            host.updateLayout(latestState, settings, latestKeepAwake, zone, forceTouch = true)
                            val reason = when {
                                latestState.isDeviceLocked && settings.lockScreenPreset == com.example.system.LockScreenPreset.HIDDEN ->
                                    "Hidden on lock screen"
                                latestState.mode == IslandMode.IDLE && settings.whenIdleMode == com.example.system.WhenIdleMode.HIDDEN ->
                                    "Idle mode set to Hidden"
                                else -> "Visible (Keep-Alive overlay)"
                            }
                            app.islandController.setHiddenReason(reason, serviceAlive = true)
                        }
                    } else if (host.isAttached.value) {
                        host.detach()
                        _isFallbackHosting.value = false
                        if (!accHosting && !settings.islandEnabled) {
                            app.islandController.setHiddenReason("Master switch turned OFF in Home Hub", serviceAlive = true)
                        }
                    } else if (!accHosting) {
                        val reason = when {
                            !settings.islandEnabled -> "Master switch turned OFF in Home Hub"
                            !Settings.canDrawOverlays(this@IslandKeepAliveService) ->
                                "Draw-over-other-apps & Accessibility both inactive"
                            else -> "Waiting for overlay host"
                        }
                        app.islandController.setHiddenReason(reason, serviceAlive = true)
                    }
                }
            }

            launch {
                app.islandController.state.collectLatest { state ->
                    latestState = state
                    fallbackHost?.updateLayout(state, latestSettings, latestKeepAwake, localExclusionZone.value)
                }
            }

            launch {
                app.quickTogglesManager.state.collectLatest { toggles ->
                    if (latestKeepAwake != toggles.isKeepAwakeOn) {
                        latestKeepAwake = toggles.isKeepAwakeOn
                        fallbackHost?.updateLayout(latestState, latestSettings, latestKeepAwake, localExclusionZone.value)
                    }
                }
            }
        }
    }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            val app = applicationContext as? DynamicIslandApp ?: return@launch
            var alerted = false
            while (isActive) {
                val active = IslandService.isRunning.value
                val settings = app.settingsRepository.settingsFlow.first()
                latestSettings = settings
                val wasEnabled = settings.accessibilityWasEnabled
                val accHosting = IslandService.isHostingOverlay.value
                val host = fallbackHost

                if (host != null && !accHosting && settings.islandEnabled && Settings.canDrawOverlays(this@IslandKeepAliveService)) {
                    if (!host.isAttached.value) {
                        localExclusionZone.value = DeadZoneGeometry.measureLive(this@IslandKeepAliveService, settings)
                        val ok = host.attach(app, settings, latestState, latestKeepAwake)
                        _isFallbackHosting.value = ok
                    }
                }

                if (active) {
                    alerted = false
                    (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                        ?.cancel(ALERT_ACCESSIBILITY_OFF_ID)
                } else if (wasEnabled && !active && !alerted) {
                    alerted = true
                    postAccessibilityStoppedNotification()
                }
                delay(10_000L)
            }
        }
    }

    private fun postAccessibilityStoppedNotification() {
        val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            this,
            901,
            accIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, DynamicIslandApp.CHANNEL_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Dynamic Island Accessibility switched off")
            .setContentText("MIUI stopped the overlay service. Tap to re-enable Accessibility.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(0, "Open Accessibility", pi)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(ALERT_ACCESSIBILITY_OFF_ID, notif)
    }

    private fun buildForegroundNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this,
            900,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, DynamicIslandApp.CHANNEL_KEEP_ALIVE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Dynamic Island Active")
            .setContentText("Keeping island overlay & MIUI status active")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        overlaySyncJob?.cancel()
        fallbackHost?.detach()
        fallbackHost = null
        _isFallbackHosting.value = false
        scope.cancel()
        super.onDestroy()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as? DynamicIslandApp
            app?.let {
                IslandKeepAliveService.start(context)
            }
        }
    }
}
