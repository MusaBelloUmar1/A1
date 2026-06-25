package com.musx.a1.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.musx.a1.data.entity.Book
import com.musx.a1.data.entity.Chapter
import com.musx.a1.playback.PlaybackService
import com.musx.a1.repository.AppRepository
import com.musx.a1.ui.state.AppState
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures

class PlayerViewModel(private val repository: AppRepository) : ViewModel() {
    private val _appState = MutableStateFlow<AppState>(AppState.Idle)
    val appState: StateFlow<AppState> = _appState
    private var mediaController: MediaController? = null

    fun initializeController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onCustomCommand(
                    controller: MediaController,
                    command: SessionCommand,
                    args: android.os.Bundle
                ): ListenableFuture<SessionResult> {
                    if (command.customAction == "PLAYBACK_UPDATE") {
                        _currentSentence.value = args.getString("sentence", "")
                        _currentPageIndex.value = args.getInt("pageIndex", 0)
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            })
            .buildAsync()
        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            controller.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
            })
        }, MoreExecutors.directExecutor())
    }
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters

    private var chaptersJob: kotlinx.coroutines.Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSentence = MutableStateFlow("Tap play to start listening.")
    val currentSentence: StateFlow<String> = _currentSentence

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    private val _repeatMode = MutableStateFlow(0) // 0: None, 1: One, 2: All
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _sleepTimerMillis = MutableStateFlow(0L)
    val sleepTimerMillis: StateFlow<Long> = _sleepTimerMillis

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val args = android.os.Bundle().apply { putFloat("speed", speed) }
        mediaController?.sendCustomCommand(
            SessionCommand("SET_SPEED", android.os.Bundle.EMPTY),
            args
        )
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        val args = android.os.Bundle().apply { putBoolean("enabled", _isShuffleEnabled.value) }
        mediaController?.sendCustomCommand(
            SessionCommand("TOGGLE_SHUFFLE", android.os.Bundle.EMPTY),
            args
        )
    }

    fun toggleRepeatMode() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
        val args = android.os.Bundle().apply { putInt("mode", _repeatMode.value) }
        mediaController?.sendCustomCommand(
            SessionCommand("SET_REPEAT_MODE", android.os.Bundle.EMPTY),
            args
        )
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMillis.value = minutes * 60 * 1000L
        val args = android.os.Bundle().apply { putInt("minutes", minutes) }
        mediaController?.sendCustomCommand(
            SessionCommand("SET_SLEEP_TIMER", android.os.Bundle.EMPTY),
            args
        )
    }

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _appState.value = AppState.LoadingBook
            val book = repository.getBookById(bookId)
            _currentBook.value = book

            if (book != null) {
                // Load chapters in a separate coroutine
                chaptersJob?.cancel()
                chaptersJob = repository.getChaptersForBook(bookId)
                    .onEach { _chapters.value = it }
                    .launchIn(viewModelScope)

                _appState.value = AppState.Ready

                val args = android.os.Bundle().apply { putLong("bookId", bookId) }
                mediaController?.sendCustomCommand(
                    SessionCommand("SET_BOOK_ID", android.os.Bundle.EMPTY),
                    args
                )
            } else {
                _appState.value = AppState.Ready
            }
        }
    }

    fun loadExternalUri(uri: String) {
        viewModelScope.launch {
            _appState.value = AppState.ParsingPdf
            // Create a temporary book object for UI
            _currentBook.value = Book(
                id = -1L,
                title = "External PDF",
                filePath = uri,
                totalPages = 0,
                coverImage = null
            )
            _appState.value = AppState.Ready

            // Auto-start playback for external URIs
            val args = android.os.Bundle().apply {
                putString("filePath", uri)
                putInt("pageIndex", 0)
                putInt("sentenceIndex", 0)
            }
            mediaController?.sendCustomCommand(
                SessionCommand("START_BOOK", android.os.Bundle.EMPTY),
                args
            )
            _isPlaying.value = true
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            _currentBook.value?.let { book ->
                val updatedBook = book.copy(favorite = !book.favorite)
                repository.updateBook(updatedBook)
                _currentBook.value = updatedBook
            }
        }
    }

    fun seekTo(position: Float) {
        val book = _currentBook.value ?: return
        val pageToSeek = (position * book.totalPages).toInt().coerceIn(0, book.totalPages - 1)
        val args = android.os.Bundle().apply {
            putString("filePath", book.filePath)
            putInt("pageIndex", pageToSeek)
            putInt("sentenceIndex", 0)
        }
        mediaController?.sendCustomCommand(
            SessionCommand("START_BOOK", android.os.Bundle.EMPTY),
            args
        )
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            mediaController?.pause()
            _isPlaying.value = false
        } else {
            if (mediaController?.isPlaying == false && currentBook.value != null) {
                viewModelScope.launch {
                    val book = currentBook.value!!
                    val progress = repository.getProgressForBook(book.id).first()
                    val args = android.os.Bundle().apply {
                        putString("filePath", book.filePath)
                        putInt("pageIndex", progress?.chapterIndex ?: 0)
                        putInt("sentenceIndex", progress?.sentenceIndex ?: 0)
                    }
                    mediaController?.sendCustomCommand(
                        androidx.media3.session.SessionCommand("START_BOOK", android.os.Bundle.EMPTY),
                        args
                    )
                    mediaController?.play()
                    _isPlaying.value = true
                }
            } else {
                mediaController?.play()
                _isPlaying.value = true
            }
        }
    }

    fun playChapter(chapter: Chapter) {
        val book = _currentBook.value ?: return
        val args = android.os.Bundle().apply {
            putString("filePath", book.filePath)
            putInt("pageIndex", chapter.startPage)
            putInt("sentenceIndex", 0)
        }
        mediaController?.sendCustomCommand(
            SessionCommand("START_BOOK", android.os.Bundle.EMPTY),
            args
        )
        _isPlaying.value = true
        mediaController?.play()
    }

    fun skipNext() {
        mediaController?.sendCustomCommand(
            SessionCommand("SKIP_NEXT", android.os.Bundle.EMPTY),
            android.os.Bundle.EMPTY
        )
    }

    fun skipPrevious() {
        mediaController?.sendCustomCommand(
            SessionCommand("SKIP_PREVIOUS", android.os.Bundle.EMPTY),
            android.os.Bundle.EMPTY
        )
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
