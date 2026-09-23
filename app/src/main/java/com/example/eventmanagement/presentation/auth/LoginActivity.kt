package com.example.eventmanagement.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.eventmanagement.databinding.ActivityLoginBinding
import com.example.eventmanagement.presentation.main.MainActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences.applySavedTheme()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (viewModel.isLoggedIn()) {
            openMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.applyTopInsetAsPadding()
        binding.contentColumn.applyBottomInsetAsPadding()
        playEntrance()

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString()
            )
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> setLoading(state.isLoading) }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            AuthUiEffect.Authenticated -> openMain()
                            is AuthUiEffect.Error ->
                                Snackbar.make(binding.root, effect.error.toUserMessage(this@LoginActivity), Snackbar.LENGTH_LONG).show()
                            AuthUiEffect.ResetSent -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun playEntrance() {
        binding.logoHolder.animatePop(delay = 80L)
        binding.tvAppTitle.animateEntrance(200L)
        binding.tvAppSubtitle.animateEntrance(280L)
        binding.formCard.animateEntrance(340L)
        binding.formColumn.staggerChildren(stepMs = 60L, startDelayMs = 420L)
        binding.tvSignUp.animateEntrance(720L)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "" else getString(R.string.login)
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
