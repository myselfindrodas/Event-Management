package com.example.eventmanagement.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.eventmanagement.R
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.databinding.ActivityLoginBinding
import com.example.eventmanagement.ui.main.MainActivity
import com.example.eventmanagement.util.ThemePrefs
import com.example.eventmanagement.util.animateEntrance
import com.example.eventmanagement.util.animatePop
import com.example.eventmanagement.util.applyBottomInsetAsPadding
import com.example.eventmanagement.util.applyTopInsetAsPadding
import com.example.eventmanagement.util.staggerChildren
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.applySavedTheme(this)
        super.onCreate(savedInstanceState)
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

        viewModel.authResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    openMain()
                }
                is Resource.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
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
