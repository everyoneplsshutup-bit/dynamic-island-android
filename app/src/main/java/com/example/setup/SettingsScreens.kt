package com.example.setup

import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.DynamicIslandApp
import com.example.floating.FloatingLaunchPath
import com.example.ocr.OcrPrivacyGate
import com.example.search.LaunchableApp
import com.example.system.AccentTheme
import com.example.system.AnimationStyle
import com.example.system.CompactPreference
import com.example.system.CompanionPosition
import com.example.system.ContentEffect
import com.example.system.EventBehaviorConfig
import com.example.system.EventKind
import com.example.system.ExpandedSwipeAction
import com.example.system.HapticsStrength
import com.example.system.IdleWidgetType
import com.example.system.IslandGesture
import com.example.system.IslandSettings
import com.example.system.LockScreenPreset
import com.example.system.OpeningEffect
import com.example.system.PillStyle
import com.example.system.PillSwipeAction
import com.example.system.SpringFeel
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MotionTokens
import kotlinx.coroutines.launch

enum class SettingsGroup(val title: String, val subtitle: String, val icon: ImageVector) {
    LOOK("Look", "Pill style, glass density, solid fallback & theme", Icons.Filled.Palette),
    MOTION("Motion", "Animation speed, spring feel, haptics & sound", Icons.Filled.Animation),
    PILL("Pill", "Camera calibration, compact view, idle widget & lock screen", Icons.Filled.RadioButtonChecked),
    FEATURES("Features", "Master toggles, Customise Island, quick toggles & apps", Icons.Filled.Tune),
    PRIVACY("Privacy", "Screen reading / OCR (off by default), Vault & clipboard", Icons.Filled.Security),
    ABOUT("About / Danger Zone", "Version, permissions status, backup, reset & delete", Icons.Filled.Info)
}

private data class SettingsSearchEntry(
    val title: String,
    val subtext: String,
    val breadcrumb: String,
    val group: SettingsGroup
)

