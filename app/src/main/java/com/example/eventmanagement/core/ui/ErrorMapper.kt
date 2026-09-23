package com.example.eventmanagement.core.ui

import android.content.Context
import com.example.eventmanagement.R
import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.ValidationField

fun AppError.toUserMessage(context: Context): String {
    return when (this) {
        is AppError.Network -> context.getString(R.string.error_network)
        is AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
        is AppError.PermissionDenied -> context.getString(R.string.error_permission_denied)
        is AppError.NotFound -> context.getString(R.string.error_not_found)
        is AppError.TooManyRequests -> context.getString(R.string.error_too_many_requests)
        is AppError.InvalidEventId -> context.getString(R.string.error_invalid_event_id)
        is AppError.InvalidCredentials -> context.getString(R.string.error_invalid_credentials)
        is AppError.AccountNotFound -> context.getString(R.string.error_account_not_found)
        is AppError.EmailAlreadyInUse -> context.getString(R.string.error_email_in_use)
        is AppError.WeakPassword -> context.getString(R.string.error_weak_password)
        is AppError.UserDisabled -> context.getString(R.string.error_user_disabled)
        is AppError.Validation -> this.field.toUserMessage(context)
        is AppError.Unknown -> context.getString(R.string.error_unknown)
    }
}

private fun ValidationField.toUserMessage(context: Context): String {
    return when (this) {
        ValidationField.EMAIL_REQUIRED -> context.getString(R.string.error_email_required)
        ValidationField.EMAIL_INVALID -> context.getString(R.string.error_email_invalid)
        ValidationField.PASSWORD_REQUIRED -> context.getString(R.string.error_password_required)
        ValidationField.PASSWORD_TOO_SHORT -> context.getString(R.string.error_password_too_short)
        ValidationField.PASSWORDS_DO_NOT_MATCH -> context.getString(R.string.error_passwords_mismatch)
        ValidationField.TITLE_REQUIRED -> context.getString(R.string.error_title_required)
    }
}
