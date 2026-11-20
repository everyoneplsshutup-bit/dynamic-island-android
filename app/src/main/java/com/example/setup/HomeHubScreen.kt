package com.example.setup

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.island.AlertsIslandPanel
import com.example.island.BottomChooserBar
import com.example.island.ClipsIslandPanel
import com.example.island.IslandService
import com.example.island.MemoIslandPanel
import com.example.island.NotesIslandPanel
import com.example.island.SearchIslandPanel
import com.example.island.ToolsIslandPanel
import com.example.island.VaultIslandPanel
import com.example.media.MusicExpandedView
import com.example.system.AccentTheme
import com.example.system.CalculatorIslandView
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.system.TimerStopwatchPanel
import com.example.ui.theme.IslandColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHubScreen(
    app: DynamicIslandApp,
    onOpenSettings: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenFeaturePicker: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val islandState by app.islandController.state.collectAsState()
    val isServiceRunning by IslandService.isRunning.collectAsState()
    val mediaState by app.mediaHub.state.collectAsState()
    val timerState by app.timerEngine.state.collectAsState()
    val calcState by app.calculatorEngine.state.collectAsState()

    val accentColor = when (settings.accentTheme) {
        AccentTheme.ORANGE -> IslandColors.AccentOrange
        AccentTheme.CYAN -> IslandColors.AccentCyan
        AccentTheme.GREEN -> IslandColors.AccentGreen
        AccentTheme.DYNAMIC -> IslandColors.AccentOrange
    }

    // Zero-permission Photo Picker for adding directly to 10-min Vault
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch { app.vaultManager.saveFromUri(uri, sourceType = "PICKED") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dynamic Island", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isServiceRunning) "Overlay active around punch-hole" else "Overlay inactive · Tap Setup Wizard",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isServiceRunning) IslandColors.AccentGreen else IslandColors.AccentOrange
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Open Settings")
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
                .padding(horizontal = 16.dp)
                .widthIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Honest MIUI force-stop Accessibility detection banner
            if (settings.accessibilityWasEnabled && !isServiceRunning) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = IslandColors.AccentRed.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = IslandColors.AccentRed)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Accessibility was switched off", fontWeight = FontWeight.Bold)
                            Text(
                                "MIUI force-stop disables Accessibility. Tap to re-enable.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentRed)
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }

            // Live Interactive Island Sandbox (allows testing Calculator, Timer, Chooser, etc. inside the app & browser emulator)
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = IslandColors.PillBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF101117), IslandColors.PillBlack)
                            )
                        )
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF24262E))
                                    .border(1.dp, accentColor, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Live Island · ${islandState.activeTab.title}",
                                color = IslandColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (isServiceRunning) "Overlay ON" else "Preview",
                            color = if (isServiceRunning) IslandColors.AccentGreen else accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    when (islandState.activeTab) {
                        IslandTab.MUSIC -> MusicExpandedView(
                            state = mediaState,
                            accentColor = accentColor,
                            onTogglePlayPause = { app.mediaHub.togglePlayPause() },
                            onPrevious = { app.mediaHub.skipPrevious() },
                            onNext = { app.mediaHub.skipNext() },
                            onSeekTo = { app.mediaHub.seekTo(it) },
                            onToggleShuffle = { app.mediaHub.sendShuffleCommand() },
                            onCycleRepeat = { app.mediaHub.sendRepeatCommand() },
                            onToggleLike = { app.mediaHub.sendLikeCommand() },
                            onOpenPlayer = { app.mediaHub.openPlayerApp() }
                        )

                        IslandTab.CALC -> CalculatorIslandView(
                            state = calcState,
                            onDigit = { app.calculatorEngine.pressDigit(it) },
                            onDecimal = { app.calculatorEngine.pressDecimal() },
                            onClear = { app.calculatorEngine.pressClear() },
                            onToggleSign = { app.calculatorEngine.pressToggleSign() },
                            onPercent = { app.calculatorEngine.pressPercent() },
                            onOperator = { app.calculatorEngine.pressOperator(it) },
                            onEquals = { app.calculatorEngine.pressEquals() },
                            compactMode = false
                        )

                        IslandTab.TIMER -> TimerStopwatchPanel(
                            state = timerState,
                            accentColor = accentColor,
                            onPickerChanged = { h, m, s -> app.timerEngine.setPickerDuration(h, m, s) },
                            onStartTimer = { app.timerEngine.startTimer() },
                            onPauseTimer = { app.timerEngine.pauseTimer() },
                            onResumeTimer = { app.timerEngine.resumeTimer() },
                            onCancelTimer = { app.timerEngine.cancelTimer() },
                            onStartStopwatch = { app.timerEngine.startStopwatch() },
                            onPauseStopwatch = { app.timerEngine.pauseStopwatch() },
                            onResetStopwatch = { app.timerEngine.resetStopwatch() },
                            onLapStopwatch = { app.timerEngine.lapStopwatch() }
                        )

                        IslandTab.SEARCH -> SearchIslandPanel(app = app, accentColor = accentColor, onCollapse = {})
                        IslandTab.TOOLS -> ToolsIslandPanel(app = app, settings = settings, accentColor = accentColor, onHaptic = {}, onCollapse = {})
                        IslandTab.VAULT -> VaultIslandPanel(app = app, accentColor = accentColor, onCollapse = {})
                        IslandTab.CLIPS -> ClipsIslandPanel(app = app, accentColor = accentColor)
                        IslandTab.NOTES -> NotesIslandPanel(app = app, accentColor = accentColor)
                        IslandTab.MEMO -> MemoIslandPanel(app = app, accentColor = accentColor)
                        IslandTab.ALERTS -> AlertsIslandPanel(app = app, state = islandState, accentColor = accentColor)
                    }

                    Spacer(Modifier.height(8.dp))

                    BottomChooserBar(
                        settings = settings,
                        activeTab = islandState.activeTab,
                        isMoreOpen = islandState.isMoreRowOpen,
                        accentColor = accentColor,
                        useLightGlass = false,
                        onSelectTab = { app.islandController.selectTab(it) },
                        onToggleMore = { app.islandController.toggleMoreRow() }
                    )
                }
            }

            // Quick Actions Grid
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HubTile(
                    title = "Setup Wizard",
                    subtitle = if (settings.wizardCompleted) "Checklist ready" else "Required steps",
                    icon = if (settings.wizardCompleted) Icons.Filled.CheckCircle else Icons.Filled.RocketLaunch,
                    modifier = Modifier.weight(1f).testTag("open_wizard_button"),
                    onClick = onOpenWizard
                )
                HubTile(
                    title = "Calibration",
                    subtitle = "Align punch-hole",
                    icon = Icons.Filled.CenterFocusStrong,
                    modifier = Modifier.weight(1f).testTag("open_calibration_button"),
                    onClick = onOpenCalibration
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HubTile(
                    title = "Customise (${settings.enabledTabs.size}/8)",
                    subtitle = "Pick & reorder tabs",
                    icon = Icons.Filled.DashboardCustomize,
                    modifier = Modifier.weight(1f).testTag("open_feature_picker_button"),
                    onClick = onOpenFeaturePicker
                )
                HubTile(
                    title = "Notifications",
                    subtitle = "Inbox & app rules",
                    icon = Icons.Filled.Notifications,
                    modifier = Modifier.weight(1f).testTag("open_notifications_button"),
                    onClick = onOpenNotifications
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add to Vault")
                }

                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("hub_settings_btn")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("All Settings")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HubTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
