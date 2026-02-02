package org.example.app.data

import android.content.Context
import android.content.SharedPreferences
import org.example.app.model.DeviceSummary
import org.example.app.model.UserSettings

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): UserSettings {
        return UserSettings(
            autoConnectLast = prefs.getBoolean(KEY_AUTO_CONNECT, true),
            savedDeviceLabel = prefs.getString(KEY_DEVICE_LABEL, "") ?: "",
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, false),
            demoModeEnabled = prefs.getBoolean(KEY_DEMO_MODE, true)
        )
    }

    fun setSettings(settings: UserSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_CONNECT, settings.autoConnectLast)
            .putString(KEY_DEVICE_LABEL, settings.savedDeviceLabel)
            .putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            .putBoolean(KEY_DEMO_MODE, settings.demoModeEnabled)
            .apply()
    }

    fun getLastDevice(): DeviceSummary? {
        val address = prefs.getString(KEY_LAST_ADDRESS, null) ?: return null
        val name = prefs.getString(KEY_LAST_NAME, null)
        val isBle = prefs.getBoolean(KEY_LAST_IS_BLE, false)
        return DeviceSummary(address = address, name = name, isBle = isBle)
    }

    fun setLastDevice(device: DeviceSummary?) {
        val editor = prefs.edit()
        if (device == null) {
            editor.remove(KEY_LAST_ADDRESS)
                .remove(KEY_LAST_NAME)
                .remove(KEY_LAST_IS_BLE)
        } else {
            editor.putString(KEY_LAST_ADDRESS, device.address)
                .putString(KEY_LAST_NAME, device.name)
                .putBoolean(KEY_LAST_IS_BLE, device.isBle)
        }
        editor.apply()
    }

    private companion object {
        private const val PREFS_NAME = "earbud_connect_prefs"

        private const val KEY_AUTO_CONNECT = "auto_connect_last"
        private const val KEY_DEVICE_LABEL = "device_label"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_DEMO_MODE = "demo_mode_enabled"

        private const val KEY_LAST_ADDRESS = "last_device_address"
        private const val KEY_LAST_NAME = "last_device_name"
        private const val KEY_LAST_IS_BLE = "last_device_is_ble"
    }
}
