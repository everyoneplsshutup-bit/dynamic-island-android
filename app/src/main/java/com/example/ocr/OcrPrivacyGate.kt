package com.example.ocr

import com.example.system.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object OcrPrivacyGate {

    const val DISCLOSURE_TITLE = "Opt-in Single-Node Screen Reading"
    const val DISCLOSURE_BODY = """When you turn this ON and enter Select Mode:
• Only the exact element you long-press is inspected.
• Dynamic Island never reads the whole screen, never runs in the background, and never logs or transmits anything off your device.
• Password fields, FLAG_SECURE screens, and sensitive system nodes are always refused.
• You can turn this OFF at any time in Settings → Privacy."""

    fun observeAllowed(settingsRepository: SettingsRepository): Flow<Boolean> =
        settingsRepository.settingsFlow.map { it.screenReadingEnabled && it.ocrFeatureEnabled }

    suspend fun isScreenReadingAllowed(settingsRepository: SettingsRepository): Boolean {
        val s = settingsRepository.settingsFlow.first()
        return s.screenReadingEnabled && s.ocrFeatureEnabled
    }

    suspend fun isTierBAllowed(settingsRepository: SettingsRepository): Boolean {
        val s = settingsRepository.settingsFlow.first()
        return s.screenReadingEnabled && s.ocrFeatureEnabled && s.ocrTierBEnabled
    }
}
