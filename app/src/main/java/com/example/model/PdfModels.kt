package com.example.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

enum class PdfOperationType(val displayName: String) {
    MERGE("Merge"),
    SPLIT("Split"),
    EXTRACT("Extract"),
    ROTATE("Rotate"),
    IMAGE_TO_PDF("Image to PDF"),
    TEXT_TO_PDF("Text to PDF"),
    LOCK("Lock"),
    UNLOCK("Unlock"),
    COMPRESS("Compress"),
    SIGN("Sign")
}

@Entity(tableName = "recent_pdfs")
data class RecentPdf(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val operationType: String
)

enum class UserEntitlement {
    FREE,
    PREMIUM,
    UNKNOWN
}

enum class DocPageSize(val title: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("US Letter (8.5 × 11 in)", 612, 792),
    AUTO("Fit to Image", 0, 0)
}

enum class DocOrientation(val title: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    AUTO("Auto")
}

enum class DocMargin(val title: String, val paddingPt: Int) {
    NONE("None (0 pt)", 0),
    SMALL("Small (18 pt)", 18),
    NORMAL("Normal (36 pt)", 36)
}

enum class ImageQualityPreset(val title: String, val qualityPercent: Int) {
    HIGH("High (90%)", 90),
    BALANCED("Balanced (75%)", 75),
    LOW("Low (55%)", 55)
}

enum class CompressionPreset(val title: String, val description: String, val scaleFactor: Float, val jpegQuality: Int) {
    MAXIMUM_QUALITY("Maximum Quality", "Low compression, sharp text and photos", 1.0f, 85),
    BALANCED("Balanced", "Recommended balance of size and clarity", 0.75f, 70),
    SMALLER_SIZE("Smaller Size", "High compression for easy email/sharing", 0.55f, 50)
}

enum class SplitMode {
    ALL_PAGES,
    PAGE_RANGE,
    CUSTOM_PAGES
}

data class SelectedFileItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long = 0L,
    val pageCount: Int = 0
)

data class SelectedImageItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long = 0L
)

sealed class PdfProcessResult {
    data class Success(
        val file: File,
        val name: String,
        val sizeBytes: Long,
        val pageCount: Int
    ) : PdfProcessResult()

    data class Error(val message: String) : PdfProcessResult()
    object Cancelled : PdfProcessResult()
}
