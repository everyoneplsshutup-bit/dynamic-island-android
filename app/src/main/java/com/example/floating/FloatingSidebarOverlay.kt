package com.example.floating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import com.example.ui.theme.MotionTokens
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.search.LaunchableApp
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.ui.theme.IslandColors
import com.example.ui.theme.LocalAppBackdrop
import com.example.ui.theme.islandGlassChrome

@Composable
fun FloatingSidebarOverlay(
    app: DynamicIslandApp,
    settings: IslandSettings,
    accentColor: Color,
    onClose: () -> Unit
) {
    var allApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var collapsedToRail by remember { mutableStateOf(false) }
    val lastResult by app.floatingWindowManager.lastResult.collectAsState()
    val islandState by app.islandController.state.collectAsState()

    LaunchedEffect(Unit) {
        allApps = app.searchEngine.getInstalledApps()
    }

    val unreadCounts = remember(islandState.notificationsHistory) {
        islandState.notificationsHistory.groupingBy { it.packageName }.eachCount()
    }

    // Uncapped app list when no manual subset configured (deleted .take(8))
    val selectedApps = remember(allApps, settings.floatingAppPackages) {
        if (settings.floatingAppPackages.isEmpty()) {
            allApps
        } else {
            val map = allApps.associateBy { it.packageName }
            settings.floatingAppPackages.mapNotNull { map[it] }.ifEmpty { allApps }
        }
    }

    val sidebarShape = RoundedCornerShape(
        topStart = 28.dp,
        bottomStart = 28.dp,
        topEnd = 8.dp,
        bottomEnd = 8.dp
    )
    val effectiveAccent = if (settings.purpleSidebarTheme) IslandColors.AccentPurple else accentColor
    val bgBrush = if (settings.purpleSidebarTheme) {
        Brush.verticalGradient(listOf(Color(0xF5191026), Color(0xF50B0812)))
    } else {
        null
    }

    AnimatedContent(targetState = collapsedToRail, label = "sidebar_rail_toggle") { isRail ->
        if (isRail) {
            // Compact 56 dp curved rail mode
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .width(56.dp)
                    .islandGlassChrome(
                        shape = sidebarShape,
                        pillStyle = if (settings.solidFallback) com.example.system.PillStyle.BLACK else settings.pillStyle,
                        glassDensity = settings.glassDensity,
                        reduceGlass = settings.reduceGlass,
                        solidFallback = settings.solidFallback,
                        accentColor = effectiveAccent,
                        backdrop = LocalAppBackdrop.current
                    )
                    .let { if (bgBrush != null) it.background(bgBrush) else it }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                IconButton(
                    onClick = { collapsedToRail = false },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Expand sidebar", tint = effectiveAccent)
                }
                selectedApps.take(6).forEach { item ->
                    val unread = unreadCounts[item.packageName] ?: 0
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                app.soundManager.playTap()
                                app.floatingWindowManager.launchAppInFloatingWindow(item.packageName)
                                onClose()
                                app.islandController.collapse()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = item.iconBitmap
                        if (bmp != null) {
                            Image(bitmap = bmp, contentDescription = item.label, modifier = Modifier.size(30.dp).clip(CircleShape))
                        } else {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                        }
                        if (unread > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(effectiveAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(unread.coerceAtMost(9).toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                IconButton(onClick = { app.soundManager.playCollapse(); onClose() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close sidebar", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            // Full 280 dp curved Floating Windows Panel
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .islandGlassChrome(
                        shape = sidebarShape,
                        pillStyle = if (settings.solidFallback) com.example.system.PillStyle.BLACK else settings.pillStyle,
                        glassDensity = settings.glassDensity,
                        reduceGlass = settings.reduceGlass,
                        solidFallback = settings.solidFallback,
                        accentColor = effectiveAccent,
                        backdrop = LocalAppBackdrop.current
                    )
                    .let { if (bgBrush != null) it.background(bgBrush) else it }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Floating Windows",
                        color = IslandColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                app.soundManager.playTap()
                                collapsedToRail = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Dock as narrow rail", tint = IslandColors.TextSecondary)
                        }
                        IconButton(
                            onClick = {
                                app.soundManager.playCollapse()
                                onClose()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close sidebar", tint = IslandColors.TextSecondary)
                        }
                    }
                }

                // Built-in draggable overlay Floating Mini-Windows
                Text(
                    text = "Overlay Mini-Windows (Draggable)",
                    color = IslandColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MiniWindowShortcut(
                        label = "Notes",
                        icon = Icons.Filled.Description,
                        accentColor = effectiveAccent,
                        modifier = Modifier.weight(1f),
                        testTag = "floating_mini_notes"
                    ) {
                        app.soundManager.playExpand()
                        app.islandController.setFloatingMiniTab(IslandTab.NOTES)
                        app.islandController.collapse()
                    }
                    MiniWindowShortcut(
                        label = "Timer",
                        icon = Icons.Filled.Timer,
                        accentColor = effectiveAccent,
                        modifier = Modifier.weight(1f),
                        testTag = "floating_mini_timer"
                    ) {
                        app.soundManager.playExpand()
                        app.islandController.setFloatingSidebarOpen(false)
                        app.islandController.expand(IslandTab.TIMER)
                    }
                    MiniWindowShortcut(
                        label = "Search",
                        icon = Icons.Filled.Search,
                        accentColor = effectiveAccent,
                        modifier = Modifier.weight(1f),
                        testTag = "floating_mini_search"
                    ) {
                        app.soundManager.playSearchOpen()
                        app.islandController.setFloatingMiniTab(IslandTab.SEARCH)
                        app.islandController.collapse()
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = if (lastResult != null) {
                        lastResult!!.pathUsed.title
                    } else {
                        "All Apps (${selectedApps.size}) · Tap for Floating Window"
                    },
                    color = effectiveAccent,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(188.dp)) {
                    items(selectedApps, key = { it.packageName }) { item ->
                        val unread = unreadCounts[item.packageName] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    app.soundManager.playTap()
                                    app.floatingWindowManager.launchAppInFloatingWindow(item.packageName)
                                    onClose()
                                    app.islandController.collapse()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                val bmp = item.iconBitmap
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = item.label, modifier = Modifier.size(28.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                                }
                                if (unread > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(effectiveAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(unread.coerceAtMost(9).toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = item.label,
                                color = IslandColors.TextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = IslandColors.TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniWindowShortcut(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (pressed) MotionTokens.TAP_SQUISH_SCALE else 1.0f,
        animationSpec = MotionTokens.spatialFast(),
        label = "mini_win_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scaleState.value
                scaleY = scaleState.value
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = IslandColors.TextPrimary, fontSize = 11.sp, maxLines = 1)
    }
}
