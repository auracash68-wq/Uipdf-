package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.CompressionPreset
import com.example.model.DocMargin
import com.example.model.DocOrientation
import com.example.model.DocPageSize
import com.example.model.ImageQualityPreset
import com.example.model.PdfProcessResult
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

data class PdfStatusInfo(
    val isEncrypted: Boolean,
    val pageCount: Int,
    val errorMessage: String? = null
)

class PdfProcessor(private val context: Context) {

    /**
     * Inspect PDF to determine encryption status and page count safely.
     */
    suspend fun inspectPdf(uri: Uri): PdfStatusInfo = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        var doc: PDDocument? = null
        try {
            tempFile = FileUtils.copyUriToTempFile(context, uri, "inspect_src")
            try {
                doc = PDDocument.load(tempFile)
                val encrypted = doc.isEncrypted
                val count = doc.numberOfPages
                PdfStatusInfo(isEncrypted = encrypted, pageCount = count)
            } catch (e: InvalidPasswordException) {
                PdfStatusInfo(isEncrypted = true, pageCount = 0)
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("password", ignoreCase = true) ||
                    msg.contains("encrypted", ignoreCase = true) ||
                    msg.contains("crypt", ignoreCase = true)
                ) {
                    PdfStatusInfo(isEncrypted = true, pageCount = 0)
                } else {
                    PdfStatusInfo(isEncrypted = false, pageCount = 0, errorMessage = e.message)
                }
            }
        } catch (e: Exception) {
            PdfStatusInfo(isEncrypted = false, pageCount = 0, errorMessage = e.message)
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            tempFile?.delete()
        }
    }

    /**
     * Merge multiple PDF URIs into a single destination PDF.
     */
    suspend fun mergePdfs(
        uris: List<Uri>,
        outputBaseName: String = "Merged_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        val tempFiles = mutableListOf<File>()
        try {
            if (uris.size < 2) {
                return@withContext PdfProcessResult.Error("Please select at least 2 PDF files to merge.")
            }

            val merger = PDFMergerUtility()
            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            merger.destinationFileName = outputFile.absolutePath

            for (uri in uris) {
                val temp = FileUtils.copyUriToTempFile(context, uri, "merge_src")
                tempFiles.add(temp)
                merger.addSource(temp)
            }

            merger.mergeDocuments(null)

            // Validate result
            if (!outputFile.exists() || outputFile.length() == 0L) {
                return@withContext PdfProcessResult.Error("Failed to merge PDF files.")
            }

            val pageCount = getPdfPageCount(outputFile)
            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = pageCount
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Merge operation failed.")
        } finally {
            tempFiles.forEach { it.delete() }
        }
    }

    /**
     * Split a PDF into page ranges or individual pages.
     */
    suspend fun splitPdf(
        sourceUri: Uri,
        pageRangeString: String,
        outputBaseName: String = "Split_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        var tempSource: File? = null
        var loadedDoc: PDDocument? = null
        var newDoc: PDDocument? = null
        try {
            tempSource = FileUtils.copyUriToTempFile(context, sourceUri, "split_src")
            loadedDoc = PDDocument.load(tempSource)
            val totalPages = loadedDoc.numberOfPages

            val selectedPages = ValidationUtils.parsePageRanges(pageRangeString, totalPages)
            if (selectedPages.isEmpty()) {
                return@withContext PdfProcessResult.Error("No valid pages selected for split.")
            }

            newDoc = PDDocument()
            for (p in selectedPages) {
                // PDFBox 0-indexed page lookup
                val page = loadedDoc.getPage(p - 1)
                newDoc.addPage(page)
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            newDoc.save(outputFile)

            val pageCount = selectedPages.size
            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = pageCount
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to split PDF.")
        } finally {
            try { loadedDoc?.close() } catch (_: Exception) {}
            try { newDoc?.close() } catch (_: Exception) {}
            tempSource?.delete()
        }
    }

    /**
     * Extract specific pages into a new PDF.
     */
    suspend fun extractPages(
        sourceUri: Uri,
        pageNumbers: List<Int>,
        outputBaseName: String = "Extracted_Pages"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        var tempSource: File? = null
        var loadedDoc: PDDocument? = null
        var newDoc: PDDocument? = null
        try {
            if (pageNumbers.isEmpty()) {
                return@withContext PdfProcessResult.Error("No pages selected to extract.")
            }

            tempSource = FileUtils.copyUriToTempFile(context, sourceUri, "extract_src")
            loadedDoc = PDDocument.load(tempSource)
            val totalPages = loadedDoc.numberOfPages

            newDoc = PDDocument()
            for (p in pageNumbers) {
                if (p in 1..totalPages) {
                    newDoc.addPage(loadedDoc.getPage(p - 1))
                }
            }

            if (newDoc.numberOfPages == 0) {
                return@withContext PdfProcessResult.Error("Extracted document has 0 pages.")
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            newDoc.save(outputFile)

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = newDoc.numberOfPages
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to extract pages.")
        } finally {
            try { loadedDoc?.close() } catch (_: Exception) {}
            try { newDoc?.close() } catch (_: Exception) {}
            tempSource?.delete()
        }
    }

    /**
     * Rotate pages of a PDF by 90, 180, or 270 degrees.
     */
    suspend fun rotatePdf(
        sourceUri: Uri,
        degrees: Int,
        targetPages: List<Int>? = null, // null means all pages
        outputBaseName: String = "Rotated_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        var tempSource: File? = null
        var doc: PDDocument? = null
        try {
            tempSource = FileUtils.copyUriToTempFile(context, sourceUri, "rotate_src")
            doc = PDDocument.load(tempSource)
            val totalPages = doc.numberOfPages

            val pagesToRotate = targetPages ?: (1..totalPages).toList()

            for (p in pagesToRotate) {
                if (p in 1..totalPages) {
                    val page = doc.getPage(p - 1)
                    val currentRotation = page.rotation
                    page.rotation = (currentRotation + degrees) % 360
                }
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            doc.save(outputFile)

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = totalPages
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to rotate PDF.")
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            tempSource?.delete()
        }
    }

    /**
     * Convert multiple images to a single formatted PDF document.
     */
    suspend fun imagesToPdf(
        imageUris: List<Uri>,
        pageSize: DocPageSize = DocPageSize.A4,
        orientation: DocOrientation = DocOrientation.PORTRAIT,
        margin: DocMargin = DocMargin.SMALL,
        quality: ImageQualityPreset = ImageQualityPreset.BALANCED,
        outputBaseName: String = "Images_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            return@withContext PdfProcessResult.Error("No images selected.")
        }

        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            var pageIndex = 1
            for (uri in imageUris) {
                val bitmap = decodeBitmapFromUri(uri, maxDimension = 2048)
                    ?: continue

                val isImageLandscape = bitmap.width > bitmap.height

                val isLandscape = when (orientation) {
                    DocOrientation.PORTRAIT -> false
                    DocOrientation.LANDSCAPE -> true
                    DocOrientation.AUTO -> isImageLandscape
                }

                val targetWidth: Int
                val targetHeight: Int

                if (pageSize == DocPageSize.AUTO) {
                    targetWidth = bitmap.width + (margin.paddingPt * 2)
                    targetHeight = bitmap.height + (margin.paddingPt * 2)
                } else {
                    val (stdW, stdH) = if (isLandscape) {
                        pageSize.heightPt to pageSize.widthPt
                    } else {
                        pageSize.widthPt to pageSize.heightPt
                    }
                    targetWidth = stdW
                    targetHeight = stdH
                }

                val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, pageIndex).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawColor(Color.WHITE)

                val availWidth = targetWidth - (margin.paddingPt * 2)
                val availHeight = targetHeight - (margin.paddingPt * 2)

                // Calculate fit aspect ratio
                val scale = min(
                    availWidth.toFloat() / bitmap.width.toFloat(),
                    availHeight.toFloat() / bitmap.height.toFloat()
                )

                val destW = bitmap.width * scale
                val destH = bitmap.height * scale

                val left = margin.paddingPt + (availWidth - destW) / 2f
                val top = margin.paddingPt + (availHeight - destH) / 2f

                val destRect = RectF(left, top, left + destW, top + destH)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(page)
                bitmap.recycle()
                pageIndex++
            }

            if (pageIndex == 1) {
                return@withContext PdfProcessResult.Error("Could not process any of the selected images.")
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = pageIndex - 1
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to create PDF from images.")
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
    }

    /**
     * Create a formatted PDF document from text.
     */
    suspend fun textToPdf(
        title: String,
        bodyText: String,
        fontSizeSp: Float = 14f,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        outputBaseName: String = "Text_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        if (bodyText.isBlank() && title.isBlank()) {
            return@withContext PdfProcessResult.Error("Text content cannot be empty.")
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard pt
        val pageHeight = 842
        val margin = 48
        val contentWidth = pageWidth - (margin * 2)
        val contentHeight = pageHeight - (margin * 2)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 22f
            isFakeBoldText = true
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            textSize = fontSizeSp
        }

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184) // Slate 400
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }

        try {
            // Build StaticLayout for title if present
            val titleLayout = if (title.isNotBlank()) {
                StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, contentWidth)
                    .setAlignment(alignment)
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)
                    .build()
            } else null

            // Build StaticLayout for body text
            val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, bodyPaint, contentWidth)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1.35f)
                .setIncludePad(false)
                .build()

            val totalLines = bodyLayout.lineCount
            var currentLine = 0
            var pageIndex = 1

            while (currentLine < totalLines || (pageIndex == 1 && titleLayout != null)) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(Color.WHITE)

                var yOffset = margin.toFloat()

                // Draw title on first page
                if (pageIndex == 1 && titleLayout != null) {
                    canvas.save()
                    canvas.translate(margin.toFloat(), yOffset)
                    titleLayout.draw(canvas)
                    canvas.restore()
                    yOffset += titleLayout.height + 24f
                }

                // Measure how many lines fit on this page
                val pageAvailableHeight = pageHeight - margin - yOffset - 30f // reserve footer
                val startLineForThisPage = currentLine
                var linesOnThisPage = 0
                var accumulatedHeight = 0f

                while (currentLine < totalLines) {
                    val lineHeight = bodyLayout.getLineBottom(currentLine) - bodyLayout.getLineTop(currentLine)
                    if (accumulatedHeight + lineHeight > pageAvailableHeight && linesOnThisPage > 0) {
                        break
                    }
                    accumulatedHeight += lineHeight
                    linesOnThisPage++
                    currentLine++
                }

                if (linesOnThisPage > 0) {
                    val startOffset = bodyLayout.getLineStart(startLineForThisPage)
                    val endOffset = bodyLayout.getLineEnd(currentLine - 1)
                    val pageTextSegment = bodyText.substring(startOffset, endOffset)

                    val pageSegmentLayout = StaticLayout.Builder.obtain(pageTextSegment, 0, pageTextSegment.length, bodyPaint, contentWidth)
                        .setAlignment(alignment)
                        .setLineSpacing(0f, 1.35f)
                        .setIncludePad(false)
                        .build()

                    canvas.save()
                    canvas.translate(margin.toFloat(), yOffset)
                    pageSegmentLayout.draw(canvas)
                    canvas.restore()
                }

                // Draw Footer page number
                canvas.drawText("Page $pageIndex", (pageWidth / 2).toFloat(), (pageHeight - 24).toFloat(), footerPaint)

                pdfDocument.finishPage(page)
                pageIndex++

                if (currentLine >= totalLines) break
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = pageIndex - 1
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to create PDF from text.")
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
    }

    /**
     * Lock/Encrypt PDF with user-provided password using PDFBox StandardProtectionPolicy.
     */
    suspend fun lockPdf(
        sourceUri: Uri,
        password: String,
        outputBaseName: String = "Locked_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        var tempSource: File? = null
        var doc: PDDocument? = null
        try {
            ValidationUtils.validatePassword(password)
            tempSource = FileUtils.copyUriToTempFile(context, sourceUri, "lock_src")
            doc = PDDocument.load(tempSource)

            val accessPermission = AccessPermission()
            val spp = StandardProtectionPolicy(password, password, accessPermission).apply {
                encryptionKeyLength = 128
                permissions = accessPermission
            }
            doc.protect(spp)

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            doc.save(outputFile)

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = doc.numberOfPages
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to lock PDF.")
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            tempSource?.delete()
        }
    }

    /**
     * Unlock/Decrypt PDF with user-provided password.
     */
    suspend fun unlockPdf(
        sourceUri: Uri,
        password: String,
        outputBaseName: String = "Unlocked_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        var tempSource: File? = null
        var doc: PDDocument? = null
        try {
            if (password.isEmpty()) {
                return@withContext PdfProcessResult.Error("Password cannot be empty.")
            }
            tempSource = FileUtils.copyUriToTempFile(context, sourceUri, "unlock_src")

            try {
                doc = PDDocument.load(tempSource, password)
            } catch (e: InvalidPasswordException) {
                return@withContext PdfProcessResult.Error("Incorrect PDF password. Please verify and try again.")
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("password", ignoreCase = true) ||
                    msg.contains("crypt", ignoreCase = true) ||
                    msg.contains("encrypted", ignoreCase = true)
                ) {
                    return@withContext PdfProcessResult.Error("Incorrect PDF password. Please verify and try again.")
                }
                return@withContext PdfProcessResult.Error("Decryption failed: ${e.localizedMessage ?: "Invalid password or corrupted file."}")
            }

            if (doc.isEncrypted) {
                doc.isAllSecurityToBeRemoved = true
            }

            val pageCount = doc.numberOfPages
            if (pageCount == 0) {
                return@withContext PdfProcessResult.Error("Decrypted document has no pages.")
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            doc.save(outputFile)

            // Close source doc
            try { doc.close() } catch (_: Exception) {}
            doc = null

            // Verify the generated output file can be loaded without a password
            var verifyDoc: PDDocument? = null
            try {
                verifyDoc = PDDocument.load(outputFile)
                if (verifyDoc.isEncrypted) {
                    return@withContext PdfProcessResult.Error("Failed to remove encryption from the document.")
                }
            } catch (e: Exception) {
                return@withContext PdfProcessResult.Error("Verification of unlocked PDF failed: ${e.message}")
            } finally {
                try { verifyDoc?.close() } catch (_: Exception) {}
            }

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = pageCount
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to unlock PDF.")
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            tempSource?.delete()
        }
    }

    /**
     * Compress PDF by downsampling and re-encoding page streams.
     */
    suspend fun compressPdf(
        sourceUri: Uri,
        preset: CompressionPreset = CompressionPreset.BALANCED,
        outputBaseName: String = "Compressed_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        var tempSource: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            tempSource = FileUtils.copyUriToTempFile(context, sourceUri, "compress_src")
            pfd = ParcelFileDescriptor.open(tempSource, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val targetW = (page.width * preset.scaleFactor).toInt().coerceAtLeast(100)
                val targetH = (page.height * preset.scaleFactor).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Re-compress via JPEG byte stream for size reduction
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, preset.jpegQuality, stream)
                val compressedBytes = stream.toByteArray()
                bitmap.recycle()

                val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val canvas = docPage.canvas
                canvas.drawColor(Color.WHITE)

                val destRect = Rect(0, 0, page.width, page.height)
                canvas.drawBitmap(compressedBitmap, null, destRect, paint)
                pdfDocument.finishPage(docPage)
                compressedBitmap.recycle()
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = pageCount
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to compress PDF.")
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            tempSource?.delete()
        }
    }

    /**
     * Add hand-drawn or imported signature to selected pages of the PDF.
     * Preserves original vector fidelity using PDFBox Lossless image placement with fallback.
     */
    suspend fun signPdf(
        sourceUri: Uri,
        signatureBitmap: Bitmap,
        targetPageNumber: Int,
        normX: Float, // Normalized 0..1 position (top-left of signature box)
        normY: Float,
        normWidth: Float,
        normHeight: Float,
        allPages: Boolean = false,
        targetPages: List<Int>? = null,
        outputBaseName: String = "Signed_Document"
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        var tempSource: File? = null
        var doc: PDDocument? = null
        try {
            tempSource = FileUtils.copyUriToTempFile(context, sourceUri, "sign_src")
            doc = PDDocument.load(tempSource)
            val totalPages = doc.numberOfPages

            if (totalPages == 0) {
                return@withContext PdfProcessResult.Error("The document has 0 pages.")
            }

            val pagesToSign = when {
                allPages -> (1..totalPages).toList()
                targetPages != null -> targetPages.filter { it in 1..totalPages }
                else -> listOf(targetPageNumber.coerceIn(1, totalPages))
            }

            val pdImage = LosslessFactory.createFromImage(doc, signatureBitmap)

            for (pageNum in pagesToSign) {
                val pageIndex = pageNum - 1
                if (pageIndex in 0 until totalPages) {
                    val page = doc.getPage(pageIndex)
                    val mediaBox = page.cropBox ?: page.mediaBox
                    val pageWidthPt = mediaBox.width
                    val pageHeightPt = mediaBox.height

                    val sigWidthPt = (normWidth * pageWidthPt).coerceAtLeast(10f)
                    val sigHeightPt = (normHeight * pageHeightPt).coerceAtLeast(10f)
                    val sigXPt = (normX * pageWidthPt).coerceIn(0f, pageWidthPt - sigWidthPt)
                    // Convert screen top-down Y to PDF bottom-up Y coordinate
                    val sigYPt = (pageHeightPt - (normY * pageHeightPt) - sigHeightPt).coerceIn(0f, pageHeightPt - sigHeightPt)

                    val contentStream = PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)
                    contentStream.drawImage(pdImage, sigXPt, sigYPt, sigWidthPt, sigHeightPt)
                    contentStream.close()
                }
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            doc.save(outputFile)

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = totalPages
            )
        } catch (e: Exception) {
            // If PDFBox direct edit encounters issue, use high-res PdfRenderer fallback
            signPdfFallback(tempSource, signatureBitmap, targetPageNumber, normX, normY, normWidth, normHeight, allPages, targetPages, outputBaseName)
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            tempSource?.delete()
        }
    }

    private suspend fun signPdfFallback(
        tempSource: File?,
        signatureBitmap: Bitmap,
        targetPageNumber: Int,
        normX: Float,
        normY: Float,
        normWidth: Float,
        normHeight: Float,
        allPages: Boolean,
        targetPages: List<Int>?,
        outputBaseName: String
    ): PdfProcessResult = withContext(Dispatchers.IO) {
        if (tempSource == null || !tempSource.exists()) {
            return@withContext PdfProcessResult.Error("Source document could not be read.")
        }
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            pfd = ParcelFileDescriptor.open(tempSource, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            val pagesToSign = when {
                allPages -> (1..pageCount).toSet()
                targetPages != null -> targetPages.toSet()
                else -> setOf(targetPageNumber.coerceIn(1, pageCount))
            }

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val w = page.width
                val h = page.height

                val bitmap = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                val docPage = pdfDocument.startPage(pageInfo)
                val canvas = docPage.canvas

                canvas.drawBitmap(bitmap, null, Rect(0, 0, w, h), paint)
                bitmap.recycle()

                if ((i + 1) in pagesToSign) {
                    val sigX = normX * w
                    val sigY = normY * h
                    val sigW = normWidth * w
                    val sigH = normHeight * h
                    val destRect = RectF(sigX, sigY, sigX + sigW, sigY + sigH)
                    canvas.drawBitmap(signatureBitmap, null, destRect, paint)
                }

                pdfDocument.finishPage(docPage)
            }

            val outputFile = FileUtils.createManagedOutputFile(context, outputBaseName)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            PdfProcessResult.Success(
                file = outputFile,
                name = outputFile.name,
                sizeBytes = outputFile.length(),
                pageCount = pageCount
            )
        } catch (e: Exception) {
            PdfProcessResult.Error(e.message ?: "Failed to sign PDF.")
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Helper to get total page count of a PDF file.
     */
    fun getPdfPageCount(file: File): Int {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (_: Exception) {
            1
        }
    }

    /**
     * Render a single page thumbnail for preview in UI.
     */
    suspend fun renderPageThumbnail(uri: Uri, pageIndex: Int = 0, maxWidth: Int = 400): Bitmap? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            tempFile = FileUtils.copyUriToTempFile(context, uri, "thumb")
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex in 0 until renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        val scale = maxWidth.toFloat() / page.width.toFloat()
                        val w = (page.width * scale).toInt().coerceAtLeast(50)
                        val h = (page.height * scale).toInt().coerceAtLeast(50)
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        return@withContext bitmap
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            tempFile?.delete()
        }
    }

    private fun decodeBitmapFromUri(uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            // First decode bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }
}
