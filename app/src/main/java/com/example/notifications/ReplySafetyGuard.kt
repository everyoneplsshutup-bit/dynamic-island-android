package com.example.notifications

object ReplySafetyGuard {
    private val sensitivePattern = Regex(
        """\b(pay|transfer|send|upi|bank|otp|login|approve|confirm|password)\b""",
        RegexOption.IGNORE_CASE
    )

    fun isSensitiveContext(title: String, text: String): Boolean {
        val combined = "$title $text"
        return sensitivePattern.containsMatchIn(combined)
    }

    fun requiresDoubleConfirm(title: String, text: String, replyDraft: String = ""): Boolean {
        return isSensitiveContext(title, "$text $replyDraft")
    }
}
