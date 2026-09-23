package com.example.eventmanagement.domain.usecase.notification

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.repository.NotificationRepository
import javax.inject.Inject

class SubscribeToNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(): Resource<Unit> =
        notificationRepository.subscribeToReminders()
}
