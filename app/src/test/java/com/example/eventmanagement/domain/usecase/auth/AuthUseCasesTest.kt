package com.example.eventmanagement.domain.usecase.auth

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.error.ValidationField
import com.example.eventmanagement.core.validation.AuthValidator
import com.example.eventmanagement.domain.model.AuthUser
import com.example.eventmanagement.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    @Test
    fun login_rejectsInvalidEmail() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(), AuthValidator())
        val result = useCase("bad", "password1")
        assertTrue(result is Resource.Error)
        assertEquals(
            AppError.Validation(ValidationField.EMAIL_INVALID),
            (result as Resource.Error).error
        )
    }

    @Test
    fun signUp_rejectsMismatchedPasswords() = runTest {
        val useCase = SignUpUseCase(FakeAuthRepository(), AuthValidator())
        val result = useCase("user@example.com", "password1", "password2")
        assertEquals(
            AppError.Validation(ValidationField.PASSWORDS_DO_NOT_MATCH),
            (result as Resource.Error).error
        )
    }

    @Test
    fun login_succeedsForValidInput() = runTest {
        val useCase = LoginUseCase(FakeAuthRepository(), AuthValidator())
        val result = useCase("user@example.com", "password1")
        assertTrue(result is Resource.Success)
    }

    private class FakeAuthRepository : AuthRepository {
        override val currentUser: AuthUser? = null
        override val currentUserEmail: String? = null
        override fun isLoggedIn(): Boolean = false
        override fun observeAuthState(): Flow<AuthUser?> = flowOf(null)
        override suspend fun signUp(email: String, password: String): Resource<AuthUser> =
            Resource.Success(AuthUser("1", email))
        override suspend fun login(email: String, password: String): Resource<AuthUser> =
            Resource.Success(AuthUser("1", email))
        override suspend fun resetPassword(email: String): Resource<Unit> = Resource.Success(Unit)
        override fun logout() = Unit
    }
}
