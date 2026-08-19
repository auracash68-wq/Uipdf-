package com.example.data

import com.example.data.db.PdfDao
import com.example.model.RecentPdf
import kotlinx.coroutines.flow.Flow
import java.io.File

class RecentPdfRepository(private val pdfDao: PdfDao) {
    val allRecentPdfs: Flow<List<RecentPdf>> = pdfDao.getAllRecentPdfs()

    fun getRecentPdfsLimited(limit: Int): Flow<List<RecentPdf>> = pdfDao.getRecentPdfsLimited(limit)

    suspend fun addRecentPdf(
        name: String,
        file: File,
        pageCount: Int,
        operationType: String
    ): Long {
        val recentPdf = RecentPdf(
            name = name,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            pageCount = pageCount,
            operationType = operationType
        )
        return pdfDao.insertRecentPdf(recentPdf)
    }

    suspend fun deleteRecentPdf(id: Long, filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (_: Exception) {}
        pdfDao.deleteById(id)
    }

    suspend fun renameRecentPdf(id: Long, newName: String) {
        pdfDao.updateName(id, newName)
    }

    suspend fun clearAll() {
        pdfDao.clearAll()
    }
}
