package com.example.system

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslandColors
import com.example.ui.theme.MotionTokens

/**
 * Working iOS-style Calculator strictly capped within 160 dp height inside the island card.
 * Features aligned 4 x 5 capsule/circular grid (no history row in full), non-clipping press tint,
 * M3E 0.97 press scale + radius squish, and unique Calc Key sound callback.
 */
@Composable
fun CalculatorIslandView(
    state: CalculatorState,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onClear: () -> Unit,
    onToggleSign: () -> Unit,
    onPercent: () -> Unit,
    onOperator: (CalcOperator) -> Unit,
    onEquals: () -> Unit,
    onBackspace: () -> Unit = {},
    onHaptic: () -> Unit = {},
    onKeySound: () -> Unit = {},
    compactMode: Boolean = true
) {
    if (compactMode) {
        CompactCappedCalculator(
            state = state,
            onDigit = onDigit,
            onDecimal = onDecimal,
            onClear = onClear,
            onToggleSign = onToggleSign,
            onPercent = onPercent,
            onOperator = onOperator,
            onEquals = onEquals,
            onBackspace = onBackspace,
            onHaptic = onHaptic,
            onKeySound = onKeySound
        )
    } else {
        FullCalculator(
            state = state,
            onDigit = onDigit,
            onDecimal = onDecimal,
            onClear = onClear,
            onToggleSign = onToggleSign,
            onPercent = onPercent,
            onOperator = onOperator,
            onEquals = onEquals,
            onBackspace = onBackspace,
            onHaptic = onHaptic,
            onKeySound = onKeySound
        )
    }
}

