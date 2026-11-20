package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Tier B OCR: Opt-in fallback only when Tier A finds no text.
 * Crops strictly to the pressed element bounds, runs bundled on-device ML Kit
 * (Latin + Devanagari), and immediately discards all bitmaps.
 */
object TierBMlKitOcr {

    suspend fun recognizeCroppedElement(fullFrame: Bitmap, elementBounds: Rect?): String? {
        val cropped = cropSafely(fullFrame, elementBounds)
        // Immediately discard the full frame if cropped is a separate bitmap
        if (cropped !== fullFrame && !fullFrame.isRecycled) {
            fullFrame.recycle()
        }
        return try {
            val inputImage = InputImage.fromBitmap(cropped, 0)
            val latinText = runLatinRecognizer(inputImage)
            if (!latinText.isNullOrBlank()) {
                latinText
            } else {
                runDevanagariRecognizer(inputImage)
            }
        } finally {
            if (!cropped.isRecycled) {
                cropped.recycle()
            }
        }
    }

    private fun cropSafely(bitmap: Bitmap, bounds: Rect?): Bitmap {
        if (bounds == null || bounds.isEmpty) return bitmap
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

    private suspend fun runLatinRecognizer(image: InputImage): String? =
        suspendCancellableCoroutine { cont ->
            val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            client.process(image)
                .addOnSuccessListener { result ->
                    client.close()
                    cont.resume(result.text.trim().takeIf { it.isNotEmpty() })
                }
                .addOnFailureListener {
                    client.close()
                    cont.resume(null)
                }
        }

    private suspend fun runDevanagariRecognizer(image: InputImage): String? =
        suspendCancellableCoroutine { cont ->
            val client = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            client.process(image)
                .addOnSuccessListener { result ->
                    client.close()
                    cont.resume(result.text.trim().takeIf { it.isNotEmpty() })
                }
                .addOnFailureListener {
                    client.close()
                    cont.resume(null)
                }
        }
}
