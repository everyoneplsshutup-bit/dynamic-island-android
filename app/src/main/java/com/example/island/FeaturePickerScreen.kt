package com.example.island

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.system.IslandSettings
import com.example.system.IslandTab
import com.example.ui.theme.IslandColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturePickerScreen(
    app: DynamicIslandApp,
    onBack: () -> Unit
) {
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val scope = rememberCoroutineScope()
    var capBanner by remember { mutableStateOf<String?>(null) }

    val enabledList = settings.enabledTabs
    val disabledList = IslandTab.entries.filterNot { it in enabledList }
    val orderedRows = enabledList + disabledList

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customise Island") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("feature_picker_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = if (enabledList.size >= IslandSettings.MAX_ISLAND_FEATURES) {
                            IslandColors.AccentOrange.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.padding(end = 12.dp).testTag("feature_cap_badge")
                    ) {
                        Text(
                            text = "${enabledList.size} / ${IslandSettings.MAX_ISLAND_FEATURES}",
                            fontWeight = FontWeight.Bold,
                            color = if (enabledList.size >= IslandSettings.MAX_ISLAND_FEATURES) {
                                IslandColors.AccentOrange
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Pinned live mini-preview of the island at the top
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0A0A0E),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Live Island Mini-Preview",
                        color = IslandColors.TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    // Mini Pill around camera hole
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        enabledList.take(3).forEach { tab ->
                            Icon(
                                imageVector = tabIcon(tab),
                                contentDescription = null,
                                tint = IslandColors.AccentOrange,
                                modifier = Modifier.size(14.dp).padding(end = 2.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22242C))
                        )
                        enabledList.drop(3).take(5).forEach { tab ->
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = tabIcon(tab),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (capBanner != null || enabledList.size >= IslandSettings.MAX_ISLAND_FEATURES) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IslandColors.AccentOrange.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("feature_cap_notice")
                ) {
                    Text(
                        text = capBanner ?: "8 / 8 features active. Turn one off before adding another.",
                        color = IslandColors.AccentOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(orderedRows, key = { it.name }) { tab ->
                    val isEnabled = tab in enabledList
                    val enabledIdx = enabledList.indexOf(tab)
                    var menuExpanded by remember { mutableStateOf(false) }
                    var dragDeltaY by remember { mutableFloatStateOf(0f) }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = if (isEnabled) 2.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feature_row_${tab.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            // Six-dot drag handle (minimum 44x44 px -> 48.dp touch target)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(44.dp)
                                    .pointerInput(tab, enabledIdx) {
                                        if (!isEnabled) return@pointerInput
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { dragDeltaY = 0f },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragDeltaY += dragAmount.y
                                                if (dragDeltaY > 72f) {
                                                    dragDeltaY = 0f
                                                    scope.launch { app.settingsRepository.moveTab(tab, +1) }
                                                } else if (dragDeltaY < -72f) {
                                                    dragDeltaY = 0f
                                                    scope.launch { app.settingsRepository.moveTab(tab, -1) }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DragIndicator,
                                    contentDescription = "Reorder ${tab.title}",
                                    tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.35f)
                                )
                            }

                            Icon(
                                imageVector = tabIcon(tab),
                                contentDescription = null,
                                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(tab.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    tab.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            // Non-drag Move Up / Move Down menu (accessibility compliant)
                            if (isEnabled) {
                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.testTag("feature_menu_${tab.name.lowercase()}")
                                    ) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "Reorder menu")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
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
                            }

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = {
                                    scope.launch {
                                        val ok = app.settingsRepository.toggleTabInIsland(tab)
                                        capBanner = if (!ok) {
                                            "Maximum 8 / 8 features reached. Disable one to add ${tab.title}."
                                        } else {
                                            null
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("feature_toggle_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}
