package com.example.setup

import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.system.EventGroup
import com.example.system.EventKind
import com.example.system.ExpandedSwipeAction
import com.example.system.HapticsStrength
import com.example.system.IdleWidgetType
import com.example.system.IslandGesture
import com.example.system.IslandSettings
import com.example.system.LockScreenPreset
import com.example.system.MiniCapsuleSize
import com.example.system.OpeningEffect
import com.example.ui.theme.LocalAppBackdrop
import com.example.system.PillSizePreset
import com.example.system.PillStyle
import com.example.system.PillSwipeAction
import com.example.system.WhenIdleMode
import com.example.ui.theme.CrimsonPillButton
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MockupScreenHeader
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import com.example.ui.theme.islandGlassChrome
import kotlinx.coroutines.launch

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
    var motionSubSection by remember { mutableIntStateOf(0) }
    var backupText by remember { mutableStateOf("") }
    var showAppMultiPickerFor by remember { mutableStateOf<String?>(null) }

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
                Toast.makeText(context, "Screenshots folder linked for 10-min Auto-Shelf", Toast.LENGTH_SHORT).show()
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
                title = group.title,
                subtitle = group.subtitle,
                icon = group.icon,
                onBack = onBack,
                backTestTag = "settings_detail_back"
            )

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .testTag("settings_detail_list")
            ) {
                when (group) {
                    SettingsGroup.LOOK -> {
                        val lookAccent = IslandColors.resolveAccentColor(context, settings.accentTheme)
                        item {
                            ObsidianGlassCard(accentColor = lookAccent) {
                                SettingsSectionHeader("Pill Style & Live Preview")
                                Spacer(Modifier.height(6.dp))
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    PillStyle.entries.forEachIndexed { idx, style ->
                                        SegmentedButton(
                                            selected = settings.pillStyle == style,
                                            onClick = {
                                                app.soundManager.playTap()
                                                scope.launch { app.settingsRepository.update { it.copy(pillStyle = style) } }
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(idx, PillStyle.entries.size),
                                            modifier = Modifier.testTag("look_style_${style.name.lowercase()}")
                                        ) {
                                            Text(style.label, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                // Live mini-island preview over a luminous wallpaper strip so glass translucency is clearly visible
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    lookAccent.copy(alpha = 0.45f),
                                                    Color(0xFF1B2336),
                                                    lookAccent.copy(alpha = 0.60f),
                                                    Color(0xFF171E2E)
                                                )
                                            )
                                        )
                                        .padding(8.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        PillStyle.entries.forEach { style ->
                                            val selected = settings.pillStyle == style
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .islandGlassChrome(
                                                        shape = CircleShape,
                                                        pillStyle = style,
                                                        glassDensity = settings.glassDensity,
                                                        reduceGlass = settings.reduceGlass,
                                                        solidFallback = false,
                                                        accentColor = lookAccent,
                                                        isHighlighted = selected,
                                                        backdrop = LocalAppBackdrop.current
                                                    )
                                                    .clickable {
                                                        app.soundManager.playTap()
                                                        scope.launch { app.settingsRepository.update { it.copy(pillStyle = style) } }
                                                    }
                                                    .padding(horizontal = 8.dp)
                                                    .testTag("look_preview_${style.name.lowercase()}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(9.dp)
                                                            .clip(CircleShape)
                                                            .background(if (selected) lookAccent else Color.White.copy(alpha = 0.30f))
                                                    )
                                                    Text(
                                                        text = style.label,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            ObsidianGlassCard(accentColor = lookAccent) {
                                SettingsSectionHeader("Pill Size")
                                Spacer(Modifier.height(6.dp))
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    PillSizePreset.entries.forEachIndexed { idx, preset ->
                                        SegmentedButton(
                                            selected = settings.pillSizePreset == preset,
                                            onClick = {
                                                app.soundManager.playTap()
                                                scope.launch { app.settingsRepository.update { it.copy(pillSizePreset = preset) } }
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(idx, PillSizePreset.entries.size),
                                            modifier = Modifier.testTag("look_size_${preset.name.lowercase()}")
                                        ) {
                                            Text(preset.label, fontSize = 12.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            ObsidianGlassCard(accentColor = lookAccent) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Glass Density", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (settings.glassDensity < 0.35f) "Clear" else if (settings.glassDensity > 0.72f) "Tinted" else "Balanced",
                                        color = lookAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = settings.glassDensity,
                                    onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(glassDensity = v) } } },
                                    enabled = settings.pillStyle != PillStyle.BLACK && !settings.solidFallback,
                                    colors = SliderDefaults.colors(
                                        thumbColor = lookAccent,
                                        activeTrackColor = lookAccent
                                    )
                                )
                            }
                        }
                        item {
                            SwitchRow(
                                title = "Reduce Glass",
                                helper = "Softer tint and lighter specular highlights for low power & clarity.",
                                checked = settings.reduceGlass,
                                onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(reduceGlass = c) } } }
                            )
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
                            ObsidianGlassCard(accentColor = lookAccent) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SettingsSectionHeader("Accent Colour")
                                    Text(settings.accentTheme.label, color = lookAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AccentTheme.entries.forEach { theme ->
                                        val swatchColor = if (theme == AccentTheme.DYNAMIC) {
                                            IslandColors.extractMaterialYouAccent(context)
                                        } else {
                                            Color(theme.swatchHex)
                                        }
                                        val active = settings.accentTheme == theme
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    app.soundManager.playTap()
                                                    scope.launch { app.settingsRepository.update { it.copy(accentTheme = theme) } }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .testTag("accent_swatch_${theme.name.lowercase()}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchColor)
                                                    .border(
                                                        width = if (active) 2.5.dp else 1.dp,
                                                        color = if (active) Color.White else Color.White.copy(alpha = 0.28f),
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                if (active) {
                                                    Icon(Icons.Filled.Check, contentDescription = theme.label, tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Text(
                                                text = theme.label,
                                                color = if (active) Color.White else IslandColors.TextSecondary,
                                                fontSize = 10.sp,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SwitchRow(
                                title = "Rotate Vinyl Album Art",
                                helper = "Gently spin album art in Expanded Music while audio plays.",
                                checked = settings.rotateAlbumArt,
                                onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(rotateAlbumArt = c) } } }
                            )
                        }
                        item {
                            SwitchRow(
                                title = "Purple Edge Sidebar Tint",
                                helper = "Apply subtle purple accent to the curved floating-apps sidebar.",
                                checked = settings.purpleSidebarTheme,
                                onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(purpleSidebarTheme = c) } } }
                            )
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
                                ObsidianGlassCard {
                                    SettingsSectionHeader("Background & Surface")
                                    Spacer(Modifier.height(6.dp))
                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                        PillStyle.entries.forEachIndexed { idx, style ->
                                            SegmentedButton(
                                                selected = settings.pillStyle == style,
                                                onClick = { scope.launch { app.settingsRepository.update { it.copy(pillStyle = style) } } },
                                                shape = SegmentedButtonDefaults.itemShape(idx, PillStyle.entries.size)
                                            ) {
                                                Text(style.label, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Glass Density", color = Color.White, fontWeight = FontWeight.Medium)
                                        Text("${(settings.glassDensity * 100).toInt()}%", color = IslandColors.CrimsonBright)
                                    }
                                    Slider(
                                        value = settings.glassDensity,
                                        onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(glassDensity = v) } } },
                                        enabled = settings.pillStyle != PillStyle.BLACK && !settings.solidFallback
                                    )
                                }
                            }
                        }

                        if (motionSubSection == 0 || motionSubSection == 2) {
                            item {
                                ObsidianGlassCard {
                                    SettingsSectionHeader("Motion Style (Springy / Standard / Calm)")
                                    Spacer(Modifier.height(6.dp))
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
                            }
                            item {
                                ObsidianGlassCard {
                                    SettingsSectionHeader("Capsule Opening Effect")
                                    Spacer(Modifier.height(6.dp))
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
                            }
                            item {
                                ObsidianGlassCard {
                                    SettingsSectionHeader("Inner Content Effect")
                                    Spacer(Modifier.height(6.dp))
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
                            }
                            item {
                                ObsidianGlassCard {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Speed", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                Text("${String.format("%.1f", settings.animationSpeed)}x", color = IslandColors.CrimsonBright, fontSize = 12.sp)
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
                                                Text("Elasticity", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                Text("${(settings.animationElasticity * 100).toInt()}%", color = IslandColors.CrimsonBright, fontSize = 12.sp)
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
                            ObsidianGlassCard {
                                SettingsSectionHeader("Haptics Strength")
                                Spacer(Modifier.height(6.dp))
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
                            ObsidianGlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Sound Effects", color = Color.White, fontWeight = FontWeight.Medium)
                                        Text(
                                            "Short UI chimes for expand, collapse, taps, timer finish, and switches.",
                                            color = IslandColors.TextSecondary,
                                            fontSize = 12.sp
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
                                        colors = crimsonSwitchColors(),
                                        modifier = Modifier.testTag("motion_sound_switch")
                                    )
                                }
                            }
                        }
                        item {
                            ObsidianGlassCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sound Volume", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text("${(settings.soundVolume * 100).toInt()}%", color = IslandColors.CrimsonBright)
                                }
                                Slider(
                                    value = settings.soundVolume,
                                    valueRange = 0f..1f,
                                    enabled = settings.soundEnabled,
                                    onValueChange = { v ->
                                        app.soundManager.updateCachedAudioSettings(settings.soundEnabled, v)
                                        scope.launch { app.settingsRepository.update { it.copy(soundVolume = v) } }
                                    },
                                    onValueChangeFinished = { app.soundManager.playTap() },
                                    modifier = Modifier.testTag("motion_sound_volume_slider")
                                )
                            }
                        }
                        item {
                            CrimsonPillButton(
                                text = "Try It · Interactive Motion Demo",
                                icon = Icons.Filled.PlayArrow,
                                onClick = {
                                    app.soundManager.playExpand()
                                    showMotionDemoDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().testTag("motion_try_it_btn")
                            )
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
                            ObsidianGlassCard {
                                SettingsSectionHeader("When Idle Mode")
                                Spacer(Modifier.height(6.dp))
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    WhenIdleMode.entries.forEachIndexed { idx, mode ->
                                        SegmentedButton(
                                            selected = settings.whenIdleMode == mode,
                                            onClick = {
                                                app.soundManager.playTap()
                                                scope.launch { app.settingsRepository.update { it.copy(whenIdleMode = mode) } }
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(idx, WhenIdleMode.entries.size),
                                            modifier = Modifier.testTag("pill_idle_mode_${mode.name.lowercase()}")
                                        ) {
                                            Text(mode.label, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                        if (settings.whenIdleMode == WhenIdleMode.INFO_PILL) {
                            item {
                                EnumPickerRow(
                                    title = "Left Idle Slot Widget",
                                    helper = "Quiet readout left of the camera in Info Pill mode.",
                                    selectedLabel = settings.idleLeftWidget.label,
                                    options = IdleWidgetType.entries.map { it.label to it },
                                    onSelect = { w -> scope.launch { app.settingsRepository.update { it.copy(idleLeftWidget = w) } } }
                                )
                            }
                            item {
                                EnumPickerRow(
                                    title = "Right Idle Slot Widget",
                                    helper = "Quiet readout right of the camera in Info Pill mode.",
                                    selectedLabel = settings.idleRightWidget.label,
                                    options = IdleWidgetType.entries.map { it.label to it },
                                    onSelect = { w -> scope.launch { app.settingsRepository.update { it.copy(idleRightWidget = w) } } }
                                )
                            }
                        }
                        item {
                            ObsidianGlassCard {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Alerts Dwell Timer", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text("${String.format("%.1f", settings.alertsDwellMs / 1000f)} s", color = IslandColors.CrimsonBright)
                                }
                                Slider(
                                    value = settings.alertsDwellMs.toFloat().coerceIn(1500f, 10000f),
                                    valueRange = 1500f..10000f,
                                    steps = 16,
                                    onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(alertsDwellMs = v.toLong()) } } },
                                    modifier = Modifier.testTag("pill_alerts_dwell_slider")
                                )
                            }
                        }
                        item {
                            ObsidianGlassCard {
                                SettingsSectionHeader("Split Companion Circle Side")
                                Spacer(Modifier.height(6.dp))
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
                        }
                        item {
                            ObsidianGlassCard {
                                SettingsSectionHeader("Mini Capsule")
                                Spacer(Modifier.height(6.dp))
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    MiniCapsuleSize.entries.forEachIndexed { idx, sz ->
                                        SegmentedButton(
                                            selected = settings.miniCapsuleSize == sz,
                                            onClick = {
                                                app.soundManager.playTap()
                                                scope.launch { app.settingsRepository.update { it.copy(miniCapsuleSize = sz) } }
                                            },
                                            shape = SegmentedButtonDefaults.itemShape(idx, MiniCapsuleSize.entries.size)
                                        ) {
                                            Text(sz.label, fontSize = 11.sp, maxLines = 1)
                                        }
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
                                title = "Lock-Screen Preset",
                                helper = "Controls island visibility and text redaction while locked.",
                                selectedLabel = settings.lockScreenPreset.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                options = LockScreenPreset.entries.map { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } to it },
                                onSelect = { p -> scope.launch { app.settingsRepository.update { it.copy(lockScreenPreset = p) } } }
                            )
                        }
                        item {
                            SwitchRow(
                                title = "Reels / Shorts Shelf Shortcut",
                                helper = "Show a quick Save to Shelf pill when watching Instagram, TikTok, or YouTube.",
                                checked = settings.reelOverlayEnabled,
                                onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(reelOverlayEnabled = c) } } }
                            )
                        }
                        item {
                            SwitchRow(
                                title = "Swipe Up to Dismiss",
                                helper = "Swipe up on the compact or expanded island to dismiss alerts or collapse.",
                                checked = settings.swipeUpDismissEnabled,
                                onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(swipeUpDismissEnabled = c) } } }
                            )
                        }
                        item {
                            SwitchRow(
                                title = "Hide Notification When App Is Open",
                                helper = "Suppress banner pop-ups from whichever app is already open on screen.",
                                checked = settings.hideNotifInForegroundApp,
                                onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(hideNotifInForegroundApp = c) } } }
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
                            SwitchRow("Search Launcher", "Apps, pinned apps, contacts, notes & Google handoff", settings.searchEnabled) { c ->
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
                            SwitchRow("10-Minute Photo Shelf", "Auto-expiring screenshots & shared images", settings.vaultEnabled) { c ->
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
                        item { SettingsSectionHeader("Event Group Master Switches") }
                        items(EventGroup.entries, key = { "grp_${it.name}" }) { grp ->
                            SwitchRow(
                                title = "${grp.label} Events",
                                helper = "Master switch for all ${grp.label.lowercase()} events in this group.",
                                checked = settings.isGroupEnabled(grp),
                                onCheckedChange = { next ->
                                    scope.launch { app.settingsRepository.setEventGroupEnabled(grp, next) }
                                }
                            )
                        }
                        item { SettingsSectionHeader("Per-Event Behavior Switches") }
                        items(EventKind.entries, key = { it.name }) { kind ->
                            val cfg = settings.eventConfig(kind)
                            ObsidianGlassCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(kind.label, color = Color.White, fontWeight = FontWeight.Medium)
                                        Text(kind.subtitle, color = IslandColors.TextSecondary, fontSize = 12.sp)
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
                                        colors = crimsonSwitchColors(),
                                        modifier = Modifier.testTag("event_enable_${kind.name.lowercase()}")
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text("Auto-expand when event arrives", fontSize = 12.sp, color = IslandColors.TextSecondary)
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
                                        colors = crimsonSwitchColors(),
                                        modifier = Modifier.testTag("event_autoexpand_${kind.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    SettingsGroup.PRIVACY -> {
                        item {
                            ObsidianGlassCard(highlighted = settings.screenReadingEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Screen Reading / OCR (Select Mode)", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(
                                            "OFF by default. Reads ONLY the exact node you long-press in Select Mode. Never reads passwords or FLAG_SECURE.",
                                            color = IslandColors.TextSecondary,
                                            fontSize = 12.sp
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
                                        colors = crimsonSwitchColors(),
                                        modifier = Modifier.testTag("privacy_screen_reading_switch")
                                    )
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
                                title = "3-Finger Screenshot Auto-Shelf",
                                helper = "Auto-store new screenshots in app-private Shelf for 10 min (24h Trash).",
                                checked = settings.vaultAutoCapture3Finger,
                                onCheckedChange = { c -> scope.launch { app.settingsRepository.update { it.copy(vaultAutoCapture3Finger = c) } } }
                            )
                        }
                        item {
                            ActionRow(
                                title = "Screenshots Folder (SAF Access)",
                                helper = if (settings.vaultScreenshotsTreeUri.isNullOrBlank()) {
                                    "Not linked yet · Tap to select DCIM/Screenshots for automatic 10m Shelf capture"
                                } else {
                                    "Linked · Automatic screenshot Shelf capture active"
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
                            ObsidianGlassCard(highlighted = true, accentColor = IslandColors.AccentGreen) {
                                Text(
                                    text = "On-Device Privacy Guarantee: Dynamic Island declares zero INTERNET permission. Notes, Shelf photos, memos, clips, and OCR stay strictly on your phone.",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    SettingsGroup.ABOUT -> {
                        item {
                            var permSnap by remember { mutableStateOf(readPermissionStatus(context)) }
                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        permSnap = readPermissionStatus(context)
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }
                            val islandStateDiag by app.islandController.state.collectAsState()
                            val diag = islandStateDiag.diagnostics
                            ObsidianGlassCard {
                                Text("Dynamic Island v${BuildConfig.VERSION_NAME}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Application ID: ${BuildConfig.APPLICATION_ID}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.14f))
                                Text("Live Permissions Status:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("• Accessibility Overlay: ${if (permSnap.accessibilityGranted) "Granted" else "Not granted"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Draw Over Other Apps: ${if (permSnap.overlayGranted) "Granted" else "Not granted"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Notification Listener: ${if (permSnap.notificationListenerGranted) "Granted" else "Not granted"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Notifications (Android 13): ${if (permSnap.postNotificationsGranted) "Granted" else "Not granted"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Do Not Disturb Policy: ${if (permSnap.dndPolicyGranted) "Granted" else "Not granted"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Microphone: ${if (permSnap.microphoneGranted) "Granted" else "In-context"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Exact Alarms: ${if (permSnap.exactAlarmGranted) "Granted" else "Not granted"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Battery Unrestricted: ${if (permSnap.batteryUnrestricted) "Granted" else "Restricted"}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.14f))
                                Text("Runtime Diagnostics:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("• Last Notification: ${diag.lastNotificationApp}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Last Suppression Reason: ${diag.lastSuppressionReason}", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                Text("• Dead-Zone Collisions: ${diag.deadZoneCollisionCount}", color = IslandColors.TextSecondary, fontSize = 12.sp)
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
                            Text("Danger Zone", color = IslandColors.CrimsonBright, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        item {
                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                shape = CircleShape,
                                border = BorderStroke(1.dp, IslandColors.CrimsonBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IslandColors.CrimsonBright),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Reset Settings to Default")
                            }
                        }
                        item {
                            Button(
                                onClick = { showDeleteAllConfirm = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("danger_delete_all_btn")
                            ) {
                                Text("Delete All App Data")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOcrDisclosure) {
        AlertDialog(
            onDismissRequest = { showOcrDisclosure = false },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(OcrPrivacyGate.DISCLOSURE_TITLE) },
            text = { Text(OcrPrivacyGate.DISCLOSURE_BODY) },
            confirmButton = {
                Button(
                    onClick = {
                        showOcrDisclosure = false
                        app.soundManager.playToggle(isOn = true)
                        scope.launch { app.settingsRepository.update { it.copy(screenReadingEnabled = true) } }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)
                ) { Text("I Understand, Turn ON") }
            },
            dismissButton = { TextButton(onClick = { showOcrDisclosure = false }) { Text("Cancel") } }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Reset Settings?") },
            text = { Text("Restores Look, Motion, Pill calibration, and Privacy toggles to defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        scope.launch { app.settingsRepository.resetSettings() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Delete All App Data?") },
            text = { Text("Permanently erases all Shelf photos, Notes, Voice Memos, Clipboard history, Notification inbox, and Settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllConfirm = false
                        scope.launch {
                            app.deleteAllUserData()
                            Toast.makeText(context, "All app data deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Backup / Restore Settings JSON") },
            text = {
                OutlinedTextField(
                    value = backupText,
                    onValueChange = { backupText = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = app.settingsRepository.importBackupJson(backupText)
                            Toast.makeText(context, if (res.isSuccess) "Settings restored" else "Invalid JSON", Toast.LENGTH_SHORT).show()
                            showBackupDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)
                ) { Text("Restore JSON") }
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
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
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
                            Text(item.label, color = Color.White, modifier = Modifier.weight(1f))
                            if (isChecked) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = IslandColors.CrimsonBright)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAppMultiPickerFor = null },
                    colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)
                ) { Text("Done") }
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
    var demoStage by remember { mutableIntStateOf(0) }
    val style = if (settings.reducedMotion) AnimationStyle.NONE else settings.animationStyle
    val targetScaleX = when (demoStage) { 0 -> 0.34f; 1 -> 0.72f; 2 -> 0.90f; 3 -> 1.00f; else -> 0.84f }
    val targetScaleY = when (demoStage) { 0 -> 0.38f; 1 -> 0.44f; 2 -> 0.68f; 3 -> 1.00f; else -> 0.44f }
    val animScaleXState = animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = MotionTokens.openingEffectSpec(settings.openingEffect, style, settings.animationElasticity, settings.animationSpeed),
        label = "demo_sx"
    )
    val animScaleYState = animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = MotionTokens.openingEffectSpec(settings.openingEffect, style, settings.animationElasticity, settings.animationSpeed),
        label = "demo_sy"
    )
    var demoEvent by remember { mutableStateOf("Music · Starlight") }
    var demoColor by remember { mutableStateOf(IslandColors.CrimsonBright) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IslandColors.ObsidianCardElevated,
        titleContentColor = Color.White,
        textContentColor = Color.White,
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
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                            .height(92.dp)
                            .graphicsLayer { scaleX = animScaleXState.value; scaleY = animScaleYState.value }
                            .clip(RoundedCornerShape(36.dp))
                            .background(Color(0xFF1A0D10))
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Idle", 1 to "Compact", 2 to "Stage 1", 3 to "Stage 2", 4 to "Split +1").forEach { (st, title) ->
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple("Music", "Music · Starlight", IslandColors.CrimsonBright),
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
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = IslandColors.CrimsonPrimary)
            ) { Text("Close") }
        }
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Box(Modifier.width(3.dp).height(14.dp).background(IslandColors.CrimsonBright, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
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
    ObsidianGlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(helper, color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { next ->
                    app?.soundManager?.playToggle(isOn = next)
                    onCheckedChange(next)
                },
                colors = crimsonSwitchColors()
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
    ObsidianGlassCard(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(helper, color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = IslandColors.TextSecondary)
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
    ObsidianGlassCard(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(helper, color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
            Text(selectedLabel, color = IslandColors.CrimsonBright, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
