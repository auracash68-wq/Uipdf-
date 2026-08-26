package com.example.ui.screens

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.FileUtils
import com.example.model.RecentPdf
import com.example.ui.PdfViewModel
import com.example.ui.components.AdBannerContainer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ToolVisualIdentity(
    val icon: ImageVector,
    val iconTint: Color,
    val containerBg: Color,
    val toolLabel: String
)

private fun getToolVisualIdentity(operationType: String?, fileName: String): ToolVisualIdentity {
    val op = (operationType ?: "").lowercase(Locale.ROOT)
    val name = fileName.lowercase(Locale.ROOT)

    return when {
        op.contains("merge") || name.contains("merge") -> ToolVisualIdentity(
            icon = Icons.Default.MergeType,
            iconTint = Color(0xFF2563EB),
            containerBg = Color(0xFFEFF6FF),
            toolLabel = "Merge"
        )
        op.contains("split") || name.contains("split") -> ToolVisualIdentity(
            icon = Icons.Default.CallSplit,
            iconTint = Color(0xFFD97706),
            containerBg = Color(0xFFFEF3C7),
            toolLabel = "Split"
        )
        op.contains("compress") || name.contains("compress") -> ToolVisualIdentity(
            icon = Icons.Default.Compress,
            iconTint = Color(0xFF059669),
            containerBg = Color(0xFFECFDF5),
            toolLabel = "Compress"
        )
        op.contains("image") || op.contains("photo") || name.contains("photo") || name.contains("image") -> ToolVisualIdentity(
            icon = Icons.Default.Image,
            iconTint = Color(0xFF7C3AED),
            containerBg = Color(0xFFF5F3FF),
            toolLabel = "Image to PDF"
        )
        op.contains("text") || name.contains("notes") || name.contains("text") -> ToolVisualIdentity(
            icon = Icons.Default.TextFields,
            iconTint = Color(0xFF0891B2),
            containerBg = Color(0xFFECFEFF),
            toolLabel = "Text to PDF"
        )
        op.contains("lock") || op.contains("protect") || name.contains("protected") || name.contains("locked") -> ToolVisualIdentity(
            icon = Icons.Default.Lock,
            iconTint = Color(0xFFDC2626),
            containerBg = Color(0xFFFEF2F2),
            toolLabel = "Lock"
        )
        op.contains("unlock") || name.contains("unlocked") -> ToolVisualIdentity(
            icon = Icons.Default.LockOpen,
            iconTint = Color(0xFFEA580C),
            containerBg = Color(0xFFFFF7ED),
            toolLabel = "Unlock"
        )
        op.contains("rotate") || name.contains("rotated") -> ToolVisualIdentity(
            icon = Icons.Default.RotateRight,
            iconTint = Color(0xFF0284C7),
            containerBg = Color(0xFFF0F9FF),
            toolLabel = "Rotate"
        )
        op.contains("extract") || name.contains("extracted") -> ToolVisualIdentity(
            icon = Icons.Default.ContentCopy,
            iconTint = Color(0xFFDB2777),
            containerBg = Color(0xFFFDF2F8),
            toolLabel = "Extract"
        )
        op.contains("sign") || name.contains("signed") -> ToolVisualIdentity(
            icon = Icons.Default.Draw,
            iconTint = Color(0xFF4F46E5),
            containerBg = Color(0xFFEEF2FF),
            toolLabel = "Sign"
        )
        else -> ToolVisualIdentity(
            icon = Icons.Default.Description,
            iconTint = Color(0xFFE11D48),
            containerBg = Color(0xFFFFF1F2),
            toolLabel = "PDF"
        )
    }
}

