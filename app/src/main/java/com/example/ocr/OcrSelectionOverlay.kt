package com.example.ocr

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.island.OcrSelectionState
import com.example.island.OcrTextToken
import com.example.ui.theme.IslandColors
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Flicker-free iOS + native Android Blue OCR Selection Overlay:
 * - Zero global StateFlow writes during handle drags: local draft state drives graphicsLayer & Canvas
 *   and commits back via onUpdateRange only when the resolved word boundary actually changes.
 * - pointerInput is keyed ONLY on tokens so gesture detection never restarts mid-drag.
 * - Screen-anchored finger tracking eliminates the moving-origin oscillation loop.
 * - Line-aware word-boundary snapping + 22x22 dp teardrop handles + clamped Magnifier Loupe.
 * - Single tap = select word / cycle word->sentence->all / dismiss outside; triple-tap = sentence.
 * - Copy executes ONLY when user explicitly taps Copy in the floating callout menu.
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
    val view = LocalView.current
    val tokens = selection.tokens
    if (tokens.isEmpty()) return

    var draftStartIdx by remember(tokens) {
        mutableIntStateOf(selection.startTokenIndex.coerceIn(0, tokens.lastIndex))
    }
    var draftEndIdx by remember(tokens) {
        mutableIntStateOf(selection.endTokenIndex.coerceIn(0, tokens.lastIndex))
    }
    var activeHandle by remember(tokens) { mutableIntStateOf(0) } // 0=none, 1=start, 2=end
    var dragFingerX by remember(tokens) { mutableFloatStateOf(selection.anchorX) }
    var dragFingerY by remember(tokens) { mutableFloatStateOf(selection.anchorY) }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    var tapStreak by remember { mutableIntStateOf(0) }
    var cycleTier by remember(tokens) { mutableIntStateOf(0) } // 0=word, 1=sentence, 2=all

    // Sync external range updates (e.g. Select All button) only when user is NOT actively dragging
    LaunchedEffect(tokens, selection.startTokenIndex, selection.endTokenIndex) {
        if (activeHandle == 0) {
            draftStartIdx = selection.startTokenIndex.coerceIn(0, tokens.lastIndex)
            draftEndIdx = selection.endTokenIndex.coerceIn(draftStartIdx, tokens.lastIndex)
        }
    }

    val startIdx = draftStartIdx.coerceIn(0, tokens.lastIndex)
    val endIdx = draftEndIdx.coerceIn(startIdx, tokens.lastIndex)
    val startTok = tokens[startIdx]
    val endTok = tokens[endIdx]

    val currentStartState by rememberUpdatedState(startIdx)
    val currentEndState by rememberUpdatedState(endIdx)
    val currentOnUpdateRange by rememberUpdatedState(onUpdateRange)
    val currentOnFinishDrag by rememberUpdatedState(onFinishDrag)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnHaptic by rememberUpdatedState(onHapticTick)

    val triggerHandleHaptic = remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
            currentOnHaptic()
        }
    }

    val selectedText = remember(tokens, selection.fullText, startIdx, endIdx) {
        val cStart = tokens[startIdx].startChar.coerceIn(0, selection.fullText.length)
        val cEnd = tokens[endIdx].endChar.coerceIn(cStart, selection.fullText.length)
        selection.fullText.substring(cStart, cEnd).trim().ifEmpty { selection.fullText }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
            .pointerInput(tokens) {
                val handleTouchPx = with(density) { 44.dp.toPx() }
                detectTapGestures(
                    onTap = { pos ->
                        val hitTokIdx = findNearestTokenIndexLineAware(
                            tokens = tokens,
                            x = pos.x,
                            y = pos.y,
                            maxDistancePx = handleTouchPx * 1.35f
                        )
                        if (hitTokIdx < 0 && !selection.elementBounds.contains(pos.x.toInt(), pos.y.toInt())) {
                            currentOnDismiss()
                            return@detectTapGestures
                        }
                        if (hitTokIdx >= 0) {
                            val now = System.currentTimeMillis()
                            tapStreak = if (now - lastTapTimeMs < 380L) (tapStreak + 1).coerceAtMost(3) else 1
                            lastTapTimeMs = now
                            triggerHandleHaptic()

                            val tappedInsideCurrent = hitTokIdx in currentStartState..currentEndState
                            when {
                                tapStreak >= 3 -> {
                                    // Triple-tap selects sentence around hit token
                                    val (sS, sE) = findSentenceRange(tokens, hitTokIdx)
                                    draftStartIdx = sS
                                    draftEndIdx = sE
                                    cycleTier = 1
                                    currentOnUpdateRange(sS, sE, 0, pos.x, pos.y)
                                }
                                tapStreak == 2 -> {
                                    // Double-tap snaps cleanly to the tapped word
                                    draftStartIdx = hitTokIdx
                                    draftEndIdx = hitTokIdx
                                    cycleTier = 0
                                    currentOnUpdateRange(hitTokIdx, hitTokIdx, 0, pos.x, pos.y)
                                }
                                tappedInsideCurrent -> {
                                    // Single tap inside selection cycles word -> sentence -> all
                                    val nextTier = (cycleTier + 1) % 3
                                    cycleTier = nextTier
                                    val (nS, nE) = when (nextTier) {
                                        1 -> findSentenceRange(tokens, hitTokIdx)
                                        2 -> 0 to tokens.lastIndex
                                        else -> hitTokIdx to hitTokIdx
                                    }
                                    draftStartIdx = nS
                                    draftEndIdx = nE
                                    currentOnUpdateRange(nS, nE, 0, pos.x, pos.y)
                                }
                                else -> {
                                    draftStartIdx = hitTokIdx
                                    draftEndIdx = hitTokIdx
                                    cycleTier = 0
                                    currentOnUpdateRange(hitTokIdx, hitTokIdx, 0, pos.x, pos.y)
                                }
                            }
                        }
                    }
                )
            }
            .testTag("ocr_selection_overlay")
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(360f)
        val screenHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(640f)
        val handleSizePx = with(density) { 22.dp.toPx() }
        val handleBoxSizePx = with(density) { 48.dp.toPx() }
        val loupeWidthPx = with(density) { 196.dp.toPx() }
        val loupeHeightPx = with(density) { 48.dp.toPx() }
        val menuWidthPx = with(density) { 280.dp.toPx() }
        val menuHeightPx = with(density) { 46.dp.toPx() }

        // Group contiguous selected tokens into per-line horizontal bands so highlights hug lines cleanly
        val selectedLineBands = remember(tokens, startIdx, endIdx) {
            buildLineBands(tokens, startIdx, endIdx)
        }

        // Line-following Android blue highlights + handle stems on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            selectedLineBands.forEach { band ->
                drawRoundRect(
                    color = IslandColors.SelectionBlue.copy(alpha = 0.40f),
                    topLeft = Offset(band.left.toFloat() - 3f, band.top.toFloat() - 2f),
                    size = Size(
                        width = (band.right - band.left).coerceAtLeast(16).toFloat() + 6f,
                        height = (band.bottom - band.top).coerceAtLeast(22).toFloat() + 4f
                    ),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
            // Start vertical edge bar
            drawLine(
                color = IslandColors.SelectionBlue,
                start = Offset(startTok.leftPx.toFloat(), startTok.topPx.toFloat() - 4f),
                end = Offset(startTok.leftPx.toFloat(), startTok.bottomPx.toFloat() + 4f),
                strokeWidth = 2.8.dp.toPx()
            )
            // End vertical edge bar
            drawLine(
                color = IslandColors.SelectionBlue,
                start = Offset(endTok.rightPx.toFloat(), endTok.topPx.toFloat() - 4f),
                end = Offset(endTok.rightPx.toFloat(), endTok.bottomPx.toFloat() + 4f),
                strokeWidth = 2.8.dp.toPx()
            )
        }

        // Start 22x22 dp Teardrop Handle (positioned strictly above-left, moved via graphicsLayer)
        val startHandleOriginX = (startTok.leftPx - handleBoxSizePx * 0.65f).coerceIn(0f, screenWidthPx - handleBoxSizePx)
        val startHandleOriginY = (startTok.topPx - handleBoxSizePx * 0.80f).coerceIn(0f, screenHeightPx - handleBoxSizePx)

        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    translationX = startHandleOriginX
                    translationY = startHandleOriginY
                }
                .pointerInput(tokens) {
                    var anchorStartX = 0f
                    var anchorStartY = 0f
                    detectDragGestures(
                        onDragStart = {
                            val s = currentStartState
                            val t = tokens[s]
                            anchorStartX = t.leftPx.toFloat()
                            anchorStartY = ((t.topPx + t.bottomPx) / 2f)
                            dragFingerX = anchorStartX
                            dragFingerY = anchorStartY
                            activeHandle = 1
                            triggerHandleHaptic()
                            currentOnUpdateRange(s, currentEndState, 1, anchorStartX, anchorStartY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            anchorStartX = (anchorStartX + dragAmount.x).coerceIn(0f, screenWidthPx)
                            anchorStartY = (anchorStartY + dragAmount.y).coerceIn(0f, screenHeightPx)
                            dragFingerX = anchorStartX
                            dragFingerY = anchorStartY

                            val nearest = findNearestTokenIndexLineAware(
                                tokens = tokens,
                                x = anchorStartX,
                                y = anchorStartY,
                                currentIdx = draftStartIdx
                            )
                            if (nearest >= 0 && nearest != draftStartIdx) {
                                val nextStart = nearest
                                val nextEnd = maxOf(nearest, draftEndIdx)
                                draftStartIdx = nextStart
                                draftEndIdx = nextEnd
                                triggerHandleHaptic()
                                currentOnUpdateRange(nextStart, nextEnd, 1, anchorStartX, anchorStartY)
                            }
                        },
                        onDragEnd = {
                            activeHandle = 0
                            currentOnUpdateRange(draftStartIdx, draftEndIdx, 0, dragFingerX, dragFingerY)
                            currentOnFinishDrag()
                        },
                        onDragCancel = {
                            activeHandle = 0
                            currentOnUpdateRange(draftStartIdx, draftEndIdx, 0, dragFingerX, dragFingerY)
                            currentOnFinishDrag()
                        }
                    )
                }
                .testTag("ocr_handle_start"),
            contentAlignment = Alignment.BottomEnd
        ) {
            TeardropHandle(pointBottomRight = true, sizePx = handleSizePx)
        }

        // End 22x22 dp Teardrop Handle (positioned strictly bottom-right, moved via graphicsLayer)
        val endHandleOriginX = (endTok.rightPx - handleBoxSizePx * 0.35f).coerceIn(0f, screenWidthPx - handleBoxSizePx)
        val endHandleOriginY = (endTok.bottomPx - handleBoxSizePx * 0.20f).coerceIn(0f, screenHeightPx - handleBoxSizePx)

        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    translationX = endHandleOriginX
                    translationY = endHandleOriginY
                }
                .pointerInput(tokens) {
                    var anchorEndX = 0f
                    var anchorEndY = 0f
                    detectDragGestures(
                        onDragStart = {
                            val e = currentEndState
                            val t = tokens[e]
                            anchorEndX = t.rightPx.toFloat()
                            anchorEndY = ((t.topPx + t.bottomPx) / 2f)
                            dragFingerX = anchorEndX
                            dragFingerY = anchorEndY
                            activeHandle = 2
                            triggerHandleHaptic()
                            currentOnUpdateRange(currentStartState, e, 2, anchorEndX, anchorEndY)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            anchorEndX = (anchorEndX + dragAmount.x).coerceIn(0f, screenWidthPx)
                            anchorEndY = (anchorEndY + dragAmount.y).coerceIn(0f, screenHeightPx)
                            dragFingerX = anchorEndX
                            dragFingerY = anchorEndY

                            val nearest = findNearestTokenIndexLineAware(
                                tokens = tokens,
                                x = anchorEndX,
                                y = anchorEndY,
                                currentIdx = draftEndIdx
                            )
                            if (nearest >= 0 && nearest != draftEndIdx) {
                                val nextEnd = nearest
                                val nextStart = minOf(draftStartIdx, nearest)
                                draftStartIdx = nextStart
                                draftEndIdx = nextEnd
                                triggerHandleHaptic()
                                currentOnUpdateRange(nextStart, nextEnd, 2, anchorEndX, anchorEndY)
                            }
                        },
                        onDragEnd = {
                            activeHandle = 0
                            currentOnUpdateRange(draftStartIdx, draftEndIdx, 0, dragFingerX, dragFingerY)
                            currentOnFinishDrag()
                        },
                        onDragCancel = {
                            activeHandle = 0
                            currentOnUpdateRange(draftStartIdx, draftEndIdx, 0, dragFingerX, dragFingerY)
                            currentOnFinishDrag()
                        }
                    )
                }
                .testTag("ocr_handle_end"),
            contentAlignment = Alignment.TopStart
        ) {
            TeardropHandle(pointBottomRight = false, sizePx = handleSizePx)
        }

        // Magnifier Loupe Pill strictly ABOVE drag finger, clamped to live display width
        AnimatedVisibility(
            visible = activeHandle != 0,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut(),
            modifier = Modifier.graphicsLayer {
                translationX = (dragFingerX - loupeWidthPx / 2f).coerceIn(16f, (screenWidthPx - loupeWidthPx - 16f).coerceAtLeast(16f))
                translationY = (dragFingerY - loupeHeightPx - with(density) { 68.dp.toPx() }).coerceAtLeast(with(density) { 72.dp.toPx() })
            }
        ) {
            val focusIdx = if (activeHandle == 1) startIdx else endIdx
            val snippet = buildLoupeSnippet(tokens, focusIdx)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(min = 140.dp, max = 216.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFA141720))
                    .border(1.5.dp, IslandColors.SelectionBlue, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 9.dp)
                    .testTag("ocr_magnifier_loupe")
            ) {
                Text(
                    text = snippet,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Floating Callout Menu ABOVE (or below if near status bar), COPY executes ONLY on tap
        val selectionCenterX = ((startTok.leftPx + endTok.rightPx) / 2f)
        val selectionTopY = tokens.subList(startIdx, endIdx + 1).minOf { it.topPx }.toFloat()
        val selectionBottomY = tokens.subList(startIdx, endIdx + 1).maxOf { it.bottomPx }.toFloat()
        val placeBelow = selectionTopY < with(density) { 140.dp.toPx() }
        val calloutY = if (placeBelow) {
            (selectionBottomY + with(density) { 30.dp.toPx() }).coerceAtMost(screenHeightPx - menuHeightPx - 32f)
        } else {
            (selectionTopY - menuHeightPx - with(density) { 26.dp.toPx() }).coerceAtLeast(with(density) { 64.dp.toPx() })
        }

        AnimatedVisibility(
            visible = activeHandle == 0,
            enter = fadeIn() + scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = 0.78f, stiffness = 750f)),
            exit = fadeOut(),
            modifier = Modifier.graphicsLayer {
                translationX = (selectionCenterX - menuWidthPx / 2f).coerceIn(16f, (screenWidthPx - menuWidthPx - 16f).coerceAtLeast(16f))
                translationY = calloutY
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF61A1C24))
                    .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("ocr_callout_menu")
            ) {
                CalloutActionChip(
                    label = "Copy",
                    icon = Icons.Filled.ContentCopy,
                    accent = IslandColors.SelectionBlue,
                    testTag = "ocr_callout_copy",
                    onClick = { onCopy(selectedText) }
                )
                CalloutActionChip(
                    label = "All",
                    icon = Icons.Filled.SelectAll,
                    accent = Color.White,
                    testTag = "ocr_callout_select_all",
                    onClick = {
                        draftStartIdx = 0
                        draftEndIdx = tokens.lastIndex
                        cycleTier = 2
                        onSelectAll()
                    }
                )
                CalloutActionChip(
                    label = "Share",
                    icon = Icons.Filled.Share,
                    accent = Color.White,
                    testTag = "ocr_callout_share",
                    onClick = { onShare(selectedText) }
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
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close OCR selection",
                        tint = IslandColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TeardropHandle(
    pointBottomRight: Boolean,
    sizePx: Float
) {
    val handleDp = with(LocalDensity.current) { sizePx.toDp() }
    Canvas(modifier = Modifier.size(handleDp)) {
        val w = size.width
        val h = size.height
        val r = w * 0.42f
        val path = Path().apply {
            if (pointBottomRight) {
                // Start teardrop: pointed stem top-right touching edge bar, round bulb bottom-left
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = 0f,
                        top = h - r * 2f,
                        right = r * 2f,
                        bottom = h
                    )
                )
                moveTo(w * 0.85f, 0f)
                lineTo(w * 0.85f, h * 0.52f)
                lineTo(r * 1.4f, h - r * 0.7f)
                close()
            } else {
                // End teardrop: pointed stem top-left touching edge bar, round bulb bottom-right
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = w - r * 2f,
                        top = h - r * 2f,
                        right = w,
                        bottom = h
                    )
                )
                moveTo(w * 0.15f, 0f)
                lineTo(w * 0.15f, h * 0.52f)
                lineTo(w - r * 1.4f, h - r * 0.7f)
                close()
            }
        }
        drawPath(path = path, color = IslandColors.SelectionBlue)
        drawCircle(
            color = Color.White.copy(alpha = 0.88f),
            radius = r * 0.34f,
            center = Offset(if (pointBottomRight) r else w - r, h - r)
        )
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

