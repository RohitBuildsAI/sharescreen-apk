package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.KeyGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_companion_secure_prefs", Context.MODE_PRIVATE)

    private val _deviceRoleFlow = MutableStateFlow(getDeviceRole())
    val deviceRoleFlow: StateFlow<String> = _deviceRoleFlow.asStateFlow()

    private val _isAppLockedFlow = MutableStateFlow(isAppLockEnabled())
    val isAppLockedFlow: StateFlow<Boolean> = _isAppLockedFlow.asStateFlow()

    private val _isScreenSharingActiveFlow = MutableStateFlow(false)
    val isScreenSharingActiveFlow: StateFlow<Boolean> = _isScreenSharingActiveFlow.asStateFlow()

    init {
        // Ensure device identity and pairing secrets exist
        if (prefs.getString(KEY_DEVICE_ID, null) == null) {
            val newId = UUID.randomUUID().toString()
            val defaultName = Build.MODEL ?: "Android Device"
            prefs.edit()
                .putString(KEY_DEVICE_ID, newId)
                .putString(KEY_DEVICE_NAME, defaultName)
                .putString(KEY_SECRET_KEY, generateSecureRandomHex(32))
                .apply()
        }
    }

    fun getDeviceId(): String {
        return prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    fun getDeviceName(): String {
        return prefs.getString(KEY_DEVICE_NAME, Build.MODEL ?: "My Phone") ?: "My Phone"
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
    }

    fun getDeviceRole(): String {
        return prefs.getString(KEY_DEVICE_ROLE, ROLE_UNCONFIGURED) ?: ROLE_UNCONFIGURED
    }

    fun setDeviceRole(role: String) {
        prefs.edit().putString(KEY_DEVICE_ROLE, role).apply()
        _deviceRoleFlow.value = role
    }

    fun getLocalSecretKey(): String {
        return prefs.getString(KEY_SECRET_KEY, null) ?: generateSecureRandomHex(32).also {
            prefs.edit().putString(KEY_SECRET_KEY, it).apply()
        }
    }

    // 6-digit secure pairing code generation
    fun generatePairingCode(): String {
        val random = SecureRandom()
        val code = String.format("%06d", random.nextInt(1000000))
        prefs.edit().putString(KEY_CURRENT_PAIRING_CODE, code).apply()
        return code
    }

    fun getCurrentPairingCode(): String? {
        return prefs.getString(KEY_CURRENT_PAIRING_CODE, null)
    }

    fun clearPairingCode() {
        prefs.edit().remove(KEY_CURRENT_PAIRING_CODE).apply()
    }

    // Paired companion information
    fun savePairedCompanion(id: String, name: String, ip: String, port: Int, token: String) {
        prefs.edit()
            .putString(KEY_PAIRED_DEVICE_ID, id)
            .putString(KEY_PAIRED_DEVICE_NAME, name)
            .putString(KEY_PAIRED_DEVICE_IP, ip)
            .putInt(KEY_PAIRED_DEVICE_PORT, port)
            .putString(KEY_PAIRED_DEVICE_TOKEN, token)
            .apply()
    }

    fun getPairedDeviceId(): String? = prefs.getString(KEY_PAIRED_DEVICE_ID, null)
    fun getPairedDeviceName(): String? = prefs.getString(KEY_PAIRED_DEVICE_NAME, null)
    fun getPairedDeviceIp(): String? = prefs.getString(KEY_PAIRED_DEVICE_IP, null)
    fun getPairedDevicePort(): Int = prefs.getInt(KEY_PAIRED_DEVICE_PORT, 8765)
    fun getPairedDeviceToken(): String? = prefs.getString(KEY_PAIRED_DEVICE_TOKEN, null)

    fun disconnectPairedDevice() {
        prefs.edit()
            .remove(KEY_PAIRED_DEVICE_ID)
            .remove(KEY_PAIRED_DEVICE_NAME)
            .remove(KEY_PAIRED_DEVICE_IP)
            .remove(KEY_PAIRED_DEVICE_PORT)
            .remove(KEY_PAIRED_DEVICE_TOKEN)
            .apply()
    }

    // App Lock (Biometric / PIN)
    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        _isAppLockedFlow.value = enabled
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun setPin(pin: String) {
        val salt = generateSecureRandomHex(16)
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin, salt) == savedHash
    }

    fun hasPinSet(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Monitoring Preferences
    fun isNotificationCollectionPaused(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATIONS_PAUSED, false)

    fun setNotificationCollectionPaused(paused: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_PAUSED, paused).apply()
    }

    fun isAiAnalysisEnabled(): Boolean = prefs.getBoolean(KEY_AI_ENABLED, true)

    fun setAiAnalysisEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
    }

    fun getScreenQuality(): String = prefs.getString(KEY_SCREEN_QUALITY, "MEDIUM") ?: "MEDIUM"

    fun setScreenQuality(quality: String) {
        prefs.edit().putString(KEY_SCREEN_QUALITY, quality).apply()
    }

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun setScreenSharingActive(active: Boolean) {
        _isScreenSharingActiveFlow.value = active
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
        _deviceRoleFlow.value = ROLE_UNCONFIGURED
        _isAppLockedFlow.value = false
        _isScreenSharingActiveFlow.value = false
    }

    private fun generateSecureRandomHex(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val ROLE_UNCONFIGURED = "UNCONFIGURED"
        const val ROLE_MONITORED = "MONITORED"
        const val ROLE_VIEWER = "VIEWER"

        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_DEVICE_ROLE = "device_role"
        private const val KEY_SECRET_KEY = "secret_key"
        private const val KEY_CURRENT_PAIRING_CODE = "current_pairing_code"

        private const val KEY_PAIRED_DEVICE_ID = "paired_device_id"
        private const val KEY_PAIRED_DEVICE_NAME = "paired_device_name"
        private const val KEY_PAIRED_DEVICE_IP = "paired_device_ip"
        private const val KEY_PAIRED_DEVICE_PORT = "paired_device_port"
        private const val KEY_PAIRED_DEVICE_TOKEN = "paired_device_token"

        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"

        private const val KEY_NOTIFICATIONS_PAUSED = "notifications_paused"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_SCREEN_QUALITY = "screen_quality"
        private const val KEY_THEME_MODE = "theme_mode"

        @Volatile
        private var instance: SecurityStorage? = null

        fun getInstance(context: Context): SecurityStorage {
            return instance ?: synchronized(this) {
                val inst = SecurityStorage(context.applicationContext)
                instance = inst
                inst
            }
        }
    }
}
