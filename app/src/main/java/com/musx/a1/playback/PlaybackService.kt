package com.musx.a1.playback

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.musx.a1.engine.TtsManager
import com.musx.a1.engine.QueueEngine
import com.musx.a1.data.AppDatabase
import com.musx.a1.data.entity.Progress
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var ttsManager: TtsManager
    private val queueEngine = QueueEngine()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentBookId: Long = -1L

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

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(CustomMediaSessionCallback())
            .build()

        ttsManager = TtsManager(this) {
            handleSentenceFinished()
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand("SET_BOOK_ID", Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(sessionCommands, Player.Commands.EMPTY)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == "SET_BOOK_ID") {
                currentBookId = args.getLong("bookId", -1L)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    private fun handleSentenceFinished() {
        val next = queueEngine.nextSentence()
        if (next != null) {
            ttsManager.speak(next)
            saveProgress()
        }
    }

    private fun saveProgress() {
        if (currentBookId == -1L) return

        val chapterIndex = queueEngine.getCurrentChapterIndex()
        val sentenceIndex = queueEngine.getCurrentSentenceIndex()

        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PlaybackService)
            db.progressDao().saveProgress(
                Progress(currentBookId, chapterIndex, sentenceIndex, 0f)
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
