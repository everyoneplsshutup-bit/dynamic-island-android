package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.island.AlertKind
import com.example.island.IslandActivity
import com.example.island.IslandController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

data class BatteryUiState(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val pluggedLabel: String = "Battery",
    val wattageWatts: Float? = null
)

class BatteryMonitor(
    private val context: Context,
    private val islandController: IslandController,
    private val settingsRepository: SettingsRepository? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val _state = MutableStateFlow(BatteryUiState())
    val state: StateFlow<BatteryUiState> = _state.asStateFlow()

    @Volatile
    private var latestSettings: IslandSettings = IslandSettings()

    private var registered = false
    private var lastChargingState: Boolean? = null
    private var lastFullNotified = false
    private var shownLow20 = false
    private var shownLow15 = false
    private var shownLow10 = false

    init {
        settingsRepository?.let { repo ->
            scope.launch {
                repo.settingsFlow.collectLatest { latestSettings = it }
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                Intent.ACTION_POWER_CONNECTED -> {
                    val cur = _state.value
                    shownLow20 = false
                    shownLow15 = false
                    shownLow10 = false
                    islandController.updateBatteryTelemetry(cur.levelPercent, isCharging = true, isLowBatteryWarning = false)
                    emitChargingCapsule(
                        pct = cur.levelPercent,
                        isFull = cur.isFull,
                        wattage = cur.wattageWatts,
                        pluggedLabel = cur.pluggedLabel,
                        isFreshConnect = true
                    )
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    lastFullNotified = false
                    islandController.removeActivity("battery_charging")
                    val cur = _state.value
                    islandController.updateBatteryTelemetry(cur.levelPercent, isCharging = false, isLowBatteryWarning = cur.levelPercent <= 15)
                }
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED",
                android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val lvl = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                    @Suppress("DEPRECATION")
                    val device = intent.getParcelableExtra<android.bluetooth.BluetoothDevice>(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                    val devName = runCatching { device?.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Earbuds"
                    if (lvl in 0..100) {
                        islandController.updateEarbudsBattery(devName, lvl)
                        if (latestSettings.batteryEnabled) {
                            islandController.upsertActivity(
                                IslandActivity.Alert(
                                    id = "alert_earbuds_${devName.hashCode()}",
                                    kind = AlertKind.EARBUDS_BATTERY,
                                    title = "$devName · $lvl%",
                                    subtitle = "Bluetooth audio connected"
                                ),
                                autoShowCompact = true,
                                autoDismissMs = 2000L
                            )
                        }
                    }
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
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
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

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val currentUa = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Int.MIN_VALUE
        val wattage = if (isCharging && voltageMv > 0 && currentUa != Int.MIN_VALUE && abs(currentUa) > 10_000) {
            val amps = abs(currentUa) / 1_000_000f
            val volts = voltageMv / 1000f
            (volts * amps).takeIf { it in 0.5f..150f }
        } else {
            null
        }

        val lowWarningActive = !isCharging && pct <= 15
        _state.value = BatteryUiState(
            levelPercent = pct,
            isCharging = isCharging,
            isFull = isFull,
            pluggedLabel = pluggedLabel,
            wattageWatts = wattage
        )
        islandController.updateBatteryTelemetry(
            levelPercent = pct,
            isCharging = isCharging,
            isLowBatteryWarning = lowWarningActive
        )

        val wasCharging = lastChargingState
        lastChargingState = isCharging

        if (isCharging) {
            shownLow20 = false
            shownLow15 = false
            shownLow10 = false
            if (wasCharging == false) {
                emitChargingCapsule(pct, isFull, wattage, pluggedLabel, isFreshConnect = true)
            } else if (islandController.state.value.activityQueue.any { it.id == "battery_charging" }) {
                emitChargingCapsule(pct, isFull, wattage, pluggedLabel, isFreshConnect = false)
            }
        } else {
            if (pct > 22) {
                shownLow20 = false
                shownLow15 = false
                shownLow10 = false
            } else {
                val cfg = latestSettings.eventConfig(EventKind.LOW_BATTERY)
                if (latestSettings.batteryEnabled && cfg.enabled) {
                    when {
                        pct <= 10 && !shownLow10 -> {
                            shownLow10 = true
                            shownLow15 = true
                            shownLow20 = true
                            emitLowBatteryCapsule(pct, cfg.autoExpand)
                        }
                        pct <= 15 && !shownLow15 -> {
                            shownLow15 = true
                            shownLow20 = true
                            emitLowBatteryCapsule(pct, cfg.autoExpand)
                        }
                        pct <= 20 && !shownLow20 -> {
                            shownLow20 = true
                            emitLowBatteryCapsule(pct, cfg.autoExpand)
                        }
                    }
                }
            }
        }

        if (isFull && !lastFullNotified) {
            lastFullNotified = true
            val cfg = latestSettings.eventConfig(EventKind.CHARGING)
            if (latestSettings.batteryEnabled && cfg.enabled) {
                islandController.upsertActivity(
                    IslandActivity.Alert(
                        id = "alert_charging_full",
                        kind = AlertKind.CHARGING_FULL,
                        title = "Battery Full (100%)",
                        subtitle = "Unplug charger to preserve battery health"
                    ),
                    autoShowCompact = true,
                    autoDismissMs = 2000L,
                    autoExpandToFull = cfg.autoExpand
                )
            }
        } else if (!isCharging) {
            lastFullNotified = false
        }
    }

    private fun emitLowBatteryCapsule(pct: Int, autoExpand: Boolean) {
        islandController.upsertActivity(
            IslandActivity.Alert(
                id = "alert_low_battery_$pct",
                kind = AlertKind.LOW_BATTERY,
                title = "Low Battery · $pct%",
                subtitle = if (pct <= 15) "Connect charger soon" else "20% battery remaining"
            ),
            autoShowCompact = true,
            autoDismissMs = 2000L,
            autoExpandToFull = autoExpand
        )
    }

    private fun emitChargingCapsule(
        pct: Int,
        isFull: Boolean,
        wattage: Float?,
        pluggedLabel: String,
        isFreshConnect: Boolean
    ) {
        val cfg = latestSettings.eventConfig(EventKind.CHARGING)
        if (!latestSettings.batteryEnabled || !cfg.enabled) return
        val capsuleDuration = 2000L
        islandController.upsertActivity(
            IslandActivity.Charging(
                levelPercent = pct,
                isCharging = true,
                isFull = isFull,
                wattageWatts = wattage,
                pluggedLabel = pluggedLabel
            ),
            autoShowCompact = isFreshConnect,
            autoDismissMs = if (isFreshConnect) capsuleDuration else null,
            autoExpandToFull = isFreshConnect && cfg.autoExpand
        )
    }
}
