package com.musx.a1.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musx.a1.data.entity.Book
import com.musx.a1.data.entity.Chapter
import com.musx.a1.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: AppRepository) : ViewModel() {
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
            // Load chapters and progress...
        }
    }

    fun togglePlayback() {
        _isPlaying.value = !_isPlaying.value
    }
}
