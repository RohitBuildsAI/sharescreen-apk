package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.DeviceRole
import com.example.ui.ai.AiInsightsScreen
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.dashboard.MonitoredDashboardScreen
import com.example.ui.dashboard.ViewerDashboardScreen
import com.example.ui.device.DeviceInfoScreen
import com.example.ui.navigation.AppScreen
import com.example.ui.notifications.NotificationHistoryScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.pairing.PairingScreen
import com.example.ui.screen.ScreenShareScreen
import com.example.ui.search.GlobalSearchScreen
import com.example.ui.security.AppLockOverlay
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.usage.AppUsageScreen
import com.example.ui.viewmodel.AiInsightsViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NotificationViewModel
import com.example.ui.viewmodel.UsageViewModel

import com.example.ui.auth.SignInScreen
import com.example.ui.viewmodel.AuthViewModel

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(getString(R.string.google_app_id))
                    .setApiKey(getString(R.string.google_api_key))
                    .setProjectId(getString(R.string.project_id))
                    .setGcmSenderId(getString(R.string.gcm_defaultSenderId))
                    .build()
                FirebaseApp.initializeApp(this, options)
            } catch (e: Exception) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (_: Exception) {}
            }
        }

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val mainViewModel: MainViewModel = viewModel()
            val notificationViewModel: NotificationViewModel = viewModel()
            val usageViewModel: UsageViewModel = viewModel()
            val aiInsightsViewModel: AiInsightsViewModel = viewModel()

            val authUser by authViewModel.currentUser.collectAsState()
            val role by mainViewModel.deviceRole.collectAsState()
            val isAppLocked by mainViewModel.isAppLocked.collectAsState()
            val themeMode = mainViewModel.securityStorage.getThemeMode()

            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                if (authUser == null) {
                    SignInScreen(authViewModel = authViewModel)
                } else if (role == DeviceRole.UNCONFIGURED) {
                    OnboardingScreen(
                        onRoleSelected = { selectedRole ->
                            mainViewModel.selectRole(selectedRole)
                        }
                    )
                } else if (isAppLocked) {
                    AppLockOverlay(viewModel = mainViewModel)
                } else {
                    CompanionAppContent(
                        mainViewModel = mainViewModel,
                        notificationViewModel = notificationViewModel,
                        usageViewModel = usageViewModel,
                        aiInsightsViewModel = aiInsightsViewModel,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionAppContent(
    mainViewModel: MainViewModel,
    notificationViewModel: NotificationViewModel,
    usageViewModel: UsageViewModel,
    aiInsightsViewModel: AiInsightsViewModel,
    authViewModel: AuthViewModel
) {
    val role by mainViewModel.deviceRole.collectAsState()
    val isConnected by mainViewModel.isConnected.collectAsState()
    val connectedDeviceName by mainViewModel.connectedDeviceName.collectAsState()
    val pairingCode by mainViewModel.pairingCode.collectAsState()
    val deviceStatus by mainViewModel.deviceStatus.collectAsState()

    var currentScreen by remember(role) {
        mutableStateOf<AppScreen>(
            if (role == DeviceRole.MONITORED) AppScreen.MonitoredDashboard
            else AppScreen.ViewerDashboard
        )
    }

    val defaultDashboard = if (role == DeviceRole.MONITORED) AppScreen.MonitoredDashboard else AppScreen.ViewerDashboard

    // Proper back handling
    BackHandler(enabled = currentScreen != defaultDashboard) {
        currentScreen = defaultDashboard
    }

    val bottomNavItems = if (role == DeviceRole.MONITORED) {
        AppScreen.bottomNavItemsMonitored
    } else {
        AppScreen.bottomNavItemsViewer
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Share Screen: Parental Control",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Mode Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (role == DeviceRole.MONITORED)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (isConnected) Color(0xFF2EC4B6) else Color.Gray,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (role == DeviceRole.MONITORED) "Device A" else "Device B",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (role == DeviceRole.MONITORED)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        if (currentScreen != defaultDashboard) {
                            Text(
                                text = currentScreen?.title.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentScreen != defaultDashboard) {
                        IconButton(onClick = { currentScreen = defaultDashboard }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentScreen = AppScreen.GlobalSearch },
                        modifier = Modifier.testTag("topbar_search_action")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(
                        onClick = { currentScreen = AppScreen.DeviceInfo },
                        modifier = Modifier.testTag("topbar_device_info_action")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Device Info")
                    }
                    IconButton(
                        onClick = {
                            mainViewModel.refreshDeviceStatus()
                            usageViewModel.loadUsage()
                            aiInsightsViewModel.generateInsights()
                        },
                        modifier = Modifier.testTag("topbar_refresh_action")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                bottomNavItems.filterNotNull().forEach { screen ->
                    val isSelected = currentScreen?.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            screen.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag("nav_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.MonitoredDashboard -> {
                    MonitoredDashboardScreen(
                        deviceStatus = deviceStatus,
                        isConnected = isConnected,
                        connectedDeviceName = connectedDeviceName,
                        pairingCode = pairingCode,
                        onNavigateToScreenShare = { currentScreen = AppScreen.ScreenShare },
                        onNavigateToPairing = { currentScreen = AppScreen.Pairing },
                        onNavigateToNotifications = { currentScreen = AppScreen.Notifications },
                        onNavigateToUsage = { currentScreen = AppScreen.Usage },
                        onNavigateToPermissions = { currentScreen = AppScreen.PermissionsCenter },
                        onNavigateToAi = { currentScreen = AppScreen.AiInsights },
                        onNavigateToAnalytics = { currentScreen = AppScreen.Analytics },
                        onRefresh = { mainViewModel.refreshDeviceStatus() }
                    )
                }

                AppScreen.ViewerDashboard -> {
                    ViewerDashboardScreen(
                        deviceStatus = deviceStatus,
                        isConnected = isConnected,
                        connectedDeviceName = connectedDeviceName,
                        onNavigateToPairing = { currentScreen = AppScreen.Pairing },
                        onNavigateToScreenShare = { currentScreen = AppScreen.ScreenShare },
                        onNavigateToNotifications = { currentScreen = AppScreen.Notifications },
                        onNavigateToUsage = { currentScreen = AppScreen.Usage },
                        onNavigateToAnalytics = { currentScreen = AppScreen.Analytics },
                        onNavigateToAi = { currentScreen = AppScreen.AiInsights },
                        onNavigateToDeviceInfo = { currentScreen = AppScreen.DeviceInfo },
                        onDisconnect = { mainViewModel.disconnect() }
                    )
                }

                AppScreen.Notifications -> {
                    NotificationHistoryScreen(viewModel = notificationViewModel)
                }

                AppScreen.Usage -> {
                    AppUsageScreen(viewModel = usageViewModel)
                }

                AppScreen.ScreenShare -> {
                    ScreenShareScreen(viewModel = mainViewModel)
                }

                AppScreen.Pairing -> {
                    PairingScreen(viewModel = mainViewModel)
                }

                AppScreen.Analytics -> {
                    AnalyticsScreen(
                        usageViewModel = usageViewModel,
                        notificationViewModel = notificationViewModel
                    )
                }

                AppScreen.AiInsights -> {
                    AiInsightsScreen(viewModel = aiInsightsViewModel)
                }

                AppScreen.GlobalSearch -> {
                    GlobalSearchScreen(
                        usageViewModel = usageViewModel,
                        notificationViewModel = notificationViewModel
                    )
                }

                AppScreen.DeviceInfo -> {
                    DeviceInfoScreen(viewModel = mainViewModel)
                }

                AppScreen.PermissionsCenter, AppScreen.Settings -> {
                    SettingsScreen(
                        viewModel = mainViewModel,
                        onRoleChanged = { newRole ->
                            currentScreen = if (newRole == DeviceRole.MONITORED)
                                AppScreen.MonitoredDashboard
                            else
                                AppScreen.ViewerDashboard
                        },
                        authViewModel = authViewModel
                    )
                }

                else -> {
                    MonitoredDashboardScreen(
                        deviceStatus = deviceStatus,
                        isConnected = isConnected,
                        connectedDeviceName = connectedDeviceName,
                        pairingCode = pairingCode,
                        onNavigateToScreenShare = { currentScreen = AppScreen.ScreenShare },
                        onNavigateToPairing = { currentScreen = AppScreen.Pairing },
                        onNavigateToNotifications = { currentScreen = AppScreen.Notifications },
                        onNavigateToUsage = { currentScreen = AppScreen.Usage },
                        onNavigateToPermissions = { currentScreen = AppScreen.PermissionsCenter },
                        onNavigateToAi = { currentScreen = AppScreen.AiInsights },
                        onNavigateToAnalytics = { currentScreen = AppScreen.Analytics },
                        onRefresh = { mainViewModel.refreshDeviceStatus() }
                    )
                }
            }
        }
    }
}