@Composable
private fun CompactCappedCalculator(
    state: CalculatorState,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onClear: () -> Unit,
    onToggleSign: () -> Unit,
    onPercent: () -> Unit,
    onOperator: (CalcOperator) -> Unit,
    onEquals: () -> Unit,
    onBackspace: () -> Unit,
    onHaptic: () -> Unit,
    onKeySound: () -> Unit
) {
    val charCount = state.display.replace("-", "").replace(".", "").length
    val shrinkMultiplier = when {
        charCount >= 9 -> 0.72f
        charCount == 8 -> 0.80f
        charCount == 7 -> 0.90f
        else -> 1.0f
    }
    val displayFontSp = (22f * shrinkMultiplier).sp
    val rowGap = 3.dp
    val keyHeight = 23.dp
    val keyFont = 13f
    var accumDrag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(IslandColors.CalcBackground)
            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(rowGap)
    ) {
        // Compact expression hint + swipe-to-backspace numeric display (24 dp — zero history row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { accumDrag = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            accumDrag += dragAmount
                            if (kotlin.math.abs(accumDrag) >= 28f) {
                                accumDrag = 0f
                                onHaptic()
                                onKeySound()
                                onBackspace()
                            }
                        }
                    )
                }
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.activeOperator?.let { "Op ${it.glyph}" } ?: "",
                color = IslandColors.AccentOrange.copy(alpha = 0.92f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = state.display,
                style = TextStyle(
                    color = IslandColors.CalcDigitText,
                    fontSize = displayFontSp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.testTag("calc_display")
            )
        }

        // Row 1: AC/C, ±, %, ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CompactCalcKey(
                label = if (state.canClearEntry && state.display != "0") "C" else "AC",
                bgColor = IslandColors.CalcFunctionBg,
                fgColor = IslandColors.CalcFunctionText,
                height = keyHeight,
                fontSizeSp = keyFont * 0.88f,
                fontWeight = FontWeight.SemiBold,
                testTag = "calc_key_clear",
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onClear() }

            CompactCalcKey(
                label = "±",
                bgColor = IslandColors.CalcFunctionBg,
                fgColor = IslandColors.CalcFunctionText,
                height = keyHeight,
                fontSizeSp = keyFont,
                fontWeight = FontWeight.SemiBold,
                testTag = "calc_key_sign",
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onToggleSign() }

            CompactCalcKey(
                label = "%",
                bgColor = IslandColors.CalcFunctionBg,
                fgColor = IslandColors.CalcFunctionText,
                height = keyHeight,
                fontSizeSp = keyFont * 0.92f,
                fontWeight = FontWeight.SemiBold,
                testTag = "calc_key_percent",
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onPercent() }

            CompactOperatorKey(
                op = CalcOperator.DIVIDE,
                isActive = state.activeOperator == CalcOperator.DIVIDE,
                height = keyHeight,
                fontSizeSp = keyFont + 1f,
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onOperator(CalcOperator.DIVIDE) }
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf("7", "8", "9").forEach { d ->
                CompactCalcKey(
                    label = d,
                    bgColor = IslandColors.CalcDigitBg,
                    fgColor = IslandColors.CalcDigitText,
                    height = keyHeight,
                    fontSizeSp = keyFont,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_$d",
                    modifier = Modifier.weight(1f)
                ) { onHaptic(); onKeySound(); onDigit(d) }
            }
            CompactOperatorKey(
                op = CalcOperator.MULTIPLY,
                isActive = state.activeOperator == CalcOperator.MULTIPLY,
                height = keyHeight,
                fontSizeSp = keyFont + 1f,
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onOperator(CalcOperator.MULTIPLY) }
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf("4", "5", "6").forEach { d ->
                CompactCalcKey(
                    label = d,
                    bgColor = IslandColors.CalcDigitBg,
                    fgColor = IslandColors.CalcDigitText,
                    height = keyHeight,
                    fontSizeSp = keyFont,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_$d",
                    modifier = Modifier.weight(1f)
                ) { onHaptic(); onKeySound(); onDigit(d) }
            }
            CompactOperatorKey(
                op = CalcOperator.SUBTRACT,
                isActive = state.activeOperator == CalcOperator.SUBTRACT,
                height = keyHeight,
                fontSizeSp = keyFont + 1f,
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onOperator(CalcOperator.SUBTRACT) }
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            listOf("1", "2", "3").forEach { d ->
                CompactCalcKey(
                    label = d,
                    bgColor = IslandColors.CalcDigitBg,
                    fgColor = IslandColors.CalcDigitText,
                    height = keyHeight,
                    fontSizeSp = keyFont,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_$d",
                    modifier = Modifier.weight(1f)
                ) { onHaptic(); onKeySound(); onDigit(d) }
            }
            CompactOperatorKey(
                op = CalcOperator.ADD,
                isActive = state.activeOperator == CalcOperator.ADD,
                height = keyHeight,
                fontSizeSp = keyFont + 1f,
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onOperator(CalcOperator.ADD) }
        }

        // Row 5: 0 (2x width, left-aligned), ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CompactCalcKey(
                label = "0",
                bgColor = IslandColors.CalcDigitBg,
                fgColor = IslandColors.CalcDigitText,
                height = keyHeight,
                fontSizeSp = keyFont,
                fontWeight = FontWeight.Medium,
                testTag = "calc_key_0",
                leftAlign = true,
                modifier = Modifier.weight(2.04f)
            ) { onHaptic(); onKeySound(); onDigit("0") }

            CompactCalcKey(
                label = ".",
                bgColor = IslandColors.CalcDigitBg,
                fgColor = IslandColors.CalcDigitText,
                height = keyHeight,
                fontSizeSp = keyFont + 1f,
                fontWeight = FontWeight.SemiBold,
                testTag = "calc_key_dot",
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onDecimal() }

            CompactCalcKey(
                label = "=",
                bgColor = IslandColors.CalcOperatorBg,
                fgColor = IslandColors.CalcOperatorText,
                height = keyHeight,
                fontSizeSp = keyFont + 1f,
                fontWeight = FontWeight.Bold,
                testTag = "calc_key_equals",
                modifier = Modifier.weight(1f)
            ) { onHaptic(); onKeySound(); onEquals() }
        }
    }
}

