package com.example.eventmanagement.domain.repository

import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?
    val currentUserEmail: String?
    fun isLoggedIn(): Boolean
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun signUp(email: String, password: String): Resource<AuthUser>
    suspend fun login(email: String, password: String): Resource<AuthUser>
    suspend fun resetPassword(email: String): Resource<Unit>
    fun logout()
}
