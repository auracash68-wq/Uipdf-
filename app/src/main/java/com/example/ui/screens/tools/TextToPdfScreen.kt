package com.example.ui.screens.tools

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ProcessingProgressDialog
import com.example.ui.components.SuccessResultDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToPdfScreen(
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()

    var titleText by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableStateOf(14f) }
    var outputName by remember { mutableStateOf("Notes_Document") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                title = { Text(stringResource(R.string.text_to_pdf_title)) },
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
                .testTag("text_to_pdf_screen"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(stringResource(R.string.doc_title_label)) },
                    placeholder = { Text(stringResource(R.string.doc_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bodyText,
                    onValueChange = { bodyText = it; errorMessage = null },
                    label = { Text(stringResource(R.string.doc_content_label)) },
                    placeholder = { Text(stringResource(R.string.doc_content_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Typography size control
            item {
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
                            text = "Font Size",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "${fontSizeSp.toInt()} sp",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    fontSizeSp = when (fontSizeSp.toInt()) {
                                        12 -> 14f
                                        14 -> 16f
                                        16 -> 18f
                                        else -> 12f
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
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

                Spacer(modifier = Modifier.height(18.dp))

                PrimaryButton(
                    text = stringResource(R.string.btn_create_pdf),
                    enabled = bodyText.isNotBlank() || titleText.isNotBlank(),
                    onClick = {
                        if (context is Activity) {
                            viewModel.textToPdf(context, titleText, bodyText, fontSizeSp, outputName)
                        }
                    },
                    testTag = "create_text_pdf_button"
                )
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
