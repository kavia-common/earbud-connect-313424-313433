package org.example.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.example.app.model.DeviceStatus
import org.example.app.repo.AppRepository

class SharedDeviceViewModel(
    private val repo: AppRepository
) : ViewModel() {

    val status: StateFlow<DeviceStatus> = repo.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.status.value)

    fun start() {
        // No-op; keeping as hook for future.
    }
}
