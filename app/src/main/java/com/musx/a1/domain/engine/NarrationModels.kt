package com.musx.a1.domain.engine

enum class Emotion {
    NEUTRAL,
    NARRATIVE,
    DIALOGUE,
    QUESTION,
    EXCLAMATION,
    DRAMATIC_PAUSE,
    WHISPER_STYLE
}

data class SpeechInstruction(
    val sentence: String,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val pauseBeforeMs: Long = 0,
    val pauseAfterMs: Long = 0,
    val emphasis: Boolean = false,
    val emotion: Emotion = Emotion.NEUTRAL
)
