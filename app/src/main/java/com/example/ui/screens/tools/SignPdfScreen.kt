package com.example.ui.screens.tools

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.FileUtils
import com.example.ui.OperationUiState
import com.example.ui.PdfViewModel
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.DrawingPath
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ProcessingProgressDialog
import com.example.ui.components.SignaturePad
import com.example.ui.components.SuccessResultDialog
import com.example.ui.components.exportSignatureToBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var targetPage by remember { mutableStateOf(1) }
    var outputName by remember { mutableStateOf("Signed_Document") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val signaturePaths = remember { mutableStateListOf<DrawingPath>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = FileUtils.getFileName(context, uri)
            outputName = selectedFileName.removeSuffix(".pdf") + "_signed"
            errorMessage = null
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ready to sign",
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

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedUri != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DRAW YOUR SIGNATURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (signaturePaths.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { signaturePaths.clear() },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_clear_canvas), fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SignaturePad(
                        paths = signaturePaths,
                        onPathAdded = { errorMessage = null }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Target Page & Placement
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Target Page",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { if (targetPage > 1) targetPage-- },
                                    enabled = targetPage > 1,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "$targetPage",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                                OutlinedButton(
                                    onClick = { targetPage++ },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                            if (signaturePaths.isEmpty()) {
                                errorMessage = "Please draw your signature first."
                                return@PrimaryButton
                            }
                            val bitmap = exportSignatureToBitmap(signaturePaths)
                            if (bitmap == null) {
                                errorMessage = "Failed to export signature."
                                return@PrimaryButton
                            }
                            val uri = selectedUri
                            if (uri != null && context is Activity) {
                                // Default signature placement on bottom right (normX = 0.55, normY = 0.82, normW = 0.38, normH = 0.12)
                                viewModel.signPdf(
                                    context, uri, bitmap, targetPage,
                                    normX = 0.55f, normY = 0.82f, normWidth = 0.38f, normHeight = 0.12f,
                                    outputName = outputName
                                )
                            }
                        },
                        testTag = "sign_action_button"
                    )
                }
            }

            if (errorMessage != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                AdBannerContainer(entitlement = entitlement)
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
