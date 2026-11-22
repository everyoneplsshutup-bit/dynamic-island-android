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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.example.ui.theme.MotionTokens
import com.example.vault.VaultConstants
import com.example.vault.VaultItemEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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

    val isTyping = query.isNotBlank()
    val displayedApps = remember(results.apps, isTyping) {
        if (isTyping) results.apps.take(3) else results.apps
    }

    val (sectionRows, letterToRowIndex, availableLetters) = remember(displayedApps, isTyping) {
        val rows = mutableListOf<SearchListRow>()
        val letterMap = mutableMapOf<Char, Int>()
        val letters = mutableListOf<Char>()
        if (isTyping) {
            displayedApps.forEach { rows.add(SearchListRow.AppItem(it)) }
        } else {
            val grouped = displayedApps.groupBy { it.sectionLetter }
            val sortedKeys = grouped.keys.sortedWith(compareBy<Char> { it == '#' }.thenBy { it })
            sortedKeys.forEach { letter ->
                letterMap[letter] = rows.size
                letters.add(letter)
                rows.add(SearchListRow.SectionHeader(letter))
                grouped[letter].orEmpty().forEach { item ->
                    rows.add(SearchListRow.AppItem(item))
                }
            }
        }
        Triple(rows, letterMap, letters)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps, notes, math, Google...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor, modifier = Modifier.size(18.dp)) },
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
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp, max = 48.dp)
                .onFocusChanged { if (it.isFocused) app.islandController.setKeyboardFocusNeeded(true) }
                .testTag("island_search_input")
        )

        if (results.inlineMathResult != null) {
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .clickable {
                        app.soundManager.playTap()
                        app.clipboardMonitor.copyToClipboard(results.inlineMathResult!!)
                        app.islandController.showTransientBanner("Copied ${results.inlineMathResult}")
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("= ${results.inlineMathResult}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Tap to copy", color = IslandColors.TextSecondary, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (!isTyping && availableLetters.size > 1) 22.dp else 0.dp)
                    .testTag("search_app_opener_list")
            ) {
                if (isTyping) {
                    item(key = "google_search_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .clickable {
                                    app.soundManager.playSearchOpen()
                                    app.searchEngine.launchWebSearch(query, preferFloating = false)
                                    onCollapse()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("search_google_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Google Search", tint = accentColor, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Search Google for \"$query\"", color = IslandColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (isTyping && results.apps.isEmpty() && results.notes.isEmpty() && results.contacts.isEmpty() && results.inlineMathResult == null) {
                    item(key = "empty_search") {
                        Text(
                            text = "No matches for \"$query\"",
                            color = IslandColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        is SearchListRow.AppItem -> {
                            val launchApp = row.app
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
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
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("search_app_row_${launchApp.packageName}")
                            ) {
                                val bmp = launchApp.iconBitmap
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = launchApp.label, modifier = Modifier.size(24.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = launchApp.label,
                                    color = IslandColors.TextPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = "Open ${launchApp.label} in floating window",
                                    tint = IslandColors.TextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable {
                                            app.soundManager.playExpand()
                                            app.floatingWindowManager.launchAppInFloatingWindow(launchApp.packageName)
                                            onCollapse()
                                        }
                                        .padding(5.dp)
                                )
                            }
                        }
                    }
                }

                items(results.notes.take(if (isTyping) 2 else 3), key = { "note_${it.id}" }) { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                app.soundManager.playTap()
                                app.clipboardMonitor.copyToClipboard(note.content)
                                app.islandController.showTransientBanner("Copied note")
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Note: ${note.title}", color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                items(results.contacts.take(if (isTyping) 2 else 3), key = { "contact_${it.name}_${it.phone}" }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(contact.name, color = IslandColors.TextPrimary, fontSize = 12.sp)
                        Text(contact.phone, color = accentColor, fontSize = 12.sp)
                    }
                }
            }

            if (!isTyping && availableLetters.size > 1) {
                var stripHeightPx by remember { mutableFloatStateOf(1f) }
                val jumpToFraction: (Float) -> Unit = { yPx ->
                    val frac = (yPx / stripHeightPx).coerceIn(0f, 0.999f)
                    val idx = (frac * availableLetters.size).toInt().coerceIn(0, availableLetters.lastIndex)
                    val ch = availableLetters[idx]
                    if (activeScrubLetter != ch) {
                        activeScrubLetter = ch
                        val rowIdx = letterToRowIndex[ch] ?: 0
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
                    val displayedLetters = if (availableLetters.size <= 10) {
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeScrubLetter.toString(),
                        color = Color.Black,
                        fontSize = 18.sp,
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
            .heightIn(max = 160.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
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

                    QuickToggleType.KEEP_AWAKE -> Triple(
                        Icons.Filled.WbSunny,
                        toggleState.isKeepAwakeOn
                    ) {
                        onHaptic()
                        val next = app.quickTogglesManager.toggleKeepAwake()
                        app.soundManager.playToggle(isOn = next)
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
                Text("Torch", color = IslandColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp))
                if (toggleState.maxTorchStrength > 1) {
                    Slider(
                        value = toggleState.torchStrength.toFloat(),
                        onValueChange = { app.quickTogglesManager.setTorchStrength(it.toInt()) },
                        valueRange = 1f..toggleState.maxTorchStrength.toFloat(),
                        steps = (toggleState.maxTorchStrength - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        modifier = Modifier.weight(1f).height(22.dp)
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

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.BrightnessHigh, contentDescription = "Brightness", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            if (toggleState.hasWriteSettingsPermission) {
                Slider(
                    value = toggleState.brightness.toFloat(),
                    onValueChange = { app.quickTogglesManager.setScreenBrightness(it.toInt()) },
                    valueRange = 10f..255f,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                    modifier = Modifier.weight(1f).height(24.dp)
                )
            } else {
                TextButton(onClick = { app.soundManager.playTap(); app.quickTogglesManager.openWriteSettings() }) {
                    Text("Allow Modify System Settings for Brightness", color = accentColor, fontSize = 11.sp)
                }
            }
        }

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
                        .height(38.dp)
                        .testTag("tools_screenshot_vault_btn")
                ) {
                    Icon(Icons.Filled.Screenshot, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Screenshot → Shelf", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        .height(38.dp)
                        .testTag("tools_select_ocr_btn")
                ) {
                    Icon(Icons.Filled.SelectAll, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Select OCR", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

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
                        if (abs(dragAccum) >= 26f && toggleState.maxTorchStrength > 1) {
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
        targetValue = if (pressed) MotionTokens.BUTTON_PRESS_SCALE else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "toggle_tile_scale"
    )
    val radiusDp by animateDpAsState(
        targetValue = MotionTokens.pressRadiusDp(pressed, baseDp = 16f, squishedDp = 22f).dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "toggle_tile_radius"
    )
    val tileShape = RoundedCornerShape(radiusDp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(tileShape)
            .background(if (isActive) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.09f))
            .border(
                width = 0.75.dp,
                color = if (isActive) accentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
                shape = tileShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) accentColor else IslandColors.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(2.dp))
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
    var selectedItemId by remember { mutableStateOf<Long?>(null) }
    var dialogPreviewItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                    app.islandController.showTransientBanner("Auto-Shelf enabled for Screenshots")
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
            .heightIn(max = 152.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                selectedItemId = null
                                selectedFilter = filter
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
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

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectedFilter != VaultFilter.TRASH && active.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            app.soundManager.playCollapse()
                            selectedItemId = null
                            scope.launch { app.vaultManager.clearAllActiveItemsImmediate() }
                        }
                    ) {
                        Text("Clear all", color = IslandColors.AccentRed, fontSize = 10.sp)
                    }
                } else if (settings.vaultScreenshotsTreeUri.isNullOrBlank()) {
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
                            }
                        }
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = IslandColors.AccentGreen, modifier = Modifier.size(12.dp))
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
                    .height(98.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.22f),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        ),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedFilter == VaultFilter.TRASH) "Trash is empty" else "No images yet",
                        color = IslandColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (selectedFilter == VaultFilter.TRASH) "Deleted images remain 24h" else "Images auto-delete after 10 min",
                        color = IslandColors.TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 114.dp)
                    .testTag("vault_thumbnail_grid")
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val remainingMs = (item.expiresAt - nowMs).coerceAtLeast(0L)
                    val fraction = (remainingMs.toFloat() / VaultConstants.EXPIRY_MILLIS.toFloat()).coerceIn(0f, 1f)
                    val isSelected = selectedItemId == item.id
                    val isExpiredFade = !item.isTrashed && remainingMs == 0L
                    val ringColor = when {
                        item.isTrashed -> IslandColors.AccentRed
                        remainingMs < 60_000L -> IslandColors.AccentRed
                        remainingMs < 120_000L -> IslandColors.AccentAmber
                        else -> IslandColors.AccentLime
                    }
                    val tileScale by animateFloatAsState(
                        targetValue = when {
                            isExpiredFade -> 0.72f
                            isSelected -> 1.05f
                            else -> 1.0f
                        },
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 540f),
                        label = "shelf_tile_scale"
                    )
                    val tileAlpha by animateFloatAsState(
                        targetValue = if (isExpiredFade) 0f else 1f,
                        animationSpec = tween(220),
                        label = "shelf_tile_alpha"
                    )
                    var swipeX by remember(item.id) { mutableFloatStateOf(0f) }

                    val openPreview = {
                        if (selectedItemId == item.id) {
                            selectedItemId = null
                        } else {
                            app.soundManager.playTap()
                            if (onPreviewItem != null) {
                                onPreviewItem(item)
                            } else {
                                dialogPreviewItem = item
                            }
                        }
                    }

                    val dragGripMod = if (!item.isTrashed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Modifier.dragAndDropSource(
                            drawDragDecoration = {},
                            block = {
                                detectDragGestures(
                                    onDragStart = {
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
                                    onDrag = { _, _ -> }
                                )
                            }
                        )
                    } else {
                        Modifier
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = swipeX
                                scaleX = tileScale
                                scaleY = tileScale
                                alpha = tileAlpha
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.22f)
                                .pointerInput(item.id, item.isTrashed) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            swipeX = (swipeX + dragAmount).coerceIn(-120f, 120f)
                                        },
                                        onDragEnd = {
                                            if (abs(swipeX) > 62f) {
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .combinedClickable(
                                    onClick = openPreview,
                                    onLongClick = {
                                        app.soundManager.playTap()
                                        selectedItemId = if (selectedItemId == item.id) null else item.id
                                    }
                                )
                                .testTag("vault_grid_item_${item.id}")
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(File(item.filePath)),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(10.dp))
                            )

                            // Clean 2 dp perimeter countdown ring (zero text over photo!)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 2.dp.toPx()
                                val inset = strokePx / 2f
                                val r = 12.dp.toPx()
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.14f),
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - strokePx, size.height - strokePx),
                                    cornerRadius = CornerRadius(r, r),
                                    style = Stroke(width = strokePx)
                                )
                                val topBarLen = (size.width - strokePx) * fraction
                                if (topBarLen > 2f) {
                                    drawLine(
                                        color = ringColor,
                                        start = Offset(r * 0.5f, inset),
                                        end = Offset((r * 0.5f + topBarLen).coerceAtMost(size.width - r * 0.5f), inset),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            // 3-dot drag grip bottom-right
                            if (!item.isTrashed) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.58f))
                                        .then(dragGripMod),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragIndicator,
                                        contentDescription = "Drag to attach",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Long-press revealed × delete button ONLY when selected
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(IslandColors.AccentRed)
                                        .clickable {
                                            app.soundManager.playCollapse()
                                            selectedItemId = null
                                            scope.launch {
                                                if (item.isTrashed) app.vaultManager.purgeItemNow(item.id)
                                                else app.vaultManager.moveToTrash(item.id)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        // Countdown timer below thumbnail in 11 sp muted tabular text
                        Text(
                            text = if (item.isTrashed) "Trash" else TimerEngine.formatDuration(remainingMs),
                            style = TextStyle(
                                color = if (item.isTrashed || remainingMs < 60_000L) IslandColors.AccentRed else IslandColors.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = "tnum"
                            ),
                            maxLines = 1
                        )
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
                onSaveToGallery = {
                    app.soundManager.playTap()
                    scope.launch {
                        val saved = app.vaultManager.saveItemToGallery(localPreview)
                        app.islandController.showTransientBanner(
                            if (saved) "Saved to Pictures/DynamicIsland" else "Could not save image"
                        )
                    }
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
            onSaveToGallery = {
                app.soundManager.playTap()
                scope.launch {
                    val saved = app.vaultManager.saveItemToGallery(item)
                    app.islandController.showTransientBanner(
                        if (saved) "Saved to Pictures/DynamicIsland" else "Could not save image"
                    )
                }
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
    onSaveToGallery: () -> Unit,
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
                    text = if (item.isTrashed) "In 24h Trash" else "Shelf · Expires in ${TimerEngine.formatDuration(remainingMs)}",
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
                .height(248.dp)
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
                Spacer(Modifier.width(4.dp))
                Text(if (item.isTrashed) "Restore" else "Delete", fontSize = 12.sp)
            }
            if (!item.isTrashed) {
                Button(
                    onClick = onSaveToGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontSize = 12.sp)
                }
                Button(
                    onClick = onAttach,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                    modifier = Modifier.weight(1.1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Attach", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            .heightIn(max = 148.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status, color = IslandColors.TextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { app.soundManager.playTap(); app.clipboardMonitor.captureIfAllowed(isForeground = true) }) {
                Text("Capture", color = accentColor, fontSize = 11.sp)
            }
        }

        if (clips.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                Text("No clips captured in foreground yet", color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 104.dp)) {
                itemsIndexed(clips, key = { _, c -> c.id }) { idx, clip ->
                    var visible by remember(clip.id) { mutableStateOf(false) }
                    LaunchedEffect(clip.id) {
                        delay((idx * MotionTokens.STAGGER_STEP_MS).toLong().coerceAtMost(270L))
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(150)) + slideInVertically(tween(170)) { it / 3 }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    app.soundManager.playTap()
                                    app.clipboardMonitor.copyToClipboard(clip.content)
                                    app.islandController.showTransientBanner("Copied to clipboard")
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesIslandPanel(
    app: DynamicIslandApp,
    accentColor: Color
) {
    val notes by app.database.noteDao().observeAll().collectAsState(initial = emptyList())
    val links by app.database.savedLinkDao().observeAll().collectAsState(initial = emptyList())
    var editingId by remember { mutableStateOf<Long?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(isEditing) {
        app.islandController.setKeyboardFocusNeeded(isEditing)
    }

    fun persistDraft(text: String) {
        val clean = text.trim()
        scope.launch {
            if (clean.isEmpty()) {
                val curId = editingId
                if (curId != null && curId > 0L) {
                    app.database.noteDao().deleteById(curId)
                    editingId = null
                }
                return@launch
            }
            val title = clean.lineSequence().firstOrNull().orEmpty().take(36).ifBlank { "Untitled Note" }
            val savedId = app.database.noteDao().upsert(
                NoteEntity(
                    id = editingId ?: 0L,
                    title = title,
                    content = text,
                    updatedAt = System.currentTimeMillis()
                )
            )
            if (editingId == null || editingId == 0L) {
                editingId = savedId
            }
        }
    }

    AnimatedContent(
        targetState = isEditing,
        label = "notes_mode_switch",
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 148.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) { editing ->
        if (editing) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                app.soundManager.playTap()
                                isEditing = false
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Notes", tint = IslandColors.AccentLime, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Notes", color = IslandColors.AccentLime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = {
                            app.soundManager.playCollapse()
                            val curId = editingId
                            if (curId != null && curId > 0L) {
                                scope.launch { app.database.noteDao().deleteById(curId) }
                            }
                            editingId = null
                            draftText = ""
                            isEditing = false
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = IslandColors.AccentRed, modifier = Modifier.size(16.dp))
                    }
                }
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { next ->
                        draftText = next
                        persistDraft(next)
                    },
                    placeholder = { Text("Start typing — auto-saved on every keystroke...", fontSize = 12.sp) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IslandColors.TextPrimary,
                        unfocusedTextColor = IslandColors.TextPrimary,
                        focusedBorderColor = IslandColors.AccentLime,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp, max = 104.dp)
                        .onFocusChanged { if (it.isFocused) app.islandController.setKeyboardFocusNeeded(true) }
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${notes.size} Notes",
                        color = IslandColors.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(IslandColors.AccentLime)
                            .clickable {
                                app.soundManager.playTap()
                                editingId = null
                                draftText = ""
                                isEditing = true
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("New note", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (notes.isEmpty() && links.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                        Text("No notes yet", color = IslandColors.TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 108.dp)
                    ) {
                        items(links.take(2), key = { "link_${it.id}" }) { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .clickable {
                                        app.soundManager.playTap()
                                        if (link.urlOrUri.startsWith("http")) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.urlOrUri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${link.itemType}: ${link.title}", color = IslandColors.TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        items(notes, key = { "note_${it.id}" }) { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp, max = 58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .combinedClickable(
                                        onClick = {
                                            app.soundManager.playTap()
                                            editingId = note.id
                                            draftText = note.content
                                            isEditing = true
                                        },
                                        onLongClick = {
                                            app.soundManager.playCollapse()
                                            scope.launch { app.database.noteDao().deleteById(note.id) }
                                        }
                                    )
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(IslandColors.AccentLime)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled" },
                                        color = IslandColors.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = note.content,
                                        color = IslandColors.TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Today, ${timeFmt.format(Date(note.updatedAt))}",
                                    color = IslandColors.TextTertiary,
                                    fontSize = 9.sp
                                )
                            }
                        }
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
            .heightIn(max = 148.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (ui.isRecording) IslandColors.AccentRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                .clickable {
                    app.soundManager.playTap()
                    if (ui.isRecording) {
                        app.voiceMemoManager.stopRecording()
                    } else if (!app.voiceMemoManager.startRecording()) {
                        Toast.makeText(context, "Grant Microphone permission in Setup Wizard", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ui.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = "Voice Memo",
                    tint = if (ui.isRecording) IslandColors.AccentRed else accentColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (ui.isRecording) "Recording · ${TimerEngine.formatDuration(ui.recordingElapsedMs)} (Tap to stop)" else "Tap to Record Voice Memo",
                    color = IslandColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 92.dp)) {
            items(memos, key = { it.id }) { memo ->
                val isPlayingThis = ui.playingMemoId == memo.id && ui.isPlaying
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            app.soundManager.playTap()
                            app.voiceMemoManager.togglePlayMemo(memo)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play memo",
                            tint = accentColor
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(memo.title, color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(TimerEngine.formatDuration(memo.durationMs), color = IslandColors.TextSecondary, fontSize = 10.sp)
                    }
                    IconButton(
                        onClick = { app.soundManager.playTap(); app.voiceMemoManager.deleteMemo(memo) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete memo", tint = IslandColors.TextSecondary, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

private val OTP_REGEX = Regex("""\b(\d{4,8})\b""")

@Composable
fun AlertsIslandPanel(
    app: DynamicIslandApp,
    state: IslandState,
    accentColor: Color
) {
    val expandedApps = remember { mutableStateMapOf<String, Boolean>() }
    val groupedNotifs = remember(state.notificationsHistory) {
        state.notificationsHistory.groupBy { it.packageName }
    }
    val totalCount = state.notificationsHistory.size + state.alertsHistory.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 144.dp)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        // Sticky header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notifications", color = IslandColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (totalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.24f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(totalCount.toString(), color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.undoableClearedInbox != null) {
                    TextButton(onClick = { app.islandController.undoInboxClear() }) {
                        Text("Undo", color = IslandColors.AccentLime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (totalCount > 0) {
                    TextButton(onClick = { app.islandController.clearInboxWithUndo() }) {
                        Text("Clear all", color = IslandColors.AccentRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (totalCount == 0) {
            Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                Text("No notifications", color = IslandColors.TextSecondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 104.dp)
            ) {
                groupedNotifs.forEach { (pkg, itemsForApp) ->
                    val first = itemsForApp.first()
                    val isExpanded = expandedApps[pkg] == true
                    item(key = "grp_$pkg") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(pkg) {
                                        var dx = 0f
                                        detectHorizontalDragGestures(
                                            onDragStart = { dx = 0f },
                                            onHorizontalDrag = { _, d -> dx += d },
                                            onDragEnd = {
                                                if (dx < -72f) {
                                                    app.islandController.dismissAppGroup(pkg)
                                                }
                                            }
                                        )
                                    }
                                    .clickable { expandedApps[pkg] = !isExpanded }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${first.appName.ifBlank { pkg }} (${itemsForApp.size})",
                                        color = IslandColors.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    if (!isExpanded) {
                                        Text(
                                            text = first.title.ifBlank { first.text },
                                            color = IslandColors.TextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = IslandColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isExpanded) {
                                Spacer(Modifier.height(4.dp))
                                itemsForApp.forEach { n ->
                                    val otp = OTP_REGEX.find("${n.title} ${n.text}")?.groupValues?.getOrNull(1)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .pointerInput(n.id) {
                                                var dx = 0f
                                                detectHorizontalDragGestures(
                                                    onDragStart = { dx = 0f },
                                                    onHorizontalDrag = { _, d -> dx += d },
                                                    onDragEnd = {
                                                        if (dx < -64f) app.islandController.dismissNotificationById(n.id)
                                                    }
                                                )
                                            }
                                            .clickable {
                                                app.soundManager.playTap()
                                                app.searchEngine.launchApp(n.packageName)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 3.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(n.title, color = IslandColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (n.text.isNotBlank()) {
                                                Text(n.text, color = IslandColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (otp != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.22f))
                                                    .clickable {
                                                        app.soundManager.playTap()
                                                        app.clipboardMonitor.copyToClipboard(otp)
                                                        app.islandController.showTransientBanner("Copied OTP")
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text("••••${otp.takeLast(2)} Copy", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
                                            app.islandController.dismissActiveNotification()
                                        }
                                    }
                                )
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (alert.kind == AlertKind.LOW_BATTERY) IslandColors.AccentRed else accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, color = IslandColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(alert.subtitle, color = IslandColors.TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
