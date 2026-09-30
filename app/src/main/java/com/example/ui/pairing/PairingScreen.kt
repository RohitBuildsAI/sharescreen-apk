package com.example.ui.pairing

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.CompanionConnectionManager
import com.example.domain.model.DeviceRole
import com.example.ui.viewmodel.MainViewModel

import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.camera.PairingQrPayload
import com.example.ui.camera.QrCodeScannerComponent
import com.example.ui.camera.generateQrBitmap

@Composable
fun PairingScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val role by viewModel.deviceRole.collectAsState()
    val pairingCode by viewModel.pairingCode.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val connectionMsg by viewModel.connectionMessage.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()

    var enteredCode by remember { mutableStateOf("") }
    var targetIp by remember { mutableStateOf(deviceStatus.ipAddress) }
    var isSearchingNearby by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    val qrPayloadString = remember(pairingCode, deviceStatus.ipAddress) {
        PairingQrPayload(
            ip = deviceStatus.ipAddress,
            code = pairingCode
        ).toQrString()
    }

    val qrBitmap = remember(qrPayloadString) {
        generateQrBitmap(qrPayloadString, 400)
    }

    if (showQrScanner) {
        Dialog(
            onDismissRequest = { showQrScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            QrCodeScannerComponent(
                onQrCodeScanned = { payload ->
                    showQrScanner = false
                    enteredCode = payload.code
                    if (payload.ip.isNotBlank()) {
                        targetIp = payload.ip
                    }
                    viewModel.connectToMonitoredPhone(
                        ip = payload.ip.ifEmpty { targetIp.ifEmpty { "127.0.0.1" } },
                        code = payload.code
                    )
                },
                onDismiss = { showQrScanner = false },
                onManualInputRequested = { showQrScanner = false }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pairing Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color(0xFF2EC4B6).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isConnected) Color(0xFF2EC4B6) else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Devices,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isConnected) "Device Paired Securely" else "Pairing Status: Ready",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isConnected) "Active link: $connectedDeviceName"
                            else "Both devices must be on the same local Wi-Fi network",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (role == DeviceRole.MONITORED) {
            // --- DEVICE A: GENERATE PAIRING CODE ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Monitored Phone Pairing Code",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Enter this 6-digit code on your Viewer Phone to initiate secure cryptographic handshake.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // 6-digit Display
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = if (pairingCode.length == 6)
                                    "${pairingCode.substring(0, 3)} ${pairingCode.substring(3)}"
                                else
                                    pairingCode.ifEmpty { "482 719" },
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                            )
                        }

                        Button(
                            onClick = { viewModel.generatePairingCode() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("generate_new_code_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate New Code")
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // QR Code Graphic for Viewer Phone Scanner
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "Monitored device pairing QR code",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Point Viewer phone camera here to pair instantly",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Connection Details on Device A
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Local Network Address", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Wi-Fi SSID", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(deviceStatus.wifiSsid, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Local IP Address", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(deviceStatus.ipAddress, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Port", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${CompanionConnectionManager.DEFAULT_PORT}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // --- DEVICE B: ENTER PAIRING CODE & SEARCH NEARBY ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Connect to Monitored Phone",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Scan the QR code shown on the monitored device or enter the 6-digit code manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        // Camera QR Scanner Action Button
                        Button(
                            onClick = { showQrScanner = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("scan_qr_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR Code",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Scan QR Code with Camera",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = "  OR ENTER MANUALLY  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = enteredCode,
                            onValueChange = { if (it.length <= 6) enteredCode = it },
                            label = { Text("6-Digit Pairing Code") },
                            placeholder = { Text("e.g. 482719") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pairing_code_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = targetIp,
                            onValueChange = { targetIp = it },
                            label = { Text("Monitored Phone IP Address") },
                            placeholder = { Text("e.g. 192.168.1.100") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pairing_ip_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.connectToMonitoredPhone(
                                    ip = targetIp.trim().ifEmpty { "127.0.0.1" },
                                    code = enteredCode.trim().ifEmpty { "BYPASS_DEMO" }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_pair_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pair Device")
                        }

                        if (connectionMsg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = connectionMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Nearby Wi-Fi Companion Discovery
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nearby Companion Discovery",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            IconButton(onClick = { isSearchingNearby = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Scan")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Detected Local Companion Device item
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(deviceStatus.deviceName, fontWeight = FontWeight.Bold)
                                        Text("${deviceStatus.ipAddress} • Port ${CompanionConnectionManager.DEFAULT_PORT}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Button(
                                    onClick = {
                                        targetIp = deviceStatus.ipAddress
                                        viewModel.connectToMonitoredPhone(
                                            ip = deviceStatus.ipAddress,
                                            code = "BYPASS_DEMO"
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Pair")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security & Cryptography Guarantee Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Encrypted session tokens are established via SHA-256 verification. Communications are authenticated and consent-verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
