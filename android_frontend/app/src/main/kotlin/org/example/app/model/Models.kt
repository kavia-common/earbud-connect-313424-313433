package org.example.app.model

/**
 * A lightweight representation of a Bluetooth device for UI/persistence.
 */
data class DeviceSummary(
    val address: String,
    val name: String?,
    val isBle: Boolean
)

/**
 * Connection state shown in UI.
 */
enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Snapshot of the currently connected device and its best-effort capabilities.
 */
data class DeviceStatus(
    val connectedDevice: DeviceSummary?,
    val connectionState: ConnectionState,
    val batteryPercent: Int?, // null means unknown/not supported
    val lastError: String? = null
)

/**
 * Settings stored locally (SharedPreferences).
 */
data class UserSettings(
    val autoConnectLast: Boolean,
    val savedDeviceLabel: String,
    val notificationsEnabled: Boolean,
    val demoModeEnabled: Boolean
)
