package com.example.island

import androidx.compose.animation.core.animateFloatAsState
import com.example.ui.theme.MotionTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.setup.crimsonSwitchColors
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MockupScreenHeader
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import kotlinx.coroutines.launch

@Composable
fun FeaturePickerScreen(
    app: DynamicIslandApp,
    onBack: () -> Unit
) {
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val scope = rememberCoroutineScope()
    var capBanner by remember { mutableStateOf<String?>(null) }
    var activeDragTab by remember { mutableStateOf<IslandTab?>(null) }

    val enabledList = settings.enabledTabs
    val disabledList = IslandTab.entries.filterNot { it in enabledList }

    ObsidianCrimsonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Customise Island",
                subtitle = "Pick & reorder up to ${IslandSettings.MAX_ISLAND_FEATURES} dock slots",
                icon = Icons.Filled.DashboardCustomize,
                onBack = onBack,
                backTestTag = "feature_picker_back",
                trailingAction = {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF470B13))
                            .border(1.dp, IslandColors.CrimsonBorderStrong, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("feature_cap_badge")
                    ) {
                        Text(
                            text = "${enabledList.size} / ${IslandSettings.MAX_ISLAND_FEATURES}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // Live Dock Preview
                ObsidianGlassCard(highlighted = true, cornerRadius = 24.dp, contentPadding = PaddingValues(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Live Dock Order (First 5 in Main · Next 3 in More)",
                            color = IslandColors.TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.dp, IslandColors.CrimsonBorderStrong, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            enabledList.take(3).forEach { tab ->
                                Icon(tabIcon(tab), contentDescription = null, tint = IslandColors.CrimsonBright, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                            }
                            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF221317)))
                            enabledList.drop(3).take(5).forEach { tab ->
                                Spacer(Modifier.width(5.dp))
                                Icon(tabIcon(tab), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (capBanner != null || enabledList.size >= IslandSettings.MAX_ISLAND_FEATURES) {
                    ObsidianGlassCard(
                        highlighted = true,
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("feature_cap_notice")
                    ) {
                        Text(
                            text = capBanner ?: "8 / 8 features active. Turn one off before adding another.",
                            color = IslandColors.CrimsonBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "section_active_hdr") {
                        Text(
                            text = "Active in Dock (${enabledList.size}) · Drag handle to reorder",
                            color = IslandColors.CrimsonBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }

                    items(enabledList, key = { it.name }) { tab ->
                        val enabledIdx = enabledList.indexOf(tab)
                        val isDraggingThis = activeDragTab == tab
                        var menuExpanded by remember { mutableStateOf(false) }
                        var dragDeltaY by remember { mutableFloatStateOf(0f) }

                        val liftScaleState = animateFloatAsState(
                            targetValue = if (isDraggingThis) 1.03f else 1f,
                            animationSpec = MotionTokens.spatialFast(),
                            label = "feature_drag_lift"
                        )

                        ObsidianGlassCard(
                            highlighted = isDraggingThis,
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = if (isDraggingThis) dragDeltaY.coerceIn(-44f, 44f) else 0f
                                    scaleX = liftScaleState.value
                                    scaleY = liftScaleState.value
                                }
                                .testTag("feature_row_${tab.name.lowercase()}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .size(48.dp)
                                        .pointerInput(tab, enabledIdx) {
                                            detectDragGestures(
                                                onDragStart = { dragDeltaY = 0f; activeDragTab = tab },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragDeltaY += dragAmount.y
                                                    if (dragDeltaY > 48f) {
                                                        dragDeltaY = 0f
                                                        app.soundManager.playTap()
                                                        scope.launch { app.settingsRepository.moveTab(tab, +1) }
                                                    } else if (dragDeltaY < -48f) {
                                                        dragDeltaY = 0f
                                                        app.soundManager.playTap()
                                                        scope.launch { app.settingsRepository.moveTab(tab, -1) }
                                                    }
                                                },
                                                onDragEnd = { dragDeltaY = 0f; activeDragTab = null },
                                                onDragCancel = { dragDeltaY = 0f; activeDragTab = null }
                                            )
                                        }
                                        .testTag("feature_drag_handle_${tab.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragIndicator,
                                        contentDescription = "Drag to reorder ${tab.title}",
                                        tint = if (isDraggingThis) IslandColors.CrimsonBright else IslandColors.TextSecondary
                                    )
                                }

                                CrimsonSquircleIcon(icon = tabIcon(tab), size = 34.dp, iconSize = 18.dp)
                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(tab.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(
                                            text = if (enabledIdx < 5) "Main #${enabledIdx + 1}" else "More",
                                            fontSize = 10.sp,
                                            color = if (enabledIdx < 5) IslandColors.CrimsonBright else IslandColors.TextSecondary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(tab.description, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.testTag("feature_menu_${tab.name.lowercase()}")
                                    ) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "Reorder menu", tint = IslandColors.TextSecondary)
                                    }
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Move up") },
                                            leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                                            enabled = enabledIdx > 0,
                                            onClick = {
                                                menuExpanded = false
                                                scope.launch { app.settingsRepository.moveTab(tab, -1) }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Move down") },
                                            leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                                            enabled = enabledIdx in 0 until enabledList.lastIndex,
                                            onClick = {
                                                menuExpanded = false
                                                scope.launch { app.settingsRepository.moveTab(tab, +1) }
                                            }
                                        )
                                    }
                                }

                                Switch(
                                    checked = true,
                                    onCheckedChange = {
                                        app.soundManager.playToggle(isOn = false)
                                        scope.launch {
                                            app.settingsRepository.toggleTabInIsland(tab)
                                            capBanner = null
                                        }
                                    },
                                    colors = crimsonSwitchColors(),
                                    modifier = Modifier.testTag("feature_toggle_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (disabledList.isNotEmpty()) {
                        item(key = "section_disabled_hdr") {
                            Text(
                                text = "Disabled Modules (${disabledList.size})",
                                color = IslandColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }
                        items(disabledList, key = { it.name }) { tab ->
                            ObsidianGlassCard(
                                cornerRadius = 18.dp,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.fillMaxWidth().alpha(0.70f).testTag("feature_row_${tab.name.lowercase()}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(tabIcon(tab), contentDescription = null, tint = IslandColors.TextSecondary, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tab.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(tab.description, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                                    }
                                    Switch(
                                        checked = false,
                                        onCheckedChange = {
                                            app.soundManager.playToggle(isOn = true)
                                            scope.launch {
                                                val ok = app.settingsRepository.toggleTabInIsland(tab)
                                                capBanner = if (!ok) "Maximum 8 / 8 features reached. Disable one to add ${tab.title}." else null
                                            }
                                        },
                                        colors = crimsonSwitchColors(),
                                        modifier = Modifier.testTag("feature_toggle_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(18.dp)) }
                }
            }
        }
    }
}
