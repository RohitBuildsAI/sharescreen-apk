package com.example.data.repository

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityStorage
import com.example.data.local.entity.NotificationEntity
import com.example.service.CompanionNotificationListenerService
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val securityStorage: SecurityStorage
) {
    private val notificationDao = database.notificationDao()

    fun getAllNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getAllNotifications()

    fun getRecentNotifications(limit: Int = 50): Flow<List<NotificationEntity>> =
        notificationDao.getRecentNotifications(limit)

    fun searchNotifications(query: String): Flow<List<NotificationEntity>> =
        notificationDao.searchNotifications(query)

    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsByPackage(packageName)

    fun getNotificationsBetween(startTime: Long, endTime: Long): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsBetween(startTime, endTime)

    fun getDistinctApps(): Flow<List<String>> =
        notificationDao.getDistinctApps()

    fun getNotificationCount(): Flow<Int> =
        notificationDao.getCount()

    suspend fun insertNotification(notification: NotificationEntity) {
        if (!securityStorage.isNotificationCollectionPaused()) {
            notificationDao.insertNotification(notification)
        }
    }

    suspend fun insertNotifications(notifications: List<NotificationEntity>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteNotificationById(id)
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    fun isNotificationAccessGranted(): Boolean {
        return try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabledPackages.contains(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun isCollectionPaused(): Boolean = securityStorage.isNotificationCollectionPaused()

    fun setCollectionPaused(paused: Boolean) {
        securityStorage.setNotificationCollectionPaused(paused)
    }
}
