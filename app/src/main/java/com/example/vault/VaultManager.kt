package com.example.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.island.AlertKind
import com.example.island.IslandActivity
import com.example.island.IslandController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class VaultManager(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val islandController: IslandController,
    private val onVaultAddedSound: () -> Unit = {}
) {
    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
    }

    private val ingestMutex = Mutex()
    private val recentIngested = ConcurrentHashMap<String, Long>()

    val activeItems: Flow<List<VaultItemEntity>> = vaultDao.observeActive()
    val trashedItems: Flow<List<VaultItemEntity>> = vaultDao.observeTrashed()

    suspend fun saveFromUri(uri: Uri, sourceType: String): VaultItemEntity? = withContext(Dispatchers.IO) {
        ingestMutex.withLock {
            runCatching {
                val displayName = resolveDisplayName(uri) ?: uri.lastPathSegment ?: "vault_${System.currentTimeMillis()}"
                val dedupeKey = "${uri.toString()}|$displayName"
                if (sourceType.startsWith("SCREENSHOT") && wasRecentlyIngested(dedupeKey, displayName)) {
                    return@withContext null
                }

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when {
                    mimeType.contains("png", ignoreCase = true) -> "png"
                    mimeType.contains("webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }
                val finalName = if (displayName.contains('.')) displayName else "$displayName.$ext"
                val destFile = File(vaultDir, "${UUID.randomUUID()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                if (destFile.length() == 0L) {
                    destFile.delete()
                    return@withContext null
                }

                markIngested(dedupeKey, finalName)
                insertAndSchedule(destFile, mimeType, finalName, sourceType)
            }.getOrNull()
        }
    }

    suspend fun saveBitmap(bitmap: Bitmap, sourceType: String, namePrefix: String = "capture"): VaultItemEntity? =
        withContext(Dispatchers.IO) {
            ingestMutex.withLock {
                runCatching {
                    val now = System.currentTimeMillis()
                    val displayName = "${namePrefix}_$now.png"
                    if (sourceType.startsWith("SCREENSHOT") && wasRecentlyIngested(displayName, namePrefix)) {
                        return@withContext null
                    }
                    val destFile = File(vaultDir, "${UUID.randomUUID()}.png")
                    FileOutputStream(destFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    markIngested(displayName, namePrefix)
                    insertAndSchedule(destFile, "image/png", displayName, sourceType)
                }.getOrNull()
            }
        }

    /**
     * Reads newly created screenshot documents from a user-selected SAF Screenshots folder
     * (zero broad storage permissions required — strictly scoped to chosen tree URI).
     */
    suspend fun ingestRecentScreenshotFromTree(treeUri: Uri): VaultItemEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val cutoffMs = now - 25_000L
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            var newestDocId: String? = null
            var newestModified = 0L

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx).orEmpty() else ""
                    val modified = if (modIdx >= 0) cursor.getLong(modIdx) else 0L

                    val isImage = mime.startsWith("image/", ignoreCase = true) ||
                        name.endsWith(".png", ignoreCase = true) ||
                        name.endsWith(".jpg", ignoreCase = true) ||
                        name.endsWith(".jpeg", ignoreCase = true) ||
                        name.endsWith(".webp", ignoreCase = true)
                    if (!isImage || name.startsWith(".pending")) continue
                    if (modified in cutoffMs..now + 5_000L && modified >= newestModified) {
                        newestModified = modified
                        newestDocId = docId
                    }
                }
            }

            val docId = newestDocId ?: return@withContext null
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            saveFromUri(docUri, sourceType = "SCREENSHOT_AUTO")
        }.getOrNull()
    }

    private fun wasRecentlyIngested(key: String, name: String): Boolean {
        val now = System.currentTimeMillis()
        recentIngested.entries.removeIf { now - it.value > 15_000L }
        return recentIngested.containsKey(key) ||
            (name.isNotBlank() && recentIngested.containsKey("name:$name"))
    }

    private fun markIngested(key: String, name: String) {
        val now = System.currentTimeMillis()
        recentIngested[key] = now
        if (name.isNotBlank()) {
            recentIngested["name:$name"] = now
        }
    }

    private suspend fun insertAndSchedule(
        file: File,
        mimeType: String,
        displayName: String,
        sourceType: String
    ): VaultItemEntity {
        val now = System.currentTimeMillis()
        val expiresAt = now + VaultConstants.EXPIRY_MILLIS
        val entity = VaultItemEntity(
            filePath = file.absolutePath,
            mimeType = mimeType,
            displayName = displayName,
            sourceType = sourceType,
            createdAt = now,
            expiresAt = expiresAt,
            isTrashed = false
        )
        val id = vaultDao.insert(entity)
        val saved = entity.copy(id = id)
        scheduleExpiryWork(id, VaultConstants.EXPIRY_MILLIS)
        onVaultAddedSound()

        islandController.upsertActivity(
            IslandActivity.Alert(
                id = "vault_saved_$id",
                kind = AlertKind.VAULT_EXPIRING,
                title = "Saved to Photo Vault",
                subtitle = "$displayName · expires in 10 min"
            ),
            autoShowCompact = true,
            autoDismissMs = 3500L
        )
        return saved
    }

    suspend fun moveToTrash(id: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val purgeAt = now + VaultConstants.TRASH_RETENTION_MILLIS
        vaultDao.moveToTrash(id, now, purgeAt)
        schedulePurgeWork(id, VaultConstants.TRASH_RETENTION_MILLIS)
    }

    suspend fun restoreItem(id: Long) = withContext(Dispatchers.IO) {
        val newExpiresAt = System.currentTimeMillis() + VaultConstants.EXPIRY_MILLIS
        vaultDao.restore(id, newExpiresAt)
        scheduleExpiryWork(id, VaultConstants.EXPIRY_MILLIS)
    }

    suspend fun purgeItemNow(id: Long) = withContext(Dispatchers.IO) {
        val item = vaultDao.getById(id)
        if (item != null) {
            runCatching { File(item.filePath).delete() }
            vaultDao.deleteById(id)
        }
        WorkManager.getInstance(context).cancelUniqueWork("vault_expire_$id")
        WorkManager.getInstance(context).cancelUniqueWork("vault_purge_$id")
    }

    suspend fun processExpirations() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expired = vaultDao.findExpiredActive(now)
        expired.forEach { item ->
            val purgeAt = now + VaultConstants.TRASH_RETENTION_MILLIS
            vaultDao.moveToTrash(item.id, now, purgeAt)
            schedulePurgeWork(item.id, VaultConstants.TRASH_RETENTION_MILLIS)
        }
        if (expired.isNotEmpty()) {
            islandController.upsertActivity(
                IslandActivity.Alert(
                    id = "vault_expired_${System.currentTimeMillis()}",
                    kind = AlertKind.VAULT_EXPIRING,
                    title = "${expired.size} Vault item(s) moved to Trash",
                    subtitle = "Open Vault to restore (24h window)"
                ),
                autoShowCompact = true,
                autoDismissMs = 5000L
            )
        }

        val readyToPurge = vaultDao.findReadyToPurge(now)
        readyToPurge.forEach { item ->
            runCatching { File(item.filePath).delete() }
            vaultDao.deleteById(item.id)
        }
    }

    fun getFileProviderUri(item: VaultItemEntity): Uri? {
        val file = File(item.filePath)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildDragClipData(item: VaultItemEntity): ClipData? {
        val uri = getFileProviderUri(item) ?: return null
        return ClipData.newUri(context.contentResolver, item.displayName, uri)
    }

    /**
     * Two-way attach:
     * 1) Puts a content-URI ClipData on the system clipboard so rich chat keyboards show a 1-tap paste image chip.
     * 2) If a foreground chat app supports ACTION_SEND image, sends directly into that app; otherwise opens Share Chooser.
     */
    fun attachOrShareItem(item: VaultItemEntity, targetChatPackage: String? = null) {
        val uri = getFileProviderUri(item) ?: return

        // Copy image URI to clipboard so user can paste into any open chat box too
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newUri(context.contentResolver, item.displayName, uri))
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType.ifBlank { "image/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(item.displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (!targetChatPackage.isNullOrBlank() && targetChatPackage != context.packageName) {
            val directIntent = Intent(sendIntent).setPackage(targetChatPackage)
            val handlers = context.packageManager.queryIntentActivities(directIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (handlers.isNotEmpty()) {
                runCatching {
                    context.grantUriPermission(targetChatPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(directIntent)
                    islandController.showTransientBanner("Attached image to chat")
                }
                return
            }
        }

        val resolved = context.packageManager.queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY)
        resolved.forEach { info ->
            runCatching {
                context.grantUriPermission(info.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val chooser = Intent.createChooser(sendIntent, "Attach ${item.displayName}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(chooser) }
    }

    fun scheduleExpiryWork(itemId: Long, delayMs: Long) {
        val req = OneTimeWorkRequestBuilder<VaultExpiryWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(VaultExpiryWorker.KEY_ITEM_ID to itemId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "vault_expire_$itemId",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun schedulePurgeWork(itemId: Long, delayMs: Long) {
        val req = OneTimeWorkRequestBuilder<VaultPurgeWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(VaultExpiryWorker.KEY_ITEM_ID to itemId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "vault_purge_$itemId",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    suspend fun clearAllVaultFiles() = withContext(Dispatchers.IO) {
        vaultDir.listFiles()?.forEach { runCatching { it.delete() } }
        vaultDao.clearAll()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }
}
