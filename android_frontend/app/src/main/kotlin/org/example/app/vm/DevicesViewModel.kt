package org.example.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.example.app.model.DeviceSummary
import org.example.app.repo.AppRepository

class DevicesViewModel(
    private val repo: AppRepository
) : ViewModel() {

    val scanResults: StateFlow<List<DeviceSummary>> = repo.scanResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun start() {
        // No-op
    }

    fun startScan() = repo.startScan()
    fun stopScan() = repo.stopScan()
    fun connect(device: DeviceSummary) = repo.connect(device)
    fun disconnect() = repo.disconnect()
}
