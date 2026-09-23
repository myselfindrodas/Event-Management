package com.example.eventmanagement.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.domain.usecase.auth.GetCurrentUserUseCase
import com.example.eventmanagement.domain.usecase.auth.LoginUseCase
import com.example.eventmanagement.domain.usecase.auth.ResetPasswordUseCase
import com.example.eventmanagement.domain.usecase.auth.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false
)

sealed interface AuthUiEffect {
    data object Authenticated : AuthUiEffect
    data object ResetSent : AuthUiEffect
    data class Error(val error: AppError) : AuthUiEffect
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AuthUiEffect>()
    val effects: SharedFlow<AuthUiEffect> = _effects.asSharedFlow()

    fun isLoggedIn(): Boolean = getCurrentUserUseCase() != null

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = loginUseCase(email, password)) {
                Resource.Loading -> Unit
                is Resource.Success -> {
                    _uiState.value = AuthUiState(isLoading = false)
                    _effects.emit(AuthUiEffect.Authenticated)
                }
                is Resource.Error -> {
                    _uiState.value = AuthUiState(isLoading = false)
                    _effects.emit(AuthUiEffect.Error(result.error))
                }
            }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = signUpUseCase(email, password, confirmPassword)) {
                Resource.Loading -> Unit
                is Resource.Success -> {
                    _uiState.value = AuthUiState(isLoading = false)
                    _effects.emit(AuthUiEffect.Authenticated)
                }
                is Resource.Error -> {
                    _uiState.value = AuthUiState(isLoading = false)
                    _effects.emit(AuthUiEffect.Error(result.error))
                }
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = resetPasswordUseCase(email)) {
                Resource.Loading -> Unit
                is Resource.Success -> {
                    _uiState.value = AuthUiState(isLoading = false)
                    _effects.emit(AuthUiEffect.ResetSent)
                }
                is Resource.Error -> {
                    _uiState.value = AuthUiState(isLoading = false)
                    _effects.emit(AuthUiEffect.Error(result.error))
                }
            }
        }
    }
}
