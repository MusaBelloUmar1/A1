package com.musx.a1.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musx.a1.engine.TtsManager
import com.musx.a1.engine.QueueEngine
import com.musx.a1.data.AppDatabase
import com.musx.a1.data.entity.Progress
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var ttsManager: TtsManager
    private val queueEngine = QueueEngine()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        mediaSession = MediaSession.Builder(this, player).build()

        ttsManager = TtsManager(this) {
            handleSentenceFinished()
        }
    }

    private fun handleSentenceFinished() {
        val next = queueEngine.nextSentence()
        if (next != null) {
            ttsManager.speak(next)
            saveProgress()
        } else {
            // End of chapter logic
        }
    }

    private fun saveProgress() {
        val bookId = 0L // Need current bookId
        val chapterIndex = queueEngine.getCurrentChapterIndex()
        val sentenceIndex = queueEngine.getCurrentSentenceIndex()

        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PlaybackService)
            db.progressDao().saveProgress(
                Progress(bookId, chapterIndex, sentenceIndex, 0f)
            )
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        ttsManager.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}
