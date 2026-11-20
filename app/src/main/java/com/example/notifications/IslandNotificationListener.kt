package com.example.notifications

import android.app.Notification
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.DynamicIslandApp
import com.example.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IslandNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val clockPackages = setOf(
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.miui.clock"
    )

    private val screenshotPackages = setOf(
        "com.miui.screenshot",
        "com.android.systemui"
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        val app = applicationContext as? DynamicIslandApp ?: return
        scope.launch(Dispatchers.Main) {
            app.mediaHub.refreshSessions()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        val app = applicationContext as? DynamicIslandApp ?: return
        scope.launch(Dispatchers.Main) {
            app.mediaHub.stop()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim().orEmpty()

        val app = applicationContext as? DynamicIslandApp ?: return

        // 1. Clock timer mirroring
        if (pkg in clockPackages && (title.contains("Timer", ignoreCase = true) || text.contains(":"))) {
            val parsedMs = parseChronometerOrClock(notification, "$title $text")
            if (parsedMs != null && parsedMs > 0L) {
                app.timerEngine.updateMirroredClockTimer(title.ifEmpty { "Clock Timer" }, parsedMs)
            }
            return
        }

        // 2. 3-finger screenshot capture via MIUI / SystemUI screenshot notification URI or BigPicture
        if (pkg in screenshotPackages && isScreenshotNotification(title, text)) {
            scope.launch {
                val settings = app.settingsRepository.settingsFlow.first()
                if (settings.vaultEnabled && settings.vaultAutoCapture3Finger) {
                    val uri = extractImageUri(notification)
                    if (uri != null) {
                        app.vaultManager.saveFromUri(uri, sourceType = "SCREENSHOT_3FINGER")
                    } else {
                        extractPictureBitmap(notification)?.let { bmp ->
                            app.vaultManager.saveBitmap(bmp, sourceType = "SCREENSHOT_3FINGER", namePrefix = "screenshot")
                        }
                    }
                }
            }
            return
        }

        // Ignore media-style notifications (handled cleanly by MediaHub)
        if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            scope.launch(Dispatchers.Main) { app.mediaHub.refreshSessions() }
            return
        }

        if (title.isEmpty() && text.isEmpty()) return

        scope.launch {
            val settings = app.settingsRepository.settingsFlow.first()
            if (!settings.notificationsEnabled) return@launch

            val appName = runCatching {
                val ai = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(ai).toString()
            }.getOrDefault(pkg)

            val existingRule = app.database.appRuleDao().getByPackage(pkg)
            if (existingRule == null) {
                app.database.appRuleDao().upsert(
                    AppRuleEntity(packageName = pkg, appName = appName, allowIslandPreview = true)
                )
            } else if (!existingRule.allowIslandPreview) {
                return@launch
            }

            val entity = NotificationEntity(
                notificationKey = sbn.key,
                packageName = pkg,
                appName = appName,
                title = title.ifEmpty { appName },
                text = text.ifEmpty { subText },
                category = notification.category ?: "",
                timestamp = sbn.postTime
            )
            app.database.notificationDao().insert(entity)

            // Preview only — compact pill, never auto-expand message text!
            app.islandController.upsertActivity(
                IslandActivity.NotificationPreview(
                    id = "notif_${sbn.key}",
                    packageName = pkg,
                    appName = appName,
                    title = title.ifEmpty { appName },
                    text = text.ifEmpty { subText },
                    timestamp = sbn.postTime
                ),
                autoShowCompact = true,
                autoDismissMs = 4500L
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val app = applicationContext as? DynamicIslandApp ?: return
        if (sbn.packageName in clockPackages) {
            app.timerEngine.updateMirroredClockTimer(null, null)
        }
        app.islandController.removeActivity("notif_${sbn.key}")
    }

    private fun isScreenshotNotification(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        return combined.contains("screenshot") || combined.contains("screen capture")
    }

    @Suppress("DEPRECATION")
    private fun extractImageUri(notification: Notification): Uri? {
        val extras = notification.extras ?: return null
        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(android.content.Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            extras.getParcelable(android.content.Intent.EXTRA_STREAM)
        }
        if (streamUri != null) return streamUri
        return null
    }

    @Suppress("DEPRECATION")
    private fun extractPictureBitmap(notification: Notification): android.graphics.Bitmap? {
        val extras = notification.extras ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_PICTURE, android.graphics.Bitmap::class.java)
        } else {
            extras.getParcelable(Notification.EXTRA_PICTURE)
        }
    }

    private fun parseChronometerOrClock(notification: Notification, text: String): Long? {
        if (notification.`when` > System.currentTimeMillis()) {
            return (notification.`when` - System.currentTimeMillis()).coerceAtLeast(0L)
        }
        val match = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""").find(text) ?: return null
        val groups = match.groupValues
        return if (groups[3].isNotEmpty()) {
            val h = groups[1].toLongOrNull() ?: 0L
            val m = groups[2].toLongOrNull() ?: 0L
            val s = groups[3].toLongOrNull() ?: 0L
            ((h * 3600L) + (m * 60L) + s) * 1000L
        } else {
            val m = groups[1].toLongOrNull() ?: 0L
            val s = groups[2].toLongOrNull() ?: 0L
            ((m * 60L) + s) * 1000L
        }
    }
}
