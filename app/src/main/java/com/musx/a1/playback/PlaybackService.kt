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
import com.musx.a1.domain.engine.NarrationEngine
import com.musx.a1.domain.engine.SpeechInstruction
import com.musx.a1.data.AppDatabase
import com.musx.a1.data.entity.Progress
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

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
    private var shuffledIndices: List<Int> = emptyList()
    private var shufflePointer: Int = 0
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
                .add(SessionCommand("SET_SLEEP_TIMER", Bundle.EMPTY))                .build()
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
                    val providedPage = args.getInt("pageIndex", -1)
                    val providedSentence = args.getInt("sentenceIndex", -1)

                    serviceScope.launch {
                        val path = currentFilePath
                        if (path != null && shuffleEnabled) {
                            val totalPages = withContext(Dispatchers.IO) { pdfParser.getPageCount(path) }
                            generateShuffledIndices(totalPages)
                        }

                        if (providedPage != -1 && providedSentence != -1) {
                            currentPageIndex = providedPage
                            instructionIndex = providedSentence
                            if (shuffleEnabled) {
                                shufflePointer = shuffledIndices.indexOf(currentPageIndex).coerceAtLeast(0)
                            }
                        } else {
                            if (currentBookId != -1L) {
                                val db = AppDatabase.getDatabase(this@PlaybackService)
                                val progress = withContext(Dispatchers.IO) {
                                    db.progressDao().getProgressForBook(currentBookId).first()
                                }
                                currentPageIndex = progress?.chapterIndex ?: 0
                                instructionIndex = progress?.sentenceIndex ?: 0
                                if (shuffleEnabled) {
                                    shufflePointer = shuffledIndices.indexOf(currentPageIndex).coerceAtLeast(0)
                                }
                            } else {
                                currentPageIndex = 0
                                instructionIndex = 0
                                shufflePointer = 0
                            }
                        }
                        loadAndPlayCurrentPage()
                    }
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
                    if (shuffleEnabled && currentFilePath != null) {
                        serviceScope.launch {
                            val totalPages = withContext(Dispatchers.IO) { pdfParser.getPageCount(currentFilePath!!) }
                            generateShuffledIndices(totalPages)
                            shufflePointer = shuffledIndices.indexOf(currentPageIndex).coerceAtLeast(0)
                        }
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SET_REPEAT_MODE" -> {
                    repeatMode = args.getInt("mode", 0)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SET_SPEED" -> {
                    playbackSpeed = args.getFloat("speed", 1.0f)
                    ttsManager.setSpeed(playbackSpeed)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                "SET_SLEEP_TIMER" -> {
                    val minutes = args.getInt("minutes", 0)
                    startSleepTimer(minutes)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    private fun startSleepTimer(minutes: Int) {
        handler.removeCallbacks(sleepTimerRunnable)
        if (minutes > 0) {
            handler.postDelayed(sleepTimerRunnable, minutes.toLong() * 60 * 1000L)
        }
    }

    private val sleepTimerRunnable = Runnable {
        mediaSession?.player?.pause()
    }

    private fun loadAndPlayCurrentPage() {
        val path = currentFilePath ?: return
        serviceScope.launch(Dispatchers.IO) {
            try {
                val text = pdfParser.extractTextFromPage(path, currentPageIndex)
                if (text != null && text.isNotBlank()) {
                    currentInstructions = narrationEngine.process(text)
                    withContext(Dispatchers.Main) {
                        playCurrentInstruction()
                    }
                } else {
                    // Empty page or extraction failed, try next page
                    withContext(Dispatchers.Main) {
                        skipNext()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Log error or notify UI
                    broadcastError("Failed to extract text from page $currentPageIndex")
                }
            }
        }
    }

    private fun generateShuffledIndices(totalPages: Int) {
        shuffledIndices = (0 until totalPages).shuffled()
    }

    private fun playCurrentInstruction() {
        if (mediaSession?.player?.playWhenReady == false) return

        val instruction = currentInstructions.getOrNull(instructionIndex)
        if (instruction != null) {
            ttsManager.speak(instruction)
            saveProgress()
            broadcastUpdate()
        } else {
            // Page finished, check if book finished
            serviceScope.launch {
                val path = currentFilePath ?: return@launch
                val totalPages = withContext(Dispatchers.IO) { pdfParser.getPageCount(path) }

                if (shuffleEnabled) {
                    if (shufflePointer + 1 < shuffledIndices.size) {
                        shufflePointer++
                        currentPageIndex = shuffledIndices[shufflePointer]
                        instructionIndex = 0
                        loadAndPlayCurrentPage()
                    } else {
                        handleBookFinished(totalPages)
                    }
                } else {
                    if (currentPageIndex + 1 < totalPages) {
                        currentPageIndex++
                        instructionIndex = 0
                        loadAndPlayCurrentPage()
                    } else {
                        handleBookFinished(totalPages)
                    }
                }
            }
        }
    }

    private fun handleBookFinished(totalPages: Int) {
        if (repeatMode == 2) { // Repeat All
            if (shuffleEnabled) {
                generateShuffledIndices(totalPages)
                shufflePointer = 0
                if (shuffledIndices.isNotEmpty()) {
                    currentPageIndex = shuffledIndices[shufflePointer]
                } else {
                    currentPageIndex = 0
                }
            } else {
                currentPageIndex = 0
            }
            instructionIndex = 0
            loadAndPlayCurrentPage()
        } else {
            mediaSession?.player?.pause()
            currentPageIndex = 0
            instructionIndex = 0
            shufflePointer = 0
        }
    }

    private fun handleSentenceFinished() {
        val currentInstruction = currentInstructions.getOrNull(instructionIndex)
        val delay = currentInstruction?.pauseAfterMs ?: 0L

        handler.postDelayed({
            if (repeatMode == 1) { // Repeat One (Sentence)
                playCurrentInstruction()
            } else {
                instructionIndex++
                playCurrentInstruction()
            }
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
            serviceScope.launch {
                val path = currentFilePath ?: return@launch
                val totalPages = withContext(Dispatchers.IO) { pdfParser.getPageCount(path) }

                if (shuffleEnabled && shuffledIndices.isNotEmpty()) {
                    if (shufflePointer + 1 < shuffledIndices.size) {
                        shufflePointer++
                    } else {
                        shufflePointer = 0
                    }
                    currentPageIndex = shuffledIndices[shufflePointer]
                } else {
                    if (currentPageIndex + 1 < totalPages) {
                        currentPageIndex++
                    } else {
                        currentPageIndex = 0
                    }
                }
                instructionIndex = 0
                loadAndPlayCurrentPage()
            }
        }
    }

    private fun skipPrevious() {
        if (instructionIndex > 0) {
            instructionIndex--
            playCurrentInstruction()
        } else {
            serviceScope.launch {
                val path = currentFilePath ?: return@launch
                val totalPages = withContext(Dispatchers.IO) { pdfParser.getPageCount(path) }

                if (shuffleEnabled && shuffledIndices.isNotEmpty()) {
                    if (shufflePointer > 0) {
                        shufflePointer--
                    } else {
                        shufflePointer = (shuffledIndices.size - 1).coerceAtLeast(0)
                    }
                    currentPageIndex = shuffledIndices[shufflePointer]
                } else {
                    if (currentPageIndex > 0) {
                        currentPageIndex--
                    } else {
                        currentPageIndex = (totalPages - 1).coerceAtLeast(0)
                    }
                }
                instructionIndex = 0
                loadAndPlayPreviousPage()
            }
        }
    }

    private fun loadAndPlayPreviousPage() {
        val path = currentFilePath ?: return
        serviceScope.launch(Dispatchers.IO) {
            try {
                val text = pdfParser.extractTextFromPage(path, currentPageIndex)
                if (text != null && text.isNotBlank()) {
                    currentInstructions = narrationEngine.process(text)
                    instructionIndex = (currentInstructions.size - 1).coerceAtLeast(0)
                    withContext(Dispatchers.Main) {
                        playCurrentInstruction()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        skipPrevious()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    broadcastError("Failed to extract text from page $currentPageIndex")
                }
            }
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

    private fun broadcastError(message: String) {
        val args = Bundle().apply {
            putString("error", message)
        }
        mediaSession?.broadcastCustomCommand(
            SessionCommand("PLAYBACK_ERROR", Bundle.EMPTY),
            args
        )
    }

    private fun broadcastUpdate() {
        val instruction = currentInstructions.getOrNull(instructionIndex)
        val args = Bundle().apply {
            putString("sentence", instruction?.sentence ?: "")
            putInt("pageIndex", currentPageIndex)
            putInt("sentenceIndex", instructionIndex)
            putInt("totalSentences", currentInstructions.size)
            putStringArrayList("pageSentences", ArrayList(currentInstructions.map { it.sentence }))
        }
        mediaSession?.broadcastCustomCommand(
            SessionCommand("PLAYBACK_UPDATE", Bundle.EMPTY),
            args
        )
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
