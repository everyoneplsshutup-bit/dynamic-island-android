package com.example.setup

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.DynamicIslandApp
import com.example.system.AccentTheme
import com.example.system.IslandSettings
import com.example.system.PillSizePreset
import com.example.system.PillStyle
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import com.example.ui.theme.islandGlassChrome
import kotlinx.coroutines.launch

enum class SettingsGroup(val title: String, val subtitle: String, val icon: ImageVector) {
    LOOK("Look", "Pill style, glass density, energy ring, privacy dots & theme", Icons.Filled.Palette),
    MOTION("Motion", "Animation speed, 4-way gestures, haptics & sound", Icons.Filled.Animation),
    MEDIA("Music & Lyrics", "LRCLIB lyrics, timing offset & expanded marquee", Icons.Filled.MusicNote),
    PILL("Pill", "Camera calibration, idle mode, dwell time & lock screen", Icons.Filled.RadioButtonChecked),
    FEATURES("Features", "Master toggles, Customise Island, Calendar & channels", Icons.Filled.Tune),
    PRIVACY("Privacy", "Screen reading / OCR (off by default), Shelf & clipboard", Icons.Filled.Security),
    ABOUT("About / Danger Zone", "Diagnostics, permissions status, backup & reset", Icons.Filled.Info)
}

private data class SettingsSearchEntry(
    val title: String,
    val subtext: String,
    val breadcrumb: String,
    val group: SettingsGroup
)

