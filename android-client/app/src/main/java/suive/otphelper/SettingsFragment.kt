package suive.otphelper

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceFragmentCompat
import java.net.URL

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("server_url")?.onPreferenceChangeListener =
            OnPreferenceChangeListener { _, newValue ->
                try {
                    URL(newValue.toString())
                    true
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Provided value is not a valid URL: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }
            }
    }
}