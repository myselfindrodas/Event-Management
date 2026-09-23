package com.example.eventmanagement.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.BuildConfig
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.log.AppLogger
import com.example.eventmanagement.domain.usecase.auth.GetCurrentUserUseCase
import com.example.eventmanagement.domain.usecase.auth.LogoutUseCase
import com.example.eventmanagement.domain.usecase.notification.GetFcmTokenUseCase
import com.example.eventmanagement.domain.usecase.notification.RegisterFcmTokenUseCase
import com.example.eventmanagement.domain.usecase.notification.SubscribeToNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val subscribeToNotificationsUseCase: SubscribeToNotificationsUseCase,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
    private val getFcmTokenUseCase: GetFcmTokenUseCase,
    private val logger: AppLogger
) : ViewModel() {

    fun isLoggedIn(): Boolean = getCurrentUserUseCase() != null

    fun onLoggedIn() {
        viewModelScope.launch {
            subscribeToNotificationsUseCase()
            registerFcmTokenUseCase()
            if (BuildConfig.DEBUG) {
                when (val result = getFcmTokenUseCase()) {
                    is Resource.Success -> logger.d(TAG, "FCM token registered")
                    is Resource.Error -> logger.e(TAG, "Failed to get FCM token")
                    Resource.Loading -> Unit
                }
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
