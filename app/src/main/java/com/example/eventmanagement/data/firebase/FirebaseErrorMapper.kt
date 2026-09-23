package com.example.eventmanagement.data.firebase

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.ValidationField
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

fun mapFirebaseException(error: Throwable): AppError {
    if (error is CancellationException) throw error
    return when (error) {
        is FirebaseAuthException -> mapAuthCode(error.errorCode)
        is FirebaseFirestoreException -> mapFirestoreCode(error.code)
        is FirebaseNetworkException -> AppError.Network
        else -> {
            val message = error.message.orEmpty()
            if (message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("Network", ignoreCase = true)
            ) {
                AppError.Network
            } else {
                AppError.Unknown(error)
            }
        }
    }
}

private fun mapAuthCode(code: String?): AppError = when (code) {
    "ERROR_INVALID_EMAIL" -> AppError.Validation(ValidationField.EMAIL_INVALID)
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> AppError.InvalidCredentials
    "ERROR_USER_NOT_FOUND" -> AppError.AccountNotFound
    "ERROR_USER_DISABLED" -> AppError.UserDisabled
    "ERROR_EMAIL_ALREADY_IN_USE" -> AppError.EmailAlreadyInUse
    "ERROR_WEAK_PASSWORD" -> AppError.WeakPassword
    "ERROR_TOO_MANY_REQUESTS" -> AppError.TooManyRequests
    "ERROR_NETWORK_REQUEST_FAILED" -> AppError.Network
    else -> AppError.Unknown()
}

private fun mapFirestoreCode(code: FirebaseFirestoreException.Code): AppError = when (code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED,
    FirebaseFirestoreException.Code.UNAUTHENTICATED -> AppError.PermissionDenied
    FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Network
    FirebaseFirestoreException.Code.NOT_FOUND -> AppError.NotFound
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> AppError.TooManyRequests
    else -> AppError.Unknown()
}
