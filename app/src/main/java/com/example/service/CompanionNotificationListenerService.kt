package com.example.service

import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityStorage
import com.example.data.local.entity.NotificationEntity
import com.example.data.remote.CompanionConnectionManager
import com.example.data.remote.model.CompanionMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CompanionNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: AppDatabase
    private lateinit var securityStorage: SecurityStorage
    private lateinit var packageManager: PackageManager

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(applicationContext)
        securityStorage = SecurityStorage.getInstance(applicationContext)
        packageManager = applicationContext.packageManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Respect user pause setting
        if (securityStorage.isNotificationCollectionPaused()) return

        // Avoid logging our own companion notifications
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName.substringAfterLast('.')
        }

        val entity = NotificationEntity(
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            category = sbn.notification.category,
            notificationKey = sbn.key
        )

        serviceScope.launch {
            try {
                val insertedId = database.notificationDao().insertNotification(entity)
                // If paired viewer is connected, forward notification
                if (CompanionConnectionManager.isConnected.value) {
                    val syncItem = CompanionMessage.NotificationItem(
                        id = insertedId,
                        packageName = entity.packageName,
                        appName = entity.appName,
                        title = entity.title,
                        text = entity.text,
                        timestamp = entity.timestamp,
                        category = entity.category
                    )
                    val message = CompanionMessage.NotificationsSync(listOf(syncItem))
                    CompanionConnectionManager.sendMessage(message.toJson())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
