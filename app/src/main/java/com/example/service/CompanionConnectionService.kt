package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityStorage
import com.example.data.remote.CompanionConnectionManager
import com.example.data.remote.model.CompanionMessage
import com.example.data.repository.DeviceRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UsageStatsRepository
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CompanionConnectionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var securityStorage: SecurityStorage
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var usageStatsRepository: UsageStatsRepository

    override fun onCreate() {
        super.onCreate()
        securityStorage = SecurityStorage.getInstance(applicationContext)
        val db = AppDatabase.getInstance(applicationContext)
        deviceRepository = DeviceRepository(applicationContext, securityStorage)
        notificationRepository = NotificationRepository(applicationContext, db, securityStorage)
        usageStatsRepository = UsageStatsRepository(applicationContext, db)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return START_NOT_STICKY
        }

        try {
            startForegroundWithNotification()
            startCompanionBackgroundSync()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        try {
            val stopIntent = Intent(this, CompanionConnectionService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPending = PendingIntent.getService(
                this,
                10,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val openIntent = Intent(this, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                this,
                11,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val role = securityStorage.getDeviceRole()
            val title = if (role == SecurityStorage.ROLE_MONITORED) {
                "Companion Device Active (Monitored)"
            } else {
                "Companion Device Active (Viewer)"
            }

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Listening and synchronizing with paired companion device.")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stopPending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCompanionBackgroundSync() {
        serviceScope.launch {
            // Start local Server if in monitored role
            if (securityStorage.getDeviceRole() == SecurityStorage.ROLE_MONITORED) {
                CompanionConnectionManager.startServer {
                    securityStorage.getCurrentPairingCode()
                }
            }

            // Sync loop: send telemetry every 5 seconds if connected
            while (isActive) {
                try {
                    val status = deviceRepository.getLocalDeviceStatus()
                    val hasNotif = notificationRepository.isNotificationAccessGranted()
                    val hasUsage = usageStatsRepository.hasUsageAccess()

                    val statusMsg = CompanionMessage.DeviceStatusSync(
                        deviceName = status.deviceName,
                        model = status.model,
                        osVersion = status.osVersion,
                        batteryPercentage = status.batteryPercentage,
                        isCharging = status.isCharging,
                        wifiSsid = status.wifiSsid,
                        ipAddress = status.ipAddress,
                        networkType = status.networkType,
                        isScreenSharing = securityStorage.isScreenSharingActiveFlow.value,
                        notificationAccessGranted = hasNotif,
                        usageAccessGranted = hasUsage,
                        totalNotifications = 0,
                        totalUsageDurationMs = 0L
                    )

                    CompanionConnectionManager.postLocalStatus(
                        status.copy(
                            hasNotificationAccess = hasNotif,
                            hasUsageAccess = hasUsage
                        )
                    )

                    if (CompanionConnectionManager.isConnected.value) {
                        CompanionConnectionManager.sendMessage(statusMsg.toJson())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Companion Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows while companion connection service is active in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ai_companion_sync"
        const val NOTIFICATION_ID = 2002
        const val ACTION_STOP = "com.example.service.action.STOP_SYNC"

        fun start(context: Context) {
            try {
                val intent = Intent(context, CompanionConnectionService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // In Android 14+, ForegroundServiceStartNotAllowedException can occur when background
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, CompanionConnectionService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
