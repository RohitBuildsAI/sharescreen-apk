package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val id: String,
    val deviceName: String,
    val deviceType: String, // "MONITORED" or "VIEWER"
    val pairingStatus: String, // "PAIRED", "DISCONNECTED", "PENDING"
    val lastConnected: Long,
    val ipAddress: String? = null,
    val port: Int = 8765,
    val pairingCode: String? = null
)
