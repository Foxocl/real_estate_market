package by.kotlin.salesAppartment

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var tvLanguage: TextView
    private lateinit var tvTheme: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        tvLanguage = view.findViewById(R.id.tvLanguage)
        tvTheme = view.findViewById(R.id.tvTheme)

        loadSettings()

        // Click listeners on the whole rows
        view.findViewById<View>(R.id.layoutLanguage).setOnClickListener { showLanguageDialog() }
        view.findViewById<View>(R.id.layoutTheme).setOnClickListener { showThemeDialog() }

        return view
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Русский")
        val currentLang = tvLanguage.text.toString()
        val checkedItem = languages.indexOfFirst { it.equals(currentLang, ignoreCase = true) }.takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.language_label))
            .setSingleChoiceItems(languages, checkedItem) { dialog: DialogInterface, which: Int ->
                val selected = languages[which]
                tvLanguage.text = selected
                saveLanguage(selected)
                applyLanguage()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Light", "Dark")
        val currentTheme = tvTheme.text.toString()
        val checkedItem = themes.indexOfFirst { it.equals(currentTheme, ignoreCase = true) }.takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.theme_label))
            .setSingleChoiceItems(themes, checkedItem) { dialog: DialogInterface, which: Int ->
                val selected = themes[which]
                tvTheme.text = selected
                saveTheme(selected)
                applyTheme(selected)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveLanguage(lang: String) {
        val prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language", lang).apply()
    }

    private fun saveTheme(theme: String) {
        val prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("theme", theme).apply()
    }

    private fun loadSettings() {
        val prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        tvLanguage.text = prefs.getString("language", getString(R.string.language_placeholder))
        tvTheme.text = prefs.getString("theme", getString(R.string.theme_placeholder))
    }

    private fun applyLanguage() {
        // Recreate activity – the language will be applied in attachBaseContext
        requireActivity().recreate()
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme.lowercase(Locale.ROOT)) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        requireActivity().recreate()
    }
}