package com.musx.a1.repository

import com.musx.a1.data.dao.BookDao
import com.musx.a1.data.dao.ChapterDao
import com.musx.a1.data.dao.FolderDao
import com.musx.a1.data.entity.Book
import com.musx.a1.data.entity.Chapter
import com.musx.a1.data.entity.Folder
import kotlinx.coroutines.flow.Flow

import com.musx.a1.data.dao.PlaylistDao
import com.musx.a1.data.dao.ProgressDao
import com.musx.a1.data.entity.Playlist
import com.musx.a1.data.entity.PlaylistBookCrossRef
import com.musx.a1.data.dao.BookmarkDao
import com.musx.a1.data.entity.Bookmark

class AppRepository(
    private val bookmarkDao: BookmarkDao,
    private val bookDao: BookDao,
    private val folderDao: FolderDao,
    private val playlistDao: PlaylistDao,
    private val progressDao: ProgressDao,
    private val chapterDao: ChapterDao
) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val favoriteBooks: Flow<List<Book>> = bookDao.getFavoriteBooks()
    val recentlyOpenedBooks: Flow<List<Book>> = bookDao.getRecentlyOpenedBooks()
    val folders: Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun getBookById(id: Long) = bookDao.getBookById(id)
    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun updateBook(book: Book) = bookDao.updateBook(book)
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)

    suspend fun insertFolder(folder: Folder) = folderDao.insertFolder(folder)

    // Playlists
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    fun getBooksInPlaylist(playlistId: Long) = playlistDao.getBooksInPlaylist(playlistId)
    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylist(playlist)
    suspend fun addBookToPlaylist(playlistId: Long, bookId: Long) =
        playlistDao.addBookToPlaylist(PlaylistBookCrossRef(playlistId, bookId))

    // Progress
    fun getProgressForBook(bookId: Long) = progressDao.getProgressForBook(bookId)
    suspend fun saveProgress(bookId: Long, pageIndex: Int, sentenceIndex: Int) =
        progressDao.saveProgress(com.musx.a1.data.entity.Progress(bookId, pageIndex, sentenceIndex, 0f))

    // Chapters
    fun getChaptersForBook(bookId: Long): Flow<List<Chapter>> = chapterDao.getChaptersForBook(bookId)
    suspend fun insertChapters(chapters: List<Chapter>) = chapterDao.insertChapters(chapters)

    // Bookmarks
    fun getBookmarksForBook(bookId: Long) = bookmarkDao.getBookmarksForBook(bookId)
    suspend fun insertBookmark(bookmark: Bookmark) = bookmarkDao.insertBookmark(bookmark)
    suspend fun deleteBookmark(bookmark: Bookmark) = bookmarkDao.deleteBookmark(bookmark)
}