private data class SelectionLineBand(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

private fun buildLineBands(
    tokens: List<OcrTextToken>,
    startIdx: Int,
    endIdx: Int
): List<SelectionLineBand> {
    if (startIdx > endIdx || startIdx < 0 || endIdx >= tokens.size) return emptyList()
    val bands = mutableListOf<SelectionLineBand>()
    var cur = SelectionLineBand(tokens[startIdx].leftPx, tokens[startIdx].topPx, tokens[startIdx].rightPx, tokens[startIdx].bottomPx)
    for (i in (startIdx + 1)..endIdx) {
        val t = tokens[i]
        val curCenterY = (cur.top + cur.bottom) / 2f
        val tCenterY = (t.topPx + t.bottomPx) / 2f
        val lineTolerance = ((cur.bottom - cur.top).coerceAtLeast(24) * 0.55f)
        if (abs(tCenterY - curCenterY) <= lineTolerance && t.leftPx >= cur.left - 12) {
            cur = SelectionLineBand(
                left = minOf(cur.left, t.leftPx),
                top = minOf(cur.top, t.topPx),
                right = maxOf(cur.right, t.rightPx),
                bottom = maxOf(cur.bottom, t.bottomPx)
            )
        } else {
            bands.add(cur)
            cur = SelectionLineBand(t.leftPx, t.topPx, t.rightPx, t.bottomPx)
        }
    }
    bands.add(cur)
    return bands
}

private fun findSentenceRange(tokens: List<OcrTextToken>, idx: Int): Pair<Int, Int> {
    if (tokens.isEmpty()) return 0 to 0
    val safeIdx = idx.coerceIn(0, tokens.lastIndex)
    var s = safeIdx
    while (s > 0) {
        val prev = tokens[s - 1].text
        if (prev.endsWith('.') || prev.endsWith('!') || prev.endsWith('?') || prev.endsWith('\n')) break
        s--
    }
    var e = safeIdx
    while (e < tokens.lastIndex) {
        val cur = tokens[e].text
        if (cur.endsWith('.') || cur.endsWith('!') || cur.endsWith('?') || cur.endsWith('\n')) break
        e++
    }
    return s to e
}

/**
 * Line-aware nearest token lookup with slight hysteresis to prevent oscillation on drag boundaries.
 */
private fun findNearestTokenIndexLineAware(
    tokens: List<OcrTextToken>,
    x: Float,
    y: Float,
    maxDistancePx: Float = Float.MAX_VALUE,
    currentIdx: Int = -1
): Int {
    if (tokens.isEmpty()) return -1
    var bestIdx = -1
    var bestScore = Double.MAX_VALUE
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
        // Weight vertical line mismatches strongly so drag follows line rows cleanly
        var dist = hypot(dx, dy * 1.8)
        if (idx == currentIdx) {
            dist = (dist - 10.0).coerceAtLeast(0.0)
        }
        if (dist < bestScore) {
            bestScore = dist
            bestIdx = idx
        }
    }
    return if (bestScore <= maxDistancePx.toDouble()) bestIdx else -1
}

private fun buildLoupeSnippet(tokens: List<OcrTextToken>, focusIdx: Int): String {
    val from = (focusIdx - 1).coerceAtLeast(0)
    val to = (focusIdx + 1).coerceAtMost(tokens.lastIndex)
    return tokens.subList(from, to + 1).joinToString(" ") { it.text }
}
