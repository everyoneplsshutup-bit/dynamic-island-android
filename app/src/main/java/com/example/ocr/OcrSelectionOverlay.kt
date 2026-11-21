package com.example.ocr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.island.OcrSelectionState
import com.example.island.OcrTextToken
import com.example.ui.theme.IslandColors
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Custom iOS-style interactive OCR Selection Overlay:
 * - Blue highlight band across selected tokens + teardrop drag handles at start & end
 * - Live Magnifier Loupe pill centered above the active drag finger
 * - Floating dark Callout Menu (Copy | Select All | Share) — NEVER auto-copies until user taps Copy
 * - Double-tap word / triple-tap sentence / tap outside to dismiss
 */
@Composable
fun OcrSelectionOverlay(
    selection: OcrSelectionState,
    onUpdateRange: (startIdx: Int, endIdx: Int, activeHandle: Int, anchorX: Float, anchorY: Float) -> Unit,
    onFinishDrag: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
    onHapticTick: () -> Unit
) {
    val density = LocalDensity.current
    val tokens = selection.tokens
    if (tokens.isEmpty()) return

    val startIdx = selection.startTokenIndex.coerceIn(0, tokens.lastIndex)
    val endIdx = selection.endTokenIndex.coerceIn(startIdx, tokens.lastIndex)
    val startTok = tokens[startIdx]
    val endTok = tokens[endIdx]

    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    var tapStreak by remember { mutableStateOf(0) }

    val menuWidthPx = with(density) { 276.dp.toPx() }
    val menuHeightPx = with(density) { 46.dp.toPx() }
    val handleSizeDp = 28.dp
    val handleTouchPx = with(density) { 44.dp.toPx() }

    val selectionCenterX = ((startTok.leftPx + endTok.rightPx) / 2f)
    val selectionTopY = tokens.subList(startIdx, endIdx + 1).minOf { it.topPx }.toFloat()
    val selectionBottomY = tokens.subList(startIdx, endIdx + 1).maxOf { it.bottomPx }.toFloat()

    // Position Callout cleanly above selection (or below if near top status bar)
    val placeBelow = selectionTopY < with(density) { 130.dp.toPx() }
    val calloutY = if (placeBelow) {
        selectionBottomY + with(density) { 26.dp.toPx() }
    } else {
        (selectionTopY - menuHeightPx - with(density) { 18.dp.toPx() }).coerceAtLeast(with(density) { 64.dp.toPx() })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
            .pointerInput(selection.fullText, startIdx, endIdx) {
                detectTapGestures(
                    onTap = { pos ->
                        val hitTokIdx = findNearestTokenIndex(tokens, pos.x, pos.y, maxDistancePx = handleTouchPx * 1.4f)
                        if (hitTokIdx < 0 && !selection.elementBounds.contains(pos.x.toInt(), pos.y.toInt())) {
                            onDismiss()
                        } else if (hitTokIdx >= 0) {
                            val now = System.currentTimeMillis()
                            tapStreak = if (now - lastTapTimeMs < 420L) (tapStreak + 1).coerceAtMost(3) else 1
                            lastTapTimeMs = now
                            onHapticTick()
                            if (tapStreak >= 3) {
                                onSelectAll()
                            } else {
                                onUpdateRange(hitTokIdx, hitTokIdx, 0, pos.x, pos.y)
                            }
                        }
                    },
                    onDoubleTap = { pos ->
                        val hitTokIdx = findNearestTokenIndex(tokens, pos.x, pos.y, maxDistancePx = handleTouchPx * 1.5f)
                        if (hitTokIdx >= 0) {
                            onHapticTick()
                            onUpdateRange(hitTokIdx, hitTokIdx, 0, pos.x, pos.y)
                        }
                    }
                )
            }
            .testTag("ocr_selection_overlay")
    ) {
        // Highlight boxes + handle stems on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in startIdx..endIdx) {
                val tok = tokens[i]
                drawRoundRect(
                    color = IslandColors.SelectionBlue.copy(alpha = 0.36f),
                    topLeft = Offset(tok.leftPx.toFloat() - 3f, tok.topPx.toFloat() - 2f),
                    size = Size(
                        width = (tok.rightPx - tok.leftPx).coerceAtLeast(16).toFloat() + 6f,
                        height = (tok.bottomPx - tok.topPx).coerceAtLeast(22).toFloat() + 4f
                    ),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
            // Start vertical bar
            drawLine(
                color = IslandColors.SelectionBlue,
                start = Offset(startTok.leftPx.toFloat(), startTok.topPx.toFloat() - 4f),
                end = Offset(startTok.leftPx.toFloat(), startTok.bottomPx.toFloat() + 6f),
                strokeWidth = 3.dp.toPx()
            )
            // End vertical bar
            drawLine(
                color = IslandColors.SelectionBlue,
                start = Offset(endTok.rightPx.toFloat(), endTok.topPx.toFloat() - 4f),
                end = Offset(endTok.rightPx.toFloat(), endTok.bottomPx.toFloat() + 6f),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Start Drag Handle (Top-left lollipop above start edge)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (startTok.leftPx - with(density) { (handleSizeDp / 2).roundToPx() }).coerceAtLeast(0),
                        y = (startTok.topPx - with(density) { handleSizeDp.roundToPx() }).coerceAtLeast(0)
                    )
                }
                .size(handleSizeDp + 16.dp)
                .pointerInput(tokens) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onHapticTick()
                            onUpdateRange(startIdx, endIdx, 1, startTok.leftPx.toFloat(), startTok.topPx.toFloat())
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val fingerX = change.position.x + startTok.leftPx - with(density) { (handleSizeDp / 2).toPx() }
                            val fingerY = change.position.y + startTok.topPx - with(density) { handleSizeDp.toPx() }
                            val nearest = findNearestTokenIndex(tokens, fingerX, fingerY)
                            if (nearest >= 0) {
                                if (nearest != startIdx) onHapticTick()
                                onUpdateRange(nearest, maxOf(nearest, endIdx), 1, fingerX, fingerY)
                            }
                        },
                        onDragEnd = { onFinishDrag() },
                        onDragCancel = { onFinishDrag() }
                    )
                }
                .testTag("ocr_handle_start"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(IslandColors.SelectionBlue)
                    .border(2.5.dp, Color.White, CircleShape)
            )
        }

        // End Drag Handle (Bottom-right lollipop)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (endTok.rightPx - with(density) { (handleSizeDp / 2).roundToPx() }).coerceAtLeast(0),
                        y = endTok.bottomPx.coerceAtLeast(0)
                    )
                }
                .size(handleSizeDp + 16.dp)
                .pointerInput(tokens) {
                    detectDragGestures(
                        onDragStart = {
                            onHapticTick()
                            onUpdateRange(startIdx, endIdx, 2, endTok.rightPx.toFloat(), endTok.bottomPx.toFloat())
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val fingerX = change.position.x + endTok.rightPx - with(density) { (handleSizeDp / 2).toPx() }
                            val fingerY = change.position.y + endTok.bottomPx
                            val nearest = findNearestTokenIndex(tokens, fingerX, fingerY)
                            if (nearest >= 0) {
                                if (nearest != endIdx) onHapticTick()
                                onUpdateRange(minOf(startIdx, nearest), nearest, 2, fingerX, fingerY)
                            }
                        },
                        onDragEnd = { onFinishDrag() },
                        onDragCancel = { onFinishDrag() }
                    )
                }
                .testTag("ocr_handle_end"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(IslandColors.SelectionBlue)
                    .border(2.5.dp, Color.White, CircleShape)
            )
        }

        // Magnifier Loupe Pill strictly ABOVE active drag finger
        AnimatedVisibility(
            visible = selection.activeDragHandle != 0,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut(),
            modifier = Modifier
                .offset {
                    val loupeW = with(density) { 184.dp.roundToPx() }
                    val loupeH = with(density) { 46.dp.roundToPx() }
                    IntOffset(
                        x = (selection.anchorX.roundToInt() - loupeW / 2).coerceIn(16, 860),
                        y = (selection.anchorY.roundToInt() - loupeH - with(density) { 64.dp.roundToPx() }).coerceAtLeast(80)
                    )
                }
        ) {
            val focusIdx = if (selection.activeDragHandle == 1) startIdx else endIdx
            val snippet = buildLoupeSnippet(tokens, focusIdx)
            Box(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xF614161D))
                    .border(1.5.dp, IslandColors.SelectionBlue, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("ocr_magnifier_loupe"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = snippet,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // iOS-style Dark Callout Menu (Copy | Select All | Share | Close) — Copy ONLY when tapped!
        if (selection.activeDragHandle == 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (selectionCenterX - menuWidthPx / 2f).roundToInt().coerceIn(24, 780),
                            y = calloutY.roundToInt().coerceAtLeast(88)
                        )
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF51A1C24))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("ocr_callout_menu")
            ) {
                CalloutActionChip(
                    label = "Copy",
                    icon = Icons.Filled.ContentCopy,
                    accent = IslandColors.SelectionBlue,
                    testTag = "ocr_callout_copy",
                    onClick = { onCopy(selection.selectedText) }
                )
                CalloutActionChip(
                    label = "All",
                    icon = Icons.Filled.SelectAll,
                    accent = Color.White,
                    testTag = "ocr_callout_select_all",
                    onClick = onSelectAll
                )
                CalloutActionChip(
                    label = "Share",
                    icon = Icons.Filled.Share,
                    accent = Color.White,
                    testTag = "ocr_callout_share",
                    onClick = { onShare(selection.selectedText) }
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .testTag("ocr_callout_dismiss"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close OCR selection", tint = IslandColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CalloutActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun findNearestTokenIndex(
    tokens: List<OcrTextToken>,
    x: Float,
    y: Float,
    maxDistancePx: Float = Float.MAX_VALUE
): Int {
    if (tokens.isEmpty()) return -1
    var bestIdx = -1
    var bestDist = Double.MAX_VALUE
    tokens.forEachIndexed { idx, tok ->
        val dx = when {
            x < tok.leftPx -> (tok.leftPx - x).toDouble()
            x > tok.rightPx -> (x - tok.rightPx).toDouble()
            else -> 0.0
        }
        val dy = when {
            y < tok.topPx -> (tok.topPx - y).toDouble()
            y > tok.bottomPx -> (y - tok.bottomPx).toDouble()
            else -> 0.0
        }
        val d = hypot(dx, dy)
        if (d < bestDist) {
            bestDist = d
            bestIdx = idx
        }
    }
    return if (bestDist <= maxDistancePx.toDouble()) bestIdx else -1
}

private fun buildLoupeSnippet(tokens: List<OcrTextToken>, focusIdx: Int): String {
    val from = (focusIdx - 1).coerceAtLeast(0)
    val to = (focusIdx + 1).coerceAtMost(tokens.lastIndex)
    return tokens.subList(from, to + 1).joinToString(" ") { it.text }
}
