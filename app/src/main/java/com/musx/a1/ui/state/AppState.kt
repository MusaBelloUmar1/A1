package com.musx.a1.ui.state

sealed class AppState {
    object Idle : AppState()
    object LoadingLibrary : AppState()
    object ScanningFolders : AppState()
    object ParsingPdf : AppState()
    object PreparingNarration : AppState()
    object LoadingBook : AppState()
    object Ready : AppState()
    data class Error(val message: String) : AppState()
}

data class LoadingProgress(
    val step: String,
    val percent: Int
)
