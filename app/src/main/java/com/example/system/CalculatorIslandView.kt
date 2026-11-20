package com.example.system

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslandColors
import kotlin.math.min

/**
 * Working iOS-style Calculator strictly matching Feature 5 specification:
 * - 4 x 5 grid, equal 12.dp gutter both ways, 16.dp side padding
 * - D = (W - 32.dp - 36.dp) / 4 (clamped to fit viewport cleanly if inside compact card)
 * - 0 button = 2D + gap wide, pill-shaped, left-aligned text
 * - Digit font size = 42% of D (Light 300), operators Semibold (600)
 * - Display font ~ 2.6x button font with auto-shrink ladder at 7 / 8 / 9 characters
 * - Colours: #000000 bg, #333333 digits, #A5A5A5 functions (black text), #FF9F0A operators,
 *   active operator inverted #D4D4D2 bg with #FF9F0A text; pressed brightness 130% / scale 0.95
 * - Real glyphs: ÷ × −
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
    onHaptic: () -> Unit = {},
    compactMode: Boolean = false
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(IslandColors.CalcBackground)
            .padding(horizontal = 16.dp, vertical = if (compactMode) 8.dp else 12.dp)
    ) {
        // Live viewport width W (BoxWithConstraints maxWidth is W - 32.dp already, so reconstruct W)
        val viewportW = maxWidth + 32.dp
        val gap = 12.dp
        val rawD = (viewportW - 32.dp - 36.dp) / 4f
        // In compactMode (inside floating island overlay), scale down cleanly if vertical budget is tight,
        // while keeping minimum touch target >= 48.dp; in full screen/expanded test it uses exact D = (W - 32 - 36) / 4.
        val d: Dp = if (compactMode) min(rawD.value, 54f).coerceAtLeast(48f).dp else rawD.coerceAtLeast(48.dp)

        val buttonFontSp = (d.value * 0.42f).sp
        val charCount = state.display.replace("-", "").replace(".", "").length
        val shrinkMultiplier = when {
            charCount >= 9 -> 0.68f
            charCount == 8 -> 0.78f
            charCount == 7 -> 0.88f
            else -> 1.0f
        }
        val displayFontSp = (buttonFontSp.value * 2.55f * shrinkMultiplier).sp

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            // Result Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = state.display,
                    color = IslandColors.CalcDigitText,
                    fontSize = displayFontSp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("calc_display")
                )
            }

            // Row 1: AC/C, ±, %, ÷
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
                    onClick = { onHaptic(); onClear() }
                )
                CalcKey(
                    label = "±",
                    bgColor = IslandColors.CalcFunctionBg,
                    fgColor = IslandColors.CalcFunctionText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_sign",
                    onClick = { onHaptic(); onToggleSign() }
                )
                CalcKey(
                    label = "%",
                    bgColor = IslandColors.CalcFunctionBg,
                    fgColor = IslandColors.CalcFunctionText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value * 0.9f,
                    fontWeight = FontWeight.Medium,
                    testTag = "calc_key_percent",
                    onClick = { onHaptic(); onPercent() }
                )
                OperatorKey(
                    op = CalcOperator.DIVIDE,
                    isActive = state.activeOperator == CalcOperator.DIVIDE,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onOperator(CalcOperator.DIVIDE) }
                )
            }

            // Row 2: 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                listOf("7", "8", "9").forEach { digit ->
                    DigitKey(digit, d, buttonFontSp.value) { onHaptic(); onDigit(digit) }
                }
                OperatorKey(
                    op = CalcOperator.MULTIPLY,
                    isActive = state.activeOperator == CalcOperator.MULTIPLY,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onOperator(CalcOperator.MULTIPLY) }
                )
            }

            // Row 3: 4, 5, 6, −
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                listOf("4", "5", "6").forEach { digit ->
                    DigitKey(digit, d, buttonFontSp.value) { onHaptic(); onDigit(digit) }
                }
                OperatorKey(
                    op = CalcOperator.SUBTRACT,
                    isActive = state.activeOperator == CalcOperator.SUBTRACT,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onOperator(CalcOperator.SUBTRACT) }
                )
            }

            // Row 4: 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
            ) {
                listOf("1", "2", "3").forEach { digit ->
                    DigitKey(digit, d, buttonFontSp.value) { onHaptic(); onDigit(digit) }
                }
                OperatorKey(
                    op = CalcOperator.ADD,
                    isActive = state.activeOperator == CalcOperator.ADD,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    onClick = { onHaptic(); onOperator(CalcOperator.ADD) }
                )
            }

            // Row 5: 0 (2D + gap wide, left-aligned), ., =
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
                    onClick = { onHaptic(); onDigit("0") }
                )
                CalcKey(
                    label = ".",
                    bgColor = IslandColors.CalcDigitBg,
                    fgColor = IslandColors.CalcDigitText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    fontWeight = FontWeight.Light,
                    testTag = "calc_key_dot",
                    onClick = { onHaptic(); onDecimal() }
                )
                CalcKey(
                    label = "=",
                    bgColor = IslandColors.CalcOperatorBg,
                    fgColor = IslandColors.CalcOperatorText,
                    diameter = d,
                    fontSizeSp = buttonFontSp.value,
                    fontWeight = FontWeight.SemiBold,
                    testTag = "calc_key_equals",
                    onClick = { onHaptic(); onEquals() }
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
        targetValue = if (pressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = 700f),
        label = "calc_scale"
    )
    // Brightness ~130% on press via lightened overlay
    val effectiveBg = if (pressed) {
        Color(
            red = (bgColor.red * 1.3f).coerceIn(0f, 1f),
            green = (bgColor.green * 1.3f).coerceIn(0f, 1f),
            blue = (bgColor.blue * 1.3f).coerceIn(0f, 1f),
            alpha = 1f
        )
    } else {
        bgColor
    }

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