@Composable
private fun CompactOperatorKey(
    op: CalcOperator,
    isActive: Boolean,
    height: Dp,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CompactCalcKey(
        label = op.glyph,
        bgColor = if (isActive) IslandColors.CalcOperatorActiveBg else IslandColors.CalcOperatorBg,
        fgColor = if (isActive) IslandColors.CalcOperatorActiveText else IslandColors.CalcOperatorText,
        height = height,
        fontSizeSp = fontSizeSp,
        fontWeight = FontWeight.Bold,
        testTag = "calc_key_${op.name.lowercase()}",
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun CompactCalcKey(
    label: String,
    bgColor: Color,
    fgColor: Color,
    height: Dp,
    fontSizeSp: Float,
    fontWeight: FontWeight,
    testTag: String,
    modifier: Modifier = Modifier,
    leftAlign: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) MotionTokens.BUTTON_PRESS_SCALE else 1.0f,
        animationSpec = if (pressed) spring(dampingRatio = 1f, stiffness = 900f) else spring(dampingRatio = 0.72f, stiffness = 550f),
        label = "calc_key_scale"
    )
    val cornerDp by animateDpAsState(
        targetValue = if (pressed) 14.dp else 9.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 620f),
        label = "calc_key_squish"
    )
    val effectiveBg = IslandColors.pressTint(bgColor, pressed)
    val shape = RoundedCornerShape(cornerDp)

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(effectiveBg)
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = if (leftAlign) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = label,
            color = fgColor,
            fontSize = fontSizeSp.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            modifier = if (leftAlign) Modifier.padding(start = 18.dp) else Modifier
        )
    }
}

