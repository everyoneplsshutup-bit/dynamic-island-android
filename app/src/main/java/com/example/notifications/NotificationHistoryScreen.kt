package com.example.notifications

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.setup.crimsonSwitchColors
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MockupScreenHeader
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationHistoryScreen(
    app: DynamicIslandApp,
    onBack: () -> Unit
) {
    val history by app.database.notificationDao().observeAll().collectAsState(initial = emptyList())
    val rules by app.database.appRuleDao().observeAll().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    ObsidianCrimsonBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Notifications & Rules",
                subtitle = "Inbox history & per-app island previews",
                icon = Icons.Filled.Notifications,
                onBack = onBack,
                trailingAction = {
                    if (selectedTab == 0 && history.isNotEmpty()) {
                        IconButton(
                            onClick = { scope.launch { app.database.notificationDao().clearAll() } },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA230B10))
                                .border(1.dp, IslandColors.CrimsonBorder, CircleShape)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear history", tint = Color.White)
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                ObsidianGlassCard(cornerRadius = 24.dp, contentPadding = PaddingValues(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("History (${history.size})", "App Rules (${rules.size})").forEachIndexed { idx, label ->
                            val active = selectedTab == idx
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFF470B13) else Color.Transparent)
                                    .then(if (active) Modifier.border(1.dp, IslandColors.CrimsonBorderStrong, CircleShape) else Modifier)
                                    .clickable { selectedTab = idx }
                            ) {
                                Text(
                                    label,
                                    color = if (active) Color.White else IslandColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (selectedTab == 0) {
                    if (history.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CrimsonSquircleIcon(icon = Icons.Filled.NotificationsNone, size = 56.dp, iconSize = 26.dp)
                                Spacer(Modifier.height(10.dp))
                                Text("No notification previews captured yet", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(history, key = { it.id }) { item ->
                                ObsidianGlassCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.appName, fontWeight = FontWeight.SemiBold, color = IslandColors.CrimsonBright, fontSize = 13.sp)
                                        Text(formatter.format(Date(item.timestamp)), color = IslandColors.TextTertiary, fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    if (item.text.isNotBlank()) {
                                        Text(item.text, color = IslandColors.TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (rules.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Per-app preview rules appear here as apps post notifications.", color = IslandColors.TextSecondary)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rules, key = { it.packageName }) { rule ->
                                ObsidianGlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(rule.appName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text(rule.packageName, color = IslandColors.TextTertiary, fontSize = 11.sp)
                                        }
                                        Switch(
                                            checked = rule.allowIslandPreview,
                                            onCheckedChange = { allowed ->
                                                app.soundManager.playToggle(allowed)
                                                scope.launch {
                                                    app.database.appRuleDao().setIslandPreviewAllowed(rule.packageName, allowed)
                                                }
                                            },
                                            colors = crimsonSwitchColors()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
