package com.musx.a1.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.musx.a1.domain.engine.SpeechInstruction
import java.util.Locale

class TtsManager(context: Context, private val onSentenceFinished: () -> Unit) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val handler = Handler(Looper.getMainLooper())

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
                pendingInstruction?.let { speak(it) }
                pendingInstruction = null

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        onSentenceFinished()
                    }
                    override fun onError(utteranceId: String?) {}
                })
            }
        }
    }

    private var pendingInstruction: SpeechInstruction? = null

    /**
     * Speaks a instruction with specific timing and voice parameters.
     */
    fun speak(instruction: SpeechInstruction) {
        if (!isInitialized) {
            pendingInstruction = instruction
            return
        }

        handler.postDelayed({
            tts?.setSpeechRate(instruction.speed)
            tts?.setPitch(instruction.pitch)

            // Adding a small delay for pauseAfterMs via UtteranceProgressListener
            // is complex, so we'll simulate the "after pause" by delaying the next call
            // in the PlaybackService or by adding silence.

            tts?.speak(instruction.sentence, TextToSpeech.QUEUE_FLUSH, null, "sentence_id")
        }, instruction.pauseBeforeMs)
    }

    fun stop() {
        tts?.stop()
        handler.removeCallbacksAndMessages(null)
    }

    fun release() {
        tts?.shutdown()
        handler.removeCallbacksAndMessages(null)
    }
}
