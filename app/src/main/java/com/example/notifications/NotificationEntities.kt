package com.example.notifications

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notifications_inbox")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val category: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val allowIslandPreview: Boolean = true,
    val isExemptFromAutoCollapse: Boolean = false
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications_inbox ORDER BY timestamp DESC LIMIT 200")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications_inbox WHERE isRead = 0 ORDER BY timestamp DESC")
    fun observeUnread(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NotificationEntity): Long

    @Query("UPDATE notifications_inbox SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notifications_inbox SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notifications_inbox WHERE notificationKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM notifications_inbox")
    suspend fun clearAll()
}

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun observeAll(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRuleEntity)

    @Query("UPDATE app_rules SET allowIslandPreview = :allowed WHERE packageName = :pkg")
    suspend fun setIslandPreviewAllowed(pkg: String, allowed: Boolean)

    @Query("DELETE FROM app_rules")
    suspend fun clearAll()
}
