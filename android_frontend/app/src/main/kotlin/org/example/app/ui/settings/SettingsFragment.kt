package org.example.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.fragment.app.Fragment
import org.example.app.R
import org.example.app.model.UserSettings
import org.example.app.ui.shared.RepoAccess
import org.example.app.ui.shared.collectWithLifecycle
import org.example.app.vm.AppViewModelFactory
import org.example.app.vm.SettingsViewModel

class SettingsFragment : Fragment() {

    private val repo by lazy { RepoAccess.repo(requireActivity() as org.example.app.MainActivity) }

    private val settingsViewModel: SettingsViewModel by lazy {
        AppViewModelFactory.createSettingsViewModel(requireActivity(), repo)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val swAuto = view.findViewById<Switch>(R.id.switchAutoConnect)
        val swNotif = view.findViewById<Switch>(R.id.switchNotifications)
        val swDemo = view.findViewById<Switch>(R.id.switchDemoMode)
        val etLabel = view.findViewById<EditText>(R.id.etDeviceLabel)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)

        settingsViewModel.settings.collectWithLifecycle(viewLifecycleOwner) { settings ->
            swAuto.isChecked = settings.autoConnectLast
            swNotif.isChecked = settings.notificationsEnabled
            swDemo.isChecked = settings.demoModeEnabled
            etLabel.setText(settings.savedDeviceLabel)
        }

        btnSave.setOnClickListener {
            val updated = UserSettings(
                autoConnectLast = swAuto.isChecked,
                savedDeviceLabel = etLabel.text?.toString() ?: "",
                notificationsEnabled = swNotif.isChecked,
                demoModeEnabled = swDemo.isChecked
            )
            settingsViewModel.update(updated)
        }
    }

    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
