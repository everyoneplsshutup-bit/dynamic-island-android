package com.example.vault

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class VaultManager(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val islandController: IslandController
) {
    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
    }

    val activeItems: Flow<List<VaultItemEntity>> = vaultDao.observeActive()
    val trashedItems: Flow<List<VaultItemEntity>> = vaultDao.observeTrashed()

    suspend fun saveFromUri(uri: Uri, sourceType: String): VaultItemEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val displayName = resolveDisplayName(uri) ?: "vault_${System.currentTimeMillis()}.$ext"
            val destFile = File(vaultDir, "${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            insertAndSchedule(destFile, mimeType, displayName, sourceType)
        }.getOrNull()
    }

    suspend fun saveBitmap(bitmap: Bitmap, sourceType: String, namePrefix: String = "capture"): VaultItemEntity? =
        withContext(Dispatchers.IO) {
            runCatching {
                val displayName = "${namePrefix}_${System.currentTimeMillis()}.png"
                val destFile = File(vaultDir, "${UUID.randomUUID()}.png")
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                insertAndSchedule(destFile, "image/png", displayName, sourceType)
            }.getOrNull()
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

    fun attachOrShareItem(item: VaultItemEntity) {
        val uri = getFileProviderUri(item) ?: return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
