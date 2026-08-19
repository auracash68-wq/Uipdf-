package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.util.UUID

object FileUtils {

    fun createTempPdfFile(context: Context, prefix: String = "proc"): File {
        val tempDir = File(context.cacheDir, "pdf_temp").apply { if (!exists()) mkdirs() }
        val randomSuffix = UUID.randomUUID().toString().take(8)
        val file = File(tempDir, "${prefix}_${System.currentTimeMillis()}_$randomSuffix.pdf")
        file.createNewFile()
        return file
    }

    fun createTempImageFile(context: Context, extension: String = "jpg"): File {
        val tempDir = File(context.cacheDir, "pdf_temp").apply { if (!exists()) mkdirs() }
        val randomSuffix = UUID.randomUUID().toString().take(8)
        val file = File(tempDir, "img_${System.currentTimeMillis()}_$randomSuffix.$extension")
        file.createNewFile()
        return file
    }

    fun createManagedOutputFile(context: Context, baseName: String): File {
        val docsDir = File(context.filesDir, "generated_pdfs").apply { if (!exists()) mkdirs() }
        val sanitized = sanitizeFileName(baseName)
        val finalName = if (sanitized.lowercase().endsWith(".pdf")) sanitized else "$sanitized.pdf"
        var target = File(docsDir, finalName)
        var counter = 1
        val nameWithoutExt = finalName.removeSuffix(".pdf")
        while (target.exists()) {
            target = File(docsDir, "${nameWithoutExt}_$counter.pdf")
            counter++
        }
        target.createNewFile()
        return target
    }

    fun cleanTempFiles(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "pdf_temp")
            if (tempDir.exists() && tempDir.isDirectory) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile) file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (colIndex != -1) {
                            name = cursor.getString(colIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment ?: "document.pdf"
        }
        return name ?: "document.pdf"
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (colIndex != -1) {
                            val size = cursor.getLong(colIndex)
                            if (size > 0) return size
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return 0L
    }

    fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "in_pdf"): File {
        val tempFile = createTempPdfFile(context, prefix)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Cannot read file from specified URI.")
        return tempFile
    }

    fun exportPdfToUri(context: Context, sourceFile: File, destUri: Uri) {
        context.contentResolver.openOutputStream(destUri)?.use { output ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to write to destination.")
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val df = DecimalFormat("#.##")
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
            bytes >= 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
            bytes >= 1024 -> "${df.format(bytes / 1024.0)} KB"
            else -> "$bytes B"
        }
    }

    fun checkAvailableStorage(context: Context, requiredBytes: Long = 10 * 1024 * 1024): Boolean {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            available >= requiredBytes
        } catch (_: Exception) {
            true
        }
    }

    fun sanitizeFileName(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "document.pdf"
        // Remove characters forbidden on filesystem: / \ ? % * : | " < >
        val sanitized = trimmed.replace(Regex("[/\\\\?%*:|\"<>]"), "_")
        return sanitized.ifEmpty { "document.pdf" }
    }
}
