package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.island.OcrSelectionState
import com.example.island.OcrTextToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot

data class TierBSelectionResult(
    val text: String,
    val selectionState: OcrSelectionState
)

/**
 * Tier B OCR: Opt-in fallback only when Tier A finds no text.
 * Crops strictly around the pressed element / tap point, runs bundled on-device ML Kit
 * (Latin + Devanagari), builds word-level tokens in screen coordinates for interactive
 * iOS-style handle selection, and immediately recycles all bitmaps.
 */
object TierBMlKitOcr {

    suspend fun recognizeSelectionAtPoint(
        fullFrame: Bitmap,
        elementBounds: Rect?,
        tapX: Int = -1,
        tapY: Int = -1
    ): TierBSelectionResult? {
        val cropRect = resolveFocusedCropRect(fullFrame.width, fullFrame.height, elementBounds, tapX, tapY)
        val cropped = cropSafely(fullFrame, cropRect)
        if (cropped !== fullFrame && !fullFrame.isRecycled) {
            fullFrame.recycle()
        }
        val localTapX = if (tapX >= 0) (tapX - cropRect.left).coerceIn(0, cropped.width) else cropped.width / 2
        val localTapY = if (tapY >= 0) (tapY - cropRect.top).coerceIn(0, cropped.height) else cropped.height / 2

        return try {
            val inputImage = InputImage.fromBitmap(cropped, 0)
            val latinResult = runLatinRecognizer(inputImage)
            val latinMatch = latinResult?.let { selectPressedLine(it, cropRect, localTapX, localTapY, tapX, tapY) }
            if (latinMatch != null) {
                latinMatch
            } else {
                val devResult = runDevanagariRecognizer(inputImage)
                devResult?.let { selectPressedLine(it, cropRect, localTapX, localTapY, tapX, tapY) }
            }
        } finally {
            if (!cropped.isRecycled) {
                cropped.recycle()
            }
        }
    }

    suspend fun recognizeCroppedElement(
        fullFrame: Bitmap,
        elementBounds: Rect?,
        tapX: Int = -1,
        tapY: Int = -1
    ): String? = recognizeSelectionAtPoint(fullFrame, elementBounds, tapX, tapY)?.text

    private fun resolveFocusedCropRect(
        bitmapW: Int,
        bitmapH: Int,
        bounds: Rect?,
        tapX: Int,
        tapY: Int
    ): Rect {
        val w = bitmapW.coerceAtLeast(1)
        val h = bitmapH.coerceAtLeast(1)
        if (bounds != null && !bounds.isEmpty && bounds.width() <= 680 && bounds.height() <= 260) {
            return Rect(
                bounds.left.coerceIn(0, w - 1),
                bounds.top.coerceIn(0, h - 1),
                bounds.right.coerceIn((bounds.left + 1).coerceAtMost(w), w),
                bounds.bottom.coerceIn((bounds.top + 1).coerceAtMost(h), h)
            )
        }
        val cx = if (tapX >= 0) tapX.coerceIn(0, w - 1) else w / 2
        val cy = if (tapY >= 0) tapY.coerceIn(0, h - 1) else h / 2
        val halfW = 260
        val halfH = 100
        val left = (cx - halfW).coerceIn(0, (w - 2).coerceAtLeast(0))
        val top = (cy - halfH).coerceIn(0, (h - 2).coerceAtLeast(0))
        val right = (cx + halfW).coerceIn(left + 1, w)
        val bottom = (cy + halfH).coerceIn(top + 1, h)
        return Rect(left, top, right, bottom)
    }

