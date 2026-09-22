package com.example.eventmanagement.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.eventmanagement.R
import com.example.eventmanagement.databinding.ActivityMainBinding
import com.example.eventmanagement.ui.auth.AuthViewModel
import com.example.eventmanagement.ui.auth.LoginActivity
import com.example.eventmanagement.ui.dashboard.DashboardFragment
import com.example.eventmanagement.ui.events.EventListFragment
import com.example.eventmanagement.util.ThemePrefs
import com.example.eventmanagement.util.animateEntrance
import com.example.eventmanagement.util.applySystemBarInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels { AuthViewModel.Factory() }
    private var currentDestination = R.id.nav_events

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (!authViewModel.isLoggedIn()) {
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
        requestNotificationPermissionIfNeeded()
        subscribeToRemindersTopic()
        printFcmToken()

        if (savedInstanceState == null) {
            openFragment(EventListFragment(), getString(R.string.events))
            binding.bottomNav.selectedItemId = R.id.nav_events
        } else {
            currentDestination = savedInstanceState.getInt(KEY_DESTINATION, R.id.nav_events)
            binding.tvToolbarTitle.text = getString(
                if (currentDestination == R.id.nav_dashboard) R.string.dashboard else R.string.events
            )
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_events -> {
                    if (currentDestination != R.id.nav_events) {
                        openFragment(EventListFragment(), getString(R.string.events))
                    }
                    true
                }
                R.id.nav_dashboard -> {
                    if (currentDestination != R.id.nav_dashboard) {
                        openFragment(DashboardFragment(), getString(R.string.dashboard))
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.action_toggle_theme)?.apply {
            setIcon(if (ThemePrefs.isDark(this@MainActivity)) R.drawable.ic_sun else R.drawable.ic_moon)
            title = getString(
                if (ThemePrefs.isDark(this@MainActivity)) R.string.switch_to_light
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

    private fun openFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_enter,
                R.anim.fragment_exit,
                R.anim.fragment_enter,
                R.anim.fragment_exit
            )
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        setToolbarTitle(title)
        currentDestination = when (fragment) {
            is DashboardFragment -> R.id.nav_dashboard
            else -> R.id.nav_events
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
        val current = ThemePrefs.currentMode(this)
        val next = when (current) {
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }
        ThemePrefs.setNightMode(this, next)
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_logout)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                authViewModel.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_DESTINATION, currentDestination)
        super.onSaveInstanceState(outState)
    }

    private fun subscribeToRemindersTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("event_reminders")
    }

    private fun printFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token: $token")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to get FCM token", error)
            }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val TAG = "FCM"
        private const val KEY_DESTINATION = "current_destination"
    }
}
