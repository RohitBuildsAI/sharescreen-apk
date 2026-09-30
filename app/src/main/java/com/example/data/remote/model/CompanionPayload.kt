package com.example.data.remote.model

import org.json.JSONArray
import org.json.JSONObject

sealed class CompanionMessage(val type: String) {

    data class HandshakeRequest(
        val deviceId: String,
        val deviceName: String,
        val role: String,
        val pairingCode: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CompanionMessage(TYPE_HANDSHAKE_REQUEST) {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", TYPE_HANDSHAKE_REQUEST)
                put("deviceId", deviceId)
                put("deviceName", deviceName)
                put("role", role)
                put("pairingCode", pairingCode)
                put("timestamp", timestamp)
            }.toString()
        }

        companion object {
            fun fromJson(json: JSONObject): HandshakeRequest {
                return HandshakeRequest(
                    deviceId = json.optString("deviceId", ""),
                    deviceName = json.optString("deviceName", "Unknown"),
                    role = json.optString("role", "MONITORED"),
                    pairingCode = json.optString("pairingCode", ""),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
            }
        }
    }

    data class HandshakeResponse(
        val success: Boolean,
        val deviceId: String,
        val deviceName: String,
        val token: String,
        val message: String
    ) : CompanionMessage(TYPE_HANDSHAKE_RESPONSE) {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", TYPE_HANDSHAKE_RESPONSE)
                put("success", success)
                put("deviceId", deviceId)
                put("deviceName", deviceName)
                put("token", token)
                put("message", message)
            }.toString()
        }

        companion object {
            fun fromJson(json: JSONObject): HandshakeResponse {
                return HandshakeResponse(
                    success = json.optBoolean("success", false),
                    deviceId = json.optString("deviceId", ""),
                    deviceName = json.optString("deviceName", ""),
                    token = json.optString("token", ""),
                    message = json.optString("message", "")
                )
            }
        }
    }

    data class DeviceStatusSync(
        val deviceName: String,
        val model: String,
        val osVersion: String,
        val batteryPercentage: Int,
        val isCharging: Boolean,
        val wifiSsid: String,
        val ipAddress: String,
        val networkType: String,
        val isScreenSharing: Boolean,
        val notificationAccessGranted: Boolean,
        val usageAccessGranted: Boolean,
        val totalNotifications: Int,
        val totalUsageDurationMs: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : CompanionMessage(TYPE_DEVICE_STATUS_SYNC) {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", TYPE_DEVICE_STATUS_SYNC)
                put("deviceName", deviceName)
                put("model", model)
                put("osVersion", osVersion)
                put("batteryPercentage", batteryPercentage)
                put("isCharging", isCharging)
                put("wifiSsid", wifiSsid)
                put("ipAddress", ipAddress)
                put("networkType", networkType)
                put("isScreenSharing", isScreenSharing)
                put("notificationAccessGranted", notificationAccessGranted)
                put("usageAccessGranted", usageAccessGranted)
                put("totalNotifications", totalNotifications)
                put("totalUsageDurationMs", totalUsageDurationMs)
                put("timestamp", timestamp)
            }.toString()
        }

        companion object {
            fun fromJson(json: JSONObject): DeviceStatusSync {
                return DeviceStatusSync(
                    deviceName = json.optString("deviceName", "Android Device"),
                    model = json.optString("model", "Android"),
                    osVersion = json.optString("osVersion", "14"),
                    batteryPercentage = json.optInt("batteryPercentage", 100),
                    isCharging = json.optBoolean("isCharging", false),
                    wifiSsid = json.optString("wifiSsid", "Wi-Fi"),
                    ipAddress = json.optString("ipAddress", ""),
                    networkType = json.optString("networkType", "Wi-Fi"),
                    isScreenSharing = json.optBoolean("isScreenSharing", false),
                    notificationAccessGranted = json.optBoolean("notificationAccessGranted", false),
                    usageAccessGranted = json.optBoolean("usageAccessGranted", false),
                    totalNotifications = json.optInt("totalNotifications", 0),
                    totalUsageDurationMs = json.optLong("totalUsageDurationMs", 0L),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
            }
        }
    }

    data class NotificationItem(
        val id: Long,
        val packageName: String,
        val appName: String,
        val title: String,
        val text: String,
        val timestamp: Long,
        val category: String? = null
    ) {
        fun toJsonObject(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("packageName", packageName)
                put("appName", appName)
                put("title", title)
                put("text", text)
                put("timestamp", timestamp)
                if (category != null) put("category", category)
            }
        }

        companion object {
            fun fromJsonObject(json: JSONObject): NotificationItem {
                return NotificationItem(
                    id = json.optLong("id", 0L),
                    packageName = json.optString("packageName", ""),
                    appName = json.optString("appName", ""),
                    title = json.optString("title", ""),
                    text = json.optString("text", ""),
                    timestamp = json.optLong("timestamp", 0L),
                    category = if (json.has("category")) json.getString("category") else null
                )
            }
        }
    }

    data class NotificationsSync(
        val notifications: List<NotificationItem>
    ) : CompanionMessage(TYPE_NOTIFICATIONS_SYNC) {
        fun toJson(): String {
            val array = JSONArray()
            notifications.forEach { array.put(it.toJsonObject()) }
            return JSONObject().apply {
                put("type", TYPE_NOTIFICATIONS_SYNC)
                put("notifications", array)
            }.toString()
        }

        companion object {
            fun fromJson(json: JSONObject): NotificationsSync {
                val array = json.optJSONArray("notifications") ?: JSONArray()
                val list = mutableListOf<NotificationItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(NotificationItem.fromJsonObject(obj))
                }
                return NotificationsSync(list)
            }
        }
    }

    data class UsageItem(
        val packageName: String,
        val appName: String,
        val date: String,
        val usageDuration: Long,
        val lastUsed: Long,
        val launchCount: Int
    ) {
        fun toJsonObject(): JSONObject {
            return JSONObject().apply {
                put("packageName", packageName)
                put("appName", appName)
                put("date", date)
                put("usageDuration", usageDuration)
                put("lastUsed", lastUsed)
                put("launchCount", launchCount)
            }
        }

        companion object {
            fun fromJsonObject(json: JSONObject): UsageItem {
                return UsageItem(
                    packageName = json.optString("packageName", ""),
                    appName = json.optString("appName", ""),
                    date = json.optString("date", ""),
                    usageDuration = json.optLong("usageDuration", 0L),
                    lastUsed = json.optLong("lastUsed", 0L),
                    launchCount = json.optInt("launchCount", 0)
                )
            }
        }
    }

    data class UsageSync(
        val items: List<UsageItem>
    ) : CompanionMessage(TYPE_USAGE_SYNC) {
        fun toJson(): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJsonObject()) }
            return JSONObject().apply {
                put("type", TYPE_USAGE_SYNC)
                put("items", array)
            }.toString()
        }

        companion object {
            fun fromJson(json: JSONObject): UsageSync {
                val array = json.optJSONArray("items") ?: JSONArray()
                val list = mutableListOf<UsageItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(UsageItem.fromJsonObject(obj))
                }
                return UsageSync(list)
            }
        }
    }

    data class ScreenFrame(
        val base64Data: String,
        val frameNumber: Long,
        val fps: Int,
        val width: Int,
        val height: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : CompanionMessage(TYPE_SCREEN_FRAME) {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", TYPE_SCREEN_FRAME)
                put("data", base64Data)
                put("frame", frameNumber)
                put("fps", fps)
                put("w", width)
                put("h", height)
                put("ts", timestamp)
            }.toString()
        }

        companion object {
            fun fromJson(json: JSONObject): ScreenFrame {
                return ScreenFrame(
                    base64Data = json.optString("data", ""),
                    frameNumber = json.optLong("frame", 0L),
                    fps = json.optInt("fps", 0),
                    width = json.optInt("w", 0),
                    height = json.optInt("h", 0),
                    timestamp = json.optLong("ts", System.currentTimeMillis())
                )
            }
        }
    }

    data class Command(
        val action: String,
        val payload: String = ""
    ) : CompanionMessage(TYPE_COMMAND) {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", TYPE_COMMAND)
                put("action", action)
                put("payload", payload)
            }.toString()
        }

        companion object {
            fun fromJson(json: JSONObject): Command {
                return Command(
                    action = json.optString("action", ""),
                    payload = json.optString("payload", "")
                )
            }
        }
    }

    companion object {
        const val TYPE_HANDSHAKE_REQUEST = "HANDSHAKE_REQ"
        const val TYPE_HANDSHAKE_RESPONSE = "HANDSHAKE_RESP"
        const val TYPE_DEVICE_STATUS_SYNC = "STATUS_SYNC"
        const val TYPE_NOTIFICATIONS_SYNC = "NOTIF_SYNC"
        const val TYPE_USAGE_SYNC = "USAGE_SYNC"
        const val TYPE_SCREEN_FRAME = "SCREEN_FRAME"
        const val TYPE_COMMAND = "COMMAND"

        const val CMD_START_SCREEN = "START_SCREEN"
        const val CMD_STOP_SCREEN = "STOP_SCREEN"
        const val CMD_REQUEST_REFRESH = "REQUEST_REFRESH"
        const val CMD_PING = "PING"
        const val CMD_PONG = "PONG"
    }
}
