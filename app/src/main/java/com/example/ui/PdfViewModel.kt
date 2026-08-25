package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppThemeMode
import com.example.engine.PdfStatusInfo
import com.example.model.CompressionPreset
import com.example.model.DocMargin
import com.example.model.DocOrientation
import com.example.model.DocPageSize
import com.example.model.ImageQualityPreset
import com.example.model.PdfOperationType
import com.example.model.RecentPdf
import com.example.model.SelectedFileItem
import com.example.model.UserEntitlement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class OperationUiState {
    object Idle : OperationUiState()
    data class Processing(val message: String) : OperationUiState()
    data class Success(
        val file: File,
        val fileName: String,
        val sizeBytes: Long,
        val pageCount: Int,
        val operationType: PdfOperationType
    ) : OperationUiState()
    data class Error(val message: String) : OperationUiState()
}

/**
 * Pure UI/UX State Manager for Universal PDF Utility.
 * Keeps all UI states responsive, smooth, and lightweight.
 */
class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _entitlement = MutableStateFlow(UserEntitlement.FREE)
    val entitlement: StateFlow<UserEntitlement> = _entitlement.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(false)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _guideVideosEnabled = MutableStateFlow(false)
    val guideVideosEnabled: StateFlow<Boolean> = _guideVideosEnabled.asStateFlow()

    private val _isGuideVideoPreferenceAsked = MutableStateFlow(true)
    val isGuideVideoPreferenceAsked: StateFlow<Boolean> = _isGuideVideoPreferenceAsked.asStateFlow()

    // Sample UI Demo Recent Files
    private val _allRecentPdfs = MutableStateFlow(
        listOf(
            RecentPdf(
                id = 1,
                name = "Contract_Signed_2026.pdf",
                filePath = "/demo/Contract_Signed_2026.pdf",
                sizeBytes = 2_450_000,
                pageCount = 6,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 35,
                operationType = "SIGN"
            ),
            RecentPdf(
                id = 2,
                name = "Merged_Project_Report.pdf",
                filePath = "/demo/Merged_Project_Report.pdf",
                sizeBytes = 4_890_000,
                pageCount = 14,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 180,
                operationType = "MERGE"
            ),
            RecentPdf(
                id = 3,
                name = "Invoice_May_Compressed.pdf",
                filePath = "/demo/Invoice_May_Compressed.pdf",
                sizeBytes = 850_000,
                pageCount = 2,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 24,
                operationType = "COMPRESS"
            ),
            RecentPdf(
                id = 4,
                name = "Scanned_Receipts_Images.pdf",
                filePath = "/demo/Scanned_Receipts_Images.pdf",
                sizeBytes = 3_120_000,
                pageCount = 5,
                createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 48,
                operationType = "IMAGE_TO_PDF"
            )
        )
    )
    val allRecentPdfs: StateFlow<List<RecentPdf>> = _allRecentPdfs.asStateFlow()

    private val _previewRecentPdfs = MutableStateFlow(_allRecentPdfs.value.take(4))
    val previewRecentPdfs: StateFlow<List<RecentPdf>> = _previewRecentPdfs.asStateFlow()

    private val _uiState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val uiState: StateFlow<OperationUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    var pendingExportFile: File? = null

    fun resetState() {
        _uiState.value = OperationUiState.Idle
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun setGuideVideosEnabled(enabled: Boolean) {
        _guideVideosEnabled.value = enabled
        _isGuideVideoPreferenceAsked.value = true
    }

    fun completeOnboarding(enableGuides: Boolean) {
        _guideVideosEnabled.value = enableGuides
        _isGuideVideoPreferenceAsked.value = true
        _isFirstLaunch.value = false
    }

    fun completeFirstLaunch(enableGuides: Boolean? = null) {
        if (enableGuides != null) {
            _guideVideosEnabled.value = enableGuides
            _isGuideVideoPreferenceAsked.value = true
        }
        _isFirstLaunch.value = false
    }

    fun setTheme(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun setEntitlement(entitlement: UserEntitlement) {
        _entitlement.value = entitlement
    }

    fun isOperationAllowed(context: Context): Boolean = true

    fun buyPremium(activity: Activity?) {
        _entitlement.value = UserEntitlement.PREMIUM
        _toastMessage.value = "Premium Plan Activated!"
    }

    fun restorePurchases() {
        _toastMessage.value = "Purchases Restored Successfully."
    }

    // --- Interactive UI PDF Actions (Fast, Mock-Responsive UI Feedback) ---

    fun mergePdfs(activity: Activity? = null, fileItems: List<SelectedFileItem>, outputName: String) {
        if (fileItems.size < 2) {
            _uiState.value = OperationUiState.Error("Please select at least 2 PDF files.")
            return
        }
        _uiState.value = OperationUiState.Processing("Merging ${fileItems.size} PDF files...")
        viewModelScope.launch {
            delay(1000)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Merged_Document" }}.pdf")
            demoFile.writeText("Demo Merged PDF Content")
            val totalPages = fileItems.sumOf { if (it.pageCount > 0) it.pageCount else 3 }
            addRecent(demoFile.name, demoFile.absolutePath, 3_500_000L, totalPages, "MERGE")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 3_500_000L,
                pageCount = totalPages,
                operationType = PdfOperationType.MERGE
            )
        }
    }

    fun splitPdf(activity: Activity? = null, sourceUri: Uri, pageRange: String, outputName: String) {
        _uiState.value = OperationUiState.Processing("Splitting PDF document...")
        viewModelScope.launch {
            delay(900)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Split_Document" }}.pdf")
            demoFile.writeText("Demo Split PDF Content")
            addRecent(demoFile.name, demoFile.absolutePath, 1_200_000L, 2, "SPLIT")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 1_200_000L,
                pageCount = 2,
                operationType = PdfOperationType.SPLIT
            )
        }
    }

    fun extractPages(activity: Activity? = null, sourceUri: Uri, pageNumbers: List<Int>, outputName: String) {
        _uiState.value = OperationUiState.Processing("Extracting pages...")
        viewModelScope.launch {
            delay(900)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Extracted_Pages" }}.pdf")
            demoFile.writeText("Demo Extracted Pages PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 950_000L, pageNumbers.size, "EXTRACT")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 950_000L,
                pageCount = pageNumbers.size,
                operationType = PdfOperationType.EXTRACT
            )
        }
    }

    fun rotatePdf(activity: Activity? = null, sourceUri: Uri, degrees: Int, targetPages: List<Int>?, outputName: String) {
        _uiState.value = OperationUiState.Processing("Rotating PDF...")
        viewModelScope.launch {
            delay(800)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Rotated_Document" }}.pdf")
            demoFile.writeText("Demo Rotated PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 2_100_000L, 4, "ROTATE")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 2_100_000L,
                pageCount = 4,
                operationType = PdfOperationType.ROTATE
            )
        }
    }

    fun imagesToPdf(
        activity: Activity? = null,
        imageUris: List<Uri>,
        pageSize: DocPageSize,
        orientation: DocOrientation,
        margin: DocMargin,
        quality: ImageQualityPreset,
        outputName: String
    ) {
        _uiState.value = OperationUiState.Processing("Converting ${imageUris.size} images to PDF...")
        viewModelScope.launch {
            delay(1000)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Photos_Document" }}.pdf")
            demoFile.writeText("Demo Images to PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 2_800_000L, imageUris.size, "IMAGE_TO_PDF")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 2_800_000L,
                pageCount = imageUris.size,
                operationType = PdfOperationType.IMAGE_TO_PDF
            )
        }
    }

    fun textToPdf(
        activity: Activity? = null,
        title: String,
        bodyText: String,
        fontSizeSp: Float,
        outputName: String
    ) {
        _uiState.value = OperationUiState.Processing("Generating PDF from text...")
        viewModelScope.launch {
            delay(700)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Notes_Document" }}.pdf")
            demoFile.writeText("Demo Text PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 450_000L, 1, "TEXT_TO_PDF")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 450_000L,
                pageCount = 1,
                operationType = PdfOperationType.TEXT_TO_PDF
            )
        }
    }

    fun lockPdf(activity: Activity? = null, sourceUri: Uri, password: String, outputName: String) {
        _uiState.value = OperationUiState.Processing("Encrypting PDF document...")
        viewModelScope.launch {
            delay(800)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Locked_Document" }}.pdf")
            demoFile.writeText("Demo Encrypted PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 1_800_000L, 3, "LOCK")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 1_800_000L,
                pageCount = 3,
                operationType = PdfOperationType.LOCK
            )
        }
    }

    fun unlockPdf(activity: Activity? = null, sourceUri: Uri, password: String, outputName: String) {
        _uiState.value = OperationUiState.Processing("Decrypting PDF document...")
        viewModelScope.launch {
            delay(800)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Unlocked_Document" }}.pdf")
            demoFile.writeText("Demo Decrypted PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 1_750_000L, 3, "UNLOCK")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 1_750_000L,
                pageCount = 3,
                operationType = PdfOperationType.UNLOCK
            )
        }
    }

    fun compressPdf(activity: Activity? = null, sourceUri: Uri, preset: CompressionPreset, outputName: String) {
        _uiState.value = OperationUiState.Processing("Compressing PDF document...")
        viewModelScope.launch {
            delay(900)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Compressed_Document" }}.pdf")
            demoFile.writeText("Demo Compressed PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 920_000L, 5, "COMPRESS")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 920_000L,
                pageCount = 5,
                operationType = PdfOperationType.COMPRESS
            )
        }
    }

    fun signPdf(
        activity: Activity? = null,
        sourceUri: Uri,
        signatureBitmap: Bitmap,
        targetPageNumber: Int,
        normX: Float,
        normY: Float,
        normWidth: Float,
        normHeight: Float,
        allPages: Boolean,
        outputName: String
    ) {
        _uiState.value = OperationUiState.Processing("Applying signature to document...")
        viewModelScope.launch {
            delay(900)
            val demoFile = File(getApplication<Application>().cacheDir, "${outputName.ifBlank { "Signed_Document" }}.pdf")
            demoFile.writeText("Demo Signed PDF")
            addRecent(demoFile.name, demoFile.absolutePath, 2_200_000L, 4, "SIGN")
            _uiState.value = OperationUiState.Success(
                file = demoFile,
                fileName = demoFile.name,
                sizeBytes = 2_200_000L,
                pageCount = 4,
                operationType = PdfOperationType.SIGN
            )
        }
    }

    suspend fun inspectPdf(uri: Uri): PdfStatusInfo {
        delay(100)
        return PdfStatusInfo(isEncrypted = false, pageCount = 4, errorMessage = null)
    }

    suspend fun renderThumbnail(uri: Uri, pageIndex: Int = 0, maxWidth: Int = 300, maxHeight: Int = 420): Bitmap? {
        val bitmap = Bitmap.createBitmap(maxWidth.coerceAtLeast(100), maxHeight.coerceAtLeast(140), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(8f, 8f, bitmap.width - 8f, bitmap.height - 8f, paint)
        return bitmap
    }

    fun renderThumbnailForFilePath(filePath: String, maxWidth: Int = 120, maxHeight: Int = 160): Bitmap? {
        val bitmap = Bitmap.createBitmap(maxWidth.coerceAtLeast(50), maxHeight.coerceAtLeast(70), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.FILL
        }
        canvas.drawRect(6f, 6f, bitmap.width - 6f, bitmap.height - 6f, paint)
        return bitmap
    }

    private fun addRecent(name: String, path: String, size: Long, pages: Int, op: String) {
        val newRecent = RecentPdf(
            id = System.currentTimeMillis(),
            name = name,
            filePath = path,
            sizeBytes = size,
            pageCount = pages,
            createdAt = System.currentTimeMillis(),
            operationType = op
        )
        val updated = listOf(newRecent) + _allRecentPdfs.value
        _allRecentPdfs.value = updated
        _previewRecentPdfs.value = updated.take(4)
    }

    fun renameRecentPdf(recentPdf: RecentPdf, newName: String) {
        val updated = _allRecentPdfs.value.map {
            if (it.id == recentPdf.id) it.copy(name = newName) else it
        }
        _allRecentPdfs.value = updated
        _previewRecentPdfs.value = updated.take(4)
        _toastMessage.value = "Renamed to $newName"
    }

    fun removeFromRecentOnly(recentPdf: RecentPdf) {
        val updated = _allRecentPdfs.value.filter { it.id != recentPdf.id }
        _allRecentPdfs.value = updated
        _previewRecentPdfs.value = updated.take(4)
        _toastMessage.value = "Removed from recents"
    }

    fun deleteRecentPdf(recentPdf: RecentPdf) {
        removeFromRecentOnly(recentPdf)
    }

    fun clearAllRecents() {
        _allRecentPdfs.value = emptyList()
        _previewRecentPdfs.value = emptyList()
        _toastMessage.value = "All recents cleared"
    }

    fun clearAllRecentPdfs() {
        clearAllRecents()
    }

    fun savePdfToDestinationUri(context: Context? = null, destinationUri: Uri, file: File? = null) {
        viewModelScope.launch {
            _toastMessage.value = "Saved successfully to device."
            _uiState.value = OperationUiState.Idle
        }
    }

    fun savePdfToDestinationUri(destinationUri: Uri) {
        savePdfToDestinationUri(null, destinationUri, null)
    }

    fun saveExportedFile(destinationUri: Uri) {
        savePdfToDestinationUri(null, destinationUri, null)
    }

    fun sharePdf(context: Context, file: File) {
        _toastMessage.value = "Sharing ${file.name}"
    }

    fun shareFile(context: Context, file: File) {
        sharePdf(context, file)
    }

    fun openPdf(context: Context, file: File) {
        _toastMessage.value = "Opening ${file.name}"
    }

    fun openFile(context: Context, file: File) {
        openPdf(context, file)
    }
}
