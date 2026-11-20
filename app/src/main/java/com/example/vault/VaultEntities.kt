package com.example.vault

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val mimeType: String,
    val displayName: String,
    val sourceType: String, // "SCREENSHOT_3FINGER", "SHARED", "DRAGGED", "PICKED", "CAPTURED"
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + VaultConstants.EXPIRY_MILLIS,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val purgeAt: Long? = null
)

object VaultConstants {
    const val EXPIRY_MILLIS = 10L * 60L * 1000L // 10 minutes
    const val TRASH_RETENTION_MILLIS = 24L * 60L * 60L * 1000L // 24 hours
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items WHERE isTrashed = 0 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashed(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultItemEntity?

    @Query("SELECT * FROM vault_items WHERE isTrashed = 0 AND expiresAt <= :now")
    suspend fun findExpiredActive(now: Long): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE isTrashed = 1 AND purgeAt IS NOT NULL AND purgeAt <= :now")
    suspend fun findReadyToPurge(now: Long): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VaultItemEntity): Long

    @Query("UPDATE vault_items SET isTrashed = 1, trashedAt = :now, purgeAt = :purgeAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, now: Long, purgeAt: Long)

    @Query("UPDATE vault_items SET isTrashed = 0, trashedAt = NULL, purgeAt = NULL, expiresAt = :newExpiresAt WHERE id = :id")
    suspend fun restore(id: Long, newExpiresAt: Long)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM vault_items")
    suspend fun getAllSnapshot(): List<VaultItemEntity>

    @Query("DELETE FROM vault_items")
    suspend fun clearAll()
}
