package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityStorage
import com.example.data.remote.CompanionConnectionManager
import com.example.data.repository.DeviceRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.DeviceRole
import com.example.domain.model.DeviceStatus
import com.example.service.BatteryManagerService
import com.example.service.CompanionConnectionService
import com.example.service.ScreenCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    val securityStorage = SecurityStorage.getInstance(context)
    private val database = AppDatabase.getInstance(context)
    val deviceRepository = DeviceRepository(context, securityStorage)
    val notificationRepository = NotificationRepository(context, database, securityStorage)
    val usageStatsRepository = UsageStatsRepository(context, database)

    private val _deviceRole = MutableStateFlow(getInitialRole())
    val deviceRole: StateFlow<DeviceRole> = _deviceRole.asStateFlow()

    private val _isAppLocked = MutableStateFlow(securityStorage.isAppLockEnabled() && securityStorage.hasPinSet())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _deviceStatus = MutableStateFlow(deviceRepository.getLocalDeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _pairingCode = MutableStateFlow(securityStorage.getCurrentPairingCode() ?: "")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    private val _connectionMessage = MutableStateFlow("")
    val connectionMessage: StateFlow<String> = _connectionMessage.asStateFlow()

    val isConnected: StateFlow<Boolean> = CompanionConnectionManager.isConnected
    val connectedDeviceName: StateFlow<String> = CompanionConnectionManager.connectedDeviceName
    val isScreenSharing: StateFlow<Boolean> = securityStorage.isScreenSharingActiveFlow
    val screenFps: StateFlow<Int> = CompanionConnectionManager.screenFps

    val batteryManagerService = BatteryManagerService.getInstance(context)
    val batteryState: StateFlow<com.example.domain.model.BatteryState> = batteryManagerService.batteryState
    val batteryPercentage: StateFlow<Int> = batteryManagerService.batteryPercentage
    val chargingState: StateFlow<com.example.domain.model.BatteryChargingState> = batteryManagerService.chargingState
    val isCharging: StateFlow<Boolean> = batteryManagerService.isCharging
    val batteryHealth: StateFlow<String> = batteryManagerService.healthStatus

    init {
        refreshDeviceStatus()
        // Collect real-time battery changes on local monitored device
        viewModelScope.launch {
            batteryManagerService.batteryState.collect { bState ->
                if (_deviceRole.value == DeviceRole.MONITORED) {
                    _deviceStatus.value = _deviceStatus.value.copy(
                        batteryPercentage = bState.percentage,
                        isCharging = bState.isCharging,
                        batteryHealth = bState.healthStatus,
                        chargingType = bState.plugType.displayText
                    )
                }
            }
        }
        // If role is configured, start the companion service
        if (_deviceRole.value != DeviceRole.UNCONFIGURED) {
            try {
                CompanionConnectionService.start(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getInitialRole(): DeviceRole {
        return when (securityStorage.getDeviceRole()) {
            SecurityStorage.ROLE_MONITORED -> DeviceRole.MONITORED
            SecurityStorage.ROLE_VIEWER -> DeviceRole.VIEWER
            else -> DeviceRole.UNCONFIGURED
        }
    }

    fun selectRole(role: DeviceRole) {
        _deviceRole.value = role
        val roleStr = when (role) {
            DeviceRole.MONITORED -> SecurityStorage.ROLE_MONITORED
            DeviceRole.VIEWER -> SecurityStorage.ROLE_VIEWER
            DeviceRole.UNCONFIGURED -> SecurityStorage.ROLE_UNCONFIGURED
        }
        securityStorage.setDeviceRole(roleStr)
        if (role != DeviceRole.UNCONFIGURED) {
            try {
                CompanionConnectionService.start(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (role == DeviceRole.MONITORED && _pairingCode.value.isBlank()) {
                generatePairingCode()
            }
        }
    }

    fun generatePairingCode(): String {
        val code = securityStorage.generatePairingCode()
        _pairingCode.value = code
        // Ensure server is running for monitored device
        CompanionConnectionManager.startServer { code }
        return code
    }

    fun connectToMonitoredPhone(ip: String, port: Int = CompanionConnectionManager.DEFAULT_PORT, code: String) {
        _connectionMessage.value = "Connecting to $ip:$port..."
        CompanionConnectionManager.connectToMonitoredDevice(
            host = ip,
            port = port,
            pairingCode = code,
            myDeviceName = securityStorage.getDeviceName()
        ) { success, msg ->
            _connectionMessage.value = msg
            if (success) {
                securityStorage.savePairedCompanion(
                    id = "monitored-device",
                    name = CompanionConnectionManager.connectedDeviceName.value,
                    ip = ip,
                    port = port,
                    token = "paired_token"
                )
            }
        }
    }

    fun refreshDeviceStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = deviceRepository.getLocalDeviceStatus()
            val notifGranted = notificationRepository.isNotificationAccessGranted()
            val usageGranted = usageStatsRepository.hasUsageAccess()
            _deviceStatus.value = status.copy(
                hasNotificationAccess = notifGranted,
                hasUsageAccess = usageGranted,
                isScreenSharing = securityStorage.isScreenSharingActiveFlow.value
            )
        }
    }

    fun startScreenCapture(resultCode: Int, data: Intent) {
        val intent = ScreenCaptureService.startServiceIntent(context, resultCode, data)
        context.startService(intent)
    }

    fun stopScreenCapture() {
        val intent = ScreenCaptureService.stopServiceIntent(context)
        context.startService(intent)
    }

    fun unlockAppWithPin(pin: String): Boolean {
        if (securityStorage.verifyPin(pin)) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun unlockAppWithBiometric() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (securityStorage.isAppLockEnabled() && securityStorage.hasPinSet()) {
            _isAppLocked.value = true
        }
    }

    fun disconnect() {
        CompanionConnectionManager.disconnectClient()
        securityStorage.disconnectPairedDevice()
        _connectionMessage.value = "Disconnected"
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            securityStorage.clearAllData()
            _deviceRole.value = DeviceRole.UNCONFIGURED
            _isAppLocked.value = false
        }
    }
}
