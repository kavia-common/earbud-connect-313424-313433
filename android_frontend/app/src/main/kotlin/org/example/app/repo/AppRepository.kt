package org.example.app.repo

import kotlinx.coroutines.flow.StateFlow
import org.example.app.bluetooth.AndroidBluetoothPlatform
import org.example.app.bluetooth.BluetoothPlatform
import org.example.app.bluetooth.DemoBluetoothPlatform
import org.example.app.data.AppPreferences
import org.example.app.model.DeviceStatus
import org.example.app.model.DeviceSummary
import org.example.app.model.UserSettings

class AppRepository(
    private val prefs: AppPreferences,
    private val androidPlatform: AndroidBluetoothPlatform
) {
    private var platform: BluetoothPlatform = androidPlatform

    val status: StateFlow<DeviceStatus> get() = platform.status
    val scanResults: StateFlow<List<DeviceSummary>> get() = platform.scanResults

    fun getSettings(): UserSettings = prefs.getSettings()
    fun setSettings(settings: UserSettings) = prefs.setSettings(settings)

    fun getLastDevice(): DeviceSummary? = prefs.getLastDevice()
    fun setLastDevice(device: DeviceSummary?) = prefs.setLastDevice(device)

    fun bootstrap() {
        // Choose platform based on settings.
        val settings = prefs.getSettings()
        platform = if (settings.demoModeEnabled) DemoBluetoothPlatform() else androidPlatform

        // Auto-reconnect best effort.
        if (settings.autoConnectLast) {
            platform.reconnectLast(prefs.getLastDevice())
        }
    }

    fun startScan() = platform.startScan()
    fun stopScan() = platform.stopScan()

    fun connect(device: DeviceSummary) {
        platform.connect(device)
        prefs.setLastDevice(device)
    }

    fun disconnect() = platform.disconnect()

    fun shutdown() = platform.shutdown()
}
