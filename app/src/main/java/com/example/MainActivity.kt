package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppThemeMode
import com.example.ui.PdfViewModel
import com.example.ui.components.OnboardingDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.RecentScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.tools.CompressPdfScreen
import com.example.ui.screens.tools.ExtractPagesScreen
import com.example.ui.screens.tools.ImageToPdfScreen
import com.example.ui.screens.tools.LockPdfScreen
import com.example.ui.screens.tools.MergePdfScreen
import com.example.ui.screens.tools.RotatePagesScreen
import com.example.ui.screens.tools.SignPdfScreen
import com.example.ui.screens.tools.SplitPdfScreen
import com.example.ui.screens.tools.TextToPdfScreen
import com.example.ui.screens.tools.UnlockPdfScreen
import com.example.ui.theme.UniversalPdfTheme

sealed class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem("home", R.string.nav_home, Icons.Filled.GridView, Icons.Outlined.GridView)
    object Tools : BottomNavItem("tools", R.string.nav_tools, Icons.Filled.Build, Icons.Outlined.Build)
    object Recent : BottomNavItem("recent", R.string.nav_recent, Icons.Filled.History, Icons.Outlined.History)
    object Settings : BottomNavItem("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            UniversalPdfTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val bottomBarRoutes = listOf(
                    BottomNavItem.Home.route,
                    BottomNavItem.Tools.route,
                    BottomNavItem.Recent.route,
                    BottomNavItem.Settings.route
                )
                val shouldShowBottomBar = currentRoute in bottomBarRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (shouldShowBottomBar) {
                            val items = listOf(
                                BottomNavItem.Home,
                                BottomNavItem.Tools,
                                BottomNavItem.Recent,
                                BottomNavItem.Settings
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                items.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.titleRes),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(item.titleRes),
                                                fontSize = 11.sp
                                            )
                                        },
                                        selected = isSelected,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = BottomNavItem.Home.route
                        ) {
                            // Bottom Nav Screens
                            composable(BottomNavItem.Home.route) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToTool = { toolRoute ->
                                        navController.navigate(toolRoute)
                                    },
                                    onNavigateToPremium = {
                                        navController.navigate("premium")
                                    },
                                    onNavigateToRecent = {
                                        navController.navigate(BottomNavItem.Recent.route)
                                    }
                                )
                            }

                            composable(BottomNavItem.Tools.route) {
                                ToolsScreen(
                                    viewModel = viewModel,
                                    onNavigateToTool = { toolRoute ->
                                        navController.navigate(toolRoute)
                                    }
                                )
                            }

                            composable(BottomNavItem.Recent.route) {
                                RecentScreen(
                                    viewModel = viewModel,
                                    onNavigateToHome = {
                                        navController.navigate(BottomNavItem.Home.route)
                                    }
                                )
                            }

                            composable(BottomNavItem.Settings.route) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToPremium = {
                                        navController.navigate("premium")
                                    }
                                )
                            }

                            // Tool screens
                            composable("merge") {
                                MergePdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("split") {
                                SplitPdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("image_to_pdf") {
                                ImageToPdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("text_to_pdf") {
                                TextToPdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("lock") {
                                LockPdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("unlock") {
                                UnlockPdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("compress") {
                                CompressPdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("sign") {
                                SignPdfScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("extract") {
                                ExtractPagesScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("rotate") {
                                RotatePagesScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("premium") {
                                PremiumScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        if (isFirstLaunch) {
                            OnboardingDialog(
                                onDismiss = {
                                    viewModel.completeOnboarding()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
