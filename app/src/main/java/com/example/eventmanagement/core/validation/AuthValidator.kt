package com.example.eventmanagement.core.validation

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.ValidationField
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthValidator @Inject constructor() {

    fun validateEmail(email: String): AppError? {
        if (email.isBlank()) return AppError.Validation(ValidationField.EMAIL_REQUIRED)
        if (!EMAIL_REGEX.matches(email.trim())) {
            return AppError.Validation(ValidationField.EMAIL_INVALID)
        }
        return null
    }

    fun validatePassword(password: String, minLength: Int = 6): AppError? {
        if (password.isBlank()) return AppError.Validation(ValidationField.PASSWORD_REQUIRED)
        if (password.length < minLength) {
            return AppError.Validation(ValidationField.PASSWORD_TOO_SHORT)
        }
        return null
    }

    fun validateLogin(email: String, password: String): AppError? =
        validateEmail(email) ?: validatePassword(password)

    fun validateSignUp(email: String, password: String, confirmPassword: String): AppError? {
        validateEmail(email)?.let { return it }
        validatePassword(password)?.let { return it }
        if (password != confirmPassword) {
            return AppError.Validation(ValidationField.PASSWORDS_DO_NOT_MATCH)
        }
        return null
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
