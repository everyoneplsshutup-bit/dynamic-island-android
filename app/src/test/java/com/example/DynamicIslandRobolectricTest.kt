package com.example

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
import com.example.island.IslandActivity
import com.example.island.IslandController
import com.example.island.IslandMode
import com.example.ocr.OcrPrivacyGate
import com.example.system.IslandSettings
import com.example.system.IslandSoundEvent
import com.example.system.IslandTab
import com.example.system.SettingsRepository
import com.example.system.TimerEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DynamicIslandRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `island state machine transitions Idle Compact Split Expanded`() {
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
        controller.toggleMoreRow()
        controller.collapse()
        assertEquals(IslandMode.SPLIT, controller.state.value.mode)

        assertEquals(
            listOf(
                IslandSoundEvent.EXPAND,
                IslandSoundEvent.TAP,
                IslandSoundEvent.TAP,
                IslandSoundEvent.COLLAPSE
            ),
            events
        )
    }

    @Test
    fun `timer completion fires sound callback and sound defaults are enabled`() {
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
        assertFalse(app.soundManager.play(IslandSoundEvent.TAP))
        app.soundManager.updateCachedAudioSettings(enabled = true, volume = 0.75f)
    }

    @Test
    fun `feature picker enforces 8 feature hard cap`() = runTest {
        val repo = SettingsRepository(composeRule.activity)
        repo.resetSettings()
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
        assertFalse(OcrPrivacyGate.isScreenReadingAllowed(repo))
    }

    @Test
    fun `main screen opens settings 6 groups, setup wizard, and interactive calculator`() {
        composeRule.waitForIdle()

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

        // Open Motion settings and verify Sound Effects toggle (default ON) + Sound Volume next to Haptics Strength
        composeRule.onNodeWithTag("settings_home_list").performScrollToNode(hasTestTag("settings_group_motion"))
        composeRule.onNodeWithTag("settings_group_motion").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Haptics Strength").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_switch"))
        composeRule.onNodeWithTag("motion_sound_switch").assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithTag("settings_detail_list").performScrollToNode(hasTestTag("motion_sound_volume_slider"))
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsDisplayed().assertIsEnabled()

        // Toggling Sound Effects OFF disables the volume slider; toggling back ON restores it
        composeRule.onNodeWithTag("motion_sound_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("motion_sound_switch").assertIsOff()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsNotEnabled()
        composeRule.onNodeWithTag("motion_sound_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            runCatching {
                composeRule.onNodeWithTag("motion_sound_switch").assertIsOn()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("motion_sound_volume_slider").assertIsEnabled()

        // Navigate back to Home and open Setup Wizard
        composeRule.onNodeWithTag("settings_detail_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("open_wizard_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wizard_acc_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("wizard_finish_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
    }
}
