package org.example.app.ui.devices

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.permissions.PermissionHelper
import org.example.app.repo.AppRepository
import org.example.app.ui.shared.collectWithLifecycle
import org.example.app.vm.AppViewModelFactory
import org.example.app.vm.DevicesViewModel
import org.example.app.vm.SettingsViewModel

class DevicesFragment : Fragment() {

    private val repo: AppRepository by lazy { (requireActivity() as org.example.app.MainActivity).let { it.javaClass; getRepo(it) } }

    private val devicesViewModel: DevicesViewModel by lazy {
        AppViewModelFactory.createDevicesViewModel(requireActivity(), repo)
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        AppViewModelFactory.createSettingsViewModel(requireActivity(), repo)
    }

    private lateinit var permissionsPanel: LinearLayout
    private lateinit var demoBanner: TextView
    private lateinit var emptyText: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: DevicesAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissionUi()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        permissionsPanel = view.findViewById(R.id.permissionsPanel)
        demoBanner = view.findViewById(R.id.demoBanner)
        emptyText = view.findViewById(R.id.emptyText)
        recycler = view.findViewById(R.id.recyclerDevices)

        adapter = DevicesAdapter { device ->
            devicesViewModel.connect(device)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<Button>(R.id.btnScan).setOnClickListener {
            if (!PermissionHelper.hasBluetoothPermissions(requireContext()) && !settingsViewModel.settings.value.demoModeEnabled) {
                requestBluetoothPermissions()
            } else {
                devicesViewModel.startScan()
            }
        }

        view.findViewById<Button>(R.id.btnStop).setOnClickListener {
            devicesViewModel.stopScan()
        }

        view.findViewById<Button>(R.id.btnGrantPermissions).setOnClickListener {
            requestBluetoothPermissions()
        }

        refreshPermissionUi()

        // Observe scan results
        devicesViewModel.scanResults.collectWithLifecycle(viewLifecycleOwner) { list ->
            adapter.submit(list)
            emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        settingsViewModel.settings.collectWithLifecycle(viewLifecycleOwner) { settings ->
            demoBanner.visibility = if (settings.demoModeEnabled) View.VISIBLE else View.GONE
            refreshPermissionUi()
        }
    }

    private fun refreshPermissionUi() {
        val demoMode = settingsViewModel.settings.value.demoModeEnabled
        val has = PermissionHelper.hasBluetoothPermissions(requireContext())
        permissionsPanel.visibility = if (!demoMode && !has) View.VISIBLE else View.GONE
    }

    private fun requestBluetoothPermissions() {
        val perms = PermissionHelper.requiredBluetoothPermissions()
        // If already granted, do nothing
        val allGranted = perms.all { p ->
            ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) return
        permissionLauncher.launch(perms)
    }

    private fun getRepo(activity: org.example.app.MainActivity): AppRepository {
        // Accessing the private field directly isn't possible; use a known-cast pattern:
        // the repository is held by MainActivity instance; we expose it through a package-private accessor.
        // Here we call that accessor via an extension defined in RepoAccess.
        return org.example.app.ui.shared.RepoAccess.repo(activity)
    }

    companion object {
        fun newInstance(): DevicesFragment = DevicesFragment()
    }
}
