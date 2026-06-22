package com.musx.a1.data.dao

import androidx.room.*
import com.musx.a1.data.entity.Playlist
import com.musx.a1.data.entity.PlaylistBookCrossRef
import com.musx.a1.data.entity.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToPlaylist(crossRef: PlaylistBookCrossRef)

    @Query("""
        SELECT * FROM books
        INNER JOIN playlist_books ON books.id = playlist_books.bookId
        WHERE playlist_books.playlistId = :playlistId
    """)
    fun getBooksInPlaylist(playlistId: Long): Flow<List<Book>>
}
