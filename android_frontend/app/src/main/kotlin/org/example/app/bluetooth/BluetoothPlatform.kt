package org.example.app.bluetooth

import kotlinx.coroutines.flow.StateFlow
import org.example.app.model.DeviceStatus
import org.example.app.model.DeviceSummary

/**
 * Platform abstraction for Bluetooth operations.
 *
 * Real implementation uses Android Bluetooth APIs; demo implementation simulates a device.
 */
interface BluetoothPlatform {

    val status: StateFlow<DeviceStatus>
    val scanResults: StateFlow<List<DeviceSummary>>

    fun startScan()
    fun stopScan()

    fun connect(device: DeviceSummary)
    fun disconnect()

    fun reconnectLast(last: DeviceSummary?)
    fun shutdown()
}
