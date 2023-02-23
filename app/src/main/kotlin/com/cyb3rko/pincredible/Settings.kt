package com.cyb3rko.pincredible

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.cyb3rko.pincredible.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal const val KEY_BUTTON_RANDOMIZER = "button_randomizer"
internal const val KEY_ADAPTIVE_COLORS = "adaptive_colors"

internal class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager
            .beginTransaction()
            .replace(binding.settingsContainer.id, SettingsFragment())
            .commit()

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }

    internal class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val adaptiveColors = findPreference<SwitchPreferenceCompat>(KEY_ADAPTIVE_COLORS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                adaptiveColors?.isEnabled = true
            } else {
                adaptiveColors?.setSummary(R.string.preference_item_material_you_note)
                return
            }

            adaptiveColors?.onPreferenceChangeListener = OnPreferenceChangeListener { _, _ ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.dialog_restart_title))
                    .setMessage(getString(R.string.dialog_restart_message))
                    .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                        val packageManager = requireActivity().packageManager
                        val packageName = requireActivity().packageName
                        val intent = packageManager.getLaunchIntentForPackage(packageName)
                        val componentName = intent!!.component
                        val mainIntent = Intent.makeRestartActivityTask(componentName)
                        startActivity(mainIntent)
                        Runtime.getRuntime().exit(0)
                    }
                    .setNegativeButton(getString(R.string.dialog_restart_button2), null)
                    .show()

                true
            }
        }
    }
}
