package org.example.app.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.example.app.repo.AppRepository

object AppViewModelFactory {

    // PUBLIC_INTERFACE
    /**
     * Creates a DevicesViewModel tied to the Activity.
     */
    fun createDevicesViewModel(context: Context, repo: AppRepository): DevicesViewModel {
        val provider = ViewModelProvider(context as androidx.fragment.app.FragmentActivity, Factory(repo))
        return provider[DevicesViewModel::class.java]
    }

    // PUBLIC_INTERFACE
    /**
     * Creates a SharedDeviceViewModel tied to the Activity (shared across screens).
     */
    fun createSharedDeviceViewModel(context: Context, repo: AppRepository): SharedDeviceViewModel {
        val provider = ViewModelProvider(context as androidx.fragment.app.FragmentActivity, Factory(repo))
        return provider[SharedDeviceViewModel::class.java]
    }

    // PUBLIC_INTERFACE
    /**
     * Creates a SettingsViewModel tied to the Activity.
     */
    fun createSettingsViewModel(context: Context, repo: AppRepository): SettingsViewModel {
        val provider = ViewModelProvider(context as androidx.fragment.app.FragmentActivity, Factory(repo))
        return provider[SettingsViewModel::class.java]
    }

    private class Factory(private val repo: AppRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return when {
                modelClass.isAssignableFrom(DevicesViewModel::class.java) -> DevicesViewModel(repo) as T
                modelClass.isAssignableFrom(SharedDeviceViewModel::class.java) -> SharedDeviceViewModel(repo) as T
                modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repo) as T
                else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
            }
        }
    }
}
