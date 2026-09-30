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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopScreenShare
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
fun MonitoredDashboardScreen(
    deviceStatus: DeviceStatus,
    isConnected: Boolean,
    connectedDeviceName: String,
    pairingCode: String,
    onNavigateToScreenShare: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToUsage: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: Device Name & Mode Badge
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
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
                                    .size(44.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Monitored Phone",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = deviceStatus.deviceName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Monitored Phone • ${deviceStatus.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Connection Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isConnected) Color.Green else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isConnected) "Connected" else "Listening",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isConnected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Paired with: $connectedDeviceName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (pairingCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Pairing Code: $pairingCode (Waiting for viewer)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Monitoring Access & Status Grid
        item {
            Text(
                text = "Consent & Access Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    title = "Notification Access",
                    statusText = if (deviceStatus.hasNotificationAccess) "ON" else "OFF",
                    isActive = deviceStatus.hasNotificationAccess,
                    icon = Icons.Default.Notifications,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPermissions
                )

                StatusCard(
                    title = "Usage Access",
                    statusText = if (deviceStatus.hasUsageAccess) "ON" else "OFF",
                    isActive = deviceStatus.hasUsageAccess,
                    icon = Icons.Default.QueryStats,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPermissions
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    title = "Screen Sharing",
                    statusText = if (deviceStatus.isScreenSharing) "ACTIVE" else "OFF",
                    isActive = deviceStatus.isScreenSharing,
                    icon = if (deviceStatus.isScreenSharing) Icons.Default.ScreenShare else Icons.Default.StopScreenShare,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToScreenShare
                )

                StatusCard(
                    title = "Battery & Power",
                    statusText = "${deviceStatus.batteryPercentage}%" + if (deviceStatus.isCharging) " (Chg)" else "",
                    isActive = deviceStatus.isCharging || deviceStatus.batteryPercentage > 20,
                    icon = if (deviceStatus.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    modifier = Modifier.weight(1f),
                    onClick = onRefresh
                )
            }
        }

        // Real-time Battery Telemetry Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (deviceStatus.isCharging) Color(0xFF2EC4B6).copy(alpha = 0.15f)
                                else if (deviceStatus.batteryPercentage <= 20) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (deviceStatus.isCharging) Icons.Default.BatteryChargingFull
                            else if (deviceStatus.batteryPercentage <= 20) Icons.Default.BatteryAlert
                            else Icons.Default.BatteryFull,
                            contentDescription = "Battery Status",
                            tint = if (deviceStatus.isCharging) Color(0xFF2EC4B6)
                            else if (deviceStatus.batteryPercentage <= 20) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Battery: ${deviceStatus.batteryPercentage}%",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (deviceStatus.batteryHealth.equals("Good", ignoreCase = true))
                                    Color(0xFF2EC4B6).copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "Health: ${deviceStatus.batteryHealth}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (deviceStatus.batteryHealth.equals("Good", ignoreCase = true))
                                        Color(0xFF0F9D58)
                                    else
                                        MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (deviceStatus.isCharging)
                                "Status: Charging (${deviceStatus.chargingType})"
                            else
                                "Status: On Battery (${deviceStatus.chargingType})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Network Status Strip
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deviceStatus.wifiSsid,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "IP: ${deviceStatus.ipAddress} • ${deviceStatus.networkType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToPairing,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pair Code")
                    }
                }
            }
        }

        // Quick Navigation Tiles
        item {
            Text(
                text = "Companion Features",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureRow(
                    title = "Notification History",
                    subtitle = "View and manage captured notifications",
                    icon = Icons.Default.Notifications,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToNotifications
                )

                FeatureRow(
                    title = "App Usage Tracker",
                    subtitle = "Check screen time, daily usage & app launches",
                    icon = Icons.Default.QueryStats,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToUsage
                )

                FeatureRow(
                    title = "Remote Screen Sharing",
                    subtitle = if (deviceStatus.isScreenSharing) "Screen streaming is currently ACTIVE" else "Consent-based MediaProjection live stream",
                    icon = Icons.Default.ScreenShare,
                    iconColor = if (deviceStatus.isScreenSharing) Color(0xFFE63946) else MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToScreenShare
                )

                FeatureRow(
                    title = "AI Insights",
                    subtitle = "Personalized usage trends and distraction summary",
                    icon = Icons.Default.Psychology,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToAi
                )

                FeatureRow(
                    title = "Analytics & Charts",
                    subtitle = "Visual trends, daily distribution, and metrics",
                    icon = Icons.Default.Analytics,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToAnalytics
                )

                FeatureRow(
                    title = "Permissions Center",
                    subtitle = "Verify and manage Android system access",
                    icon = Icons.Default.Security,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToPermissions
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    statusText: String,
    isActive: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isActive) Color(0xFF2EC4B6) else Color.Gray,
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FeatureRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