private val settingsIndex = listOf(
    SettingsSearchEntry("Pill Style", "Dark or Apple Liquid", "Look > Style", SettingsGroup.LOOK),
    SettingsSearchEntry("Pill Size", "Small, Medium, or Large slot width", "Look > Size", SettingsGroup.LOOK),
    SettingsSearchEntry("Energy Ring", "Arc battery indicator hugging the punch-hole camera", "Look > Energy Ring", SettingsGroup.LOOK),
    SettingsSearchEntry("Privacy Dots", "Live Camera, Microphone & Location dots beside island", "Look > Privacy Dots", SettingsGroup.LOOK),
    SettingsSearchEntry("Glass Density", "Clear to Tinted density slider", "Look > Glass", SettingsGroup.LOOK),
    SettingsSearchEntry("Reduce Glass", "Soften translucent blur & specular glow", "Look > Glass", SettingsGroup.LOOK),
    SettingsSearchEntry("Solid Fallback", "Force solid black over glass", "Look > Fallback", SettingsGroup.LOOK),
    SettingsSearchEntry("Performance Mode", "Lower glass overhead for maximum 120 Hz smoothness", "Look > Performance", SettingsGroup.LOOK),
    SettingsSearchEntry("Match app colour", "Tint notification glow pill with app icon accent", "Look > Accent", SettingsGroup.LOOK),
    SettingsSearchEntry("MIUI Blur / Advanced Textures", "Check live cross-window blur status", "Look > Blur", SettingsGroup.LOOK),
    SettingsSearchEntry("Accent Colour", "Blue, Purple, Cyan, Amber, Coral, Mint, or Auto", "Look > Accent", SettingsGroup.LOOK),
    SettingsSearchEntry("Rotate Album Art", "Spin vinyl album art when music plays", "Look > Music", SettingsGroup.LOOK),
    SettingsSearchEntry("Animation Style", "Springy, Standard, Calm, or Off", "Motion > Style", SettingsGroup.MOTION),
    SettingsSearchEntry("Animation Speed", "Adjust spring transition speed", "Motion > Speed", SettingsGroup.MOTION),
    SettingsSearchEntry("Elasticity", "Adjust overshoot bounce intensity", "Motion > Elasticity", SettingsGroup.MOTION),
    SettingsSearchEntry("Metaball Glass Effect", "Liquid RenderEffect blur+threshold during morphs", "Motion > Metaball", SettingsGroup.MOTION),
    SettingsSearchEntry("4-Way Pill Gestures & Sensitivity", "Left, Right, Up, Down swipes & Gesture Playground", "Motion > Gestures", SettingsGroup.MOTION),
    SettingsSearchEntry("Two-Stage Open", "Compact pill -> Stage 1 preview -> Full card", "Motion > Stages", SettingsGroup.MOTION),
    SettingsSearchEntry("Try It Motion Demo", "Interactive capsule animation playground", "Motion > Demo", SettingsGroup.MOTION),
    SettingsSearchEntry("Reduced Motion", "Calm fallback for sensitive eyes", "Motion > Accessibility", SettingsGroup.MOTION),
    SettingsSearchEntry("Haptics Strength", "Off, Light tick, or Medium click", "Motion > Haptics", SettingsGroup.MOTION),
    SettingsSearchEntry("Sound Effects", "Enable or mute Dynamic Island UI sound effects", "Motion > Sound", SettingsGroup.MOTION),
    SettingsSearchEntry("Extra Morph Sound", "Optional subtle swoosh on pill morphs (default OFF)", "Motion > Sound", SettingsGroup.MOTION),
    SettingsSearchEntry("Sound Volume", "Adjust UI sound effects volume", "Motion > Sound", SettingsGroup.MOTION),
    SettingsSearchEntry("Lyrics download from LRCLIB (lrclib.net)", "Online synced lyrics fallback (disabled offline)", "Music & Lyrics > Lyrics", SettingsGroup.MEDIA),
    SettingsSearchEntry("Lyrics Timing Offset", "Nudge synced lyrics -5s to +5s", "Music & Lyrics > Lyrics", SettingsGroup.MEDIA),
    SettingsSearchEntry("Scroll Long Song Titles", "Opt-in marquee inside expanded music card", "Music & Lyrics > Music", SettingsGroup.MEDIA),
    SettingsSearchEntry("Camera Calibration", "Nudge X, Y, width, and height around punch-hole", "Pill > Calibration", SettingsGroup.PILL),
    SettingsSearchEntry("Companion Circle Side", "Place +1 split bubble on Left or Right", "Pill > Split", SettingsGroup.PILL),
    SettingsSearchEntry("When Idle Mode", "Circle Ring or Hidden", "Pill > Idle", SettingsGroup.PILL),
    SettingsSearchEntry("Alerts Dwell Timer", "Adjust compact notification dwell 1.5s to 10s", "Pill > Alerts", SettingsGroup.PILL),
    SettingsSearchEntry("Compact Pill Content", "Auto, Music, Timer, Battery, or Notifications", "Pill > Compact", SettingsGroup.PILL),
    SettingsSearchEntry("Lock-Screen Preset", "Minimal, Hide Message Text, or Hidden", "Pill > Lock Screen", SettingsGroup.PILL),
    SettingsSearchEntry("Per-Event Switches", "Group & per-event auto-expand behavior", "Features > Events", SettingsGroup.FEATURES),
    SettingsSearchEntry("Customise Island", "Pick & reorder up to 8 island features", "Features > Chooser", SettingsGroup.FEATURES),
    SettingsSearchEntry("Master Feature Toggles", "Enable/disable Music, Timer, Search, Shelf, OCR & more", "Features > Toggles", SettingsGroup.FEATURES),
    SettingsSearchEntry("Calendar Reminders", "Show next 12h meeting & Join link in expanded island", "Features > Calendar", SettingsGroup.FEATURES),
    SettingsSearchEntry("Landscape Quiet Mode", "Silence island notifications during full-screen landscape", "Features > Orientation", SettingsGroup.FEATURES),
    SettingsSearchEntry("Auto-Rotate Suggestion", "Show rotate pill when rotating device with rotation locked", "Features > Orientation", SettingsGroup.FEATURES),
    SettingsSearchEntry("Reset Muted Channels", "Unmute notification channels muted from island actions", "Features > Notifications", SettingsGroup.FEATURES),
    SettingsSearchEntry("Pinned Quick Launch Apps", "Choose shortcuts shown in Tools & Search", "Features > Shortcuts", SettingsGroup.FEATURES),
    SettingsSearchEntry("Screen Reading / OCR", "Strictly opt-in, OFF by default", "Privacy > Screen Reading", SettingsGroup.PRIVACY),
    SettingsSearchEntry("OCR Tier B (ML Kit)", "Opt-in cropped fallback when Tier A finds no text", "Privacy > OCR Tier B", SettingsGroup.PRIVACY),
    SettingsSearchEntry("Shelf 10-Min Rules", "3-finger screenshot auto-capture & 24h trash", "Privacy > Shelf", SettingsGroup.PRIVACY),
    SettingsSearchEntry("Clipboard History", "Foreground-only capture on Android 13", "Privacy > Clips", SettingsGroup.PRIVACY),
    SettingsSearchEntry("Diagnostics & Permissions", "Service alive, hidden reason & hardware checks", "About > Diagnostics", SettingsGroup.ABOUT),
    SettingsSearchEntry("Backup / Restore Settings", "Export or import settings JSON", "About > Backup", SettingsGroup.ABOUT),
    SettingsSearchEntry("Delete All App Data", "Erase Shelf, Notes, Memos, Inbox & settings", "About > Danger Zone", SettingsGroup.ABOUT)
)

