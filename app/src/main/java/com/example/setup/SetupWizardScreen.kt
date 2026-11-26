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
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.DynamicIslandApp
import com.example.island.IslandService
import com.example.keepalive.IslandKeepAliveService
import com.example.system.IslandSettings
import com.example.ui.theme.CrimsonPillButton
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MockupScreenHeader
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PermissionStatusSnapshot(
    val accessibilityGranted: Boolean,
    val notificationListenerGranted: Boolean,
    val overlayGranted: Boolean,
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
    val overlayOk = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)

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
        overlayGranted = overlayOk,
        postNotificationsGranted = postNotif,
        dndPolicyGranted = dnd,
        microphoneGranted = mic,
        exactAlarmGranted = exactAlarm,
        batteryUnrestricted = batteryOk
    )
}

@Composable
fun SetupWizardScreen(
    app: DynamicIslandApp,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var refreshTick by remember { mutableIntStateOf(0) }
    val accessibilityLive by IslandService.isRunning.collectAsState()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val status = remember(refreshTick, accessibilityLive) { readPermissionStatus(context) }

    DisposableEffect(lifecycleOwner, settings.islandEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
                val snap = readPermissionStatus(context)
                if (settings.islandEnabled && (snap.accessibilityGranted || snap.overlayGranted)) {
                    IslandKeepAliveService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }
    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }
    val screenshotsFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch {
                app.settingsRepository.update { it.copy(vaultScreenshotsTreeUri = treeUri.toString()) }
            }
        }
    }

    ObsidianCrimsonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Setup Wizard",
                subtitle = "Grant overlay & MIUI permissions",
                icon = Icons.Filled.Layers,
                onBack = onDone,
                backTestTag = "wizard_back"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero neutral capsule emblem
                val accent = MaterialTheme.colorScheme.primary
                var activeStep by remember(status) {
                    mutableIntStateOf(
                        when {
                            !status.accessibilityGranted -> 0
                            !status.notificationListenerGranted -> 1
                            !status.overlayGranted -> 2
                            !settings.calibrationConfirmed -> 3
                            else -> 4
                        }
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(86.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.34f),
                                    IslandColors.ObsidianCardElevated,
                                    IslandColors.ObsidianBg
                                )
                            )
                        )
                        .border(1.4.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.5.dp, accent, CircleShape)
                    )
                }

                Text(
                    text = "Dynamic Island",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your phone, beautifully alive.",
                    color = IslandColors.TextSecondary,
                    fontSize = 14.sp
                )

                // 5-Step Guided Progress Indicator (Accessibility -> Notifications -> Overlay -> Cutout -> Done)
                val stepLabels = listOf("Access", "Notifs", "Overlay", "Cutout", "Done")
                ObsidianGlassCard(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        stepLabels.forEachIndexed { idx, label ->
                            val stepDone = when (idx) {
                                0 -> status.accessibilityGranted
                                1 -> status.notificationListenerGranted
                                2 -> status.overlayGranted
                                3 -> settings.calibrationConfirmed
                                else -> settings.wizardCompleted
                            }
                            val current = activeStep == idx
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { activeStep = idx }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                stepDone -> IslandColors.AccentGreen.copy(alpha = 0.25f)
                                                current -> accent.copy(alpha = 0.28f)
                                                else -> Color.White.copy(alpha = 0.08f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                stepDone -> IslandColors.AccentGreen
                                                current -> accent
                                                else -> Color.White.copy(alpha = 0.16f)
                                            },
                                            CircleShape
                                        )
                                ) {
                                    if (stepDone) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = IslandColors.AccentGreen, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text("${idx + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = if (current) Color.White else IslandColors.TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step ${activeStep + 1} of 5 · ${stepLabels[activeStep]}",
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                if (activeStep < 4) activeStep++ else onDone()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Skip", color = IslandColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 3 Primary Permission Cards matching setup-wizard mockup
                WizardPermissionRow(
                    title = "Accessibility",
                    subtitle = "Let the island appear over lock screen & other apps.",
                    icon = Icons.Filled.AccessibilityNew,
                    granted = status.accessibilityGranted,
                    buttonLabel = "Grant",
                    testTag = "wizard_acc_btn",
                    onAction = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        refreshTick++
                    }
                )

                WizardPermissionRow(
                    title = "Notifications",
                    subtitle = "Get alerts and live updates on the island.",
                    icon = Icons.Filled.Notifications,
                    granted = status.notificationListenerGranted,
                    buttonLabel = "Grant",
                    testTag = "wizard_nl_btn",
                    onAction = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                        refreshTick++
                    }
                )

                WizardPermissionRow(
                    title = "Draw Over Other Apps / Overlay",
                    subtitle = "Show the island reliably on top of everything.",
                    icon = Icons.Filled.Layers,
                    granted = status.overlayGranted,
                    buttonLabel = "Grant",
                    testTag = "wizard_overlay_btn",
                    onAction = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(intent) }
                        refreshTick++
                    }
                )

                // Continue & Skip CTA matching setup-wizard mockup
                CrimsonPillButton(
                    text = if (status.accessibilityGranted || status.overlayGranted) "Continue & Activate" else "Finish Setup",
                    onClick = {
                        scope.launch {
                            app.settingsRepository.update {
                                it.copy(wizardCompleted = true, calibrationConfirmed = true)
                            }
                            withContext(Dispatchers.Main) {
                                if (settings.islandEnabled && (status.accessibilityGranted || status.overlayGranted)) {
                                    IslandKeepAliveService.start(context)
                                }
                                onDone()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_finish_btn")
                )

                Text(
                    text = "Skip for now",
                    color = IslandColors.TextSecondary,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable(onClick = onDone)
                        .padding(vertical = 4.dp)
                )

                // Additional Feature Permissions
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Additional Permissions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                WizardPermissionRow(
                    title = "Post Notifications (Android 13)",
                    subtitle = "Keep-alive monitor & timer completion alerts.",
                    icon = Icons.Filled.NotificationsActive,
                    granted = status.postNotificationsGranted,
                    buttonLabel = "Allow",
                    testTag = "wizard_notif_btn",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                WizardPermissionRow(
                    title = "Do Not Disturb Policy",
                    subtitle = "Allows Tools to toggle Do Not Disturb directly.",
                    icon = Icons.Filled.DoNotDisturbOn,
                    granted = status.dndPolicyGranted,
                    buttonLabel = "Grant",
                    testTag = "wizard_dnd_btn",
                    onAction = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                        refreshTick++
                    }
                )

                WizardPermissionRow(
                    title = "Microphone (Voice Memo)",
                    subtitle = "Only active while recording a voice memo.",
                    icon = Icons.Filled.Mic,
                    granted = status.microphoneGranted,
                    buttonLabel = "Allow",
                    testTag = "wizard_mic_btn",
                    onAction = { micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )

                WizardPermissionRow(
                    title = "Exact Alarms",
                    subtitle = "Fires countdown completion at the exact second.",
                    icon = Icons.Filled.Alarm,
                    granted = status.exactAlarmGranted,
                    buttonLabel = "Allow",
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

                WizardPermissionRow(
                    title = "Screenshots Folder (Auto-Shelf)",
                    subtitle = "Automatically store new screenshots for 10 min via SAF.",
                    icon = Icons.Filled.FolderOpen,
                    granted = !settings.vaultScreenshotsTreeUri.isNullOrBlank(),
                    buttonLabel = "Select",
                    testTag = "wizard_screenshots_btn",
                    onAction = {
                        val initial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            DocumentsContract.buildDocumentUri(
                                "com.android.externalstorage.documents",
                                "primary:DCIM/Screenshots"
                            )
                        } else null
                        screenshotsFolderPicker.launch(initial)
                    }
                )

                // Android 13 Sideload & Restricted Settings Card
                ObsidianGlassCard(highlighted = !status.accessibilityGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = IslandColors.AccentOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("Android 13 Sideload Note", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "If installing via APK: open App Info -> 3-dots menu -> Allow restricted settings before switching Accessibility or Notification Access ON.",
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(i) }
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Open App Info", fontSize = 12.sp)
                    }
                }

                // MIUI 14 Checklist
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("MIUI 14 Survival Checklist (Redmi Note 10 Pro)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                MiuiChecklistCard(
                    step = "1. Autostart — ON",
                    detail = "Allow Dynamic Island to start automatically on boot.",
                    buttonLabel = "Autostart",
                    onClick = { openMiuiAutostart(context) }
                )
                MiuiChecklistCard(
                    step = "2. Display pop-up windows & Background — ON",
                    detail = "Enable background pop-ups in MIUI Other Permissions.",
                    buttonLabel = "Other Perms",
                    onClick = { openMiuiPermissions(context) }
                )
                MiuiChecklistCard(
                    step = "3. Battery Saver — No restrictions",
                    detail = if (status.batteryUnrestricted) "Granted: Unrestricted" else "Set MIUI Battery Saver to No restrictions.",
                    buttonLabel = "Battery",
                    onClick = { openBatteryUnrestricted(context) }
                )
                MiuiChecklistCard(
                    step = "4. Lock app in Recents",
                    detail = "Long-press Dynamic Island in Recents and tap the Lock icon so MIUI Clear All keeps it running.",
                    buttonLabel = null,
                    onClick = {}
                )
                MiuiChecklistCard(
                    step = "5. Disable Unused App Hibernation",
                    detail = "Switch OFF 'Pause app activity if unused' in App Info.",
                    buttonLabel = "App Info",
                    onClick = {
                        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(i) }
                    }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WizardPermissionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    granted: Boolean,
    buttonLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    ObsidianGlassCard(
        highlighted = !granted,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CrimsonSquircleIcon(
                icon = if (granted) Icons.Filled.Check else icon,
                tint = if (granted) IslandColors.AccentGreen else accent,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAction,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (granted) IslandColors.AccentGreen.copy(alpha = 0.22f) else accent,
                    contentColor = if (granted) IslandColors.AccentGreen else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    if (granted) IslandColors.AccentGreen.copy(alpha = 0.65f) else accent.copy(alpha = 0.72f)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp).testTag(testTag)
            ) {
                Text(
                    text = if (granted) "Granted" else buttonLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
    val accent = MaterialTheme.colorScheme.primary
    ObsidianGlassCard(contentPadding = PaddingValues(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(step, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(detail, color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
            if (buttonLabel != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onClick,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(buttonLabel, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
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
