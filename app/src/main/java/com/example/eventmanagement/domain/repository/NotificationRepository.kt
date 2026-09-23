package com.example.eventmanagement.domain.repository

import com.example.eventmanagement.core.error.Resource

interface NotificationRepository {
    suspend fun subscribeToReminders(): Resource<Unit>
    suspend fun unsubscribeFromReminders(): Resource<Unit>
    suspend fun getToken(): Resource<String>
    suspend fun registerToken(token: String): Resource<Unit>
    suspend fun unregisterCurrentToken(): Resource<Unit>
}
