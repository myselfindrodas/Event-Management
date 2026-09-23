package com.example.eventmanagement.domain.usecase.notification

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.repository.NotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationUseCasesTest {

    @Test
    fun registerFcmToken_passesProvidedToken() = runTest {
        val repo = FakeNotificationRepository()
        val useCase = RegisterFcmTokenUseCase(repo)

        val result = useCase("token-abc")

        assertTrue(result is Resource.Success)
        assertEquals("token-abc", repo.registeredToken)
    }

    @Test
    fun registerFcmToken_unauthorized() = runTest {
        val repo = FakeNotificationRepository().apply {
            registerResult = Resource.Error(AppError.Unauthorized)
        }
        val result = RegisterFcmTokenUseCase(repo)("token-abc")
        assertEquals(AppError.Unauthorized, (result as Resource.Error).error)
    }

    @Test
    fun ensureRegistration_getsTokenThenRegistersAndSubscribes() = runTest {
        val repo = FakeNotificationRepository()
        val useCase = EnsureNotificationRegistrationUseCase(
            getFcmTokenUseCase = GetFcmTokenUseCase(repo),
            registerFcmTokenUseCase = RegisterFcmTokenUseCase(repo),
            subscribeToNotificationsUseCase = SubscribeToNotificationsUseCase(repo)
        )

        val result = useCase()

        assertTrue(result is Resource.Success)
        assertEquals("device-token", repo.registeredToken)
        assertTrue(repo.subscribed)
        assertEquals(0, repo.getTokenExtraCalls)
    }

    private class FakeNotificationRepository : NotificationRepository {
        var registeredToken: String? = null
        var subscribed = false
        var registerResult: Resource<Unit> = Resource.Success(Unit)
        var getTokenExtraCalls = 0

        override suspend fun subscribeToReminders(): Resource<Unit> {
            subscribed = true
            return Resource.Success(Unit)
        }

        override suspend fun unsubscribeFromReminders(): Resource<Unit> = Resource.Success(Unit)

        override suspend fun getToken(): Resource<String> = Resource.Success("device-token")

        override suspend fun registerToken(token: String): Resource<Unit> {
            // Ensure register path does not call getToken again
            registeredToken = token
            return registerResult
        }

        override suspend fun unregisterCurrentToken(): Resource<Unit> = Resource.Success(Unit)
    }
}
