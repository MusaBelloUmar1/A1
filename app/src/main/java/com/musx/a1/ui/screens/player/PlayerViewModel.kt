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
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: AppRepository) : ViewModel() {
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
            _currentBook.value = repository.allBooks.firstOrNull()?.find { it.id == bookId }
            val args = android.os.Bundle().apply { putLong("bookId", bookId) }
            mediaController?.sendCustomCommand(
                androidx.media3.session.SessionCommand("SET_BOOK_ID", android.os.Bundle.EMPTY),
                args
            )
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
        _isPlaying.value = !_isPlaying.value
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
