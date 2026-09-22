package com.example.eventmanagement.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.eventmanagement.R
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.databinding.ActivitySignUpBinding
import com.example.eventmanagement.ui.main.MainActivity
import com.example.eventmanagement.util.ThemePrefs
import com.example.eventmanagement.util.animateEntrance
import com.example.eventmanagement.util.applyBottomInsetAsPadding
import com.example.eventmanagement.util.applyTopInsetAsPadding
import com.example.eventmanagement.util.staggerChildren
import com.google.android.material.snackbar.Snackbar

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.applySavedTheme(this)
        super.onCreate(savedInstanceState)
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

        viewModel.authResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
                is Resource.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
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
