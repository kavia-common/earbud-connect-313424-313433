package org.example.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.Manifest
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.example.app.model.ConnectionState
import org.example.app.model.DeviceStatus
import org.example.app.model.DeviceSummary
import java.util.UUID

class AndroidBluetoothPlatform(
    private val context: Context
) : BluetoothPlatform {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _status = MutableStateFlow(
        DeviceStatus(
            connectedDevice = null,
            connectionState = ConnectionState.DISCONNECTED,
            batteryPercent = null,
            lastError = null
        )
    )
    override val status: StateFlow<DeviceStatus> = _status

    private val _scanResults = MutableStateFlow<List<DeviceSummary>>(emptyList())
    override val scanResults: StateFlow<List<DeviceSummary>> = _scanResults

    private val btManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val btAdapter: BluetoothAdapter? = btManager?.adapter

    private var bleScannerCallback: ScanCallback? = null
    private var discoveryReceiverRegistered = false

    private var gatt: BluetoothGatt? = null

    override fun startScan() {
        _status.update { it.copy(connectionState = ConnectionState.SCANNING, lastError = null) }
        _scanResults.value = emptyList()

        // BLE scan requires BLUETOOTH_SCAN on Android 12+.
        if (hasScanPermission()) {
            val scanner = btAdapter?.bluetoothLeScanner
            if (scanner != null) {
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        if (result == null) return
                        val device = result.device ?: return
                        val name = safeDeviceName(device)
                        addOrUpdateScanResult(
                            DeviceSummary(
                                address = device.address ?: return,
                                name = name,
                                isBle = true
                            )
                        )
                    }

                    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                        results?.forEach { r ->
                            val device = r.device ?: return@forEach
                            addOrUpdateScanResult(
                                DeviceSummary(
                                    address = device.address ?: return@forEach,
                                    name = safeDeviceName(device),
                                    isBle = true
                                )
                            )
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        _status.update {
                            it.copy(
                                connectionState = ConnectionState.ERROR,
                                lastError = "BLE scan failed: $errorCode"
                            )
                        }
                    }
                }
                bleScannerCallback = callback
                try {
                    scanner.startScan(callback)
                } catch (se: SecurityException) {
                    _status.update {
                        it.copy(connectionState = ConnectionState.ERROR, lastError = "Missing Bluetooth permissions")
                    }
                } catch (t: Throwable) {
                    _status.update {
                        it.copy(connectionState = ConnectionState.ERROR, lastError = "BLE scan error: ${t.message}")
                    }
                }
            }
        } else {
            _status.update {
                it.copy(connectionState = ConnectionState.ERROR, lastError = "Missing Bluetooth scan permission")
            }
        }

        // Start classic discovery (best effort; also requires scan permission on Android 12+).
        startClassicDiscovery()
    }

    override fun stopScan() {
        _status.update { it.copy(connectionState = ConnectionState.DISCONNECTED, lastError = null) }
        stopBleScan()
        stopClassicDiscovery()
    }

    override fun connect(device: DeviceSummary) {
        _status.update { it.copy(connectionState = ConnectionState.CONNECTING, connectedDevice = device, lastError = null) }

        stopBleScan()
        stopClassicDiscovery()

        // Connecting/reading device info requires BLUETOOTH_CONNECT on Android 12+.
        if (!hasConnectPermission()) {
            _status.update { it.copy(connectionState = ConnectionState.ERROR, lastError = "Missing Bluetooth connect permission") }
            return
        }

        try {
            val adapter = btAdapter
            if (adapter == null) {
                _status.update { it.copy(connectionState = ConnectionState.ERROR, lastError = "Bluetooth not available") }
                return
            }

            val remote: BluetoothDevice = adapter.getRemoteDevice(device.address)
            if (device.isBle) {
                connectGatt(remote, device)
            } else {
                // Classic connection is not generally possible without a profile-specific API.
                // We best-effort mark as "connected" if paired/bonded (or after bonding attempt).
                if (remote.bondState != BluetoothDevice.BOND_BONDED) {
                    tryBond(remote)
                }
                _status.update { it.copy(connectionState = ConnectionState.CONNECTED, connectedDevice = device) }
            }
        } catch (se: SecurityException) {
            _status.update { it.copy(connectionState = ConnectionState.ERROR, lastError = "Missing Bluetooth permissions") }
        } catch (t: Throwable) {
            _status.update { it.copy(connectionState = ConnectionState.ERROR, lastError = t.message ?: "Connect error") }
        }
    }

    override fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Throwable) {
        } finally {
            gatt = null
            _status.update { it.copy(connectionState = ConnectionState.DISCONNECTED, connectedDevice = null, batteryPercent = null) }
        }
    }

    override fun reconnectLast(last: DeviceSummary?) {
        if (last == null) return
        connect(last)
    }

    override fun shutdown() {
        stopScan()
        disconnect()
    }

    private fun addOrUpdateScanResult(device: DeviceSummary) {
        _scanResults.update { current ->
            val existingIdx = current.indexOfFirst { it.address == device.address }
            if (existingIdx >= 0) {
                current.toMutableList().also { it[existingIdx] = device }
            } else {
                current + device
            }
        }
    }

    private fun startClassicDiscovery() {
        val adapter = btAdapter ?: return

        // Classic discovery also needs scan permission on Android 12+.
        if (!hasScanPermission()) return

        if (!discoveryReceiverRegistered) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val action = intent?.action ?: return
                    if (action == BluetoothDevice.ACTION_FOUND) {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        if (device != null && device.address != null) {
                            addOrUpdateScanResult(
                                DeviceSummary(
                                    address = device.address,
                                    name = safeDeviceName(device),
                                    isBle = false
                                )
                            )
                        }
                    }
                }
            }
            // Keep reference via context registration; unregistered in stopClassicDiscovery.
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            classicReceiver = receiver
            discoveryReceiverRegistered = true
        }

        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (_: SecurityException) {
            // Missing permission; ignore.
        } catch (_: Throwable) {
            // Ignore; some devices restrict discovery without location enabled.
        }
    }

    private var classicReceiver: BroadcastReceiver? = null

    private fun stopClassicDiscovery() {
        val adapter = btAdapter ?: return
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
        if (discoveryReceiverRegistered) {
            try {
                context.unregisterReceiver(classicReceiver)
            } catch (_: Throwable) {
            } finally {
                classicReceiver = null
                discoveryReceiverRegistered = false
            }
        }
    }

    private fun stopBleScan() {
        val adapter = btAdapter ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        val callback = bleScannerCallback ?: return
        try {
            scanner.stopScan(callback)
        } catch (_: Throwable) {
        } finally {
            bleScannerCallback = null
        }
    }

    private fun tryBond(device: BluetoothDevice) {
        if (!hasConnectPermission()) return
        try {
            device.createBond()
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
    }

    private fun connectGatt(device: BluetoothDevice, summary: DeviceSummary) {
        if (!hasConnectPermission()) return
        try {
            gatt?.close()
        } catch (_: Throwable) {
        }
        gatt = try {
            device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        _status.update {
                            it.copy(
                                connectedDevice = summary,
                                connectionState = ConnectionState.CONNECTED,
                                lastError = null
                            )
                        }
                        try {
                            gatt?.discoverServices()
                        } catch (_: Throwable) {
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        _status.update {
                            it.copy(
                                connectedDevice = null,
                                connectionState = ConnectionState.DISCONNECTED,
                                batteryPercent = null
                            )
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    val batteryService = gatt?.getService(BATTERY_SERVICE_UUID)
                    val levelChar = batteryService?.getCharacteristic(BATTERY_LEVEL_UUID)
                    if (levelChar != null) {
                        try {
                            gatt.readCharacteristic(levelChar)
                        } catch (_: Throwable) {
                            // ignore
                        }
                    } else {
                        _status.update { it.copy(batteryPercent = null) }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
                    if (characteristic == null) return
                    if (characteristic.uuid == BATTERY_LEVEL_UUID) {
                        val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                        _status.update { it.copy(batteryPercent = value) }
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int
                ) {
                    if (characteristic.uuid == BATTERY_LEVEL_UUID && value.isNotEmpty()) {
                        val percent = value[0].toInt() and 0xFF
                        _status.update { it.copy(batteryPercent = percent) }
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE)
        } catch (_: SecurityException) {
            null
        }

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _status.update {
                        it.copy(
                            connectedDevice = summary,
                            connectionState = ConnectionState.CONNECTED,
                            lastError = null
                        )
                    }
                    try {
                        gatt?.discoverServices()
                    } catch (_: Throwable) {
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _status.update {
                        it.copy(
                            connectedDevice = null,
                            connectionState = ConnectionState.DISCONNECTED,
                            batteryPercent = null
                        )
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                val batteryService = gatt?.getService(BATTERY_SERVICE_UUID)
                val levelChar = batteryService?.getCharacteristic(BATTERY_LEVEL_UUID)
                if (levelChar != null) {
                    try {
                        gatt.readCharacteristic(levelChar)
                    } catch (_: Throwable) {
                        // ignore
                    }
                } else {
                    _status.update { it.copy(batteryPercent = null) }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (characteristic == null) return
                if (characteristic.uuid == BATTERY_LEVEL_UUID) {
                    val value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    _status.update { it.copy(batteryPercent = value) }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (characteristic.uuid == BATTERY_LEVEL_UUID && value.isNotEmpty()) {
                    val percent = value[0].toInt() and 0xFF
                    _status.update { it.copy(batteryPercent = percent) }
                }
            }
        // If connectGatt failed due to permission issues, make sure UI reflects it.
        if (gatt == null) {
            _status.update { it.copy(connectionState = ConnectionState.ERROR, lastError = "Missing Bluetooth permissions") }
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 12: scanning may require location; we best-effort allow and catch failures.
            true
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun safeDeviceName(device: BluetoothDevice): String? {
        return if (!hasConnectPermission()) {
            null
        } else {
            try {
                device.name
            } catch (_: SecurityException) {
                null
            }
        }
    }

    private companion object {
        private val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
        private val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
    }
}
