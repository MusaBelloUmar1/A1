package com.musx.a1.playback

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.musx.a1.engine.TtsManager
import com.musx.a1.engine.PdfParser
import com.musx.a1.engine.QueueEngine
import com.musx.a1.domain.engine.NarrationEngine
import com.musx.a1.domain.engine.SpeechInstruction
import com.musx.a1.data.AppDatabase
import com.musx.a1.data.entity.Progress
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var ttsManager: TtsManager
    private lateinit var pdfParser: PdfParser
    private val narrationEngine = NarrationEngine()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentBookId: Long = -1L
    private var currentFilePath: String? = null
    private val handler = Handler(Looper.getMainLooper())

    private var currentInstructions: List<SpeechInstruction> = emptyList()
    private var instructionIndex: Int = 0
    private var currentPageIndex: Int = 0

    override fun onCreate() {
        super.onCreate()
        pdfParser = PdfParser(this)
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
                .add(SessionCommand("START_BOOK", Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(sessionCommands, Player.Commands.EMPTY)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                "SET_BOOK_ID" -> {
                    currentBookId = args.getLong("bookId", -1L)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "START_BOOK" -> {
                    currentFilePath = args.getString("filePath")
                    currentPageIndex = args.getInt("pageIndex", 0)
                    instructionIndex = args.getInt("sentenceIndex", 0)
                    loadAndPlayCurrentPage()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    private fun loadAndPlayCurrentPage() {
        val path = currentFilePath ?: return
        serviceScope.launch(Dispatchers.IO) {
            val text = pdfParser.extractTextFromPage(path, currentPageIndex)
            if (text != null) {
                currentInstructions = narrationEngine.process(text)
                withContext(Dispatchers.Main) {
                    playCurrentInstruction()
                }
            }
        }
    }

    private fun playCurrentInstruction() {
        val instruction = currentInstructions.getOrNull(instructionIndex)
        if (instruction != null) {
            ttsManager.speak(instruction)
            saveProgress()
        } else {
            // Page finished, load next
            currentPageIndex++
            instructionIndex = 0
            loadAndPlayCurrentPage()
        }
    }

    private fun handleSentenceFinished() {
        val currentInstruction = currentInstructions.getOrNull(instructionIndex)
        val delay = currentInstruction?.pauseAfterMs ?: 0L

        handler.postDelayed({
            instructionIndex++
            playCurrentInstruction()
        }, delay)
    }

    private fun saveProgress() {
        if (currentBookId == -1L) return

        serviceScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PlaybackService)
            db.progressDao().saveProgress(
                Progress(currentBookId, currentPageIndex, instructionIndex, 0f)
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
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
