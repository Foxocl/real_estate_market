package by.kotlin.salesAppartment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import by.kotlin.salesAppartment.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentFragmentId: Int = R.id.catalog

    companion object {
        private const val PREFS_SETTINGS = "settings_prefs"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_CURRENT_FRAGMENT = "current_fragment_id"
        private const val REQUEST_NOTIFICATION_PERMISSION = 101
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateBaseContextLocale(newBase))
    }

    private fun updateBaseContextLocale(context: Context): Context {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        val language = prefs.getString(KEY_LANGUAGE, null)
        val locale = when (language) {
            "Русский" -> Locale("ru")
            else -> Locale("en")
        }
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before any UI is created
        applySavedTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState != null) {
            currentFragmentId = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, R.id.catalog)
            val fragment = getFragment(currentFragmentId)
            replaceFragment(fragment, currentFragmentId, false)
            binding.bottomNavigationView.selectedItemId = currentFragmentId
        } else {
            replaceFragment(CatalogFragment(), R.id.catalog, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        val prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
        if (prefs.getBoolean("notifications_enabled", false)) {
            NotificationWorker.schedule(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_FRAGMENT, currentFragmentId)
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
        val theme = prefs.getString(KEY_THEME, "Light")
        val mode = when (theme?.lowercase(Locale.ROOT)) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            replaceFragment(getFragment(item.itemId), item.itemId, true)
            true
        }
    }

    private fun getFragment(menuItemId: Int): Fragment {
        return when (menuItemId) {
            R.id.catalog -> CatalogFragment()
            R.id.newitem -> NewItemFragment()
            R.id.profile -> ProfileFragment()
            R.id.settings -> SettingsFragment()
            else -> CatalogFragment()
        }
    }

    private fun replaceFragment(fragment: Fragment, menuItemId: Int, updateCurrent: Boolean) {
        if (updateCurrent) {
            currentFragmentId = menuItemId
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}