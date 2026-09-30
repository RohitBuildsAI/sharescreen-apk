package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager as SystemBatteryManager
import com.example.domain.model.BatteryChargingState
import com.example.domain.model.BatteryPlugType
import com.example.domain.model.BatteryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicBoolean

class BatteryManagerService(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    val batteryPercentage: StateFlow<Int> = _batteryState
        .map { it.percentage }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = _batteryState.value.percentage
        )

    val chargingState: StateFlow<BatteryChargingState> = _batteryState
        .map { it.chargingState }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = _batteryState.value.chargingState
        )

    val isCharging: StateFlow<Boolean> = _batteryState
        .map { it.isCharging }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = _batteryState.value.isCharging
        )

    val healthStatus: StateFlow<String> = _batteryState
        .map { it.healthStatus }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = _batteryState.value.healthStatus
        )

    private val isRegistered = AtomicBoolean(false)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { parseAndUpdateBattery(it) }
        }
    }

    init {
        // Read sticky battery status immediately upon service creation
        readStickyBatteryStatus()
        startMonitoring()
    }

    fun startMonitoring() {
        if (isRegistered.compareAndSet(false, true)) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            try {
                val sticky = context.registerReceiver(batteryReceiver, filter)
                sticky?.let { parseAndUpdateBattery(it) }
            } catch (e: Exception) {
                isRegistered.set(false)
            }
        }
    }

    fun stopMonitoring() {
        if (isRegistered.compareAndSet(true, false)) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }
    }

    fun refresh(): BatteryState {
        readStickyBatteryStatus()
        return _batteryState.value
    }

    fun getCurrentBatteryState(): BatteryState {
        return _batteryState.value
    }

    private fun readStickyBatteryStatus() {
        try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            intent?.let { parseAndUpdateBattery(it) }
        } catch (_: Exception) {}
    }

    private fun parseAndUpdateBattery(intent: Intent) {
        val level = intent.getIntExtra(SystemBatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(SystemBatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            _batteryState.value.percentage
        }

        val rawStatus = intent.getIntExtra(SystemBatteryManager.EXTRA_STATUS, -1)
        val isCharging = rawStatus == SystemBatteryManager.BATTERY_STATUS_CHARGING ||
                rawStatus == SystemBatteryManager.BATTERY_STATUS_FULL

        val chargingState = when (rawStatus) {
            SystemBatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargingState.CHARGING
            SystemBatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargingState.DISCHARGING
            SystemBatteryManager.BATTERY_STATUS_FULL -> BatteryChargingState.FULL
            SystemBatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargingState.NOT_CHARGING
            else -> if (isCharging) BatteryChargingState.CHARGING else BatteryChargingState.DISCHARGING
        }

        val plugged = intent.getIntExtra(SystemBatteryManager.EXTRA_PLUGGED, 0)
        val plugType = when (plugged) {
            SystemBatteryManager.BATTERY_PLUGGED_AC -> BatteryPlugType.AC
            SystemBatteryManager.BATTERY_PLUGGED_USB -> BatteryPlugType.USB
            SystemBatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPlugType.WIRELESS
            else -> BatteryPlugType.NONE
        }

        val rawHealth = intent.getIntExtra(
            SystemBatteryManager.EXTRA_HEALTH,
            SystemBatteryManager.BATTERY_HEALTH_UNKNOWN
        )
        val healthStatus = when (rawHealth) {
            SystemBatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            SystemBatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
            SystemBatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            SystemBatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            SystemBatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            SystemBatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good"
        }

        val rawTemp = intent.getIntExtra(SystemBatteryManager.EXTRA_TEMPERATURE, 250)
        val tempCelsius = if (rawTemp > 0) rawTemp / 10f else 25.0f

        val voltage = intent.getIntExtra(SystemBatteryManager.EXTRA_VOLTAGE, 4000)

        _batteryState.value = BatteryState(
            percentage = percentage,
            isCharging = isCharging,
            chargingState = chargingState,
            plugType = plugType,
            healthStatus = healthStatus,
            temperatureCelsius = tempCelsius,
            voltageMv = voltage
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: BatteryManagerService? = null

        fun getInstance(context: Context): BatteryManagerService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BatteryManagerService(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
