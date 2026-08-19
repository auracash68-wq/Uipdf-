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
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.engine.ValidationUtils
import com.example.ui.OperationUiState
import com.example.ui.PdfViewModel
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ProcessingProgressDialog
import com.example.ui.components.SuccessResultDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotatePagesScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var rotationAngle by remember { mutableStateOf(90) }
    var applyToAll by remember { mutableStateOf(true) }
    var customPageInput by remember { mutableStateOf("1") }
    var outputName by remember { mutableStateOf("Rotated_Document") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = FileUtils.getFileName(context, uri)
            outputName = selectedFileName.removeSuffix(".pdf") + "_rotated"
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
                title = { Text(stringResource(R.string.rotate_title)) },
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
                .testTag("rotate_screen"),
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
                                text = "Choose a PDF file to rotate pages",
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
                                tint = Color(0xFF16A34A),
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
                                    text = "Ready to rotate",
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
                    Text(
                        text = "ROTATION ANGLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rotationAngle = 90 }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = rotationAngle == 90, onClick = { rotationAngle = 90 })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.angle_90), style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rotationAngle = 180 }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = rotationAngle == 180, onClick = { rotationAngle = 180 })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.angle_180), style = MaterialTheme.typography.bodyMedium)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rotationAngle = 270 }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = rotationAngle == 270, onClick = { rotationAngle = 270 })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.angle_270), style = MaterialTheme.typography.bodyMedium)
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
                        text = stringResource(R.string.btn_rotate_action),
                        onClick = {
                            val uri = selectedUri
                            if (uri != null && context is Activity) {
                                viewModel.rotatePdf(context, uri, rotationAngle, null, outputName)
                            }
                        },
                        testTag = "rotate_action_button"
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
