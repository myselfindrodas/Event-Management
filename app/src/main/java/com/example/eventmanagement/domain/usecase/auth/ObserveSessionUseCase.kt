package com.example.eventmanagement.domain.usecase.auth

import com.example.eventmanagement.domain.model.AuthUser
import com.example.eventmanagement.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> = authRepository.observeAuthState()
}
