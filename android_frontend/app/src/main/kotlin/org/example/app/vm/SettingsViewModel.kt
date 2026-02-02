package org.example.app.vm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.example.app.model.UserSettings
import org.example.app.repo.AppRepository

class SettingsViewModel(
    private val repo: AppRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(repo.getSettings())
    val settings: StateFlow<UserSettings> = _settings

    fun start() {
        _settings.value = repo.getSettings()
    }

    fun update(settings: UserSettings) {
        repo.setSettings(settings)
        _settings.value = settings

        // Re-bootstrap to apply demo mode / auto-connect changes.
        repo.bootstrap()
    }
}
