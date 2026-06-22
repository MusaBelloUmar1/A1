package com.musx.a1.repository

import com.musx.a1.data.dao.BookDao
import com.musx.a1.data.dao.FolderDao
import com.musx.a1.data.entity.Book
import com.musx.a1.data.entity.Folder
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val bookDao: BookDao,
    private val folderDao: FolderDao
) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val favoriteBooks: Flow<List<Book>> = bookDao.getFavoriteBooks()
    val recentlyOpenedBooks: Flow<List<Book>> = bookDao.getRecentlyOpenedBooks()
    val folders: Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun insertBook(book: Book) = bookDao.insertBook(book)
    suspend fun updateBook(book: Book) = bookDao.updateBook(book)
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)

    suspend fun insertFolder(folder: Folder) = folderDao.insertFolder(folder)
}
