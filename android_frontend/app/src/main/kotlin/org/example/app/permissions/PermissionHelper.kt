package org.example.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    // PUBLIC_INTERFACE
    /**
     * Returns the runtime permissions needed for scanning/connecting on this Android version.
     */
    fun requiredBluetoothPermissions(): Array<String> {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            // Pre-Android 12: scanning may require location.
            perms += Manifest.permission.ACCESS_FINE_LOCATION
        }
        return perms.toTypedArray()
    }

    // PUBLIC_INTERFACE
    /**
     * Returns whether all Bluetooth-related runtime permissions are granted for scanning/connecting.
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return requiredBluetoothPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    // PUBLIC_INTERFACE
    /**
     * Returns the notification permission for Android 13+, otherwise null.
     */
    fun optionalNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }
}
