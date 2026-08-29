package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.AppThemeMode
import com.example.model.UserEntitlement
import com.example.ui.PdfViewModel
import com.example.ui.components.AdBannerContainer
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.PremiumGold
import com.example.ui.theme.ThemeCategory

@Composable
fun SettingsScreen(
    viewModel: PdfViewModel,
    onNavigateToPremium: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val colorTheme by viewModel.colorTheme.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorThemeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Preferences & Privacy Control",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section: Appearance
        item {
            SectionHeader(title = "APPEARANCE")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingRow(
                    icon = Icons.Default.DarkMode,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Light / Dark Mode",
                    subtitle = when (themeMode) {
                        AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                        AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                    },
                    onClick = { showThemeDialog = true }
                )

                SettingRow(
                    icon = Icons.Default.Palette,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Color Theme",
                    subtitle = colorTheme.displayName,
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-4).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            colorTheme.previewColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                        }
                    },
                    onClick = { showColorThemeDialog = true }
                )
            }
        }

        // Section: Monetization
        item {
            SectionHeader(title = "MONETIZATION")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingRow(
                    icon = Icons.Default.MonetizationOn,
                    iconTint = PremiumGold,
                    title = "Subscription",
                    subtitle = "Subscription",
                    onClick = onNavigateToPremium
                )
            }
        }

        // Section: Privacy & Security
        item {
            SectionHeader(title = "PRIVACY & SECURITY")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingRow(
                    icon = Icons.Default.Shield,
                    iconTint = Color(0xFF16A34A),
                    title = "100% On-Device Processing",
                    subtitle = "Zero cloud uploads. All operations run locally.",
                    onClick = { showPrivacyDialog = true }
                )

                SettingRow(
                    icon = Icons.Default.Policy,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.privacy_policy_title),
                    subtitle = "Read our strict offline privacy policy",
                    onClick = { showPrivacyDialog = true }
                )

                SettingRow(
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.terms_title),
                    subtitle = "Terms of Service",
                    onClick = { showTermsDialog = true }
                )
            }
        }

        // Section: About
        item {
            SectionHeader(title = "ABOUT")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingRow(
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.app_name),
                    subtitle = stringResource(R.string.app_version),
                    onClick = {}
                )
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

    if (showThemeDialog) {
        Dialog(onDismissRequest = { showThemeDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Select Mode",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ThemeOptionRow("System Default", themeMode == AppThemeMode.SYSTEM) {
                        viewModel.setTheme(AppThemeMode.SYSTEM)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Light Theme", themeMode == AppThemeMode.LIGHT) {
                        viewModel.setTheme(AppThemeMode.LIGHT)
                        showThemeDialog = false
                    }
                    ThemeOptionRow("Dark Theme", themeMode == AppThemeMode.DARK) {
                        viewModel.setTheme(AppThemeMode.DARK)
                        showThemeDialog = false
                    }
                }
            }
        }
    }

    if (showColorThemeDialog) {
        ColorThemeDialog(
            currentTheme = colorTheme,
            onThemeSelect = { selectedTheme ->
                viewModel.setColorTheme(selectedTheme)
            },
            onDismiss = { showColorThemeDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showTermsDialog) {
        Dialog(onDismissRequest = { showTermsDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "PDF Suite is provided as a utility tool for managing PDF files.\n\n" +
                                "• Free users can access all tools supported by Google AdMob advertisements.\n" +
                                "• A one-time purchase of ₹29 grants permanent ad-free access on your Google Play account.\n" +
                                "• You retain full ownership and copyright of all documents created or modified with this utility.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    PrimaryButton(text = "Close", onClick = { showTermsDialog = false })
                }
            }
        }
    }
}

