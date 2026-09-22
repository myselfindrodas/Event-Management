package com.example.eventmanagement.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.eventmanagement.R
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.databinding.ActivityForgotPasswordBinding
import com.example.eventmanagement.util.ThemePrefs
import com.example.eventmanagement.util.animateEntrance
import com.example.eventmanagement.util.animatePop
import com.example.eventmanagement.util.applyBottomInsetAsPadding
import com.example.eventmanagement.util.applyTopInsetAsPadding
import com.example.eventmanagement.util.staggerChildren
import com.google.android.material.snackbar.Snackbar

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePrefs.applySavedTheme(this)
        super.onCreate(savedInstanceState)
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

        viewModel.resetResult.observe(this) { result ->
            when (result) {
                is Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    Toast.makeText(
                        this,
                        "Password reset email sent. Check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
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
