package com.musx.a1.repository

import com.musx.a1.data.dao.*
import com.musx.a1.data.entity.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
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
    fun getProgressForBook(bookId: Long): Flow<Progress?> = progressDao.getProgressForBook(bookId)
    suspend fun saveProgress(progress: Progress) = progressDao.saveProgress(progress)

    // Chapters
    fun getChaptersForBook(bookId: Long): Flow<List<Chapter>> = chapterDao.getChaptersForBook(bookId)
    suspend fun insertChapters(chapters: List<Chapter>) = chapterDao.insertChapters(chapters)
}
