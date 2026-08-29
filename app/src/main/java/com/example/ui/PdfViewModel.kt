package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.Layout
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdManager
import com.example.data.AppThemeMode
import com.example.data.RecentPdfRepository
import com.example.data.SettingsRepository
import com.example.data.db.AppDatabase
import com.example.engine.FileUtils
import com.example.engine.PdfProcessor
import com.example.engine.PdfStatusInfo
import com.example.model.CompressionPreset
import com.example.model.DocMargin
import com.example.model.DocOrientation
import com.example.model.DocPageSize
import com.example.model.ImageQualityPreset
import com.example.model.PdfOperationType
import com.example.model.PdfProcessResult
import com.example.model.RecentPdf
import com.example.model.SelectedFileItem
import com.example.model.UserEntitlement
import com.example.ui.theme.AppColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * Production-ready ViewModel connecting Jetpack Compose UI to the local on-device
 * PDF Processing engine and Room Database for history persistence.
 */
class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val pdfProcessor = PdfProcessor(application)
    private val recentPdfRepository: RecentPdfRepository
    private val settingsRepository = SettingsRepository(application)

    private val _entitlement = MutableStateFlow(UserEntitlement.FREE)
    val entitlement: StateFlow<UserEntitlement> = _entitlement.asStateFlow()

    val themeMode: StateFlow<AppThemeMode> = settingsRepository.themeMode
    val colorTheme: StateFlow<AppColorTheme> = settingsRepository.colorTheme
    val isFirstLaunch: StateFlow<Boolean> = settingsRepository.isFirstLaunch

    val allRecentPdfs: StateFlow<List<RecentPdf>>
    val previewRecentPdfs: StateFlow<List<RecentPdf>>

    private val _uiState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val uiState: StateFlow<OperationUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    var pendingExportFile: File? = null

    init {
        val database = AppDatabase.getDatabase(application)
        recentPdfRepository = RecentPdfRepository(database.pdfDao())

        allRecentPdfs = recentPdfRepository.allRecentPdfs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        previewRecentPdfs = recentPdfRepository.getRecentPdfsLimited(4)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun resetState() {
        _uiState.value = OperationUiState.Idle
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun completeOnboarding() {
        settingsRepository.completeFirstLaunch()
    }

    fun completeFirstLaunch() {
        settingsRepository.completeFirstLaunch()
    }

    fun setTheme(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun setColorTheme(theme: AppColorTheme) {
        settingsRepository.setColorTheme(theme)
    }

    fun setEntitlement(entitlement: UserEntitlement) {
        _entitlement.value = entitlement
    }

    fun isOperationAllowed(context: Context): Boolean = true

    fun buyPremium(activity: Activity?) {
        _entitlement.value = UserEntitlement.PREMIUM
        _toastMessage.value = "Premium Plan Activated!"
    }

    // =========================================================================
    // 10 PDF TOOLS - ON-DEVICE BACKEND EXECUTION
    // =========================================================================

    /**
     * 1. Merge Multiple PDFs
     */
    fun mergePdfs(activity: Activity? = null, fileItems: List<SelectedFileItem>, outputName: String) {
        if (fileItems.size < 2) {
            _uiState.value = OperationUiState.Error("Please select at least 2 PDF files.")
            return
        }

        _uiState.value = OperationUiState.Processing("Merging ${fileItems.size} PDF files...")
        viewModelScope.launch {
            val uris = fileItems.map { it.uri }
            val result = pdfProcessor.mergePdfs(uris, outputName.ifBlank { "Merged_Document" })
            handleOperationResult(result, PdfOperationType.MERGE, activity)
        }
    }

    /**
     * 2. Split PDF Document
     */
    fun splitPdf(activity: Activity? = null, sourceUri: Uri, pageRange: String, outputName: String) {
        if (pageRange.isBlank()) {
            _uiState.value = OperationUiState.Error("Please enter valid page numbers or ranges (e.g. 1-3, 5).")
            return
        }

        _uiState.value = OperationUiState.Processing("Splitting PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.splitPdf(sourceUri, pageRange, outputName.ifBlank { "Split_Document" })
            handleOperationResult(result, PdfOperationType.SPLIT, activity)
        }
    }

    /**
     * 3. Extract Specific Pages from PDF
     */
    fun extractPages(activity: Activity? = null, sourceUri: Uri, pageNumbers: List<Int>, outputName: String) {
        if (pageNumbers.isEmpty()) {
            _uiState.value = OperationUiState.Error("Please select at least one page to extract.")
            return
        }

        _uiState.value = OperationUiState.Processing("Extracting ${pageNumbers.size} pages...")
        viewModelScope.launch {
            val result = pdfProcessor.extractPages(sourceUri, pageNumbers, outputName.ifBlank { "Extracted_Pages" })
            handleOperationResult(result, PdfOperationType.EXTRACT, activity)
        }
    }

    /**
     * 4. Rotate PDF Pages
     */
    fun rotatePdf(
        activity: Activity? = null,
        sourceUri: Uri,
        degrees: Int,
        targetPages: List<Int>?,
        outputName: String
    ) {
        _uiState.value = OperationUiState.Processing("Rotating PDF document by $degrees°...")
        viewModelScope.launch {
            val result = pdfProcessor.rotatePdf(sourceUri, degrees, targetPages, outputName.ifBlank { "Rotated_Document" })
            handleOperationResult(result, PdfOperationType.ROTATE, activity)
        }
    }

    /**
     * 5. Convert Images to PDF
     */
    fun imagesToPdf(
        activity: Activity? = null,
        imageUris: List<Uri>,
        pageSize: DocPageSize,
        orientation: DocOrientation,
        margin: DocMargin,
        quality: ImageQualityPreset,
        outputName: String
    ) {
        if (imageUris.isEmpty()) {
            _uiState.value = OperationUiState.Error("Please select at least one image.")
            return
        }

        _uiState.value = OperationUiState.Processing("Converting ${imageUris.size} images to PDF...")
        viewModelScope.launch {
            val result = pdfProcessor.imagesToPdf(
                imageUris = imageUris,
                pageSize = pageSize,
                orientation = orientation,
                margin = margin,
                quality = quality,
                outputBaseName = outputName.ifBlank { "Photos_Document" }
            )
            handleOperationResult(result, PdfOperationType.IMAGE_TO_PDF, activity)
        }
    }

    /**
     * 6. Convert Text Notes to PDF
     */
    fun textToPdf(
        activity: Activity? = null,
        title: String,
        bodyText: String,
        fontSizeSp: Float,
        outputName: String
    ) {
        if (bodyText.isBlank() && title.isBlank()) {
            _uiState.value = OperationUiState.Error("Please enter some text or title to generate PDF.")
            return
        }

        _uiState.value = OperationUiState.Processing("Generating PDF from text...")
        viewModelScope.launch {
            val result = pdfProcessor.textToPdf(
                title = title,
                bodyText = bodyText,
                fontSizeSp = fontSizeSp,
                alignment = Layout.Alignment.ALIGN_NORMAL,
                outputBaseName = outputName.ifBlank { "Notes_Document" }
            )
            handleOperationResult(result, PdfOperationType.TEXT_TO_PDF, activity)
        }
    }

    /**
     * 7. Password Protect / Encrypt PDF
     */
    fun lockPdf(activity: Activity? = null, sourceUri: Uri, password: String, outputName: String) {
        if (password.isBlank()) {
            _uiState.value = OperationUiState.Error("Password cannot be empty.")
            return
        }

        _uiState.value = OperationUiState.Processing("Encrypting PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.lockPdf(sourceUri, password, outputName.ifBlank { "Locked_Document" })
            handleOperationResult(result, PdfOperationType.LOCK, activity)
        }
    }

    /**
     * 8. Unlock / Decrypt Password-Protected PDF
     */
    fun unlockPdf(activity: Activity? = null, sourceUri: Uri, password: String, outputName: String) {
        if (password.isBlank()) {
            _uiState.value = OperationUiState.Error("Password cannot be empty.")
            return
        }

        _uiState.value = OperationUiState.Processing("Decrypting PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.unlockPdf(sourceUri, password, outputName.ifBlank { "Unlocked_Document" })
            handleOperationResult(result, PdfOperationType.UNLOCK, activity)
        }
    }

    /**
     * 9. Compress / Optimize PDF Size
     */
    fun compressPdf(activity: Activity? = null, sourceUri: Uri, preset: CompressionPreset, outputName: String) {
        _uiState.value = OperationUiState.Processing("Compressing PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.compressPdf(sourceUri, preset, outputName.ifBlank { "Compressed_Document" })
            handleOperationResult(result, PdfOperationType.COMPRESS, activity)
        }
    }

    /**
     * 10. Electronically Sign PDF Document
     */
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
            val result = pdfProcessor.signPdf(
                sourceUri = sourceUri,
                signatureBitmap = signatureBitmap,
                targetPageNumber = targetPageNumber,
                normX = normX,
                normY = normY,
                normWidth = normWidth,
                normHeight = normHeight,
                allPages = allPages,
                targetPages = null,
                outputBaseName = outputName.ifBlank { "Signed_Document" }
            )
            handleOperationResult(result, PdfOperationType.SIGN, activity)
        }
    }

    private suspend fun handleOperationResult(
        result: PdfProcessResult,
        operationType: PdfOperationType,
        activity: Activity? = null
    ) {
        when (result) {
            is PdfProcessResult.Success -> {
                pendingExportFile = result.file
                // Record in Room Database
                recentPdfRepository.addRecentPdf(
                    name = result.name,
                    file = result.file,
                    pageCount = result.pageCount,
                    operationType = operationType.name
                )
                _uiState.value = OperationUiState.Success(
                    file = result.file,
                    fileName = result.name,
                    sizeBytes = result.sizeBytes,
                    pageCount = result.pageCount,
                    operationType = operationType
                )
                // Evaluate eligible interstitial without blocking completion
                activity?.let { act ->
                    AdManager.getInstance(getApplication()).onOperationCompleted(act) {}
                }
            }
            is PdfProcessResult.Error -> {
                _uiState.value = OperationUiState.Error(result.message)
            }
            is PdfProcessResult.Cancelled -> {
                _uiState.value = OperationUiState.Idle
            }
        }
    }

    // =========================================================================
    // HELPER & PREVIEW UTILITIES
    // =========================================================================

    suspend fun inspectPdf(uri: Uri): PdfStatusInfo {
        return pdfProcessor.inspectPdf(uri)
    }

    suspend fun renderThumbnail(uri: Uri, pageIndex: Int = 0, maxWidth: Int = 300, maxHeight: Int = 420): Bitmap? {
        return pdfProcessor.renderPageThumbnail(uri, pageIndex, maxWidth)
    }

    suspend fun renderThumbnailForFilePath(filePath: String, maxWidth: Int = 160, maxHeight: Int = 200): Bitmap? {
        return pdfProcessor.renderThumbnailForFilePath(filePath, 0, maxWidth)
    }

    // =========================================================================
    // RECENT / HISTORY ROOM DATABASE OPERATIONS
    // =========================================================================

    fun renameRecentPdf(recentPdf: RecentPdf, newName: String) {
        viewModelScope.launch {
            recentPdfRepository.renameRecentPdf(recentPdf.id, newName)
            _toastMessage.value = "Renamed to $newName"
        }
    }

    fun removeFromRecentOnly(recentPdf: RecentPdf) {
        viewModelScope.launch {
            recentPdfRepository.removeRecentHistoryOnly(recentPdf.id)
            _toastMessage.value = "Removed from history."
        }
    }

    fun deleteRecentPdf(recentPdf: RecentPdf) {
        viewModelScope.launch {
            recentPdfRepository.deleteRecentPdf(recentPdf.id, recentPdf.filePath)
            _toastMessage.value = "File deleted permanently."
        }
    }

    fun clearAllRecents() {
        viewModelScope.launch {
            recentPdfRepository.clearAll()
            // Clean temp outputs
            FileUtils.clearCacheDirectory(getApplication())
            _toastMessage.value = "All recents cleared."
        }
    }

    fun clearAllRecentPdfs() {
        clearAllRecents()
    }

    // =========================================================================
    // FILE EXPORT, SHARING, AND OPENING
    // =========================================================================

    fun savePdfToDestinationUri(context: Context? = null, destinationUri: Uri, file: File? = null) {
        val targetFile = file ?: pendingExportFile
        if (targetFile == null || !targetFile.exists()) {
            _toastMessage.value = "Source document unavailable to export."
            return
        }

        val appContext = context ?: getApplication()
        viewModelScope.launch(Dispatchers.IO) {
            val success = FileUtils.exportPdfToUri(appContext, targetFile, destinationUri)
            withContext(Dispatchers.Main) {
                if (success) {
                    _toastMessage.value = "PDF saved successfully to device."
                    _uiState.value = OperationUiState.Idle
                } else {
                    _toastMessage.value = "Failed to save PDF to selected location."
                }
            }
        }
    }

    fun savePdfToDestinationUri(destinationUri: Uri) {
        savePdfToDestinationUri(null, destinationUri, null)
    }

    fun saveExportedFile(destinationUri: Uri) {
        savePdfToDestinationUri(null, destinationUri, null)
    }

    fun sharePdf(context: Context, file: File) {
        if (!file.exists()) {
            _toastMessage.value = "File not found on storage."
            return
        }
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
        } catch (e: Exception) {
            _toastMessage.value = "Unable to share: ${e.message}"
        }
    }

    fun shareFile(context: Context, file: File) {
        sharePdf(context, file)
    }

    fun openPdf(context: Context, file: File) {
        if (!file.exists()) {
            _toastMessage.value = "File not found on storage."
            return
        }
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            _toastMessage.value = "No PDF viewer app found on device."
        } catch (e: Exception) {
            _toastMessage.value = "Unable to open document: ${e.message}"
        }
    }

    fun openFile(context: Context, file: File) {
        openPdf(context, file)
    }
}
