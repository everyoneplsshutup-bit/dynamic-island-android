package com.example.setup

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.DynamicIslandApp
import com.example.island.IslandService
import com.example.ui.theme.IslandColors
import kotlinx.coroutines.launch

data class PermissionStatusSnapshot(
    val accessibilityGranted: Boolean,
    val notificationListenerGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val dndPolicyGranted: Boolean,
    val microphoneGranted: Boolean,
    val exactAlarmGranted: Boolean,
    val batteryUnrestricted: Boolean
)

fun readPermissionStatus(context: Context): PermissionStatusSnapshot {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    val accEnabled = IslandService.isRunning.value || (
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(context.packageName) == true
        )
    val nlEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?.contains(context.packageName) == true

    val postNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val dnd = nm?.isNotificationPolicyAccessGranted == true
    val exactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        am?.canScheduleExactAlarms() == true
    } else true
    val batteryOk = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

    return PermissionStatusSnapshot(
        accessibilityGranted = accEnabled,
        notificationListenerGranted = nlEnabled,
        postNotificationsGranted = postNotif,
        dndPolicyGranted = dnd,
        microphoneGranted = mic,
        exactAlarmGranted = exactAlarm,
        batteryUnrestricted = batteryOk
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    app: DynamicIslandApp,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshTick by remember { mutableIntStateOf(0) }
    val accessibilityLive by IslandService.isRunning.collectAsState()
    val status = remember(refreshTick, accessibilityLive) { readPermissionStatus(context) }

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }
    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Wizard & MIUI Checklist") },
                navigationIcon = {
                    IconButton(onClick = onDone, modifier = Modifier.testTag("wizard_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Android 13 Sideload Flow & Play Protect Note
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = IslandColors.AccentOrange.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = IslandColors.AccentOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("Android 13 Sideload & Play Protect Note", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "If installing via APK: open App Info → ⋮ (top right) → Allow restricted settings BEFORE enabling Accessibility or Notification Access. If Play Protect prompts on install, choose 'Install anyway' only for builds you compiled.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = {
                            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(i) }
                        }
                    ) {
                        Text("Open App Info (Restricted Settings)")
                    }
                }
            }

            Text("1. Core Permissions (in order)", style = MaterialTheme.typography.titleMedium)

            WizardPermissionRow(
                title = "Accessibility Service (Overlay Host)",
                subtitle = "Hosts the Dynamic Island overlay over status bar & lock screen.",
                granted = status.accessibilityGranted,
                buttonLabel = "Open Accessibility",
                testTag = "wizard_acc_btn",
                onAction = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    refreshTick++
                }
            )

            WizardPermissionRow(
                title = "Notification Listener Access",
                subtitle = "Previews notifications, mirrors Clock timers, and enables PixelMusic shuffle/repeat/like.",
                granted = status.notificationListenerGranted,
                buttonLabel = "Notification Access",
                testTag = "wizard_nl_btn",
                onAction = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    refreshTick++
                }
            )

            WizardPermissionRow(
                title = "Notifications (Android 13)",
                subtitle = "Shows Dynamic Island keep-alive monitor and timer completion alerts.",
                granted = status.postNotificationsGranted,
                buttonLabel = "Grant",
                testTag = "wizard_notif_btn",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            WizardPermissionRow(
                title = "Notification Policy (Do Not Disturb)",
                subtitle = "Allows the Tools tab to toggle Do Not Disturb directly.",
                granted = status.dndPolicyGranted,
                buttonLabel = "Grant DND",
                testTag = "wizard_dnd_btn",
                onAction = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                    refreshTick++
                }
            )

            WizardPermissionRow(
                title = "Microphone (Voice Memo only)",
                subtitle = "Requested in context; used only inside a microphone foreground service while recording.",
                granted = status.microphoneGranted,
                buttonLabel = "Allow Mic",
                testTag = "wizard_mic_btn",
                onAction = { micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )

            WizardPermissionRow(
                title = "Exact Alarms (Timer)",
                subtitle = "Fires countdown completion reliably at exact target second.",
                granted = status.exactAlarmGranted,
                buttonLabel = "Allow Alarms",
                testTag = "wizard_alarm_btn",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(i) }
                    }
                    refreshTick++
                }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Zero-Startup / In-Context Permissions", fontWeight = FontWeight.SemiBold)
                    Text(
                        "• MediaProjection is requested fresh ONLY when you tap Screenshot→Vault or OCR Tier B.\n" +
                            "• Photo Vault uses app-private storage + Android Photo Picker (no broad storage permission).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("2. MIUI 14 Survival Checklist (Redmi Note 10 Pro)", style = MaterialTheme.typography.titleMedium)

            MiuiChecklistCard(
                step = "1. Autostart — ON",
                detail = "Allow Dynamic Island to start automatically on boot.",
                buttonLabel = "MIUI Autostart",
                onClick = { openMiuiAutostart(context) }
            )
            MiuiChecklistCard(
                step = "2. Background autostart — ON",
                detail = "Allow background launch in MIUI App permissions → Other permissions.",
                buttonLabel = "Other Permissions",
                onClick = { openMiuiPermissions(context) }
            )
            MiuiChecklistCard(
                step = "3. Battery settings — No restrictions",
                detail = if (status.batteryUnrestricted) "Granted: Unrestricted" else "Set MIUI Battery Saver to 'No restrictions'.",
                buttonLabel = "Battery Settings",
                onClick = { openBatteryUnrestricted(context) }
            )
            MiuiChecklistCard(
                step = "4. Lock the app in Recents",
                detail = "Open Recents, long-press Dynamic Island card, and tap the Lock icon so MIUI 'Clear All' keeps it alive.",
                buttonLabel = null,
                onClick = {}
            )
            MiuiChecklistCard(
                step = "5. \"Pause app activity if unused\" — OFF",
                detail = "Disable unused app hibernation in App Info.",
                buttonLabel = "App Info",
                onClick = {
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    runCatching { context.startActivity(i) }
                }
            )
            MiuiChecklistCard(
                step = "6. Force-stop Accessibility note",
                detail = "Honest note: MIUI force-stop switches Accessibility OFF, and only you can switch it back on. Dynamic Island detects this and notifies you with a one-tap path back to Accessibility settings.",
                buttonLabel = "Accessibility",
                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }
            )

            Button(
                onClick = {
                    scope.launch {
                        app.settingsRepository.update { it.copy(wizardCompleted = true) }
                        onDone()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("wizard_finish_btn")
            ) {
                Text("Finish Setup")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WizardPermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    buttonLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (granted) "Granted" else "Not granted",
                tint = if (granted) IslandColors.AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onAction, modifier = Modifier.testTag(testTag)) {
                Text(if (granted) "Granted" else buttonLabel)
            }
        }
    }
}

@Composable
private fun MiuiChecklistCard(
    step: String,
    detail: String,
    buttonLabel: String?,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(step, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (buttonLabel != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onClick) {
                    Text(buttonLabel)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private fun openMiuiAutostart(context: Context) {
    val miuiIntent = Intent().apply {
        component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
    }
    if (runCatching { context.startActivity(miuiIntent) }.isFailure) {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        runCatching { context.startActivity(fallback) }
    }
}

private fun openMiuiPermissions(context: Context) {
    val miuiIntent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
        setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.permissions.PermissionsEditorActivity"
        )
        putExtra("extra_pkgname", context.packageName)
    }
    if (runCatching { context.startActivity(miuiIntent) }.isFailure) {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        runCatching { context.startActivity(fallback) }
    }
}

private fun openBatteryUnrestricted(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    if (runCatching { context.startActivity(intent) }.isFailure) {
        runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}
