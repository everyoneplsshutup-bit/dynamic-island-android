package com.example.island

import android.graphics.Rect
import com.example.system.IslandTab

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
    OCR_COPIED
}

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
        val pluggedLabel: String = "Fast AC"
    ) : IslandActivity

    data class NotificationPreview(
        override val id: String,
        override val priority: Int = 90,
        override val timestamp: Long = System.currentTimeMillis(),
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String
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
}

data class IslandState(
    val mode: IslandMode = IslandMode.IDLE,
    val activeTab: IslandTab = IslandTab.MUSIC,
    val activityQueue: List<IslandActivity> = emptyList(),
    val alertsHistory: List<IslandActivity.Alert> = emptyList(),
    val isMoreRowOpen: Boolean = false,
    val isSelectModeActive: Boolean = false,
    val ocrSelection: OcrSelectionState? = null,
    val isFlashlightCardOpen: Boolean = false,
    val pullDownProgress: Float = 0f,
    val promotedPrimaryId: String? = null,
    val isFloatingSidebarOpen: Boolean = false,
    val isDeviceLocked: Boolean = false,
    val isCalibrationPreview: Boolean = false,
    val isDragOverIsland: Boolean = false,
    val activeFloatingMiniTab: IslandTab? = null,
    val needsKeyboardFocus: Boolean = false,
    val statusBanner: String? = null,
    val batteryLevelPercent: Int = 100,
    val isBatteryCharging: Boolean = false,
    val isLowBatteryWarning: Boolean = false
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
}
