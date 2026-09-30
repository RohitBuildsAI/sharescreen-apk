package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppScreen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Onboarding : AppScreen("onboarding", "Welcome")
    data object MonitoredDashboard : AppScreen("monitored_dashboard", "Dashboard", Icons.Default.Dashboard)
    data object ViewerDashboard : AppScreen("viewer_dashboard", "Viewer", Icons.Default.Dashboard)
    data object Notifications : AppScreen("notifications", "Notifications", Icons.Default.Notifications)
    data object Usage : AppScreen("usage", "App Usage", Icons.Default.QueryStats)
    data object ScreenShare : AppScreen("screen_share", "Live Screen", Icons.Default.ScreenShare)
    data object Pairing : AppScreen("pairing", "Pairing", Icons.Default.Devices)
    data object Analytics : AppScreen("analytics", "Analytics", Icons.Default.Analytics)
    data object AiInsights : AppScreen("ai_insights", "AI Insights", Icons.Default.Psychology)
    data object GlobalSearch : AppScreen("search", "Search", Icons.Default.Search)
    data object DeviceInfo : AppScreen("device_info", "Device Info", Icons.Default.Info)
    data object PermissionsCenter : AppScreen("permissions", "Permissions", Icons.Default.Security)
    data object Settings : AppScreen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItemsMonitored: List<AppScreen>
            get() = listOf(
                MonitoredDashboard,
                Notifications,
                Usage,
                ScreenShare,
                Settings
            )

        val bottomNavItemsViewer: List<AppScreen>
            get() = listOf(
                ViewerDashboard,
                Notifications,
                Usage,
                ScreenShare,
                Settings
            )
    }
}
