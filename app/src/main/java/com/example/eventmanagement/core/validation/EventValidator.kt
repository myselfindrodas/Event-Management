package com.example.eventmanagement.core.validation

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.ValidationField
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventValidator @Inject constructor() {

    fun validateTitle(title: String): AppError? {
        if (title.isBlank()) return AppError.Validation(ValidationField.TITLE_REQUIRED)
        return null
    }
}
