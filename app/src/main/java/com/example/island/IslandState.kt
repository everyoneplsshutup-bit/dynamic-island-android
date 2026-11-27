package com.example.island

import android.app.PendingIntent
import android.app.RemoteInput
import android.graphics.Bitmap
import android.graphics.Rect
import com.example.system.IslandTab
import java.util.concurrent.ConcurrentHashMap

enum class IslandMode {
    IDLE,
    COMPACT,
    MEDIUM,
    SPLIT,
    EXPANDED
}

enum class AlertKind {
    TIMER_DONE,
    CHARGING_FULL,
    LOW_BATTERY,
    VAULT_EXPIRING,
    ACCESSIBILITY_RECOVERED,
    OCR_COPIED,
    EARBUDS_BATTERY,
    GENERAL
}

enum class DwellPauseReason {
    TOUCH,
    EXPANDED,
    SCREEN_OFF
}

data class NotificationActionItem(
    val index: Int,
    val title: String,
    val pendingIntent: PendingIntent? = null
)

object NotificationMediaCache {
    private val pictureBitmaps = ConcurrentHashMap<String, Bitmap>()
    private val contentIntents = ConcurrentHashMap<String, PendingIntent>()
    private val replyActions = ConcurrentHashMap<String, Pair<RemoteInput, PendingIntent>>()

    fun putPicture(notifId: String, bitmap: Bitmap?) {
        if (bitmap == null) {
            pictureBitmaps.remove(notifId)
        } else {
            if (pictureBitmaps.size > 24) {
                val oldest = pictureBitmaps.keys().nextElement()
                pictureBitmaps.remove(oldest)
            }
            pictureBitmaps[notifId] = bitmap
        }
    }

    fun getPicture(notifId: String): Bitmap? = pictureBitmaps[notifId]

    fun putContentIntent(notifId: String, pi: PendingIntent?) {
        if (pi == null) contentIntents.remove(notifId)
        else {
            if (contentIntents.size > 40) {
                val oldest = contentIntents.keys().nextElement()
                contentIntents.remove(oldest)
            }
            contentIntents[notifId] = pi
        }
    }

    fun getContentIntent(notifId: String): PendingIntent? = contentIntents[notifId]

    fun putReplyAction(notifId: String, pair: Pair<RemoteInput, PendingIntent>?) {
        if (pair == null) replyActions.remove(notifId)
        else {
            if (replyActions.size > 32) {
                val oldest = replyActions.keys().nextElement()
                replyActions.remove(oldest)
            }
            replyActions[notifId] = pair
        }
    }

    fun getReplyAction(notifId: String): Pair<RemoteInput, PendingIntent>? = replyActions[notifId]

    fun remove(notifId: String) {
        pictureBitmaps.remove(notifId)
        contentIntents.remove(notifId)
        replyActions.remove(notifId)
    }

    fun clear() {
        pictureBitmaps.clear()
        contentIntents.clear()
        replyActions.clear()
    }
}

data class ApprovalPromptState(
    val permissionKey: String,
    val title: String,
    val reason: String,
    val actionLabel: String = "Allow"
)

data class PrivacyIndicatorState(
    val cameraActive: Boolean = false,
    val micActive: Boolean = false,
    val locationActive: Boolean = false,
    val activeApps: List<String> = emptyList()
) {
    val anyActive: Boolean get() = cameraActive || micActive || locationActive
}

data class DiagnosticsSnapshot(
    val lastNotificationApp: String = "None yet",
    val lastNotificationTitle: String = "",
    val lastNotificationTimeMs: Long = 0L,
    val lastSuppressionReason: String = "None",
    val deadZoneCollisionCount: Int = 0,
    val hiddenReason: String = "Visible",
    val serviceAlive: Boolean = true
)

data class OcrTextToken(
    val text: String,
    val startChar: Int,
    val endChar: Int,
    val leftPx: Int,
    val topPx: Int,
    val rightPx: Int,
    val bottomPx: Int
)

data class OcrSelectionState(
    val fullText: String,
    val elementBounds: Rect,
    val tokens: List<OcrTextToken>,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
    val anchorX: Float,
    val anchorY: Float,
    val activeDragHandle: Int = 0, // 0 = none, 1 = start, 2 = end
    val isFromTierB: Boolean = false
) {
    val selectedText: String
        get() {
            if (tokens.isEmpty()) return fullText
            val s = startTokenIndex.coerceIn(0, tokens.lastIndex)
            val e = endTokenIndex.coerceIn(s, tokens.lastIndex)
            val cStart = tokens[s].startChar.coerceIn(0, fullText.length)
            val cEnd = tokens[e].endChar.coerceIn(cStart, fullText.length)
            return fullText.substring(cStart, cEnd).trim().ifEmpty { fullText }
        }
}

sealed interface IslandActivity {
    val id: String
    val priority: Int
    val timestamp: Long

    data class Music(
        override val id: String = "music_session",
        override val priority: Int = 60,
        override val timestamp: Long = System.currentTimeMillis(),
        val packageName: String,
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long
    ) : IslandActivity

