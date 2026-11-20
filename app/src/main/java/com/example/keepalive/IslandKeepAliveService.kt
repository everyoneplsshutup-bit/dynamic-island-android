package com.example.keepalive

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.DynamicIslandApp
import com.example.MainActivity
import com.example.R
import com.example.island.IslandService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors AccessibilityService liveness and detects MIUI force-stop / system kill
 * switching Accessibility OFF, offering a one-tap path back to Accessibility settings.
 */
class IslandKeepAliveService : Service() {

    companion object {
        private const val NOTIF_ID = 3001
        private const val ALERT_ACCESSIBILITY_OFF_ID = 3002

        fun start(context: Context) {
            val intent = Intent(context, IslandKeepAliveService::class.java)
            runCatching { ContextCompatStart(context, intent) }
        }

        private fun ContextCompatStart(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitorJob: Job? = null

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

        startMonitor()
        return START_STICKY
    }

    private fun startMonitor() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            val app = applicationContext as? DynamicIslandApp ?: return@launch
            var alerted = false
            while (isActive) {
                val active = IslandService.isRunning.value
                val wasEnabled = app.settingsRepository.settingsFlow.first().accessibilityWasEnabled
                if (active) {
                    alerted = false
                    (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                        ?.cancel(ALERT_ACCESSIBILITY_OFF_ID)
                } else if (wasEnabled && !active && !alerted) {
                    alerted = true
                    postAccessibilityStoppedNotification()
                }
                delay(15_000L)
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
            .setContentText("Monitoring island overlay & MIUI survival status")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
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
