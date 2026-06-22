package com.musx.a1.engine

object TextCleaner {
    /**
     * Cleans text from PDF pages by removing common noise like headers, footers,
     * and page numbers, and normalizing whitespace.
     */
    fun clean(text: String): String {
        var cleaned = text

        // Remove typical page numbers (e.g., "Page 1 of 10", "- 5 -", "123")
        cleaned = cleaned.replace(Regex("""(?m)^\s*Page\s+\d+\s+of\s+\d+\s*$"""), "")
        cleaned = cleaned.replace(Regex("""(?m)^\s*-\s*\d+\s*-\s*$"""), "")
        cleaned = cleaned.replace(Regex("""(?m)^\s*\d+\s*$"""), "")

        // Normalize line breaks: replace single line breaks with space if they don't seem to end a sentence
        // This helps fix broken lines in PDF text extraction.
        cleaned = cleaned.replace(Regex("""(?<=[^\.\!\?\:])\n(?=[a-z])"""), " ")

        // Remove multiple spaces and normalize spacing
        cleaned = cleaned.replace(Regex("""\s+"""), " ")

        return cleaned.trim()
    }
}
