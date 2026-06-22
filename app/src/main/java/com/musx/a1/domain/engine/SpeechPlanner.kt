package com.musx.a1.domain.engine

import kotlin.random.Random

object SpeechPlanner {
    /**
     * Converts a sentence and its emotion into a SpeechInstruction with pacing and rhythm.
     */
    fun plan(sentence: String, emotion: Emotion): SpeechInstruction {
        // Base randomness for micro-pauses
        val microPause = Random.nextLong(100, 300)
        val speedVariation = Random.nextFloat() * 0.04f - 0.02f // -0.02 to +0.02

        return when (emotion) {
            Emotion.DIALOGUE -> SpeechInstruction(
                sentence = sentence,
                speed = 1.05f + speedVariation,
                pitch = 1.1f,
                pauseBeforeMs = 400 + microPause,
                pauseAfterMs = 400 + microPause
            )
            Emotion.QUESTION -> SpeechInstruction(
                sentence = sentence,
                speed = 0.95f + speedVariation,
                pitch = 1.05f,
                pauseAfterMs = 600 + microPause
            )
            Emotion.EXCLAMATION -> SpeechInstruction(
                sentence = sentence,
                speed = 1.1f + speedVariation,
                pitch = 1.1f,
                pauseAfterMs = 500 + microPause,
                emphasis = true
            )
            Emotion.DRAMATIC_PAUSE -> SpeechInstruction(
                sentence = sentence,
                speed = 0.85f + speedVariation,
                pauseAfterMs = 1200 + microPause
            )
            Emotion.WHISPER_STYLE -> SpeechInstruction(
                sentence = sentence,
                speed = 0.9f + speedVariation,
                pitch = 0.9f,
                pauseBeforeMs = 300 + microPause
            )
            Emotion.NARRATIVE -> SpeechInstruction(
                sentence = sentence,
                speed = 1.0f + speedVariation,
                pauseAfterMs = 300 + microPause
            )
            else -> SpeechInstruction(
                sentence = sentence,
                speed = 1.0f,
                pauseAfterMs = 200
            )
        }
    }
}
