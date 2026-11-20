package com.example.island

import com.example.system.IslandSoundEvent
import com.example.system.IslandTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Single source of truth for the Dynamic Island StateFlow<IslandState>.
 * Notification listener, media hub, battery monitor, and timer engine only write events into it.
 */
class IslandController(
    private val onSoundEvent: (IslandSoundEvent) -> Unit = {}
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(IslandState())
    val state: StateFlow<IslandState> = _state.asStateFlow()

    private var transientCollapseJob: Job? = null
    private var bannerClearJob: Job? = null

    fun upsertActivity(activity: IslandActivity, autoShowCompact: Boolean = true, autoDismissMs: Long? = null) {
        _state.update { current ->
            val updatedQueue = current.activityQueue
                .filterNot { it.id == activity.id } + activity
            val sorted = updatedQueue.sortedByDescending { it.priority }

            val newMode = when {
                current.mode == IslandMode.EXPANDED -> IslandMode.EXPANDED
                !autoShowCompact -> current.mode
                sorted.size >= 2 -> IslandMode.SPLIT
                sorted.isNotEmpty() -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }

            val updatedAlerts = if (activity is IslandActivity.Alert) {
                (listOf(activity) + current.alertsHistory.filterNot { it.id == activity.id }).take(25)
            } else {
                current.alertsHistory
            }

            current.copy(
                mode = newMode,
                activityQueue = sorted,
                alertsHistory = updatedAlerts
            )
        }

        if (autoDismissMs != null) {
            transientCollapseJob?.cancel()
            transientCollapseJob = scope.launch {
                delay(autoDismissMs)
                removeActivity(activity.id)
            }
        }
    }

    fun removeActivity(id: String) {
        _state.update { current ->
            val remaining = current.activityQueue.filterNot { it.id == id }
            val nextMode = when {
                current.mode == IslandMode.EXPANDED -> IslandMode.EXPANDED
                remaining.size >= 2 -> IslandMode.SPLIT
                remaining.size == 1 -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }
            current.copy(
                mode = nextMode,
                activityQueue = remaining
            )
        }
    }

    fun expand(tab: IslandTab? = null) {
        val wasExpanded = _state.value.mode == IslandMode.EXPANDED
        _state.update { current ->
            val resolvedTab = tab ?: when (current.primaryActivity) {
                is IslandActivity.Timer -> IslandTab.TIMER
                is IslandActivity.Music -> IslandTab.MUSIC
                is IslandActivity.VoiceRecording -> IslandTab.MEMO
                is IslandActivity.Alert -> IslandTab.ALERTS
                else -> current.activeTab
            }
            current.copy(
                mode = IslandMode.EXPANDED,
                activeTab = resolvedTab,
                isMoreRowOpen = !resolvedTab.isMainRow
            )
        }
        if (!wasExpanded) {
            onSoundEvent(IslandSoundEvent.EXPAND)
        }
    }

    fun collapse() {
        val wasExpanded = _state.value.mode == IslandMode.EXPANDED
        _state.update { current ->
            val nextMode = when {
                current.activityQueue.size >= 2 -> IslandMode.SPLIT
                current.activityQueue.size == 1 -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }
            current.copy(
                mode = nextMode,
                isMoreRowOpen = false,
                needsKeyboardFocus = false
            )
        }
        if (wasExpanded) {
            onSoundEvent(IslandSoundEvent.COLLAPSE)
        }
    }

    fun collapseToIdle() {
        _state.update { current ->
            current.copy(
                mode = IslandMode.IDLE,
                isMoreRowOpen = false,
                needsKeyboardFocus = false
            )
        }
    }

    fun selectTab(tab: IslandTab) {
        _state.update { current ->
            current.copy(
                mode = IslandMode.EXPANDED,
                activeTab = tab,
                needsKeyboardFocus = (tab == IslandTab.SEARCH || tab == IslandTab.NOTES)
            )
        }
        onSoundEvent(IslandSoundEvent.TAP)
    }

    fun toggleMoreRow() {
        _state.update { it.copy(isMoreRowOpen = !it.isMoreRowOpen) }
        onSoundEvent(IslandSoundEvent.TAP)
    }

    fun setKeyboardFocusNeeded(needed: Boolean) {
        _state.update { it.copy(needsKeyboardFocus = needed) }
    }

    fun setSelectMode(enabled: Boolean) {
        _state.update {
            it.copy(
                isSelectModeActive = enabled,
                mode = if (enabled) IslandMode.COMPACT else it.mode
            )
        }
    }

    fun setFloatingSidebarOpen(open: Boolean) {
        _state.update { it.copy(isFloatingSidebarOpen = open) }
    }

    fun setCalibrationPreview(active: Boolean) {
        _state.update { it.copy(isCalibrationPreview = active) }
    }

    fun setDeviceLocked(locked: Boolean) {
        _state.update { it.copy(isDeviceLocked = locked) }
    }

    fun showTransientBanner(message: String, durationMs: Long = 2400L) {
        _state.update { it.copy(statusBanner = message) }
        bannerClearJob?.cancel()
        bannerClearJob = scope.launch {
            delay(durationMs)
            _state.update { it.copy(statusBanner = null) }
        }
    }

    fun clearAlerts() {
        _state.update { current ->
            val nonAlerts = current.activityQueue.filterNot { it is IslandActivity.Alert }
            current.copy(
                alertsHistory = emptyList(),
                activityQueue = nonAlerts
            )
        }
    }
}
