package com.musx.a1.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musx.a1.data.entity.Folder
import com.musx.a1.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: AppRepository) : ViewModel() {
    private val _showLyrics = MutableStateFlow(true)
    val showLyrics: StateFlow<Boolean> = _showLyrics

    private val _autoPlayNext = MutableStateFlow(true)
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext

    fun addFolder(context: android.content.Context, uri: String) {
        viewModelScope.launch {
            repository.insertFolder(Folder(uri = uri))
            val scanner = com.musx.a1.engine.scanner.FolderScanner(
                context,
                repository,
                com.musx.a1.engine.PdfParser(context)
            )
            scanner.scanFolder(uri)
        }
    }

    fun toggleLyrics(enabled: Boolean) {
        _showLyrics.value = enabled
    }

    fun toggleAutoPlay(enabled: Boolean) {
        _autoPlayNext.value = enabled
    }

    fun clearCache() {
        // In a real app, this would delete temp files or clear a database cache
        // For now, we simulate it
    }
}
