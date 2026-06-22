package com.musx.a1.domain.engine

object TextNormalizer {
    /**
     * Normalizes raw PDF text for better TTS quality.
     * Removes excessive noise, normalizes ligatures, and fixes common extraction errors.
     */
    fun normalize(text: String): String {
        var normalized = text

        // Replace common PDF ligatures
        normalized = normalized.replace("ﬁ", "fi")
            .replace("ﬂ", "fl")
            .replace("ﬀ", "ff")
            .replace("ﬃ", "ffi")
            .replace("ﬄ", "ffl")

        // Normalize quotes and dashes
        normalized = normalized.replace(Regex("[“”\"„]"), "\"")
            .replace(Regex("[‘’'‚]"), "'")
            .replace(Regex("[—–]"), " - ")

        // Fix hyphenated words at line breaks (e.g., "in- formatics" -> "informatics")
        normalized = normalized.replace(Regex("(\\w)-\\s+(\\w)"), "$1$2")

        // Remove excessive whitespace
        normalized = normalized.replace(Regex("\\s+"), " ")

        return normalized.trim()
    }
}
