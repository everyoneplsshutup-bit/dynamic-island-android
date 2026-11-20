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
 * Strictly reads ONLY the exact leaf element containing the user's pressed point (x, y).
 * Never invoked unless OcrPrivacyGate.isScreenReadingAllowed() is true.
 */
object TierAAccessibilityReader {

    private data class CandidateNode(
        val text: String?,
        val contentDesc: String?,
        val bounds: Rect,
        val area: Long,
        val depth: Int
    )

    fun readNodeAtPoint(
        service: AccessibilityService?,
        screenReadingAllowed: Boolean,
        x: Int,
        y: Int,
        ownPackageName: String
    ): TierAResult {
        // HARD PRIVACY CONSTRAINT: Never inspect nodes when OFF
        if (!screenReadingAllowed) {
            return TierAResult.Refused("Screen reading is OFF in Privacy Settings")
        }
        if (service == null) {
            return TierAResult.NoText(null)
        }

        val orderedWindows = runCatching { service.windows }
            .getOrNull()
            .orEmpty()
            .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
            .sortedByDescending { it.layer }

        val targetRoots = mutableListOf<AccessibilityNodeInfo>()
        for (win in orderedWindows) {
            val winBounds = Rect()
            win.getBoundsInScreen(winBounds)
            if (!winBounds.contains(x, y)) continue
            val root = runCatching { win.root }.getOrNull() ?: continue
            targetRoots.add(root)
        }

        if (targetRoots.isEmpty()) {
            val activeRoot = runCatching { service.rootInActiveWindow }.getOrNull()
                ?: return TierAResult.NoText(null)
            targetRoots.add(activeRoot)
        }

        val candidates = mutableListOf<CandidateNode>()
        var hitSensitive = false
        var smallestBounds: Rect? = null
        var smallestArea = Long.MAX_VALUE

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.contains(x, y)) return

            if (isSensitiveOrPassword(node)) {
                hitSensitive = true
                return
            }

            val area = rect.width().toLong().coerceAtLeast(1L) * rect.height().toLong().coerceAtLeast(1L)
            if (area < smallestArea) {
                smallestArea = area
                smallestBounds = Rect(rect)
            }

            val text = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val desc = node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            if (text != null || desc != null) {
                candidates.add(CandidateNode(text, desc, Rect(rect), area, depth))
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                visit(child, depth + 1)
                if (hitSensitive) return
            }
        }

        for (root in targetRoots) {
            // Do not read our own overlay window if it somehow slipped into rootInActiveWindow
            if (root.packageName?.toString() == ownPackageName && targetRoots.size > 1) continue
            visit(root, 0)
            if (hitSensitive) {
                return TierAResult.Refused("Refused: protected password or sensitive screen")
            }
            if (candidates.isNotEmpty()) break
        }

        if (candidates.isEmpty()) {
            return TierAResult.NoText(smallestBounds)
        }

        // Prefer nodes with explicit text over bare contentDescription, smallest bounding box (deepest leaf)
        val withText = candidates.filter { it.text != null }
        val chosen = (if (withText.isNotEmpty()) withText else candidates)
            .minWithOrNull(compareBy<CandidateNode> { it.area }.thenByDescending { it.depth })!!

        val extracted = chosen.text ?: chosen.contentDesc ?: return TierAResult.NoText(chosen.bounds)
        return TierAResult.TextFound(extracted, chosen.bounds)
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
