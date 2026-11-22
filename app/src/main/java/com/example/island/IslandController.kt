package com.example.island

import android.os.SystemClock
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
    private var undoClearJob: Job? = null

    private var activeDwellActivityId: String? = null
    private var activeDwellTotalMs: Long = 0L
    private var activeDwellRemainingMs: Long = 0L
    private var activeDwellResumedAtElapsedMs: Long = 0L
    private val dwellPauseReasons = mutableSetOf<DwellPauseReason>()

    fun upsertActivity(
        activity: IslandActivity,
        autoShowCompact: Boolean = true,
        autoDismissMs: Long? = null,
        autoExpandToFull: Boolean = false
    ) {
        var expandedNow = false
        var actuallyChanged = false
        _state.update { current ->
            val existingIdx = current.activityQueue.indexOfFirst { it.id == activity.id }
            val existing = if (existingIdx >= 0) current.activityQueue[existingIdx] else null

            // NO-FLICKER RULE (ajitpal 2.4.1):
            // 1. If visible content is identical, ignore redundant re-posts (zero redraw).
            if (existing != null && isVisuallySame(existing, activity)) {
                return@update current
            }
            actuallyChanged = true

            // Replace-not-stack transient NotificationPreview pill while keeping replaced items in Inbox
            val baseQueue = if (activity is IslandActivity.NotificationPreview && existing == null) {
                current.activityQueue.filterNot { it is IslandActivity.NotificationPreview && it.id != activity.id }
            } else {
                current.activityQueue
            }

            // 2. Updating an existing ongoing activity keeps its relative place rather than stealing front;
            //    only a genuinely new activity is sorted to front by priority.
            val updatedQueue = if (existingIdx >= 0 && baseQueue.any { it.id == activity.id }) {
                baseQueue.map { if (it.id == activity.id) activity else it }
            } else {
                (baseQueue + activity).sortedByDescending { it.priority }
            }

            val targetTab = when (activity) {
                is IslandActivity.Timer -> IslandTab.TIMER
                is IslandActivity.Music -> IslandTab.MUSIC
                is IslandActivity.VoiceRecording -> IslandTab.MEMO
                is IslandActivity.Alert, is IslandActivity.NotificationPreview -> IslandTab.ALERTS
                else -> current.activeTab
            }

            val isExistingUpdate = existing != null
            val newMode = when {
                autoExpandToFull && !isExistingUpdate -> {
                    expandedNow = current.mode != IslandMode.EXPANDED
                    IslandMode.EXPANDED
                }
                current.mode == IslandMode.EXPANDED -> IslandMode.EXPANDED
                current.mode == IslandMode.MEDIUM -> IslandMode.MEDIUM
                !autoShowCompact || isExistingUpdate -> current.mode
                updatedQueue.size >= 2 -> IslandMode.SPLIT
                updatedQueue.isNotEmpty() -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }

            val updatedAlerts = if (activity is IslandActivity.Alert) {
                (listOf(activity) + current.alertsHistory.filterNot { it.id == activity.id }).take(25)
            } else {
                current.alertsHistory
            }

            val updatedNotifs = if (activity is IslandActivity.NotificationPreview) {
                (listOf(activity) + current.notificationsHistory.filterNot { it.id == activity.id }).take(40)
            } else {
                current.notificationsHistory
            }

            if (newMode == IslandMode.MEDIUM || newMode == IslandMode.EXPANDED) {
                dwellPauseReasons.add(DwellPauseReason.EXPANDED)
            } else {
                dwellPauseReasons.remove(DwellPauseReason.EXPANDED)
            }

            current.copy(
                mode = newMode,
                activeTab = if (autoExpandToFull && !isExistingUpdate) targetTab else current.activeTab,
                activityQueue = updatedQueue,
                alertsHistory = updatedAlerts,
                notificationsHistory = updatedNotifs
            )
        }

        if (!actuallyChanged) return
        if (expandedNow) {
            onSoundEvent(IslandSoundEvent.EXPAND)
        }

        if (autoDismissMs != null && autoDismissMs > 0L) {
            startDwellTimer(activity.id, autoDismissMs)
        }
    }

    private fun startDwellTimer(activityId: String, durationMs: Long) {
        transientCollapseJob?.cancel()
        activeDwellActivityId = activityId
        activeDwellTotalMs = durationMs
        activeDwellRemainingMs = durationMs
        activeDwellResumedAtElapsedMs = SystemClock.elapsedRealtime()

        val paused = dwellPauseReasons.isNotEmpty()
        _state.update {
            it.copy(
                dwellTargetActivityId = activityId,
                dwellTotalMs = durationMs,
                dwellRemainingMs = durationMs,
                isDwellPaused = paused
            )
        }

        if (!paused) {
            scheduleDwellCountdown(activityId, durationMs)
        }
    }

    private fun scheduleDwellCountdown(activityId: String, delayMs: Long) {
        transientCollapseJob?.cancel()
        transientCollapseJob = scope.launch {
            delay(delayMs.coerceAtLeast(80L))
            if (activeDwellActivityId == activityId && dwellPauseReasons.isEmpty()) {
                activeDwellActivityId = null
                activeDwellRemainingMs = 0L
                removeActivity(activityId)
            }
        }
    }

    fun setDwellPaused(reason: DwellPauseReason, paused: Boolean) {
        val wasPaused = dwellPauseReasons.isNotEmpty()
        if (paused) {
            dwellPauseReasons.add(reason)
        } else {
            dwellPauseReasons.remove(reason)
        }
        val nowPaused = dwellPauseReasons.isNotEmpty()
        if (wasPaused == nowPaused) return

        val targetId = activeDwellActivityId ?: return
        if (nowPaused) {
            val elapsed = (SystemClock.elapsedRealtime() - activeDwellResumedAtElapsedMs).coerceAtLeast(0L)
            activeDwellRemainingMs = (activeDwellRemainingMs - elapsed).coerceAtLeast(300L)
            transientCollapseJob?.cancel()
            _state.update {
                it.copy(
                    dwellRemainingMs = activeDwellRemainingMs,
                    isDwellPaused = true
                )
            }
        } else {
            activeDwellResumedAtElapsedMs = SystemClock.elapsedRealtime()
            _state.update {
                it.copy(
                    dwellRemainingMs = activeDwellRemainingMs,
                    isDwellPaused = false
                )
            }
            scheduleDwellCountdown(targetId, activeDwellRemainingMs)
        }
    }

    fun removeActivity(id: String) {
        if (activeDwellActivityId == id) {
            transientCollapseJob?.cancel()
            activeDwellActivityId = null
            activeDwellRemainingMs = 0L
            activeDwellTotalMs = 0L
        }
        _state.update { current ->
            val remaining = current.activityQueue.filterNot { it.id == id }
            val nextPromoted = if (current.promotedPrimaryId == id) null else current.promotedPrimaryId
            val nextMode = when {
                current.mode == IslandMode.EXPANDED -> IslandMode.EXPANDED
                current.mode == IslandMode.MEDIUM && remaining.isNotEmpty() -> IslandMode.MEDIUM
                remaining.size >= 2 -> IslandMode.SPLIT
                remaining.size == 1 -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }
            current.copy(
                mode = nextMode,
                activityQueue = remaining,
                promotedPrimaryId = nextPromoted,
                dwellTargetActivityId = if (current.dwellTargetActivityId == id) null else current.dwellTargetActivityId
            )
        }
    }

    fun openFromPill(twoStageOpen: Boolean, tab: IslandTab? = null) {
        val currentMode = _state.value.mode
        if (twoStageOpen && (currentMode == IslandMode.IDLE || currentMode == IslandMode.COMPACT || currentMode == IslandMode.SPLIT)) {
            expandToMedium(tab)
        } else {
            expand(tab)
        }
    }

    fun expandToMedium(tab: IslandTab? = null) {
        val wasMediumOrExpanded = _state.value.mode == IslandMode.MEDIUM || _state.value.mode == IslandMode.EXPANDED
        setDwellPaused(DwellPauseReason.EXPANDED, true)
        _state.update { current ->
            val resolvedTab = tab ?: defaultTabForPrimary(current.primaryActivity, current.activeTab)
            current.copy(
                mode = IslandMode.MEDIUM,
                activeTab = resolvedTab,
                pullDownProgress = 0f
            )
        }
        if (!wasMediumOrExpanded) {
            onSoundEvent(IslandSoundEvent.EXPAND)
        }
    }

    fun expand(tab: IslandTab? = null) {
        val wasExpanded = _state.value.mode == IslandMode.EXPANDED
        setDwellPaused(DwellPauseReason.EXPANDED, true)
        _state.update { current ->
            val resolvedTab = tab ?: defaultTabForPrimary(current.primaryActivity, current.activeTab)
            current.copy(
                mode = IslandMode.EXPANDED,
                activeTab = resolvedTab,
                isMoreRowOpen = !resolvedTab.isMainRow,
                pullDownProgress = 0f,
                needsKeyboardFocus = (resolvedTab == IslandTab.SEARCH || resolvedTab == IslandTab.NOTES)
            )
        }
        if (!wasExpanded) {
            onSoundEvent(IslandSoundEvent.EXPAND)
        }
    }

    /**
     * Staged collapse for interactive swipe-down / Back (Full -> Medium -> Compact/Idle).
     */
    fun stepDownCollapse(twoStageOpen: Boolean) {
        val current = _state.value
        if (twoStageOpen && current.mode == IslandMode.EXPANDED) {
            expandToMedium(current.activeTab)
            onSoundEvent(IslandSoundEvent.COLLAPSE)
        } else {
            collapse()
        }
    }

    fun collapse() {
        val wasExpandedOrMedium = _state.value.mode == IslandMode.EXPANDED || _state.value.mode == IslandMode.MEDIUM
        setDwellPaused(DwellPauseReason.EXPANDED, false)
        _state.update { current ->
            val nextMode = when {
                current.activityQueue.size >= 2 -> IslandMode.SPLIT
                current.activityQueue.size == 1 -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }
            current.copy(
                mode = nextMode,
                isMoreRowOpen = false,
                isFlashlightCardOpen = false,
                pullDownProgress = 0f,
                needsKeyboardFocus = false
            )
        }
        if (wasExpandedOrMedium) {
            onSoundEvent(IslandSoundEvent.COLLAPSE)
        }
    }

    fun collapseToIdle() {
        setDwellPaused(DwellPauseReason.EXPANDED, false)
        _state.update { current ->
            current.copy(
                mode = IslandMode.IDLE,
                isMoreRowOpen = false,
                isFlashlightCardOpen = false,
                pullDownProgress = 0f,
                needsKeyboardFocus = false
            )
        }
    }

    fun swapPrimaryAndSecondary() {
        _state.update { current ->
            val secondary = current.secondaryActivity ?: return@update current
            current.copy(promotedPrimaryId = secondary.id)
        }
        onSoundEvent(IslandSoundEvent.TAP)
    }

    fun nextNotification() {
        _state.update { current ->
            val notifs = current.notificationsHistory
            if (notifs.size <= 1) {
                val activeNotifs = current.activityQueue.filterIsInstance<IslandActivity.NotificationPreview>()
                if (activeNotifs.size <= 1) return@update current
                val curId = current.primaryActivity?.id
                val curIdx = activeNotifs.indexOfFirst { it.id == curId }
                val next = activeNotifs[(curIdx + 1) % activeNotifs.size]
                return@update current.copy(promotedPrimaryId = next.id)
            }
            val curId = current.primaryActivity?.id
            val curIdx = notifs.indexOfFirst { it.id == curId }
            val next = notifs[(curIdx + 1) % notifs.size]
            val updatedQueue = (current.activityQueue.filterNot { it is IslandActivity.NotificationPreview } + next)
                .sortedByDescending { it.priority }
            current.copy(activityQueue = updatedQueue, promotedPrimaryId = next.id)
        }
        onSoundEvent(IslandSoundEvent.TAP)
    }

    fun dismissActiveNotification() {
        val current = _state.value
        val target = (current.primaryActivity as? IslandActivity.NotificationPreview)
            ?: current.activityQueue.filterIsInstance<IslandActivity.NotificationPreview>().firstOrNull()
        if (target != null) {
            dismissNotificationById(target.id)
        } else if (current.alertsHistory.isNotEmpty()) {
            _state.update { it.copy(alertsHistory = it.alertsHistory.drop(1)) }
            onSoundEvent(IslandSoundEvent.COLLAPSE)
        }
    }

    fun dismissNotificationById(id: String) {
        _state.update { current ->
            val remainingNotifs = current.notificationsHistory.filterNot { it.id == id }
            val remainingQueue = current.activityQueue.filterNot { it.id == id }
            val nextMode = when {
                current.mode == IslandMode.EXPANDED -> IslandMode.EXPANDED
                current.mode == IslandMode.MEDIUM && remainingQueue.isNotEmpty() -> IslandMode.MEDIUM
                remainingQueue.size >= 2 -> IslandMode.SPLIT
                remainingQueue.size == 1 -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }
            current.copy(
                mode = nextMode,
                notificationsHistory = remainingNotifs,
                activityQueue = remainingQueue
            )
        }
        onSoundEvent(IslandSoundEvent.COLLAPSE)
    }

    fun dismissAppGroup(packageName: String) {
        _state.update { current ->
            val remainingNotifs = current.notificationsHistory.filterNot { it.packageName == packageName }
            val remainingQueue = current.activityQueue.filterNot {
                it is IslandActivity.NotificationPreview && it.packageName == packageName
            }
            current.copy(
                notificationsHistory = remainingNotifs,
                activityQueue = remainingQueue
            )
        }
        onSoundEvent(IslandSoundEvent.COLLAPSE)
    }

    fun clearInboxWithUndo() {
        _state.update { current ->
            val snapshot = current.notificationsHistory to current.alertsHistory
            val nonAlerts = current.activityQueue.filterNot {
                it is IslandActivity.Alert || it is IslandActivity.NotificationPreview
            }
            current.copy(
                notificationsHistory = emptyList(),
                alertsHistory = emptyList(),
                activityQueue = nonAlerts,
                undoableClearedInbox = if (snapshot.first.isEmpty() && snapshot.second.isEmpty()) null else snapshot
            )
        }
        undoClearJob?.cancel()
        undoClearJob = scope.launch {
            delay(5000L)
            _state.update { it.copy(undoableClearedInbox = null) }
        }
        onSoundEvent(IslandSoundEvent.COLLAPSE)
    }

    fun undoInboxClear() {
        undoClearJob?.cancel()
        _state.update { current ->
            val snap = current.undoableClearedInbox ?: return@update current
            current.copy(
                notificationsHistory = snap.first,
                alertsHistory = snap.second,
                undoableClearedInbox = null
            )
        }
        onSoundEvent(IslandSoundEvent.TAP)
    }

    fun pruneStaleNotifications(activeNotificationIds: Set<String>) {
        _state.update { current ->
            val remaining = current.activityQueue.filter { item ->
                item !is IslandActivity.NotificationPreview || item.id in activeNotificationIds
            }
            if (remaining.size == current.activityQueue.size) return@update current
            val nextPromoted = if (current.promotedPrimaryId != null && remaining.none { it.id == current.promotedPrimaryId }) {
                null
            } else {
                current.promotedPrimaryId
            }
            val nextMode = when {
                current.mode == IslandMode.EXPANDED -> IslandMode.EXPANDED
                current.mode == IslandMode.MEDIUM && remaining.isNotEmpty() -> IslandMode.MEDIUM
                remaining.size >= 2 -> IslandMode.SPLIT
                remaining.size == 1 -> IslandMode.COMPACT
                else -> IslandMode.IDLE
            }
            current.copy(
                mode = nextMode,
                activityQueue = remaining,
                promotedPrimaryId = nextPromoted
            )
        }
    }

    fun updateBatteryTelemetry(levelPercent: Int, isCharging: Boolean, isLowBatteryWarning: Boolean) {
        _state.update { current ->
            if (current.batteryLevelPercent == levelPercent &&
                current.isBatteryCharging == isCharging &&
                current.isLowBatteryWarning == isLowBatteryWarning
            ) {
                current
            } else {
                current.copy(
                    batteryLevelPercent = levelPercent,
                    isBatteryCharging = isCharging,
                    isLowBatteryWarning = isLowBatteryWarning
                )
            }
        }
    }

    fun setPullDownProgress(progress: Float) {
        _state.update { current ->
            val clamped = progress.coerceIn(0f, 1f)
            if (current.pullDownProgress == clamped) current else current.copy(pullDownProgress = clamped)
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
        if (tab == IslandTab.SEARCH) {
            onSoundEvent(IslandSoundEvent.SEARCH_OPEN)
        } else {
            onSoundEvent(IslandSoundEvent.TAP)
        }
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
                ocrSelection = if (enabled) null else it.ocrSelection,
                mode = if (enabled) IslandMode.COMPACT else it.mode
            )
        }
    }

    fun setOcrSelection(selection: OcrSelectionState?) {
        _state.update {
            it.copy(
                isSelectModeActive = false,
                ocrSelection = selection
            )
        }
    }

    fun updateOcrRange(startIndex: Int, endIndex: Int, activeHandle: Int, anchorX: Float, anchorY: Float) {
        _state.update { current ->
            val sel = current.ocrSelection ?: return@update current
            if (sel.tokens.isEmpty()) return@update current
            val maxIdx = sel.tokens.lastIndex
            val s = startIndex.coerceIn(0, maxIdx)
            val e = endIndex.coerceIn(0, maxIdx)
            val orderedStart = minOf(s, e)
            val orderedEnd = maxOf(s, e)
            current.copy(
                ocrSelection = sel.copy(
                    startTokenIndex = orderedStart,
                    endTokenIndex = orderedEnd,
                    activeDragHandle = activeHandle,
                    anchorX = anchorX,
                    anchorY = anchorY
                )
            )
        }
    }

    fun finishOcrHandleDrag() {
        _state.update { current ->
            val sel = current.ocrSelection ?: return@update current
            current.copy(ocrSelection = sel.copy(activeDragHandle = 0))
        }
    }

    fun selectAllOcrTokens() {
        _state.update { current ->
            val sel = current.ocrSelection ?: return@update current
            if (sel.tokens.isEmpty()) return@update current
            current.copy(
                ocrSelection = sel.copy(
                    startTokenIndex = 0,
                    endTokenIndex = sel.tokens.lastIndex,
                    activeDragHandle = 0
                )
            )
        }
        onSoundEvent(IslandSoundEvent.TAP)
    }

    fun dismissOcrSelection() {
        _state.update { it.copy(ocrSelection = null, isSelectModeActive = false) }
    }

    fun setFlashlightCardOpen(open: Boolean) {
        _state.update { it.copy(isFlashlightCardOpen = open) }
    }

    fun setFloatingSidebarOpen(open: Boolean) {
        _state.update { it.copy(isFloatingSidebarOpen = open) }
    }

    fun setFloatingMiniTab(tab: IslandTab?) {
        if (tab == IslandTab.TIMER) {
            _state.update {
                it.copy(
                    activeFloatingMiniTab = null,
                    isFloatingSidebarOpen = false
                )
            }
            expand(IslandTab.TIMER)
            return
        }
        _state.update {
            it.copy(
                activeFloatingMiniTab = tab,
                isFloatingSidebarOpen = false,
                needsKeyboardFocus = (tab == IslandTab.SEARCH || tab == IslandTab.NOTES)
            )
        }
    }

    fun setDragOverIsland(active: Boolean) {
        _state.update { it.copy(isDragOverIsland = active) }
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
        clearInboxWithUndo()
    }

    private fun defaultTabForPrimary(primary: IslandActivity?, fallback: IslandTab): IslandTab = when (primary) {
        is IslandActivity.Timer -> IslandTab.TIMER
        is IslandActivity.Music -> IslandTab.MUSIC
        is IslandActivity.VoiceRecording -> IslandTab.MEMO
        is IslandActivity.Alert, is IslandActivity.NotificationPreview -> IslandTab.ALERTS
        else -> fallback
    }

    private fun isVisuallySame(a: IslandActivity, b: IslandActivity): Boolean = when {
        a is IslandActivity.Music && b is IslandActivity.Music ->
            a.packageName == b.packageName && a.title == b.title && a.artist == b.artist && a.isPlaying == b.isPlaying && a.durationMs == b.durationMs
        a is IslandActivity.Timer && b is IslandActivity.Timer ->
            a.label == b.label && (a.remainingMs / 1000L) == (b.remainingMs / 1000L) && a.isRunning == b.isRunning
        a is IslandActivity.Charging && b is IslandActivity.Charging ->
            a.levelPercent == b.levelPercent && a.isCharging == b.isCharging && a.isFull == b.isFull && a.pluggedLabel == b.pluggedLabel
        a is IslandActivity.NotificationPreview && b is IslandActivity.NotificationPreview ->
            a.packageName == b.packageName && a.title == b.title && a.text == b.text
        a is IslandActivity.VoiceRecording && b is IslandActivity.VoiceRecording ->
            (a.elapsedMs / 1000L) == (b.elapsedMs / 1000L)
        a is IslandActivity.Alert && b is IslandActivity.Alert ->
            a.kind == b.kind && a.title == b.title && a.subtitle == b.subtitle
        else -> false
    }
}
