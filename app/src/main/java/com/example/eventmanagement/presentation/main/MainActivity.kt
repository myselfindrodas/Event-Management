package com.example.eventmanagement.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.eventmanagement.R
import com.example.eventmanagement.core.theme.ThemePreferences
import com.example.eventmanagement.core.ui.animateEntrance
import com.example.eventmanagement.core.ui.applySystemBarInsets
import com.example.eventmanagement.databinding.ActivityMainBinding
import com.example.eventmanagement.presentation.auth.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences.applySavedTheme()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (!viewModel.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.titleBlock.animateEntrance()

        setupNavigation()
        requestNotificationPermissionIfNeeded()
        viewModel.onLoggedIn()
    }

    private fun setupNavigation() {
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val title = when (destination.id) {
                R.id.dashboardFragment -> getString(R.string.dashboard)
                else -> getString(R.string.events)
            }
            setToolbarTitle(title)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.action_toggle_theme)?.apply {
            setIcon(if (themePreferences.isDark()) R.drawable.ic_sun else R.drawable.ic_moon)
            title = getString(
                if (themePreferences.isDark()) R.string.switch_to_light
                else R.string.switch_to_dark
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_theme -> {
                binding.root.findViewById<View>(R.id.action_toggle_theme)?.animate()
                    ?.rotationBy(180f)
                    ?.setDuration(300L)
                    ?.start()
                binding.root.animate()
                    .alpha(0.4f)
                    .setDuration(160L)
                    .withEndAction { toggleTheme() }
                    .start()
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setToolbarTitle(title: String) {
        val label = binding.tvToolbarTitle
        if (label.text == title) return
        label.animate()
            .alpha(0f)
            .translationY(-10f)
            .setDuration(110L)
            .withEndAction {
                label.text = title
                label.translationY = 10f
                label.animate().alpha(1f).translationY(0f).setDuration(180L).start()
            }
            .start()
    }

    private fun toggleTheme() {
        val current = themePreferences.currentMode()
        val next = when (current) {
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }
        themePreferences.setNightMode(next)
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_logout)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                viewModel.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
