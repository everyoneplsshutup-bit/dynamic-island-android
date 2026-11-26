package com.example.notifications

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.setup.crimsonSwitchColors
import com.example.system.IslandSettings
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
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val expandedApps = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    val filteredHistory = remember(history, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) history
        else history.filter {
            it.appName.contains(q, ignoreCase = true) ||
                it.title.contains(q, ignoreCase = true) ||
                it.text.contains(q, ignoreCase = true)
        }
    }
    val groupedHistory = remember(filteredHistory) {
        filteredHistory.groupBy { it.packageName }
    }

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
                                .background(IslandColors.ObsidianCard)
                                .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
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
                                    .background(if (active) accent.copy(alpha = 0.24f) else Color.Transparent)
                                    .then(if (active) Modifier.border(1.dp, accent.copy(alpha = 0.72f), CircleShape) else Modifier)
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

                Spacer(Modifier.height(10.dp))

                if (selectedTab == 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search inbox...", color = IslandColors.TextSecondary, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = accent) },
                            singleLine = true,
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = IslandColors.ObsidianCardElevated,
                                unfocusedContainerColor = IslandColors.ObsidianCard,
                                focusedBorderColor = accent.copy(alpha = 0.72f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(if (settings.notifInboxGrouped) accent.copy(alpha = 0.24f) else IslandColors.ObsidianCard)
                                .border(1.dp, if (settings.notifInboxGrouped) accent else Color.White.copy(alpha = 0.14f), CircleShape)
                                .clickable {
                                    app.soundManager.playTap()
                                    scope.launch { app.settingsRepository.update { it.copy(notifInboxGrouped = !it.notifInboxGrouped) } }
                                }
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (settings.notifInboxGrouped) "Grouped" else "All",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (filteredHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CrimsonSquircleIcon(icon = Icons.Filled.NotificationsNone, size = 56.dp, iconSize = 26.dp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    if (searchQuery.isBlank()) "No notification previews captured yet" else "No matching notifications",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (settings.notifInboxGrouped) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            groupedHistory.forEach { (pkg, appItems) ->
                                val first = appItems.first()
                                val expanded = expandedApps[pkg] == true || searchQuery.isNotBlank()
                                item(key = "grp_$pkg") {
                                    ObsidianGlassCard(onClick = { expandedApps[pkg] = !expanded }) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${first.appName.ifBlank { pkg }} (${appItems.size})",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = accent,
                                                    fontSize = 14.sp
                                                )
                                                if (!expanded) {
                                                    Text(first.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                                }
                                            }
                                            Icon(
                                                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null,
                                                tint = IslandColors.TextSecondary
                                            )
                                        }
                                        if (expanded) {
                                            Spacer(Modifier.height(8.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                appItems.forEach { item ->
                                                    val otp = OtpExtractor.extract(item.title, item.text)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color.White.copy(alpha = 0.05f))
                                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                                Spacer(Modifier.width(8.dp))
                                                                Text(formatter.format(Date(item.timestamp)), color = IslandColors.TextTertiary, fontSize = 11.sp)
                                                            }
                                                            if (item.text.isNotBlank()) {
                                                                Text(item.text, color = IslandColors.TextSecondary, fontSize = 12.sp)
                                                            }
                                                        }
                                                        if (otp != null) {
                                                            Spacer(Modifier.width(8.dp))
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier
                                                                    .clip(CircleShape)
                                                                    .background(accent.copy(alpha = 0.24f))
                                                                    .clickable {
                                                                        app.soundManager.playCopy()
                                                                        app.clipboardMonitor.copyToClipboard(otp)
                                                                        Toast.makeText(context, "Copied OTP $otp", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                            ) {
                                                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(otp, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredHistory, key = { it.id }) { item ->
                                val otp = OtpExtractor.extract(item.title, item.text)
                                ObsidianGlassCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.appName, fontWeight = FontWeight.SemiBold, color = accent, fontSize = 13.sp)
                                        Text(formatter.format(Date(item.timestamp)), color = IslandColors.TextTertiary, fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                            if (item.text.isNotBlank()) {
                                                Text(item.text, color = IslandColors.TextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                        if (otp != null) {
                                            Spacer(Modifier.width(8.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(accent.copy(alpha = 0.24f))
                                                    .clickable {
                                                        app.soundManager.playCopy()
                                                        app.clipboardMonitor.copyToClipboard(otp)
                                                        Toast.makeText(context, "Copied OTP $otp", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(otp, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
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
