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
    private var shuffleEnabled = false
    private var repeatMode = 0 // 0: None, 1: One, 2: All
    private var sleepTimerRunnable: Runnable? = null

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

        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    ttsManager.stop()
                    // Don't remove all callbacks, only the sleep timer if needed or specific ones
                } else {
                    if (currentInstructions.isNotEmpty()) {
                        playCurrentInstruction()
                    }
                }
            }
        })

        ttsManager = TtsManager(this) {
            handleSentenceFinished()
        }
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand("SET_BOOK_ID", Bundle.EMPTY))
                .add(SessionCommand("START_BOOK", Bundle.EMPTY))
                .add(SessionCommand("SKIP_NEXT", Bundle.EMPTY))
                .add(SessionCommand("SKIP_PREVIOUS", Bundle.EMPTY))
                .add(SessionCommand("TOGGLE_SHUFFLE", Bundle.EMPTY))
                .add(SessionCommand("SET_REPEAT_MODE", Bundle.EMPTY))
                .add(SessionCommand("SET_SPEED", Bundle.EMPTY))
                .add(SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY))
                .add(SessionCommand("SEEK_TO_PAGE_PERCENT", Bundle.EMPTY))
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
                "SKIP_NEXT" -> {
                    skipNext()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SKIP_PREVIOUS" -> {
                    skipPrevious()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "TOGGLE_SHUFFLE" -> {
                    shuffleEnabled = args.getBoolean("enabled", false)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SET_REPEAT_MODE" -> {
                    repeatMode = args.getInt("mode", 0)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SET_SPEED" -> {
                    val speed = args.getFloat("speed", 1.0f)
                    ttsManager.setSpeed(speed)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SET_SLEEP_TIMER" -> {
                    val minutes = args.getInt("minutes", 0)
                    sleepTimerRunnable?.let { handler.removeCallbacks(it) }
                    if (minutes > 0) {
                        val runnable = Runnable {
                            mediaSession?.player?.pause()
                            sleepTimerRunnable = null
                        }
                        sleepTimerRunnable = runnable
                        handler.postDelayed(runnable, minutes * 60 * 1000L)
                    } else {
                        sleepTimerRunnable = null
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SEEK_TO_PAGE_PERCENT" -> {
                    val percent = args.getFloat("percent", 0f)
                    serviceScope.launch {
                        val totalPages = pdfParser.getPageCount(currentFilePath ?: "")
                        currentPageIndex = (totalPages * percent).toInt().coerceIn(0, totalPages - 1)
                        instructionIndex = 0
                        loadAndPlayCurrentPage()
                    }
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
        if (mediaSession?.player?.playWhenReady == false) return

        val instruction = currentInstructions.getOrNull(instructionIndex)
        if (instruction != null) {
            ttsManager.speak(instruction)
            saveProgress()
            broadcastState()
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

    private fun broadcastState() {
        val instruction = currentInstructions.getOrNull(instructionIndex) ?: return
        val bundle = Bundle().apply {
            putString("sentence", instruction.sentence)
            putInt("pageIndex", currentPageIndex)
            putInt("instructionIndex", instructionIndex)
        }
        mediaSession?.setSessionExtras(bundle)
    }

    private fun skipNext() {
        if (instructionIndex + 1 < currentInstructions.size) {
            instructionIndex++
            playCurrentInstruction()
        } else {
            currentPageIndex++
            instructionIndex = 0
            loadAndPlayCurrentPage()
        }
    }

    private fun skipPrevious() {
        if (instructionIndex > 0) {
            instructionIndex--
            playCurrentInstruction()
        } else if (currentPageIndex > 0) {
            currentPageIndex--
            instructionIndex = 0
            loadAndPlayCurrentPage()
        }
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
        sleepTimerRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
