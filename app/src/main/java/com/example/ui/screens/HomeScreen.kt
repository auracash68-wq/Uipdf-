package com.example.ui.screens

import android.app.Activity
import android.text.format.DateUtils
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.model.UserEntitlement
import com.example.ui.PdfViewModel
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.BentoGridCard
import com.example.ui.components.PremiumBannerCard
import java.io.File

@Composable
fun HomeScreen(
    viewModel: PdfViewModel,
    onNavigateToTool: (String) -> Unit,
    onNavigateToRecent: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val context = LocalContext.current
    val entitlement by viewModel.entitlement.collectAsState()
    val previewRecents by viewModel.previewRecentPdfs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 1. Header with Bento branding
        item {
            HeaderSection(onNavigateToPremium = onNavigateToPremium, isPremium = entitlement == UserEntitlement.PREMIUM)
        }

        // 2. Premium Promotion Banner (if free user)
        if (entitlement != UserEntitlement.PREMIUM) {
            item {
                PremiumBannerCard(
                    onUpgradeClick = onNavigateToPremium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
        }

        // 3. Bento Grid - Primary PDF Operations
        item {
            BentoToolsGrid(
                onNavigateToTool = onNavigateToTool
            )
        }

        // 4. Recent Activity Preview
        item {
            RecentActivityHeader(onViewAllClick = onNavigateToRecent)
        }

        if (previewRecents.isEmpty()) {
            item {
                EmptyRecentCard(onNavigateToMerge = { onNavigateToTool("merge") })
            }
        } else {
            items(previewRecents) { recent ->
                RecentFileItem(
                    recentPdf = recent,
                    onOpen = {
                        val file = File(recent.filePath)
                        if (file.exists()) viewModel.openPdf(context, file)
                    },
                    onShare = {
                        val file = File(recent.filePath)
                        if (file.exists()) viewModel.sharePdf(context, file)
                    },
                    onDelete = { viewModel.deleteRecentPdf(recent) }
                )
            }
        }

        // 5. AdMob Banner (free tier)
        item {
            Spacer(modifier = Modifier.height(12.dp))
            AdBannerContainer(
                entitlement = entitlement,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun HeaderSection(
    onNavigateToPremium: () -> Unit,
    isPremium: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "PRIVATE • OFFLINE • SECURE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable(onClick = onNavigateToPremium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = "PDF Utility",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun BentoToolsGrid(
    onNavigateToTool: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Merge & Split
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoGridCard(
                title = stringResource(R.string.tool_merge),
                subtitle = "Combine files",
                icon = Icons.Default.MergeType,
                iconBgColor = Color(0xFFEEF2FF),
                iconTintColor = Color(0xFF4F46E5),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTool("merge") }
            )

            BentoGridCard(
                title = stringResource(R.string.tool_split),
                subtitle = "Extract pages",
                icon = Icons.Default.CallSplit,
                iconBgColor = Color(0xFFFEF3C7),
                iconTintColor = Color(0xFFD97706),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTool("split") }
            )
        }

        // Row 2: Image to PDF & Lock PDF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoGridCard(
                title = stringResource(R.string.tool_image_to_pdf),
                subtitle = "Photos to PDF",
                icon = Icons.Default.Image,
                iconBgColor = Color(0xFFEFF6FF),
                iconTintColor = Color(0xFF2563EB),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTool("image_to_pdf") }
            )

            BentoGridCard(
                title = stringResource(R.string.tool_lock),
                subtitle = "Password protect",
                icon = Icons.Default.Lock,
                iconBgColor = Color(0xFFECFDF5),
                iconTintColor = Color(0xFF059669),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTool("lock") }
            )
        }

        // Row 3: Text to PDF & Advanced Suite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoGridCard(
                title = stringResource(R.string.tool_text_to_pdf),
                subtitle = "Notes to PDF",
                icon = Icons.Default.TextFields,
                iconBgColor = Color(0xFFFAF5FF),
                iconTintColor = Color(0xFF9333EA),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTool("text_to_pdf") }
            )

            BentoGridCard(
                title = "Advanced",
                subtitle = "Compress, sign, etc",
                icon = Icons.Default.Widgets,
                iconBgColor = Color.White.copy(alpha = 0.2f),
                iconTintColor = Color.White,
                isHighlighted = true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTool("tools_tab") }
            )
        }
    }
}

@Composable
private fun RecentActivityHeader(onViewAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "RECENT ACTIVITY",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = stringResource(R.string.view_all),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onViewAllClick)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun RecentFileItem(
    recentPdf: RecentPdf,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        recentPdf.createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "PDF Document",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recentPdf.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${recentPdf.operationType} • ${FileUtils.formatFileSize(recentPdf.sizeBytes)} • $relativeTime",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = { menuExpanded = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { menuExpanded = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecentCard(onNavigateToMerge: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.empty_recent_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_recent_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
