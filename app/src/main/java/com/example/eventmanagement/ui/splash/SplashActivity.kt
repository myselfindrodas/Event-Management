package com.example.eventmanagement.ui.splash

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.eventmanagement.R
import com.example.eventmanagement.data.repository.AuthRepository
import com.example.eventmanagement.databinding.ActivitySplashBinding
import com.example.eventmanagement.ui.auth.LoginActivity
import com.example.eventmanagement.ui.main.MainActivity
import com.example.eventmanagement.util.ThemePrefs
import com.example.eventmanagement.util.applySystemBarInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        playIntro()

        lifecycleScope.launch {
            delay(SPLASH_DURATION_MS)
            navigateOnward()
        }
    }

    private fun playIntro() {
        expandRing(binding.ringOuter, startDelay = 260L, duration = 1100L)
        expandRing(binding.ringInner, startDelay = 120L, duration = 900L)

        binding.logoHolder.apply {
            alpha = 0f
            scaleX = 0.55f
            scaleY = 0.55f
            rotation = -25f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(760L)
                .setInterpolator(OvershootInterpolator(1.8f))
                .start()
        }

        slideUp(binding.tvBrand, delay = 300L)
        slideUp(binding.tvTagline, delay = 440L)

        binding.loaderDots.alpha = 0f
        binding.loaderDots.animate().alpha(1f).setStartDelay(620L).setDuration(300L).start()
        listOf(binding.dot1, binding.dot2, binding.dot3).forEachIndexed { index, dot ->
            breathe(dot, delay = 620L + index * 160L)
        }
    }

    private fun expandRing(ring: View, startDelay: Long, duration: Long) {
        ring.alpha = 0f
        ring.scaleX = 0.3f
        ring.scaleY = 0.3f
        ring.animate()
            .alpha(0.9f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(startDelay)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator(2f))
            .withEndAction {
                ring.animate().alpha(0.25f).setDuration(500L).start()
            }
            .start()
    }

    private fun slideUp(view: View, delay: Long) {
        view.alpha = 0f
        view.translationY = 36f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(520L)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
    }

    private fun breathe(dot: View, delay: Long) {
        dot.alpha = 0.35f
        dot.animate()
            .alpha(1f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setStartDelay(delay)
            .setDuration(480L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                dot.animate().alpha(0.35f).scaleX(1f).scaleY(1f).setDuration(480L).start()
            }
            .start()
    }

    private fun navigateOnward() {
        val destination = if (AuthRepository().isLoggedIn()) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }
        val options = ActivityOptions.makeCustomAnimation(
            this,
            R.anim.activity_enter,
            R.anim.activity_exit
        )
        startActivity(Intent(this, destination), options.toBundle())
        finish()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1750L
    }
}
