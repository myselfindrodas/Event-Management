package com.example.eventmanagement.presentation.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.eventmanagement.R
import com.example.eventmanagement.core.theme.ThemePreferences
import com.example.eventmanagement.core.ui.animateEntrance
import com.example.eventmanagement.core.ui.animatePop
import com.example.eventmanagement.core.ui.applyBottomInsetAsPadding
import com.example.eventmanagement.core.ui.applyTopInsetAsPadding
import com.example.eventmanagement.core.ui.staggerChildren
import com.example.eventmanagement.core.ui.toUserMessage
import com.example.eventmanagement.databinding.ActivityForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences.applySavedTheme()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.applyTopInsetAsPadding()
        binding.contentColumn.applyBottomInsetAsPadding()
        playEntrance()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnReset.setOnClickListener {
            viewModel.resetPassword(binding.etEmail.text.toString())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> setLoading(state.isLoading) }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            AuthUiEffect.ResetSent -> {
                                Toast.makeText(
                                    this@ForgotPasswordActivity,
                                    R.string.reset_email_sent,
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                            is AuthUiEffect.Error ->
                                Snackbar.make(binding.root, effect.error.toUserMessage(this@ForgotPasswordActivity), Snackbar.LENGTH_LONG).show()
                            AuthUiEffect.Authenticated -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun playEntrance() {
        binding.toolbar.animateEntrance(40L)
        binding.iconHolder.animatePop(delay = 120L)
        binding.tvTitle.animateEntrance(220L)
        binding.tvSubtitle.animateEntrance(290L)
        binding.formCard.animateEntrance(350L)
        binding.formColumn.staggerChildren(stepMs = 60L, startDelayMs = 430L)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnReset.isEnabled = !loading
        binding.btnReset.text = if (loading) "" else getString(R.string.reset_password)
    }
}
