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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var allInstalledApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    val appsVersion by app.searchEngine.appsVersion.collectAsState()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var activeScrubLetter by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(Unit) {
        app.islandController.setKeyboardFocusNeeded(true)
    }

    LaunchedEffect(appsVersion) {
        allInstalledApps = app.searchEngine.getInstalledApps()
    }

    LaunchedEffect(query, appsVersion) {
        if (query.isNotEmpty()) delay(100L)
        results = app.searchEngine.search(query)
    }

    val pinnedPackages = settings.pinnedAppPackages
    val pinnedPackageSet = remember(pinnedPackages) { pinnedPackages.toSet() }
    val pinnedApps = remember(allInstalledApps, results.apps, pinnedPackages, query) {
        val pool = (allInstalledApps + results.apps).associateBy { it.packageName }
        val ordered = pinnedPackages.mapNotNull { pool[it] }
        val q = query.trim()
        if (q.isEmpty()) ordered else ordered.filter {
            it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }
    }

    val isTyping = query.isNotBlank()
    val displayedApps = remember(results.apps, isTyping) {
        if (isTyping) results.apps.take(4) else results.apps
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

    var prevPinnedCount by remember { mutableStateOf(pinnedPackages.size) }
    LaunchedEffect(pinnedPackages.size) {
        if (pinnedPackages.size > prevPinnedCount) {
            listState.scrollToItem(0)
        }
        prevPinnedCount = pinnedPackages.size
    }

    val togglePin: (String) -> Unit = { pkg ->
        app.soundManager.playToggle(isOn = pkg !in pinnedPackageSet)
        scope.launch {
            app.settingsRepository.update { cur ->
                val updated = if (pkg in cur.pinnedAppPackages) {
                    cur.pinnedAppPackages.filterNot { it == pkg }
                } else {
                    (cur.pinnedAppPackages + pkg).take(12)
                }
                cur.copy(pinnedAppPackages = updated)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 232.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Roomy Liquid Glass search bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(
                    width = 0.9.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.34f), accentColor.copy(alpha = 0.56f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f)
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search apps, math, notes...",
                        color = IslandColors.TextTertiary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = IslandColors.TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        app.soundManager.playSearchOpen()
                        app.searchEngine.launchWebSearch(query, preferFloating = false)
                        onCollapse()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) app.islandController.setKeyboardFocusNeeded(true) }
                        .testTag("island_search_input")
                )
            }
            if (results.inlineMathResult != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.24f))
                        .clickable {
                            app.soundManager.playTap()
                            app.clipboardMonitor.copyToClipboard(results.inlineMathResult!!)
                            app.islandController.showTransientBanner("Copied ${results.inlineMathResult}")
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("= ${results.inlineMathResult}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
            }
            if (query.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { query = "" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = IslandColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (pinnedApps.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .horizontalScroll(rememberScrollState())
                    .testTag("search_pinned_section")
            ) {
                Text(
                    text = "Pinned (${pinnedApps.size})",
                    color = IslandColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 2.dp)
                )
                pinnedApps.forEach { pinned ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.75.dp, accentColor.copy(alpha = 0.55f), CircleShape)
                            .combinedClickable(
                                onClick = {
                                    app.soundManager.playTap()
                                    app.searchEngine.launchApp(pinned.packageName)
                                    onCollapse()
                                },
                                onLongClick = {
                                    app.soundManager.playExpand()
                                    app.floatingWindowManager.launchAppInFloatingWindow(pinned.packageName)
                                    onCollapse()
                                }
                            )
                            .padding(horizontal = 8.dp)
                            .testTag("search_pinned_chip_${pinned.packageName}")
                    ) {
                        val bmp = pinned.iconBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = pinned.label,
                                modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = pinned.label,
                            color = IslandColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Unpin ${pinned.label}",
                            tint = IslandColors.TextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { togglePin(pinned.packageName) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (!isTyping && availableLetters.size > 1) 30.dp else 0.dp)
                    .testTag("search_app_opener_list")
            ) {
                if (isTyping) {
                    item(key = "google_search_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    app.soundManager.playSearchOpen()
                                    app.searchEngine.launchWebSearch(query, preferFloating = false)
                                    onCollapse()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("search_google_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Google Search", tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Google \"$query\"", color = IslandColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (isTyping && results.apps.isEmpty() && results.notes.isEmpty() && results.contacts.isEmpty() && results.inlineMathResult == null) {
                    item(key = "empty_search") {
                        Text(
                            text = "No matches for \"$query\"",
                            color = IslandColors.TextSecondary,
                            fontSize = 13.sp,
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        is SearchListRow.AppItem -> {
                            val launchApp = row.app
                            val isPinned = launchApp.packageName in pinnedPackageSet
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
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
                                    Image(bitmap = bmp, contentDescription = launchApp.label, modifier = Modifier.size(28.dp).clip(CircleShape))
                                } else {
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = launchApp.label,
                                    color = IslandColors.TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable { togglePin(launchApp.packageName) }
                                        .testTag("search_pin_${launchApp.packageName}")
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = if (isPinned) "Unpin ${launchApp.label}" else "Pin ${launchApp.label}",
                                        tint = if (isPinned) accentColor else IslandColors.TextSecondary.copy(alpha = 0.78f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            app.soundManager.playExpand()
                                            app.floatingWindowManager.launchAppInFloatingWindow(launchApp.packageName)
                                            onCollapse()
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInNew,
                                        contentDescription = "Open ${launchApp.label} in floating window",
                                        tint = IslandColors.TextSecondary.copy(alpha = 0.78f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = IslandColors.TextSecondary.copy(alpha = 0.65f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                items(results.notes.take(2), key = { "note_${it.id}" }) { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                app.soundManager.playTap()
                                app.clipboardMonitor.copyToClipboard(note.content)
                                app.islandController.showTransientBanner("Copied note")
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Note: ${note.title}", color = IslandColors.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                items(results.contacts.take(2), key = { "contact_${it.name}_${it.phone}" }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 38.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(contact.name, color = IslandColors.TextPrimary, fontSize = 13.sp)
                        Text(contact.phone, color = accentColor, fontSize = 13.sp)
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
                        .padding(end = 2.dp)
                        .width(22.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(0.75.dp, accentColor.copy(alpha = 0.35f), CircleShape)
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
                        .padding(vertical = 4.dp)
                        .testTag("search_az_strip"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val displayedLetters = if (availableLetters.size <= 10) {
                        availableLetters
                    } else {
                        val step = (availableLetters.size / 8).coerceAtLeast(2)
                        availableLetters.filterIndexed { i, _ -> i % step == 0 || i == availableLetters.lastIndex }.take(10)
                    }
                    displayedLetters.forEach { letter ->
                        Text(
                            text = letter.toString(),
                            fontSize = 9.sp,
                            fontWeight = if (activeScrubLetter == letter) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeScrubLetter == letter) Color.White else accentColor,
                            lineHeight = 10.sp,
                            maxLines = 1
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
                        color = Color.White,
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

    val gridToggles = remember(settings.includedQuickToggles) {
        val primary = settings.includedQuickToggles.filter {
            it != QuickToggleType.SELECT_OCR && it != QuickToggleType.VAULT_CAPTURE
        }
        (if (primary.isNotEmpty()) primary else settings.includedQuickToggles).take(6)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 232.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridToggles.chunked(3).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { type ->
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
                                app.soundManager.playError()
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
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        // Roomy Brightness / Torch strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            Icon(Icons.Filled.BrightnessHigh, contentDescription = "Brightness", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            if (toggleState.hasWriteSettingsPermission) {
                var barWidthPx by remember { mutableFloatStateOf(1f) }
                val frac = ((toggleState.brightness - 10f) / 245f).coerceIn(0f, 1f)
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val f = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                app.quickTogglesManager.setScreenBrightness((10 + f * 245f).toInt())
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(frac.coerceAtLeast(0.04f))
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                }
            } else {
                Text(
                    text = "Allow brightness control",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { app.soundManager.playTap(); app.quickTogglesManager.openWriteSettings() }
                )
                Spacer(Modifier.weight(1f))
            }
            if (toggleState.isTorchOn) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.24f))
                        .clickable {
                            onHaptic()
                            app.soundManager.playExpand()
                            app.islandController.setFlashlightCardOpen(true)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Beam", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (settings.vaultEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .clickable {
                            onHaptic()
                            app.soundManager.playVaultAdd()
                            onCollapse()
                            MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                        }
                        .padding(horizontal = 8.dp)
                        .testTag("tools_screenshot_vault_btn")
                ) {
                    Icon(Icons.Filled.Screenshot, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Screenshot → Shelf", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }
            if (settings.ocrFeatureEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(CircleShape)
                        .background(if (settings.screenReadingEnabled) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.10f))
                        .border(0.75.dp, if (settings.screenReadingEnabled) accentColor else Color.White.copy(alpha = 0.20f), CircleShape)
                        .clickable {
                            onHaptic()
                            if (!settings.screenReadingEnabled) {
                                app.soundManager.playError()
                                app.islandController.showTransientBanner("Turn ON Screen Reading in Settings → Privacy")
                            } else {
                                app.soundManager.playToggle(isOn = true)
                                app.islandController.setSelectMode(true)
                            }
                        }
                        .padding(horizontal = 8.dp)
                        .testTag("tools_select_ocr_btn")
                ) {
                    Icon(Icons.Filled.SelectAll, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Select OCR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
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

    val beamGlow = if (toggleState.isTorchOn) 1.0f else 0.35f

    val strengthFraction = if (toggleState.maxTorchStrength > 1) {
        (toggleState.torchStrength.toFloat() / toggleState.maxTorchStrength.toFloat()).coerceIn(0.2f, 1f)
    } else 0.85f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("flashlight_beam_overlay"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .width(280.dp)
                .heightIn(max = 152.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xF6111218))
                .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(32.dp))
                .pointerInput(toggleState.maxTorchStrength, toggleState.torchStrength) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        dragAccum += dragAmount
                        if (abs(dragAccum) >= 24f && toggleState.maxTorchStrength > 1) {
                            val step = if (dragAccum < 0f) 1 else -1
                            dragAccum = 0f
                            app.quickTogglesManager.setTorchStrength(toggleState.torchStrength + step)
                        }
                    }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (toggleState.isTorchOn) "Flashlight Beam ON" else "Flashlight OFF",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (toggleState.maxTorchStrength > 1) {
                            "Swipe vertically · ${toggleState.torchStrength}/${toggleState.maxTorchStrength}"
                        } else {
                            "Compact pill torch control"
                        },
                        color = IslandColors.TextSecondary,
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close beam card", tint = IslandColors.TextSecondary, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                val topW = size.width * 0.22f
                val botW = size.width * (0.55f + 0.32f * strengthFraction)
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
                    radius = 7.dp.toPx(),
                    center = Offset(cx, 6.dp.toPx())
                )
            }

            Spacer(Modifier.height(6.dp))

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
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Icon(Icons.Filled.FlashlightOn, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (toggleState.isTorchOn) "Turn Torch Off" else "Turn Torch On", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (pressed) MotionTokens.TAP_SQUISH_SCALE else 1.0f,
        animationSpec = MotionTokens.spatialFast(),
        label = "toggle_tile_scale"
    )
    val tileShape = RoundedCornerShape(14.dp)
    val tileBackground = if (isActive) {
        Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.44f),
                accentColor.copy(alpha = 0.16f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scaleState.value
                scaleY = scaleState.value
            }
            .clip(tileShape)
            .background(tileBackground)
            .border(
                width = 0.9.dp,
                brush = Brush.linearGradient(
                    colors = if (isActive) {
                        listOf(Color.White.copy(alpha = 0.55f), accentColor.copy(alpha = 0.85f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.08f))
                    }
                ),
                shape = tileShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.White else accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = IslandColors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
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

    var selectedFilter by remember { mutableStateOf(VaultFilter.ALL) }
    var selectedItemId by remember { mutableStateOf<Long?>(null) }
    var detailItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var croppingItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var cropLeft by remember { mutableFloatStateOf(0.10f) }
    var cropTop by remember { mutableFloatStateOf(0.10f) }
    var cropRight by remember { mutableFloatStateOf(0.90f) }
    var cropBottom by remember { mutableFloatStateOf(0.90f) }
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
                scope.launch {
                    val saved = app.vaultManager.saveFromUri(uri, sourceType = "PICKED")
                    if (saved == null) {
                        app.soundManager.playError()
                        app.islandController.showTransientBanner("Could not add image to Shelf")
                    }
                }
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
            .heightIn(min = 160.dp, max = 232.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        val cropTarget = croppingItem
        val inspectTarget = detailItem

        if (cropTarget != null) {
            // Roomy Inline Shelf Crop Mode
            Row(
                modifier = Modifier.fillMaxWidth().height(28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Crop · ${cropTarget.displayName}",
                    color = IslandColors.TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable {
                                app.soundManager.playTap()
                                cropLeft = 0.10f; cropTop = 0.10f; cropRight = 0.90f; cropBottom = 0.90f
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("Free", color = Color.White, fontSize = 9.sp) }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable {
                                app.soundManager.playTap()
                                cropLeft = 0.18f; cropTop = 0.18f; cropRight = 0.82f; cropBottom = 0.82f
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("1:1", color = Color.White, fontSize = 9.sp) }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable {
                                app.soundManager.playTap()
                                cropLeft = 0.12f; cropTop = 0.18f; cropRight = 0.88f; cropBottom = 0.82f
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("4:3", color = Color.White, fontSize = 9.sp) }
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Crop Canvas inside Shelf
                var canvasW by remember { mutableFloatStateOf(1f) }
                var canvasH by remember { mutableFloatStateOf(1f) }
                var activeCorner by remember(cropTarget.id) { mutableStateOf(0) } // 1=TL,2=TR,3=BL,4=BR,5=PAN
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .onSizeChanged {
                            canvasW = it.width.toFloat().coerceAtLeast(1f)
                            canvasH = it.height.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(cropTarget.id) {
                            detectDragGestures(
                                onDragStart = { pos ->
                                    val fx = (pos.x / canvasW).coerceIn(0f, 1f)
                                    val fy = (pos.y / canvasH).coerceIn(0f, 1f)
                                    val corners = listOf(
                                        1 to (abs(fx - cropLeft) + abs(fy - cropTop)),
                                        2 to (abs(fx - cropRight) + abs(fy - cropTop)),
                                        3 to (abs(fx - cropLeft) + abs(fy - cropBottom)),
                                        4 to (abs(fx - cropRight) + abs(fy - cropBottom))
                                    )
                                    val nearest = corners.minByOrNull { it.second }
                                    activeCorner = if (nearest != null && nearest.second < 0.30f) nearest.first else 5
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val dx = drag.x / canvasW
                                    val dy = drag.y / canvasH
                                    val minSize = 0.16f
                                    when (activeCorner) {
                                        1 -> {
                                            cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                            cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                        }
                                        2 -> {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                            cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                        }
                                        3 -> {
                                            cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                        }
                                        4 -> {
                                            cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                            cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                        }
                                        else -> {
                                            val w = cropRight - cropLeft
                                            val h = cropBottom - cropTop
                                            val nl = (cropLeft + dx).coerceIn(0f, 1f - w)
                                            val nt = (cropTop + dy).coerceIn(0f, 1f - h)
                                            cropLeft = nl
                                            cropRight = nl + w
                                            cropTop = nt
                                            cropBottom = nt + h
                                        }
                                    }
                                },
                                onDragEnd = { activeCorner = 0 },
                                onDragCancel = { activeCorner = 0 }
                            )
                        }
                        .testTag("shelf_crop_canvas")
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(File(cropTarget.filePath)),
                        contentDescription = cropTarget.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val l = cropLeft * size.width
                        val t = cropTop * size.height
                        val r = cropRight * size.width
                        val b = cropBottom * size.height
                        val cw = (r - l).coerceAtLeast(1f)
                        val ch = (b - t).coerceAtLeast(1f)
                        val scrim = Color.Black.copy(alpha = 0.56f)
                        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, t))
                        drawRect(scrim, topLeft = Offset(0f, b), size = Size(size.width, (size.height - b).coerceAtLeast(0f)))
                        drawRect(scrim, topLeft = Offset(0f, t), size = Size(l, ch))
                        drawRect(scrim, topLeft = Offset(r, t), size = Size((size.width - r).coerceAtLeast(0f), ch))

                        drawRect(
                            color = accentColor,
                            topLeft = Offset(l, t),
                            size = Size(cw, ch),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Rule of thirds subtle lines
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(l + cw / 3f, t), Offset(l + cw / 3f, b), strokeWidth = 0.7.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(l + 2f * cw / 3f, t), Offset(l + 2f * cw / 3f, b), strokeWidth = 0.7.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(l, t + ch / 3f), Offset(r, t + ch / 3f), strokeWidth = 0.7.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.3f), Offset(l, t + 2f * ch / 3f), Offset(r, t + 2f * ch / 3f), strokeWidth = 0.7.dp.toPx())
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.width(68.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(23.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .clickable {
                                val target = cropTarget
                                scope.launch {
                                    val updated = app.vaultManager.cropVaultItem(target.id, cropLeft, cropTop, cropRight, cropBottom)
                                    if (updated != null) {
                                        app.soundManager.playVaultAdd()
                                        detailItem = updated
                                        app.islandController.showTransientBanner("Cropped photo saved")
                                    } else {
                                        app.soundManager.playError()
                                        app.islandController.showTransientBanner("Could not crop image")
                                    }
                                    croppingItem = null
                                }
                            }
                            .testTag("shelf_crop_apply_btn")
                    ) {
                        Text("Apply", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(23.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable {
                                app.soundManager.playTap()
                                croppingItem = null
                            }
                            .testTag("shelf_crop_cancel_btn")
                    ) {
                        Text("Cancel", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        } else if (inspectTarget != null) {
            // Compact Inline Detail View inside Shelf (Crop, Attach, Save, Del/Restore, Back)
            val remainingMs = (inspectTarget.expiresAt - nowMs).coerceAtLeast(0L)
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            app.soundManager.playTap()
                            detailItem = null
                        }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Shelf", tint = accentColor, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(inspectTarget.displayName, color = IslandColors.TextPrimary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(120.dp))
                }
                Text(
                    text = if (inspectTarget.isTrashed) "Trash · 24h" else "Expires ${TimerEngine.formatDuration(remainingMs)}",
                    color = if (inspectTarget.isTrashed) IslandColors.AccentRed else accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(File(inspectTarget.filePath)),
                    contentDescription = inspectTarget.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (!inspectTarget.isTrashed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.22f))
                                    .clickable {
                                        app.soundManager.playTap()
                                        cropLeft = 0.10f; cropTop = 0.10f; cropRight = 0.90f; cropBottom = 0.90f
                                        croppingItem = inspectTarget
                                    }
                                    .testTag("shelf_crop_open_btn")
                            ) {
                                Icon(Icons.Filled.Crop, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Crop", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .clickable {
                                        app.soundManager.playTap()
                                        app.vaultManager.attachOrShareItem(inspectTarget, targetChatPackage = foregroundChatPkg)
                                        detailItem = null
                                        onCollapse()
                                    }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Attach", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    app.soundManager.playTap()
                                    scope.launch {
                                        if (inspectTarget.isTrashed) app.vaultManager.restoreItem(inspectTarget.id)
                                        else app.vaultManager.moveToTrash(inspectTarget.id)
                                        detailItem = null
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = if (inspectTarget.isTrashed) Icons.Filled.Restore else Icons.Filled.Delete,
                                contentDescription = null,
                                tint = if (inspectTarget.isTrashed) IslandColors.AccentGreen else IslandColors.AccentRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (inspectTarget.isTrashed) "Restore" else "Trash",
                                color = if (inspectTarget.isTrashed) IslandColors.AccentGreen else IslandColors.AccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (!inspectTarget.isTrashed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable {
                                    app.soundManager.playTap()
                                    scope.launch {
                                        val saved = app.vaultManager.saveItemToGallery(inspectTarget)
                                        if (!saved) app.soundManager.playError()
                                        app.islandController.showTransientBanner(
                                            if (saved) "Saved to Pictures/DynamicIsland" else "Could not save image"
                                        )
                                    }
                                }
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save to Gallery", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } else {
            // Browse Shelf Mode: Row 1 = Filter tabs (never clipped), Row 2 = Auto/Clear + "+ Add Photo"
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                VaultFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        VaultFilter.ALL -> active.size
                        VaultFilter.SCREENSHOTS -> active.count { it.sourceType.contains("SCREENSHOT", ignoreCase = true) }
                        VaultFilter.PHOTOS -> active.count { !it.sourceType.contains("SCREENSHOT", ignoreCase = true) }
                        VaultFilter.TRASH -> trashed.size
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(IslandColors.ChipRadius))
                            .background(if (isSelected) accentColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
                            .border(
                                0.85.dp,
                                if (isSelected) accentColor else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(IslandColors.ChipRadius)
                            )
                            .clickable {
                                app.soundManager.playTap()
                                selectedItemId = null
                                selectedFilter = filter
                            }
                            .padding(horizontal = 10.dp)
                            .testTag("vault_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            color = if (isSelected) accentColor else IslandColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedFilter != VaultFilter.TRASH && active.isNotEmpty()) {
                    Text(
                        text = "Clear Active",
                        color = IslandColors.AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                app.soundManager.playCollapse()
                                selectedItemId = null
                                scope.launch { app.vaultManager.clearAllActiveItemsImmediate() }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
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
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = IslandColors.AccentGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (settings.vaultScreenshotsTreeUri.isNullOrBlank()) "Auto-Capture Folder" else "Auto-Capture ON",
                            color = IslandColors.AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.24f))
                        .border(0.85.dp, accentColor.copy(alpha = 0.70f), CircleShape)
                        .clickable {
                            app.soundManager.playTap()
                            if (photoPicker != null) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else {
                                onCollapse()
                                MediaProjectionPromptActivity.launchForVaultScreenshot(context)
                            }
                        }
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "+ Add",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.22f),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            ),
                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                        )
                    }
                    Text(
                        text = if (selectedFilter == VaultFilter.TRASH) {
                            "Trash empty (24h restore window)"
                        } else {
                            "Tap + Add · Items auto-delete after 10 min"
                        },
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vault_thumbnail_grid")
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val remainingMs = (item.expiresAt - nowMs).coerceAtLeast(0L)
                        val fraction = (remainingMs.toFloat() / VaultConstants.EXPIRY_MILLIS.toFloat()).coerceIn(0f, 1f)
                        val isSelected = selectedItemId == item.id
                        val isFocusedTile = isSelected || (selectedItemId == null && filteredItems.firstOrNull()?.id == item.id)
                        val isExpiredFade = !item.isTrashed && remainingMs == 0L
                        val ringColor = when {
                            item.isTrashed -> IslandColors.AccentRed
                            remainingMs < 60_000L -> IslandColors.AccentRed
                            remainingMs < 120_000L -> IslandColors.AccentAmber
                            else -> IslandColors.AccentLime
                        }
                        val tileScaleState = animateFloatAsState(
                            targetValue = when {
                                isExpiredFade -> 0.72f
                                isSelected -> 1.04f
                                else -> 1.0f
                            },
                            animationSpec = MotionTokens.spatialFast(),
                            label = "shelf_tile_scale"
                        )
                        val tileAlphaState = animateFloatAsState(
                            targetValue = if (isExpiredFade) 0f else 1f,
                            animationSpec = MotionTokens.effectsDefault(),
                            label = "shelf_tile_alpha"
                        )
                        var swipeY by remember(item.id) { mutableFloatStateOf(0f) }

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
                                .width(102.dp)
                                .graphicsLayer {
                                    translationY = swipeY
                                    scaleX = tileScaleState.value
                                    scaleY = tileScaleState.value
                                    alpha = tileAlphaState.value
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF140E11))
                                    .combinedClickable(
                                        onClick = {
                                            app.soundManager.playTap()
                                            selectedItemId = null
                                            detailItem = item
                                        },
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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isFocusedTile) 2.dp else 1.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(dragGripMod)
                                )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokePx = if (isFocusedTile) 2.dp.toPx() else 1.dp.toPx()
                                    val inset = strokePx / 2f
                                    val r = 10.dp.toPx()
                                    drawRoundRect(
                                        color = if (isFocusedTile) accentColor else Color.White.copy(alpha = 0.14f),
                                        topLeft = Offset(inset, inset),
                                        size = Size(size.width - strokePx, size.height - strokePx),
                                        cornerRadius = CornerRadius(r, r),
                                        style = Stroke(width = strokePx)
                                    )
                                    val topBarLen = (size.width - strokePx) * fraction
                                    if (topBarLen > 2f && !isFocusedTile) {
                                        drawLine(
                                            color = ringColor,
                                            start = Offset(r * 0.4f, inset),
                                            end = Offset((r * 0.4f + topBarLen).coerceAtMost(size.width - r * 0.4f), inset),
                                            strokeWidth = strokePx,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }

                                if (!item.isTrashed && isFocusedTile) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable {
                                                app.soundManager.playTap()
                                                cropLeft = 0.10f; cropTop = 0.10f; cropRight = 0.90f; cropBottom = 0.90f
                                                croppingItem = item
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CropFree,
                                            contentDescription = "Quick crop ${item.displayName}",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                } else if (!item.isTrashed) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.58f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DragIndicator,
                                            contentDescription = "Drag to attach",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(16.dp)
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
                                        Icon(Icons.Filled.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))
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
            .heightIn(min = 160.dp, max = 232.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status, color = IslandColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "Capture",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        app.soundManager.playTap()
                        app.clipboardMonitor.captureIfAllowed(isForeground = true)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (clips.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No clips captured in foreground yet", color = IslandColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                itemsIndexed(clips, key = { _, c -> c.id }) { idx, clip ->
                    var visible by remember(clip.id) { mutableStateOf(false) }
                    LaunchedEffect(clip.id) {
                        delay((idx * (MotionTokens.CONTENT_STAGGER_MS / 4)).toLong().coerceAtMost(MotionTokens.CONTENT_STAGGER_MS.toLong()))
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(140)) + slideInVertically(tween(150)) { it / 3 }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable {
                                    app.soundManager.playTap()
                                    app.clipboardMonitor.copyToClipboard(clip.content)
                                    app.islandController.showTransientBanner("Copied to clipboard")
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(clip.content, color = IslandColors.TextPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
            .heightIn(min = 160.dp, max = 232.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) { editing ->
        if (editing) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
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
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Notes", tint = IslandColors.AccentLime, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Notes", color = IslandColors.AccentLime, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note", tint = IslandColors.AccentRed, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.85.dp, IslandColors.AccentLime.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (draftText.isEmpty()) {
                        Text("Start typing — auto-saved...", color = IslandColors.TextTertiary, fontSize = 13.sp)
                    }
                    BasicTextField(
                        value = draftText,
                        onValueChange = { next ->
                            draftText = next
                            persistDraft(next)
                        },
                        textStyle = TextStyle(color = IslandColors.TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(IslandColors.AccentLime),
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxSize()
                            .onFocusChanged { if (it.isFocused) app.islandController.setKeyboardFocusNeeded(true) }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${notes.size} Notes",
                        color = IslandColors.TextSecondary,
                        fontSize = 12.sp,
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
                        Text("New note", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(6.dp))

                if (notes.isEmpty() && links.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No notes yet", color = IslandColors.TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(links.take(2), key = { "link_${it.id}" }) { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
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
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${link.itemType}: ${link.title}", color = IslandColors.TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        items(notes, key = { "note_${it.id}" }) { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
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
                                    .padding(end = 10.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(IslandColors.AccentLime)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled" },
                                        color = IslandColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = note.content,
                                        color = IslandColors.TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = timeFmt.format(Date(note.updatedAt)),
                                    color = IslandColors.TextTertiary,
                                    fontSize = 11.sp
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
            .heightIn(min = 160.dp, max = 232.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (ui.isRecording) IslandColors.AccentRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f))
                .clickable {
                    if (ui.isRecording) {
                        app.soundManager.playTap()
                        app.voiceMemoManager.stopRecording()
                    } else if (!app.voiceMemoManager.startRecording()) {
                        app.soundManager.playError()
                        app.islandController.showTransientBanner("Grant Microphone permission in Setup Wizard")
                        Toast.makeText(context, "Grant Microphone permission in Setup Wizard", Toast.LENGTH_SHORT).show()
                    } else {
                        app.soundManager.playTap()
                    }
                }
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ui.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = "Voice Memo",
                    tint = if (ui.isRecording) IslandColors.AccentRed else accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (ui.isRecording) "Recording · ${TimerEngine.formatDuration(ui.recordingElapsedMs)} (Tap to stop)" else "Tap to Record Voice Memo",
                    color = IslandColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (memos.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No voice memos recorded yet", color = IslandColors.TextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(memos, key = { it.id }) { memo ->
                    val isPlayingThis = ui.playingMemoId == memo.id && ui.isPlaying
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                app.soundManager.playTap()
                                app.voiceMemoManager.togglePlayMemo(memo)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play memo",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(memo.title, color = IslandColors.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(TimerEngine.formatDuration(memo.durationMs), color = IslandColors.TextSecondary, fontSize = 11.sp)
                        IconButton(
                            onClick = { app.soundManager.playTap(); app.voiceMemoManager.deleteMemo(memo) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete memo", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
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
            .heightIn(min = 160.dp, max = 232.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notifications", color = IslandColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (totalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.24f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(totalCount.toString(), color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.undoableClearedInbox != null) {
                    Text(
                        text = "Undo",
                        color = IslandColors.AccentLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { app.islandController.undoInboxClear() }
                    )
                }
                if (totalCount > 0) {
                    Text(
                        text = "Clear all",
                        color = IslandColors.AccentRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { app.islandController.clearInboxWithUndo() }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (totalCount == 0) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No notifications right now", color = IslandColors.TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                groupedNotifs.forEach { (pkg, itemsForApp) ->
                    val first = itemsForApp.first()
                    val isExpanded = expandedApps[pkg] == true
                    item(key = "grp_$pkg") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
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
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${first.appName.ifBlank { pkg }} (${itemsForApp.size})",
                                        color = IslandColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!isExpanded) {
                                        Text(
                                            text = first.title.ifBlank { first.text },
                                            color = IslandColors.TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = IslandColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
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
                                            .heightIn(min = 38.dp)
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
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(n.title, color = IslandColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (n.text.isNotBlank()) {
                                                Text(n.text, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (otp != null) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.24f))
                                                    .clickable {
                                                        app.soundManager.playTap()
                                                        app.clipboardMonitor.copyToClipboard(otp)
                                                        app.islandController.showTransientBanner("Copied OTP")
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("••••${otp.takeLast(2)} Copy", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
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
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = if (alert.kind == AlertKind.LOW_BATTERY) IslandColors.AccentRed else accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.title, color = IslandColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(alert.subtitle, color = IslandColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
