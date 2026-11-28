package com.example.setup

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import com.example.DynamicIslandApp
import com.example.notes.NoteEntity
import com.example.notes.TaskEntity
import com.example.notifications.OtpExtractor
import com.example.system.IslandSettings
import com.example.system.TimerEngine
import com.example.ui.theme.CrimsonPillButton
import com.example.ui.theme.CrimsonSquircleIcon
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MockupScreenHeader
import com.example.ui.theme.ObsidianCrimsonBackground
import com.example.ui.theme.ObsidianGlassCard
import com.example.ui.theme.ObsidianPillButton
import com.example.vault.VaultItemEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotesTabScreen(
    app: DynamicIslandApp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notes by app.database.noteDao().observeAll().collectAsState(initial = emptyList())
    val tasks by app.database.taskDao().observeAll().collectAsState(initial = emptyList())
    val clips by app.clipboardMonitor.historyFlow.collectAsState(initial = emptyList())
    val memos by app.voiceMemoManager.memosFlow.collectAsState(initial = emptyList())
    val memoState by app.voiceMemoManager.uiState.collectAsState()

    var filterMode by remember { mutableIntStateOf(0) } // 0=Notes, 1=Clips, 2=Memos, 3=Tasks
    var showModeStrip by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var taskDraft by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            app.voiceMemoManager.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is needed for Voice Memos", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredNotes = remember(notes, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) notes else notes.filter {
            it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)
        }
    }
    val filteredClips = remember(clips, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) clips else clips.filter { it.content.contains(q, ignoreCase = true) }
    }
    val filteredTasks = remember(tasks, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) tasks else tasks.filter { it.title.contains(q, ignoreCase = true) }
    }

    val accent = MaterialTheme.colorScheme.primary

    ObsidianCrimsonBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Notes",
                subtitle = "Capture your thoughts & ideas",
                icon = Icons.Filled.Description,
                trailingAction = {
                    Box {
                        IconButton(
                            onClick = { showOverflow = true },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xD41B1D23))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Notes menu", tint = Color.White)
                        }
                        DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                            DropdownMenuItem(
                                text = { Text("New Note") },
                                onClick = {
                                    showOverflow = false
                                    editingNote = null
                                    showNoteDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Notes (${notes.size})") },
                                onClick = { showOverflow = false; filterMode = 0 }
                            )
                            DropdownMenuItem(
                                text = { Text("Clipboard (${clips.size})") },
                                onClick = {
                                    showOverflow = false
                                    app.clipboardMonitor.captureIfAllowed(isForeground = true)
                                    filterMode = 1
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Voice Memos (${memos.size})") },
                                onClick = { showOverflow = false; filterMode = 2 }
                            )
                            DropdownMenuItem(
                                text = { Text("Tasks (${tasks.size})") },
                                onClick = { showOverflow = false; filterMode = 3 }
                            )
                            if (clips.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Clear Clipboard History") },
                                    onClick = {
                                        showOverflow = false
                                        app.clipboardMonitor.clearAll()
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // Sleek Search Notes Pill matching youtubers742-notes.png
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search notes", color = IslandColors.TextSecondary, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = IslandColors.TextSecondary)
                },
                trailingIcon = {
                    IconButton(onClick = { showModeStrip = !showModeStrip }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Switch Notes / Clips / Memos / Tasks",
                            tint = if (showModeStrip || filterMode != 0) accent else IslandColors.TextSecondary
                        )
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xDD22252D),
                    unfocusedContainerColor = Color(0xD41B1D23),
                    focusedBorderColor = accent.copy(alpha = 0.72f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            if (showModeStrip || filterMode != 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Notes" to notes.size,
                        "Clips" to clips.size,
                        "Memos" to memos.size,
                        "Tasks" to tasks.size
                    ).forEachIndexed { idx, (label, count) ->
                        val active = filterMode == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (active) accent.copy(alpha = 0.24f) else Color(0xD41B1D23))
                                .border(
                                    1.dp,
                                    if (active) accent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.12f),
                                    CircleShape
                                )
                                .clickable { filterMode = idx }
                        ) {
                            Text(
                                text = "$label ($count)",
                                color = if (active) Color.White else IslandColors.TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (filterMode) {
                0 -> {
                    if (filteredNotes.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                CrimsonSquircleIcon(icon = Icons.Filled.Description, size = 56.dp, iconSize = 28.dp)
                                Spacer(Modifier.height(12.dp))
                                Text("No notes yet", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("Tap + to write your first note on-device.", color = IslandColors.TextSecondary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                var itemMenu by remember { mutableStateOf(false) }
                                val noteIcon = remember(note.id, note.isPinned, note.title) {
                                    when {
                                        note.isPinned -> Icons.Filled.Star
                                        note.title.contains("idea", ignoreCase = true) -> Icons.Filled.Lightbulb
                                        note.title.contains("goal", ignoreCase = true) ||
                                            note.title.contains("list", ignoreCase = true) -> Icons.AutoMirrored.Filled.List
                                        else -> Icons.Filled.Description
                                    }
                                }
                                ObsidianGlassCard(
                                    cornerRadius = 22.dp,
                                    highlighted = note.isPinned,
                                    contentPadding = PaddingValues(16.dp),
                                    onClick = {
                                        editingNote = note
                                        showNoteDialog = true
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CrimsonSquircleIcon(
                                            icon = noteIcon,
                                            size = 48.dp,
                                            iconSize = 24.dp,
                                            tint = accent
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.title.ifBlank { "Untitled Note" },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (note.content.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = note.content,
                                                    color = IslandColors.TextSecondary,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.CalendarToday,
                                                    contentDescription = null,
                                                    tint = IslandColors.TextTertiary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(5.dp))
                                                Text(
                                                    text = formatRelativeDay(note.updatedAt),
                                                    color = IslandColors.TextTertiary,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                        Box {
                                            IconButton(onClick = { itemMenu = true }) {
                                                Icon(Icons.Filled.MoreVert, contentDescription = "Note options", tint = IslandColors.TextSecondary)
                                            }
                                            DropdownMenu(expanded = itemMenu, onDismissRequest = { itemMenu = false }) {
                                                DropdownMenuItem(
                                                    text = { Text(if (note.isPinned) "Unpin" else "Pin to top") },
                                                    onClick = {
                                                        itemMenu = false
                                                        scope.launch { app.database.noteDao().upsert(note.copy(isPinned = !note.isPinned)) }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Copy Text") },
                                                    onClick = {
                                                        itemMenu = false
                                                        app.clipboardMonitor.copyToClipboard("${note.title}\n${note.content}".trim())
                                                        Toast.makeText(context, "Copied note", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete", color = IslandColors.CrimsonBright) },
                                                    onClick = {
                                                        itemMenu = false
                                                        scope.launch { app.database.noteDao().deleteById(note.id) }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            ObsidianGlassCard(cornerRadius = 22.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Foreground Clipboard", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Captures clips only while Dynamic Island is in the foreground.",
                                            color = IslandColors.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    CrimsonPillButton(
                                        text = "Read",
                                        onClick = { app.clipboardMonitor.captureIfAllowed(isForeground = true) }
                                    )
                                }
                            }
                        }
                        if (filteredClips.isEmpty()) {
                            item {
                                Text(
                                    "No clipboard items saved yet.",
                                    color = IslandColors.TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(filteredClips, key = { it.id }) { clip ->
                                ObsidianGlassCard(cornerRadius = 20.dp) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = clip.content,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = {
                                            app.clipboardMonitor.copyToClipboard(clip.content)
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = accent)
                                        }
                                        IconButton(onClick = { app.clipboardMonitor.deleteClip(clip.id) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = IslandColors.TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            ObsidianGlassCard(cornerRadius = 22.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = taskDraft,
                                        onValueChange = { taskDraft = it },
                                        placeholder = { Text("New persistent task…", color = IslandColors.TextSecondary) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CrimsonPillButton(
                                        text = "Add",
                                        onClick = {
                                            val clean = taskDraft.trim()
                                            if (clean.isNotEmpty()) {
                                                app.soundManager.playTap()
                                                taskDraft = ""
                                                scope.launch { app.database.taskDao().upsert(TaskEntity(title = clean)) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        if (filteredTasks.isEmpty()) {
                            item {
                                Text(
                                    "No tasks yet. Add a quick checklist item above.",
                                    color = IslandColors.TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(filteredTasks, key = { "task_${it.id}" }) { task ->
                                ObsidianGlassCard(
                                    cornerRadius = 20.dp,
                                    onClick = {
                                        app.soundManager.playToggle(!task.isDone)
                                        scope.launch { app.database.taskDao().toggleDone(task.id, !task.isDone) }
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (task.isDone) accent else Color.Transparent)
                                                .border(1.5.dp, if (task.isDone) accent else Color.White.copy(alpha = 0.4f), CircleShape)
                                        ) {
                                            if (task.isDone) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = task.title,
                                            color = if (task.isDone) IslandColors.TextTertiary else Color.White,
                                            fontSize = 14.sp,
                                            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { scope.launch { app.database.taskDao().deleteById(task.id) } }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete task", tint = IslandColors.TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            ObsidianGlassCard(cornerRadius = 22.dp, highlighted = memoState.isRecording) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    CrimsonSquircleIcon(icon = Icons.Filled.Mic, tint = accent)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (memoState.isRecording) "Recording ${TimerEngine.formatDuration(memoState.recordingElapsedMs)}"
                                            else "Voice Memo Recorder",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Stored strictly on-device in private app storage.",
                                            color = IslandColors.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    CrimsonPillButton(
                                        text = if (memoState.isRecording) "Stop" else "Record",
                                        icon = if (memoState.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                                        onClick = {
                                            if (memoState.isRecording) {
                                                app.voiceMemoManager.stopRecording()
                                            } else if (app.voiceMemoManager.hasMicPermission()) {
                                                app.voiceMemoManager.startRecording()
                                            } else {
                                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        if (memos.isEmpty()) {
                            item {
                                Text(
                                    "No voice memos recorded yet.",
                                    color = IslandColors.TextSecondary,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            items(memos, key = { it.id }) { memo ->
                                val playingThis = memoState.playingMemoId == memo.id && memoState.isPlaying
                                ObsidianGlassCard(cornerRadius = 20.dp, highlighted = playingThis) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { app.voiceMemoManager.togglePlayMemo(memo) },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(accent.copy(alpha = 0.22f))
                                        ) {
                                            Icon(
                                                imageVector = if (playingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = if (playingThis) "Pause" else "Play",
                                                tint = accent
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(memo.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${TimerEngine.formatDuration(memo.durationMs)} · ${formatRelativeDay(memo.createdAt)}",
                                                color = IslandColors.TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        IconButton(onClick = { app.voiceMemoManager.deleteMemo(memo) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete memo", tint = IslandColors.TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filterMode == 0) {
            FloatingActionButton(
                onClick = {
                    editingNote = null
                    showNoteDialog = true
                },
                containerColor = accent,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 18.dp)
                    .size(58.dp)
                    .border(1.4.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .testTag("notes_add_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Note", modifier = Modifier.size(26.dp))
            }
        }
    }

    if (showNoteDialog) {
        NoteEditorDialog(
            initial = editingNote,
            onDismiss = { showNoteDialog = false },
            onSave = { title, body, pinned ->
                showNoteDialog = false
                scope.launch {
                    val note = editingNote?.copy(
                        title = title,
                        content = body,
                        isPinned = pinned,
                        updatedAt = System.currentTimeMillis()
                    ) ?: NoteEntity(
                        title = title.ifBlank { "Quick Note" },
                        content = body,
                        isPinned = pinned,
                        updatedAt = System.currentTimeMillis()
                    )
                    app.database.noteDao().upsert(note)
                }
            },
            onDelete = editingNote?.let { existing ->
                {
                    showNoteDialog = false
                    scope.launch { app.database.noteDao().deleteById(existing.id) }
                }
            }
        )
    }
}

@Composable
private fun NoteEditorDialog(
    initial: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var body by remember(initial) { mutableStateOf(initial?.content.orEmpty()) }
    var pinned by remember(initial) { mutableStateOf(initial?.isPinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IslandColors.ObsidianCardElevated,
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text(if (initial == null) "New Note" else "Edit Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Content") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Pin note to top", modifier = Modifier.weight(1f), color = Color.White)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), body.trim(), pinned) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = IslandColors.CrimsonBright) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private data class DisplayAlertItem(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val subtitle: String,
    val timestampMs: Long,
    val isLive: Boolean
)

@Composable
fun AlertsTabScreen(
    app: DynamicIslandApp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val islandState by app.islandController.state.collectAsState()
    val history by app.database.notificationDao().observeAll().collectAsState(initial = emptyList())
    val rules by app.database.appRuleDao().observeAll().collectAsState(initial = emptyList())
    val mediaState by app.mediaHub.state.collectAsState()

    var showRules by remember { mutableStateOf(false) }
    var inspectingAlert by remember { mutableStateOf<DisplayAlertItem?>(null) }

    val mergedAlerts = remember(islandState.notificationsHistory, islandState.alertsHistory, history) {
        val list = mutableListOf<DisplayAlertItem>()
        islandState.alertsHistory.forEach { a ->
            list.add(DisplayAlertItem("alert_${a.id}", context.packageName, "System Alert", a.title, a.subtitle, a.timestamp, true))
        }
        islandState.notificationsHistory.forEach { n ->
            list.add(DisplayAlertItem("notif_${n.id}", n.packageName, n.appName, n.title, n.text, n.timestamp, true))
        }
        history.forEach { h ->
            if (list.none { it.title == h.title && kotlin.math.abs(it.timestampMs - h.timestamp) < 5000L }) {
                list.add(DisplayAlertItem("db_${h.id}", h.packageName, h.appName, h.title, h.text, h.timestamp, false))
            }
        }
        list.sortedByDescending { it.timestampMs }
    }

    val accent = MaterialTheme.colorScheme.primary

    ObsidianCrimsonBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Alerts",
                subtitle = "Your notifications and alerts",
                icon = Icons.Filled.Notifications,
                trailingAction = {
                    OutlinedButton(
                        onClick = {
                            app.islandController.clearInboxWithUndo()
                            scope.launch { app.database.notificationDao().clearAll() }
                        },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.50f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear all", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            )

            // Compact view-mode toggle bar (keeps Alerts stack cleanly high like youtubers742-alerts.png)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (!showRules) accent.copy(alpha = 0.20f) else Color(0xD41B1D23))
                        .border(1.dp, if (!showRules) accent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable { showRules = false }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Inbox (${mergedAlerts.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (showRules) accent.copy(alpha = 0.20f) else Color(0xD41B1D23))
                        .border(1.dp, if (showRules) accent.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable { showRules = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("App Rules (${rules.size})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!mediaState.hasNotificationAccess) {
                Spacer(Modifier.height(6.dp))
                ObsidianGlassCard(
                    cornerRadius = 20.dp,
                    highlighted = true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Access Needed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Grant access so incoming alerts appear in the pill & here.", color = IslandColors.TextSecondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        CrimsonPillButton(
                            text = "Grant",
                            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }
                        )
                    }
                }
            }

            if (islandState.undoableClearedInbox != null) {
                Spacer(Modifier.height(6.dp))
                ObsidianGlassCard(
                    cornerRadius = 20.dp,
                    highlighted = true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Inbox cleared", color = Color.White, modifier = Modifier.weight(1f))
                        TextButton(onClick = { app.islandController.undoInboxClear() }) {
                            Text("Undo", color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (!showRules) {
                if (mergedAlerts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CrimsonSquircleIcon(icon = Icons.Filled.NotificationsOff, size = 56.dp, iconSize = 26.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("All caught up", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Incoming notifications and alerts appear here.", color = IslandColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(mergedAlerts.size, key = { mergedAlerts[it].id }) { index ->
                            val item = mergedAlerts[index]
                            val isNewest = index == 0
                            val otpCode = remember(item.title, item.subtitle) {
                                OtpExtractor.extract(item.title, item.subtitle)
                            }
                            val appBitmap: ImageBitmap? = remember(item.packageName) {
                                runCatching {
                                    context.packageManager.getApplicationIcon(item.packageName).toBitmap(72, 72).asImageBitmap()
                                }.getOrNull()
                            }

                            ObsidianGlassCard(
                                cornerRadius = 22.dp,
                                highlighted = isNewest,
                                contentPadding = PaddingValues(16.dp),
                                onClick = { inspectingAlert = item }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (appBitmap != null) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF1F222A))
                                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                        ) {
                                            Image(
                                                bitmap = appBitmap,
                                                contentDescription = item.appName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    } else {
                                        CrimsonSquircleIcon(icon = Icons.Filled.Notifications, size = 48.dp, tint = accent)
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.appName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = buildString {
                                                append(item.title)
                                                if (item.subtitle.isNotBlank()) {
                                                    append(" · ")
                                                    append(item.subtitle)
                                                }
                                            },
                                            color = IslandColors.TextSecondary,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(5.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.AccessTime,
                                                    contentDescription = null,
                                                    tint = IslandColors.TextTertiary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = formatTimeAgo(item.timestampMs),
                                                    color = IslandColors.TextTertiary,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                            if (otpCode != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(accent.copy(alpha = 0.22f))
                                                        .border(1.dp, accent.copy(alpha = 0.65f), CircleShape)
                                                        .clickable {
                                                            app.soundManager.playCopy()
                                                            app.clipboardMonitor.copyToClipboard(otpCode)
                                                            Toast.makeText(context, "Copied code $otpCode", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(horizontal = 9.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Copy $otpCode", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (isNewest) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(accent)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Inspect alert",
                                            tint = IslandColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (rules.isEmpty()) {
                        item {
                            ObsidianGlassCard(cornerRadius = 20.dp) {
                                Text("Per-app preview toggles appear once apps send notifications.", color = IslandColors.TextSecondary)
                            }
                        }
                    } else {
                        items(rules, key = { it.packageName }) { rule ->
                            ObsidianGlassCard(cornerRadius = 20.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rule.appName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text(rule.packageName, color = IslandColors.TextTertiary, fontSize = 11.sp, maxLines = 1)
                                    }
                                    Switch(
                                        checked = rule.allowIslandPreview,
                                        onCheckedChange = { allowed ->
                                            app.soundManager.playToggle(allowed)
                                            scope.launch { app.database.appRuleDao().setIslandPreviewAllowed(rule.packageName, allowed) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (inspectingAlert != null) {
        val alert = inspectingAlert!!
        val otpCode = remember(alert.title, alert.subtitle) { OtpExtractor.extract(alert.title, alert.subtitle) }
        AlertDialog(
            onDismissRequest = { inspectingAlert = null },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(alert.appName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(alert.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                    if (alert.subtitle.isNotBlank()) {
                        Text(alert.subtitle, color = IslandColors.TextSecondary, fontSize = 13.sp)
                    }
                    Text(formatTimeAgo(alert.timestampMs), color = IslandColors.TextTertiary, fontSize = 11.5.sp)
                    if (otpCode != null) {
                        CrimsonPillButton(
                            text = "Copy OTP $otpCode",
                            icon = Icons.Filled.ContentCopy,
                            onClick = {
                                app.soundManager.playCopy()
                                app.clipboardMonitor.copyToClipboard(otpCode)
                                Toast.makeText(context, "Copied code $otpCode", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingAlert = null }) { Text("Close") }
            },
            dismissButton = {
                if (alert.id.startsWith("notif_")) {
                    TextButton(onClick = {
                        app.islandController.dismissNotificationById(alert.id.removePrefix("notif_"))
                        inspectingAlert = null
                    }) {
                        Text("Dismiss", color = IslandColors.CrimsonBright)
                    }
                }
            }
        )
    }
}

@Composable
fun ShelfTabScreen(
    app: DynamicIslandApp,
    onOpenWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by app.settingsRepository.settingsFlow.collectAsState(initial = IslandSettings())
    val activeItems by app.vaultManager.activeItems.collectAsState(initial = emptyList())
    val trashedItems by app.vaultManager.trashedItems.collectAsState(initial = emptyList())

    var filterTab by remember { mutableIntStateOf(0) } // 0=Photos, 1=Videos/Recordings, 2=Trash
    var selectedItemId by remember { mutableStateOf<Long?>(null) }
    var cropTargetItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var renameTargetItem by remember { mutableStateOf<VaultItemEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeItems.size) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { app.vaultManager.saveFromUri(uri, sourceType = "PICKED") }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scope.launch { app.settingsRepository.update { it.copy(vaultScreenshotsTreeUri = treeUri.toString()) } }
        }
    }

    val displayed = when (filterTab) {
        1 -> activeItems.filter {
            it.isPermanent || it.sourceType == "SCREEN_RECORDING" || it.mimeType.startsWith("video/", ignoreCase = true)
        }
        2 -> trashedItems
        else -> activeItems
    }
    val selectedItem = displayed.firstOrNull { it.id == selectedItemId } ?: displayed.firstOrNull()
    val accent = MaterialTheme.colorScheme.primary

    ObsidianCrimsonBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
        ) {
            MockupScreenHeader(
                title = "Shelf",
                subtitle = "Your media collection",
                icon = Icons.Filled.Layers,
                trailingAction = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.24f))
                                .border(1.dp, accent.copy(alpha = 0.55f), CircleShape)
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add Photo", tint = Color.White)
                        }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xD41B1D23))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                            ) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Shelf options", tint = Color.White)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Link Screenshots Folder (SAF)") },
                                    leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        val initial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            DocumentsContract.buildDocumentUri(
                                                "com.android.externalstorage.documents",
                                                "primary:DCIM/Screenshots"
                                            )
                                        } else null
                                        folderPicker.launch(initial)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Setup Auto-Capture") },
                                    onClick = {
                                        showMenu = false
                                        onOpenWizard()
                                    }
                                )
                            }
                        }
                    }
                }
            )

            // 3-tab glass segmented pill matching youtubers742-shelf.png
            ObsidianGlassCard(
                cornerRadius = 26.dp,
                contentPadding = PaddingValues(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val tabs = listOf(
                    Triple("Photos", Icons.Filled.PhotoLibrary, "vault_filter_all"),
                    Triple("Videos", Icons.Filled.Videocam, "vault_filter_videos"),
                    Triple("Trash", Icons.Filled.BookmarkBorder, "vault_filter_trash")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tabs.forEachIndexed { idx, (label, icon, tag) ->
                        val active = filterTab == idx
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(CircleShape)
                                .background(if (active) accent.copy(alpha = 0.24f) else Color.Transparent)
                                .then(if (active) Modifier.border(1.dp, accent.copy(alpha = 0.72f), CircleShape) else Modifier)
                                .clickable { filterTab = idx }
                                .testTag(tag)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) Color.White else IslandColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = if (active) Color.White else IslandColors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (settings.vaultScreenshotsTreeUri.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                ObsidianGlassCard(
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = {
                        val initial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            DocumentsContract.buildDocumentUri(
                                "com.android.externalstorage.documents",
                                "primary:DCIM/Screenshots"
                            )
                        } else null
                        folderPicker.launch(initial)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Tap to link DCIM/Screenshots for auto-capture",
                            color = IslandColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (displayed.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        CrimsonSquircleIcon(icon = Icons.Filled.PhotoLibrary, size = 56.dp, iconSize = 28.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when (filterTab) {
                                1 -> "No screen recordings yet"
                                2 -> "Trash is empty"
                                else -> "Your 10-min Shelf is empty"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        if (filterTab == 0) {
                            Spacer(Modifier.height(8.dp))
                            CrimsonPillButton(
                                text = "Add Photo",
                                icon = Icons.Filled.AddPhotoAlternate,
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(displayed, key = { it.id }) { item ->
                        val isSelected = selectedItem?.id == item.id
                        val isVideo = item.isPermanent ||
                            item.sourceType == "SCREEN_RECORDING" ||
                            item.mimeType.startsWith("video/", ignoreCase = true)
                        val remMs = (item.expiresAt - nowMs).coerceAtLeast(0L)
                        val previewFile = remember(item.thumbnailPath, item.filePath) {
                            item.thumbnailPath?.takeIf { File(it).exists() }?.let { File(it) } ?: File(item.filePath)
                        }
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(IslandColors.ObsidianCard)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accent else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedItemId = item.id }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = previewFile),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isVideo) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xC8000000))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = IslandColors.AccentRed, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Saved", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (!item.isTrashed) {
                                Text(
                                    text = TimerEngine.formatDuration(remMs),
                                    color = if (remMs < 120_000L) IslandColors.AccentAmber else Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xB8000000))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            // Crisp white corner crop handles matching youtubers742-shelf.png
                            if (isSelected) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val r = 5.5.dp.toPx()
                                    val inset = 2.dp.toPx()
                                    listOf(
                                        Offset(inset, inset),
                                        Offset(size.width - inset, inset),
                                        Offset(inset, size.height - inset),
                                        Offset(size.width - inset, size.height - inset)
                                    ).forEach { corner ->
                                        drawCircle(color = Color.White, radius = r, center = corner)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar matching youtubers742-shelf.png (Attach + Copy to clipboard, NO Share)
            if (selectedItem != null) {
                val selectedIsVideo = selectedItem.isPermanent ||
                    selectedItem.sourceType == "SCREEN_RECORDING" ||
                    selectedItem.mimeType.startsWith("video/", ignoreCase = true)

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    if (selectedItem.isTrashed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            CrimsonPillButton(
                                text = "Restore",
                                icon = Icons.Filled.Restore,
                                onClick = { scope.launch { app.vaultManager.restoreItem(selectedItem.id) } },
                                modifier = Modifier.weight(1f)
                            )
                            ObsidianPillButton(
                                text = "Purge",
                                icon = Icons.Filled.DeleteOutline,
                                onClick = { scope.launch { app.vaultManager.purgeItemNow(selectedItem.id) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (selectedIsVideo) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            CrimsonPillButton(
                                text = "Play",
                                icon = Icons.Filled.PlayArrow,
                                onClick = {
                                    val uri = app.vaultManager.getFileProviderUri(selectedItem)
                                    if (uri != null) {
                                        val playIntent = Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(uri, "video/mp4")
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        runCatching { context.startActivity(playIntent) }
                                    } else {
                                        Toast.makeText(context, "Recording file missing", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ObsidianPillButton(
                                text = "Rename",
                                icon = Icons.Filled.Edit,
                                onClick = { renameTargetItem = selectedItem },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ObsidianPillButton(
                                text = "Save",
                                icon = Icons.Filled.Download,
                                onClick = {
                                    scope.launch {
                                        val ok = app.vaultManager.saveItemToGallery(selectedItem)
                                        Toast.makeText(
                                            context,
                                            if (ok) "Saved to Movies/DynamicIsland" else "Could not export video",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ObsidianPillButton(
                                text = "Delete",
                                icon = Icons.Filled.DeleteOutline,
                                onClick = { scope.launch { app.vaultManager.purgeItemNow(selectedItem.id) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Slim quick utility chips (Crop / Save / Trash)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ShelfMiniActionChip(
                                text = "Crop",
                                icon = Icons.Filled.Crop,
                                onClick = { cropTargetItem = selectedItem },
                                modifier = Modifier.weight(1f)
                            )
                            ShelfMiniActionChip(
                                text = "Save to Gallery",
                                icon = Icons.Filled.Download,
                                onClick = {
                                    scope.launch {
                                        val ok = app.vaultManager.saveItemToGallery(selectedItem)
                                        Toast.makeText(
                                            context,
                                            if (ok) "Saved to Pictures/DynamicIsland" else "Could not save photo",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1.3f)
                            )
                            ShelfMiniActionChip(
                                text = "Trash",
                                icon = Icons.Filled.DeleteOutline,
                                onClick = { scope.launch { app.vaultManager.moveToTrash(selectedItem.id) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Exact mockup dual bottom pills: [Attach] + [Copy to clipboard]
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ObsidianPillButton(
                                text = "Attach",
                                icon = Icons.Filled.AttachFile,
                                onClick = {
                                    app.vaultManager.attachOrShareItem(selectedItem)
                                    Toast.makeText(context, "Ready to paste in chat", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ObsidianPillButton(
                                text = "Copy to clipboard",
                                icon = Icons.Filled.ContentCopy,
                                onClick = {
                                    app.soundManager.playCopy()
                                    app.vaultManager.attachOrShareItem(selectedItem)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.35f)
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (renameTargetItem != null) {
        val target = renameTargetItem!!
        var nameDraft by remember(target.id) { mutableStateOf(target.displayName) }
        AlertDialog(
            onDismissRequest = { renameTargetItem = null },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Recording name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = nameDraft.trim()
                        renameTargetItem = null
                        if (cleaned.isNotEmpty()) {
                            scope.launch { app.vaultManager.renameItem(target.id, cleaned) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetItem = null }) { Text("Cancel") }
            }
        )
    }

    if (cropTargetItem != null) {
        val target = cropTargetItem!!
        AlertDialog(
            onDismissRequest = { cropTargetItem = null },
            containerColor = IslandColors.ObsidianCardElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Crop Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a quick crop frame for ${target.displayName}:", color = IslandColors.TextSecondary, fontSize = 13.sp)
                    listOf(
                        "Center Square" to listOf(0.15f, 0.15f, 0.85f, 0.85f),
                        "Trim Edges (10%)" to listOf(0.10f, 0.10f, 0.90f, 0.90f),
                        "Top Half" to listOf(0.0f, 0.0f, 1.0f, 0.55f),
                        "Bottom Half" to listOf(0.0f, 0.45f, 1.0f, 1.0f)
                    ).forEach { (label, rect) ->
                        ObsidianPillButton(
                            text = label,
                            onClick = {
                                cropTargetItem = null
                                scope.launch {
                                    app.vaultManager.cropVaultItem(target.id, rect[0], rect[1], rect[2], rect[3])
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val itemToTrash = target
                    cropTargetItem = null
                    scope.launch { app.vaultManager.moveToTrash(itemToTrash.id) }
                }) {
                    Text("Move to Trash", color = IslandColors.CrimsonBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { cropTargetItem = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ShelfMiniActionChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(Color(0xD41B1D23))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun formatRelativeDay(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
    return when {
        now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> "Today"
        now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestampMs))
    }
}

private fun formatTimeAgo(timestampMs: Long): String {
    val diffSec = ((System.currentTimeMillis() - timestampMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86_400L -> "${diffSec / 3600L}h ago"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestampMs))
    }
}
