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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MergeType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.FileUtils
import com.example.model.SelectedFileItem
import com.example.ui.OperationUiState
import com.example.ui.PdfViewModel
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ProcessingProgressDialog
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SuccessResultDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()

    val selectedFiles = remember { mutableStateListOf<SelectedFileItem>() }
    var outputFileName by remember { mutableStateOf("Merged_Document") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (uris != null && uris.isNotEmpty()) {
            errorMessage = null
            uris.forEach { uri ->
                val name = FileUtils.getFileName(context, uri)
                val size = FileUtils.getFileSize(context, uri)
                selectedFiles.add(SelectedFileItem(uri = uri, name = name, sizeBytes = size))
            }
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
                title = { Text(stringResource(R.string.merge_title)) },
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
                .testTag("merge_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // Instructions
            item {
                Text(
                    text = stringResource(R.string.merge_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Add Files Button
            item {
                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedFiles.isEmpty()) stringResource(R.string.btn_select_pdfs) else stringResource(R.string.btn_add_more),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // File items
            if (selectedFiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                imageVector = Icons.Default.MergeType,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select at least 2 PDF files to merge",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "SELECTED FILES (${selectedFiles.size}) - DRAG / REORDER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(selectedFiles) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FileUtils.formatFileSize(item.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Move Up
                            if (index > 0) {
                                IconButton(onClick = {
                                    val temp = selectedFiles[index]
                                    selectedFiles[index] = selectedFiles[index - 1]
                                    selectedFiles[index - 1] = temp
                                }) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                                }
                            }

                            // Move Down
                            if (index < selectedFiles.size - 1) {
                                IconButton(onClick = {
                                    val temp = selectedFiles[index]
                                    selectedFiles[index] = selectedFiles[index + 1]
                                    selectedFiles[index + 1] = temp
                                }) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                                }
                            }

                            // Remove
                            IconButton(onClick = { selectedFiles.removeAt(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Output Name & Action
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = outputFileName,
                        onValueChange = { outputFileName = it },
                        label = { Text("Output PDF Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = stringResource(R.string.btn_merge_action),
                        enabled = selectedFiles.size >= 2,
                        onClick = {
                            if (context is Activity) {
                                viewModel.mergePdfs(context, selectedFiles.toList(), outputFileName)
                            }
                        },
                        testTag = "merge_action_button"
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
                Spacer(modifier = Modifier.height(20.dp))
                AdBannerContainer(entitlement = entitlement)
            }
        }
    }

    // Processing Dialog
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
