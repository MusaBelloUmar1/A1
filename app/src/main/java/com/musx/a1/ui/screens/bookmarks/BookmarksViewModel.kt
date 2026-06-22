package com.musx.a1.ui.screens.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musx.a1.data.dao.BookmarkDao
import com.musx.a1.data.entity.Bookmark
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BookmarksViewModel(private val bookmarkDao: BookmarkDao) : ViewModel() {
    val bookmarks: StateFlow<List<Bookmark>> = bookmarkDao.getAllBookmarks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
