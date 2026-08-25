package com.example.ui.screens.tools

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.FileUtils
import com.example.ui.OperationUiState
import com.example.ui.PdfViewModel
import com.example.ui.components.DrawingPath
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ProcessingProgressDialog
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SignaturePad
import com.example.ui.components.SuccessResultDialog
import com.example.ui.components.ToolGuideAndAdSection
import com.example.ui.components.exportSignatureToBitmap
import com.example.ui.components.processImportedSignature
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SignatureInputMode {
    DRAW,
    UPLOAD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()
    val guideVideosEnabled by viewModel.guideVideosEnabled.collectAsState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var totalPages by remember { mutableIntStateOf(1) }
    var targetPage by remember { mutableIntStateOf(1) }
    var allPages by remember { mutableStateOf(false) }
    var outputName by remember { mutableStateOf("Signed_Document") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Signature mode & assets
    var inputMode by remember { mutableStateOf(SignatureInputMode.DRAW) }
    val signaturePaths = remember { mutableStateListOf<DrawingPath>() }
    var penColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var penWidth by remember { mutableFloatStateOf(6f) }
    var importedSignatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Placement normalized coordinates (0..1)
    var normX by remember { mutableFloatStateOf(0.55f) }
    var normY by remember { mutableFloatStateOf(0.78f) }
    var sigScale by remember { mutableFloatStateOf(0.35f) } // width as fraction of page

    // Page preview bitmap
    var pageThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isThumbnailLoading by remember { mutableStateOf(false) }

    // Document Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = FileUtils.getFileName(context, uri)
            outputName = selectedFileName.removeSuffix(".pdf") + "_signed"
            errorMessage = null
            targetPage = 1
            coroutineScope.launch {
                val info = viewModel.inspectPdf(uri)
                totalPages = if (info.pageCount > 0) info.pageCount else 1
                isThumbnailLoading = true
                pageThumbnail = viewModel.renderThumbnail(uri, 0, 500)
                isThumbnailLoading = false
            }
        }
    }

    // Signature Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { imgUri: Uri? ->
        if (imgUri != null) {
            coroutineScope.launch {
                try {
                    val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imgUri)) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, imgUri)
                    }
                    importedSignatureBitmap = processImportedSignature(rawBitmap)
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Failed to load signature image: ${e.message}"
                }
            }
        }
    }

    // Refresh thumbnail when page changes
    LaunchedEffect(targetPage, selectedUri) {
        val uri = selectedUri
        if (uri != null) {
            isThumbnailLoading = true
            pageThumbnail = viewModel.renderThumbnail(uri, (targetPage - 1).coerceAtLeast(0), 500)
            isThumbnailLoading = false
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri: Uri? ->
        if (destUri != null) {
            viewModel.savePdfToDestinationUri(context, destUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sign_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.resetState(); onBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("sign_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // PDF File Card
            item {
                if (selectedUri == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.btn_select_pdf),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose a PDF document to sign",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedFileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$totalPages page(s) • Ready to sign",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF16A34A)
                                )
                            }
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Change", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (selectedUri != null) {
                // Signature Input Section
                item {
                    Text(
                        text = "SIGNATURE SOURCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TabRow(
                        selectedTabIndex = inputMode.ordinal,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        Tab(
                            selected = inputMode == SignatureInputMode.DRAW,
                            onClick = { inputMode = SignatureInputMode.DRAW },
                            text = { Text("Draw Signature", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = inputMode == SignatureInputMode.UPLOAD,
                            onClick = { inputMode = SignatureInputMode.UPLOAD },
                            text = { Text("Upload Stamp", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // DRAW MODE
                if (inputMode == SignatureInputMode.DRAW) {
                    item {
                        // Drawing Controls (Color + Clear/Undo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color choices
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val colors = listOf(
                                    Color(0xFF0F172A) to "Black",
                                    Color(0xFF1D4ED8) to "Blue",
                                    Color(0xFFB91C1C) to "Red"
                                )
                                colors.forEach { (color, label) ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (penColor == color) 3.dp else 1.dp,
                                                color = if (penColor == color) MaterialTheme.colorScheme.primary else Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable { penColor = color },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (penColor == color) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = label,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Actions (Undo, Clear)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (signaturePaths.isNotEmpty()) {
                                    IconButton(
                                        onClick = { if (signaturePaths.isNotEmpty()) signaturePaths.removeAt(signaturePaths.lastIndex) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { signaturePaths.clear() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        SignaturePad(
                            paths = signaturePaths,
                            strokeColor = penColor,
                            strokeWidth = penWidth,
                            onPathAdded = { errorMessage = null }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // UPLOAD MODE
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (importedSignatureBitmap != null) {
                                    Image(
                                        bitmap = importedSignatureBitmap!!.asImageBitmap(),
                                        contentDescription = "Imported Signature",
                                        modifier = Modifier
                                            .height(80.dp)
                                            .padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Signature Stamp Loaded (Tap to change)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Pick Signature Image",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Auto-removes white background",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Interactive Document Preview & Positioning Section
                item {
                    Text(
                        text = "SIGNATURE PLACEMENT & PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Target Page Selector bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (targetPage > 1) targetPage-- },
                                        enabled = targetPage > 1,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Page")
                                    }
                                    Text(
                                        text = "Page $targetPage of $totalPages",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { if (targetPage < totalPages) targetPage++ },
                                        enabled = targetPage < totalPages,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = allPages,
                                        onCheckedChange = { allPages = it }
                                    )
                                    Text(
                                        text = "All Pages",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Interactive Page Preview Box
                            val activeSignatureBitmap = remember(inputMode, signaturePaths.size, importedSignatureBitmap) {
                                if (inputMode == SignatureInputMode.DRAW) {
                                    exportSignatureToBitmap(signaturePaths)
                                } else {
                                    importedSignatureBitmap
                                }
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                val containerWidth = maxWidth
                                val containerHeight = maxHeight

                                if (isThumbnailLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                } else if (pageThumbnail != null) {
                                    Image(
                                        bitmap = pageThumbnail!!.asImageBitmap(),
                                        contentDescription = "Page Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Page $targetPage", color = Color.Gray)
                                    }
                                }

                                // Draggable signature overlay
                                val sigBoxWidth = containerWidth * sigScale
                                val sigBoxHeight = sigBoxWidth * 0.45f

                                val dragOffsetX = (normX * (containerWidth.value - sigBoxWidth.value)).coerceIn(0f, containerWidth.value - sigBoxWidth.value)
                                val dragOffsetY = (normY * (containerHeight.value - sigBoxHeight.value)).coerceIn(0f, containerHeight.value - sigBoxHeight.value)

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset { IntOffset(dragOffsetX.dp.roundToPx(), dragOffsetY.dp.roundToPx()) }
                                        .size(width = sigBoxWidth, height = sigBoxHeight)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                                        .pointerInput(containerWidth, containerHeight, sigBoxWidth, sigBoxHeight) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val maxAvailableX = (containerWidth.toPx() - sigBoxWidth.toPx()).coerceAtLeast(1f)
                                                val maxAvailableY = (containerHeight.toPx() - sigBoxHeight.toPx()).coerceAtLeast(1f)

                                                val newX = (normX * maxAvailableX + dragAmount.x).coerceIn(0f, maxAvailableX)
                                                val newY = (normY * maxAvailableY + dragAmount.y).coerceIn(0f, maxAvailableY)

                                                normX = newX / maxAvailableX
                                                normY = newY / maxAvailableY
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activeSignatureBitmap != null) {
                                        Image(
                                            bitmap = activeSignatureBitmap.asImageBitmap(),
                                            contentDescription = "Signature Placement",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Drag to Place", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Position Quick Presets
                            Text(
                                text = "Position Presets",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = normX > 0.4f && normY > 0.6f,
                                    onClick = { normX = 0.58f; normY = 0.82f },
                                    label = { Text("Bottom-Right", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = normX < 0.3f && normY > 0.6f,
                                    onClick = { normX = 0.08f; normY = 0.82f },
                                    label = { Text("Bottom-Left", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = normX in 0.3f..0.5f && normY > 0.6f,
                                    onClick = { normX = 0.32f; normY = 0.82f },
                                    label = { Text("Center", fontSize = 11.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Size Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Size", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                Slider(
                                    value = sigScale,
                                    onValueChange = { sigScale = it },
                                    valueRange = 0.20f..0.60f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                )
                                Text("${(sigScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Output Name & Action
                item {
                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
                        label = { Text("Output PDF Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PrimaryButton(
                        text = stringResource(R.string.btn_sign_action),
                        onClick = {
                            val activeBitmap = if (inputMode == SignatureInputMode.DRAW) {
                                exportSignatureToBitmap(signaturePaths)
                            } else {
                                importedSignatureBitmap
                            }

                            if (activeBitmap == null) {
                                errorMessage = if (inputMode == SignatureInputMode.DRAW) {
                                    "Please draw your signature first."
                                } else {
                                    "Please select or upload a signature image first."
                                }
                                return@PrimaryButton
                            }

                            val uri = selectedUri
                            if (uri != null && context is Activity) {
                                viewModel.signPdf(
                                    activity = context,
                                    sourceUri = uri,
                                    signatureBitmap = activeBitmap,
                                    targetPageNumber = targetPage,
                                    normX = normX,
                                    normY = normY,
                                    normWidth = sigScale,
                                    normHeight = sigScale * 0.45f,
                                    allPages = allPages,
                                    outputName = outputName
                                )
                            }
                        },
                        testTag = "sign_action_button"
                    )
                }
            }

            val currentError = errorMessage ?: (uiState as? OperationUiState.Error)?.message
            if (currentError != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }

            // Video Guide & Large Ad Section (Bottom UX Priority)
            item {
                ToolGuideAndAdSection(
                    toolKey = "sign",
                    entitlement = entitlement,
                    guideVideosEnabled = guideVideosEnabled
                )
            }
        }
    }

    // Dialogs
    when (val state = uiState) {
        is OperationUiState.Processing -> {
            ProcessingProgressDialog(statusText = state.message)
        }
        is OperationUiState.Success -> {
            SuccessResultDialog(
                fileName = state.fileName,
                fileSizeFormatted = FileUtils.formatFileSize(state.sizeBytes),
                pageCount = state.pageCount,
                onSaveToDevice = {
                    createDocLauncher.launch(state.fileName)
                },
                onShare = {
                    viewModel.sharePdf(context, state.file)
                },
                onOpen = {
                    viewModel.openPdf(context, state.file)
                },
                onDismiss = {
                    viewModel.resetState()
                    onBack()
                }
            )
        }
        is OperationUiState.Error -> {
            errorMessage = state.message
        }
        OperationUiState.Idle -> {}
    }
}