@Composable
fun SettingsHomeScreen(
    app: DynamicIslandApp,
    onBack: () -> Unit,
    onOpenGroup: (SettingsGroup) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val lookAccent = IslandColors.resolveAccentColor(context, settings.accentTheme)
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

    ObsidianCrimsonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            // Centered Mockup Header Bar (<- | Settings | (i))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(IslandColors.ObsidianGlassDark)
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        .testTag("settings_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Settings", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Customize your Dynamic Island experience",
                        color = IslandColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onOpenGroup(SettingsGroup.ABOUT) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(IslandColors.ObsidianCard)
                        .border(1.dp, lookAccent.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "About & Permissions", tint = Color.White)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("settings_home_list")
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search settings...", color = IslandColors.TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search settings", tint = lookAccent) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = IslandColors.ObsidianCardElevated,
                            unfocusedContainerColor = IslandColors.ObsidianCard,
                            focusedBorderColor = lookAccent.copy(alpha = 0.72f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
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
                                color = IslandColors.TextSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    } else {
                        items(matchingEntries) { entry ->
                            ObsidianGlassCard(onClick = { onOpenGroup(entry.group) }) {
                                Text(entry.breadcrumb, color = lookAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(entry.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(entry.subtext, color = IslandColors.TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Master Dynamic Island Overlay Toggle Card
                    item {
                        ObsidianGlassCard(highlighted = settings.islandEnabled, accentColor = lookAccent) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(IslandColors.ObsidianCardElevated)
                                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(11.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                            .border(1.8.dp, lookAccent, CircleShape)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dynamic Island", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("v${BuildConfig.VERSION_NAME} · On-Device + Optional LRCLIB", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = settings.islandEnabled,
                                    onCheckedChange = { enabled ->
                                        app.soundManager.playToggle(enabled)
                                        scope.launch { app.settingsRepository.update { it.copy(islandEnabled = enabled) } }
                                    },
                                    colors = crimsonSwitchColors(),
                                    modifier = Modifier.testTag("settings_island_master_switch")
                                )
                            }
                        }
                    }

                    // Inline Look Quick-Card (Two Looks ONLY: Dark / Apple Liquid + Accent Colour Swatches + Pill Size)
                    item {
                        ObsidianGlassCard(accentColor = lookAccent) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(3.dp).height(18.dp).background(lookAccent, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Look", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Style, translucency & accent colour", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                PillStyle.entries.forEach { style ->
                                    val selected = settings.pillStyle == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(92.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF141821),
                                                        lookAccent.copy(alpha = 0.28f),
                                                        Color(0xFF121018)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) lookAccent else Color.White.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                app.soundManager.playTap()
                                                scope.launch { app.settingsRepository.update { it.copy(pillStyle = style) } }
                                            }
                                            .padding(6.dp)
                                    ) {
                                        // Luminous streak behind mini pill preview so Liquid Glass translucency is immediately visible
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .width(46.dp)
                                                .height(14.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(lookAccent.copy(alpha = 0.85f), Color.White.copy(alpha = 0.65f))
                                                    )
                                                )
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF0A0A0D)))
                                            Box(
                                                modifier = Modifier
                                                    .width(64.dp)
                                                    .height(24.dp)
                                                    .islandGlassChrome(
                                                        shape = CircleShape,
                                                        pillStyle = style,
                                                        glassDensity = settings.glassDensity,
                                                        reduceGlass = settings.reduceGlass,
                                                        solidFallback = false,
                                                        accentColor = lookAccent,
                                                        isHighlighted = selected,
                                                        backdrop = com.example.ui.theme.LocalAppBackdrop.current
                                                    )
                                            )
                                            Text(
                                                text = style.label,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                        if (selected) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(lookAccent)
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Accent Colour", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(settings.accentTheme.label, color = lookAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
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
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                app.soundManager.playTap()
                                                scope.launch { app.settingsRepository.update { it.copy(accentTheme = theme) } }
                                            }
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                            .testTag("home_accent_swatch_${theme.name.lowercase()}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(
                                                    width = if (active) 2.2.dp else 1.dp,
                                                    color = if (active) Color.White else Color.White.copy(alpha = 0.25f),
                                                    shape = CircleShape
                                                )
                                        ) {
                                            if (active) {
                                                Icon(Icons.Filled.Check, contentDescription = theme.label, tint = Color.White, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                        Text(
                                            text = theme.label,
                                            color = if (active) Color.White else IslandColors.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    CrimsonSquircleIcon(icon = Icons.Filled.Widgets, size = 34.dp, iconSize = 17.dp, tint = lookAccent)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Pill Size", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Adjust size of Dynamic Island", color = IslandColors.TextSecondary, fontSize = 11.sp)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(IslandColors.ObsidianCard)
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                        .padding(3.dp)
                                ) {
                                    PillSizePreset.entries.forEach { preset ->
                                        val selected = settings.pillSizePreset == preset
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (selected) lookAccent else Color.Transparent)
                                                .clickable {
                                                    app.soundManager.playTap()
                                                    scope.launch { app.settingsRepository.update { it.copy(pillSizePreset = preset) } }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                preset.label,
                                                color = if (selected) Color.White else IslandColors.TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Inline Motion Quick-Card
                    item {
                        ObsidianGlassCard(accentColor = lookAccent) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(3.dp).height(18.dp).background(lookAccent, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Motion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Control animations and sound effects", color = IslandColors.TextSecondary, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Animation Speed", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("${String.format("%.1f", settings.animationSpeed)}x", color = lookAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = settings.animationSpeed,
                                valueRange = 0.6f..1.6f,
                                onValueChange = { v -> scope.launch { app.settingsRepository.update { it.copy(animationSpeed = v) } } },
                                colors = SliderDefaults.colors(
                                    thumbColor = lookAccent,
                                    activeTrackColor = lookAccent,
                                    inactiveTrackColor = IslandColors.ObsidianCardElevated
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                CrimsonSquircleIcon(icon = Icons.Filled.VolumeUp, size = 34.dp, iconSize = 17.dp, tint = lookAccent)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Sound Effects", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Play subtle chimes for actions", color = IslandColors.TextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.soundEnabled,
                                    onCheckedChange = { enabled ->
                                        app.soundManager.updateCachedAudioSettings(enabled, settings.soundVolume)
                                        if (enabled) app.soundManager.playToggle(true)
                                        scope.launch { app.settingsRepository.update { it.copy(soundEnabled = enabled) } }
                                    },
                                    colors = crimsonSwitchColors()
                                )
                            }
                        }
                    }

                    // 7 Grouped Settings Sections (Look, Motion, Music/Lyrics/Mochi, Pill, Features, Privacy, About)
                    items(SettingsGroup.entries) { group ->
                        ObsidianGlassCard(
                            highlighted = group == SettingsGroup.ABOUT,
                            accentColor = lookAccent,
                            onClick = { onOpenGroup(group) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_group_${group.name.lowercase()}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                CrimsonSquircleIcon(icon = group.icon, size = 40.dp, iconSize = 20.dp, tint = lookAccent)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(group.subtitle, color = IslandColors.TextSecondary, fontSize = 12.sp)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = IslandColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun crimsonSwitchColors(): androidx.compose.material3.SwitchColors {
    val primary = MaterialTheme.colorScheme.primary
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = primary,
        checkedBorderColor = primary.copy(alpha = 0.75f),
        uncheckedThumbColor = Color(0xFFB5B6BE),
        uncheckedTrackColor = IslandColors.ObsidianCardElevated,
        uncheckedBorderColor = Color.White.copy(alpha = 0.22f)
    )
}
