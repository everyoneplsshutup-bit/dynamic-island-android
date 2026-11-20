package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.floating.FloatingWindowManager
import com.example.island.IslandController
import com.example.media.MediaHub
import com.example.notes.ClipboardMonitor
import com.example.notes.ShareToIslandActivity
import com.example.notes.VoiceMemoManager
import com.example.search.SearchEngine
import com.example.system.AppDatabase
import com.example.system.BatteryMonitor
import com.example.system.CalculatorEngine
import com.example.system.IslandSoundManager
import com.example.system.QuickTogglesManager
import com.example.system.SettingsRepository
import com.example.system.TimerEngine
import com.example.vault.VaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DynamicIslandApp : Application() {

    companion object {
        const val CHANNEL_KEEP_ALIVE = "island_keep_alive"
        const val CHANNEL_ALERTS = "island_alerts"
        const val CHANNEL_ACTIVE_RECORDING = "island_active_recording"
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var soundManager: IslandSoundManager
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var islandController: IslandController
        private set
    lateinit var calculatorEngine: CalculatorEngine
        private set
    lateinit var timerEngine: TimerEngine
        private set
    lateinit var batteryMonitor: BatteryMonitor
        private set
    lateinit var quickTogglesManager: QuickTogglesManager
        private set
    lateinit var mediaHub: MediaHub
        private set
    lateinit var vaultManager: VaultManager
        private set
    lateinit var voiceMemoManager: VoiceMemoManager
        private set
    lateinit var clipboardMonitor: ClipboardMonitor
        private set
    lateinit var floatingWindowManager: FloatingWindowManager
        private set
    lateinit var searchEngine: SearchEngine
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        settingsRepository = SettingsRepository(this)
        soundManager = IslandSoundManager(this, settingsRepository)
        database = AppDatabase.getInstance(this)
        islandController = IslandController(onSoundEvent = { event -> soundManager.play(event) })
        calculatorEngine = CalculatorEngine()
        timerEngine = TimerEngine(this, islandController, onTimerCompletedSound = { soundManager.playTimerDone() })
        batteryMonitor = BatteryMonitor(this, islandController)
        quickTogglesManager = QuickTogglesManager(this)
        mediaHub = MediaHub(this, islandController)
        vaultManager = VaultManager(this, database.vaultDao(), islandController) {
            soundManager.playVaultAdd()
        }
        voiceMemoManager = VoiceMemoManager(this, database.voiceMemoDao(), islandController)
        clipboardMonitor = ClipboardMonitor(this, database.clipboardDao())
        floatingWindowManager = FloatingWindowManager(this, settingsRepository)
        searchEngine = SearchEngine(this, database.noteDao(), floatingWindowManager)

        batteryMonitor.start()
        publishDirectShareShortcuts()

        appScope.launch {
            vaultManager.processExpirations()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val keepAlive = NotificationChannel(
            CHANNEL_KEEP_ALIVE,
            "Dynamic Island Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the Dynamic Island monitor active"
            setShowBadge(false)
        }

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            "Dynamic Island Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer completions, vault alerts, and Accessibility disconnect warnings"
        }

        val recording = NotificationChannel(
            CHANNEL_ACTIVE_RECORDING,
            "Active Recording & Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown during active voice memo recording or single-frame capture"
        }

        nm.createNotificationChannels(listOf(keepAlive, alerts, recording))
    }

    private fun publishDirectShareShortcuts() {
        runCatching {
            val vaultShortcut = ShortcutInfoCompat.Builder(this, "direct_save_to_vault")
                .setShortLabel(getString(R.string.action_save_to_vault))
                .setLongLabel(getString(R.string.action_save_to_vault))
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setCategories(setOf("com.example.category.SAVE_TO_VAULT"))
                .setIntent(Intent(this, ShareToIslandActivity::class.java).setAction(Intent.ACTION_SEND))
                .setLongLived(true)
                .build()

            val linkShortcut = ShortcutInfoCompat.Builder(this, "direct_save_link")
                .setShortLabel(getString(R.string.action_save_link))
                .setLongLabel(getString(R.string.action_save_link))
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setCategories(setOf("com.example.category.SAVE_LINK"))
                .setIntent(Intent(this, ShareToIslandActivity::class.java).setAction(Intent.ACTION_SEND))
                .setLongLived(true)
                .build()

            ShortcutManagerCompat.addDynamicShortcuts(this, listOf(vaultShortcut, linkShortcut))
        }
    }

    suspend fun deleteAllUserData() {
        database.notificationDao().clearAll()
        database.appRuleDao().clearAll()
        database.noteDao().clearAll()
        database.voiceMemoDao().clearAll()
        database.clipboardDao().clearAll()
        database.savedLinkDao().clearAll()
        vaultManager.clearAllVaultFiles()
        settingsRepository.resetSettings()
    }
}