    data class Timer(
        override val id: String = "timer_primary",
        override val priority: Int = 80,
        override val timestamp: Long = System.currentTimeMillis(),
        val label: String,
        val remainingMs: Long,
        val totalMs: Long,
        val isRunning: Boolean,
        val isMirroredFromClock: Boolean = false
    ) : IslandActivity

    data class Charging(
        override val id: String = "battery_charging",
        override val priority: Int = 70,
        override val timestamp: Long = System.currentTimeMillis(),
        val levelPercent: Int,
        val isCharging: Boolean,
        val isFull: Boolean,
        val wattageWatts: Float? = null,
        val pluggedLabel: String = "Fast AC",
        val chargeEtaMinutes: Int? = null
    ) : IslandActivity

    data class NotificationPreview(
        override val id: String,
        override val priority: Int = 90,
        override val timestamp: Long = System.currentTimeMillis(),
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val hasPicture: Boolean = false,
        val actions: List<NotificationActionItem> = emptyList(),
        val otpCode: String? = null,
        val channelId: String? = null,
        val hasReplyAction: Boolean = false,
        val accentArgb: Int? = null
    ) : IslandActivity

    data class Alert(
        override val id: String,
        override val priority: Int = 100,
        override val timestamp: Long = System.currentTimeMillis(),
        val kind: AlertKind,
        val title: String,
        val subtitle: String
    ) : IslandActivity

    data class VoiceRecording(
        override val id: String = "voice_memo_rec",
        override val priority: Int = 95,
        override val timestamp: Long = System.currentTimeMillis(),
        val elapsedMs: Long
    ) : IslandActivity

    data class DownloadProgress(
        override val id: String,
        override val priority: Int = 85,
        override val timestamp: Long = System.currentTimeMillis(),
        val packageName: String,
        val appName: String,
        val title: String,
        val progress: Int,
        val max: Int,
        val indeterminate: Boolean,
        val speedText: String = "",
        val etaText: String = ""
    ) : IslandActivity

    data class ScreenRecording(
        override val id: String = "screen_rec",
        override val priority: Int = 96,
        override val timestamp: Long = System.currentTimeMillis(),
        val elapsedMs: Long = 0L,
        val isCountdown: Boolean = false,
        val countdownSec: Int = 0
    ) : IslandActivity
}

data class IslandState(
    val mode: IslandMode = IslandMode.IDLE,
    val activeTab: IslandTab = IslandTab.MUSIC,
    val activityQueue: List<IslandActivity> = emptyList(),
    val alertsHistory: List<IslandActivity.Alert> = emptyList(),
    val notificationsHistory: List<IslandActivity.NotificationPreview> = emptyList(),
    val undoableClearedInbox: Pair<List<IslandActivity.NotificationPreview>, List<IslandActivity.Alert>>? = null,
    val isMoreRowOpen: Boolean = false,
    val isSelectModeActive: Boolean = false,
    val ocrSelection: OcrSelectionState? = null,
    val isFlashlightCardOpen: Boolean = false,
    val pullDownProgress: Float = 0f,
    val promotedPrimaryId: String? = null,
    val isDeviceLocked: Boolean = false,
    val isCalibrationPreview: Boolean = false,
    val isDragOverIsland: Boolean = false,
    val needsKeyboardFocus: Boolean = false,
    val statusBanner: String? = null,
    val batteryLevelPercent: Int = 100,
    val isBatteryCharging: Boolean = false,
    val isLowBatteryWarning: Boolean = false,
    val earbudsName: String? = null,
    val earbudsBatteryPercent: Int? = null,
    val currentForegroundPackage: String? = null,
    val approvalPrompt: ApprovalPromptState? = null,
    val diagnostics: DiagnosticsSnapshot = DiagnosticsSnapshot(),
    val privacyIndicator: PrivacyIndicatorState = PrivacyIndicatorState(),
    val isScreenRecording: Boolean = false,
    val isLandscapeQuiet: Boolean = false,
    val pendingAutoRotateSuggestion: Int? = null,
    val dwellTargetActivityId: String? = null,
    val dwellTotalMs: Long = 0L,
    val dwellRemainingMs: Long = 0L,
    val isDwellPaused: Boolean = false
) {
    val primaryActivity: IslandActivity?
        get() {
            val promoted = promotedPrimaryId?.let { id -> activityQueue.firstOrNull { it.id == id } }
            return promoted ?: activityQueue.maxByOrNull { it.priority }
        }

    val secondaryActivity: IslandActivity?
        get() {
            val primary = primaryActivity ?: return null
            return activityQueue
                .filter { it.id != primary.id }
                .maxByOrNull { it.priority }
        }

    val companionCount: Int
        get() = (activityQueue.size - 1).coerceAtLeast(0)

    val isReelAppForeground: Boolean
        get() = currentForegroundPackage in REEL_PACKAGES

    companion object {
        val REEL_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.google.android.youtube"
        )
    }
}
