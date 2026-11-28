package com.example.setup

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DynamicIslandApp
import com.example.search.LaunchableApp
import com.example.search.SearchResults
import com.example.system.IslandSettings
import com.example.system.TimerEngine
import com.example.ui.theme.CrimsonPillButton
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MockupScreenHeader
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import com.example.ui.theme.ObsidianPillButton
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerTabScreen(
    app: DynamicIslandApp,
    initialMode: Int = 0, // 0=Timer, 1=Sleep, 2=Stopwatch
    modifier: Modifier = Modifier
) {
    val timerState by app.timerEngine.state.collectAsState()
    var mode by remember(initialMode) { mutableIntStateOf(initialMode) }
    var selectedPresetMinutes by remember { mutableIntStateOf(5) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    val timerProgress = if (timerState.totalMs > 0L) {
        (timerState.remainingMs.toFloat() / timerState.totalMs.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val ringFraction = when (mode) {
        2 -> ((timerState.stopwatchElapsedMs % 60_000L) / 60_000f).coerceIn(0f, 1f)
        else -> timerProgress
    }

    val centerReadout = when (mode) {
        2 -> TimerEngine.formatStopwatch(timerState.stopwatchElapsedMs)
        else -> {
            val ms = if (timerState.isActive) timerState.remainingMs
            else ((timerState.selectedHours * 3600L + timerState.selectedMinutes * 60L + timerState.selectedSeconds) * 1000L)
            TimerEngine.formatDuration(ms)
        }
    }

    ObsidianCrimsonBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Timer",
                subtitle = "Set a timer for anything",
                icon = Icons.Filled.Timer,
                trailingAction = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xD41B1D23))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Timer options", tint = Color.White)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Custom Duration") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showCustomDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Timer") },
                                onClick = { showMenu = false; mode = 0 }
                            )
                            DropdownMenuItem(
                                text = { Text("Sleep Timer") },
                                onClick = { showMenu = false; mode = 1 }
                            )
                            DropdownMenuItem(
                                text = { Text("Stopwatch") },
                                onClick = { showMenu = false; mode = 2 }
                            )
                        }
                    }
                }
            )

            // Slim Mode Switcher (Timer | Sleep | Stopwatch)
            ObsidianGlassCard(
                cornerRadius = 26.dp,
                contentPadding = PaddingValues(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Timer", "Sleep", "Stopwatch").forEachIndexed { idx, label ->
                        val active = mode == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (active) accent.copy(alpha = 0.24f) else Color.Transparent)
                                .then(if (active) Modifier.border(1.dp, accent.copy(alpha = 0.72f), CircleShape) else Modifier)
                                .clickable { mode = idx }
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else IslandColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                if (timerState.mirroredClockLabel != null && timerState.mirroredRemainingMs != null) {
                    ObsidianGlassCard(cornerRadius = 20.dp, highlighted = true) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = accent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Clock Timer: ${timerState.mirroredClockLabel} · ${TimerEngine.formatDuration(timerState.mirroredRemainingMs!!)}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Large Glowing Circular Arc Gauge matching youtubers742-timer.png
                val dialColor = if (mode == 2) accent else accent
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(258.dp)
                        .clickable(enabled = mode == 0 && !timerState.isActive) { showCustomDialog = true }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 12.dp.toPx()
                        val pad = strokePx / 2f + 12.dp.toPx()
                        val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
                        val topLeft = Offset(pad, pad)

                        // Soft radial outer halo behind ring
                        drawArc(
                            color = dialColor.copy(alpha = 0.14f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx + 12.dp.toPx())
                        )

                        // Track ring
                        drawArc(
                            color = Color(0xFF20222A),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        val sweep = (ringFraction * 360f).coerceIn(6f, 360f)
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    dialColor,
                                    dialColor.copy(alpha = 0.88f),
                                    Color.White.copy(alpha = 0.78f),
                                    dialColor
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        val tipAngleRad = ((-90f + sweep) * PI / 180.0)
                        val radius = arcSize.width / 2f
                        val cx = size.width / 2f + cos(tipAngleRad).toFloat() * radius
                        val cy = size.height / 2f + sin(tipAngleRad).toFloat() * radius
                        drawCircle(color = dialColor.copy(alpha = 0.50f), radius = 10.dp.toPx(), center = Offset(cx, cy))
                        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(cx, cy))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = centerReadout,
                            color = Color.White,
                            fontSize = if (mode == 2) 38.sp else 50.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (mode == 1) Icons.Filled.Bedtime else Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = IslandColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when (mode) {
                                    1 -> "Sleep Timer"
                                    2 -> if (timerState.isStopwatchRunning) "Running" else "Stopwatch"
                                    else -> if (timerState.isDone) "Completed!" else timerState.label
                                },
                                color = IslandColors.TextSecondary,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }

                when (mode) {
                    0 -> {
                        // 4 Preset Pills matching youtubers742-timer.png
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1, 5, 10, 30).forEach { mins ->
                                val selected = selectedPresetMinutes == mins && !timerState.isActive
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) accent.copy(alpha = 0.22f) else Color(0xD41B1D23))
                                        .border(
                                            1.dp,
                                            if (selected) accent.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.14f),
                                            CircleShape
                                        )
                                        .clickable {
                                            app.soundManager.playTimerSet()
                                            selectedPresetMinutes = mins
                                            app.timerEngine.setPickerDuration(0, mins, 0)
                                        }
                                ) {
                                    Text(
                                        text = "$mins min",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Large Dual Bottom Pill Buttons: [Start] + [Reset]
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            CrimsonPillButton(
                                text = when {
                                    timerState.isRunning -> "Pause"
                                    timerState.isActive -> "Resume"
                                    else -> "Start"
                                },
                                icon = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                onClick = {
                                    app.soundManager.playTap()
                                    when {
                                        timerState.isRunning -> app.timerEngine.pauseTimer()
                                        timerState.isActive -> app.timerEngine.resumeTimer()
                                        else -> app.timerEngine.startTimer()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(54.dp)
                            )
                            ObsidianPillButton(
                                text = "Reset",
                                icon = Icons.Filled.Refresh,
                                onClick = {
                                    app.soundManager.playTap()
                                    app.timerEngine.cancelTimer()
                                },
                                modifier = Modifier.weight(1f).height(54.dp)
                            )
                        }
                    }

                    1 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xD41B1D23))
                                        .border(1.dp, accent.copy(alpha = 0.48f), CircleShape)
                                        .clickable {
                                            app.soundManager.playTimerSet()
                                            app.timerEngine.startSleepTimer(mins * 60_000L)
                                        }
                                ) {
                                    Text("${mins}m", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            CrimsonPillButton(
                                text = "Sleep 30m",
                                icon = Icons.Filled.Bedtime,
                                onClick = {
                                    app.soundManager.playTimerSet()
                                    app.timerEngine.startSleepTimer(30 * 60_000L)
                                },
                                modifier = Modifier.weight(1f).height(54.dp)
                            )
                            ObsidianPillButton(
                                text = "Stop",
                                icon = Icons.Filled.Refresh,
                                onClick = { app.timerEngine.cancelTimer() },
                                modifier = Modifier.weight(1f).height(54.dp)
                            )
                        }
                    }

                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            CrimsonPillButton(
                                text = if (timerState.isStopwatchRunning) "Pause" else "Start",
                                icon = if (timerState.isStopwatchRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                onClick = {
                                    app.soundManager.playTap()
                                    if (timerState.isStopwatchRunning) app.timerEngine.pauseStopwatch()
                                    else app.timerEngine.startStopwatch()
                                },
                                modifier = Modifier.weight(1f).height(54.dp)
                            )
                            ObsidianPillButton(
                                text = if (timerState.isStopwatchRunning) "Lap" else "Reset",
                                icon = if (timerState.isStopwatchRunning) Icons.Filled.Flag else Icons.Filled.Refresh,
                                onClick = {
                                    app.soundManager.playTap()
                                    if (timerState.isStopwatchRunning) app.timerEngine.lapStopwatch()
                                    else app.timerEngine.resetStopwatch()
                                },
                                modifier = Modifier.weight(1f).height(54.dp)
                            )
                        }

                        if (timerState.laps.isNotEmpty()) {
                            ObsidianGlassCard(cornerRadius = 22.dp, modifier = Modifier.fillMaxWidth()) {
                                Text("Laps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(6.dp))
                                timerState.laps.forEachIndexed { idx, lapMs ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Lap ${timerState.laps.size - idx}", color = IslandColors.TextSecondary, fontSize = 13.sp)
                                        Text(TimerEngine.formatStopwatch(lapMs), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        var h by remember { mutableIntStateOf(timerState.selectedHours) }
        var m by remember { mutableIntStateOf(timerState.selectedMinutes) }
        var s by remember { mutableIntStateOf(timerState.selectedSeconds) }

        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Custom Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hours: $h", color = Color.White)
                    Slider(
                        value = h.toFloat(),
                        onValueChange = { h = it.toInt() },
                        valueRange = 0f..12f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                    Text("Minutes: $m", color = Color.White)
                    Slider(
                        value = m.toFloat(),
                        onValueChange = { m = it.toInt() },
                        valueRange = 0f..59f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                    Text("Seconds: $s", color = Color.White)
                    Slider(
                        value = s.toFloat(),
                        onValueChange = { s = it.toInt() },
                        valueRange = 0f..59f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        app.timerEngine.setPickerDuration(h, m, s)
                        app.soundManager.playTimerSet()
                        showCustomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SearchTabScreen(
    app: DynamicIslandApp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val appsVersion by app.searchEngine.appsVersion.collectAsState()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }
    var editPinned by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(true) }
    val searchAccent = MaterialTheme.colorScheme.primary

    LaunchedEffect(query, appsVersion) {
        results = app.searchEngine.search(query)
    }

    val allAppsSorted = remember(results.apps, sortAscending) {
        if (sortAscending) results.apps else results.apps.reversed()
    }
    val pinnedApps = remember(results.apps, settings.pinnedAppPackages) {
        settings.pinnedAppPackages.mapNotNull { pkg -> results.apps.firstOrNull { it.packageName == pkg } }
    }

    fun togglePin(pkg: String) {
        scope.launch {
            app.settingsRepository.update { s ->
                val next = s.pinnedAppPackages.toMutableList()
                if (pkg in next) next.remove(pkg) else next.add(pkg)
                s.copy(pinnedAppPackages = next)
            }
        }
    }

    ObsidianCrimsonBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Search",
                subtitle = "Find anything, anywhere",
                icon = Icons.Filled.Search,
                trailingAction = {
                    IconButton(
                        onClick = { app.searchEngine.refreshApps() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xD41B1D23))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh installed apps", tint = Color.White)
                    }
                }
            )

            // Search Pill matching youtubers742-search.png
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps, notes, math or Web", color = IslandColors.TextSecondary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = searchAccent) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = IslandColors.TextSecondary)
                            }
                            IconButton(onClick = { app.searchEngine.launchWebSearch(query) }) {
                                Icon(Icons.Filled.TravelExplore, contentDescription = "Search web", tint = searchAccent)
                            }
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xDD22252D),
                    unfocusedContainerColor = Color(0xD41B1D23),
                    focusedBorderColor = searchAccent.copy(alpha = 0.72f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (results.inlineMathResult != null) {
                    item {
                        ObsidianGlassCard(
                            cornerRadius = 20.dp,
                            highlighted = true,
                            onClick = {
                                app.clipboardMonitor.copyToClipboard(results.inlineMathResult!!)
                                Toast.makeText(context, "Copied result ${results.inlineMathResult}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Quick Math", color = searchAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$query = ${results.inlineMathResult}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy result", tint = searchAccent)
                            }
                        }
                    }
                }

                // PINNED APPS header & 3-column grid
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(15.dp).background(searchAccent, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("PINNED APPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        TextButton(onClick = { editPinned = !editPinned }) {
                            Text(if (editPinned) "Done" else "Edit", color = searchAccent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = searchAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (pinnedApps.isEmpty()) {
                    item {
                        ObsidianGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                            Text(
                                "Tap the pin icon on any app below to keep your favourites at the top.",
                                color = IslandColors.TextSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                } else {
                    items(pinnedApps.chunked(3)) { rowApps ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            rowApps.forEach { appItem ->
                                PinnedAppTile(
                                    item = appItem,
                                    editMode = editPinned,
                                    onClick = {
                                        if (editPinned) togglePin(appItem.packageName)
                                        else app.searchEngine.launchApp(appItem.packageName)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowApps.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (query.isNotBlank() && results.notes.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(15.dp).background(searchAccent, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("MATCHING NOTES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                    items(results.notes, key = { "note_${it.id}" }) { note ->
                        ObsidianGlassCard(cornerRadius = 18.dp, onClick = { app.clipboardMonitor.copyToClipboard(note.content) }) {
                            Text(note.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(note.content, color = IslandColors.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // ALL APPS header & A-Z sortable list matching youtubers742-search.png
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(15.dp).background(searchAccent, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("ALL APPS (${allAppsSorted.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { sortAscending = !sortAscending }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(if (sortAscending) "A – Z" else "Z – A", color = IslandColors.TextSecondary, fontSize = 12.5.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = if (sortAscending) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Toggle sort order",
                                tint = IslandColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                items(allAppsSorted, key = { it.packageName }) { appItem ->
                    val isPinned = appItem.packageName in settings.pinnedAppPackages
                    ObsidianGlassCard(
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = { app.searchEngine.launchApp(appItem.packageName) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (appItem.iconBitmap != null) {
                                Image(
                                    bitmap = appItem.iconBitmap,
                                    contentDescription = appItem.label,
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                CrimsonSquircleIcon(icon = Icons.Filled.Apps, size = 38.dp, iconSize = 20.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = appItem.label,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { togglePin(appItem.packageName) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = if (isPinned) "Unpin" else "Pin",
                                    tint = if (isPinned) searchAccent else IslandColors.TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = IslandColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedAppTile(
    item: LaunchableApp,
    editMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ObsidianGlassCard(
            cornerRadius = 18.dp,
            highlighted = editMode,
            contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (item.iconBitmap != null) {
                    Image(
                        bitmap = item.iconBitmap,
                        contentDescription = item.label,
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                    )
                } else {
                    CrimsonSquircleIcon(icon = Icons.Filled.Apps, size = 46.dp, iconSize = 22.dp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.PushPin, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}
