package de.tub.affinity3.android.fragments

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import de.tub.affinity3.android.R
import de.tub.affinity3.android.activities.PrivacyRegionsActivity
import de.tub.affinity3.android.classes.DiscoveryServiceManager
import de.tub.affinity3.android.constants.AppConstants

class PrivacyRegionsSettingsFragment : PreferenceFragmentCompat() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.setTitle(R.string.pref_privacy_regions_settings_title)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.fragment_privacy_regions_settings)

        findPreference<Preference>(
            getString(R.string.pref_privacy_regions_key)
        )?.setOnPreferenceClickListener {
            startActivity(
                Intent(activity, PrivacyRegionsActivity::class.java)
            )
            return@setOnPreferenceClickListener true
        }

        findPreference<CheckBoxPreference>(getString(R.string.pref_privacy_regions_enabled_key))!!.setOnPreferenceChangeListener { _, newValue ->
            val checked = (newValue as Boolean)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
            with(sharedPreferences.edit()){
                putBoolean(getString(R.string.pref_privacy_regions_enabled_key), checked)
                apply()
            }
            return@setOnPreferenceChangeListener true
        }

        findPreference<Preference>(
            getString(R.string.pref_sharing_region_recommendations_key)
        )?.setOnPreferenceClickListener {
            startActivity(
                Intent(activity, PrivacyRegionsActivity::class.java)
                    .putExtra(AppConstants.RECOMMENDATIONS_FLAG_EXTRA, true)
            )
            return@setOnPreferenceClickListener true
        }

        findPreference<CheckBoxPreference>(getString(R.string.pref_context_sensing_key))!!.setOnPreferenceChangeListener { _, newValue ->
            val checked = (newValue as Boolean)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
            with(sharedPreferences.edit()){
                putBoolean(getString(R.string.pref_context_sensing_key), checked)
                apply()
            }

            val bck = DiscoveryServiceManager()
            bck.stopDiscoveryService(requireContext())

            Toast.makeText(context, "Please restart the service to apply settings.", Toast.LENGTH_SHORT).show()

            return@setOnPreferenceChangeListener true
        }
    }
}
