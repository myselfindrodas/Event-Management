package com.example.eventmanagement.core.error

sealed interface AppError {
    data object Network : AppError
    data object Unauthorized : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object TooManyRequests : AppError
    data object InvalidEventId : AppError
    data object InvalidCredentials : AppError
    data object AccountNotFound : AppError
    data object EmailAlreadyInUse : AppError
    data object WeakPassword : AppError
    data object UserDisabled : AppError
    data class Validation(val field: ValidationField) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}
