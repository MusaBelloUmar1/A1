package com.musx.a1.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val iconRes: String? = null // Optional icon identifier
)

@Entity(tableName = "playlist_books", primaryKeys = ["playlistId", "bookId"])
data class PlaylistBookCrossRef(
    val playlistId: Long,
    val bookId: Long
)