private val settingsIndex = listOf(
    SettingsSearchEntry("Pill Style", "Solid Black or Apple-type Glass", "Look › Style", SettingsGroup.LOOK),
    SettingsSearchEntry("Glass Density", "Clear to Tinted density slider", "Look › Glass", SettingsGroup.LOOK),
    SettingsSearchEntry("Solid Fallback", "Force solid black over glass", "Look › Fallback", SettingsGroup.LOOK),
    SettingsSearchEntry("Accent Theme", "Orange, Cyan, Green, or Dynamic", "Look › Theme", SettingsGroup.LOOK),
    SettingsSearchEntry("Animation Style", "Like a Fruit jelly, Spring, Smooth, or Off", "Motion › Style", SettingsGroup.MOTION),
    SettingsSearchEntry("Animation Speed", "Adjust spring transition speed", "Motion › Speed", SettingsGroup.MOTION),
    SettingsSearchEntry("Elasticity", "Adjust overshoot bounce intensity", "Motion › Elasticity", SettingsGroup.MOTION),
    SettingsSearchEntry("Two-Stage Open", "Compact pill -> Stage 1 preview -> Full card", "Motion › Stages", SettingsGroup.MOTION),
    SettingsSearchEntry("Try It Motion Demo", "Interactive capsule animation playground", "Motion › Demo", SettingsGroup.MOTION),
    SettingsSearchEntry("Reduced Motion", "Calm fallback for sensitive eyes", "Motion › Accessibility", SettingsGroup.MOTION),
    SettingsSearchEntry("Haptics Strength", "Off, Light tick, or Medium click", "Motion › Haptics", SettingsGroup.MOTION),
    SettingsSearchEntry("Sound Effects", "Enable or mute Dynamic Island UI sound effects", "Motion › Sound", SettingsGroup.MOTION),
    SettingsSearchEntry("Sound Volume", "Adjust UI sound effects volume", "Motion › Sound", SettingsGroup.MOTION),
    SettingsSearchEntry("Camera Calibration", "Nudge X, Y, width, and height around punch-hole", "Pill › Calibration", SettingsGroup.PILL),
    SettingsSearchEntry("Companion Circle Side", "Place +1 split bubble on Left or Right", "Pill › Split", SettingsGroup.PILL),
    SettingsSearchEntry("Compact Pill Content", "Auto, Music, Timer, Battery, or Notifications", "Pill › Compact", SettingsGroup.PILL),
    SettingsSearchEntry("Idle Widget", "None, Clock, Battery %, Search, or Quick Note", "Pill › Idle", SettingsGroup.PILL),
    SettingsSearchEntry("Lock-Screen Preset", "Minimal, Hide Message Text, or Hidden", "Pill › Lock Screen", SettingsGroup.PILL),
    SettingsSearchEntry("Per-Event Switches", "Enable & auto-expand behavior per event kind", "Features › Events", SettingsGroup.FEATURES),
    SettingsSearchEntry("Customise Island", "Pick & reorder up to 8 island features", "Features › Chooser", SettingsGroup.FEATURES),
    SettingsSearchEntry("Master Feature Toggles", "Enable/disable Music, Timer, Calc, Vault, OCR & more", "Features › Toggles", SettingsGroup.FEATURES),
    SettingsSearchEntry("Floating-Window Apps", "Pick apps for MIUI sidebar & freeform launch", "Features › Floating", SettingsGroup.FEATURES),
    SettingsSearchEntry("Pinned Quick Launch Apps", "Choose shortcuts shown in expanded island", "Features › Shortcuts", SettingsGroup.FEATURES),
    SettingsSearchEntry("Screen Reading / OCR", "Strictly opt-in, OFF by default", "Privacy › Screen Reading", SettingsGroup.PRIVACY),
    SettingsSearchEntry("OCR Tier B (ML Kit)", "Opt-in cropped fallback when Tier A finds no text", "Privacy › OCR Tier B", SettingsGroup.PRIVACY),
    SettingsSearchEntry("Vault 10-Min Rules", "3-finger screenshot auto-capture & 24h trash", "Privacy › Vault", SettingsGroup.PRIVACY),
    SettingsSearchEntry("Clipboard History", "Foreground-only capture on Android 13", "Privacy › Clips", SettingsGroup.PRIVACY),
    SettingsSearchEntry("Permissions Status", "Live granted / not-granted hardware checks", "About › Permissions", SettingsGroup.ABOUT),
    SettingsSearchEntry("Backup / Restore Settings", "Export or import settings JSON", "About › Backup", SettingsGroup.ABOUT),
    SettingsSearchEntry("Delete All App Data", "Erase Vault, Notes, Memos, Inbox & settings", "About › Danger Zone", SettingsGroup.ABOUT)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(
    app: DynamicIslandApp,
    onBack: () -> Unit,
    onOpenGroup: (SettingsGroup) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val matchingEntries = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else settingsIndex.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.subtext.contains(q, ignoreCase = true) ||
                it.breadcrumb.contains(q, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .widthIn(max = 600.dp)
                .testTag("settings_home_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search settings...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search settings") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_search_input")
                )
            }

            if (searchQuery.isNotBlank()) {
                if (matchingEntries.isEmpty()) {
                    item {
                        Text(
                            "No settings matched \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(matchingEntries) { entry ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenGroup(entry.group) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(entry.breadcrumb, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(entry.title, fontWeight = FontWeight.SemiBold)
                                Text(entry.subtext, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                // Clean 6-group structure
                items(SettingsGroup.entries) { group ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGroup(group) }
                            .testTag("settings_group_${group.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = group.icon,
                                contentDescription = null,
                                tint = if (group == SettingsGroup.ABOUT) IslandColors.AccentRed else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(group.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGroupDetailScreen(
    app: DynamicIslandApp,
    group: SettingsGroup,
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenFeaturePicker: () -> Unit,
    onOpenNotificationHistory: () -> Unit,
    onOpenWizard: () -> Unit
) {
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showOcrDisclosure by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showMotionDemoDialog by remember { mutableStateOf(false) }
    var motionSubSection by remember { mutableIntStateOf(0) } // 0 = All, 1 = Background, 2 = Animations, 3 = Gestures
    var backupText by remember { mutableStateOf("") }
    var showAppMultiPickerFor by remember { mutableStateOf<String?>(null) } // "FLOATING" or "PINNED"

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
                Toast.makeText(context, "Screenshots folder linked for 10-min Auto-Vault", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.title) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .widthIn(max = 600.dp)
                .testTag("settings_detail_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (group) {
                SettingsGroup.LOOK -> {
                    item {
                        SettingsSectionHeader("Pill Style")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            PillStyle.entries.forEachIndexed { idx, style ->
                                SegmentedButton(
                                    selected = settings.pillStyle == style,
                                    onClick = { scope.launch { app.settingsRepository.update { it.copy(pillStyle = style) } } },
                                    shape = SegmentedButtonDefaults.itemShape(idx, PillStyle.entries.size)
                                ) {
                                    Text(if (style == PillStyle.BLACK) "Solid Black" else "Apple Glass")
                                }
                            }
                        }
                    }
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Glass Density", fontWeight = FontWeight.Medium)
                                Text(
                                    if (settings.glassDensity < 0.35f) "Clear" else if (settings.glassDensity > 0.72f) "Tinted" else "Balanced",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.glassDensity,
                                onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(glassDensity = v) } } },
                                enabled = settings.pillStyle == PillStyle.GLASS && !settings.solidFallback
                            )
                        }
                    }
                    item {
                        SwitchRow(
                            title = "Solid Fallback",
                            helper = "Use solid black background instead of translucent glass on floating chrome.",
                            checked = settings.solidFallback,
                            onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(solidFallback = c) } } }
                        )
                    }
                    item {
                        SettingsSectionHeader("Accent Theme")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            AccentTheme.entries.forEachIndexed { idx, theme ->
                                SegmentedButton(
                                    selected = settings.accentTheme == theme,
                                    onClick = { scope.launch { app.settingsRepository.update { it.copy(accentTheme = theme) } } },
                                    shape = SegmentedButtonDefaults.itemShape(idx, AccentTheme.entries.size)
                                ) {
                                    Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                SettingsGroup.MOTION -> {
                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("All", "Background", "Animations", "Gestures").forEachIndexed { idx, label ->
                                SegmentedButton(
                                    selected = motionSubSection == idx,
                                    onClick = {
                                        app.soundManager.playTap()
                                        motionSubSection = idx
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(idx, 4),
                                    modifier = Modifier.testTag("motion_subtab_${label.lowercase()}")
                                ) {
                                    Text(label, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    if (motionSubSection == 1) {
                        item {
                            SettingsSectionHeader("Background & Surface")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                PillStyle.entries.forEachIndexed { idx, style ->
                                    SegmentedButton(
                                        selected = settings.pillStyle == style,
                                        onClick = { scope.launch { app.settingsRepository.update { it.copy(pillStyle = style) } } },
                                        shape = SegmentedButtonDefaults.itemShape(idx, PillStyle.entries.size)
                                    ) {
                                        Text(if (style == PillStyle.BLACK) "Solid Black" else "Apple Glass")
                                    }
                                }
                            }
                        }
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Glass Density", fontWeight = FontWeight.Medium)
                                    Text("${(settings.glassDensity * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = settings.glassDensity,
                                    onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(glassDensity = v) } } },
                                    enabled = settings.pillStyle == PillStyle.GLASS && !settings.solidFallback
                                )
                            }
                        }
                    }

                    if (motionSubSection == 0 || motionSubSection == 2) {
                        item {
                            SettingsSectionHeader("Motion Style (Springy / Standard / Calm)")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                AnimationStyle.entries.forEachIndexed { idx, style ->
                                    SegmentedButton(
                                        selected = settings.animationStyle == style,
                                        onClick = {
                                            app.soundManager.playTap()
                                            scope.launch { app.settingsRepository.update { it.copy(animationStyle = style) } }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(idx, AnimationStyle.entries.size),
                                        modifier = Modifier.testTag("motion_style_${style.name.lowercase()}")
                                    ) {
                                        Text(style.label, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                        item {
                            SettingsSectionHeader("Capsule Opening Effect")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                OpeningEffect.entries.forEachIndexed { idx, eff ->
                                    SegmentedButton(
                                        selected = settings.openingEffect == eff,
                                        onClick = {
                                            app.soundManager.playTap()
                                            scope.launch { app.settingsRepository.update { it.copy(openingEffect = eff) } }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(idx, OpeningEffect.entries.size),
                                        modifier = Modifier.testTag("opening_effect_${eff.name.lowercase()}")
                                    ) {
                                        Text(eff.label, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                        item {
                            SettingsSectionHeader("Inner Content Effect")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                ContentEffect.entries.forEachIndexed { idx, eff ->
                                    SegmentedButton(
                                        selected = settings.contentEffect == eff,
                                        onClick = {
                                            app.soundManager.playTap()
                                            scope.launch { app.settingsRepository.update { it.copy(contentEffect = eff) } }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(idx, ContentEffect.entries.size),
                                        modifier = Modifier.testTag("content_effect_${eff.name.lowercase()}")
                                    ) {
                                        Text(eff.label, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Speed", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("${String.format("%.1f", settings.animationSpeed)}x", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                    Slider(
                                        value = settings.animationSpeed,
                                        valueRange = 0.6f..1.6f,
                                        onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(animationSpeed = v) } } },
                                        modifier = Modifier.testTag("motion_speed_slider")
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Elasticity", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("${(settings.animationElasticity * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                    Slider(
                                        value = settings.animationElasticity,
                                        valueRange = 0f..1f,
                                        enabled = settings.animationStyle == AnimationStyle.FRUIT || settings.animationStyle == AnimationStyle.SPRING,
                                        onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(animationElasticity = v) } } },
                                        modifier = Modifier.testTag("motion_elasticity_slider")
                                    )
                                }
                            }
                        }
                    }

                    if (motionSubSection == 0 || motionSubSection == 3) {
                        item {
                            SettingsSectionHeader("Swipes & Long-Press Gestures")
                            EnumPickerRow(
                                title = "Collapsed Pill Swipe Left",
                                helper = "Next track, cycle notifications, adjust volume, or open search.",
                                selectedLabel = settings.pillSwipeLeftGesture.label,
                                options = PillSwipeAction.entries.map { it.label to it },
                                onSelect = { a -> scope.launch { app.settingsRepository.update { it.copy(pillSwipeLeftGesture = a) } } }
                            )
                        }
                        item {
                            EnumPickerRow(
                                title = "Collapsed Pill Swipe Right",
                                helper = "Previous track, volume up, or open search.",
                                selectedLabel = settings.pillSwipeRightGesture.label,
                                options = PillSwipeAction.entries.map { it.label to it },
                                onSelect = { a -> scope.launch { app.settingsRepository.update { it.copy(pillSwipeRightGesture = a) } } }
                            )
                        }
                        item {
                            EnumPickerRow(
                                title = "Expanded Card Swipe Left",
                                helper = "Dismiss active notification or collapse card.",
                                selectedLabel = settings.expandedSwipeLeftGesture.label,
                                options = ExpandedSwipeAction.entries.map { it.label to it },
                                onSelect = { a -> scope.launch { app.settingsRepository.update { it.copy(expandedSwipeLeftGesture = a) } } }
                            )
                        }
                        item {
                            EnumPickerRow(
                                title = "Expanded Card Swipe Right",
                                helper = "Cycle to next notification or collapse.",
                                selectedLabel = settings.expandedSwipeRightGesture.label,
                                options = ExpandedSwipeAction.entries.map { it.label to it },
                                onSelect = { a -> scope.launch { app.settingsRepository.update { it.copy(expandedSwipeRightGesture = a) } } }
                            )
                        }
                        item {
                            EnumPickerRow(
                                title = "Pill Long-Press Action",
                                helper = "Expand island, trigger Select Mode OCR, or open Customise.",
                                selectedLabel = settings.longPressGesture.name.replace("_", " "),
                                options = IslandGesture.entries.map { it.name.replace("_", " ") to it },
                                onSelect = { g -> scope.launch { app.settingsRepository.update { it.copy(longPressGesture = g) } } }
                            )
                        }
                    }

                    item {
                        SettingsSectionHeader("Haptics Strength")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            HapticsStrength.entries.forEachIndexed { idx, h ->
                                SegmentedButton(
                                    selected = settings.hapticsStrength == h,
                                    onClick = { scope.launch { app.settingsRepository.update { it.copy(hapticsStrength = h) } } },
                                    shape = SegmentedButtonDefaults.itemShape(idx, HapticsStrength.entries.size)
                                ) {
                                    Text(h.name.lowercase().replaceFirstChar { it.uppercase() })
                                }
                            }
                        }
                    }
                    item {
                        SwitchRow(
                            title = "Two-Stage Open",
                            helper = "First tap reveals Stage 1 compact preview; second tap opens Stage 2 full card.",
                            checked = settings.twoStageOpen,
                            onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(twoStageOpen = c) } } }
                        )
                    }
                    item {
                        SwitchRow(
                            title = "Reduced Motion",
                            helper = "Uses calm critically-damped transitions (also respects system animation scale).",
                            checked = settings.reducedMotion,
                            onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(reducedMotion = c) } } }
                        )
                    }
                    item {
                        SettingsSectionHeader("Sound Effects")
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Sound Effects", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Short UI chimes for expand, collapse, taps, timer finish, and switches.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Switch(
                                    checked = settings.soundEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            app.soundManager.updateCachedAudioSettings(true, settings.soundVolume)
                                            app.soundManager.playToggle(isOn = true)
                                        } else {
                                            app.soundManager.playToggle(isOn = false)
                                            app.soundManager.updateCachedAudioSettings(false, settings.soundVolume)
                                        }
                                        scope.launch { app.settingsRepository.update { it.copy(soundEnabled = enabled) } }
                                    },
                                    modifier = Modifier.testTag("motion_sound_switch")
                                )
                            }
                        }
                    }
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sound Volume", fontWeight = FontWeight.Medium)
                                Text("${(settings.soundVolume * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = settings.soundVolume,
                                valueRange = 0f..1f,
                                enabled = settings.soundEnabled,
                                onValueChange = { v ->
                                    app.soundManager.updateCachedAudioSettings(settings.soundEnabled, v)
                                    scope.launch { app.settingsRepository.update { it.copy(soundVolume = v) } }
                                },
                                onValueChangeFinished = {
                                    app.soundManager.playTap()
                                },
                                modifier = Modifier.testTag("motion_sound_volume_slider")
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                app.soundManager.playExpand()
                                showMotionDemoDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("motion_try_it_btn")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Try It · Interactive Motion Demo")
                        }
                    }
                }

                SettingsGroup.PILL -> {
                    item {
                        ActionRow(
                            title = "Camera Calibration",
                            helper = "Nudge X (${settings.cutoutXOffsetDp.toInt()}dp), Y (${settings.cutoutYOffsetDp.toInt()}dp), W (${settings.cutoutWidthDp.toInt()}dp), H (${settings.cutoutHeightDp.toInt()}dp)",
                            onClick = onOpenCalibration
                        )
                    }
                    item {
                        SettingsSectionHeader("Split Companion Circle Side")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            CompanionPosition.entries.forEachIndexed { idx, side ->
                                SegmentedButton(
                                    selected = settings.companionPosition == side,
                                    onClick = {
                                        app.soundManager.playTap()
                                        scope.launch { app.settingsRepository.update { it.copy(companionPosition = side) } }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(idx, CompanionPosition.entries.size),
                                    modifier = Modifier.testTag("pill_companion_${side.name.lowercase()}")
                                ) {
                                    Text(if (side == CompanionPosition.LEFT) "Left of Camera" else "Right of Camera")
                                }
                            }
                        }
                    }
                    item {
                        EnumPickerRow(
                            title = "Compact Pill Priority",
                            helper = "Choose what the compact pill prefers when multiple activities run.",
                            selectedLabel = settings.compactPreference.name.lowercase().replaceFirstChar { it.uppercase() },
                            options = CompactPreference.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } to it },
                            onSelect = { pref -> scope.launch { app.settingsRepository.update { it.copy(compactPreference = pref) } } }
                        )
                    }
                    item {
                        EnumPickerRow(
                            title = "Idle Widget Slot",
                            helper = "Optional micro-widget alongside the idle camera ring.",
                            selectedLabel = settings.idleWidget.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            options = IdleWidgetType.entries.map { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } to it },
                            onSelect = { w -> scope.launch { app.settingsRepository.update { it.copy(idleWidget = w) } } }
                        )
                    }
                    item {
                        EnumPickerRow(
                            title = "Lock-Screen Preset",
                            helper = "Controls island visibility and text redaction while locked.",
                            selectedLabel = settings.lockScreenPreset.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            options = LockScreenPreset.entries.map { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } to it },
                            onSelect = { p -> scope.launch { app.settingsRepository.update { it.copy(lockScreenPreset = p) } } }
                        )
                    }
                }

                SettingsGroup.FEATURES -> {
                    item {
                        ActionRow(
                            title = "Customise Island (${settings.enabledTabs.size} / 8)",
                            helper = "Choose and reorder up to 8 active modules on the island chooser.",
                            onClick = onOpenFeaturePicker,
                            testTag = "settings_customise_island_row"
                        )
                    }
                    item {
                        ActionRow(
                            title = "Notification Source Apps & History",
                            helper = "View notification inbox and toggle per-app island previews.",
                            onClick = onOpenNotificationHistory
                        )
                    }
                    item {
                        ActionRow(
                            title = "Floating-Window Apps (${settings.floatingAppPackages.size} selected)",
                            helper = "MIUI path status: ${runCatching { FloatingLaunchPath.valueOf(settings.verifiedFloatingPath).title }.getOrDefault(settings.verifiedFloatingPath)}",
                            onClick = { showAppMultiPickerFor = "FLOATING" }
                        )
                    }
                    item {
                        ActionRow(
                            title = "Pinned Quick Launch Shortcuts (${settings.pinnedAppPackages.size})",
                            helper = "Apps shown in the Quick Launch strip inside the expanded island.",
                            onClick = { showAppMultiPickerFor = "PINNED" }
                        )
                    }
                    item {
                        EnumPickerRow(
                            title = "Tap Gesture",
                            helper = "Action when tapping the compact island pill.",
                            selectedLabel = settings.tapGesture.name.replace("_", " "),
                            options = IslandGesture.entries.map { it.name.replace("_", " ") to it },
                            onSelect = { g -> scope.launch { app.settingsRepository.update { it.copy(tapGesture = g) } } }
                        )
                    }
                    item {
                        EnumPickerRow(
                            title = "Long-Press Gesture",
                            helper = "Action when long-pressing the compact island pill.",
                            selectedLabel = settings.longPressGesture.name.replace("_", " "),
                            options = IslandGesture.entries.map { it.name.replace("_", " ") to it },
                            onSelect = { g -> scope.launch { app.settingsRepository.update { it.copy(longPressGesture = g) } } }
                        )
                    }
                    item { SettingsSectionHeader("Master Feature Toggles") }
                    item {
                        SwitchRow("Music (PixelMusic-first)", "Media controls, lyrics & knob seek bar", settings.musicEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(musicEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("Timer & Stopwatch", "Apple Clock ring, wheel pickers & Clock mirror", settings.timerEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(timerEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("iOS Calculator", "Working 4×5 grid calculator", settings.calculatorEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(calculatorEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("Search Launcher", "Apps, contacts, notes, math & Google handoff", settings.searchEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(searchEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("Notifications & Alerts", "Compact notification previews & event alerts", settings.notificationsEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(notificationsEnabled = c, alertsEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("Charging & Battery", "Charging animation & 100% alert", settings.batteryEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(batteryEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("Quick Toggles & Floating Windows", "Torch, DND, brightness, Wi-Fi/BT panels & sidebar", settings.togglesEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(togglesEnabled = c, floatingEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("10-Minute Photo Vault", "Auto-expiring screenshots & shared images", settings.vaultEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(vaultEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("Notes, Voice Memo & Clips", "Quick notes, microphone memos & foreground clips", settings.notesEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(notesEnabled = c, memoEnabled = c, clipsEnabled = c) } }
                        }
                    }
                    item {
                        SwitchRow("OCR Text Copy Feature", "Allows Select Mode (still gated by Privacy toggle)", settings.ocrFeatureEnabled) { c ->
                            scope.launch { app.settingsRepository.update { it.copy(ocrFeatureEnabled = c) } }
                        }
                    }
                    item { SettingsSectionHeader("Per-Event Behavior Switches") }
                    items(EventKind.entries, key = { it.name }) { kind ->
                        val cfg = settings.eventConfig(kind)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(kind.label, fontWeight = FontWeight.Medium)
                                        Text(kind.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = cfg.enabled,
                                        onCheckedChange = { next ->
                                            app.soundManager.playToggle(isOn = next)
                                            scope.launch {
                                                app.settingsRepository.update { s ->
                                                    val map = s.perEventConfig.toMutableMap()
                                                    map[kind.name] = cfg.copy(enabled = next)
                                                    s.copy(perEventConfig = map)
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("event_enable_${kind.name.lowercase()}")
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text("Auto-expand when event arrives", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Switch(
                                        checked = cfg.autoExpand,
                                        enabled = cfg.enabled,
                                        onCheckedChange = { nextAuto ->
                                            app.soundManager.playToggle(isOn = nextAuto)
                                            scope.launch {
                                                app.settingsRepository.update { s ->
                                                    val map = s.perEventConfig.toMutableMap()
                                                    map[kind.name] = cfg.copy(autoExpand = nextAuto)
                                                    s.copy(perEventConfig = map)
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("event_autoexpand_${kind.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsGroup.PRIVACY -> {
                    // Screen reading / OCR toggle MUST be at the top of Privacy, OFF by default
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Screen Reading / OCR (Select Mode)", fontWeight = FontWeight.Bold)
                                        Text(
                                            "OFF by default. Reads ONLY the exact node you long-press in Select Mode. Never reads passwords or FLAG_SECURE.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = settings.screenReadingEnabled,
                                        onCheckedChange = { wantsOn ->
                                            app.soundManager.playToggle(isOn = wantsOn)
                                            if (wantsOn) {
                                                showOcrDisclosure = true
                                            } else {
                                                scope.launch {
                                                    app.settingsRepository.update {
                                                        it.copy(screenReadingEnabled = false, ocrTierBEnabled = false)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("privacy_screen_reading_switch")
                                    )
                                }
                            }
                        }
                    }
                    item {
                        SwitchRow(
                            title = "OCR Tier B (On-Device ML Kit Crop)",
                            helper = "Opt-in fallback only if Tier A finds no text. Captures one frame, crops to pressed element only, discards immediately.",
                            checked = settings.ocrTierBEnabled,
                            enabled = settings.screenReadingEnabled,
                            onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(ocrTierBEnabled = c) } } }
                        )
                    }
                    item {
                        SwitchRow(
                            title = "3-Finger Screenshot Auto-Vault",
                            helper = "Auto-store new screenshots in app-private Vault for 10 min (24h Trash).",
                            checked = settings.vaultAutoCapture3Finger,
                            onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(vaultAutoCapture3Finger = c) } } }
                        )
                    }
                    item {
                        ActionRow(
                            title = "Screenshots Folder (SAF Access)",
                            helper = if (settings.vaultScreenshotsTreeUri.isNullOrBlank()) {
                                "Not linked yet · Tap to select DCIM/Screenshots for automatic 10m Vault capture"
                            } else {
                                "Linked · Automatic screenshot Vault capture active"
                            },
                            onClick = {
                                app.soundManager.playTap()
                                val initial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    DocumentsContract.buildDocumentUri(
                                        "com.android.externalstorage.documents",
                                        "primary:DCIM/Screenshots"
                                    )
                                } else null
                                screenshotsFolderPicker.launch(initial)
                            }
                        )
                    }
                    item {
                        SwitchRow(
                            title = "Foreground Clipboard History",
                            helper = "Captures clipboard only when app/island is in foreground (Android 13 rule).",
                            checked = settings.clipboardHistoryEnabled,
                            onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(clipboardHistoryEnabled = c) } } }
                        )
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = IslandColors.AccentGreen.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "On-Device Privacy Guarantee: Dynamic Island declares zero INTERNET permission. Notes, Vault photos, memos, clips, and OCR stay strictly on your phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                SettingsGroup.ABOUT -> {
                    item {
                        val permSnap = remember { readPermissionStatus(context) }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Dynamic Island v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                                Text("Application ID: ${BuildConfig.APPLICATION_ID}", style = MaterialTheme.typography.labelSmall)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Text("Live Permissions Status:", fontWeight = FontWeight.SemiBold)
                                Text("• Accessibility Overlay: ${if (permSnap.accessibilityGranted) "Granted" else "Not granted"}")
                                Text("• Notification Listener: ${if (permSnap.notificationListenerGranted) "Granted" else "Not granted"}")
                                Text("• Notifications (Android 13): ${if (permSnap.postNotificationsGranted) "Granted" else "Not granted"}")
                                Text("• Do Not Disturb Policy: ${if (permSnap.dndPolicyGranted) "Granted" else "Not granted"}")
                                Text("• Microphone: ${if (permSnap.microphoneGranted) "Granted" else "In-context"}")
                                Text("• Exact Alarms: ${if (permSnap.exactAlarmGranted) "Granted" else "Not granted"}")
                                Text("• Battery Unrestricted: ${if (permSnap.batteryUnrestricted) "Granted" else "Restricted"}")
                            }
                        }
                    }
                    item {
                        ActionRow("Open Setup Wizard & MIUI Checklist", "Verify all permissions and MIUI keep-alive steps", onClick = onOpenWizard)
                    }
                    item {
                        ActionRow("Backup / Restore Settings", "Export or import settings JSON on-device", onClick = {
                            scope.launch {
                                backupText = app.settingsRepository.exportBackupJson()
                                showBackupDialog = true
                            }
                        })
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Danger Zone", color = IslandColors.AccentRed, fontWeight = FontWeight.Bold)
                    }
                    item {
                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = IslandColors.AccentRed),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Reset Settings to Default")
                        }
                    }
                    item {
                        Button(
                            onClick = { showDeleteAllConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentRed, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("danger_delete_all_btn")
                        ) {
                            Text("Delete All App Data")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Plain-language Disclosure before enabling Screen Reading / OCR
    if (showOcrDisclosure) {
        AlertDialog(
            onDismissRequest = { showOcrDisclosure = false },
            title = { Text(OcrPrivacyGate.DISCLOSURE_TITLE) },
            text = { Text(OcrPrivacyGate.DISCLOSURE_BODY) },
            confirmButton = {
                Button(onClick = {
                    showOcrDisclosure = false
                    app.soundManager.playToggle(isOn = true)
                    scope.launch { app.settingsRepository.update { it.copy(screenReadingEnabled = true) } }
                }) {
                    Text("I Understand, Turn ON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOcrDisclosure = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Settings?") },
            text = { Text("Restores Look, Motion, Pill calibration, and Privacy toggles to defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        scope.launch { app.settingsRepository.resetSettings() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentRed)
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            title = { Text("Delete All App Data?") },
            text = { Text("Permanently erases all Vault photos, Notes, Voice Memos, Clipboard history, Notification inbox, and Settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirm = false
                        scope.launch {
                            app.deleteAllUserData()
                            Toast.makeText(context, "All app data deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.AccentRed)
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup / Restore Settings JSON") },
            text = {
                OutlinedTextField(
                    value = backupText,
                    onValueChange = { backupText = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val res = app.settingsRepository.importBackupJson(backupText)
                        Toast.makeText(
                            context,
                            if (res.isSuccess) "Settings restored" else "Invalid JSON",
                            Toast.LENGTH_SHORT
                        ).show()
                        showBackupDialog = false
                    }
                }) { Text("Restore JSON") }
            },
            dismissButton = {
                TextButton(onClick = {
                    app.clipboardMonitor.copyToClipboard(backupText)
                    Toast.makeText(context, "Backup JSON copied", Toast.LENGTH_SHORT).show()
                }) { Text("Copy JSON") }
            }
        )
    }

    if (showAppMultiPickerFor != null) {
        val mode = showAppMultiPickerFor!!
        var allApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
        LaunchedEffect(Unit) { allApps = app.searchEngine.getInstalledApps() }
        val currentSelection = if (mode == "FLOATING") settings.floatingAppPackages else settings.pinnedAppPackages

        AlertDialog(
            onDismissRequest = { showAppMultiPickerFor = null },
            title = { Text(if (mode == "FLOATING") "Pick Floating-Window Apps" else "Pin Quick Launch Shortcuts") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(allApps, key = { it.packageName }) { item ->
                        val isChecked = item.packageName in currentSelection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        app.settingsRepository.update { s ->
                                            val list = (if (mode == "FLOATING") s.floatingAppPackages else s.pinnedAppPackages).toMutableList()
                                            if (item.packageName in list) list.remove(item.packageName) else list.add(item.packageName)
                                            if (mode == "FLOATING") s.copy(floatingAppPackages = list) else s.copy(pinnedAppPackages = list)
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(item.label, modifier = Modifier.weight(1f))
                            if (isChecked) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAppMultiPickerFor = null }) { Text("Done") }
            }
        )
    }

    if (showMotionDemoDialog) {
        InteractiveMotionDemoDialog(
            settings = settings,
            app = app,
            onDismiss = { showMotionDemoDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveMotionDemoDialog(
    settings: IslandSettings,
    app: DynamicIslandApp,
    onDismiss: () -> Unit
) {
    var demoStage by remember { mutableIntStateOf(0) } // 0=Idle, 1=Compact, 2=Medium, 3=Expanded, 4=Split+1
    val style = if (settings.reducedMotion) AnimationStyle.NONE else settings.animationStyle
    val targetScaleX = when (demoStage) {
        0 -> 0.34f
        1 -> 0.72f
        2 -> 0.90f
        3 -> 1.00f
        else -> 0.84f
    }
    val targetScaleY = when (demoStage) {
        0 -> 0.38f
        1 -> 0.44f
        2 -> 0.68f
        3 -> 1.00f
        else -> 0.44f
    }
    val animScaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = MotionTokens.openingEffectSpec(settings.openingEffect, style, settings.animationElasticity, settings.animationSpeed),
        label = "demo_sx"
    )
    val animScaleY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = MotionTokens.openingEffectSpec(settings.openingEffect, style, settings.animationElasticity, settings.animationSpeed),
        label = "demo_sy"
    )
    var demoEvent by remember { mutableStateOf("Music · Starlight") }
    var demoColor by remember { mutableStateOf(IslandColors.AccentOrange) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Try It · ${style.label} (${settings.openingEffect.label})") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0A0A0E)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                            .fillMaxWidth()
                            .height(112.dp)
                            .graphicsLayer {
                                scaleX = animScaleX
                                scaleY = animScaleY
                            }
                            .clip(RoundedCornerShape(36.dp))
                            .background(Color(0xFF171923))
                            .border(1.dp, demoColor.copy(alpha = 0.72f), RoundedCornerShape(36.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(targetState = demoStage to demoEvent, label = "demo_text") { (st, ev) ->
                            val label = when (st) {
                                0 -> "Idle Ring"
                                1 -> "Compact · $ev"
                                2 -> "Stage 1 · $ev"
                                3 -> "Stage 2 · Expanded"
                                else -> "Split Bubble +1"
                            }
                            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "Idle",
                        1 to "Compact",
                        2 to "Stage 1",
                        3 to "Stage 2",
                        4 to "Split +1"
                    ).forEach { (st, title) ->
                        FilterChip(
                            selected = demoStage == st,
                            onClick = {
                                if (st > demoStage) app.soundManager.playExpand() else app.soundManager.playCollapse()
                                demoStage = st
                            },
                            label = { Text(title, fontSize = 11.sp) }
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("Music", "Music · Starlight", IslandColors.AccentPurple),
                        Triple("Timer", "Timer · 04:30", IslandColors.AccentOrange),
                        Triple("Charging", "Charging · 75%", IslandColors.AccentGreen),
                        Triple("Low Bat", "Low Battery · 15%", IslandColors.AccentRed),
                        Triple("Notif", "Message · Alex", IslandColors.AccentCyan)
                    ).forEach { (chipLabel, evText, color) ->
                        FilterChip(
                            selected = demoEvent == evText,
                            onClick = {
                                app.soundManager.playTap()
                                demoEvent = evText
                                demoColor = color
                                if (demoStage == 0) demoStage = 1
                            },
                            label = { Text(chipLabel, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    helper: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val app = LocalContext.current.applicationContext as? DynamicIslandApp
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(helper, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { next ->
                    app?.soundManager?.playToggle(isOn = next)
                    onCheckedChange(next)
                }
            )
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    helper: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(helper, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun <T> EnumPickerRow(
    title: String,
    helper: String,
    selectedLabel: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(helper, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(selectedLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (label, value) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onSelect(value)
                        }
                    )
                }
            }
        }
    }
}
