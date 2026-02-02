package org.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.example.app.bluetooth.AndroidBluetoothPlatform
import org.example.app.data.AppPreferences
import org.example.app.repo.AppRepository
import org.example.app.ui.controls.ControlsFragment
import org.example.app.ui.devices.DevicesFragment
import org.example.app.ui.settings.SettingsFragment
import org.example.app.ui.status.StatusFragment
import org.example.app.vm.AppViewModelFactory
import org.example.app.vm.DevicesViewModel
import org.example.app.vm.SettingsViewModel
import org.example.app.vm.SharedDeviceViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var repository: AppRepository

    private val sharedDeviceViewModel: SharedDeviceViewModel by lazy {
        AppViewModelFactory.createSharedDeviceViewModel(this, repository)
    }

    private val devicesViewModel: DevicesViewModel by lazy {
        AppViewModelFactory.createDevicesViewModel(this, repository)
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        AppViewModelFactory.createSettingsViewModel(this, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Build app singletons (lightweight; no DI framework).
        val preferences = AppPreferences(this)
        val platform = AndroidBluetoothPlatform(this)
        repository = AppRepository(preferences, platform)

        setContentView(R.layout.activity_main)

        // Ensure the Bluetooth layer is initialized with preferences (demo mode, auto-connect, etc.)
        repository.bootstrap()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainer, DevicesFragment.newInstance())
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_devices -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragmentContainer, DevicesFragment.newInstance())
                    }
                    true
                }

                R.id.nav_status -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragmentContainer, StatusFragment.newInstance())
                    }
                    true
                }

                R.id.nav_controls -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragmentContainer, ControlsFragment.newInstance())
                    }
                    true
                }

                R.id.nav_settings -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragmentContainer, SettingsFragment.newInstance())
                    }
                    true
                }

                else -> false
            }
        }

        // Wire viewmodels to start observing.
        // (This also ensures state survives fragment replacement within activity lifetime.)
        sharedDeviceViewModel.start()
        devicesViewModel.start()
        settingsViewModel.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.shutdown()
    }
}
