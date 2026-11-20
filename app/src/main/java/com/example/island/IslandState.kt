package com.example.island

import com.example.system.IslandTab

enum class IslandMode {
    IDLE,
    COMPACT,
    SPLIT,
    EXPANDED
}

enum class AlertKind {
    TIMER_DONE,
    CHARGING_FULL,
    VAULT_EXPIRING,
    ACCESSIBILITY_RECOVERED,
    OCR_COPIED
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
        val isFull: Boolean
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
    val isFloatingSidebarOpen: Boolean = false,
    val isDeviceLocked: Boolean = false,
    val isCalibrationPreview: Boolean = false,
    val needsKeyboardFocus: Boolean = false,
    val statusBanner: String? = null
) {
    val primaryActivity: IslandActivity?
        get() = activityQueue.maxByOrNull { it.priority }

    val secondaryActivity: IslandActivity?
        get() {
            val primary = primaryActivity ?: return null
            return activityQueue
                .filter { it.id != primary.id }
                .maxByOrNull { it.priority }
        }
}
