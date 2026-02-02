package org.example.app.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.example.app.R
import org.example.app.model.ConnectionState
import org.example.app.ui.shared.RepoAccess
import org.example.app.ui.shared.collectWithLifecycle
import org.example.app.vm.AppViewModelFactory
import org.example.app.vm.SharedDeviceViewModel

class StatusFragment : Fragment() {

    private val repo by lazy { RepoAccess.repo(requireActivity() as org.example.app.MainActivity) }

    private val sharedDeviceViewModel: SharedDeviceViewModel by lazy {
        AppViewModelFactory.createSharedDeviceViewModel(requireActivity(), repo)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvDevice = view.findViewById<TextView>(R.id.tvDevice)
        val tvState = view.findViewById<TextView>(R.id.tvState)
        val tvBattery = view.findViewById<TextView>(R.id.tvBattery)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        sharedDeviceViewModel.status.collectWithLifecycle(viewLifecycleOwner) { status ->
            val deviceName = status.connectedDevice?.name ?: status.connectedDevice?.address ?: getString(R.string.status_unknown)
            tvDevice.text = "${getString(R.string.status_connected_device)}: $deviceName"

            val stateLabel = when (status.connectionState) {
                ConnectionState.CONNECTED -> getString(R.string.common_connected)
                ConnectionState.CONNECTING -> getString(R.string.common_connecting)
                ConnectionState.DISCONNECTED -> getString(R.string.common_disconnected)
                ConnectionState.SCANNING -> "Scanning"
                ConnectionState.ERROR -> getString(R.string.common_error)
            }
            tvState.text = "${getString(R.string.status_connection_state)}: $stateLabel"

            val battery = status.batteryPercent?.let { "$it%" } ?: getString(R.string.status_unknown)
            tvBattery.text = "${getString(R.string.status_battery_level)}: $battery"

            if (status.lastError.isNullOrBlank()) {
                tvError.visibility = View.GONE
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = "Last error: ${status.lastError}"
            }
        }
    }

    companion object {
        fun newInstance(): StatusFragment = StatusFragment()
    }
}
