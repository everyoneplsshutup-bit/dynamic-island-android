package com.example.island

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.view.View
import android.widget.Toast
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.DynamicIslandApp
import com.example.notes.NoteEntity
import com.example.ocr.MediaProjectionPromptActivity
import com.example.search.SearchResults
import com.example.system.IslandSettings
import com.example.system.QuickToggleType
import com.example.system.TimerEngine
import com.example.ui.theme.IslandColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    LaunchedEffect(Unit) {
        app.islandController.setKeyboardFocusNeeded(true)
    }

    LaunchedEffect(query) {
        results = app.searchEngine.search(query)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search Google, apps, notes, or math...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                app.soundManager.playSearchOpen()
                app.searchEngine.launchWebSearch(query, preferFloating = false)
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
                .onFocusChanged { if (it.isFocused) app.islandController.setKeyboardFocusNeeded(true) }
                .testTag("island_search_input")
        )

        if (results.inlineMathResult != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .clickable {
                        app.soundManager.playTap()
                        app.clipboardMonitor.copyToClipboard(results.inlineMathResult!!)
                        app.islandController.showTransientBanner("Copied ${results.inlineMathResult}")
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("= ${results.inlineMathResult}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Tap to copy", color = IslandColors.TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (query.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .clickable {
                                app.soundManager.playSearchOpen()
                                app.searchEngine.launchWebSearch(query, preferFloating = false)
                                onCollapse()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("search_google_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Google Search", tint = accentColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Search Google for \"$query\"", color = IslandColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (results.apps.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(results.apps, key = { it.packageName }) { launchApp ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        app.soundManager.playTap()
                                        app.searchEngine.launchApp(launchApp.packageName)
                                        onCollapse()
                                    }
                                    .padding(4.dp)
                            ) {
                                val bmp = launchApp.iconBitmap
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = launchApp.label, modifier = Modifier.size(30.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
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
                            app.soundManager.playTap()
                            app.clipboardMonitor.copyToClipboard(note.content)
                            app.islandController.showTransientBanner("Copied note")
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("Note: ${note.title}", color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            items(results.contacts) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
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
            .height(216.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Quick Toggles Strip
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings.includedQuickToggles) { type ->
                val (icon, active, onClick) = when (type) {
                    QuickToggleType.FLASHLIGHT -> Triple(
                        Icons.Filled.FlashlightOn,
                        toggleState.isTorchOn
                    ) {
                        onHaptic()
                        app.soundManager.playToggle(isOn = !toggleState.isTorchOn)
                        app.quickTogglesManager.toggleFlashlight()
                        Unit
                    }

                    QuickToggleType.DND -> Triple(
                        Icons.Filled.DoNotDisturbOn,
                        toggleState.isDndOn
                    ) {
                        onHaptic()
                        app.soundManager.playToggle(isOn = !toggleState.isDndOn)
                        app.quickTogglesManager.toggleDoNotDisturb()
                        Unit
                    }

                    QuickToggleType.BRIGHTNESS -> Triple(
                        Icons.Filled.BrightnessHigh,
                        toggleState.hasWriteSettingsPermission
                    ) {
                        onHaptic()
                        app.soundManager.playTap()
                        if (!toggleState.hasWriteSettingsPermission) app.quickTogglesManager.openWriteSettings()
                    }

                    QuickToggleType.WIFI -> Triple(
                        Icons.Filled.Wifi,
                        false
                    ) {
                        onHaptic()
                        app.soundManager.playTap()
                        app.quickTogglesManager.openWifiPanel()
                        onCollapse()
                    }

                    QuickToggleType.BLUETOOTH -> Triple(
                        Icons.Filled.Bluetooth,
                        false
                    ) {
                        onHaptic()
                        app.soundManager.playTap()
                        app.quickTogglesManager.openBluetoothPanel()
                        onCollapse()
                    }

                    QuickToggleType.SELECT_OCR -> Triple(
                        Icons.Filled.SelectAll,
                        false
                    ) {
                        onHaptic()
                        if (!settings.screenReadingEnabled) {
                            app.soundManager.playToggle(isOn = false)
                            app.islandController.showTransientBanner("Enable Screen Reading in Settings → Privacy first")
                        } else {
                            app.soundManager.playToggle(isOn = true)
                            app.islandController.setSelectMode(true)
                        }
                    }

                    QuickToggleType.VAULT_CAPTURE -> Triple(
                        Icons.Filled.CameraAlt,
                        false
                    ) {
                        onHaptic()
                        app.soundManager.playTap()
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

        if (toggleState.isTorchOn && toggleState.maxTorchStrength > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Torch", color = IslandColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp))
                Slider(
                    value = toggleState.torchStrength.toFloat(),
                    onValueChange = { app.quickTogglesManager.setTorchStrength(it.toInt()) },
                    valueRange = 1f..toggleState.maxTorchStrength.toFloat(),
                    steps = (toggleState.maxTorchStrength - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                    modifier = Modifier.weight(1f).height(26.dp)
                )
            }
        }

        // Brightness slider (or permission prompt)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.BrightnessHigh, contentDescription = "Brightness", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            if (toggleState.hasWriteSettingsPermission) {
                Slider(
                    value = toggleState.brightness.toFloat(),
                    onValueChange = { app.quickTogglesManager.setScreenBrightness(it.toInt()) },
                    valueRange = 10f..255f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                    modifier = Modifier.weight(1f).height(28.dp)
                )
            } else {
                TextButton(onClick = { app.soundManager.playTap(); app.quickTogglesManager.openWriteSettings() }) {
                    Text("Allow Modify System Settings for Brightness", color = accentColor, fontSize = 11.sp)
                }
            }
        }

        // Floating Window Sidebar Trigger + Select Mode OCR Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (settings.floatingEnabled) {
                Button(
                    onClick = {
                        onHaptic()
                        app.soundManager.playExpand()
                        app.islandController.setFloatingSidebarOpen(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.weight(1f).height(42.dp).testTag("tools_floating_apps_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ViewSidebar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Floating Apps", fontSize = 12.sp, maxLines = 1)
                }
            }
            if (settings.ocrFeatureEnabled) {
                Button(
                    onClick = {
                        onHaptic()
                        if (!settings.screenReadingEnabled) {
                            app.soundManager.playToggle(isOn = false)
                            app.islandController.showTransientBanner("Turn ON Screen Reading in Settings → Privacy")
                        } else {
                            app.soundManager.playToggle(isOn = true)
                            app.islandController.setSelectMode(true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.screenReadingEnabled) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.weight(1f).height(42.dp).testTag("tools_select_ocr_btn")
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "toggle_tile_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(66.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.09f))
            .border(
                width = 0.75.dp,
                color = if (isActive) accentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 7.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) accentColor else IslandColors.TextPrimary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(label, color = IslandColors.TextPrimary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color,
    onCollapse: () -> Unit
) {
    val active by app.vaultManager.activeItems.collectAsState(initial = emptyList())
    val trashed by app.vaultManager.trashedItems.collectAsState(initial = emptyList())
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val foregroundChatPkg by IslandService.currentForegroundApp.collectAsState()
    var showTrash by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Live 1-second ticking countdown for all Vault items
    LaunchedEffect(active.size) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val activityResultOwner = LocalActivityResultRegistryOwner.current
    val photoPicker = if (activityResultOwner != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch { app.vaultManager.saveFromUri(uri, sourceType = "PICKED") }
            }
        }
    } else null

    val screenshotsFolderPicker = if (activityResultOwner != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                scope.launch {
                    app.settingsRepository.update { it.copy(vaultScreenshotsTreeUri = treeUri.toString()) }
                    app.vaultManager.ingestRecentScreenshotFromTree(treeUri)
                    app.islandController.showTransientBanner("Auto-store enabled for Screenshots folder")
                }
            }
        }
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showTrash) "Trash (${trashed.size}) · 24h" else "Vault (${active.size}) · 10m",
                color = IslandColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (settings.vaultScreenshotsTreeUri.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            app.soundManager.playTap()
                            if (screenshotsFolderPicker != null) {
                                val initial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    DocumentsContract.buildDocumentUri(
                                        "com.android.externalstorage.documents",
                                        "primary:DCIM/Screenshots"
                                    )
                                } else null
                                screenshotsFolderPicker.launch(initial)
                            } else {
                                onCollapse()
                                val intent = Intent(context, com.example.MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = IslandColors.AccentGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Auto", color = IslandColors.AccentGreen, fontSize = 11.sp)
                    }
                }
                TextButton(onClick = { app.soundManager.playTap(); showTrash = !showTrash }) {
                    Text(if (showTrash) "Active" else "Trash(${trashed.size})", color = accentColor, fontSize = 11.sp)
                }
                TextButton(onClick = {
                    app.soundManager.playTap()
                    if (photoPicker != null) {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        onCollapse()
                        MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                    }
                }) {
                    Text("+ Add", color = accentColor, fontSize = 11.sp)
                }
            }
        }

        val list = if (showTrash) trashed else active
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (showTrash) "Trash is empty" else "Screenshots auto-store here for 10 min",
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    if (!showTrash) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Drag image here to store · Tap stored image to attach to chat",
                            color = accentColor.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(list, key = { it.id }) { item ->
                    val remainingMs = (item.expiresAt - nowMs).coerceAtLeast(0L)
                    val dragMod = if (!item.isTrashed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Modifier.dragAndDropSource(
                            drawDragDecoration = {},
                            block = {
                                detectTapGestures(
                                    onLongPress = {
                                        val clip = app.vaultManager.buildDragClipData(item)
                                        if (clip != null) {
                                            startTransfer(
                                                DragAndDropTransferData(
                                                    clipData = clip,
                                                    flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                                                )
                                            )
                                        }
                                    },
                                    onTap = {
                                        app.soundManager.playTap()
                                        app.vaultManager.attachOrShareItem(item, targetChatPackage = foregroundChatPkg)
                                        onCollapse()
                                    }
                                )
                            }
                        )
                    } else {
                        Modifier.clickable {
                            if (!item.isTrashed) {
                                app.soundManager.playTap()
                                app.vaultManager.attachOrShareItem(item, targetChatPackage = foregroundChatPkg)
                                onCollapse()
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .then(dragMod)
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(File(item.filePath)),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp, 64.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (item.isTrashed) "Trashed" else TimerEngine.formatDuration(remainingMs),
                            style = TextStyle(
                                color = if (item.isTrashed) IslandColors.AccentRed else accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            )
                        )
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            if (item.isTrashed) {
                                IconButton(
                                    onClick = {
                                        app.soundManager.playVaultAdd()
                                        scope.launch { app.vaultManager.restoreItem(item.id) }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Restore, contentDescription = "Restore 10m", tint = IslandColors.AccentGreen, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        app.soundManager.playTap()
                                        app.vaultManager.attachOrShareItem(item, targetChatPackage = foregroundChatPkg)
                                        onCollapse()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Attach / Share", tint = IslandColors.TextPrimary, modifier = Modifier.size(15.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    app.soundManager.playTap()
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
            .height(216.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status, color = IslandColors.TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { app.soundManager.playTap(); app.clipboardMonitor.captureIfAllowed(isForeground = true) }) {
                Text("Capture", color = accentColor, fontSize = 12.sp)
            }
        }

        if (clips.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No clips captured in foreground yet", color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(clips, key = { it.id }) { clip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                app.soundManager.playTap()
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

    LaunchedEffect(Unit) {
        app.islandController.setKeyboardFocusNeeded(true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
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
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { if (it.isFocused) app.islandController.setKeyboardFocusNeeded(true) }
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty()) {
                        app.soundManager.playTap()
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

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(links.take(5), key = { "link_${it.id}" }) { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            app.soundManager.playTap()
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
                            app.soundManager.playTap()
                            app.clipboardMonitor.copyToClipboard(note.content)
                            app.islandController.showTransientBanner("Copied note")
                        }
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(note.content, color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = { app.soundManager.playTap(); scope.launch { app.database.noteDao().deleteById(note.id) } }, modifier = Modifier.size(28.dp)) {
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
            .height(216.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (ui.isRecording) IslandColors.AccentRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                .clickable {
                    app.soundManager.playTap()
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

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(memos, key = { it.id }) { memo ->
                val isPlayingThis = ui.playingMemoId == memo.id && ui.isPlaying
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { app.soundManager.playTap(); app.voiceMemoManager.togglePlayMemo(memo) },
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
                        onClick = { app.soundManager.playTap(); app.voiceMemoManager.deleteMemo(memo) },
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
            .height(216.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Island Alerts", color = IslandColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (state.alertsHistory.isNotEmpty()) {
                TextButton(onClick = { app.soundManager.playTap(); app.islandController.clearAlerts() }) {
                    Text("Clear", color = accentColor, fontSize = 12.sp)
                }
            }
        }

        if (state.alertsHistory.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Timer done, charging full, and vault expiry alerts appear here", color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
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
