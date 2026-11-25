package com.example.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.island.AlertKind
import com.example.island.IslandActivity
import com.example.island.IslandController
import com.example.system.EventKind
import com.example.system.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class VaultManager(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val islandController: IslandController,
    private val settingsRepository: SettingsRepository? = null,
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
                val displayName = resolveDisplayName(uri) ?: uri.lastPathSegment ?: "shelf_${System.currentTimeMillis()}"
                val dedupeKey = "${uri}|$displayName"
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
                }.onFailure {
                    android.util.Log.e("VaultManager", "saveBitmap failed", it)
                }.getOrNull()
            }
        }

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
        // Enforce 12-item Shelf cap: expire oldest first before inserting
        val existingActive = vaultDao.getActiveOldestFirst()
        if (existingActive.size >= VaultConstants.MAX_ACTIVE_ITEMS) {
            val overflow = existingActive.size - VaultConstants.MAX_ACTIVE_ITEMS + 1
            existingActive.take(overflow).forEach { old ->
                purgeItemNow(old.id)
            }
        }

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

        val vaultCfg = settingsRepository?.settingsFlow?.first()?.eventConfig(EventKind.VAULT)
        if (vaultCfg?.enabled != false) {
            islandController.upsertActivity(
                IslandActivity.Alert(
                    id = "shelf_saved_$id",
                    kind = AlertKind.VAULT_EXPIRING,
                    title = "Saved to Shelf",
                    subtitle = "$displayName · expires in 10:00"
                ),
                autoShowCompact = true,
                autoDismissMs = 2000L,
                autoExpandToFull = vaultCfg?.autoExpand == true
            )
        }
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

    suspend fun purgeItemNow(id: Long): Unit = withContext(Dispatchers.IO) {
        val item = vaultDao.getById(id)
        if (item != null) {
            runCatching { File(item.filePath).delete() }
            vaultDao.deleteById(id)
        }
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork("vault_expire_$id")
            WorkManager.getInstance(context).cancelUniqueWork("vault_purge_$id")
        }
    }

    suspend fun clearAllActiveItemsImmediate(): Unit = withContext(Dispatchers.IO) {
        val existing = vaultDao.getActiveOldestFirst()
        existing.forEach { item ->
            runCatching { File(item.filePath).delete() }
            vaultDao.deleteById(item.id)
            runCatching {
                WorkManager.getInstance(context).cancelUniqueWork("vault_expire_${item.id}")
                WorkManager.getInstance(context).cancelUniqueWork("vault_purge_${item.id}")
            }
        }
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
                    id = "shelf_expired_${System.currentTimeMillis()}",
                    kind = AlertKind.VAULT_EXPIRING,
                    title = "${expired.size} Shelf item(s) expired",
                    subtitle = "Open Shelf Trash to restore (24h window)"
                ),
                autoShowCompact = true,
                autoDismissMs = 2000L
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
     * Attach -> system clipboard (Build #5 rule: kept as-is; Share is DELETED everywhere).
     */
    fun attachOrShareItem(item: VaultItemEntity, @Suppress("UNUSED_PARAMETER") targetChatPackage: String? = null): Boolean {
        val uri = getFileProviderUri(item) ?: return false
        return runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newUri(context.contentResolver, item.displayName, uri))
            islandController.showTransientBanner("Attached to clipboard — paste in chat")
            true
        }.getOrDefault(false)
    }

    suspend fun cropVaultItem(
        itemId: Long,
        leftFrac: Float,
        topFrac: Float,
        rightFrac: Float,
        bottomFrac: Float
    ): VaultItemEntity? = withContext(Dispatchers.IO) {
        ingestMutex.withLock {
            runCatching {
                val item = vaultDao.getById(itemId) ?: return@withContext null
                val srcFile = File(item.filePath)
                if (!srcFile.exists()) return@withContext null

                val srcBitmap = BitmapFactory.decodeFile(srcFile.absolutePath) ?: return@withContext null
                val w = srcBitmap.width
                val h = srcBitmap.height
                if (w <= 1 || h <= 1) {
                    srcBitmap.recycle()
                    return@withContext null
                }

                val l = leftFrac.coerceIn(0f, 0.92f)
                val t = topFrac.coerceIn(0f, 0.92f)
                val r = rightFrac.coerceIn(l + 0.08f, 1f)
                val b = bottomFrac.coerceIn(t + 0.08f, 1f)

                val x = (l * w).roundToInt().coerceIn(0, w - 1)
                val y = (t * h).roundToInt().coerceIn(0, h - 1)
                val cropW = ((r - l) * w).roundToInt().coerceIn(1, w - x)
                val cropH = ((b - t) * h).roundToInt().coerceIn(1, h - y)

                val cropped = Bitmap.createBitmap(srcBitmap, x, y, cropW, cropH)
                if (cropped !== srcBitmap) {
                    srcBitmap.recycle()
                }

                val destFile = File(vaultDir, "${UUID.randomUUID()}.png")
                FileOutputStream(destFile).use { out ->
                    cropped.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                cropped.recycle()

                if (!destFile.exists() || destFile.length() == 0L) {
                    destFile.delete()
                    return@withContext null
                }

                vaultDao.updateFilePath(item.id, destFile.absolutePath, "image/png")
                if (srcFile.absolutePath != destFile.absolutePath) {
                    runCatching { srcFile.delete() }
                }
                item.copy(filePath = destFile.absolutePath, mimeType = "image/png")
            }.onFailure {
                android.util.Log.e("VaultManager", "cropVaultItem failed", it)
            }.getOrNull()
        }
    }

    suspend fun saveItemToGallery(item: VaultItemEntity): Boolean = withContext(Dispatchers.IO) {
        val src = File(item.filePath)
        if (!src.exists()) return@withContext false
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, item.displayName)
                put(MediaStore.Images.Media.MIME_TYPE, item.mimeType.ifBlank { "image/jpeg" })
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DynamicIsland")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(src).use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

    fun scheduleExpiryWork(itemId: Long, delayMs: Long) {
        runCatching {
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
    }

    fun schedulePurgeWork(itemId: Long, delayMs: Long) {
        runCatching {
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
