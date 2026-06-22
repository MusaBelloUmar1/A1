package com.musx.a1.data.dao

import androidx.room.*
import com.musx.a1.data.entity.Progress
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE bookId = :bookId")
    fun getProgressForBook(bookId: Long): Flow<Progress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: Progress)

    @Query("DELETE FROM progress WHERE bookId = :bookId")
    suspend fun deleteProgressForBook(bookId: Long)
}
