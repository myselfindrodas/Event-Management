package com.example.eventmanagement.domain.usecase.auth

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.validation.AuthValidator
import com.example.eventmanagement.domain.model.AuthUser
import com.example.eventmanagement.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val authValidator: AuthValidator
) {
    suspend operator fun invoke(email: String, password: String): Resource<AuthUser> {
        val validation = authValidator.validateLogin(email, password)
        if (validation != null) return Resource.Error(validation)
        return authRepository.login(email, password)
    }
}
