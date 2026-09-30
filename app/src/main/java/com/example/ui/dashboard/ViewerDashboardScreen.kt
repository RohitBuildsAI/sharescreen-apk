package com.example.ui.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DeviceStatus

@Composable
fun ViewerDashboardScreen(
    deviceStatus: DeviceStatus,
    isConnected: Boolean,
    connectedDeviceName: String,
    onNavigateToPairing: () -> Unit,
    onNavigateToScreenShare: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToUsage: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToDeviceInfo: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connected Device Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Viewer Mode",
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isConnected) connectedDeviceName else "No Device Paired",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isConnected) "Paired Companion Device" else "Ready to connect over local Wi-Fi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Online / Offline Status Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isConnected) Color(0xFF2EC4B6) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isConnected) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isConnected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Battery: ${deviceStatus.batteryPercentage}%" + if (deviceStatus.isCharging) " (Charging)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Screen: " + if (deviceStatus.isScreenSharing) "STREAMING" else "IDLE",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (deviceStatus.isScreenSharing) Color(0xFFE63946) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToScreenShare,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ScreenShare, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Screen")
                            }

                            OutlinedButton(
                                onClick = onDisconnect,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect")
                            }
                        }
                    } else {
                        Button(
                            onClick = onNavigateToPairing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pair_device_button")
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pair with Monitored Phone")
                        }
                    }
                }
            }
        }

        // Live Device Status Summary Cards
        item {
            Text(
                text = "Live Monitored Telemetry",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    title = "Battery Level",
                    statusText = "${deviceStatus.batteryPercentage}%" + if (deviceStatus.isCharging) " (Chg)" else " (${deviceStatus.batteryHealth})",
                    isActive = deviceStatus.isCharging || deviceStatus.batteryPercentage > 20,
                    icon = if (deviceStatus.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )

                StatusCard(
                    title = "Live Screen",
                    statusText = if (deviceStatus.isScreenSharing) "STREAMING" else "INACTIVE",
                    isActive = deviceStatus.isScreenSharing,
                    icon = Icons.Default.ScreenShare,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToScreenShare
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    title = "Notification Sync",
                    statusText = if (deviceStatus.hasNotificationAccess) "ACTIVE" else "OFF",
                    isActive = deviceStatus.hasNotificationAccess,
                    icon = Icons.Default.Notifications,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNotifications
                )

                StatusCard(
                    title = "Usage Sync",
                    statusText = if (deviceStatus.hasUsageAccess) "ACTIVE" else "OFF",
                    isActive = deviceStatus.hasUsageAccess,
                    icon = Icons.Default.QueryStats,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToUsage
                )
            }
        }

        // Navigation Sections for Viewer
        item {
            Text(
                text = "Companion Sections",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureRow(
                    title = "Notifications Feed",
                    subtitle = "Streamed notification history from monitored phone",
                    icon = Icons.Default.Notifications,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToNotifications
                )

                FeatureRow(
                    title = "App Usage Tracker",
                    subtitle = "Daily and weekly screen time and app launches",
                    icon = Icons.Default.QueryStats,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToUsage
                )

                FeatureRow(
                    title = "Live Screen Streaming",
                    subtitle = if (deviceStatus.isScreenSharing) "Screen sharing is active - tap to view" else "Waiting for monitored device to start sharing",
                    icon = Icons.Default.ScreenShare,
                    iconColor = if (deviceStatus.isScreenSharing) Color(0xFFE63946) else MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToScreenShare
                )

                FeatureRow(
                    title = "AI Usage Insights",
                    subtitle = "Smart summary of usage trends and peak notification times",
                    icon = Icons.Default.Psychology,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToAi
                )

                FeatureRow(
                    title = "Visual Analytics",
                    subtitle = "Interactive charts for screen time and app distribution",
                    icon = Icons.Default.Analytics,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToAnalytics
                )

                FeatureRow(
                    title = "Device Specifications",
                    subtitle = "Model, OS version, battery status, and storage",
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToDeviceInfo
                )
            }
        }
    }
}
