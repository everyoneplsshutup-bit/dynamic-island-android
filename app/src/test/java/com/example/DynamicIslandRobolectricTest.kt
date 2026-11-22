package com.example

import android.content.ComponentName
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Rect
import androidx.compose.animation.core.SnapSpec
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.floating.FloatingWindowManager
import com.example.island.Build4Checklist
import com.example.island.IslandActivity
import com.example.island.IslandController
import com.example.island.IslandMode
import com.example.island.OcrSelectionState
import com.example.island.OcrTextToken
import com.example.ocr.OcrPrivacyGate
import com.example.ocr.TierAAccessibilityReader
import com.example.ocr.TierAResult
import com.example.system.AnimationStyle
import com.example.system.CalculatorEngine
import com.example.system.CompanionPosition
import com.example.system.EventKind
import com.example.system.IslandSettings
import com.example.system.IslandSoundEvent
import com.example.system.IslandTab
import com.example.system.SettingsRepository
import com.example.system.TimerEngine
import com.example.ui.theme.MotionTokens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DynamicIslandRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `island state machine transitions Idle Compact Split Expanded and emits distinct sounds`() {
        val events = mutableListOf<IslandSoundEvent>()
        val controller = IslandController(onSoundEvent = { events.add(it) })
        assertEquals(IslandMode.IDLE, controller.state.value.mode)

        controller.upsertActivity(
            IslandActivity.Music(
                packageName = "com.unshoo.pixelmusic",
                title = "Starlight",
                artist = "Muse",
                isPlaying = true,
                positionMs = 1000L,
                durationMs = 180_000L
            )
        )
        assertEquals(IslandMode.COMPACT, controller.state.value.mode)

        controller.upsertActivity(
            IslandActivity.Timer(
                label = "Timer",
                remainingMs = 60_000L,
                totalMs = 60_000L,
                isRunning = true
            )
        )
        assertEquals(IslandMode.SPLIT, controller.state.value.mode)

        controller.expand(IslandTab.CALC)
        assertEquals(IslandMode.EXPANDED, controller.state.value.mode)
        assertEquals(IslandTab.CALC, controller.state.value.activeTab)

        controller.selectTab(IslandTab.TIMER)
        controller.selectTab(IslandTab.SEARCH)
        assertTrue(controller.state.value.needsKeyboardFocus)

        controller.toggleMoreRow()
        controller.collapse()
        assertEquals(IslandMode.SPLIT, controller.state.value.mode)
        assertFalse(controller.state.value.needsKeyboardFocus)

        assertEquals(
            listOf(
                IslandSoundEvent.EXPAND,
                IslandSoundEvent.TAP,
                IslandSoundEvent.SEARCH_OPEN,
                IslandSoundEvent.TAP,
                IslandSoundEvent.COLLAPSE
            ),
            events
        )
    }

    @Test
    fun `two-stage open, companion swap, MotionTokens, and Build4Checklist work cleanly`() = runTest {
        assertEquals(12, Build4Checklist.entries.size)

        val controller = IslandController()
        controller.upsertActivity(
            IslandActivity.Music(
                packageName = "com.unshoo.pixelmusic",
                title = "Track",
                artist = "Artist",
                isPlaying = true,
                positionMs = 0L,
                durationMs = 120_000L
            )
        )
        controller.upsertActivity(
            IslandActivity.Timer(
                label = "Timer",
                remainingMs = 30_000L,
                totalMs = 30_000L,
                isRunning = true
            )
        )
        val initialPrimaryId = controller.state.value.primaryActivity?.id
        val initialSecondaryId = controller.state.value.secondaryActivity?.id
        assertNotNull(initialPrimaryId)
        assertNotNull(initialSecondaryId)
        controller.swapPrimaryAndSecondary()
        assertEquals(initialSecondaryId, controller.state.value.primaryActivity?.id)

        // Two-stage open: Compact/Split -> MEDIUM -> EXPANDED
        controller.openFromPill(twoStageOpen = true)
        assertEquals(IslandMode.MEDIUM, controller.state.value.mode)
        controller.openFromPill(twoStageOpen = true)
        assertEquals(IslandMode.EXPANDED, controller.state.value.mode)
        controller.collapse()
        controller.openFromPill(twoStageOpen = false)
        assertEquals(IslandMode.EXPANDED, controller.state.value.mode)

        // MotionTokens curves & NONE snap (including OpeningEffect & non-bouncing collapse)
        assertTrue(MotionTokens.idleToMediumSpec<Float>(AnimationStyle.NONE, 0.5f, 1.0f) is SnapSpec<*>)
        assertTrue(MotionTokens.collapseSpec<Float>(AnimationStyle.NONE, 1.0f) is SnapSpec<*>)
        assertTrue(MotionTokens.openingEffectSpec<Float>(com.example.system.OpeningEffect.LIQUID, AnimationStyle.NONE, 0.5f, 1.0f) is SnapSpec<*>)
        assertFalse(MotionTokens.idleToMediumSpec<Float>(AnimationStyle.FRUIT, 0.8f, 1.0f) is SnapSpec<*>)
        assertFalse(MotionTokens.openingEffectSpec<Float>(com.example.system.OpeningEffect.POP, AnimationStyle.FRUIT, 0.8f, 1.0f) is SnapSpec<*>)

        // No-flicker rule: duplicate activity updates keep order and do not steal front
        val sameNotif = IslandActivity.NotificationPreview(
            id = "notif_1",
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alex",
            text = "On my way"
        )
        val secondNotif = IslandActivity.NotificationPreview(
            id = "notif_2",
            packageName = "com.telegram",
            appName = "Telegram",
            title = "Sam",
            text = "See you at 5"
        )
        controller.upsertActivity(sameNotif)
        controller.upsertActivity(secondNotif)
        val queueSizeBeforeDup = controller.state.value.activityQueue.size
        controller.upsertActivity(sameNotif) // Visually identical: ignored
        assertEquals(queueSizeBeforeDup, controller.state.value.activityQueue.size)

        // Notification cycling & dismiss + 15s reaper pruneStaleNotifications
        controller.nextNotification()
        controller.dismissActiveNotification()
        controller.pruneStaleNotifications(emptySet())
        assertTrue(controller.state.value.activityQueue.none { it is IslandActivity.NotificationPreview })

        // Low-battery warning <= 15% sets red glyph state and resets above threshold
        controller.updateBatteryTelemetry(levelPercent = 15, isCharging = false, isLowBatteryWarning = true)
        assertEquals(15, controller.state.value.batteryLevelPercent)
        assertTrue(controller.state.value.isLowBatteryWarning)
        controller.updateBatteryTelemetry(levelPercent = 30, isCharging = true, isLowBatteryWarning = false)
        assertFalse(controller.state.value.isLowBatteryWarning)

        // Settings persistence for Build #4 options + Opening/Content effects & Swipe actions
        val repo = SettingsRepository(composeRule.activity)
        repo.resetSettings()
        repo.update {
            it.copy(
                animationStyle = AnimationStyle.SPRING,
                openingEffect = com.example.system.OpeningEffect.POP,
                contentEffect = com.example.system.ContentEffect.SLIDE_UP,
                pillSwipeLeftGesture = com.example.system.PillSwipeAction.VOLUME,
                pillSwipeRightGesture = com.example.system.PillSwipeAction.SEARCH,
                expandedSwipeLeftGesture = com.example.system.ExpandedSwipeAction.COLLAPSE,
                expandedSwipeRightGesture = com.example.system.ExpandedSwipeAction.DISMISS,
                companionPosition = CompanionPosition.LEFT,
                twoStageOpen = false,
                animationElasticity = 0.9f
            )
        }
        val updated = repo.settingsFlow.first()
        assertEquals(AnimationStyle.SPRING, updated.animationStyle)
        assertEquals("Standard", updated.animationStyle.label)
        assertEquals("Springy", AnimationStyle.FRUIT.label)
        assertEquals("Calm", AnimationStyle.SMOOTH.label)
        assertEquals(com.example.system.OpeningEffect.POP, updated.openingEffect)
        assertEquals(com.example.system.ContentEffect.SLIDE_UP, updated.contentEffect)
        assertEquals(com.example.system.PillSwipeAction.VOLUME, updated.pillSwipeLeftGesture)
        assertEquals(com.example.system.PillSwipeAction.SEARCH, updated.pillSwipeRightGesture)
        assertEquals(com.example.system.ExpandedSwipeAction.COLLAPSE, updated.expandedSwipeLeftGesture)
        assertEquals(com.example.system.ExpandedSwipeAction.DISMISS, updated.expandedSwipeRightGesture)
        assertEquals(CompanionPosition.LEFT, updated.companionPosition)
        assertFalse(updated.twoStageOpen)
        assertEquals(0.9f, updated.animationElasticity, 0.01f)
        assertTrue(updated.eventConfig(EventKind.CHARGING).enabled)
        repo.resetSettings()
    }

    @Test
    fun `search engine returns more than 8 apps uncapped and OCR selection copies only explicit range`() = runTest {
        val app = composeRule.activity.application as DynamicIslandApp
        val shadowPm = Shadows.shadowOf(composeRule.activity.packageManager)
        val letters = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu")
        letters.forEachIndexed { idx, name ->
            val pkg = "com.example.fakeapp.$idx"
            val actName = "$pkg.MainActivity"
            val comp = ComponentName(pkg, actName)
            shadowPm.addActivityIfNotPresent(comp)
            val filter = IntentFilter(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            shadowPm.addIntentFilterForActivity(comp, filter)
            val ri = ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = pkg
                    this.name = actName
                }
                nonLocalizedLabel = name
            }
            shadowPm.addResolveInfoForIntent(
                android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER),
                ri
            )
        }

        val allApps = app.searchEngine.getInstalledApps(forceRefresh = true)
        assertTrue("Expected > 8 launchable apps uncapped, got ${allApps.size}", allApps.size >= 12)
        assertTrue(allApps.any { it.sectionLetter == 'A' })
        assertTrue(allApps.any { it.sectionLetter == 'Z' })

        // Verify OCR selection state computes only the selected token slice (never auto-copies)
        val full = "Order 48291 confirmed today"
        val tokens = listOf(
            OcrTextToken("Order", 0, 5, 10, 100, 90, 140),
            OcrTextToken("48291", 6, 11, 100, 100, 190, 140),
            OcrTextToken("confirmed", 12, 21, 200, 100, 340, 140),
            OcrTextToken("today", 22, 27, 350, 100, 440, 140)
        )
        val sel = OcrSelectionState(
            fullText = full,
            elementBounds = Rect(0, 80, 500, 160),
            tokens = tokens,
            startTokenIndex = 1,
            endTokenIndex = 1,
            anchorX = 140f,
            anchorY = 120f
        )
        assertEquals("48291", sel.selectedText)
        val expandedSel = sel.copy(startTokenIndex = 1, endTokenIndex = 2)
        assertEquals("48291 confirmed", expandedSel.selectedText)
    }

    @Test
    fun `all 13 unique UI sound assets exist and respect toggle and volume gating`() {
        assertEquals(13, IslandSoundEvent.entries.size)
        val rawResources = listOf(
            R.raw.sfx_expand,
            R.raw.sfx_collapse,
            R.raw.sfx_tap,
            R.raw.sfx_timer_set,
            R.raw.sfx_timer_done,
            R.raw.sfx_toggle,
            R.raw.sfx_toggle_off,
            R.raw.sfx_calc_key,
            R.raw.sfx_music_play,
            R.raw.sfx_music_pause,
            R.raw.sfx_music_next,
            R.raw.sfx_vault_add,
            R.raw.sfx_search_open
        )
        assertEquals(13, rawResources.distinct().size)
        rawResources.forEach { resId ->
            composeRule.activity.resources.openRawResource(resId).use { stream ->
                val header = ByteArray(12)
                val read = stream.read(header)
                assertEquals(12, read)
                assertEquals("RIFF", String(header, 0, 4))
                assertEquals("WAVE", String(header, 8, 4))
                assertTrue(stream.available() > 32)
            }
        }

        val defaults = IslandSettings()
        assertTrue(defaults.soundEnabled)
        assertEquals(0.75f, defaults.soundVolume, 0.001f)

        var timerDoneFired = false
        val controller = IslandController()
        val engine = TimerEngine(composeRule.activity, controller) {
            timerDoneFired = true
        }
        engine.onTimerCompletedFromAlarm()
        assertTrue(timerDoneFired)

        val app = composeRule.activity.application as DynamicIslandApp
        app.soundManager.updateCachedAudioSettings(enabled = false, volume = 0.5f)
        assertFalse(app.soundManager.play(IslandSoundEvent.EXPAND))
        app.soundManager.updateCachedAudioSettings(enabled = true, volume = 0f)
        assertFalse(app.soundManager.play(IslandSoundEvent.CALC_KEY))
        app.soundManager.updateCachedAudioSettings(enabled = true, volume = 0.75f)
    }

    @Test
    fun `ocr privacy gate defaults OFF and blocks reading when disabled`() = runTest {
        val repo = SettingsRepository(composeRule.activity)
        repo.resetSettings()
        assertFalse(OcrPrivacyGate.isScreenReadingAllowed(repo))
        assertFalse(OcrPrivacyGate.isTierBAllowed(repo))

        val initial = repo.settingsFlow.first()
        assertEquals(IslandSettings.MAX_ISLAND_FEATURES, initial.enabledTabs.size)

        // Adding a 9th tab must be rejected while at 8/8
        val added9th = repo.toggleTabInIsland(IslandTab.CLIPS)
        assertFalse(added9th)
        assertEquals(8, repo.settingsFlow.first().enabledTabs.size)

        // Removing one allows adding another
        assertTrue(repo.toggleTabInIsland(IslandTab.ALERTS))
        assertEquals(7, repo.settingsFlow.first().enabledTabs.size)
        assertTrue(repo.toggleTabInIsland(IslandTab.CLIPS))
        assertEquals(8, repo.settingsFlow.first().enabledTabs.size)

        // Tier A refuses without consent even if called directly
        val refused = TierAAccessibilityReader.readNodeAtPoint(
            service = null,
            screenReadingAllowed = false,
            x = 100,
            y = 100,
            ownPackageName = composeRule.activity.packageName
        )
        assertTrue(refused is TierAResult.Refused)
    }

    @Test
    fun `floating window manager builds freeform windowing mode 5 bundle`() {
        val app = composeRule.activity.application as DynamicIslandApp
        val bundle = app.floatingWindowManager.buildFreeformOptionsBundle(Rect(40, 120, 680, 960))
        assertEquals(
            FloatingWindowManager.WINDOWING_MODE_FREEFORM,
            bundle.getInt(FloatingWindowManager.KEY_LAUNCH_WINDOWING_MODE)
        )
    }

    @Test
    fun `calculator swipe backspace removes trailing digits cleanly`() {
        val calc = CalculatorEngine()
        calc.pressDigit("1")
        calc.pressDigit("2")
        calc.pressDigit("3")
        assertEquals("123", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("12", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("1", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("0", calc.state.value.display)
        calc.pressDigit("5")
        calc.pressToggleSign()
        assertEquals("-5", calc.state.value.display)
        calc.pressBackspace()
        assertEquals("0", calc.state.value.display)
    }

    @Test
    fun `main screen opens settings 6 groups, setup wizard, tools screenshot vault, sleep dial, and interactive calculator`() {
        composeRule.waitForIdle()

        // Switch sandbox tab to Tools: Screenshot->Vault exists and Floating Apps button does NOT exist
        composeRule.onNodeWithTag("chooser_tab_tools").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tools_screenshot_vault_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("tools_floating_apps_btn").assertDoesNotExist()

        // Switch sandbox tab to Timer and verify Sleep Dial tab renders
        composeRule.onNodeWithTag("chooser_tab_timer").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timer_mode_sleep").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sleep_start_button").assertIsDisplayed()

        // Switch sandbox tab to Calc and verify 7 * 8 = 56
        composeRule.onNodeWithTag("chooser_tab_calc").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calc_key_7").performClick()
        composeRule.onNodeWithTag("calc_key_multiply").performClick()
        composeRule.onNodeWithTag("calc_key_8").performClick()
        composeRule.onNodeWithTag("calc_key_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calc_display").assertIsDisplayed()
        composeRule.onNodeWithText("56").assertIsDisplayed()

        // Open Settings and verify all 6 groups render
        composeRule.onNodeWithTag("open_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_group_look").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_group_motion").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_group_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_features"))
        composeRule.onNodeWithTag("settings_group_features").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_privacy"))
        composeRule.onNodeWithTag("settings_group_privacy").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_about"))
        composeRule.onNodeWithTag("settings_group_about").assertIsDisplayed()

        // Open Motion settings and verify Build #4 Motion controls + subtabs + Sound Effects + Try It demo
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_motion"))
        composeRule.onNodeWithTag("settings_group_motion").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("motion_subtab_background").assertIsDisplayed()
        composeRule.onNodeWithTag("motion_subtab_animations").assertIsDisplayed()
        composeRule.onNodeWithTag("motion_subtab_gestures").assertIsDisplayed()
        composeRule.onNodeWithTag("motion_style_fruit").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("opening_effect_liquid"))
        composeRule.onNodeWithTag("opening_effect_liquid").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("content_effect_blur_in"))
        composeRule.onNodeWithTag("content_effect_blur_in").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_switch"))
        composeRule.onNodeWithTag("motion_sound_switch").assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_volume_slider"))
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_try_it_btn"))
        composeRule.onNodeWithTag("motion_try_it_btn").assertIsDisplayed()

        // Toggling Sound Effects OFF disables the volume slider; toggling back ON restores it
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_switch"))
        composeRule.onNodeWithTag("motion_sound_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("motion_sound_switch").assertIsOff()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_volume_slider"))
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_switch"))
        composeRule.onNodeWithTag("motion_sound_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("motion_sound_switch").assertIsOn()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_volume_slider"))
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsEnabled()

        // Navigate back, open Pill group & verify Companion Circle Left/Right side toggle
        composeRule.onNodeWithTag("settings_detail_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_pill"))
        composeRule.onNodeWithTag("settings_group_pill").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pill_companion_left").assertIsDisplayed()
        composeRule.onNodeWithTag("pill_companion_right").assertIsDisplayed()

        // Navigate back to Home and open Setup Wizard
        composeRule.onNodeWithTag("settings_detail_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("open_wizard_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_acc_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_screenshots_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_finish_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
    }
}
