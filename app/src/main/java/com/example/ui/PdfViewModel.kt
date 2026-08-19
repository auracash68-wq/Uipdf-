package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PdfApplication
import com.example.data.AdManager
import com.example.data.AppThemeMode
import com.example.data.BillingManager
import com.example.data.RecentPdfRepository
import com.example.data.SettingsRepository
import com.example.engine.FileUtils
import com.example.engine.PdfProcessor
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PdfApplication
    val recentRepository: RecentPdfRepository = app.recentPdfRepository
    val billingManager: BillingManager = app.billingManager
    val adManager: AdManager = app.adManager
    val settingsRepository: SettingsRepository = app.settingsRepository
    val pdfProcessor: PdfProcessor = app.pdfProcessor

    val entitlement: StateFlow<UserEntitlement> = billingManager.entitlement
    val themeMode: StateFlow<AppThemeMode> = settingsRepository.themeMode
    val isFirstLaunch: StateFlow<Boolean> = settingsRepository.isFirstLaunch

    val allRecentPdfs: StateFlow<List<RecentPdf>> = recentRepository.allRecentPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previewRecentPdfs: StateFlow<List<RecentPdf>> = recentRepository.getRecentPdfsLimited(4)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<OperationUiState>(OperationUiState.Idle)
    val uiState: StateFlow<OperationUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // File pending to be saved via SAF ACTION_CREATE_DOCUMENT
    var pendingExportFile: File? = null

    fun resetState() {
        _uiState.value = OperationUiState.Idle
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun completeOnboarding() {
        settingsRepository.completeFirstLaunch()
    }

    fun setTheme(mode: AppThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun buyPremium(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    // --- PDF Operations ---

    fun mergePdfs(activity: Activity, fileItems: List<SelectedFileItem>, outputName: String) {
        if (fileItems.size < 2) {
            _uiState.value = OperationUiState.Error("Please select at least 2 PDF files.")
            return
        }

        _uiState.value = OperationUiState.Processing("Merging PDF files...")
        viewModelScope.launch {
            val uris = fileItems.map { it.uri }
            val result = pdfProcessor.mergePdfs(uris, outputName.ifBlank { "Merged_Document" })
            handleResult(activity, result, PdfOperationType.MERGE)
        }
    }

    fun splitPdf(activity: Activity, sourceUri: Uri, pageRange: String, outputName: String) {
        _uiState.value = OperationUiState.Processing("Splitting PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.splitPdf(sourceUri, pageRange, outputName.ifBlank { "Split_Document" })
            handleResult(activity, result, PdfOperationType.SPLIT)
        }
    }

    fun extractPages(activity: Activity, sourceUri: Uri, pageNumbers: List<Int>, outputName: String) {
        _uiState.value = OperationUiState.Processing("Extracting pages...")
        viewModelScope.launch {
            val result = pdfProcessor.extractPages(sourceUri, pageNumbers, outputName.ifBlank { "Extracted_Pages" })
            handleResult(activity, result, PdfOperationType.EXTRACT)
        }
    }

    fun rotatePdf(activity: Activity, sourceUri: Uri, degrees: Int, targetPages: List<Int>?, outputName: String) {
        _uiState.value = OperationUiState.Processing("Rotating PDF...")
        viewModelScope.launch {
            val result = pdfProcessor.rotatePdf(sourceUri, degrees, targetPages, outputName.ifBlank { "Rotated_Document" })
            handleResult(activity, result, PdfOperationType.ROTATE)
        }
    }

    fun imagesToPdf(
        activity: Activity,
        imageUris: List<Uri>,
        pageSize: DocPageSize,
        orientation: DocOrientation,
        margin: DocMargin,
        quality: ImageQualityPreset,
        outputName: String
    ) {
        _uiState.value = OperationUiState.Processing("Creating PDF from images...")
        viewModelScope.launch {
            val result = pdfProcessor.imagesToPdf(imageUris, pageSize, orientation, margin, quality, outputName.ifBlank { "Images_Document" })
            handleResult(activity, result, PdfOperationType.IMAGE_TO_PDF)
        }
    }

    fun textToPdf(
        activity: Activity,
        title: String,
        bodyText: String,
        fontSizeSp: Float,
        outputName: String
    ) {
        _uiState.value = OperationUiState.Processing("Generating PDF from text...")
        viewModelScope.launch {
            val result = pdfProcessor.textToPdf(title, bodyText, fontSizeSp, outputBaseName = outputName.ifBlank { "Text_Document" })
            handleResult(activity, result, PdfOperationType.TEXT_TO_PDF)
        }
    }

    fun lockPdf(activity: Activity, sourceUri: Uri, password: String, outputName: String) {
        _uiState.value = OperationUiState.Processing("Encrypting PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.lockPdf(sourceUri, password, outputName.ifBlank { "Locked_Document" })
            handleResult(activity, result, PdfOperationType.LOCK)
        }
    }

    fun unlockPdf(activity: Activity, sourceUri: Uri, password: String, outputName: String) {
        _uiState.value = OperationUiState.Processing("Decrypting PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.unlockPdf(sourceUri, password, outputName.ifBlank { "Unlocked_Document" })
            handleResult(activity, result, PdfOperationType.UNLOCK)
        }
    }

    fun compressPdf(activity: Activity, sourceUri: Uri, preset: CompressionPreset, outputName: String) {
        _uiState.value = OperationUiState.Processing("Compressing PDF document...")
        viewModelScope.launch {
            val result = pdfProcessor.compressPdf(sourceUri, preset, outputName.ifBlank { "Compressed_Document" })
            handleResult(activity, result, PdfOperationType.COMPRESS)
        }
    }

    fun signPdf(
        activity: Activity,
        sourceUri: Uri,
        signatureBitmap: Bitmap,
        targetPageNumber: Int,
        normX: Float,
        normY: Float,
        normWidth: Float,
        normHeight: Float,
        outputName: String
    ) {
        _uiState.value = OperationUiState.Processing("Applying signature to document...")
        viewModelScope.launch {
            val result = pdfProcessor.signPdf(
                sourceUri, signatureBitmap, targetPageNumber,
                normX, normY, normWidth, normHeight,
                outputName.ifBlank { "Signed_Document" }
            )
            handleResult(activity, result, PdfOperationType.SIGN)
        }
    }

    private fun handleResult(
        activity: Activity,
        result: PdfProcessResult,
        operationType: PdfOperationType
    ) {
        when (result) {
            is PdfProcessResult.Success -> {
                viewModelScope.launch {
                    recentRepository.addRecentPdf(
                        name = result.name,
                        file = result.file,
                        pageCount = result.pageCount,
                        operationType = operationType.displayName
                    )
                }

                pendingExportFile = result.file

                // Check ad eligibility and frequency capping on natural operation completion
                adManager.onOperationCompleted(activity) {
                    _uiState.value = OperationUiState.Success(
                        file = result.file,
                        fileName = result.name,
                        sizeBytes = result.sizeBytes,
                        pageCount = result.pageCount,
                        operationType = operationType
                    )
                }
            }
            is PdfProcessResult.Error -> {
                _uiState.value = OperationUiState.Error(result.message)
            }
            PdfProcessResult.Cancelled -> {
                _uiState.value = OperationUiState.Idle
            }
        }
    }

    // --- File Export & Actions ---

    fun savePdfToDestinationUri(context: Context, destUri: Uri) {
        val source = pendingExportFile ?: return
        viewModelScope.launch {
            try {
                FileUtils.exportPdfToUri(context, source, destUri)
                Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
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
            Toast.makeText(context, "Unable to share file.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdf(context: Context, file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_LONG).show()
        }
    }

    fun deleteRecentPdf(recentPdf: RecentPdf) {
        viewModelScope.launch {
            recentRepository.deleteRecentPdf(recentPdf.id, recentPdf.filePath)
        }
    }

    fun renameRecentPdf(recentPdf: RecentPdf, newName: String) {
        if (newName.isNotBlank()) {
            viewModelScope.launch {
                recentRepository.renameRecentPdf(recentPdf.id, newName)
            }
        }
    }

    fun clearAllRecents() {
        viewModelScope.launch {
            recentRepository.clearAll()
        }
    }
}
