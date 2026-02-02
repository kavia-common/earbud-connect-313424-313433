package org.example.app.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.app.model.ConnectionState
import org.example.app.model.DeviceStatus
import org.example.app.model.DeviceSummary
import kotlin.random.Random

class DemoBluetoothPlatform : BluetoothPlatform {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val demoDevice = DeviceSummary(
        address = "DE:MO:EA:RB:UD:01",
        name = "Demo Earbuds",
        isBle = true
    )

    private val _status = MutableStateFlow(
        DeviceStatus(
            connectedDevice = null,
            connectionState = ConnectionState.DISCONNECTED,
            batteryPercent = null
        )
    )
    override val status: StateFlow<DeviceStatus> = _status

    private val _scanResults = MutableStateFlow<List<DeviceSummary>>(emptyList())
    override val scanResults: StateFlow<List<DeviceSummary>> = _scanResults

    override fun startScan() {
        _status.value = _status.value.copy(connectionState = ConnectionState.SCANNING, lastError = null)
        _scanResults.value = emptyList()
        scope.launch {
            delay(700)
            _scanResults.value = listOf(demoDevice)
            // Demo stays in scanning until user connects.
        }
    }

    override fun stopScan() {
        if (_status.value.connectionState == ConnectionState.SCANNING) {
            _status.value = _status.value.copy(connectionState = ConnectionState.DISCONNECTED)
        }
    }

    override fun connect(device: DeviceSummary) {
        _status.value = _status.value.copy(connectionState = ConnectionState.CONNECTING, connectedDevice = device, lastError = null)
        scope.launch {
            delay(600)
            _status.value = _status.value.copy(connectionState = ConnectionState.CONNECTED, batteryPercent = Random.nextInt(35, 101))
            // Simulate battery drain.
            scope.launch {
                while (_status.value.connectionState == ConnectionState.CONNECTED) {
                    delay(6_000)
                    val current = _status.value.batteryPercent ?: 100
                    val next = (current - Random.nextInt(0, 3)).coerceAtLeast(0)
                    _status.value = _status.value.copy(batteryPercent = next)
                }
            }
        }
    }

    override fun disconnect() {
        _status.value = _status.value.copy(connectionState = ConnectionState.DISCONNECTED, connectedDevice = null, batteryPercent = null)
    }

    override fun reconnectLast(last: DeviceSummary?) {
        if (last != null) connect(last)
    }

    override fun shutdown() {
        // no-op
    }
}