@Composable
private fun ColorThemeDialog(
    currentTheme: AppColorTheme,
    onThemeSelect: (AppColorTheme) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryTab by remember { mutableStateOf<ThemeCategory?>(null) }

    val filteredThemes = remember(selectedCategoryTab) {
        if (selectedCategoryTab == null) {
            AppColorTheme.entries
        } else {
            AppColorTheme.entries.filter { it.category == selectedCategoryTab }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Color Theme",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose a coordinated visual palette",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryTab == null,
                        onClick = { selectedCategoryTab = null },
                        label = { Text("All (${AppColorTheme.entries.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedCategoryTab == ThemeCategory.SINGLE_COLOR,
                        onClick = { selectedCategoryTab = ThemeCategory.SINGLE_COLOR },
                        label = { Text("Single", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedCategoryTab == ThemeCategory.MULTI_COLOR,
                        onClick = { selectedCategoryTab = ThemeCategory.MULTI_COLOR },
                        label = { Text("Combos", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Theme Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredThemes) { theme ->
                        val isSelected = theme == currentTheme
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onThemeSelect(theme)
                                }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                    } else {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left: Swatches and Name
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // 3 Swatch Dots
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-5).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        theme.previewColors.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        1.5.dp,
                                                        MaterialTheme.colorScheme.surface,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = theme.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = theme.category.title,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PrimaryButton(
                    text = "Done",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailingContent != null) {
            trailingContent()
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "PDF Suite • ViridOrigin Systems",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Meta Information Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Document Metadata",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Application: PDF Suite\n" +
                                        "• Package ID: com.aistudio.pdfutility.qxvrmp\n" +
                                        "• Publisher / Developer: ViridOrigin Systems (SahidHosenGazi)\n" +
                                        "• Contact: viridoriginsystems@gmail.com\n" +
                                        "• Location: West Bengal, PIN: 743425, India\n" +
                                        "• Effective / Last Updated: August 29, 2026",
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    PrivacySection(
                        title = "1. About PDF Suite",
                        body = "PDF Suite is a utility application designed to help Android users create, manage, and process PDF documents locally on their mobile devices.\n\n" +
                                "The application currently includes tools for Merging PDFs, Splitting PDFs, Compressing PDFs, converting Images to PDF, converting Text to PDF, Password-Protecting (Locking) PDFs, Removing Passwords (Unlocking) PDFs, Rotating Pages, Extracting Pages, and Applying Signatures to PDFs.\n\n" +
                                "The application is built on an on-device processing model where core document operations execute locally inside the private Android sandbox on your device."
                    )

                    PrivacySection(
                        title = "2. Information We Process",
                        body = "ViridOrigin Systems does not operate user accounts, customer registration systems, or remote document-ingestion servers for PDF Suite.\n\n" +
                                "Information processed by or through the application is categorized as follows:\n\n" +
                                "• User-Selected Documents and Images: Files you explicitly select using Android's system picker are accessed strictly for the requested utility operation.\n" +
                                "• User-Entered Text and Annotations: Text entered into the Text-to-PDF tool and hand-drawn strokes created in the signature pad are processed in volatile memory on-device.\n" +
                                "• Cryptographic Passwords: Passwords entered to protect or unlock documents exist only in temporary memory during processing and are never logged, stored, or transmitted.\n" +
                                "• Local History Metadata: Basic file descriptors (file name, local file path, file size, page count, operation timestamp) are stored locally in an on-device SQLite database via Room.\n" +
                                "• User Preferences: UI preferences such as theme mode, color palettes, and language choices are stored locally in Android SharedPreferences.\n" +
                                "• Third-Party Diagnostic & Advertising Data: In the ad-supported free version, the Google Mobile Ads SDK (AdMob) may receive standard device diagnostics, IP address, and advertising identifiers (e.g., Google Advertising ID) in accordance with Google's privacy policies."
                    )

                    PrivacySection(
                        title = "3. How PDF Files and Images Are Processed",
                        body = "• Storage Access Framework (SAF): When you choose to open or modify a file, you interact with Android's system file picker. PDF Suite receives temporary access only to the specific URI you select.\n\n" +
                                "• Local Temporary Staging: The selected document stream is temporarily copied into the application's private cache directory (cache/pdf_temp/) for processing.\n\n" +
                                "• On-Device Engine: PDF processing is executed using the open-source Apache PDFBox Android library (com.tom-roush:pdfbox-android) and native Android Graphics APIs on your device CPU/memory.\n\n" +
                                "• Output Generation: Newly created or modified PDF files are written to the application's sandboxed storage directory (files/generated_pdfs/).\n\n" +
                                "• Exporting: When you save a generated file, Android's system document creator (CreateDocument) allows you to choose an external destination of your choice.\n\n" +
                                "• Sharing: When you share a document, the application uses Android's secure FileProvider to generate a temporary, read-only content URI for the receiving application.\n\n" +
                                "• Passwords & Signatures: Passwords are used in memory to configure 128-bit encryption/decryption policies and are immediately cleared. Signature drawings are converted in memory to an image stamp and applied to the target page without recording biometric parameters."
                    )

                    PrivacySection(
                        title = "4. Local Storage and Recent Files",
                        body = "PDF Suite maintains a local record of recently generated documents using an on-device SQLite database managed by Android Jetpack Room (pdf_utility_database).\n\n" +
                                "The recent_pdfs table stores:\n" +
                                "• Unique item ID (auto-generated)\n" +
                                "• Display file name\n" +
                                "• Sandboxed internal file path\n" +
                                "• Creation timestamp\n" +
                                "• File size in bytes\n" +
                                "• Total page count\n" +
                                "• Operation type identifier (e.g., MERGE, LOCK, SIGN)\n\n" +
                                "The database does not store document text or binary contents. You can delete individual recent entries (which deletes the file from internal storage) or tap 'Clear All Recents' to remove all records and purge temporary cache files."
                    )

                    PrivacySection(
                        title = "5. Permissions and Device Access",
                        body = "PDF Suite declares and uses only the following minimal standard permissions in its Android Manifest:\n\n" +
                                "• INTERNET (android.permission.INTERNET): Required exclusively for Google Mobile Ads SDK communication to load advertisements in the free tier, and for Google Play Billing operations.\n\n" +
                                "• ACCESS_NETWORK_STATE (android.permission.ACCESS_NETWORK_STATE): Used to detect active Internet connectivity before making ad requests.\n\n" +
                                "What We DO NOT Request or Access:\n" +
                                "• NO Broad Storage Permissions: The app does not request READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, or MANAGE_EXTERNAL_STORAGE. All file access is strictly scoped per file through the Android Storage Access Framework.\n" +
                                "• NO Camera or Microphone Access.\n" +
                                "• NO Location (GPS or Network) Access.\n" +
                                "• NO Contacts, Phone State, or SMS Access.\n" +
                                "• NO Bluetooth, NFC, or Biometric Sensor Access."
                    )

                    PrivacySection(
                        title = "6. Internet and Network Communication",
                        body = "ViridOrigin Systems does not maintain any custom backend servers, web services, REST APIs, or cloud document repositories for PDF Suite.\n\n" +
                                "Your documents, images, text, and signatures are processed locally and are NEVER transmitted over the Internet to ViridOrigin Systems or any cloud storage provider.\n\n" +
                                "Network traffic generated by the application is confined to:\n" +
                                "1. Google Mobile Ads SDK (AdMob) for serving banner and interstitial advertisements in the ad-supported tier.\n" +
                                "2. Google Play Billing Client for verifying in-app entitlement status when Google Play services are present."
                    )

                    PrivacySection(
                        title = "7. Advertising and Third-Party Services",
                        body = "To keep core PDF utility features accessible free of charge, PDF Suite integrates the Google Mobile Ads SDK (com.google.android.gms:play-services-ads).\n\n" +
                                "• Data Handled by Google: When advertisements are requested, Google AdMob may collect and process device-specific information, coarse network information (such as IP address), app performance diagnostics, and advertising identifiers (such as the Google Advertising ID / AAID) subject to Google's Privacy Policy.\n\n" +
                                "• Ad Placement & Frequency: The app displays standard banner ads and frequency-capped interstitial ads. Ads are throttled to avoid disruptive experiences.\n\n" +
                                "• Ad Suppression: Advertisements are suppressed across the entire application if an ad-free premium entitlement is active.\n\n" +
                                "• User Ad Choices: You can manage or reset your advertising ID and opt out of personalized ads at any time via your device settings (Settings > Google > Ads)."
                    )

                    PrivacySection(
                        title = "8. Payments and Premium Features",
                        body = "PDF Suite includes architectural support for Google Play In-App Billing (com.android.billingclient:billing-ktx) to enable an optional one-time purchase for permanent ad removal.\n\n" +
                                "• Marketplace Neutrality: In distributions or marketplaces where in-app billing is not provisioned or active, the application operates in standard mode.\n\n" +
                                "• Financial Data Security: ViridOrigin Systems does not collect, receive, process, or store credit card details, debit card numbers, bank accounts, or billing addresses. All payment transactions are handled directly and securely by the relevant app store platform (such as Google Play)."
                    )

                    PrivacySection(
                        title = "9. Data Sharing and Disclosure",
                        body = "ViridOrigin Systems does not sell, rent, lease, trade, or disclose your personal data or document content to any third party.\n\n" +
                                "Data is shared only in the following scenarios:\n" +
                                "• User-Initiated Sharing: When you select 'Share PDF', you choose the third-party application (e.g., email client, messaging app) that receives temporary read-only access to the file via Android FileProvider.\n" +
                                "• Third-Party Advertising: Google Mobile Ads SDK interacts with Google's servers as disclosed in Section 7.\n" +
                                "• Legal Compliance: In the unlikely event required by applicable law, regulation, or valid court order."
                    )

                    PrivacySection(
                        title = "10. Data Retention and Deletion",
                        body = "• Temporary Files: Files placed in cache/pdf_temp/ are automatically purged during cache clearing operations or when reclaimed by the Android operating system.\n\n" +
                                "• Generated Files: Files in files/generated_pdfs/ remain stored locally on your device until you delete them via the Recent screen or clear the application's storage in Android Settings.\n\n" +
                                "• History Records: Recent file metadata in the Room SQLite database is retained until manually deleted by you or upon app uninstallation.\n\n" +
                                "• Full Erasure: Uninstalling PDF Suite permanently deletes all sandboxed files, local database records, cache, and SharedPreferences from your device."
                    )

                    PrivacySection(
                        title = "11. Security Architecture",
                        body = "We implement reasonable and standard technical safeguards to protect your files and local data:\n\n" +
                                "• App Sandboxing: Processing occurs within the isolated Android application sandbox, preventing other installed apps from accessing private files.\n" +
                                "• Secure File Sharing: File sharing uses Android FileProvider with temporary, scoped read grants (FLAG_GRANT_READ_URI_PERMISSION).\n" +
                                "• PDF Cryptography: PDF password protection utilizes standard 128-bit encryption algorithms provided by Apache PDFBox.\n\n" +
                                "Please note that no method of electronic storage or device execution is 100% secure against physical device theft, root compromises, or operating system-level malware."
                    )

                    PrivacySection(
                        title = "12. Backup and Device Transfer",
                        body = "PDF Suite participates in Android's standard Auto Backup framework (as defined in backup_rules.xml and data_extraction_rules.xml).\n\n" +
                                "Application preferences (theme, settings) and Room database metadata may be backed up to your personal cloud backup (e.g., Google Drive) according to your device OS settings.\n\n" +
                                "Temporary processing caches are excluded from backup. You can disable cloud backups for all apps in your Android system settings."
                    )

                    PrivacySection(
                        title = "13. Children's Privacy",
                        body = "PDF Suite is a general-purpose utility tool intended for general audiences. The application is not directed at children under the age of 13 (or the applicable age in your jurisdiction).\n\n" +
                                "We do not knowingly collect, solicit, or store personal information from children."
                    )

                    PrivacySection(
                        title = "14. Your Privacy Choices and Control",
                        body = "You maintain full control over your data while using PDF Suite:\n\n" +
                                "• File Ingestion Control: You choose which specific documents to open via the system picker.\n" +
                                "• Deletion Controls: You can delete individual recent entries or clear the entire history at any time.\n" +
                                "• Storage Reset: You can clear all data by navigating to Android Settings > Apps > PDF Suite > Storage > Clear Data.\n" +
                                "• Advertising Preferences: You can reset your Advertising ID or opt out of interest-based ads through your Android system settings.\n" +
                                "• Depending on your location and applicable data protection legislation (such as in India, the EEA/UK, or California), you may have statutory rights regarding third-party service data."
                    )

                    PrivacySection(
                        title = "15. Third-Party Services and Reference Links",
                        body = "For additional details regarding how third-party SDKs process diagnostic and advertising data, please consult their respective policies:\n\n" +
                                "• Google Privacy Policy: https://policies.google.com/privacy\n" +
                                "• Google Advertising Technologies: https://policies.google.com/technologies/ads\n" +
                                "• Apache PDFBox Android: Open-source under Apache License 2.0 (https://www.apache.org/licenses/LICENSE-2.0)"
                    )

                    PrivacySection(
                        title = "16. Changes to This Privacy Policy",
                        body = "ViridOrigin Systems may update this Privacy Policy from time to time to reflect operational, legal, or technical changes. Any revisions will be published directly within the application along with an updated 'Last Updated' date. We encourage you to review this policy periodically."
                    )

                    PrivacySection(
                        title = "17. Contact Us",
                        body = "If you have any questions, inquiries, or concerns regarding this Privacy Policy or the data practices of PDF Suite, please contact us:\n\n" +
                                "• Entity: ViridOrigin Systems\n" +
                                "• Developer: SahidHosenGazi\n" +
                                "• Email: viridoriginsystems@gmail.com\n" +
                                "• Location: West Bengal, PIN: 743425, India\n" +
                                "• Application Package: com.aistudio.pdfutility.qxvrmp"
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bottom Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    PrimaryButton(
                        text = "I Understand",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    body: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 21.sp,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
