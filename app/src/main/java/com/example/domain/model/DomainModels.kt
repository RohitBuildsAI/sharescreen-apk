package com.example.domain.model

enum class DeviceRole {
    MONITORED,
    VIEWER,
    UNCONFIGURED
}

data class DeviceStatus(
    val deviceName: String = "Unknown Device",
    val model: String = "Android",
    val osVersion: String = "Android",
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false,
    val batteryHealth: String = "Good",
    val chargingType: String = "Unplugged",
    val wifiSsid: String = "Connected",
    val ipAddress: String = "127.0.0.1",
    val networkType: String = "Wi-Fi",
    val isConnected: Boolean = false,
    val isScreenSharing: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val totalNotifications: Int = 0,
    val totalUsageDurationMs: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class UsageStatItem(
    val packageName: String,
    val appName: String,
    val usageDurationMs: Long,
    val launchCount: Int = 0,
    val lastUsedMs: Long = 0L,
    val firstUsedMs: Long = 0L,
    val percentageOfTotal: Float = 0f
)

data class AiInsight(
    val id: String,
    val title: String,
    val summary: String,
    val detail: String,
    val type: InsightType,
    val actionableTip: String
)

enum class InsightType {
    APP_USAGE,
    NOTIFICATIONS,
    SCREEN_TIME,
    PRODUCTIVITY
}

data class PairingInfo(
    val deviceId: String,
    val deviceName: String,
    val pairingCode: String,
    val ipAddress: String,
    val port: Int
)
