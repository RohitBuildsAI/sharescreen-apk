package com.example.domain.model

enum class BatteryChargingState {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN;

    val displayText: String
        get() = when (this) {
            CHARGING -> "Charging"
            DISCHARGING -> "Discharging"
            FULL -> "Fully Charged"
            NOT_CHARGING -> "Not Charging"
            UNKNOWN -> "Unknown"
        }
}

enum class BatteryPlugType {
    NONE,
    AC,
    USB,
    WIRELESS;

    val displayText: String
        get() = when (this) {
            NONE -> "Unplugged"
            AC -> "AC Adapter"
            USB -> "USB Cable"
            WIRELESS -> "Wireless Dock"
        }
}

data class BatteryState(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val chargingState: BatteryChargingState = BatteryChargingState.UNKNOWN,
    val plugType: BatteryPlugType = BatteryPlugType.NONE,
    val healthStatus: String = "Good",
    val temperatureCelsius: Float = 25.0f,
    val voltageMv: Int = 4000
)
