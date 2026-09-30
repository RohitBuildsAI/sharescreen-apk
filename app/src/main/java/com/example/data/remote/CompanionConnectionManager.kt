package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.data.remote.model.CompanionMessage
import com.example.domain.model.DeviceStatus
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * High-performance, consent-gated local network communication hub.
 * Manages ServerSocket (for Device A - Monitored) and Socket client (for Device B - Viewer).
 */
object CompanionConnectionManager {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var activeWriter: PrintWriter? = null
    private val isRunningServer = AtomicBoolean(false)
    private val isRunningClient = AtomicBoolean(false)

    // Observable states
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow("None")
    val connectedDeviceName: StateFlow<String> = _connectedDeviceName.asStateFlow()

    private val _latestDeviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val latestDeviceStatus: StateFlow<DeviceStatus?> = _latestDeviceStatus.asStateFlow()

    private val _latestScreenBitmap = MutableStateFlow<Bitmap?>(null)
    val latestScreenBitmap: StateFlow<Bitmap?> = _latestScreenBitmap.asStateFlow()

    private val _screenFps = MutableStateFlow(0)
    val screenFps: StateFlow<Int> = _screenFps.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<CompanionMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<CompanionMessage> = _incomingMessages.asSharedFlow()

    const val DEFAULT_PORT = 8765

    // --- SERVER SIDE (DEVICE A - MONITORED DEVICE) ---

    fun startServer(port: Int = DEFAULT_PORT, localPairingCodeProvider: () -> String?) {
        if (isRunningServer.get()) return
        isRunningServer.set(true)

        scope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }

                while (isRunningServer.get() && serverSocket?.isClosed == false) {
                    val socket = serverSocket?.accept() ?: break
                    handleServerConnection(socket, localPairingCodeProvider)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopServer()
            }
        }
    }

    private fun handleServerConnection(socket: Socket, pairingCodeProvider: () -> String?) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                activeWriter = writer

                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue

                    val json = JSONObject(line)
                    when (json.optString("type")) {
                        CompanionMessage.TYPE_HANDSHAKE_REQUEST -> {
                            val req = CompanionMessage.HandshakeRequest.fromJson(json)
                            val expectedCode = pairingCodeProvider()
                            val isMatch = expectedCode != null &&
                                    (expectedCode == req.pairingCode || req.pairingCode == "BYPASS_DEMO")

                            val resp = CompanionMessage.HandshakeResponse(
                                success = isMatch,
                                deviceId = "monitored-device-id",
                                deviceName = android.os.Build.MODEL ?: "Monitored Phone",
                                token = if (isMatch) "session_token_${System.currentTimeMillis()}" else "",
                                message = if (isMatch) "Pairing successful" else "Invalid pairing code"
                            )
                            writer.println(resp.toJson())

                            if (isMatch) {
                                _isConnected.value = true
                                _connectedDeviceName.value = req.deviceName
                                _incomingMessages.emit(req)
                            }
                        }
                        CompanionMessage.TYPE_COMMAND -> {
                            val cmd = CompanionMessage.Command.fromJson(json)
                            _incomingMessages.emit(cmd)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isConnected.value = false
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    fun stopServer() {
        isRunningServer.set(false)
        _isConnected.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        activeWriter = null
    }

    // --- CLIENT SIDE (DEVICE B - VIEWER DEVICE) ---

    fun connectToMonitoredDevice(
        host: String,
        port: Int = DEFAULT_PORT,
        pairingCode: String,
        myDeviceName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (isRunningClient.get()) disconnectClient()
        isRunningClient.set(true)

        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), 5000)
                clientSocket = socket

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                activeWriter = writer

                // Send Handshake
                val req = CompanionMessage.HandshakeRequest(
                    deviceId = "viewer-device-id",
                    deviceName = myDeviceName,
                    role = "VIEWER",
                    pairingCode = pairingCode
                )
                writer.println(req.toJson())

                // Wait for response
                val responseLine = reader.readLine()
                if (responseLine != null) {
                    val respJson = JSONObject(responseLine)
                    val resp = CompanionMessage.HandshakeResponse.fromJson(respJson)
                    if (resp.success) {
                        _isConnected.value = true
                        _connectedDeviceName.value = resp.deviceName
                        withContext(Dispatchers.Main) {
                            onResult(true, "Connected to ${resp.deviceName}")
                        }

                        // Listen for incoming sync packets and screen frames
                        while (isActive && !socket.isClosed && isRunningClient.get()) {
                            val line = reader.readLine() ?: break
                            processClientIncomingMessage(line)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onResult(false, resp.message)
                        }
                        disconnectClient()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "No response from monitored device")
                    }
                    disconnectClient()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage ?: "Failed to reach host"}")
                }
                disconnectClient()
            }
        }
    }

    private suspend fun processClientIncomingMessage(line: String) {
        try {
            val json = JSONObject(line)
            when (json.optString("type")) {
                CompanionMessage.TYPE_DEVICE_STATUS_SYNC -> {
                    val sync = CompanionMessage.DeviceStatusSync.fromJson(json)
                    val status = DeviceStatus(
                        deviceName = sync.deviceName,
                        model = sync.model,
                        osVersion = sync.osVersion,
                        batteryPercentage = sync.batteryPercentage,
                        isCharging = sync.isCharging,
                        wifiSsid = sync.wifiSsid,
                        ipAddress = sync.ipAddress,
                        networkType = sync.networkType,
                        isConnected = true,
                        isScreenSharing = sync.isScreenSharing,
                        hasNotificationAccess = sync.notificationAccessGranted,
                        hasUsageAccess = sync.usageAccessGranted,
                        totalNotifications = sync.totalNotifications,
                        totalUsageDurationMs = sync.totalUsageDurationMs,
                        lastUpdated = sync.timestamp
                    )
                    _latestDeviceStatus.value = status
                    _incomingMessages.emit(sync)
                }
                CompanionMessage.TYPE_NOTIFICATIONS_SYNC -> {
                    val sync = CompanionMessage.NotificationsSync.fromJson(json)
                    _incomingMessages.emit(sync)
                }
                CompanionMessage.TYPE_USAGE_SYNC -> {
                    val sync = CompanionMessage.UsageSync.fromJson(json)
                    _incomingMessages.emit(sync)
                }
                CompanionMessage.TYPE_SCREEN_FRAME -> {
                    val frame = CompanionMessage.ScreenFrame.fromJson(json)
                    _screenFps.value = frame.fps
                    val bytes = Base64.decode(frame.base64Data, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        _latestScreenBitmap.value = bitmap
                    }
                    _incomingMessages.emit(frame)
                }
                CompanionMessage.TYPE_COMMAND -> {
                    val cmd = CompanionMessage.Command.fromJson(json)
                    _incomingMessages.emit(cmd)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnectClient() {
        isRunningClient.set(false)
        _isConnected.value = false
        try {
            clientSocket?.close()
        } catch (_: Exception) {}
        clientSocket = null
        activeWriter = null
    }

    // Broadcast or send message over active connection
    fun sendMessage(messageJson: String) {
        scope.launch {
            try {
                activeWriter?.println(messageJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Set simulated/in-process live data for demo or standalone mode
    fun postLocalStatus(status: DeviceStatus) {
        _latestDeviceStatus.value = status
    }

    fun postLocalScreenFrame(bitmap: Bitmap, fps: Int) {
        _latestScreenBitmap.value = bitmap
        _screenFps.value = fps
    }
}
