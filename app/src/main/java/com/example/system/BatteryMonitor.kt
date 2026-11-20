package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.island.AlertKind
import com.example.island.IslandActivity
import com.example.island.IslandController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryUiState(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val pluggedLabel: String = "Battery"
)

class BatteryMonitor(
    private val context: Context,
    private val islandController: IslandController
) {
    private val _state = MutableStateFlow(BatteryUiState())
    val state: StateFlow<BatteryUiState> = _state.asStateFlow()

    private var registered = false
    private var lastChargingState: Boolean? = null
    private var lastFullNotified = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                Intent.ACTION_POWER_CONNECTED -> {
                    val cur = _state.value
                    islandController.upsertActivity(
                        IslandActivity.Charging(
                            levelPercent = cur.levelPercent,
                            isCharging = true,
                            isFull = cur.isFull
                        ),
                        autoShowCompact = true,
                        autoDismissMs = 4500L
                    )
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    lastFullNotified = false
                    islandController.removeActivity("battery_charging")
                }
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        val sticky = context.registerReceiver(receiver, filter)
        registered = true
        sticky?.let { handleBatteryChanged(it) }
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else _state.value.levelPercent

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || (isCharging && pct >= 100)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val pluggedLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        _state.value = BatteryUiState(
            levelPercent = pct,
            isCharging = isCharging,
            isFull = isFull,
            pluggedLabel = pluggedLabel
        )

        val wasCharging = lastChargingState
        lastChargingState = isCharging

        if (isCharging && wasCharging == false) {
            islandController.upsertActivity(
                IslandActivity.Charging(
                    levelPercent = pct,
                    isCharging = true,
                    isFull = isFull
                ),
                autoShowCompact = true,
                autoDismissMs = 4500L
            )
        } else if (isCharging) {
            // Update existing pill silently if already visible, or keep updated state
            if (islandController.state.value.activityQueue.any { it.id == "battery_charging" }) {
                islandController.upsertActivity(
                    IslandActivity.Charging(
                        levelPercent = pct,
                        isCharging = true,
                        isFull = isFull
                    ),
                    autoShowCompact = false
                )
            }
        }

        if (isFull && !lastFullNotified) {
            lastFullNotified = true
            islandController.upsertActivity(
                IslandActivity.Alert(
                    id = "alert_charging_full",
                    kind = AlertKind.CHARGING_FULL,
                    title = "Battery Full (100%)",
                    subtitle = "Unplug charger to preserve battery health"
                ),
                autoShowCompact = true,
                autoDismissMs = 8000L
            )
        } else if (!isCharging) {
            lastFullNotified = false
        }
    }
}
