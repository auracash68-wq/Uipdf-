package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.RecentPdf
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM recent_pdfs ORDER BY createdAt DESC")
    fun getAllRecentPdfs(): Flow<List<RecentPdf>>

    @Query("SELECT * FROM recent_pdfs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentPdfsLimited(limit: Int): Flow<List<RecentPdf>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPdf(pdf: RecentPdf): Long

    @Query("DELETE FROM recent_pdfs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recent_pdfs SET name = :newName WHERE id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("DELETE FROM recent_pdfs")
    suspend fun clearAll()
}
