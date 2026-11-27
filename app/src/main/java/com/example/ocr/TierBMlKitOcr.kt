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
 * Tier B OCR (§5 Path B): Frozen-frame ML Kit text recognition.
 * - Preserves the frozen screenshot in [OcrBitmapHolder] when requested so the frozen ImageView
 *   plus platform Magnifier & non-touchable highlight canvas align 1:1 with underlying text.
 * - Rejects oversize (>= 80% screen area) blocks and builds word-level tokens in bitmap/screen coords.
 */
object TierBMlKitOcr {

    suspend fun recognizeSelectionAtPoint(
        fullFrame: Bitmap,
        elementBounds: Rect?,
        tapX: Int = -1,
        tapY: Int = -1,
        preserveFrozenFrameForOverlay: Boolean = false
    ): TierBSelectionResult? {
        val frameW = fullFrame.width.coerceAtLeast(1)
        val frameH = fullFrame.height.coerceAtLeast(1)
        val cropRect = resolveFocusedCropRect(frameW, frameH, elementBounds, tapX, tapY)
        val cropped = cropSafely(fullFrame, cropRect)

        if (preserveFrozenFrameForOverlay) {
            OcrBitmapHolder.setFrozenFrame(fullFrame)
        } else if (cropped !== fullFrame && !fullFrame.isRecycled) {
            fullFrame.recycle()
        }

        val localTapX = if (tapX >= 0) (tapX - cropRect.left).coerceIn(0, cropped.width) else cropped.width / 2
        val localTapY = if (tapY >= 0) (tapY - cropRect.top).coerceIn(0, cropped.height) else cropped.height / 2

        return try {
            val inputImage = InputImage.fromBitmap(cropped, 0)
            val latinResult = runLatinRecognizer(inputImage)
            val devResult = runDevanagariRecognizer(inputImage)
            val mergedBlocks = mergeBlocks(
                latinResult?.textBlocks.orEmpty(),
                devResult?.textBlocks.orEmpty()
            )
            selectPressedBlock(mergedBlocks, cropRect, frameW, frameH, localTapX, localTapY, tapX, tapY)
        } finally {
            if (cropped !== fullFrame && !cropped.isRecycled) {
                cropped.recycle()
            } else if (!preserveFrozenFrameForOverlay && !cropped.isRecycled) {
                cropped.recycle()
            }
        }
    }

    private fun mergeBlocks(
        primary: List<Text.TextBlock>,
        secondary: List<Text.TextBlock>
    ): List<Text.TextBlock> {
        if (primary.isEmpty()) return secondary
        if (secondary.isEmpty()) return primary
        val merged = primary.toMutableList()
        for (cand in secondary) {
            val cBox = cand.boundingBox
            if (cBox == null) {
                merged.add(cand)
                continue
            }
            val overlapsExisting = merged.any { existing ->
                val eBox = existing.boundingBox ?: return@any false
                val inter = Rect(eBox)
                if (!inter.intersect(cBox)) return@any false
                val interArea = inter.width().toLong() * inter.height().toLong()
                val minArea = minOf(
                    eBox.width().toLong() * eBox.height().toLong(),
                    cBox.width().toLong() * cBox.height().toLong()
                ).coerceAtLeast(1L)
                interArea.toFloat() / minArea.toFloat() > 0.65f
            }
            if (!overlapsExisting) {
                merged.add(cand)
            }
        }
        return merged
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
        if (bounds != null && !bounds.isEmpty && bounds.width() <= 960 && bounds.height() <= 600 &&
            !SelectionCoordinateMapper.isOversizeSelection(bounds, w, h)
        ) {
            return Rect(
                bounds.left.coerceIn(0, w - 1),
                bounds.top.coerceIn(0, h - 1),
                bounds.right.coerceIn((bounds.left + 1).coerceAtMost(w), w),
                bounds.bottom.coerceIn((bounds.top + 1).coerceAtMost(h), h)
            )
        }
        val cx = if (tapX >= 0) tapX.coerceIn(0, w - 1) else w / 2
        val cy = if (tapY >= 0) tapY.coerceIn(0, h - 1) else h / 2
        val halfW = 460
        val halfH = 280
        val left = (cx - halfW).coerceIn(0, (w - 2).coerceAtLeast(0))
        val top = (cy - halfH).coerceIn(0, (h - 2).coerceAtLeast(0))
        val right = (cx + halfW).coerceIn(left + 1, w)
        val bottom = (cy + halfH).coerceIn(top + 1, h)
        return Rect(left, top, right, bottom)
    }

