package com.example.island

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import com.example.DynamicIslandApp
import com.example.notes.NoteEntity
import com.example.ocr.MediaProjectionPromptActivity
import com.example.ocr.OcrPrivacyGate
import com.example.search.SearchResults
import com.example.system.IslandSettings
import com.example.system.QuickToggleType
import com.example.system.TimerEngine
import com.example.ui.theme.IslandColors
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SearchIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color,
    onCollapse: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }

    LaunchedEffect(query) {
        results = app.searchEngine.search(query)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps, notes, contacts, or math...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                app.searchEngine.launchWebSearch(query, preferFloating = true)
                onCollapse()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IslandColors.TextPrimary,
                unfocusedTextColor = IslandColors.TextPrimary,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                focusedContainerColor = Color.White.copy(alpha = 0.06f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("island_search_input")
        )

        if (results.inlineMathResult != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .clickable {
                        app.clipboardMonitor.copyToClipboard(results.inlineMathResult!!)
                        app.islandController.showTransientBanner("Copied ${results.inlineMathResult}")
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("= ${results.inlineMathResult}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Tap to copy", color = IslandColors.TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.height(148.dp)) {
            if (query.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                app.searchEngine.launchWebSearch(query, preferFloating = true)
                                onCollapse()
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Web Search", tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search Google for \"$query\"", color = IslandColors.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (results.apps.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(results.apps) { launchApp ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        app.searchEngine.launchApp(launchApp.packageName)
                                        onCollapse()
                                    }
                                    .padding(4.dp)
                            ) {
                                val bmp = remember(launchApp.packageName) {
                                    launchApp.icon?.toBitmap(64, 64)?.asImageBitmap()
                                }
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = launchApp.label, modifier = Modifier.size(32.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(launchApp.label, color = IslandColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            items(results.notes) { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            app.clipboardMonitor.copyToClipboard(note.content)
                            app.islandController.showTransientBanner("Copied note")
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Note: ${note.title}", color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            items(results.contacts) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(contact.name, color = IslandColors.TextPrimary, fontSize = 12.sp)
                    Text(contact.phone, color = accentColor, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ToolsIslandPanel(
    app: DynamicIslandApp,
    settings: IslandSettings,
    accentColor: Color,
    onHaptic: () -> Unit,
    onCollapse: () -> Unit
) {
    val toggleState by app.quickTogglesManager.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        app.quickTogglesManager.refreshSystemStates()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Toggles Grid (only enabled ones)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.includedQuickToggles) { type ->
                val (icon, active, onClick) = when (type) {
                    QuickToggleType.FLASHLIGHT -> Triple(
                        Icons.Filled.FlashlightOn,
                        toggleState.isTorchOn
                    ) { onHaptic(); app.quickTogglesManager.toggleFlashlight(); Unit }

                    QuickToggleType.DND -> Triple(
                        Icons.Filled.DoNotDisturbOn,
                        toggleState.isDndOn
                    ) { onHaptic(); app.quickTogglesManager.toggleDoNotDisturb(); Unit }

                    QuickToggleType.BRIGHTNESS -> Triple(
                        Icons.Filled.BrightnessHigh,
                        toggleState.hasWriteSettingsPermission
                    ) { onHaptic(); if (!toggleState.hasWriteSettingsPermission) app.quickTogglesManager.openWriteSettings() }

                    QuickToggleType.WIFI -> Triple(
                        Icons.Filled.Wifi,
                        false
                    ) { onHaptic(); app.quickTogglesManager.openWifiPanel(); onCollapse() }

                    QuickToggleType.BLUETOOTH -> Triple(
                        Icons.Filled.Bluetooth,
                        false
                    ) { onHaptic(); app.quickTogglesManager.openBluetoothPanel(); onCollapse() }

                    QuickToggleType.SELECT_OCR -> Triple(
                        Icons.Filled.SelectAll,
                        false
                    ) {
                        onHaptic()
                        if (!settings.screenReadingEnabled) {
                            app.islandController.showTransientBanner("Enable Screen Reading in Settings → Privacy first")
                        } else {
                            app.islandController.setSelectMode(true)
                        }
                    }

                    QuickToggleType.VAULT_CAPTURE -> Triple(
                        Icons.Filled.CameraAlt,
                        false
                    ) {
                        onHaptic()
                        onCollapse()
                        MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                    }
                }

                ToggleTile(
                    label = type.label,
                    icon = icon,
                    isActive = active,
                    accentColor = accentColor,
                    onClick = onClick
                )
            }
        }

        // Torch strength slider if Android 13+ camera reports > 1 level
        if (toggleState.isTorchOn && toggleState.maxTorchStrength > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Torch", color = IslandColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(48.dp))
                Slider(
                    value = toggleState.torchStrength.toFloat(),
                    onValueChange = { app.quickTogglesManager.setTorchStrength(it.toInt()) },
                    valueRange = 1f..toggleState.maxTorchStrength.toFloat(),
                    steps = (toggleState.maxTorchStrength - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Brightness slider (or direct permission grant prompt)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.BrightnessHigh, contentDescription = "Brightness", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            if (toggleState.hasWriteSettingsPermission) {
                Slider(
                    value = toggleState.brightness.toFloat(),
                    onValueChange = { app.quickTogglesManager.setScreenBrightness(it.toInt()) },
                    valueRange = 10f..255f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                    modifier = Modifier.weight(1f)
                )
            } else {
                TextButton(onClick = { app.quickTogglesManager.openWriteSettings() }) {
                    Text("Allow Modify System Settings for Brightness", color = accentColor, fontSize = 12.sp)
                }
            }
        }

        // Floating Window Sidebar Trigger + Direct Screen Reading / Capture shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (settings.floatingEnabled) {
                Button(
                    onClick = {
                        onHaptic()
                        app.islandController.setFloatingSidebarOpen(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.ViewSidebar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Floating Apps", fontSize = 12.sp, maxLines = 1)
                }
            }
            if (settings.ocrFeatureEnabled) {
                Button(
                    onClick = {
                        onHaptic()
                        if (!settings.screenReadingEnabled) {
                            app.islandController.showTransientBanner("Turn ON Screen Reading in Settings → Privacy")
                        } else {
                            app.islandController.setSelectMode(true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.screenReadingEnabled) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Select OCR", fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ToggleTile(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) accentColor else IslandColors.TextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, color = IslandColors.TextPrimary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun VaultIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color,
    onCollapse: () -> Unit
) {
    val active by app.vaultManager.activeItems.collectAsState(initial = emptyList())
    val trashed by app.vaultManager.trashedItems.collectAsState(initial = emptyList())
    var showTrash by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showTrash) "Trash (${trashed.size}) · 24h retention" else "10-Min Photo Vault (${active.size})",
                color = IslandColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { showTrash = !showTrash }) {
                    Text(if (showTrash) "Active" else "Trash (${trashed.size})", color = accentColor, fontSize = 12.sp)
                }
                TextButton(onClick = {
                    onCollapse()
                    MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                }) {
                    Text("+ Shot", color = accentColor, fontSize = 12.sp)
                }
            }
        }

        val list = if (showTrash) trashed else active
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showTrash) "Trash is empty" else "Share, drag, or 3-finger screenshot to store for 10 min",
                    color = IslandColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(122.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    val remainingMs = (item.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
                    Column(
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                if (!item.isTrashed) {
                                    app.vaultManager.attachOrShareItem(item)
                                    onCollapse()
                                }
                            }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(File(item.filePath)),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp, 58.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (item.isTrashed) "Trashed" else TimerEngine.formatDuration(remainingMs),
                            color = if (item.isTrashed) IslandColors.AccentRed else accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            if (item.isTrashed) {
                                IconButton(
                                    onClick = { scope.launch { app.vaultManager.restoreItem(item.id) } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Restore, contentDescription = "Restore 10m", tint = IslandColors.AccentGreen, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                IconButton(
                                    onClick = { app.vaultManager.attachOrShareItem(item); onCollapse() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Attach / Share", tint = IslandColors.TextPrimary, modifier = Modifier.size(15.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (item.isTrashed) app.vaultManager.purgeItemNow(item.id)
                                        else app.vaultManager.moveToTrash(item.id)
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = IslandColors.AccentRed, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipsIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color
) {
    val clips by app.clipboardMonitor.historyFlow.collectAsState(initial = emptyList())
    val status by app.clipboardMonitor.lastCaptureStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status, color = IslandColors.TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { app.clipboardMonitor.captureIfAllowed(isForeground = true) }) {
                Text("Capture", color = accentColor, fontSize = 12.sp)
            }
        }

        if (clips.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                Text("No clips captured in foreground yet", color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.height(128.dp)) {
                items(clips, key = { it.id }) { clip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                app.clipboardMonitor.copyToClipboard(clip.content)
                                app.islandController.showTransientBanner("Copied to clipboard")
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(clip.content, color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun NotesIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color
) {
    val notes by app.database.noteDao().observeAll().collectAsState(initial = emptyList())
    val links by app.database.savedLinkDao().observeAll().collectAsState(initial = emptyList())
    var draft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Quick note...", fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IslandColors.TextPrimary,
                    unfocusedTextColor = IslandColors.TextPrimary,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty()) {
                        scope.launch {
                            app.database.noteDao().upsert(NoteEntity(title = trimmed.take(32), content = trimmed))
                            draft = ""
                        }
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(accentColor)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Save note", tint = Color.Black)
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.height(116.dp)) {
            items(links.take(5), key = { "link_${it.id}" }) { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (link.urlOrUri.startsWith("http")) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.urlOrUri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${link.itemType}: ${link.title}", color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            items(notes, key = { "note_${it.id}" }) { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            app.clipboardMonitor.copyToClipboard(note.content)
                            app.islandController.showTransientBanner("Copied note")
                        }
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(note.content, color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { app.database.noteDao().deleteById(note.id) } }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = IslandColors.TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MemoIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color
) {
    val ui by app.voiceMemoManager.uiState.collectAsState()
    val memos by app.voiceMemoManager.memosFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (ui.isRecording) IslandColors.AccentRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                .clickable {
                    if (ui.isRecording) {
                        app.voiceMemoManager.stopRecording()
                    } else if (!app.voiceMemoManager.startRecording()) {
                        Toast.makeText(context, "Grant Microphone permission in Setup Wizard", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ui.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = "Voice Memo",
                    tint = if (ui.isRecording) IslandColors.AccentRed else accentColor
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (ui.isRecording) "Tap to Stop · ${TimerEngine.formatDuration(ui.recordingElapsedMs)}" else "Tap to Record Voice Memo",
                    color = IslandColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.height(112.dp)) {
            items(memos, key = { it.id }) { memo ->
                val isPlayingThis = ui.playingMemoId == memo.id && ui.isPlaying
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { app.voiceMemoManager.togglePlayMemo(memo) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play Memo",
                            tint = accentColor
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(memo.title, color = IslandColors.TextPrimary, fontSize = 12.sp)
                        Text(TimerEngine.formatDuration(memo.durationMs), color = IslandColors.TextSecondary, fontSize = 10.sp)
                    }
                    IconButton(
                        onClick = { app.voiceMemoManager.deleteMemo(memo) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete memo", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsIslandPanel(
    app: DynamicIslandApp,
    state: IslandState,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Island Alerts", color = IslandColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (state.alertsHistory.isNotEmpty()) {
                TextButton(onClick = { app.islandController.clearAlerts() }) {
                    Text("Clear", color = accentColor, fontSize = 12.sp)
                }
            }
        }

        if (state.alertsHistory.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(104.dp), contentAlignment = Alignment.Center) {
                Text("Timer done, charging full, and vault expiry alerts appear here", color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.height(128.dp)) {
                items(state.alertsHistory, key = { it.id }) { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, color = IslandColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(alert.subtitle, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
