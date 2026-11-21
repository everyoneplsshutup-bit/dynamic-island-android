package com.example.notifications

import android.app.Notification
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.example.DynamicIslandApp
import com.example.island.IslandActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IslandNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var staleReaperJob: kotlinx.coroutines.Job? = null

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
        staleReaperJob?.cancel()
        staleReaperJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000L)
                val activeIds = runCatching {
                    activeNotifications?.map { "notif_${it.key}" }?.toSet()
                }.getOrNull() ?: continue
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    app.islandController.pruneStaleNotifications(activeIds)
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        staleReaperJob?.cancel()
        staleReaperJob = null
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

        // 2. 3-finger screenshot capture via MIUI / SystemUI screenshot notification URI, BigPicture, or actions
        if (pkg in screenshotPackages && isScreenshotNotification(pkg, title, text, notification)) {
            scope.launch {
                val settings = app.settingsRepository.settingsFlow.first()
                if (settings.vaultEnabled && settings.vaultAutoCapture3Finger) {
                    val uri = extractImageUri(notification)
                    val savedFromUri = if (uri != null) {
                        app.vaultManager.saveFromUri(uri, sourceType = "SCREENSHOT_3FINGER")
                    } else null

                    if (savedFromUri == null) {
                        val bmp = extractPictureBitmap(notification)
                        if (bmp != null) {
                            app.vaultManager.saveBitmap(bmp, sourceType = "SCREENSHOT_3FINGER", namePrefix = "screenshot")
                        } else if (!settings.vaultScreenshotsTreeUri.isNullOrBlank()) {
                            runCatching {
                                app.vaultManager.ingestRecentScreenshotFromTree(Uri.parse(settings.vaultScreenshotsTreeUri))
                            }
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
            val notifCfg = settings.eventConfig(com.example.system.EventKind.NOTIFICATIONS)
            if (!settings.notificationsEnabled || !notifCfg.enabled) return@launch

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
                autoDismissMs = 3800L,
                autoExpandToFull = notifCfg.autoExpand
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

    private fun isScreenshotNotification(
        pkg: String,
        title: String,
        text: String,
        notification: Notification
    ): Boolean {
        val combined = "$title $text".lowercase()
        if (combined.contains("screenshot") || combined.contains("screen capture")) return true
        return pkg == "com.miui.screenshot" && extractPictureBitmap(notification) != null
    }

    @Suppress("DEPRECATION")
    private fun extractImageUri(notification: Notification): Uri? {
        val extras = notification.extras ?: return null
        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            extras.getParcelable(Intent.EXTRA_STREAM)
        }
        if (streamUri != null) return streamUri

        notification.actions?.forEach { action ->
            val pi = action.actionIntent ?: return@forEach
            val intentUri = runCatching {
                // PendingIntent#getIntent is hidden on some builds; inspect notification.extras instead if blocked
                val m = android.app.PendingIntent::class.java.getMethod("getIntent")
                val target = m.invoke(pi) as? Intent
                target?.data ?: target?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }.getOrNull()
            if (intentUri != null && intentUri.scheme == "content") return intentUri
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun extractPictureBitmap(notification: Notification): Bitmap? {
        val extras = notification.extras ?: return null
        val bigPic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
        } else {
            extras.getParcelable(Notification.EXTRA_PICTURE)
        }
        if (bigPic != null) return bigPic

        val largeIconBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, Bitmap::class.java)
                ?: extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
        } else {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG)
                ?: extras.getParcelable(Notification.EXTRA_LARGE_ICON)
        }
        if (largeIconBmp != null) return largeIconBmp

        return runCatching {
            notification.getLargeIcon()?.loadDrawable(this)?.toBitmap()
        }.getOrNull()
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
