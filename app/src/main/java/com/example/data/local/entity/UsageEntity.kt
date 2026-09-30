package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val date: String, // format yyyy-MM-dd
    val usageDuration: Long, // milliseconds
    val lastUsed: Long,
    val launchCount: Int = 0
)
