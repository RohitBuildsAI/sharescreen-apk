package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DeviceRole
import com.example.ui.viewmodel.MainViewModel

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onRoleChanged: (DeviceRole) -> Unit,
    authViewModel: AuthViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val role by viewModel.deviceRole.collectAsState()
    val security = viewModel.securityStorage
    val authUser by authViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }

    var isAppLockEnabled by remember { mutableStateOf(security.isAppLockEnabled()) }
    var isBiometricEnabled by remember { mutableStateOf(security.isBiometricEnabled()) }
    var isAiEnabled by remember { mutableStateOf(security.isAiAnalysisEnabled()) }
    var selectedTheme by remember { mutableStateOf(security.getThemeMode()) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Firebase Account
        if (authUser != null) {
            item {
                Text(
                    text = "Firebase Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "User Avatar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = authUser?.displayName ?: "Companion User",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = authUser?.email ?: "Authenticated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        SettingsActionRow(
                            title = "Sign Out of Firebase",
                            subtitle = "Disconnect account and return to authentication",
                            icon = Icons.Default.ExitToApp,
                            color = MaterialTheme.colorScheme.error,
                            onClick = {
                                authViewModel?.signOut()
                            }
                        )
                    }
                }
            }
        }

        // Section: Permissions Center
        item {
            Text(
                text = "Permissions Center",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PermissionRow(
                        title = "Notification Access",
                        subtitle = "Required to capture and record notification history",
                        isEnabled = deviceStatus.hasNotificationAccess,
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    )

                    PermissionRow(
                        title = "Usage Access",
                        subtitle = "Required to compute app time and screen analytics",
                        isEnabled = deviceStatus.hasUsageAccess,
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    )

                    PermissionRow(
                        title = "Screen Sharing (MediaProjection)",
                        subtitle = "Consent requested dynamically whenever sharing is started",
                        isEnabled = deviceStatus.isScreenSharing,
                        onOpenSettings = null
                    )
                }
            }
        }

        // Section: Security & App Lock
        item {
            Text(
                text = "Security & App Lock",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Lock Protection", fontWeight = FontWeight.SemiBold)
                            Text("Protect notification history, viewer, and settings with a PIN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked && !security.hasPinSet()) {
                                    showPinDialog = true
                                } else {
                                    isAppLockEnabled = checked
                                    security.setAppLockEnabled(checked)
                                }
                            },
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }

                    if (isAppLockEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Authentication", fontWeight = FontWeight.SemiBold)
                                Text("Use fingerprint or face unlock where available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { checked ->
                                    isBiometricEnabled = checked
                                    security.setBiometricEnabled(checked)
                                }
                            )
                        }

                        Button(
                            onClick = { showPinDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Change Security PIN")
                        }
                    }
                }
            }
        }

        // Section: Privacy & Data Deletion
        item {
            Text(
                text = "Privacy Controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Analysis", fontWeight = FontWeight.SemiBold)
                            Text("Generate automated summaries and distraction insights", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isAiEnabled,
                            onCheckedChange = { checked ->
                                isAiEnabled = checked
                                security.setAiAnalysisEnabled(checked)
                            }
                        )
                    }

                    SettingsActionRow(
                        title = "Disconnect Paired Device",
                        subtitle = "Revoke access and terminate active companion connection",
                        icon = Icons.Default.LinkOff,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.disconnect() }
                    )

                    SettingsActionRow(
                        title = "Delete All Local Data & Reset",
                        subtitle = "Purges notifications, cached usage, pairing secrets, and PIN",
                        icon = Icons.Default.Delete,
                        color = MaterialTheme.colorScheme.error,
                        onClick = { showClearAllDialog = true }
                    )
                }
            }
        }

        // Section: Device Role Switcher
        item {
            Text(
                text = "Device Role",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Mode: ${if (role == DeviceRole.MONITORED) "Device A (Monitored)" else "Device B (Viewer)"}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You can change this device's role at any time. Monitored phones share permitted data; Viewer phones display synchronized activity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = role == DeviceRole.MONITORED,
                            onClick = {
                                viewModel.selectRole(DeviceRole.MONITORED)
                                onRoleChanged(DeviceRole.MONITORED)
                            },
                            label = { Text("Monitored Phone") }
                        )

                        FilterChip(
                            selected = role == DeviceRole.VIEWER,
                            onClick = {
                                viewModel.selectRole(DeviceRole.VIEWER)
                                onRoleChanged(DeviceRole.VIEWER)
                            },
                            label = { Text("Viewer Phone") }
                        )
                    }
                }
            }
        }

        // Section: Appearance Theme
        item {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Mode", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                            FilterChip(
                                selected = selectedTheme == mode,
                                onClick = {
                                    selectedTheme = mode
                                    security.setThemeMode(mode)
                                },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set App Lock PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to lock your companion app.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinMessage.isNotBlank()) {
                        Text(pinMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            security.setPin(pinInput)
                            security.setAppLockEnabled(true)
                            isAppLockEnabled = true
                            showPinDialog = false
                            pinInput = ""
                            pinMessage = ""
                        } else {
                            pinMessage = "PIN must be exactly 4 digits"
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset All Data Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Reset All Data & Settings?") },
            text = { Text("This will permanently delete all notification history, app usage caches, pairing tokens, and reset app role. This action cannot be reversed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearAllDialog = false
                        onRoleChanged(DeviceRole.UNCONFIGURED)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
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
fun PermissionRow(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onOpenSettings: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isEnabled) Color(0xFF2EC4B6) else Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onOpenSettings != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEnabled) "Configured" else "Enable")
            }
        } else {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isEnabled) Color(0xFF2EC4B6).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isEnabled) "ACTIVE" else "IDLE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isEnabled) Color(0xFF2EC4B6) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
