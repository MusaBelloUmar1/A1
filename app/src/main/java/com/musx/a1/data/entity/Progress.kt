package com.musx.a1.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class Progress(
    @PrimaryKey val bookId: Long,
    val chapterIndex: Int,
    val sentenceIndex: Int,
    val percentage: Float,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
