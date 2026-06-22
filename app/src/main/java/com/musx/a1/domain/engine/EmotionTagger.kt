package com.musx.a1.domain.engine

object EmotionTagger {
    /**
     * Tags a sentence with an emotion based on punctuation and patterns.
     */
    fun tag(sentence: String): Emotion {
        val trimmed = sentence.trim()
        if (trimmed.isEmpty()) return Emotion.NEUTRAL

        return when {
            // Dialogue detection
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> Emotion.DIALOGUE
            trimmed.startsWith("'") && trimmed.endsWith("'") -> Emotion.DIALOGUE

            // Question detection
            trimmed.endsWith("?") -> Emotion.QUESTION

            // Exclamation detection
            trimmed.endsWith("!") -> Emotion.EXCLAMATION

            // Dramatic pause (ellipsis or short emphatic sentence)
            trimmed.endsWith("...") -> Emotion.DRAMATIC_PAUSE
            trimmed.length < 15 && !trimmed.contains(" ") -> Emotion.DRAMATIC_PAUSE

            // Whisper style (very long sentences or specific keywords)
            trimmed.length > 200 -> Emotion.WHISPER_STYLE

            // Narrative (default for most book text)
            else -> Emotion.NARRATIVE
        }
    }
}
