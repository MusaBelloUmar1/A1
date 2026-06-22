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
    private var playbackSpeed = 1.0f

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
                    handler.removeCallbacksAndMessages(null)
                } else {
                    if (currentInstructions.isEmpty() && currentFilePath != null) {
                        loadAndPlayCurrentPage()
                    } else if (currentInstructions.isNotEmpty()) {
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
                    playbackSpeed = args.getFloat("speed", 1.0f)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SET_SLEEP_TIMER" -> {
                    val minutes = args.getInt("minutes", 0)
                    handler.removeCallbacks(sleepTimerRunnable)
                    if (minutes > 0) {
                        handler.postDelayed(sleepTimerRunnable, minutes * 60 * 1000L)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SEEK_TO_PAGE_PERCENT" -> {
                    val percent = args.getFloat("position", 0f)
                    seekToPercent(percent)
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

        if (currentInstructions.isEmpty()) {
            loadAndPlayCurrentPage()
            return
        }

        val instruction = currentInstructions.getOrNull(instructionIndex)
        if (instruction != null) {
            val speedInstruction = instruction.copy(speed = instruction.speed * playbackSpeed)
            ttsManager.speak(speedInstruction)
            broadcastProgress(speedInstruction.sentence)
            saveProgress()
        } else {
            // Page finished, load next
            currentPageIndex++
            instructionIndex = 0
            loadAndPlayCurrentPage()
        }
    }

    private val sleepTimerRunnable = Runnable {
        mediaSession?.player?.pause()
    }

    private fun seekToPercent(percent: Float) {
        val path = currentFilePath ?: return
        serviceScope.launch(Dispatchers.IO) {
            val totalPages = pdfParser.getPageCount(path)
            currentPageIndex = (totalPages * percent).toInt().coerceIn(0, totalPages - 1)
            instructionIndex = 0
            withContext(Dispatchers.Main) {
                loadAndPlayCurrentPage()
            }
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
            // We need to load previous page and go to last instruction
            // This is a bit complex for a simple implementation, let's just go to start of previous page for now
            instructionIndex = 0
            loadAndPlayCurrentPage()
        }
    }

    private fun broadcastProgress(sentence: String) {
        val bundle = Bundle().apply {
            putString("sentence", sentence)
            putInt("pageIndex", currentPageIndex)
            putInt("instructionIndex", instructionIndex)
        }
        mediaSession?.broadcastCustomCommand(SessionCommand("PROGRESS_UPDATE", Bundle.EMPTY), bundle)
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
