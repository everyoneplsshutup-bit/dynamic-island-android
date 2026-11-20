package com.example.floating

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Calculate
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

@Composable
fun FloatingSidebarOverlay(
    app: DynamicIslandApp,
    settings: IslandSettings,
    accentColor: Color,
    onClose: () -> Unit
) {
    var allApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    val lastResult by app.floatingWindowManager.lastResult.collectAsState()

    LaunchedEffect(Unit) {
        allApps = app.searchEngine.getInstalledApps()
    }

    val selectedApps = remember(allApps, settings.floatingAppPackages) {
        if (settings.floatingAppPackages.isEmpty()) {
            allApps.take(8)
        } else {
            val map = allApps.associateBy { it.packageName }
            settings.floatingAppPackages.mapNotNull { map[it] }
        }
    }

    Column(
        modifier = Modifier
            .width(248.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xF20B0B10))
            .border(1.dp, IslandColors.GlassSpecularBorder, RoundedCornerShape(26.dp))
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

        // Built-in draggable overlay Floating Mini-Windows (always work on any app/ROM)
        Text(
            text = "Overlay Mini-Windows (Draggable)",
            color = IslandColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniWindowShortcut(
                label = "Calc",
                icon = Icons.Filled.Calculate,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                testTag = "floating_mini_calc"
            ) {
                app.soundManager.playExpand()
                app.islandController.setFloatingMiniTab(IslandTab.CALC)
                app.islandController.collapse()
            }
            MiniWindowShortcut(
                label = "Notes",
                icon = Icons.Filled.Description,
                accentColor = accentColor,
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
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
                testTag = "floating_mini_timer"
            ) {
                app.soundManager.playExpand()
                app.islandController.setFloatingMiniTab(IslandTab.TIMER)
                app.islandController.collapse()
            }
            MiniWindowShortcut(
                label = "Search",
                icon = Icons.Filled.Search,
                accentColor = accentColor,
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
                "Apps (MIUI Freeform · Configure in Settings)"
            },
            color = accentColor,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.height(176.dp)) {
            items(selectedApps, key = { it.packageName }) { item ->
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
                    val bmp = item.iconBitmap
                    if (bmp != null) {
                        Image(bitmap = bmp, contentDescription = item.label, modifier = Modifier.size(28.dp).clip(CircleShape))
                    } else {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
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
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "mini_win_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
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
        Text(label, color = IslandColors.TextPrimary, fontSize = 10.sp, maxLines = 1)
    }
}
