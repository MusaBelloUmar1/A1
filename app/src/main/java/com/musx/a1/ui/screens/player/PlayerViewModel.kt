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
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
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

    fun togglePlayback() {
        if (_isPlaying.value) {
            mediaController?.pause()
        } else {
            if (mediaController?.isPlaying == false && currentBook.value != null) {
                val args = android.os.Bundle().apply {
                    putString("filePath", currentBook.value?.filePath)
                    putInt("pageIndex", 0) // Resume logic should go here
                    putInt("sentenceIndex", 0)
                }
                mediaController?.sendCustomCommand(
                    androidx.media3.session.SessionCommand("START_BOOK", android.os.Bundle.EMPTY),
                    args
                )
            }
            mediaController?.play()
        }
        _isPlaying.value = !_isPlaying.value
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
