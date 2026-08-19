package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.PdfViewModel
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.BentoGridCard

@Composable
fun ToolsScreen(
    viewModel: PdfViewModel,
    onNavigateToTool: (String) -> Unit
) {
    val entitlement by viewModel.entitlement.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tools_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.section_all_tools),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "100% offline document processing engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Organize & Edit
        item {
            Text(
                text = "ORGANIZE & EDIT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoGridCard(
                        title = stringResource(R.string.tool_merge),
                        subtitle = stringResource(R.string.tool_merge_desc),
                        icon = Icons.Default.MergeType,
                        iconBgColor = Color(0xFFEEF2FF),
                        iconTintColor = Color(0xFF4F46E5),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("merge") }
                    )

                    BentoGridCard(
                        title = stringResource(R.string.tool_split),
                        subtitle = stringResource(R.string.tool_split_desc),
                        icon = Icons.Default.CallSplit,
                        iconBgColor = Color(0xFFFEF3C7),
                        iconTintColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("split") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoGridCard(
                        title = stringResource(R.string.tool_extract),
                        subtitle = stringResource(R.string.tool_extract_desc),
                        icon = Icons.Default.Layers,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconTintColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("extract") }
                    )

                    BentoGridCard(
                        title = stringResource(R.string.tool_rotate),
                        subtitle = stringResource(R.string.tool_rotate_desc),
                        icon = Icons.Default.CropRotate,
                        iconBgColor = Color(0xFFF0FDF4),
                        iconTintColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("rotate") }
                    )
                }
            }
        }

        // Section 2: Convert & Create
        item {
            Text(
                text = "CONVERT & CREATE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoGridCard(
                        title = stringResource(R.string.tool_image_to_pdf),
                        subtitle = stringResource(R.string.tool_image_to_pdf_desc),
                        icon = Icons.Default.Image,
                        iconBgColor = Color(0xFFEFF6FF),
                        iconTintColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("image_to_pdf") }
                    )

                    BentoGridCard(
                        title = stringResource(R.string.tool_text_to_pdf),
                        subtitle = stringResource(R.string.tool_text_to_pdf_desc),
                        icon = Icons.Default.TextFields,
                        iconBgColor = Color(0xFFFAF5FF),
                        iconTintColor = Color(0xFF9333EA),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("text_to_pdf") }
                    )
                }
            }
        }

        // Section 3: Security & Optimization
        item {
            Text(
                text = "SECURITY & OPTIMIZATION",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoGridCard(
                        title = stringResource(R.string.tool_lock),
                        subtitle = stringResource(R.string.tool_lock_desc),
                        icon = Icons.Default.Lock,
                        iconBgColor = Color(0xFFECFDF5),
                        iconTintColor = Color(0xFF059669),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("lock") }
                    )

                    BentoGridCard(
                        title = stringResource(R.string.tool_unlock),
                        subtitle = stringResource(R.string.tool_unlock_desc),
                        icon = Icons.Default.LockOpen,
                        iconBgColor = Color(0xFFFFFBEB),
                        iconTintColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("unlock") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoGridCard(
                        title = stringResource(R.string.tool_compress),
                        subtitle = stringResource(R.string.tool_compress_desc),
                        icon = Icons.Default.Compress,
                        iconBgColor = Color(0xFFF0FDF4),
                        iconTintColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("compress") }
                    )

                    BentoGridCard(
                        title = stringResource(R.string.tool_sign),
                        subtitle = stringResource(R.string.tool_sign_desc),
                        icon = Icons.Default.Draw,
                        iconBgColor = Color(0xFFEEF2FF),
                        iconTintColor = Color(0xFF4F46E5),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTool("sign") }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            AdBannerContainer(
                entitlement = entitlement,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}
