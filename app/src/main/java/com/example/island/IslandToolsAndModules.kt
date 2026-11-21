package com.example.island

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Screenshot
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.DynamicIslandApp
import com.example.notes.NoteEntity
import com.example.ocr.MediaProjectionPromptActivity
import com.example.search.LaunchableApp
import com.example.search.SearchResults
import com.example.system.IslandSettings
import com.example.system.QuickToggleType
import com.example.system.TimerEngine
import com.example.ui.theme.IslandColors
import com.example.vault.VaultConstants
import com.example.vault.VaultItemEntity
import com.example.vault.VaultManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private sealed interface SearchListRow {
    data class SectionHeader(val letter: Char) : SearchListRow
    data class AppItem(val app: LaunchableApp) : SearchListRow
}

private enum class VaultFilter(val label: String) {
    ALL("All"),
    SCREENSHOTS("Screenshots"),
    PHOTOS("Photos"),
    TRASH("Trash")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color,
    onCollapse: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }
    val appsVersion by app.searchEngine.appsVersion.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeScrubLetter by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(Unit) {
        app.islandController.setKeyboardFocusNeeded(true)
    }

    LaunchedEffect(query, appsVersion) {
        if (query.isNotEmpty()) delay(110L)
        results = app.searchEngine.search(query)
    }

    // Build A-Z sectioned rows + index map for right-edge fast-scroller
    val (sectionRows, letterToRowIndex, availableLetters) = remember(results.apps) {
        val rows = mutableListOf<SearchListRow>()
        val letterMap = mutableMapOf<Char, Int>()
        val letters = mutableListOf<Char>()
        val grouped = results.apps.groupBy { it.sectionLetter }
        val sortedKeys = grouped.keys.sortedWith(compareBy<Char> { it == '#' }.thenBy { it })
        sortedKeys.forEach { letter ->
            letterMap[letter] = rows.size
            letters.add(letter)
            rows.add(SearchListRow.SectionHeader(letter))
            grouped[letter].orEmpty().forEach { item ->
                rows.add(SearchListRow.AppItem(item))
            }
        }
        Triple(rows, letterMap, letters)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search Google, all apps, notes, math...", fontSize = 12.sp) },
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
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("= ${results.inlineMathResult}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Tap to copy", color = IslandColors.TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (availableLetters.size > 1) 22.dp else 0.dp)
                    .testTag("search_app_opener_list")
            ) {
                if (query.isNotBlank()) {
                    item(key = "google_search_row") {
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
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("search_google_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Google Search", tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Search Google for \"$query\"", color = IslandColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (query.isNotBlank() && results.apps.isEmpty() && results.notes.isEmpty() && results.contacts.isEmpty() && results.inlineMathResult == null) {
                    item(key = "empty_search") {
                        Text(
                            text = "No apps or notes matching \"$query\"",
                            color = IslandColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                        )
                    }
                }

                items(
                    items = sectionRows,
                    key = { row ->
                        when (row) {
                            is SearchListRow.SectionHeader -> "hdr_${row.letter}"
                            is SearchListRow.AppItem -> "app_${row.app.packageName}"
                        }
                    }
                ) { row ->
                    when (row) {
                        is SearchListRow.SectionHeader -> {
                            Text(
                                text = row.letter.toString(),
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        is SearchListRow.AppItem -> {
                            val launchApp = row.app
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(11.dp))
                                    .combinedClickable(
                                        onClick = {
                                            app.soundManager.playTap()
                                            app.searchEngine.launchApp(launchApp.packageName)
                                            onCollapse()
                                        },
                                        onLongClick = {
                                            app.soundManager.playExpand()
                                            app.floatingWindowManager.launchAppInFloatingWindow(launchApp.packageName)
                                            app.islandController.showTransientBanner("Floating · ${launchApp.label}")
                                            onCollapse()
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 5.dp)
                                    .testTag("search_app_row_${launchApp.packageName}")
                            ) {
                                val bmp = launchApp.iconBitmap
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = launchApp.label, modifier = Modifier.size(26.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(launchApp.label, color = IslandColors.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Hold for floating window", color = IslandColors.TextTertiary, fontSize = 9.sp)
                                }
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = "Open ${launchApp.label} in floating window",
                                    tint = IslandColors.TextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            app.soundManager.playExpand()
                                            app.floatingWindowManager.launchAppInFloatingWindow(launchApp.packageName)
                                            onCollapse()
                                        }
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }

                items(results.notes, key = { "note_${it.id}" }) { note ->
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

                items(results.contacts, key = { "contact_${it.name}_${it.phone}" }) { contact ->
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

            // Right-edge touchable A-Z Fast-Scroller Strip
            if (availableLetters.size > 1) {
                var stripHeightPx by remember { mutableFloatStateOf(1f) }
                val headerOffset = if (query.isNotBlank()) 1 else 0
                val jumpToFraction: (Float) -> Unit = { yPx ->
                    val frac = (yPx / stripHeightPx).coerceIn(0f, 0.999f)
                    val idx = (frac * availableLetters.size).toInt().coerceIn(0, availableLetters.lastIndex)
                    val ch = availableLetters[idx]
                    if (activeScrubLetter != ch) {
                        activeScrubLetter = ch
                        val rowIdx = (letterToRowIndex[ch] ?: 0) + headerOffset
                        scope.launch { listState.scrollToItem(rowIdx) }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(20.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .onSizeChanged { stripHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                        .pointerInput(availableLetters) {
                            detectTapGestures { pos ->
                                jumpToFraction(pos.y)
                                activeScrubLetter = null
                            }
                        }
                        .pointerInput(availableLetters) {
                            detectVerticalDragGestures(
                                onDragStart = { pos -> jumpToFraction(pos.y) },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    jumpToFraction(change.position.y)
                                },
                                onDragEnd = { activeScrubLetter = null },
                                onDragCancel = { activeScrubLetter = null }
                            )
                        }
                        .padding(vertical = 2.dp)
                        .testTag("search_az_strip"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val displayedLetters = if (availableLetters.size <= 14) {
                        availableLetters
                    } else {
                        availableLetters.filterIndexed { i, _ -> i % 2 == 0 || i == availableLetters.lastIndex }
                    }
                    displayedLetters.forEach { letter ->
                        Text(
                            text = letter.toString(),
                            fontSize = 8.sp,
                            fontWeight = if (activeScrubLetter == letter) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeScrubLetter == letter) accentColor else IslandColors.TextSecondary,
                            lineHeight = 9.sp
                        )
                    }
                }
            }

            if (activeScrubLetter != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeScrubLetter.toString(),
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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
            .height(220.dp)
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
                        val nextOn = !toggleState.isTorchOn
                        app.soundManager.playToggle(isOn = nextOn)
                        app.quickTogglesManager.toggleFlashlight()
                        app.islandController.setFlashlightCardOpen(nextOn)
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

        if (toggleState.isTorchOn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Torch", color = IslandColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(42.dp))
                if (toggleState.maxTorchStrength > 1) {
                    Slider(
                        value = toggleState.torchStrength.toFloat(),
                        onValueChange = { app.quickTogglesManager.setTorchStrength(it.toInt()) },
                        valueRange = 1f..toggleState.maxTorchStrength.toFloat(),
                        steps = (toggleState.maxTorchStrength - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        modifier = Modifier.weight(1f).height(26.dp)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                TextButton(
                    onClick = {
                        onHaptic()
                        app.soundManager.playExpand()
                        app.islandController.setFlashlightCardOpen(true)
                    }
                ) {
                    Text("Beam Card", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
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

        // Screenshot -> Vault (replaces deleted Floating Apps button) + Select Mode OCR Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (settings.vaultEnabled) {
                Button(
                    onClick = {
                        onHaptic()
                        app.soundManager.playVaultAdd()
                        onCollapse()
                        MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("tools_screenshot_vault_btn")
                ) {
                    Icon(Icons.Filled.Screenshot, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Screenshot → Vault", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("tools_select_ocr_btn")
                ) {
                    Icon(Icons.Filled.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Select OCR", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Volumetric Flashlight Beam Overlay Card:
 * - Animated tapered Canvas beam with glow
 * - Vertical drag adjusts torch strength (when supported)
 * - Tap card to turn off torch and close
 */
@Composable
fun FlashlightBeamOverlayCard(
    app: DynamicIslandApp,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val toggleState by app.quickTogglesManager.state.collectAsState()
    var dragAccum by remember { mutableFloatStateOf(0f) }

    val infinite = rememberInfiniteTransition(label = "beam_pulse")
    val beamGlow by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beam_alpha"
    )

    val strengthFraction = if (toggleState.maxTorchStrength > 1) {
        (toggleState.torchStrength.toFloat() / toggleState.maxTorchStrength.toFloat()).coerceIn(0.2f, 1f)
    } else 0.85f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss)
            .testTag("flashlight_beam_overlay"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 68.dp, start = 16.dp, end = 16.dp)
                .width(268.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xF6111218))
                .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(36.dp))
                .pointerInput(toggleState.maxTorchStrength, toggleState.torchStrength) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        dragAccum += dragAmount
                        if (kotlin.math.abs(dragAccum) >= 26f && toggleState.maxTorchStrength > 1) {
                            val step = if (dragAccum < 0f) 1 else -1
                            dragAccum = 0f
                            app.quickTogglesManager.setTorchStrength(toggleState.torchStrength + step)
                        }
                    }
                }
                .padding(16.dp)
        ) {
            Text(
                text = if (toggleState.isTorchOn) "Flashlight Beam ON" else "Flashlight OFF",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (toggleState.maxTorchStrength > 1) {
                    "Swipe up/down to dim (${toggleState.torchStrength}/${toggleState.maxTorchStrength})"
                } else {
                    "Tap button below to switch torch"
                },
                color = IslandColors.TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val topW = size.width * 0.22f
                val botW = size.width * (0.58f + 0.34f * strengthFraction)
                val cx = size.width / 2f
                val beamPath = Path().apply {
                    moveTo(cx - topW / 2f, 0f)
                    lineTo(cx + topW / 2f, 0f)
                    lineTo(cx + botW / 2f, size.height)
                    lineTo(cx - botW / 2f, size.height)
                    close()
                }
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF4C7).copy(alpha = 0.85f * beamGlow * strengthFraction),
                            accentColor.copy(alpha = 0.26f * beamGlow),
                            Color.Transparent
                        )
                    )
                )
                drawCircle(
                    color = Color(0xFFFFF8DF).copy(alpha = 0.9f),
                    radius = 12.dp.toPx(),
                    center = Offset(cx, 10.dp.toPx())
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    app.soundManager.playToggle(isOn = !toggleState.isTorchOn)
                    app.quickTogglesManager.toggleFlashlight()
                    if (!app.quickTogglesManager.state.value.isTorchOn) {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (toggleState.isTorchOn) accentColor else Color.White.copy(alpha = 0.14f),
                    contentColor = if (toggleState.isTorchOn) Color.Black else Color.White
                ),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Icon(Icons.Filled.FlashlightOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (toggleState.isTorchOn) "Turn Torch Off" else "Turn Torch On", fontWeight = FontWeight.SemiBold)
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
    onCollapse: () -> Unit,
    onPreviewItem: ((VaultItemEntity) -> Unit)? = null
) {
    val active by app.vaultManager.activeItems.collectAsState(initial = emptyList())
    val trashed by app.vaultManager.trashedItems.collectAsState(initial = emptyList())
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val foregroundChatPkg by IslandService.currentForegroundApp.collectAsState()

    var selectedFilter by remember { mutableStateOf(VaultFilter.ALL) }
    var dialogPreviewItem by remember { mutableStateOf<VaultItemEntity?>(null) }
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

    val filteredItems = remember(active, trashed, selectedFilter) {
        when (selectedFilter) {
            VaultFilter.ALL -> active
            VaultFilter.SCREENSHOTS -> active.filter { it.sourceType.contains("SCREENSHOT", ignoreCase = true) }
            VaultFilter.PHOTOS -> active.filter { !it.sourceType.contains("SCREENSHOT", ignoreCase = true) }
            VaultFilter.TRASH -> trashed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(VaultFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        VaultFilter.ALL -> active.size
                        VaultFilter.SCREENSHOTS -> active.count { it.sourceType.contains("SCREENSHOT", ignoreCase = true) }
                        VaultFilter.PHOTOS -> active.count { !it.sourceType.contains("SCREENSHOT", ignoreCase = true) }
                        VaultFilter.TRASH -> trashed.size
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(IslandColors.ChipRadius))
                            .background(if (isSelected) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
                            .border(
                                0.75.dp,
                                if (isSelected) accentColor else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(IslandColors.ChipRadius)
                            )
                            .clickable {
                                app.soundManager.playTap()
                                selectedFilter = filter
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("vault_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            color = if (isSelected) accentColor else IslandColors.TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

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
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = IslandColors.AccentGreen, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Auto", color = IslandColors.AccentGreen, fontSize = 10.sp)
                    }
                }
                TextButton(
                    onClick = {
                        app.soundManager.playTap()
                        if (photoPicker != null) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            onCollapse()
                            MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                        }
                    }
                ) {
                    Text("+ Add", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedFilter == VaultFilter.TRASH) "Trash is empty (24h retention)" else "Screenshots auto-store here for 10 min",
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    if (selectedFilter != VaultFilter.TRASH) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap thumbnail to preview · Swipe to trash · Hold to drag",
                            color = accentColor.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("vault_thumbnail_grid")
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val remainingMs = (item.expiresAt - nowMs).coerceAtLeast(0L)
                    val fraction = (remainingMs.toFloat() / VaultConstants.EXPIRY_MILLIS.toFloat()).coerceIn(0f, 1f)
                    var swipeX by remember(item.id) { mutableFloatStateOf(0f) }

                    val openPreview = {
                        app.soundManager.playTap()
                        if (onPreviewItem != null) {
                            onPreviewItem(item)
                        } else {
                            dialogPreviewItem = item
                        }
                    }

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
                                    onTap = { openPreview() }
                                )
                            }
                        )
                    } else {
                        Modifier.clickable { openPreview() }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                            .graphicsLayer { translationX = swipeX }
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                            .pointerInput(item.id, item.isTrashed) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        swipeX = (swipeX + dragAmount).coerceIn(-120f, 120f)
                                    },
                                    onDragEnd = {
                                        if (kotlin.math.abs(swipeX) > 62f) {
                                            app.soundManager.playTap()
                                            scope.launch {
                                                if (item.isTrashed) app.vaultManager.restoreItem(item.id)
                                                else app.vaultManager.moveToTrash(item.id)
                                            }
                                        }
                                        swipeX = 0f
                                    },
                                    onDragCancel = { swipeX = 0f }
                                )
                            }
                            .then(dragMod)
                            .testTag("vault_grid_item_${item.id}")
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(File(item.filePath)),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Countdown ring + compact timer badge overlay
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawArc(
                                    color = Color.White.copy(alpha = 0.25f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawArc(
                                    color = if (item.isTrashed) IslandColors.AccentRed else accentColor,
                                    startAngle = -90f,
                                    sweepAngle = if (item.isTrashed) 360f else 360f * fraction,
                                    useCenter = false,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (item.isTrashed) "Trash" else TimerEngine.formatDuration(remainingMs),
                                style = TextStyle(
                                    color = if (item.isTrashed) IslandColors.AccentRed else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }

                        // Quick Restore/Trash button on top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable {
                                    app.soundManager.playTap()
                                    scope.launch {
                                        if (item.isTrashed) app.vaultManager.restoreItem(item.id)
                                        else app.vaultManager.moveToTrash(item.id)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.isTrashed) Icons.Filled.Restore else Icons.Filled.Delete,
                                contentDescription = if (item.isTrashed) "Restore" else "Trash",
                                tint = if (item.isTrashed) IslandColors.AccentGreen else IslandColors.AccentRed,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val localPreview = dialogPreviewItem
    if (localPreview != null) {
        Dialog(onDismissRequest = { dialogPreviewItem = null }) {
            VaultPreviewCardContent(
                item = localPreview,
                nowMs = nowMs,
                accentColor = accentColor,
                onAttach = {
                    app.soundManager.playTap()
                    app.vaultManager.attachOrShareItem(localPreview, targetChatPackage = foregroundChatPkg)
                    dialogPreviewItem = null
                    onCollapse()
                },
                onTrashOrRestore = {
                    app.soundManager.playTap()
                    scope.launch {
                        if (localPreview.isTrashed) app.vaultManager.restoreItem(localPreview.id)
                        else app.vaultManager.moveToTrash(localPreview.id)
                        dialogPreviewItem = null
                    }
                },
                onClose = { dialogPreviewItem = null }
            )
        }
    }
}

@Composable
fun VaultFullScreenPreviewOverlay(
    item: VaultItemEntity,
    app: DynamicIslandApp,
    accentColor: Color,
    onClose: () -> Unit,
    onCollapseIsland: () -> Unit
) {
    val foregroundChatPkg by IslandService.currentForegroundApp.collectAsState()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(item.id) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(onClick = onClose)
            .padding(20.dp)
            .testTag("vault_fullscreen_preview"),
        contentAlignment = Alignment.Center
    ) {
        VaultPreviewCardContent(
            item = item,
            nowMs = nowMs,
            accentColor = accentColor,
            onAttach = {
                app.soundManager.playTap()
                app.vaultManager.attachOrShareItem(item, targetChatPackage = foregroundChatPkg)
                onClose()
                onCollapseIsland()
            },
            onTrashOrRestore = {
                app.soundManager.playTap()
                scope.launch {
                    if (item.isTrashed) app.vaultManager.restoreItem(item.id)
                    else app.vaultManager.moveToTrash(item.id)
                    onClose()
                }
            },
            onClose = onClose
        )
    }
}

@Composable
private fun VaultPreviewCardContent(
    item: VaultItemEntity,
    nowMs: Long,
    accentColor: Color,
    onAttach: () -> Unit,
    onTrashOrRestore: () -> Unit,
    onClose: () -> Unit
) {
    val remainingMs = (item.expiresAt - nowMs).coerceAtLeast(0L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF13151C))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
            .clickable(enabled = false) {}
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.displayName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (item.isTrashed) "In 24h Trash" else "Expires in ${TimerEngine.formatDuration(remainingMs)}",
                    color = if (item.isTrashed) IslandColors.AccentRed else accentColor,
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = IslandColors.TextSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))

        Image(
            painter = rememberAsyncImagePainter(File(item.filePath)),
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTrashOrRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = if (item.isTrashed) IslandColors.AccentGreen else IslandColors.AccentRed
                ),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(
                    imageVector = if (item.isTrashed) Icons.Filled.Restore else Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (item.isTrashed) "Restore" else "Trash", fontSize = 12.sp)
            }
            if (!item.isTrashed) {
                Button(
                    onClick = onAttach,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    modifier = Modifier.weight(1.3f).height(44.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Attach / Share", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            .height(220.dp)
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
            .height(220.dp)
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
            .height(220.dp)
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
                    text = if (ui.isRecording) "Recording · ${TimerEngine.formatDuration(ui.recordingElapsedMs)} (Tap to stop)" else "Tap to Record Voice Memo",
                    color = IslandColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
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
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            app.soundManager.playTap()
                            app.voiceMemoManager.togglePlayMemo(memo)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play memo",
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
            .height(220.dp)
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
                            .clip(RoundedCornerShape(10.dp))
                            .pointerInput(alert.id) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                    onDragEnd = {
                                        if (totalDrag < -72f) {
                                            app.soundManager.playCollapse()
                                            app.islandController.dismissActiveNotification()
                                        } else if (totalDrag > 72f) {
                                            app.soundManager.playTap()
                                            app.islandController.nextNotification()
                                        }
                                    }
                                )
                            }
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
