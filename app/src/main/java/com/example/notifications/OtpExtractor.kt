package com.example.notifications

/**
 * Extracts 4–8 digit verification codes (OTP / PIN / 2FA) from incoming notification text
 * while rejecting phone numbers, dates, timestamps, and currency amounts.
 */
object OtpExtractor {

    private val keywordRegex = Regex(
        """\b(otp|code|pin|passcode|verification|verify|confirm|2fa|auth|login|security)\b""",
        RegexOption.IGNORE_CASE
    )
    private val codeAfterKeywordRegex = Regex(
        """(?:otp|code|pin|passcode|verification(?:\s+code)?|security\s+code|2fa)\s*(?:is|:|=|-|\s)\s*(\d{4,8})\b""",
        RegexOption.IGNORE_CASE
    )
    private val standaloneDigitsRegex = Regex("""(?<![+\d.,:/-])(\d{4,8})(?![+\d.,:/-])""")
    private val yearRegex = Regex("""^(19\d{2}|20[0-3]\d)$""")

    fun extract(title: String, text: String): String? {
        val combined = "$title\n$text"
        if (!keywordRegex.containsMatchIn(combined)) return null

        val explicitMatch = codeAfterKeywordRegex.find(combined)?.groupValues?.getOrNull(1)
        if (explicitMatch != null && !yearRegex.matches(explicitMatch)) {
            return explicitMatch
        }

        val candidates = standaloneDigitsRegex.findAll(combined)
            .map { it.groupValues[1] }
            .filterNot { yearRegex.matches(it) }
            .toList()

        return candidates.firstOrNull { it.length in 4..8 }
    }
}
