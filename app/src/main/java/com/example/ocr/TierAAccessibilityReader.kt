package com.example.ocr

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

sealed interface TierAResult {
    data class TextFound(val text: String, val bounds: Rect) : TierAResult
    data class NoText(val bounds: Rect?) : TierAResult
    data class Refused(val reason: String) : TierAResult
}

/**
 * Tier A OCR: Privacy-first Accessibility node reader.
 * Strictly reads ONLY the deepest node containing the user's long-press point (x, y).
 * Never invoked unless OcrPrivacyGate.isScreenReadingAllowed() is true.
 */
object TierAAccessibilityReader {

    fun readNodeAtPoint(
        service: AccessibilityService,
        screenReadingAllowed: Boolean,
        x: Int,
        y: Int,
        ownPackageName: String
    ): TierAResult {
        // HARD PRIVACY CONSTRAINT: Never call getRootInActiveWindow() or read any node when OFF
        if (!screenReadingAllowed) {
            return TierAResult.Refused("Screen reading is OFF in Privacy Settings")
        }

        val windows = runCatching { service.windows }.getOrNull().orEmpty()
        var targetRoot: AccessibilityNodeInfo? = null

        for (win in windows) {
            if (win.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) continue
            val root = runCatching { win.root }.getOrNull() ?: continue
            if (root.packageName?.toString() == ownPackageName) continue
            val winBounds = Rect()
            win.getBoundsInScreen(winBounds)
            if (winBounds.contains(x, y)) {
                targetRoot = root
                break
            }
        }

        if (targetRoot == null) {
            val activeRoot = runCatching { service.rootInActiveWindow }.getOrNull()
                ?: return TierAResult.NoText(null)
            if (activeRoot.packageName?.toString() == ownPackageName) {
                return TierAResult.NoText(null)
            }
            targetRoot = activeRoot
        }

        val deepest = findDeepestNodeAt(targetRoot, x, y) ?: return TierAResult.NoText(null)
        val nodeBounds = Rect()
        deepest.getBoundsInScreen(nodeBounds)

        // Deliberately refuse password fields & accessibilityDataSensitive nodes
        if (isSensitiveOrPassword(deepest)) {
            return TierAResult.Refused("Refused: protected password or sensitive screen")
        }

        val extracted = deepest.text?.toString()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: deepest.contentDescription?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }

        return if (extracted != null) {
            TierAResult.TextFound(extracted, nodeBounds)
        } else {
            TierAResult.NoText(nodeBounds)
        }
    }

    private fun findDeepestNodeAt(
        node: AccessibilityNodeInfo,
        x: Int,
        y: Int
    ): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null

        var bestChild: AccessibilityNodeInfo? = null
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val candidate = findDeepestNodeAt(child, x, y)
            if (candidate != null) {
                bestChild = candidate
            }
        }
        return bestChild ?: node
    }

    private fun isSensitiveOrPassword(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val inputVariation = node.inputType and InputType.TYPE_MASK_VARIATION
        if (inputVariation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            inputVariation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            inputVariation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            inputVariation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (node.isAccessibilityDataSensitive) return true
        }
        return false
    }
}
