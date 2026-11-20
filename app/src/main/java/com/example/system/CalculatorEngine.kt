package com.example.system

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CalcOperator(val glyph: String) {
    DIVIDE("÷"),
    MULTIPLY("×"),
    SUBTRACT("−"),
    ADD("+")
}

data class CalculatorState(
    val display: String = "0",
    val activeOperator: CalcOperator? = null,
    val canClearEntry: Boolean = false
)

class CalculatorEngine {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private var accumulator: BigDecimal? = null
    private var pendingOp: CalcOperator? = null
    private var startNewEntry: Boolean = true
    private var lastOperand: BigDecimal? = null
    private var lastRepeatedOp: CalcOperator? = null

    fun pressDigit(digit: String) {
        val current = _state.value.display
        val next = if (startNewEntry || current == "0" || current == "Error") {
            startNewEntry = false
            if (digit == ".") "0." else digit
        } else {
            if (digit == "." && current.contains(".")) return
            if (current.replace("-", "").replace(".", "").length >= 9) return
            current + digit
        }
        _state.value = _state.value.copy(
            display = next,
            activeOperator = null,
            canClearEntry = true
        )
    }

    fun pressDecimal() = pressDigit(".")

    fun pressClear() {
        if (_state.value.canClearEntry && _state.value.display != "0") {
            startNewEntry = true
            _state.value = _state.value.copy(
                display = "0",
                canClearEntry = false,
                activeOperator = pendingOp
            )
        } else {
            accumulator = null
            pendingOp = null
            lastOperand = null
            lastRepeatedOp = null
            startNewEntry = true
            _state.value = CalculatorState()
        }
    }

    fun pressToggleSign() {
        val cur = _state.value.display
        if (cur == "0" || cur == "Error") return
        val toggled = if (cur.startsWith("-")) cur.drop(1) else "-$cur"
        _state.value = _state.value.copy(display = toggled)
    }

    fun pressPercent() {
        val value = parseDisplay() ?: return
        val base = accumulator
        val result = if (base != null && (pendingOp == CalcOperator.ADD || pendingOp == CalcOperator.SUBTRACT)) {
            base.multiply(value).divide(BigDecimal(100), MathContext.DECIMAL64)
        } else {
            value.divide(BigDecimal(100), MathContext.DECIMAL64)
        }
        startNewEntry = false
        _state.value = _state.value.copy(display = formatNumber(result))
    }

    fun pressOperator(op: CalcOperator) {
        val currentVal = parseDisplay() ?: BigDecimal.ZERO
        if (accumulator == null) {
            accumulator = currentVal
        } else if (!startNewEntry && pendingOp != null) {
            val computed = compute(accumulator!!, currentVal, pendingOp!!)
            if (computed == null) {
                showError()
                return
            }
            accumulator = computed
            _state.value = _state.value.copy(display = formatNumber(computed))
        }
        pendingOp = op
        lastOperand = null
        lastRepeatedOp = null
        startNewEntry = true
        _state.value = _state.value.copy(activeOperator = op)
    }

    fun pressEquals() {
        val currentVal = parseDisplay() ?: return
        val op = pendingOp ?: lastRepeatedOp ?: return
        val rhs = if (pendingOp != null) {
            lastOperand = currentVal
            lastRepeatedOp = pendingOp
            currentVal
        } else {
            lastOperand ?: currentVal
        }
        val lhs = accumulator ?: BigDecimal.ZERO
        val result = compute(lhs, rhs, op)
        if (result == null) {
            showError()
            return
        }
        accumulator = result
        pendingOp = null
        startNewEntry = true
        _state.value = _state.value.copy(
            display = formatNumber(result),
            activeOperator = null
        )
    }

    private fun showError() {
        accumulator = null
        pendingOp = null
        startNewEntry = true
        _state.value = CalculatorState(display = "Error", canClearEntry = false)
    }

    private fun parseDisplay(): BigDecimal? =
        _state.value.display.takeIf { it != "Error" }?.toBigDecimalOrNull()

    private fun compute(a: BigDecimal, b: BigDecimal, op: CalcOperator): BigDecimal? {
        return try {
            when (op) {
                CalcOperator.ADD -> a.add(b, MathContext.DECIMAL64)
                CalcOperator.SUBTRACT -> a.subtract(b, MathContext.DECIMAL64)
                CalcOperator.MULTIPLY -> a.multiply(b, MathContext.DECIMAL64)
                CalcOperator.DIVIDE -> {
                    if (b.compareTo(BigDecimal.ZERO) == 0) null
                    else a.divide(b, 10, RoundingMode.HALF_UP)
                }
            }
        } catch (_: ArithmeticException) {
            null
        }
    }

    companion object {
        fun formatNumber(num: BigDecimal): String {
            val stripped = num.stripTrailingZeros()
            val plain = stripped.toPlainString()
            return if (plain.length > 11) {
                stripped.round(MathContext(7, RoundingMode.HALF_UP)).toString()
            } else {
                plain
            }
        }

        /**
         * Inline arithmetic evaluator for the Island Search Launcher.
         * Supports +, -, *, /, ×, ÷, − and parentheses.
         */
        fun evaluateInlineExpression(raw: String): String? {
            val cleaned = raw.trim()
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace(" ", "")
            if (cleaned.isEmpty() || !cleaned.any { it in "+-*/" }) return null
            if (!cleaned.all { it.isDigit() || it in "+-*/()." }) return null

            return runCatching {
                var pos = 0
                fun peek(): Char? = cleaned.getOrNull(pos)
                fun parseNumber(): Double {
                    val start = pos
                    if (peek() == '-' || peek() == '+') pos++
                    while (peek()?.let { it.isDigit() || it == '.' } == true) pos++
                    return cleaned.substring(start, pos).toDouble()
                }
                lateinit var parseExpr: () -> Double
                fun parseFactor(): Double {
                    return if (peek() == '(') {
                        pos++
                        val v = parseExpr()
                        if (peek() == ')') pos++
                        v
                    } else {
                        parseNumber()
                    }
                }
                fun parseTerm(): Double {
                    var v = parseFactor()
                    while (peek() == '*' || peek() == '/') {
                        val op = cleaned[pos++]
                        val rhs = parseFactor()
                        v = if (op == '*') v * rhs else {
                            if (rhs == 0.0) throw ArithmeticException("Division by zero")
                            v / rhs
                        }
                    }
                    return v
                }
                parseExpr = {
                    var v = parseTerm()
                    while (peek() == '+' || peek() == '-') {
                        val op = cleaned[pos++]
                        val rhs = parseTerm()
                        v = if (op == '+') v + rhs else v - rhs
                    }
                    v
                }
                val result = parseExpr()
                if (pos != cleaned.length || result.isNaN() || result.isInfinite()) return null
                formatNumber(BigDecimal.valueOf(result))
            }.getOrNull()
        }
    }
}