@Composable
fun RecentScreen(
    viewModel: PdfViewModel,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val recentList by viewModel.allRecentPdfs.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Dialog States
    var pdfToRename by remember { mutableStateOf<RecentPdf?>(null) }
    var pdfForInfo by remember { mutableStateOf<RecentPdf?>(null) }
    var pdfToRemoveHistoryOnly by remember { mutableStateOf<RecentPdf?>(null) }
    var pdfToDeleteFile by remember { mutableStateOf<RecentPdf?>(null) }

    val filteredList = remember(recentList, searchQuery) {
        if (searchQuery.isBlank()) recentList
        else recentList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.operationType.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("recent_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.section_recent_files),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (recentList.isEmpty()) "Your created PDF history" else "${recentList.size} documents saved on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (recentList.isNotEmpty()) {
                IconButton(onClick = { showClearAllDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear all recent history",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Search bar
        if (recentList.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                placeholder = { Text("Search by document name or tool...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Central List or Empty State
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching files" else stringResource(R.string.empty_recent_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "Try searching for a different keyword" else stringResource(R.string.empty_recent_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToHome,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_create_first_pdf),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 14.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    RecentPdfCard(
                        recentPdf = item,
                        onOpen = {
                            val file = File(item.filePath)
                            if (file.exists()) viewModel.openPdf(context, file)
                        },
                        onShare = {
                            val file = File(item.filePath)
                            if (file.exists()) viewModel.sharePdf(context, file)
                        },
                        onRename = { pdfToRename = item },
                        onShowInfo = { pdfForInfo = item },
                        onRemoveFromHistory = { pdfToRemoveHistoryOnly = item },
                        onDeleteFile = { pdfToDeleteFile = item }
                    )
                }
            }
        }

        // AdMob Banner
        AdBannerContainer(
            entitlement = entitlement,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }

    // --- Dialogs ---

    // 1. Rename Dialog
    pdfToRename?.let { item ->
        var newNameText by remember(item.id) { mutableStateOf(item.name.removeSuffix(".pdf")) }
        AlertDialog(
            onDismissRequest = { pdfToRename = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                Column {
                    Text(
                        text = "Enter a new name for this PDF:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newNameText,
                        onValueChange = { newNameText = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sanitized = newNameText.trim()
                        if (sanitized.isNotBlank()) {
                            val finalName = if (sanitized.endsWith(".pdf", ignoreCase = true)) sanitized else "$sanitized.pdf"
                            viewModel.renameRecentPdf(item, finalName)
                        }
                        pdfToRename = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. File Details / Info Dialog
    pdfForInfo?.let { item ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(item.createdAt))
        val file = File(item.filePath)
        val fileExists = file.exists()

        AlertDialog(
            onDismissRequest = { pdfForInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_file_info))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "File Name", value = item.name)
                    InfoRow(label = "Operation", value = item.operationType)
                    InfoRow(label = "Page Count", value = if (item.pageCount > 0) "${item.pageCount} pages" else "N/A")
                    InfoRow(label = "File Size", value = FileUtils.formatFileSize(if (fileExists) file.length() else item.sizeBytes))
                    InfoRow(label = "Created", value = formattedDate)
                    InfoRow(label = "Storage Status", value = if (fileExists) "On Device" else "Deleted externally")
                    InfoRow(label = "Path", value = item.filePath)
                }
            },
            confirmButton = {
                Button(
                    onClick = { pdfForInfo = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // 3. Remove from Recent History Only (keeps file on disk)
    pdfToRemoveHistoryOnly?.let { item ->
        AlertDialog(
            onDismissRequest = { pdfToRemoveHistoryOnly = null },
            title = { Text(stringResource(R.string.action_remove_history)) },
            text = {
                Text("Remove '${item.name}' from the recent history list?\n\nThe PDF file itself will NOT be deleted from your device storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFromRecentOnly(item)
                        pdfToRemoveHistoryOnly = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToRemoveHistoryOnly = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Delete File from Device (deletes file + removes history)
    pdfToDeleteFile?.let { item ->
        AlertDialog(
            onDismissRequest = { pdfToDeleteFile = null },
            title = { Text(stringResource(R.string.action_delete_file)) },
            text = {
                Text("Are you sure you want to permanently delete '${item.name}' from your device storage?\n\nThis action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecentPdf(item)
                        pdfToDeleteFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDeleteFile = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Clear All History Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Recent Records?") },
            text = {
                Text("This will remove all entries from the Recent list and clean up temporary output files on this device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllRecents()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Professional Redesigned Recent PDF Item Card:
 * - Left side: Distinct visual tool icon in a clean rounded container
 * - Right side: File name and metadata
 * - Integrated actions: Share and Options dropdown menu
 */
@Composable
private fun RecentPdfCard(
    recentPdf: RecentPdf,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onShowInfo: () -> Unit,
    onRemoveFromHistory: () -> Unit,
    onDeleteFile: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val visualIdentity = remember(recentPdf.operationType, recentPdf.name) {
        getToolVisualIdentity(recentPdf.operationType, recentPdf.name)
    }
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        recentPdf.createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Distinct Tool Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = visualIdentity.containerBg,
                border = BorderStroke(1.dp, visualIdentity.iconTint.copy(alpha = 0.2f)),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = visualIdentity.icon,
                        contentDescription = visualIdentity.toolLabel,
                        tint = visualIdentity.iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right Side: File Name & Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recentPdf.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tool tag pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = visualIdentity.iconTint.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = visualIdentity.toolLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = visualIdentity.iconTint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (recentPdf.pageCount > 0) {
                        Text(
                            text = "${recentPdf.pageCount} pgs",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = FileUtils.formatFileSize(recentPdf.sizeBytes),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "$relativeTime",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick Actions & Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Document options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_open)) },
                            onClick = { menuExpanded = false; onOpen() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = { menuExpanded = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_file_info)) },
                            onClick = { menuExpanded = false; onShowInfo() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove_history)) },
                            onClick = { menuExpanded = false; onRemoveFromHistory() }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.action_delete_file),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { menuExpanded = false; onDeleteFile() }
                        )
                    }
                }
            }
        }
    }
}
