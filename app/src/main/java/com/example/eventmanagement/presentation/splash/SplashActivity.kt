package com.example.eventmanagement.presentation.splash

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.eventmanagement.R
import com.example.eventmanagement.core.theme.ThemePreferences
import com.example.eventmanagement.core.ui.applySystemBarInsets
import com.example.eventmanagement.databinding.ActivitySplashBinding
import com.example.eventmanagement.presentation.auth.LoginActivity
import com.example.eventmanagement.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences.applySavedTheme()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        playIntro()

        lifecycleScope.launch {
            delay(SPLASH_DURATION_MS)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val state = viewModel.sessionState.first { it !is SessionState.Loading }
                navigateOnward(state)
            }
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

    private fun navigateOnward(state: SessionState) {
        if (hasNavigated) return
        hasNavigated = true
        val destination = when (state) {
            SessionState.Authenticated -> MainActivity::class.java
            SessionState.Unauthenticated, SessionState.Loading -> LoginActivity::class.java
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
