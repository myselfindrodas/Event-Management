package com.example.eventmanagement.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.data.repository.AuthRepository
import com.example.eventmanagement.util.Validators
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authResult = MutableLiveData<Resource<FirebaseUser>>()
    val authResult: LiveData<Resource<FirebaseUser>> = _authResult

    private val _resetResult = MutableLiveData<Resource<Unit>>()
    val resetResult: LiveData<Resource<Unit>> = _resetResult

    fun login(email: String, password: String) {
        val emailError = Validators.validateEmail(email)
        val passwordError = Validators.validatePassword(password)
        if (emailError != null || passwordError != null) {
            _authResult.value = Resource.Error(emailError ?: passwordError!!)
            return
        }
        viewModelScope.launch {
            _authResult.value = Resource.Loading
            _authResult.value = authRepository.login(email, password)
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        val emailError = Validators.validateEmail(email)
        val passwordError = Validators.validatePassword(password)
        when {
            emailError != null -> _authResult.value = Resource.Error(emailError)
            passwordError != null -> _authResult.value = Resource.Error(passwordError)
            password != confirmPassword -> _authResult.value = Resource.Error("Passwords do not match.")
            else -> viewModelScope.launch {
                _authResult.value = Resource.Loading
                _authResult.value = authRepository.signUp(email, password)
            }
        }
    }

    fun resetPassword(email: String) {
        val emailError = Validators.validateEmail(email)
        if (emailError != null) {
            _resetResult.value = Resource.Error(emailError)
            return
        }
        viewModelScope.launch {
            _resetResult.value = Resource.Loading
            _resetResult.value = authRepository.resetPassword(email)
        }
    }

    fun logout() = authRepository.logout()

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    class Factory(
        private val authRepository: AuthRepository = AuthRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
