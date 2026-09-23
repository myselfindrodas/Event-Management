package com.example.eventmanagement.domain.usecase.auth

import com.example.eventmanagement.domain.repository.AuthRepository
import com.example.eventmanagement.domain.repository.NotificationRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke() {
        runCatching { notificationRepository.unregisterCurrentToken() }
        authRepository.logout()
    }
}
