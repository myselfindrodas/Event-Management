package com.example.eventmanagement.presentation.auth

import android.content.Intent
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
import com.example.eventmanagement.core.ui.applyBottomInsetAsPadding
import com.example.eventmanagement.core.ui.applyTopInsetAsPadding
import com.example.eventmanagement.core.ui.staggerChildren
import com.example.eventmanagement.core.ui.toUserMessage
import com.example.eventmanagement.databinding.ActivitySignUpBinding
import com.example.eventmanagement.presentation.main.MainActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePreferences.applySavedTheme()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.applyTopInsetAsPadding()
        binding.contentColumn.applyBottomInsetAsPadding()
        playEntrance()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSignUp.setOnClickListener {
            viewModel.signUp(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString(),
                binding.etConfirmPassword.text.toString()
            )
        }

        binding.tvLogin.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> setLoading(state.isLoading) }
                }
                launch {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            AuthUiEffect.Authenticated -> {
                                Toast.makeText(this@SignUpActivity, R.string.account_created, Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                                finishAffinity()
                            }
                            is AuthUiEffect.Error ->
                                Snackbar.make(binding.root, effect.error.toUserMessage(this@SignUpActivity), Snackbar.LENGTH_LONG).show()
                            AuthUiEffect.ResetSent -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun playEntrance() {
        binding.toolbar.animateEntrance(40L)
        binding.tvTitle.animateEntrance(140L)
        binding.tvSubtitle.animateEntrance(210L)
        binding.formCard.animateEntrance(280L)
        binding.formColumn.staggerChildren(stepMs = 60L, startDelayMs = 360L)
        binding.tvLogin.animateEntrance(620L)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !loading
        binding.btnSignUp.text = if (loading) "" else getString(R.string.create_account)
    }
}
