package com.example.system

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notes.ClipboardDao
import com.example.notes.ClipboardEntity
import com.example.notes.NoteDao
import com.example.notes.NoteEntity
import com.example.notes.SavedLinkDao
import com.example.notes.SavedLinkEntity
import com.example.notes.TaskDao
import com.example.notes.TaskEntity
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
        VaultItemEntity::class,
        TaskEntity::class
    ],
    version = 2,
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
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `isDone` INTEGER NOT NULL DEFAULT 0, `dueTimeMs` INTEGER, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE `vault_items` ADD COLUMN `isPermanent` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `vault_items` ADD COLUMN `thumbnailPath` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_rules_new` (`packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `allowIslandPreview` INTEGER NOT NULL, `isExemptFromAutoCollapse` INTEGER NOT NULL, PRIMARY KEY(`packageName`))"
                )
                db.execSQL(
                    "INSERT INTO `app_rules_new` (`packageName`, `appName`, `allowIslandPreview`, `isExemptFromAutoCollapse`) SELECT `packageName`, `appName`, `allowIslandPreview`, `isExemptFromAutoCollapse` FROM `app_rules`"
                )
                db.execSQL("DROP TABLE `app_rules`")
                db.execSQL("ALTER TABLE `app_rules_new` RENAME TO `app_rules`")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dynamic_island.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