    private fun selectPressedBlock(
        rawBlocks: List<Text.TextBlock>,
        cropRect: Rect,
        screenW: Int,
        screenH: Int,
        localX: Int,
        localY: Int,
        rawX: Int,
        rawY: Int
    ): TierBSelectionResult? {
        val blocks = rawBlocks.filter { block ->
            val box = block.boundingBox ?: return@filter true
            val screenBox = Rect(
                cropRect.left + box.left,
                cropRect.top + box.top,
                cropRect.left + box.right,
                cropRect.top + box.bottom
            )
            !SelectionCoordinateMapper.isOversizeSelection(screenBox, screenW, screenH)
        }
        if (blocks.isEmpty()) return null

        val hitBlock = blocks.firstOrNull { block ->
            val box = block.boundingBox
            box != null && box.contains(localX, localY)
        } ?: blocks.minByOrNull { block ->
            val box = block.boundingBox ?: return@minByOrNull Double.MAX_VALUE
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

        val blockLines = hitBlock.lines
        val hitLine = blockLines.firstOrNull { line ->
            val box = line.boundingBox
            box != null && box.contains(localX, localY)
        } ?: blockLines.minByOrNull { line ->
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
        }

        val linesToUse = if (blockLines.size <= 4) {
            blockLines
        } else {
            val hitIdx = blockLines.indexOf(hitLine).coerceAtLeast(0)
            val from = (hitIdx - 1).coerceAtLeast(0)
            val to = (from + 4).coerceAtMost(blockLines.size)
            blockLines.subList(from, to)
        }
        if (linesToUse.isEmpty()) return null

        val fullBuilder = StringBuilder()
        val tokens = mutableListOf<OcrTextToken>()
        var minL = Int.MAX_VALUE
        var minT = Int.MAX_VALUE
        var maxR = Int.MIN_VALUE
        var maxB = Int.MIN_VALUE

        linesToUse.forEachIndexed { lineIdx, line ->
            val cleanLine = line.text.trim()
            if (cleanLine.isEmpty()) return@forEachIndexed
            if (lineIdx > 0 && fullBuilder.isNotEmpty()) {
                fullBuilder.append('\n')
            }
            val lineStartInFull = fullBuilder.length
            fullBuilder.append(cleanLine)

            line.boundingBox?.let { lb ->
                minL = minOf(minL, cropRect.left + lb.left)
                minT = minOf(minT, cropRect.top + lb.top)
                maxR = maxOf(maxR, cropRect.left + lb.right)
                maxB = maxOf(maxB, cropRect.top + lb.bottom)
            }

            var lineCursor = 0
            val elements = line.elements
            if (elements.isNotEmpty()) {
                for (el in elements) {
                    val word = el.text.trim()
                    if (word.isEmpty()) continue
                    val relStart = cleanLine.indexOf(word, startIndex = lineCursor).takeIf { it >= 0 } ?: lineCursor
                    val relEnd = (relStart + word.length).coerceAtMost(cleanLine.length)
                    lineCursor = relEnd
                    val eb = el.boundingBox
                    val screenRect = if (eb != null) {
                        Rect(cropRect.left + eb.left, cropRect.top + eb.top, cropRect.left + eb.right, cropRect.top + eb.bottom)
                    } else {
                        val lb = line.boundingBox
                        if (lb != null) {
                            Rect(cropRect.left + lb.left, cropRect.top + lb.top, cropRect.left + lb.right, cropRect.top + lb.bottom)
                        } else {
                            cropRect
                        }
                    }
                    tokens.add(
                        OcrTextToken(
                            text = word,
                            startChar = lineStartInFull + relStart,
                            endChar = lineStartInFull + relEnd,
                            leftPx = screenRect.left,
                            topPx = screenRect.top,
                            rightPx = screenRect.right,
                            bottomPx = screenRect.bottom
                        )
                    )
                }
            } else {
                val lb = line.boundingBox
                val screenRect = if (lb != null) {
                    Rect(cropRect.left + lb.left, cropRect.top + lb.top, cropRect.left + lb.right, cropRect.top + lb.bottom)
                } else cropRect
                tokens.add(
                    OcrTextToken(
                        text = cleanLine,
                        startChar = lineStartInFull,
                        endChar = lineStartInFull + cleanLine.length,
                        leftPx = screenRect.left,
                        topPx = screenRect.top,
                        rightPx = screenRect.right,
                        bottomPx = screenRect.bottom
                    )
                )
            }
        }

        val combinedText = fullBuilder.toString().trim().takeIf { it.isNotEmpty() } ?: return null
        val screenBlockRect = if (minL < maxR && minT < maxB) {
            Rect(minL, minT, maxR, maxB)
        } else {
            hitBlock.boundingBox?.let {
                Rect(cropRect.left + it.left, cropRect.top + it.top, cropRect.left + it.right, cropRect.top + it.bottom)
            } ?: cropRect
        }

        val finalTokens = if (tokens.isNotEmpty()) {
            tokens
        } else {
            TierAAccessibilityReader.buildSelectionState(
                text = combinedText,
                bounds = screenBlockRect,
                tapX = rawX,
                tapY = rawY,
                isFromTierB = true
            ).tokens
        }

        val hitTokenIdx = SelectionCoordinateMapper.findNearestTokenIndex(
            tokens = finalTokens,
            x = rawX.toFloat(),
            y = rawY.toFloat()
        ).coerceAtLeast(0)

        val selectionState = OcrSelectionState(
            fullText = combinedText,
            elementBounds = screenBlockRect,
            tokens = finalTokens,
            startTokenIndex = hitTokenIdx,
            endTokenIndex = hitTokenIdx,
            anchorX = rawX.toFloat(),
            anchorY = rawY.toFloat(),
            isFromTierB = true
        )
        return TierBSelectionResult(combinedText, selectionState)
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
