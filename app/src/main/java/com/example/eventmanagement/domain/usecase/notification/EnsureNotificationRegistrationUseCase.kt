package com.example.eventmanagement.domain.usecase.notification

import com.example.eventmanagement.core.error.Resource
import javax.inject.Inject

/**
 * After login: obtain current FCM token, persist it under the user, and subscribe
 * to the global announcements topic.
 */
class EnsureNotificationRegistrationUseCase @Inject constructor(
    private val getFcmTokenUseCase: GetFcmTokenUseCase,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase,
    private val subscribeToNotificationsUseCase: SubscribeToNotificationsUseCase
) {
    suspend operator fun invoke(): Resource<Unit> {
        when (val tokenResult = getFcmTokenUseCase()) {
            is Resource.Error -> return tokenResult
            Resource.Loading -> return Resource.Loading
            is Resource.Success -> {
                when (val registerResult = registerFcmTokenUseCase(tokenResult.data)) {
                    is Resource.Error -> return registerResult
                    Resource.Loading -> return Resource.Loading
                    is Resource.Success -> Unit
                }
            }
        }
        return subscribeToNotificationsUseCase()
    }
}
