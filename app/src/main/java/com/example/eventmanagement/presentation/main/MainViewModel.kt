package com.example.eventmanagement.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.BuildConfig
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.log.AppLogger
import com.example.eventmanagement.domain.usecase.auth.GetCurrentUserUseCase
import com.example.eventmanagement.domain.usecase.auth.LogoutUseCase
import com.example.eventmanagement.domain.usecase.notification.EnsureNotificationRegistrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val ensureNotificationRegistrationUseCase: EnsureNotificationRegistrationUseCase,
    private val logger: AppLogger
) : ViewModel() {

    fun isLoggedIn(): Boolean = getCurrentUserUseCase() != null

    fun onLoggedIn() {
        viewModelScope.launch {
            when (val result = ensureNotificationRegistrationUseCase()) {
                is Resource.Success -> {
                    if (BuildConfig.DEBUG) logger.d(TAG, "FCM token registered")
                }
                is Resource.Error -> {
                    if (BuildConfig.DEBUG) logger.e(TAG, "Failed to register FCM token")
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    private companion object {
        const val TAG = "FCM"
    }
}