@Composable
private fun FullCalculator(
    state: CalculatorState,
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onClear: () -> Unit,
    onToggleSign: () -> Unit,
    onPercent: () -> Unit,
    onOperator: (CalcOperator) -> Unit,
    onEquals: () -> Unit,
    onBackspace: () -> Unit,
    onHaptic: () -> Unit,
    onKeySound: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IslandColors.CalcBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val viewportW = maxWidth + 32.dp
        val gap = 10.dp
        val d: Dp = ((viewportW - 32.dp - 30.dp) / 4f).coerceIn(48.dp, 64.dp)

        val buttonFontSp = (d.value * 0.42f).sp
        val charCount = state.display.replace("-", "").replace(".", "").length
        val shrinkMultiplier = when {
            charCount >= 9 -> 0.68f
            charCount == 8 -> 0.78f
            charCount == 7 -> 0.88f
            else -> 1.0f
        }
        val displayFontSp = (buttonFontSp.value * 2.4f * shrinkMultiplier).sp
        var accumDrag by remember { mutableFloatStateOf(0f) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { accumDrag = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                accumDrag += dragAmount
                                if (kotlin.math.abs(accumDrag) >= 28f) {
                                    accumDrag = 0f
                                    onHaptic()
                                    onKeySound()
                                    onBackspace()
                                }
                            }
                        )
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = state.display,
                    style = TextStyle(
                        color = IslandColors.CalcDigitText,
                        fontSize = displayFontSp,
                        fontWeight = FontWeight.Light,
                        fontFeatureSettings = "tnum"
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("calc_display")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                CalcKey(
                    label = if (state.canClearEntry && state.display != "0") "C" else "AC",
                    bgColor = IslandColors.CalcFunctionBg,
                    fgColor = IslandColors.CalcFunctionText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value * 0.86f,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_clear",
                    onClick = { onHaptic(); onKeySound(); onClear() }
                )
                CalcKey(
                    label = "±",
                    bgColor = IslandColors.CalcFunctionBg,
                    fgColor = IslandColors.CalcFunctionText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_sign",
                    onClick = { onHaptic(); onKeySound(); onToggleSign() }
                )
                CalcKey(
                    label = "%",
                    bgColor = IslandColors.CalcFunctionBg,
                    fgColor = IslandColors.CalcFunctionText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value * 0.9f,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_percent",
                    onClick = { onHaptic(); onKeySound(); onPercent() }
                )
                OperatorKey(
                    op = CalcOperator.DIVIDE,
                    isActive = state.activeOperator == CalcOperator.DIVIDE,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onKeySound(); onOperator(CalcOperator.DIVIDE) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                listOf("7", "8", "9").forEach { digit ->
                    DigitKey(digit, d, buttonFontSp.value) { onHaptic(); onKeySound(); onDigit(digit) }
                }
                OperatorKey(
                    op = CalcOperator.MULTIPLY,
                    isActive = state.activeOperator == CalcOperator.MULTIPLY,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onKeySound(); onOperator(CalcOperator.MULTIPLY) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                listOf("4", "5", "6").forEach { digit ->
                    DigitKey(digit, d, buttonFontSp.value) { onHaptic(); onKeySound(); onDigit(digit) }
                }
                OperatorKey(
                    op = CalcOperator.SUBTRACT,
                    isActive = state.activeOperator == CalcOperator.SUBTRACT,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onKeySound(); onOperator(CalcOperator.SUBTRACT) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                listOf("1", "2", "3").forEach { digit ->
                    DigitKey(digit, d, buttonFontSp.value) { onHaptic(); onKeySound(); onDigit(digit) }
                }
                OperatorKey(
                    op = CalcOperator.ADD,
                    isActive = state.activeOperator == CalcOperator.ADD,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onKeySound(); onOperator(CalcOperator.ADD) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                CalcKey(
                    label = "0",
                    bgColor = IslandColors.CalcDigitBg,
                    fgColor = IslandColors.CalcDigitText,
                    diameter = d,
                    widthOverride = d * 2f + gap,
                    leftAlign = true,
                    fontSizeSp = buttonFontSp.value,
                    fontWeight = FontWeight.Light,
                    testTag = "calc_key_0",
                    onClick = { onHaptic(); onKeySound(); onDigit("0") }
                )
                CalcKey(
                    label = ".",
                    bgColor = IslandColors.CalcDigitBg,
                    fgColor = IslandColors.CalcDigitText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    fontWeight = FontWeight.Light,
                    testTag = "calc_key_dot",
                    onClick = { onHaptic(); onKeySound(); onDecimal() }
                )
                CalcKey(
                    label = "=",
                    bgColor = IslandColors.CalcOperatorBg,
                    fgColor = IslandColors.CalcOperatorText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    fontWeight = FontWeight.SemiBold,
                    testTag = "calc_key_equals",
                    onClick = { onHaptic(); onKeySound(); onEquals() }
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun DigitKey(digit: String, diameter: Dp, fontSizeSp: Float, onClick: () -> Unit) {
    CalcKey(
        label = digit,
        bgColor = IslandColors.CalcDigitBg,
        fgColor = IslandColors.CalcDigitText,
        diameter = diameter,
        fontSizeSp = fontSizeSp,
        fontWeight = FontWeight.Light,
        testTag = "calc_key_$digit",
        onClick = onClick
    )
}

@Composable
private fun OperatorKey(
    op: CalcOperator,
    isActive: Boolean,
    diameter: Dp,
    fontSizeSp: Float,
    onClick: () -> Unit
) {
    CalcKey(
        label = op.glyph,
        bgColor = if (isActive) IslandColors.CalcOperatorActiveBg else IslandColors.CalcOperatorBg,
        fgColor = if (isActive) IslandColors.CalcOperatorActiveText else IslandColors.CalcOperatorText,
        diameter = diameter,
        fontSizeSp = fontSizeSp,
        fontWeight = FontWeight.SemiBold,
        testTag = "calc_key_${op.name.lowercase()}",
        onClick = onClick
    )
}

@Composable
private fun CalcKey(
    label: String,
    bgColor: Color,
    fgColor: Color,
    diameter: Dp,
    fontSizeSp: Float,
    fontWeight: FontWeight,
    testTag: String,
    widthOverride: Dp? = null,
    leftAlign: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) MotionTokens.BUTTON_PRESS_SCALE else 1.0f,
        animationSpec = spring(stiffness = 700f),
        label = "calc_scale"
    )
    val effectiveBg = IslandColors.pressTint(bgColor, pressed)

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(widthOverride ?: diameter)
            .height(diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(effectiveBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = if (leftAlign) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = label,
            color = fgColor,
            fontSize = fontSizeSp.sp,
            fontWeight = fontWeight,
            maxLines = 1,
            modifier = if (leftAlign) Modifier.padding(start = diameter * 0.36f) else Modifier
        )
    }
}
