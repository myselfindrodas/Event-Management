package com.example.eventmanagement.domain.usecase.auth

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.validation.AuthValidator
import com.example.eventmanagement.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val authValidator: AuthValidator
) {
    suspend operator fun invoke(email: String): Resource<Unit> {
        val validation = authValidator.validateEmail(email)
        if (validation != null) return Resource.Error(validation)
        return authRepository.resetPassword(email)
    }
}
