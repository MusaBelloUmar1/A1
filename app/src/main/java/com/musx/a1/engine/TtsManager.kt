package com.musx.a1.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(context: Context, private val onSentenceFinished: () -> Unit) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
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

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sentence_id")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun setSpeed(speed: Float) {
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(speed)
    }

    fun release() {
        tts?.shutdown()
    }
}
