package edu.uw.ysuzuki1.photogram

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

/**
 * Implements the settings page with the dark/light mode
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val switchDarkMode = findPreference<SwitchPreferenceCompat>("darkmode")

        switchDarkMode?.setOnPreferenceChangeListener{ preference, newValue ->
            if (newValue == true){
                val currentLayout = requireActivity().findViewById<LinearLayout>(R.id.MainActivity)
                currentLayout.setBackgroundColor(requireContext().getColor(R.color.dark))
            } else {
                val currentLayout = requireActivity().findViewById<LinearLayout>(R.id.MainActivity)
                currentLayout.setBackgroundColor(requireContext().getColor(R.color.design_default_color_background))
            }
            true
        }
    }

}
