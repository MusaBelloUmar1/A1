package com.musx.a1.repository

import com.musx.a1.data.dao.BookDao
import com.musx.a1.data.dao.FolderDao
import com.musx.a1.data.dao.ProgressDao
import com.musx.a1.data.entity.Book
import com.musx.a1.data.entity.Folder
import kotlinx.coroutines.flow.Flow

import com.musx.a1.data.dao.PlaylistDao
import com.musx.a1.data.entity.Playlist
import com.musx.a1.data.entity.PlaylistBookCrossRef

class AppRepository(
    private val bookDao: BookDao,
    private val folderDao: FolderDao,
    private val playlistDao: PlaylistDao,
    private val progressDao: ProgressDao
) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val favoriteBooks: Flow<List<Book>> = bookDao.getFavoriteBooks()
    val recentlyOpenedBooks: Flow<List<Book>> = bookDao.getRecentlyOpenedBooks()
    val folders: Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun updateBook(book: Book) = bookDao.updateBook(book)
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)

    suspend fun insertFolder(folder: Folder) = folderDao.insertFolder(folder)

    suspend fun getProgress(bookId: Long) = progressDao.getProgressForBookSync(bookId)

    // Playlists
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    fun getBooksInPlaylist(playlistId: Long) = playlistDao.getBooksInPlaylist(playlistId)
    suspend fun insertPlaylist(playlist: Playlist) = playlistDao.insertPlaylist(playlist)
    suspend fun addBookToPlaylist(playlistId: Long, bookId: Long) =
        playlistDao.addBookToPlaylist(PlaylistBookCrossRef(playlistId, bookId))
}
