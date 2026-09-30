package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.example.data.local.SecurityStorage
import com.example.domain.model.DeviceStatus
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

import com.example.service.BatteryManagerService

class DeviceRepository(
    private val context: Context,
    private val securityStorage: SecurityStorage,
    private val batteryManagerService: BatteryManagerService = BatteryManagerService.getInstance(context)
) {

    fun getLocalDeviceStatus(): DeviceStatus {
        val battery = batteryManagerService.getCurrentBatteryState()
        val network = getNetworkInfo()

        return DeviceStatus(
            deviceName = securityStorage.getDeviceName(),
            model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            batteryPercentage = battery.percentage,
            isCharging = battery.isCharging,
            batteryHealth = battery.healthStatus,
            chargingType = battery.plugType.displayText,
            wifiSsid = network.ssid,
            ipAddress = network.ipAddress,
            networkType = network.type,
            isConnected = true,
            isScreenSharing = securityStorage.isScreenSharingActiveFlow.value,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun getBatteryInfo(): Pair<Int, Boolean> {
        val state = batteryManagerService.getCurrentBatteryState()
        return Pair(state.percentage, state.isCharging)
    }

    data class NetworkInfoResult(
        val type: String,
        val ssid: String,
        val ipAddress: String
    )

    fun getNetworkInfo(): NetworkInfoResult {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)

            var type = "Offline"
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    type = "Wi-Fi"
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    type = "Mobile Network"
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    type = "Ethernet"
                }
            }

            var ssid = "Not Connected"
            if (type == "Wi-Fi") {
                try {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    @Suppress("DEPRECATION")
                    val info = wm?.connectionInfo
                    val rawSsid = info?.ssid?.replace("\"", "")
                    if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                        ssid = rawSsid
                    } else {
                        ssid = "Connected Wi-Fi"
                    }
                } catch (_: Exception) {
                    ssid = "Connected Wi-Fi"
                }
            }

            val ip = getLocalIpAddress()

            NetworkInfoResult(type, ssid, ip)
        } catch (e: Exception) {
            NetworkInfoResult("Wi-Fi", "Connected", "127.0.0.1")
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    data class StorageInfo(
        val freeBytes: Long,
        val totalBytes: Long,
        val formattedFree: String,
        val formattedTotal: String,
        val percentageFree: Int
    )

    fun getStorageInfo(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val percentage = if (totalBytes > 0) ((freeBytes.toFloat() / totalBytes) * 100).toInt() else 0

            StorageInfo(
                freeBytes = freeBytes,
                totalBytes = totalBytes,
                formattedFree = formatBytes(freeBytes),
                formattedTotal = formatBytes(totalBytes),
                percentageFree = percentage
            )
        } catch (e: Exception) {
            StorageInfo(0, 0, "Unknown", "Unknown", 0)
        }
    }

    fun getUptimeFormatted(): String {
        val millis = SystemClock.elapsedRealtime()
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        return "${hours}h ${minutes}m"
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return String.format(Locale.getDefault(), "%.1f GB", gb)
    }
}
