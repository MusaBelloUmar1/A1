package com.musx.a1.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.musx.a1.data.entity.Book
import com.musx.a1.data.entity.Chapter
import com.musx.a1.playback.PlaybackService
import com.musx.a1.repository.AppRepository
import com.musx.a1.ui.state.AppState
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
                    command: androidx.media3.session.SessionCommand,
                    args: android.os.Bundle
                ): com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                    if (command.customAction == "PROGRESS_UPDATE") {
                        _currentSentence.value = args.getString("sentence", "")
                        _currentPage.value = args.getInt("pageIndex", 0)
                    }
                    return com.google.common.util.concurrent.Futures.immediateFuture(
                        androidx.media3.session.SessionResult(androidx.media3.session.SessionResult.RESULT_SUCCESS)
                    )
                }
            })
            .buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                    if (events.contains(androidx.media3.common.Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                        _isPlaying.value = player.playWhenReady
                    }
                }
            })
        }, MoreExecutors.directExecutor())
    }
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSentence = MutableStateFlow("Tap play to start listening.")
    val currentSentence: StateFlow<String> = _currentSentence

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

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
            androidx.media3.session.SessionCommand("SET_SPEED", android.os.Bundle.EMPTY),
            args
        )
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        val args = android.os.Bundle().apply { putBoolean("enabled", _isShuffleEnabled.value) }
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand("TOGGLE_SHUFFLE", android.os.Bundle.EMPTY),
            args
        )
    }

    fun toggleRepeatMode() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
        val args = android.os.Bundle().apply { putInt("mode", _repeatMode.value) }
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand("SET_REPEAT_MODE", android.os.Bundle.EMPTY),
            args
        )
    }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMillis.value = minutes * 60 * 1000L
        val args = android.os.Bundle().apply { putInt("minutes", minutes) }
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand("SET_SLEEP_TIMER", android.os.Bundle.EMPTY),
            args
        )
    }

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _appState.value = AppState.LoadingBook
            val book = repository.allBooks.first().find { it.id == bookId }
            _currentBook.value = book
            _appState.value = AppState.Ready

            if (book != null) {
                val args = android.os.Bundle().apply { putLong("bookId", bookId) }
                mediaController?.sendCustomCommand(
                    androidx.media3.session.SessionCommand("SET_BOOK_ID", android.os.Bundle.EMPTY),
                    args
                )
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
                androidx.media3.session.SessionCommand("START_BOOK", android.os.Bundle.EMPTY),
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
        val args = android.os.Bundle().apply { putFloat("position", position) }
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand("SEEK_TO_PAGE_PERCENT", android.os.Bundle.EMPTY),
            args
        )
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            mediaController?.pause()
        } else {
            if (mediaController?.isPlaying == false && currentBook.value != null) {
                viewModelScope.launch {
                    val progress = repository.getProgress(currentBook.value!!.id)
                    val args = android.os.Bundle().apply {
                        putString("filePath", currentBook.value?.filePath)
                        putInt("pageIndex", progress?.chapterIndex ?: 0)
                        putInt("sentenceIndex", progress?.sentenceIndex ?: 0)
                    }
                    mediaController?.sendCustomCommand(
                        androidx.media3.session.SessionCommand("START_BOOK", android.os.Bundle.EMPTY),
                        args
                    )
                }
            }
            mediaController?.play()
        }
        _isPlaying.value = !_isPlaying.value
    }

    fun skipNext() {
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand("SKIP_NEXT", android.os.Bundle.EMPTY),
            android.os.Bundle.EMPTY
        )
    }

    fun skipPrevious() {
        mediaController?.sendCustomCommand(
            androidx.media3.session.SessionCommand("SKIP_PREVIOUS", android.os.Bundle.EMPTY),
            android.os.Bundle.EMPTY
        )
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
