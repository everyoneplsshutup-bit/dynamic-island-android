package com.example.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.DynamicIslandApp
import com.example.island.AlertKind
import com.example.island.IslandActivity
import com.example.island.IslandController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val selectedHours: Int = 0,
    val selectedMinutes: Int = 5,
    val selectedSeconds: Int = 0,
    val totalMs: Long = 0L,
    val remainingMs: Long = 0L,
    val isRunning: Boolean = false,
    val isActive: Boolean = false,
    val isDone: Boolean = false,
    val mirroredClockLabel: String? = null,
    val mirroredRemainingMs: Long? = null,
    // Stopwatch state in the same panel
    val stopwatchElapsedMs: Long = 0L,
    val isStopwatchRunning: Boolean = false,
    val laps: List<Long> = emptyList()
)

class TimerEngine(
    private val context: Context,
    private val islandController: IslandController
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(TimerUiState())
    val state: StateFlow<TimerUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var timerEndWallMs: Long = 0L

    private var stopwatchJob: Job? = null
    private var stopwatchStartWallMs: Long = 0L
    private var stopwatchAccumulatedMs: Long = 0L

    fun setPickerDuration(hours: Int, minutes: Int, seconds: Int) {
        _state.update {
            it.copy(
                selectedHours = hours.coerceIn(0, 23),
                selectedMinutes = minutes.coerceIn(0, 59),
                selectedSeconds = seconds.coerceIn(0, 59)
            )
        }
    }

    fun startTimer() {
        val s = _state.value
        val durationMs = ((s.selectedHours * 3600L) + (s.selectedMinutes * 60L) + s.selectedSeconds) * 1000L
        if (durationMs <= 0L) return

        timerEndWallMs = System.currentTimeMillis() + durationMs
        _state.update {
            it.copy(
                totalMs = durationMs,
                remainingMs = durationMs,
                isRunning = true,
                isActive = true,
                isDone = false
            )
        }
        scheduleExactAlarm(timerEndWallMs)
        startTicker()
    }

    fun pauseTimer() {
        if (!_state.value.isRunning) return
        timerJob?.cancel()
        cancelExactAlarm()
        val left = (timerEndWallMs - System.currentTimeMillis()).coerceAtLeast(0L)
        _state.update { it.copy(remainingMs = left, isRunning = false) }
        pushTimerToIsland()
    }

    fun resumeTimer() {
        val left = _state.value.remainingMs
        if (left <= 0L || _state.value.isRunning) return
        timerEndWallMs = System.currentTimeMillis() + left
        _state.update { it.copy(isRunning = true, isDone = false) }
        scheduleExactAlarm(timerEndWallMs)
        startTicker()
    }

    fun cancelTimer() {
        timerJob?.cancel()
        cancelExactAlarm()
        _state.update {
            it.copy(
                totalMs = 0L,
                remainingMs = 0L,
                isRunning = false,
                isActive = false,
                isDone = false
            )
        }
        islandController.removeActivity("timer_primary")
    }

    fun onTimerCompletedFromAlarm() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                remainingMs = 0L,
                isRunning = false,
                isActive = false,
                isDone = true
            )
        }
        islandController.removeActivity("timer_primary")
        islandController.upsertActivity(
            IslandActivity.Alert(
                id = "alert_timer_done_${System.currentTimeMillis()}",
                kind = AlertKind.TIMER_DONE,
                title = "Timer Done",
                subtitle = "Countdown finished"
            ),
            autoShowCompact = true,
            autoDismissMs = 12_000L
        )
    }

    fun updateMirroredClockTimer(label: String?, remainingMs: Long?) {
        _state.update {
            it.copy(
                mirroredClockLabel = label,
                mirroredRemainingMs = remainingMs
            )
        }
        if (label != null && remainingMs != null && remainingMs > 0L && !_state.value.isActive) {
            islandController.upsertActivity(
                IslandActivity.Timer(
                    id = "timer_clock_mirror",
                    label = label,
                    remainingMs = remainingMs,
                    totalMs = remainingMs.coerceAtLeast(1000L),
                    isRunning = true,
                    isMirroredFromClock = true
                ),
                autoShowCompact = true
            )
        } else if (label == null) {
            islandController.removeActivity("timer_clock_mirror")
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val left = (timerEndWallMs - System.currentTimeMillis()).coerceAtLeast(0L)
                _state.update { it.copy(remainingMs = left) }
                pushTimerToIsland()
                if (left <= 0L) {
                    onTimerCompletedFromAlarm()
                    break
                }
                delay(100L)
            }
        }
    }

    private fun pushTimerToIsland() {
        val s = _state.value
        if (!s.isActive) return
        islandController.upsertActivity(
            IslandActivity.Timer(
                id = "timer_primary",
                label = "Timer",
                remainingMs = s.remainingMs,
                totalMs = s.totalMs.coerceAtLeast(1L),
                isRunning = s.isRunning,
                isMirroredFromClock = false
            ),
            autoShowCompact = true
        )
    }

    // Stopwatch
    fun startStopwatch() {
        if (_state.value.isStopwatchRunning) return
        stopwatchStartWallMs = System.currentTimeMillis()
        _state.update { it.copy(isStopwatchRunning = true) }
        stopwatchJob?.cancel()
        stopwatchJob = scope.launch {
            while (isActive) {
                val total = stopwatchAccumulatedMs + (System.currentTimeMillis() - stopwatchStartWallMs)
                _state.update { it.copy(stopwatchElapsedMs = total) }
                delay(50L)
            }
        }
    }

    fun pauseStopwatch() {
        if (!_state.value.isStopwatchRunning) return
        stopwatchJob?.cancel()
        stopwatchAccumulatedMs += System.currentTimeMillis() - stopwatchStartWallMs
        _state.update {
            it.copy(
                stopwatchElapsedMs = stopwatchAccumulatedMs,
                isStopwatchRunning = false
            )
        }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        stopwatchAccumulatedMs = 0L
        _state.update {
            it.copy(
                stopwatchElapsedMs = 0L,
                isStopwatchRunning = false,
                laps = emptyList()
            )
        }
    }

    fun lapStopwatch() {
        val current = _state.value.stopwatchElapsedMs
        _state.update { it.copy(laps = listOf(current) + it.laps) }
    }

    private fun scheduleExactAlarm(triggerAtMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = alarmPendingIntent()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    private fun cancelExactAlarm() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            4012,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val totalSec = ((ms + 500L) / 1000L).coerceAtLeast(0L)
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
            else String.format("%02d:%02d", m, s)
        }

        fun formatStopwatch(ms: Long): String {
            val centis = (ms / 10L) % 100L
            val totalSec = ms / 1000L
            val m = totalSec / 60L
            val s = totalSec % 60L
            return String.format("%02d:%02d.%02d", m, s, centis)
        }
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? DynamicIslandApp ?: return
        app.timerEngine.onTimerCompletedFromAlarm()
    }
}
