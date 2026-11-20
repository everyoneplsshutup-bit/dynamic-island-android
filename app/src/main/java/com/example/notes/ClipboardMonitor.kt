package com.example.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground-only clipboard history.
 * Android 13 blocks background clipboard reads and throws SecurityException / returns empty.
 * We strictly read only when our app or island overlay is in foreground / user invokes capture.
 */
class ClipboardMonitor(
    private val context: Context,
    private val clipboardDao: ClipboardDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    val historyFlow: Flow<List<ClipboardEntity>> = clipboardDao.observeRecent()

    private val _lastCaptureStatus = MutableStateFlow("Foreground-only · Android 13 blocks background clipboard reads")
    val lastCaptureStatus: StateFlow<String> = _lastCaptureStatus.asStateFlow()

    private var listenerRegistered = false

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        captureIfAllowed(isForeground = true)
    }

    fun setForegroundActive(active: Boolean) {
        val cm = clipboardManager ?: return
        if (active && !listenerRegistered) {
            runCatching {
                cm.addPrimaryClipChangedListener(listener)
                listenerRegistered = true
            }
            captureIfAllowed(isForeground = true)
        } else if (!active && listenerRegistered) {
            runCatching {
                cm.removePrimaryClipChangedListener(listener)
                listenerRegistered = false
            }
        }
    }

    fun captureIfAllowed(isForeground: Boolean): Boolean {
        if (!isForeground) {
            _lastCaptureStatus.value = "Skipped background read (Android 13 restriction)"
            return false
        }
        val cm = clipboardManager ?: return false
        return try {
            val clip = cm.primaryClip ?: return false
            if (clip.itemCount == 0) return false
            val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return false
            saveClipText(text)
            _lastCaptureStatus.value = "Captured in foreground"
            true
        } catch (_: SecurityException) {
            _lastCaptureStatus.value = "Android 13 denied background clipboard access"
            false
        }
    }

    fun copyToClipboard(text: String) {
        val cm = clipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Dynamic Island Clip", text))
        saveClipText(text)
    }

    fun saveClipText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val hash = trimmed.hashCode()
        scope.launch {
            clipboardDao.deleteByHash(hash)
            clipboardDao.insert(ClipboardEntity(content = trimmed, contentHash = hash))
        }
    }

    fun deleteClip(id: Long) {
        scope.launch { clipboardDao.deleteById(id) }
    }

    fun clearAll() {
        scope.launch { clipboardDao.clearAll() }
    }
}