    private fun selectPressedLine(
        visionText: Text,
        cropRect: Rect,
        localX: Int,
        localY: Int,
        rawX: Int,
        rawY: Int
    ): TierBSelectionResult? {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return null

        val hitLine = allLines.firstOrNull { line ->
            val box = line.boundingBox
            box != null && box.contains(localX, localY)
        } ?: allLines.minByOrNull { line ->
            val box = line.boundingBox ?: return@minByOrNull Double.MAX_VALUE
            val dx = when {
                localX < box.left -> (box.left - localX).toDouble()
                localX > box.right -> (localX - box.right).toDouble()
                else -> 0.0
            }
            val dy = when {
                localY < box.top -> (box.top - localY).toDouble()
                localY > box.bottom -> (localY - box.bottom).toDouble()
                else -> 0.0
            }
            hypot(dx, dy)
        } ?: return null

        val lineText = hitLine.text.trim().takeIf { it.isNotEmpty() } ?: return null
        val screenLineRect = hitLine.boundingBox?.let {
            Rect(cropRect.left + it.left, cropRect.top + it.top, cropRect.left + it.right, cropRect.top + it.bottom)
        } ?: cropRect

        val elements = hitLine.elements
        val selectionState = if (elements.isNotEmpty()) {
            var cursor = 0
            val tokens = mutableListOf<OcrTextToken>()
            for (el in elements) {
                val word = el.text.trim()
                if (word.isEmpty()) continue
                val startChar = lineText.indexOf(word, startIndex = cursor).takeIf { it >= 0 } ?: cursor
                val endChar = (startChar + word.length).coerceAtMost(lineText.length)
                cursor = endChar
                val eb = el.boundingBox
                val tokenRect = if (eb != null) {
                    Rect(cropRect.left + eb.left, cropRect.top + eb.top, cropRect.left + eb.right, cropRect.top + eb.bottom)
                } else {
                    screenLineRect
                }
                tokens.add(
                    OcrTextToken(
                        text = word,
                        startChar = startChar,
                        endChar = endChar,
                        leftPx = tokenRect.left,
                        topPx = tokenRect.top,
                        rightPx = tokenRect.right,
                        bottomPx = tokenRect.bottom
                    )
                )
            }
            val hitTokenIdx = tokens.indices.minByOrNull { idx ->
                val tok = tokens[idx]
                hypot(((tok.leftPx + tok.rightPx) / 2.0) - rawX, ((tok.topPx + tok.bottomPx) / 2.0) - rawY)
            } ?: 0
            OcrSelectionState(
                fullText = lineText,
                elementBounds = screenLineRect,
                tokens = tokens.ifEmpty {
                    TierAAccessibilityReader.buildSelectionState(lineText, screenLineRect, rawX, rawY, isFromTierB = true).tokens
                },
                startTokenIndex = hitTokenIdx,
                endTokenIndex = hitTokenIdx,
                anchorX = rawX.toFloat(),
                anchorY = rawY.toFloat(),
                isFromTierB = true
            )
        } else {
            TierAAccessibilityReader.buildSelectionState(
                text = lineText,
                bounds = screenLineRect,
                tapX = rawX,
                tapY = rawY,
                isFromTierB = true
            )
        }

        return TierBSelectionResult(lineText, selectionState)
    }

    private fun cropSafely(bitmap: Bitmap, bounds: Rect): Bitmap {
        val left = bounds.left.coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
        val top = bounds.top.coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
        val right = bounds.right.coerceIn(left + 1, bitmap.width)
        val bottom = bounds.bottom.coerceIn(top + 1, bitmap.height)
        val w = (right - left).coerceAtLeast(1)
        val h = (bottom - top).coerceAtLeast(1)
        return runCatching {
            Bitmap.createBitmap(bitmap, left, top, w, h)
        }.getOrDefault(bitmap)
    }

    private suspend fun runLatinRecognizer(image: InputImage): Text? =
        suspendCancellableCoroutine { cont ->
            val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            client.process(image)
                .addOnSuccessListener { result ->
                    client.close()
                    cont.resume(result)
                }
                .addOnFailureListener {
                    client.close()
                    cont.resume(null)
                }
        }

    private suspend fun runDevanagariRecognizer(image: InputImage): Text? =
        suspendCancellableCoroutine { cont ->
            val client = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            client.process(image)
                .addOnSuccessListener { result ->
                    client.close()
                    cont.resume(result)
                }
                .addOnFailureListener {
                    client.close()
                    cont.resume(null)
                }
        }
}
