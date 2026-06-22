package com.musx.a1.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musx.a1.data.entity.Book
import com.musx.a1.repository.AppRepository
import com.musx.a1.ui.state.AppState
import kotlinx.coroutines.flow.*

class LibraryViewModel(private val repository: AppRepository) : ViewModel() {
    private val _appState = MutableStateFlow<AppState>(AppState.LoadingLibrary)
    val appState: StateFlow<AppState> = _appState
    val allBooks: StateFlow<List<Book>> = repository.allBooks.onEach {
        _appState.value = AppState.Ready
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentlyOpened: StateFlow<List<Book>> = repository.recentlyOpenedBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
