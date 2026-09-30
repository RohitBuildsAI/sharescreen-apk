package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.SecurityStorage
import com.example.data.remote.CompanionConnectionManager
import com.example.data.remote.model.CompanionMessage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val isCapturing = AtomicBoolean(false)
    private lateinit var securityStorage: SecurityStorage

    private var screenWidth = 720
    private var screenHeight = 1280
    private var screenDensity = 320
    private var frameCount = 0L
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var currentFps = 0
    private var framesInSecond = 0

    override fun onCreate() {
        super.onCreate()
        securityStorage = SecurityStorage.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        if (action == ACTION_STOP) {
            stopScreenCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

        if (resultCode != 0 && resultData != null) {
            startForegroundServiceWithNotification()
            startScreenCapture(resultCode, resultData)
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Sharing Active")
            .setContentText("Your screen is being shared with the connected viewer device.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Sharing", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startScreenCapture(resultCode: Int, resultData: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

        if (mediaProjection == null) {
            stopSelf()
            return
        }

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopScreenCapture()
                stopSelf()
            }
        }, null)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        // Adjust dimensions based on quality setting
        val quality = securityStorage.getScreenQuality()
        val scale = when (quality) {
            "LOW" -> 0.35f
            "HIGH" -> 0.65f
            else -> 0.45f
        }

        screenWidth = (metrics.widthPixels * scale).toInt()
        screenHeight = (metrics.heightPixels * scale).toInt()
        // Ensure even numbers for encoders
        if (screenWidth % 2 != 0) screenWidth++
        if (screenHeight % 2 != 0) screenHeight++
        screenDensity = metrics.densityDpi

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "AI_Companion_Capture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        isCapturing.set(true)
        securityStorage.setScreenSharingActive(true)

        // Start frame streaming loop
        startFrameProcessingLoop()
    }

    private fun startFrameProcessingLoop() {
        serviceScope.launch {
            var lastCaptureTime = 0L
            val targetIntervalMs = when (securityStorage.getScreenQuality()) {
                "LOW" -> 150L // ~7 fps
                "HIGH" -> 66L // ~15 fps
                else -> 100L // ~10 fps
            }

            while (isActive && isCapturing.get()) {
                val now = System.currentTimeMillis()
                if (now - lastCaptureTime >= targetIntervalMs) {
                    lastCaptureTime = now
                    captureLatestFrame()
                }
                delay(30)
            }
        }
    }

    private fun captureLatestFrame() {
        val reader = imageReader ?: return
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer: ByteBuffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                val croppedBitmap = if (rowPadding > 0) {
                    Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                } else {
                    bitmap
                }

                // FPS calculation
                framesInSecond++
                val now = System.currentTimeMillis()
                if (now - lastFpsTimestamp >= 1000) {
                    currentFps = framesInSecond
                    framesInSecond = 0
                    lastFpsTimestamp = now
                }

                frameCount++

                // Update local in-memory stream for preview / demo
                CompanionConnectionManager.postLocalScreenFrame(croppedBitmap, currentFps)

                // Compress to JPEG and send over socket if connected
                if (CompanionConnectionManager.isConnected.value) {
                    val baos = ByteArrayOutputStream()
                    val jpegQuality = when (securityStorage.getScreenQuality()) {
                        "LOW" -> 40
                        "HIGH" -> 70
                        else -> 55
                    }
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                    val frameMsg = CompanionMessage.ScreenFrame(
                        base64Data = base64,
                        frameNumber = frameCount,
                        fps = currentFps,
                        width = screenWidth,
                        height = screenHeight
                    )
                    CompanionConnectionManager.sendMessage(frameMsg.toJson())
                }
            }
        } catch (e: Exception) {
            // Ignore temporary frame drops
        } finally {
            image?.close()
        }
    }

    private fun stopScreenCapture() {
        isCapturing.set(false)
        securityStorage.setScreenSharingActive(false)
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopScreenCapture()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Sharing Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows when companion screen sharing is actively streaming"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ai_companion_screen_share"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        fun startServiceIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
        }

        fun stopServiceIntent(context: Context): Intent {
            return Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
