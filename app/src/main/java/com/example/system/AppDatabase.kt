package com.example.system

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notes.ClipboardDao
import com.example.notes.ClipboardEntity
import com.example.notes.NoteDao
import com.example.notes.NoteEntity
import com.example.notes.SavedLinkDao
import com.example.notes.SavedLinkEntity
import com.example.notes.VoiceMemoDao
import com.example.notes.VoiceMemoEntity
import com.example.notifications.AppRuleDao
import com.example.notifications.AppRuleEntity
import com.example.notifications.NotificationDao
import com.example.notifications.NotificationEntity
import com.example.vault.VaultDao
import com.example.vault.VaultItemEntity

@Database(
    entities = [
        NotificationEntity::class,
        AppRuleEntity::class,
        NoteEntity::class,
        VoiceMemoEntity::class,
        ClipboardEntity::class,
        SavedLinkEntity::class,
        VaultItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun noteDao(): NoteDao
    abstract fun voiceMemoDao(): VoiceMemoDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun savedLinkDao(): SavedLinkDao
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dynamic_island.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
